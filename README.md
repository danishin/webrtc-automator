# Requirements
1. Scala

2. SBT

# TODO
1. Haven't checked if `fetch` or `update` actually works. Since my internet connection is too slow for it. Will do when back at home.

2. Currently only supports iOS.

# Installation
1. run `bin/fetch_ios.sh`
    - Fetch the entire library of webrtc as well as chromium and other dependencies. ~12GB
    
2. run `bin/update_ios.sh`
    - Make sure all of library dependencies are fetched properly
    - Copy all objc header files and create `output/ios/headers/libjingle-umbrella.h` file that you can later copy into your project
    - Copy all third-party library files to `output/ios/third-party/`

3. run `bin/build_ios_arm.sh`
    - Build for ARMv7 & ARM64 iOS Device and spit out a fat binary.
    - (If you want to support simulator instead, run `bin/build_ios_sim.sh`)
    
4. Copy `output/ios/libWebRTC.a` to your project.

5. Copy `output/ios/headers/libjingle-umbrella.h` to your project.

6. Copy `third-party/ios/SRWebSocket/SRWebSocket.h` and `third-party/ios/SRWebSocket/SRWebSocket.m` to your project.

7. Link these standard libraries
    - libc++.dylib
    - libicucore.dylib
    - Security.framework
    - CFNetwork.framework
    - GLKit.framework
    - libstdc++.6.dylib
    - AudioToolbox.framework
    - AVFoundation.framework
    - CoreAudio.framework
    - CoreMedia.framework
    - CoreVideo.framework
    - CoreGraphics.framework
    - OpenGLES.framework
    - QuartzCore.framework
    - libsqlite3.dylib
    
8. Build your project!

# Tips
Run `while true; do du -sm src/; sleep 3; done` to keep track of the size of files being downloaded.

# Notes
## Commands 
1. `gclient sync`
    - Update files from SCM according to current configuration, for modules which have changed since last update or sync.
    - Force update even for unchanged modules

2. `gclient sync --force`
    - Update files from SCM according to current configuration, for all modules (useful for recovering files deleted from local copy)

3. `gclient sync --revision src@31000`
    - Update src directory to r31000



