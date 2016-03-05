#!/usr/bin/env bash

# $1 is location of private key .pem file
# $2 is public ip of TURN server

ssh -i "$1" ubuntu@"$2" 'ls -t -c1 /var/log/turnserver/turn* | head -1 | xargs tail -F -n 200'