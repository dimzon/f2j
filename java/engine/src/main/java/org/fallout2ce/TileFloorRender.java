package org.fallout2ce;

/**
 * Floor tile rendering ({@code src/tile.cc}): {@code tileRenderFloorsInRect}
 * and {@code tileRenderFloor} with the shading tables and the intensity-map
 * fill. The roof, grid-outline, object and stencil passes of
 * {@code tileRefreshGame} are separate slices.
 */
final class TileFloorRender {

    private static final class RightsideUpTableEntry {
        final int field0;
        final int field4;

        RightsideUpTableEntry(int field0, int field4) {
            this.field0 = field0;
            this.field4 = field4;
        }
    }

    private static final class UpsideDownTableEntry {
        final int field0;
        final int field4;

        UpsideDownTableEntry(int field0, int field4) {
            this.field0 = field0;
            this.field4 = field4;
        }
    }

    private static final class Vertex {
        final int field0;
        final int off0;
        final int off1;
        int intensity;

        Vertex(int field0, int off0, int off1) {
            this.field0 = field0;
            this.off0 = off0;
            this.off1 = off1;
        }
    }

    private static final class Triangle {
        final int field0;
        final int field4;
        final int field8;

        Triangle(int field0, int field4, int field8) {
            this.field0 = field0;
            this.field4 = field4;
            this.field8 = field8;
        }
    }

    // 0x51D99C rightside_up_table.
    private static final RightsideUpTableEntry[] RIGHTSIDE_UP_TABLE = {
        new RightsideUpTableEntry(-1, 2),
        new RightsideUpTableEntry(78, 2),
        new RightsideUpTableEntry(76, 6),
        new RightsideUpTableEntry(73, 8),
        new RightsideUpTableEntry(71, 10),
        new RightsideUpTableEntry(68, 14),
        new RightsideUpTableEntry(65, 16),
        new RightsideUpTableEntry(63, 18),
        new RightsideUpTableEntry(61, 20),
        new RightsideUpTableEntry(58, 24),
        new RightsideUpTableEntry(55, 26),
        new RightsideUpTableEntry(53, 28),
        new RightsideUpTableEntry(50, 32),
    };

    // 0x51DA04 upside_down_table.
    private static final UpsideDownTableEntry[] UPSIDE_DOWN_TABLE = {
        new UpsideDownTableEntry(0, 32),
        new UpsideDownTableEntry(48, 32),
        new UpsideDownTableEntry(49, 30),
        new UpsideDownTableEntry(52, 26),
        new UpsideDownTableEntry(55, 24),
        new UpsideDownTableEntry(57, 22),
        new UpsideDownTableEntry(60, 18),
        new UpsideDownTableEntry(63, 16),
        new UpsideDownTableEntry(65, 14),
        new UpsideDownTableEntry(67, 12),
        new UpsideDownTableEntry(70, 8),
        new UpsideDownTableEntry(73, 6),
        new UpsideDownTableEntry(75, 4),
    };

    // 0x51DA6C verticies.
    private static final Vertex[] VERTICIES = {
        new Vertex(16, -1, -201),
        new Vertex(48, -2, -2),
        new Vertex(960, 0, 0),
        new Vertex(992, 199, -1),
        new Vertex(1024, 198, 198),
        new Vertex(1936, 200, 200),
        new Vertex(1968, 399, 199),
        new Vertex(2000, 398, 398),
        new Vertex(2912, 400, 400),
        new Vertex(2944, 599, 399),
    };

    // 0x51DB0C rightside_up_triangles.
    private static final Triangle[] RIGHTSIDE_UP_TRIANGLES = {
        new Triangle(2, 3, 0),
        new Triangle(3, 4, 1),
        new Triangle(5, 6, 3),
        new Triangle(6, 7, 4),
        new Triangle(8, 9, 6),
    };

