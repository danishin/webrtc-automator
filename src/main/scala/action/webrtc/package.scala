package action

import util.Helper

package object webrtc extends Helper {
  // Type-safe tree traversal
  object root extends PathRep(sys.env("PWD")) {
    object lib extends PathRep(root("lib")) {
      object depot_tools extends PathRep(lib("depot_tools"))
      object src extends PathRep(lib("src")) {
        object webrtc extends PathRep(src("webrtc")) {
          object `webrtc_examples.gyp` extends PathRep(webrtc("webrtc_examples.gyp"))
        }
      }
      object `.gclient` extends PathRep(lib(".gclient"))
    }

    object output extends PathRep(root("output")) {
      object archive extends PathRep(output("archive"))
      object `WebRTCiOS.framework` extends PathRep(output("WebRTCiOS.framework")) {
        object Versions extends PathRep(`WebRTCiOS.framework`("Versions")) {
          object A extends PathRep(Versions("A")) {
            object Headers extends PathRep(A("Headers"))
          }
        }
      }
    }
  }
}
