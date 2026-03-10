package starcraft.core;

import java.awt.Point;

public class TerrainGrid {
    public final int cellSize, cols, rows;
    private final boolean[][] blocked;

    public TerrainGrid(int w, int h, int size) {
        this.cellSize = size;
        this.cols = w / size;
        this.rows = h / size;
        this.blocked = new boolean[rows][cols];
    }

    public boolean isWalkableCell(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows && !blocked[y][x];
    }

    public void clearBlocked() {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                blocked[y][x] = false;
            }
        }
    }

    public void blockRectWorld(double centerX, double centerY, int width, int height, int padding) {
        double minWorldX = centerX - width / 2.0 - padding;
        double maxWorldX = centerX + width / 2.0 + padding;
        double minWorldY = centerY - height / 2.0 - padding;
        double maxWorldY = centerY + height / 2.0 + padding;

        int minCellX = clamp((int) Math.floor(minWorldX / cellSize), 0, cols - 1);
        int maxCellX = clamp((int) Math.floor(maxWorldX / cellSize), 0, cols - 1);
        int minCellY = clamp((int) Math.floor(minWorldY / cellSize), 0, rows - 1);
        int maxCellY = clamp((int) Math.floor(maxWorldY / cellSize), 0, rows - 1);

        for (int y = minCellY; y <= maxCellY; y++) {
            for (int x = minCellX; x <= maxCellX; x++) {
                blocked[y][x] = true;
            }
        }
    }

    public void blockCircleWorld(double centerX, double centerY, double radius) {
        double minWorldX = centerX - radius;
        double maxWorldX = centerX + radius;
        double minWorldY = centerY - radius;
        double maxWorldY = centerY + radius;

        int minCellX = clamp((int) Math.floor(minWorldX / cellSize), 0, cols - 1);
        int maxCellX = clamp((int) Math.floor(maxWorldX / cellSize), 0, cols - 1);
        int minCellY = clamp((int) Math.floor(minWorldY / cellSize), 0, rows - 1);
        int maxCellY = clamp((int) Math.floor(maxWorldY / cellSize), 0, rows - 1);

        double radiusSq = radius * radius;
        for (int y = minCellY; y <= maxCellY; y++) {
            for (int x = minCellX; x <= maxCellX; x++) {
                double dx = cellCenterX(x) - centerX;
                double dy = cellCenterY(y) - centerY;
                if (dx * dx + dy * dy <= radiusSq) {
                    blocked[y][x] = true;
                }
            }
        }
    }

    public Point worldToCell(double x, double y) {
        return new Point(clamp((int) (x / cellSize), 0, cols - 1), clamp((int) (y / cellSize), 0, rows - 1));
    }

    public double cellCenterX(int cx) {
        return cx * cellSize + cellSize / 2.0;
    }

    public double cellCenterY(int cy) {
        return cy * cellSize + cellSize / 2.0;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
