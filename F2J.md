# f2j — Fallout 2 Community Edition, Java port

Line-by-line Java port of [fallout2-ce](https://github.com/fallout2-ce/fallout2-ce)
(upstream snapshot: `d08c9ea`, the community continuation of
`alexbatalov/fallout2-ce`). Every ported module is differential-tested
against the original C++ — behavior first, byte-for-byte where it matters.

## Layout

- `java/` — the port (Maven: `f2j-parent` + `f2j-engine`). Rules: `java/PORTING.md`.
  Build & test: `mvn -B test` in `java/` (+ `F2J_GAMEDATA=.../game-en` for
  real-archive tests).
- `docker/` — reference environment: `f2j-ref` image (Ubuntu 22.04, like CI),
  `refharness/` (C++ probe printing deterministic vectors, `golden/vN.txt`
  etalon files), `run-game.ps1` (real game under Xvfb + screenshots).
- `src/`, `third_party/`, `os/`, … — upstream reference tree (read-only).
- Game data (`master.dat`, …) is **not** committed, see `docker/README.md`.

## Workflow (per module)

1. Vectors in the harness first (`docker/refharness/ref_main.cc` + new
   `golden/vN.txt`, old ones immutable).
2. Port to `java/engine`, tests against the golden values.
3. `mvn -B test` green + `check-refharness.ps1` → `VECTORS_MATCH`, then commit.
