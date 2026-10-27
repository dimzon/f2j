#!/bin/bash
bash /src/docker/refharness/fixtures.sh >/dev/null 2>&1
/src/docker/refharness/build/refharness > /tmp/out.txt 2>&1
python3 - <<'PY'
s = open('/tmp/out.txt', errors='replace').read()
i = s.find('[aimtick]')
j = s.find('[mapnewmap]', i)
if j == -1:
    j = len(s)
print(s[i:j][:1800])
PY
