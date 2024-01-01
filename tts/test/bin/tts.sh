#!/bin/bash

[ "de-DE" = "$1" ] || {
  echo "Illegal language: $1" 1>&2
  exit 1
}

cat "`dirname $0`/../sag_mal_was.wav"

