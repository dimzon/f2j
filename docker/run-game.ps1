# Run the reference game with real data under Xvfb, capture log + screenshots.
# Run from repo root. Requires C:\Work\f2j-gamedata\game
# (master.dat, critter.dat, ce.dat, fallout2.cfg, data/).
#   powershell -File docker/run-game.ps1
#   powershell -File docker/run-game.ps1 -Seconds 60 -ShotEvery 10
param(
    [int]$Seconds = 120,
    [int]$ShotEvery = 15,
    [string]$ExtraArgs = "[ui]skip_opening_movies=2",
    [string]$GameDir = "C:\Work\f2j-gamedata\game-en"
)
$RepoRoot = (Get-Location).Path
$Inner = @"
set -e
cd /game
export SDL_AUDIODRIVER=dummy
export DISPLAY=:99
Xvfb :99 -screen 0 1280x1024x24 >/tmp/xvfb.log 2>&1 &
XVFB_PID=`$!
sleep 2
rm -f /game/shot-*.jpg /game/menu.jpg
timeout $Seconds /src/out/build/linux-x64-debug/fallout2-ce "$ExtraArgs" > /tmp/game.log 2>&1 &
GAME_PID=`$!
N=0
while kill -0 `$GAME_PID 2>/dev/null; do
  sleep $ShotEvery
  if kill -0 `$GAME_PID 2>/dev/null; then
    N=`$((N+1))
    xwd -root -silent | xwdtopnm 2>/dev/null | pnmtojpeg > /game/shot-`$N.jpg || true
  fi
done || true
wait `$GAME_PID || true
kill `$XVFB_PID || true
echo '--- game.log tail ---'
tail -15 /tmp/game.log
ls -la /game/shot-*.jpg 2>/dev/null || echo 'no screenshots'
"@
docker run --rm -v "${RepoRoot}:/src" -v "${GameDir}:/game" f2j-ref bash -lc "$Inner"
