# Requirements
1. Scala

2. SBT

# TODO
1. Haven't checked if `fetch` actually works. Since my internet connection is too slow for it. Will do when back at home.

# Features
1. Build `WebRTCiOS.framework`.

2. Bootstrap TURN server ([coturn](https://github.com/coturn/coturn)) running on EC2 instance.

# Usage
## Build WebRTCiOS.framework
1. Run `bin/webrtc/fetch_ios.sh`
    - Fetch the entire library of webrtc as well as chromium and other dependencies. ~12GB
    
2. Run `bin/webrtc/update_ios.sh`
    - Sync webrtc library to the latest release branch

3. Run `bin/webrtc/build_ios.sh (all | arm | armv7 | arm64 | sim | sim32 | sim64) (DEBUG | RELEASE)?`
    - Build for passed architectures with **default deployment target of 8.0 & RELEASE** and (over)write an archive file for each architecture in `output/archive/`.
    
4. Run `bin/webrtc/assemble_ios.sh (all | arm | armv7 | arm64 | sim | sim32 | sim64)`
    - Assemble all archives built in previous `build` phase and (over)write `WebRTCiOS.framework` in `output/`
    
5. Drag `output/WebRTCiOS.framework` to your project.

6. (If you use Swift) Add `#import <WebRTCiOS/WebRTCiOS.h>` to your bridging header.
    
7. Link these standard libraries
    - libc++.dylib
    - libstdc++.6.dylib
    - libsqlite3.dylib
    - CoreAudio.framework
    - CoreVideo.framework
    - CoreMedia.framework
    - CoreGraphics.framework
    - AudioToolbox.framework
    - VideoToolBox.framework
    - AVFoundation.framework
    - Security.framework
    - CFNetwork.framework
    - GLKit.framework
    - OpenGLES.framework
    - QuartzCore.framework
    
8. Build your project!

#### Tips
1. Run `while true; do du -sm src/; sleep 3; done` to keep track of the size of files being downloaded.

2. Install `ideviceinstaller` and Run `ideviceinstaller -i src/out_arm64/Release-iphoneos/AppRTCDemo.app` for example, to install `AppRTCDemo` on device.

## Assemble WebRTCiOS.framework from pre-built archive files
1. Make sure `output/archive` contains pre-built archive files for intended architectures.

2. Run `bin/webrtc/assemble_ios.sh (all | arm | armv7 | arm64 | sim | sim32 | sim64`

## Bootstrap TURN server
1. Create `config.json` at root directory with the following format:
    ```
    {
      "turn": {
        "ec2": {
          "aws_access_key": "",
          "aws_secret_key": "",
          "region": "ap-northeast-1",
    
          "instance_type": "t2.micro",
          "key_pair_name": "",
          "key_pair_private_key_location": ""
        },
    
        "turn_config": {
          "turn_username": "",
          "turn_password": "",
          "turn_db_realm": "",
          "admin_username": "",
          "admin_password": "",
    
          "ssl_cert_subject": {
            "country": "", // C
            "state": "", // ST
            "location": "", // L
            "organization": "", // O
            "common_name": "" // CN
          }
        }
      }
    }
    ```
    
2. Run `bin/turn/bootstrap.sh`

3. Wait Until EC2 instance is created and TURN server is bootstrapped. Once everything is set up, program will automatically switch to tailing remote log file of TURN server process.

#### Tips
1. HTTPS management interface can be accessed from the same ports as the main TURN listener.
    - i.e https://<PUBLIC_IP>:3478
    
#### Commands in Host
1. `bash bin/turn/tail_remote_log.sh PRIVATE_KEY_LOCATION PUBLIC_IP`
    - Tail remote log file of TURN server

#### Commands in EC2 instance
1. `sqlite3 /var/lib/turn/turndb`
    - Access User DB
    
2. `telnet 127.0.0.1 5766`
    - Access telnet management CLI for currently running `turnserver`