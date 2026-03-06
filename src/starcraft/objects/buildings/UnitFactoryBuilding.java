package starcraft.objects.buildings;

import starcraft.core.GamePanel;
import starcraft.core.TerrainGrid;
import starcraft.objects.Unit;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class UnitFactoryBuilding extends Building {
    private final int spawnIntervalTicks;
    private final int blockedRetryTicks;
    private final int spawnClearRadius;

    private int spawnTimer;
    private int queuedUnits = 0;

    protected UnitFactoryBuilding(double x, double y, int team, int width, int height, int maxHp,
                                  int spawnIntervalTicks, int blockedRetryTicks, int spawnClearRadius) {
        super(x, y, team, width, height, maxHp);
        this.spawnIntervalTicks = spawnIntervalTicks;
        this.blockedRetryTicks = blockedRetryTicks;
        this.spawnClearRadius = spawnClearRadius;
        this.spawnTimer = spawnIntervalTicks;
    }

    public void enqueueUnit() {
        queuedUnits++;
    }

    public int getQueuedUnits() {
        return queuedUnits;
    }

    @Override
    public void update(GamePanel panel) {
        if (panel == null || isDestroyed()) return;

        if (queuedUnits <= 0) {
            spawnTimer = spawnIntervalTicks;
            return;
        }

        if (spawnTimer > 0) {
            spawnTimer--;
            return;
        }

        Point spawnPoint = findSpawnPoint(panel);
        if (spawnPoint != null) {
            Unit spawned = createUnit(spawnPoint.x, spawnPoint.y, team);
            if (spawned != null) {
                panel.getUnits().add(spawned);
                queuedUnits = Math.max(0, queuedUnits - 1);
                spawnTimer = spawnIntervalTicks;
            } else {
                spawnTimer = blockedRetryTicks;
            }
        } else {
            spawnTimer = blockedRetryTicks;
        }
    }

    protected double getProductionProgress() {
        if (queuedUnits <= 0) return 0.0;
        if (spawnIntervalTicks <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, 1.0 - (double) spawnTimer / spawnIntervalTicks));
    }

    protected abstract Unit createUnit(int x, int y, int team);

    private Point findSpawnPoint(GamePanel panel) {
        List<Point> candidates = buildSpawnCandidates();
        TerrainGrid terrain = panel.getTerrain();

        for (Point p : candidates) {
            if (canSpawnAt(p.x, p.y, panel.getUnits(), terrain)) {
                return p;
            }
        }
        return null;
    }

    private List<Point> buildSpawnCandidates() {
        ArrayList<Point> points = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        int step = Math.max(10, spawnClearRadius - 4);
        int maxRings = 12;
        int verticalPushOut = 6;

        // Ring-first order: start at left-bottom, then trace rectangle clockwise.
        for (int ring = 0; ring < maxRings; ring++) {
            int ringOffset = spawnClearRadius + ring * step;

            int xRight = (int) Math.round(x + width / 2.0 + ringOffset);
            int xLeft = (int) Math.round(x - width / 2.0 - ringOffset);
            int yTop = (int) Math.round(y - height / 2.0 - ringOffset - verticalPushOut);
            int yBottom = (int) Math.round(y + height / 2.0 + ringOffset + verticalPushOut);

            if (yTop >= yBottom) {
                continue;
            }

            for (int px = xLeft; px <= xRight; px += step) {
                addUnique(points, seen, px, yBottom);
            }

            for (int py = yBottom - step; py >= yTop; py -= step) {
                addUnique(points, seen, xRight, py);
            }

            for (int px = xRight - step; px >= xLeft; px -= step) {
                addUnique(points, seen, px, yTop);
            }

            for (int py = yTop + step; py <= yBottom - step; py += step) {
                addUnique(points, seen, xLeft, py);
            }
        }

        return points;
    }

    private void addUnique(List<Point> points, Set<Long> seen, int px, int py) {
        long key = ((long) px << 32) ^ (py & 0xFFFFFFFFL);
        if (seen.add(key)) {
            points.add(new Point(px, py));
        }
    }

    private boolean canSpawnAt(double spawnX, double spawnY, List<Unit> units, TerrainGrid terrain) {
        if (terrain == null) return false;

        Point cell = terrain.worldToCell(spawnX, spawnY);
        if (!terrain.isWalkableCell(cell.x, cell.y)) {
            return false;
        }

        double minDistSq = spawnClearRadius * spawnClearRadius;
        for (Unit unit : units) {
            if (unit == null || unit.hp <= 0) continue;
            double dx = unit.x - spawnX;
            double dy = unit.y - spawnY;
            if (dx * dx + dy * dy < minDistSq) {
                return false;
            }
        }

        return true;
    }
}