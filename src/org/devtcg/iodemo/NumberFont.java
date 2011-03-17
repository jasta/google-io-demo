package org.devtcg.iodemo;

public class NumberFont {
    public static final Glyph[] sFont;

    public static final int CONSTANT_WIDTH = 4;
    public static final int CONSTANT_HEIGHT = 7;

    static {
        sFont = new Glyph[10];
        sFont[0] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 1, 1, 1 },
        });
        sFont[1] = new Glyph(new int[][] {
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
        });
        sFont[2] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 1, 1, 1, 1 },
                { 1, 0, 0, 0 },
                { 1, 0, 0, 0 },
                { 1, 1, 1, 1 },
        });
        sFont[3] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 1, 1, 1, 1 },
        });
        sFont[4] = new Glyph(new int[][] {
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
        });
        sFont[5] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 1, 0, 0, 0 },
                { 1, 0, 0, 0 },
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 1, 1, 1, 1 },
        });
        sFont[6] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 1, 0, 0, 0 },
                { 1, 0, 0, 0 },
                { 1, 1, 1, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 1, 1, 1 },
        });
        sFont[7] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
        });
        sFont[8] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 1, 1, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 1, 1, 1 },
        });
        sFont[9] = new Glyph(new int[][] {
                { 1, 1, 1, 1 },
                { 1, 0, 0, 1 },
                { 1, 0, 0, 1 },
                { 1, 1, 1, 1 },
                { 0, 0, 0, 1 },
                { 0, 0, 0, 1 },
                { 1, 1, 1, 1 },
        });
    }

    public static class Glyph {
        private int[][] mBitMap = new int[CONSTANT_HEIGHT][CONSTANT_WIDTH];

        private Glyph(int[][] bitmap) {
            if (bitmap.length != CONSTANT_HEIGHT || bitmap[0].length != CONSTANT_WIDTH) {
                throw new IllegalArgumentException("Unexpected bitmap dimensions");
            }
            mBitMap = bitmap;
        }

        public int getWidth() {
            return mBitMap[0].length;
        }

        public int getHeight() {
            return mBitMap.length;
        }

        public boolean isLit(int x, int y) {
            return mBitMap[y][x] != 0;
        }
    }
}
