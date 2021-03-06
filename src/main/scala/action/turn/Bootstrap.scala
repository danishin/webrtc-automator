package action.turn

import com.amazonaws.auth.{BasicAWSCredentials, AWSCredentials, PropertiesCredentials}
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model._
import com.amazonaws.util.Base64
import play.api.libs.json._
import _root_.util.{Program, Helper}

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

case class EC2Info(aws_access_key: String, aws_secret_key: String, region: String, instance_type: InstanceType, key_pair_name: String, key_pair_private_key_location: String)
object EC2Info {
  implicit val instanceTypeReads = Reads {
    case JsString(s) => Try(InstanceType.fromValue(s)) match {
      case Success(a) => JsSuccess(a)
      case Failure(e) => JsError(e.toString)
    }
    case _ => JsError()
  }

  implicit val reads = Json.reads[EC2Info]
}

case class TURNConfigInfo(turn_username: String, turn_password: String, turn_db_realm: String, admin_username: String, admin_password: String, ssl_cert_subject: TURNConfigInfo.SSLCertSubject)
object TURNConfigInfo {
  case class SSLCertSubject(country: String, state: String, location: String, organization: String, common_name: String)

  implicit val sslCertSubjectReads = Json.reads[SSLCertSubject]
  implicit val reads = Json.reads[TURNConfigInfo]
}

object Bootstrap extends Helper {
  def run(info: EC2Info, turnConfigInfo: TURNConfigInfo): Program[Unit] = for {
    _      <- echo("Create EC2 Client")
    client <- createEC2Client(info)

    _      <- echo("Create EC2 Security Group")
    sgId   <- createEC2SecurityGroup(client)

    _      <- echo("Create EC2 Instance")
    pendingInstance <- createEC2Instance(client, sgId, info, turnConfigInfo)

    _      <- echo("Wait Until EC2 Instance is Running")
    runningInstance <- waitUntilEC2IsRunning(client, pendingInstance)

    _ <- echo(
      s"""
        |Please allow about 2-3 minutes until TURN server is done setting up.
        |You can check log file for setup at /var/log/cloud-init.log in ec2 instance via ssh.
        |
        |SSH Command: `ssh -i ${info.key_pair_private_key_location} ubuntu@${runningInstance.getPublicIpAddress}`
        |
        |Created EC2 Instance:
        |
        |InstanceId: ${runningInstance.getInstanceId}
        |InstanceType: ${runningInstance.getInstanceType}
        |Region: ${runningInstance.getPlacement.getAvailabilityZone}
        |PublicDNS: ${runningInstance.getPublicDnsName}
        |PublicIP: ${runningInstance.getPublicIpAddress}
      """.stripMargin)

      _ <- echo("Wait Until TURN Server is Running")
      _ <- waitUntilTURNServerIsRunning(runningInstance)

      _ <- echo("Tail remote log file")
      _ <- shell("ssh", "-o", "StrictHostKeyChecking=no", "-i", info.key_pair_private_key_location, s"ubuntu@${runningInstance.getPublicIpAddress}", "'ls -t -c1 /var/log/turnserver/turn* | head -1 | xargs tail -F -n 200'")
  } yield ()

  private def createEC2Client(info: EC2Info): Program[AmazonEC2Client] = Program {
    val client = new AmazonEC2Client(new BasicAWSCredentials(info.aws_access_key, info.aws_secret_key))
    client.setEndpoint(s"ec2.${info.region}.amazonaws.com")
    client
  }

  private def createEC2SecurityGroup(client: AmazonEC2Client): Program[String] = Program {
    val SecurityGroupName = "turn-server-coturn"

    Try(client.describeSecurityGroups(new DescribeSecurityGroupsRequest().withGroupNames(SecurityGroupName)).getSecurityGroups.get(0)).toOption match {
      case Some(sg) =>
        debug("Security Group Already Created", sg)

        sg.getGroupId
      case None =>
        val request = new CreateSecurityGroupRequest()
          .withGroupName(SecurityGroupName)
          .withDescription("Security Group for coturn TURN server ")

        val securityGroupId = client.createSecurityGroup(request).getGroupId

        val ingressRequest = {
          def ipPermission(protocol: String, port: Int) = new IpPermission()
            .withIpProtocol(protocol)
            .withIpRanges("0.0.0.0/0")
            .withFromPort(port)
            .withToPort(port)

          new AuthorizeSecurityGroupIngressRequest()
            .withGroupId(securityGroupId)
            .withIpPermissions(
              ipPermission("tcp", 22),
              ipPermission("tcp", 3478),
              ipPermission("udp", 3478)
            )
        }

        client.authorizeSecurityGroupIngress(ingressRequest)

        debug("Security Group Created With Permissions", request, ingressRequest)

        securityGroupId
    }
  }

  private def createEC2Instance(client: AmazonEC2Client, securityGroupId: String, info: EC2Info, turnConfigInfo: TURNConfigInfo): Program[Instance] = Program {
    val cloudInitScript = txt.turn_cloud_init(turnConfigInfo).body.trim

    val request = new RunInstancesRequest()
      .withInstanceType(info.instance_type)
      .withImageId("ami-00f4c76e") // Ubuntu 15.10
      .withMinCount(1)
      .withMaxCount(1)
      .withSecurityGroupIds(securityGroupId)
      .withKeyName(info.key_pair_name)
      .withUserData(Base64.encodeAsString(cloudInitScript.getBytes(): _*))

    val ec2 = client.runInstances(request).getReservation.getInstances.get(0)

    val tagRequest = new CreateTagsRequest()
      .withResources(ec2.getInstanceId)
      .withTags(new Tag("turn-server", ""))

    client.createTags(tagRequest)

    debug("EC2 Instance Created", ec2)
    debug("Cloud Init Script", cloudInitScript)

    ec2
  }

  private def waitUntilEC2IsRunning(client: AmazonEC2Client, instance: Instance): Program[Instance] = Program {
    def checkInstance(): Instance =
      client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instance.getInstanceId))
        .getReservations
        .get(0)
        .getInstances
        .get(0)

    @annotation.tailrec
    def go(i: Int): Instance = {
      if (i > 60) throw new RuntimeException(s"EC2 instance is still not up after $i tries. Terminating...")

      debug(s"Trying EC2 instance for $i times")

      val instance = checkInstance()

      instance.getState.getCode.toInt match {
        case 0 =>
          Thread.sleep(2000)
          go(i + 1)
        case 16 =>
          debug("EC2 instance is now running", instance)
          instance
        case 32 | 48 | 64 | 80 => throw new RuntimeException(s"Invalid State: ${instance.getState}")
        case _ => throw new RuntimeException(s"Unknown State: ${instance.getState}")
      }
    }

    go(1)
  }

  private def waitUntilTURNServerIsRunning(instance: Instance) = Program {
    @annotation.tailrec
    def go(i: Int): Unit = {
      if (i > 100) throw new RuntimeException(s"TURN server still not up after $i tries. Terminating...")

      debug(s"Trying TURN server for $i times")

      Try(Http(s"http://${instance.getPublicIpAddress}:3478").timeout(2000, 2000).asString) match {
        case Success(r) if r.is2xx =>
          debug("TURN Server is running")
        case _ =>
          Thread.sleep(2000)
          go(i + 1)
      }
    }

    go(1)
  }
}
