# Reference build container for fallout2-ce (upstream C++).
# Mirrors CI job `linux (x64)` in .github/workflows/ci-build.yml:
#   ubuntu:22.04, system SDL2 + zlib, FALLOUT_VENDORED=OFF via preset linux-x64-debug.
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates \
        gpg \
        wget \
    && wget -O - https://apt.kitware.com/keys/kitware-archive-latest.asc 2>/dev/null \
        | gpg --dearmor - > /usr/share/keyrings/kitware-archive-keyring.gpg \
    && echo 'deb [signed-by=/usr/share/keyrings/kitware-archive-keyring.gpg] https://apt.kitware.com/ubuntu/ jammy main' \
        > /etc/apt/sources.list.d/kitware.list \
    && apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        cmake \
        ninja-build \
        git \
        pkg-config \
        ccache \
        libsdl2-dev \
        zlib1g-dev \
        python3 \
        xvfb \
        x11-apps \
        netpbm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /src

# ccache speeds up rebuilds when /src is bind-mounted and ccache dir is a volume.
ENV CCACHE_DIR=/ccache
ENV CMAKE_C_COMPILER_LAUNCHER=ccache
ENV CMAKE_CXX_COMPILER_LAUNCHER=ccache

CMD ["/bin/bash"]
