package action.turn

import com.amazonaws.auth.{BasicAWSCredentials, AWSCredentials, PropertiesCredentials}
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model._
import com.decodified.scalassh.{PublicKeyLogin, HostConfig, SSH}
import play.api.libs.json._
import _root_.util.{Program, Helper}

import scala.util.{Failure, Success, Try}

case class EC2Info(
                    aws_access_key: String,
                    aws_secret_key: String,
                    region: String,
                    instance_type: InstanceType,
                    key_pair_name: String,
                    key_pair_private_key_location: String
                  )
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

object Bootstrap extends Helper {
  def run(info: EC2Info): Program[Unit] = for {
    _      <- echo("Create EC2 Client")
    client <- createEC2Client(info)

    _      <- echo("Create EC2 Security Group")
    sgId   <- createEC2SecurityGroup(client)

    _      <- echo("Create EC2 Instance")
    pendingInstance <- createEC2Instance(client, sgId, info)

    _      <- echo("Wait Until EC2 Instance is Running")
    runningInstance <- waitUntilEC2IsRunning(client, pendingInstance)

    _ <- echo("Set Up EC2 Instance as TURN server")
    _ <- setupEC2Instance(runningInstance, info)
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

  private def createEC2Instance(client: AmazonEC2Client, securityGroupId: String, info: EC2Info): Program[Instance] = Program {
    val request = new RunInstancesRequest()
      .withInstanceType(info.instance_type)
      .withImageId("ami-00f4c76e") // Ubuntu 15.10
      .withMinCount(1)
      .withMaxCount(1)
      .withSecurityGroupIds(securityGroupId)
      .withKeyName(info.key_pair_name)

    val ec2 = client.runInstances(request).getReservation.getInstances.get(0)

    val tagRequest = new CreateTagsRequest()
      .withResources(ec2.getInstanceId)
      .withTags(new Tag("turn-server", ""))

    client.createTags(tagRequest)

    debug("EC2 Instance Created", ec2)

    ec2
  }

  private def waitUntilEC2IsRunning(client: AmazonEC2Client, instance: Instance): Program[Instance] = Program {
    def checkInstance(): Instance =
      client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instance.getInstanceId))
        .getReservations
        .get(0)
        .getInstances
        .get(0)

    def go(i: Int): Instance = {
      if (i > 60) throw new RuntimeException(s"EC2 instance is still not up after $i tries. Terminating...")

      val instance = checkInstance()

      instance.getState.getCode.toInt match {
        case 0 =>
          debug(s"Trying EC2 instance for $i times")
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

  private def setupEC2Instance(instance: Instance, info: EC2Info): Program[Unit] = {
    val config = HostConfig(PublicKeyLogin("ubuntu", info.key_pair_private_key_location), connectTimeout = Some(5000))

    SSH(instance.getPublicIpAddress, config) { client => for {
      _ <- client.shell("sudo apt-get install coturn")
    } yield () }.toProgram
  }
}
