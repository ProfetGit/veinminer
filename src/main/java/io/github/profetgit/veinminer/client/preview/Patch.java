package io.github.profetgit.veinminer.client.preview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A small floating vein: its cells in block coordinates round the struck cell (index 0, at 0, 0, 0). The pickaxe comes
// from the east (+x), so every cell has x <= 0.
final class Patch {
    final int[][] at;
    final boolean[] alt;
    private final Map<Long, Integer> index = new HashMap<>();

    private Patch(List<int[]> cells, List<Boolean> alts) {
        at = cells.toArray(new int[0][]);
        alt = new boolean[at.length];
        for (int i = 0; i < at.length; i++) {
            alt[i] = alts.get(i);
            index.put(key(at[i][0], at[i][1], at[i][2]), i);
        }
    }

    // one string per layer from the bottom, rows (z) separated by '/', columns (x) left to right: S the struck block, # a
    // block, d a block with the alternate look, . nothing
    static Patch of(String... layers) {
        int sc = -1, sr = -1;
        String[] first = layers[0].split("/");
        for (int r = 0; r < first.length; r++) if (first[r].indexOf('S') >= 0) {
            sr = r;
            sc = first[r].indexOf('S');
        }
        List<int[]> cells = new ArrayList<>();
        List<Boolean> alts = new ArrayList<>();
        cells.add(new int[] {0, 0, 0});
        alts.add(false);
        for (int y = 0; y < layers.length; y++) {
            String[] rows = layers[y].split("/");
            for (int r = 0; r < rows.length; r++) {
                for (int c = 0; c < rows[r].length(); c++) {
                    char ch = rows[r].charAt(c);
                    if (ch == '#' || ch == 'd') {
                        cells.add(new int[] {c - sc, y, r - sr});
                        alts.add(ch == 'd');
                    }
                }
            }
        }
        return new Patch(cells, alts);
    }

    int size() {
        return at.length;
    }

    int find(int x, int y, int z) {
        Integer i = index.get(key(x, y, z));
        return i == null ? -1 : i;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 64) << 16) | ((long) (y + 64) << 8) | (z + 64);
    }
}
