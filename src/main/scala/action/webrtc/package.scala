package action

import util.Helper

package object webrtc extends Helper {
  // Type-safe tree traversal
  object root extends PathRep(sys.env("PWD")) {
    object lib extends PathRep(root("lib")) {
      object depot_tools extends PathRep(lib("depot_tools"))
      object src extends PathRep(lib("src"))
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

    object resources extends PathRep(root("resources")) {
      object `RTCTypes.h` extends PathRep(resources("RTCTypes.h"))
    }
  }
}
