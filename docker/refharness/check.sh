#!/bin/bash
# Rebuild the harness, re-run it and diff against the golden vectors.
# Runs inside f2j-ref. Exit 0 = vectors match golden/v178.txt
# (v1-v176 are earlier snapshots; v1's INFO line sits on top pre-v2 buffering).
set -e
RUN=0 bash /src/docker/refharness/build.sh
# Compare stdout only: the INFO lines are stderr debug prints whose unbuffered
# writes interleave with buffered stdout nondeterministically (often moved by
# adding sections), so they are excluded from the gate.
/src/docker/refharness/build/refharness > /tmp/vectors.txt 2>/dev/null
# block50 measures wall-clock blocking (inputBlockForTocks) and flips 0/1
# between runs under load.
diff -u -I '^block50=' /src/docker/refharness/golden/v178.txt /tmp/vectors.txt && echo VECTORS_MATCH
