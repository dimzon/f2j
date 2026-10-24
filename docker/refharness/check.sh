#!/bin/bash
# Rebuild the harness, re-run it and diff against the golden vectors.
# Runs inside f2j-ref. Exit 0 = vectors match golden/v150.txt
# (v1-v149 are earlier snapshots; v1's INFO line sits on top pre-v2 buffering).
set -e
RUN=0 bash /src/docker/refharness/build.sh
/src/docker/refharness/build/refharness > /tmp/vectors.txt 2>&1
# block50 measures wall-clock blocking (inputBlockForTocks) and flips 0/1
# between runs under load, so the gate ignores that one line; the value
# stays in the golden files for the record.
diff -u -I '^block50=' /src/docker/refharness/golden/v150.txt /tmp/vectors.txt && echo VECTORS_MATCH
