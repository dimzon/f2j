#!/bin/bash
# Rebuild the harness, re-run it and diff against the golden vectors.
# Runs inside f2j-ref. Exit 0 = vectors match golden/v112.txt
# (v1-v26 are earlier snapshots; v1's INFO line sits on top pre-v2 buffering).
set -e
RUN=0 bash /src/docker/refharness/build.sh
/src/docker/refharness/build/refharness > /tmp/vectors.txt 2>&1
diff -u /src/docker/refharness/golden/v112.txt /tmp/vectors.txt && echo VECTORS_MATCH
