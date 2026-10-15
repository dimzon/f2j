#!/bin/bash
# Synthetic fixtures for the reference harness. Idempotent; run inside f2j-ref.
set -e

# Small ZIP: stored a.txt + deflated sub/b.txt (+ skipped dir entry).
python3 -c "
import zipfile
with zipfile.ZipFile('/tmp/f2test.zip', 'w') as z:
    z.writestr('a.txt', 'stored-data', compress_type=zipfile.ZIP_STORED)
    z.writestr('sub/b.txt', 'deflated-data-' * 100, compress_type=zipfile.ZIP_DEFLATED)
    z.writestr('sub/', '')
"

# Empty ZIP: EOCD with zero entries.
python3 -c "open('/tmp/f2empty.zip','wb').write(bytes.fromhex('504b0506' + '00'*18))"

# Empty DAT probe.
: > /tmp/empty.dat

# Case-resolution fixture tree.
mkdir -p /tmp/f2resolve/SubDir
echo x > /tmp/f2resolve/SubDir/MixedCase.Txt

# Stale file-utils outputs.
rm -f /tmp/fu-plain.txt /tmp/fu-arch.gz /tmp/fu-back.txt /tmp/fu-nope.txt

# xbase dir fixtures (priority probes).
rm -rf /tmp/f2xbase_a /tmp/f2xbase_b
mkdir -p /tmp/f2xbase_a/sub/deep /tmp/f2xbase_b
printf 'A' > /tmp/f2xbase_a/shared.txt
printf 'B' > /tmp/f2xbase_b/shared.txt
printf '\x01\x02\x03\x04' > /tmp/f2xbase_a/be.bin
printf '\x00\x00\x00\x00' > /tmp/f2xbase_a/zeros.bin
printf 'line1\r\nline2\r\n' > /tmp/f2xbase_a/crlf.txt
printf 'GZDATA-gzip-content' | gzip > /tmp/f2xbase_b/arch.gz
