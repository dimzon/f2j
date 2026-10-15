#!/bin/bash
# Build the reference harness and print vectors to stdout. Runs inside f2j-ref.
# (Called by docker/refharness/build-refharness.ps1 and check-refharness.ps1.)
set -e
bash /src/docker/refharness/fixtures.sh
OBJDIR=/src/out/build/linux-x64-debug/CMakeFiles/fallout2-ce.dir
if [ ! -d "$OBJDIR" ]; then echo "engine objects not found, run docker/build-ref.ps1 first" >&2; exit 1; fi
mkdir -p /src/docker/refharness/build
OBJS=$(find "$OBJDIR" -name "*.o" ! -name "main.cc.o" ! -name "win32.cc.o")
FPATTERN=/src/out/build/linux-x64-debug/third_party/fpattern
g++ -std=c++17 -g -I/src/src $(pkg-config --cflags sdl2) /src/docker/refharness/ref_main.cc $OBJS $FPATTERN/libfpattern-windows-static.a $FPATTERN/libfpattern-static.a -o /src/docker/refharness/build/refharness $(pkg-config --libs sdl2) -lz
if [ "${RUN:-1}" = "1" ]; then
    /src/docker/refharness/build/refharness
fi
