package starcraft.objects.resources;

import starcraft.objects.Unit;
import starcraft.objects.buildings.CommandCenter;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MineralPatch {
    private static final Image HIGH_IMAGE = loadImage("/starcraft/res/minerals_750~1500.PNG");
    private static final Image MID_IMAGE = loadImage("/starcraft/res/minerals_374-750.png");
    private static final Image LOW_IMAGE = loadImage("/starcraft/res/minerals_1-374.png");
    private static final double HARVEST_DISTANCE = 40.0;
    private static final double WAIT_DISTANCE = 60.0;
    private static final int WAIT_SLOT_COUNT = 6;
    private static final int ACTIVE_WORKER_LIMIT = 1;
    private static final int HARVEST_DIRECTION_COUNT = 4;
    private static final int PATHING_RADIUS = 14;
    private static final Point2D.Double[] HARVEST_SLOT_OFFSETS = new Point2D.Double[]{
            new Point2D.Double(0.0, -HARVEST_DISTANCE),
            new Point2D.Double(HARVEST_DISTANCE, 0.0),
            new Point2D.Double(0.0, HARVEST_DISTANCE),
            new Point2D.Double(-HARVEST_DISTANCE, 0.0)
    };

    private final double x;
    private final double y;
    private final int radius;
    private int remaining;

    private final List<Unit> loopWorkers = new ArrayList<>();
    private final List<Unit> activeWorkers = new ArrayList<>();
    private final List<Unit> unavailableWorkers = new ArrayList<>();

    private int hitEffectTimer = 0;
    private Color hitEffectColor = new Color(130, 220, 255);
    private int hitEffectStyle = 2;

    public MineralPatch(int x, int y, int amount) {
        this.x = x;
        this.y = y;
        this.remaining = Math.max(0, amount);
        this.radius = 18;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getRadius() {
        return radius;
    }

    public int getPathingRadius() {
        return PATHING_RADIUS;
    }

    public boolean isDepleted() {
        return remaining <= 0;
    }

    public int getRemaining() {
        return remaining;
    }

    public int harvest(int amount) {
        if (amount <= 0 || remaining <= 0) return 0;
        int mined = Math.min(amount, remaining);
        remaining -= mined;
        return mined;
    }

    public int getAssignedWorkerCount() {
        return loopWorkers.size();
    }

    public boolean isAvailableFor(Unit worker) {
        return worker != null;
    }

    public boolean isActiveWorker(Unit worker) {
        return worker != null && activeWorkers.contains(worker);
    }

    public boolean isAssignedWorker(Unit worker) {
        return worker != null && loopWorkers.contains(worker);
    }

    public void assignWorker(Unit worker) {
        if (worker == null) return;

        if (!loopWorkers.contains(worker)) {
            loopWorkers.add(worker);
        }
        unavailableWorkers.remove(worker);
        promoteWorkers();
    }

    public void releaseWorker(Unit worker) {
        if (worker == null) return;

        loopWorkers.remove(worker);
        activeWorkers.remove(worker);
        unavailableWorkers.remove(worker);
        promoteWorkers();
    }

    public void releaseActiveWorker(Unit worker) {
        if (worker == null) return;
        activeWorkers.remove(worker);
        if (loopWorkers.contains(worker) && !unavailableWorkers.contains(worker)) {
            unavailableWorkers.add(worker);
        }
        promoteWorkers();
    }

    public void resumeWorker(Unit worker) {
        if (worker == null || !loopWorkers.contains(worker)) return;
        unavailableWorkers.remove(worker);
        promoteWorkers();
    }

    public Point2D.Double getHarvestPoint(Unit worker, CommandCenter center) {
        int slotIndex = getAssignedHarvestSlotIndex(worker, center);
        Point2D.Double offset = HARVEST_SLOT_OFFSETS[slotIndex];
        return new Point2D.Double(x + offset.x, y + offset.y);
    }

    public Point2D.Double getApproachPoint(Unit worker, CommandCenter center) {
        if (isActiveWorker(worker)) {
            return getHarvestPoint(worker, center);
        }
        return getWaitingPoint(worker, center);
    }

    private void promoteWorkers() {
        activeWorkers.removeIf(worker -> worker == null || worker.hp <= 0 || !loopWorkers.contains(worker));
        unavailableWorkers.removeIf(worker -> worker == null || worker.hp <= 0 || !loopWorkers.contains(worker));

        while (activeWorkers.size() < ACTIVE_WORKER_LIMIT) {
            Unit next = findNextEligibleWorker();
            if (next == null) {
                break;
            }
            activeWorkers.add(next);
        }
    }

    private Unit findNextEligibleWorker() {
        for (Unit worker : loopWorkers) {
            if (worker == null || worker.hp <= 0) continue;
            if (activeWorkers.contains(worker)) continue;
            if (unavailableWorkers.contains(worker)) continue;
            return worker;
        }
        return null;
    }

    private int getAssignedHarvestSlotIndex(Unit worker, CommandCenter center) {
        List<Unit> orderedWorkers = new ArrayList<>(activeWorkers);
        if (worker != null && !orderedWorkers.contains(worker)) {
            orderedWorkers.add(worker);
        }

        int workerIndex = orderedWorkers.indexOf(worker);
        if (workerIndex < 0) {
            workerIndex = 0;
        }

        List<Integer> orderedSlots = getHarvestSlotPriority(center);
        return orderedSlots.get(workerIndex % HARVEST_DIRECTION_COUNT);
    }

    private List<Integer> getHarvestSlotPriority(CommandCenter center) {
        List<Integer> orderedSlots = new ArrayList<>(HARVEST_DIRECTION_COUNT);
        for (int i = 0; i < HARVEST_DIRECTION_COUNT; i++) {
            orderedSlots.add(i);
        }

        orderedSlots.sort(Comparator.comparingDouble(slot -> {
            Point2D.Double offset = HARVEST_SLOT_OFFSETS[slot];
            return distanceToCenter(x + offset.x, y + offset.y, center);
        }));
        return orderedSlots;
    }

    private Point2D.Double getWaitingPoint(Unit worker, CommandCenter center) {
        int index = getWaitingWorkerIndex(worker);
        List<Integer> orderedSlots = getHarvestSlotPriority(center);
        int slotIndex = orderedSlots.get(index % HARVEST_DIRECTION_COUNT);
        double baseAngle = Math.atan2(HARVEST_SLOT_OFFSETS[slotIndex].y, HARVEST_SLOT_OFFSETS[slotIndex].x);
        int ringIndex = index / HARVEST_DIRECTION_COUNT;
        double angleOffset = (Math.PI * 2.0 * ringIndex) / WAIT_SLOT_COUNT;
        double layer = 1.0 + ringIndex * 0.35;
        double angle = baseAngle + angleOffset;

        return new Point2D.Double(
                x + Math.cos(angle) * WAIT_DISTANCE * layer,
                y + Math.sin(angle) * WAIT_DISTANCE * layer
        );
    }

    private int getWaitingWorkerIndex(Unit worker) {
        int index = 0;
        for (Unit loopWorker : loopWorkers) {
            if (loopWorker == null || activeWorkers.contains(loopWorker)) {
                continue;
            }
            if (loopWorker == worker) {
                return index;
            }
            index++;
        }
        return 0;
    }

    public double getBestHarvestDistanceTo(CommandCenter center) {
        double bestDistance = Double.MAX_VALUE;
        for (Point2D.Double offset : HARVEST_SLOT_OFFSETS) {
            double distance = distanceToCenter(x + offset.x, y + offset.y, center);
            if (distance < bestDistance) {
                bestDistance = distance;
            }
        }
        return bestDistance;
    }

    private double distanceToCenter(Unit worker, CommandCenter center) {
        if (worker == null) return Double.MAX_VALUE;
        if (center == null) return Point2D.distance(worker.x, worker.y, x, y);
        return Point2D.distance(worker.x, worker.y, center.getX(), center.getY());
    }

    private double distanceToCenter(double pointX, double pointY, CommandCenter center) {
        if (center == null) return Point2D.distance(pointX, pointY, x, y);
        return Point2D.distance(pointX, pointY, center.getX(), center.getY());
    }

    public void triggerHitEffect(Color color, int style, int duration) {
        this.hitEffectColor = (color != null) ? color : new Color(130, 220, 255);
        this.hitEffectStyle = style;
        this.hitEffectTimer = Math.max(this.hitEffectTimer, Math.max(1, duration));
    }

    public boolean contains(int worldX, int worldY) {
        double dx = worldX - x;
        double dy = worldY - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    public void draw(Graphics g) {
        int drawX = (int) (x - radius);
        int drawY = (int) (y - radius);
        int size = radius * 2;
        Image image = getStageImage();

        if (image != null) {
            g.drawImage(image, drawX, drawY, size, size, null);
        } else {
            Color body = isDepleted() ? new Color(70, 90, 100) : new Color(60, 190, 255);
            Color edge = isDepleted() ? new Color(90, 110, 120) : new Color(160, 240, 255);

            g.setColor(new Color(0, 0, 0, 90));
            g.fillOval(drawX + 2, drawY + 3, size, size);
            g.setColor(body);
            g.fillOval(drawX, drawY, size, size);
            g.setColor(edge);
            g.drawOval(drawX, drawY, size, size);
        }

        drawHitEffect(g);
    }

    private Image getStageImage() {
        if (remaining >= 750) {
            return HIGH_IMAGE;
        }
        if (remaining >= 374) {
            return MID_IMAGE;
        }
        if (remaining > 0) {
            return LOW_IMAGE;
        }
        return null;
    }

    private void drawHitEffect(Graphics g) {
        if (hitEffectTimer <= 0) return;

        Graphics2D g2 = (Graphics2D) g;
        Color effect = (hitEffectColor != null) ? hitEffectColor : new Color(130, 220, 255);

        if (hitEffectStyle == 2) {
            Stroke oldStroke = g2.getStroke();
            g2.setColor(effect);
            g2.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int rays = 9;
            for (int i = 0; i < rays; i++) {
                double t = (i - (rays - 1) * 0.5) / ((rays - 1) * 0.5);
                double baseAngle = -Math.PI / 2.0 + t * (Math.PI / 2.5);
                double sway = Math.sin((hitEffectTimer + i) * 1.7) * 0.08;
                double angle = baseAngle + sway;

                int len = 7 + (i % 4);
                int sx = (int) (x + Math.cos(angle) * 2);
                int sy = (int) (y + Math.sin(angle) * 2);
                int ex = (int) (x + Math.cos(angle) * len);
                int ey = (int) (y + Math.sin(angle) * len);
                g2.drawLine(sx, sy, ex, ey);
            }

            g2.setStroke(oldStroke);
        } else {
            g2.setColor(effect);
            int innerSize = 6;
            int outerSize = 10;
            g2.drawOval((int) x - innerSize / 2, (int) y - innerSize / 2, innerSize, innerSize);
            g2.drawOval((int) x - outerSize / 2, (int) y - outerSize / 2, outerSize, outerSize);
        }

        hitEffectTimer--;
    }

    private static Image loadImage(String path) {
        try {
            return javax.imageio.ImageIO.read(MineralPatch.class.getResource(path));
        } catch (Exception e) {
            return null;
        }
    }
}