    // 0x51DB48 upside_down_triangles.
    private static final Triangle[] UPSIDE_DOWN_TRIANGLES = {
        new Triangle(0, 3, 1),
        new Triangle(2, 5, 3),
        new Triangle(3, 6, 4),
        new Triangle(5, 8, 6),
        new Triangle(6, 9, 7),
    };

    // 0x668224 intensity_map.
    private static final int[] INTENSITY_MAP = new int[3280];

    private TileFloorRender() {
    }

    // 0x4B2944 square_render_floor.
    static void tileRenderFloorsInRect(Geometry.Rect rect, int elevation) {
        int temp;
        int minY;
        int minX;
        int maxX;
        int maxY;

        int[] c1 = new int[1];
        int[] c2 = new int[1];

        TileMath.squareTileScreenToCoord(rect.left, rect.top, elevation, c1, c2);
        temp = c1[0];
        minY = c2[0];
        TileMath.squareTileScreenToCoord(rect.right, rect.top, elevation, c1, c2);
        minX = c1[0];
        temp = c2[0];
        TileMath.squareTileScreenToCoord(rect.left, rect.bottom, elevation, c1, c2);
        maxX = c1[0];
        temp = c2[0];
        TileMath.squareTileScreenToCoord(rect.right, rect.bottom, elevation, c1, c2);
        temp = c1[0];
        maxY = c2[0];

        if (minX < 0) {
            minX = 0;
        }

        if (minX >= TileMath.gSquareGridWidth) {
            minX = TileMath.gSquareGridWidth - 1;
        }

        if (minY < 0) {
            minY = 0;
        }

        // FIXME: Probably a bug - testing X, then changing Y.
        if (minX >= TileMath.gSquareGridHeight) {
            minY = TileMath.gSquareGridHeight - 1;
        }

        Light.lightGetAmbientIntensity();

        int baseSquareTile = TileMath.gSquareGridWidth * minY;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int squareTile = baseSquareTile + x;
                Map.TileData[] squareGrid = (Map.TileData[]) TileMath.gTileSquares;
                int squareFid = squareGrid[elevation].tileFid[squareTile];
                int flags = (squareFid >> 12) & 0xF;
                if ((flags & 0x1) == 0) {
                    int[] screenX = new int[1];
                    int[] screenY = new int[1];
                    TileMath.squareTileToScreenXY(squareTile, screenX, screenY, elevation);
                    int frameId = squareFid & 0xFFF;
                    if (frameId == -1) {
                        frameId = 4095;
                    }
                    tileRenderFloor(frameId, screenX[0], screenY[0], rect);
                }
            }
            baseSquareTile += TileMath.gSquareGridWidth;
        }
    }

    // 0x4B2A24 square_render_floor_tile.
    private static void tileRenderFloor(int frameId, int x, int y, Geometry.Rect rect) {
        if (Art.artIsObjectTypeHidden(ObjTypes.ObjectType.OBJ_TYPE_TILE.id()) != 0) {
            return;
        }

        Art.ArtLock artLock = Art.artLock(FrmId.ofType(ObjectType.TILE, frameId));
        if (artLock == null) {
            return;
        }

        try {
            Art.ArtData art = artLock.art();

            int elev = Map.gElevation;
            int left = rect.left;
            int top = rect.top;
            int width = rect.right - rect.left + 1;
            int height = rect.bottom - rect.top + 1;
            int frameWidth;
            int frameHeight;
            int tile;
            int v76;
            int v77;
            int v78;
            int v79;

            int savedX = x;
            int savedY = y;

            if (left < 0) {
                left = 0;
            }

            if (top < 0) {
                top = 0;
            }

            if (left + width > TileMath.gTileWindowWidth) {
                width = TileMath.gTileWindowWidth - left;
            }

            if (top + height > TileMath.gTileWindowHeight) {
                height = TileMath.gTileWindowHeight - top;
            }

            if (x >= TileMath.gTileWindowWidth || x > rect.right
                    || y >= TileMath.gTileWindowHeight || y > rect.bottom) {
                return;
            }

            frameWidth = Art.artGetWidth(art, 0, Rotation.NE.value());
            frameHeight = Art.artGetHeight(art, 0, Rotation.NE.value());

            if (left < x) {
                v79 = 0;
                int v12 = left + width;
                v77 = frameWidth + x <= v12 ? frameWidth : v12 - x;
            } else {
                v79 = left - x;
                x = left;
                v77 = frameWidth - v79;
                if (v77 > width) {
                    v77 = width;
                }
            }

            if (top < y) {
                int v14 = height + top;
                v78 = 0;
                v76 = frameHeight + y <= v14 ? frameHeight : v14 - y;
            } else {
                v78 = top - y;
                y = top;
                v76 = frameHeight - v78;
                if (v76 > height) {
                    v76 = height;
                }
            }

            if (v77 <= 0 || v76 <= 0) {
                return;
            }

            tile = TileMath.tileFromScreenXY(savedX, savedY + 13);
            if (tile != -1) {
                int parity = tile & 1;
                int ambientIntensity = Light.lightGetAmbientIntensity();
                for (int i = 0; i < 10; i++) {
                    Vertex v = VERTICIES[i];
                    v.intensity = Math.max(Light.lightGetTileIntensity(elev, tile + (parity == 0 ? v.off0 : v.off1)), ambientIntensity);
                }

                int v23 = 0;
                for (int i = 0; i < 9; i++) {
                    if (VERTICIES[i + 1].intensity != VERTICIES[i].intensity) {
                        break;
                    }
                    v23++;
                }

                byte[] frameData = Art.artGetFrameData(art, 0, Rotation.NE.value());
                if (frameData == null) {
                    return;
                }

                if (v23 == 9) {
                    darkTransBufToBuf(frameData, frameWidth * v78 + v79, v77, v76, frameWidth,
                        TileMath.gTileWindowBuffer, x, y, TileMath.gTileWindowPitch,
                        VERTICIES[0].intensity);
                    return;
                }

                for (int i = 0; i < 5; i++) {
                    Triangle triangle = RIGHTSIDE_UP_TRIANGLES[i];
                    int v32 = VERTICIES[triangle.field8].intensity;
                    int v33 = VERTICIES[triangle.field8].field0;
                    int v34 = VERTICIES[triangle.field4].intensity - VERTICIES[triangle.field0].intensity;
                    int v35 = v34 / 32;
                    int v36 = (VERTICIES[triangle.field0].intensity - v32) / 13;
                    int position = v33;
                    if (v35 != 0) {
                        if (v36 != 0) {
                            for (int r = 0; r < 13; r++) {
                                int v41 = v32;
                                int v42 = RIGHTSIDE_UP_TABLE[r].field4;
                                position += RIGHTSIDE_UP_TABLE[r].field0;
                                for (int j = 0; j < v42; j++) {
                                    INTENSITY_MAP[position++] = v41;
                                    v41 += v35;
                                }
                                v32 += v36;
                            }
                        } else {
                            for (int r = 0; r < 13; r++) {
                                int v38 = v32;
                                int v39 = RIGHTSIDE_UP_TABLE[r].field4;
                                position += RIGHTSIDE_UP_TABLE[r].field0;
                                for (int j = 0; j < v39; j++) {
                                    INTENSITY_MAP[position++] = v38;
                                    v38 += v35;
                                }
                            }
                        }
                    } else {
                        if (v36 != 0) {
                            for (int r = 0; r < 13; r++) {
                                int v46 = RIGHTSIDE_UP_TABLE[r].field4;
                                position += RIGHTSIDE_UP_TABLE[r].field0;
                                for (int j = 0; j < v46; j++) {
                                    INTENSITY_MAP[position++] = v32;
                                }
                                v32 += v36;
                            }
                        } else {
                            for (int r = 0; r < 13; r++) {
                                int v44 = RIGHTSIDE_UP_TABLE[r].field4;
                                position += RIGHTSIDE_UP_TABLE[r].field0;
                                for (int j = 0; j < v44; j++) {
                                    INTENSITY_MAP[position++] = v32;
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < 5; i++) {
                    Triangle triangle = UPSIDE_DOWN_TRIANGLES[i];
                    int v50 = VERTICIES[triangle.field0].intensity;
                    int v51 = VERTICIES[triangle.field0].field0;
                    int v52 = VERTICIES[triangle.field8].intensity - v50;
                    int v53 = v52 / 32;
                    int v54 = (VERTICIES[triangle.field4].intensity - v50) / 13;
                    int position = v51;
                    if (v53 != 0) {
                        if (v54 != 0) {
                            for (int r = 0; r < 13; r++) {
                                int v59 = v50;
                                int v60 = UPSIDE_DOWN_TABLE[r].field4;
                                position += UPSIDE_DOWN_TABLE[r].field0;
                                for (int j = 0; j < v60; j++) {
                                    INTENSITY_MAP[position++] = v59;
                                    v59 += v53;
                                }
                                v50 += v54;
                            }
                        } else {
                            for (int r = 0; r < 13; r++) {
                                int v56 = v50;
                                int v57 = UPSIDE_DOWN_TABLE[r].field4;
                                position += UPSIDE_DOWN_TABLE[r].field0;
                                for (int j = 0; j < v57; j++) {
                                    INTENSITY_MAP[position++] = v56;
                                    v56 += v53;
                                }
                            }
                        }
                    } else {
                        if (v54 != 0) {
                            for (int r = 0; r < 13; r++) {
                                int v64 = UPSIDE_DOWN_TABLE[r].field4;
                                position += UPSIDE_DOWN_TABLE[r].field0;
                                for (int j = 0; j < v64; j++) {
                                    INTENSITY_MAP[position++] = v50;
                                }
                                v50 += v54;
                            }
                        } else {
                            for (int r = 0; r < 13; r++) {
                                int v62 = UPSIDE_DOWN_TABLE[r].field4;
                                position += UPSIDE_DOWN_TABLE[r].field0;
                                for (int j = 0; j < v62; j++) {
                                    INTENSITY_MAP[position++] = v50;
                                }
                            }
                        }
                    }
                }

                byte[] window = TileMath.gTileWindowBuffer;
                int destPos = TileMath.gTileWindowPitch * y + x;
                int srcPos = frameWidth * v78 + v79;
                int intensityPos = 160 + 80 * v78 + v79;
                int v86 = frameWidth - v77;
                int v85 = TileMath.gTileWindowPitch - v77;
                int v87 = 80 - v77;

                int remaining = v76;
                while (--remaining != -1) {
                    for (int kk = 0; kk < v77; kk++) {
                        int src = Byte.toUnsignedInt(frameData[srcPos]);
                        if (src != 0) {
                            window[destPos] = Color.intensityColorTable[src][INTENSITY_MAP[intensityPos] >> 9];
                        }
                        srcPos++;
                        intensityPos++;
                        destPos++;
                    }
                    destPos += v85;
                    intensityPos += v87;
                    srcPos += v86;
                }
            }
        } finally {
            artLock.close();
        }
    }

    /** Port of {@code _dark_trans_buf_to_buf} (src/object.cc). */
    static void darkTransBufToBuf(byte[] src, int srcOffset, int srcWidth, int srcHeight,
            int srcPitch, byte[] dest, int destX, int destY, int destPitch, int intensity) {
        int sp = srcOffset;
        int dp = destPitch * destY + destX;
        int srcStep = srcPitch - srcWidth;
        int destStep = destPitch - srcWidth;
        int intensityIndex = intensity / 512;

        for (int y = 0; y < srcHeight; y++) {
            for (int x = 0; x < srcWidth; x++) {
                int color = Byte.toUnsignedInt(src[sp]);
                if (color != 0) {
                    if (color < 0xE5) {
                        color = Color.intensityColorTable[color][intensityIndex];
                    }
                    dest[dp] = (byte) color;
                }
                sp++;
                dp++;
            }
            sp += srcStep;
            dp += destStep;
        }
    }
}
