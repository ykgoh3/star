package starcraft.objects.resources;

import starcraft.objects.Unit;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class MineralPatch {
    private static final Image IMAGE = loadImage("/starcraft/res/minerals.PNG");
    private static final double HARVEST_DISTANCE = 20.0;
    private static final double WAIT_DISTANCE = 40.0;
    private static final int WAIT_SLOT_COUNT = 6;

    private final double x;
    private final double y;
    private final int radius;
    private int remaining;

    private Unit activeWorker;
    private final List<Unit> waitingWorkers = new ArrayList<>();

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

    public boolean isAvailableFor(Unit worker) {
        return worker != null && (activeWorker == null || activeWorker == worker);
    }

    public boolean hasActiveWorker() {
        return activeWorker != null;
    }

    public boolean isActiveWorker(Unit worker) {
        return worker != null && activeWorker == worker;
    }

    public void assignWorker(Unit worker) {
        if (worker == null) return;

        if (activeWorker == worker) {
            waitingWorkers.remove(worker);
            return;
        }

        if (activeWorker == null) {
            activeWorker = worker;
            waitingWorkers.remove(worker);
            return;
        }

        if (!waitingWorkers.contains(worker)) {
            waitingWorkers.add(worker);
        }
    }

    public void releaseWorker(Unit worker) {
        if (worker == null) return;

        if (activeWorker == worker) {
            activeWorker = null;
            promoteNextWorker();
            return;
        }

        waitingWorkers.remove(worker);
    }

    public Point2D.Double getHarvestPoint() {
        return new Point2D.Double(x, y - HARVEST_DISTANCE);
    }

    public Point2D.Double getApproachPoint(Unit worker) {
        if (isActiveWorker(worker)) {
            return getHarvestPoint();
        }
        return getWaitingPoint(worker);
    }

    private void promoteNextWorker() {
        while (!waitingWorkers.isEmpty()) {
            Unit next = waitingWorkers.remove(0);
            if (next != null && next.hp > 0) {
                activeWorker = next;
                return;
            }
        }
    }

    private Point2D.Double getWaitingPoint(Unit worker) {
        int index = waitingWorkers.indexOf(worker);
        if (index < 0) {
            index = 0;
        }

        double angle = (-Math.PI / 2.0) + (Math.PI * 2.0 * (index % WAIT_SLOT_COUNT) / WAIT_SLOT_COUNT);
        double layer = 1.0 + (index / WAIT_SLOT_COUNT) * 0.35;
        return new Point2D.Double(
                x + Math.cos(angle) * WAIT_DISTANCE * layer,
                y + Math.sin(angle) * WAIT_DISTANCE * layer
        );
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

        if (IMAGE != null) {
            g.drawImage(IMAGE, drawX, drawY, size, size, null);
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
