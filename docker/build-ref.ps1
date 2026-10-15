# Configure + build upstream C++ in the reference container (bind-mounts repo root).
# Run from repo root: powershell -File docker/build-ref.ps1
# Artifacts land in out/build/linux-x64-debug (git-ignored via /out).
$RepoRoot = (Get-Location).Path
docker run --rm `
    -v "${RepoRoot}:/src" `
    -v "f2j-ccache:/ccache" `
    f2j-ref bash -lc "cmake --preset linux-x64-debug && cmake --build --preset linux-x64-debug --parallel $(nproc)"
