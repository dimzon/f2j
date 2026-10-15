# Build the reference image. Run from repo root: powershell -File docker/build-image.ps1
docker build -t f2j-ref -f docker/Dockerfile docker/
