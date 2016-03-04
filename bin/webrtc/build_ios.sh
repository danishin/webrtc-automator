#!/usr/bin/env bash

# NB: depot_tools MUST be exported outside the runtime. It seems that passing PATH to `Process` doesn't work if the passed command itself relies on the given PATH.
# NB: This is because environment variables are passed to the subprocess that is generated **as a result of** given command not to the current process (obviously).
export PATH=$PATH:$PWD/lib/depot_tools

# defaults to RELEASE if unset
# http://stackoverflow.com/questions/9332802/how-to-write-a-bash-script-that-takes-optional-input-arguments
sbt "run webrtc build ios $1 ${2:-RELEASE}"