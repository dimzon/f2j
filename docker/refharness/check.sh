#!/bin/bash
# Rebuild the harness, re-run it and diff against the golden vectors.
# Runs inside f2j-ref. Exit 0 = vectors match golden/v165.txt
# (v1-v163 are earlier snapshots; v1's INFO line sits on top pre-v2 buffering).
set -e
RUN=0 bash /src/docker/refharness/build.sh
/src/docker/refharness/build/refharness > /tmp/vectors.txt 2>&1
# block50 measures wall-clock blocking (inputBlockForTocks) and flips 0/1
# between runs under load, and the randomBetween range log sometimes does not
# flush, so the gate ignores those lines; the values stay in the golden files
# for the record.
diff -u -I '^block50=' -I '^INFO: Random number' /src/docker/refharness/golden/v165.txt /tmp/vectors.txt && echo VECTORS_MATCH
