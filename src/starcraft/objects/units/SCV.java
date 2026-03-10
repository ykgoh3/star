package starcraft.objects.units;

import starcraft.core.GamePanel;
import starcraft.engine.RenderUtils;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;
import starcraft.objects.buildings.CommandCenter;
import starcraft.objects.resources.MineralPatch;

import java.awt.*;
import java.awt.geom.Point2D;

public class SCV extends Unit {
    private static final int HARVEST_TIME_TICKS = 60;
    private static final int CARGO_SIZE = 8;
    private static final double HARVEST_RANGE = 48.0;
    private static final double RETURN_RANGE = 12.0;
    private static final double RETURN_APPROACH_MARGIN = 4.0;
    private static final double WAIT_RANGE = 6.0;
    private static final double LOOP_ASSIGNMENT_RANGE = 44.0;
    private static final double JAM_ESCAPE_MARGIN = 4.0;
    private static final double JAM_REPATH_DISTANCE = 10.0;

    private MineralPatch requestedMineral;
    private MineralPatch mineralTarget;
    private CommandCenter returnCenter;
    private int carryMinerals = 0;
    private int harvestTimer = HARVEST_TIME_TICKS;
    private boolean movingToMineral = false;
    private boolean returningCargo = false;
    private boolean waitingForMineral = false;
    private boolean loopAssigned = false;

    public SCV(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 60;
        this.hp = 60;
        this.damage = 5;
        this.attackDelay = 18;
        this.range = 28;
        this.speed = 2.0;
        this.size = 24;
        this.image = loadImage("/starcraft/res/scv.png");
    }

    public void startMining(MineralPatch target, CommandCenter center, GamePanel panel) {
        if (target == null || target.isDepleted()) {
            clearHarvestOrder();
            return;
        }

        clearHarvestOrder();

        this.requestedMineral = target;
        this.mineralTarget = target;
        this.returnCenter = center;
        this.harvestTimer = HARVEST_TIME_TICKS;
        this.loopAssigned = false;
        this.target = null;
        this.isMoving = true;
        this.manualOrder = true;
        this.commandState = 2;

        approachCurrentMineral();
    }

    public void clearHarvestOrder() {
        capturePartialHarvest();

        if (this.mineralTarget != null) {
            this.mineralTarget.releaseWorker(this);
        }
        this.requestedMineral = null;
        this.mineralTarget = null;
        this.returnCenter = null;
        this.harvestTimer = HARVEST_TIME_TICKS;
        this.loopAssigned = false;
        this.movingToMineral = false;
        this.returningCargo = false;
        this.waitingForMineral = false;
    }

    public void updateHarvest(GamePanel panel) {
        if (panel == null || mineralTarget == null) return;

        if (mineralTarget.isDepleted()) {
            clearHarvestOrder();
            return;
        }

        if (requestedMineral != null && requestedMineral.isDepleted()) {
            requestedMineral = mineralTarget;
        }

        if (returnCenter == null || returnCenter.isDestroyed()) {
            returnCenter = panel.findNearestCommandCenter(x, y, team);
            if (returnCenter == null) return;
        }

        if (carryMinerals > 0) {
            moveToCenterAndReturn(panel);
            return;
        }

        if (!loopAssigned) {
            if (!isNearMineral(mineralTarget, LOOP_ASSIGNMENT_RANGE)) {
                approachCurrentMineral();
                return;
            }

            assignLoop(panel);
            if (!loopAssigned) {
                return;
            }
        }

        if (mineralTarget.isActiveWorker(this)) {
            movingToMineral = true;
            waitingForMineral = false;
            moveToMineralAndHarvest();
        } else {
            waitingForMineral = true;
            movingToMineral = false;
            waitForMineral();
        }
    }

    private void assignLoop(GamePanel panel) {
        MineralPatch origin = (requestedMineral != null && !requestedMineral.isDepleted()) ? requestedMineral : mineralTarget;
        MineralPatch assigned = panel.findBestMineralLoopAssignment(origin, this);
        if (assigned == null) {
            approachCurrentMineral();
            return;
        }

        mineralTarget = assigned;
        mineralTarget.assignWorker(this);
        loopAssigned = true;
        approachCurrentMineral();
    }

    private void capturePartialHarvest() {
        if (mineralTarget == null || carryMinerals > 0) return;
        if (!mineralTarget.isActiveWorker(this)) return;
        if (movingToMineral || waitingForMineral || returningCargo) return;

        int progressTicks = HARVEST_TIME_TICKS - harvestTimer;
        if (progressTicks <= 0) return;

        int partialAmount = (CARGO_SIZE * progressTicks) / HARVEST_TIME_TICKS;
        partialAmount = Math.max(0, Math.min(CARGO_SIZE - 1, partialAmount));
        if (partialAmount <= 0) return;

        int mined = mineralTarget.harvest(partialAmount);
        if (mined > 0) {
            carryMinerals = mined;
            mineralTarget.releaseActiveWorker(this);
        }
    }

    private void moveToMineralAndHarvest() {
        Point2D.Double harvestPoint = mineralTarget.getHarvestPoint(this, returnCenter);
        double distToPatch = vectorMath.getDistance(x, y, mineralTarget.getX(), mineralTarget.getY());
        double distToPoint = vectorMath.getDistance(x, y, harvestPoint.x, harvestPoint.y);

        Point2D.Double jamEscape = getJamEscapePoint(distToPatch, distToPoint);
        if (jamEscape != null) {
            commandMove(jamEscape.x, jamEscape.y);
            movingToMineral = true;
            returningCargo = false;
            waitingForMineral = false;
            return;
        }

        if (distToPatch > HARVEST_RANGE || distToPoint > Math.max(speed, 3.0)) {
            commandMove(harvestPoint.x, harvestPoint.y);
            movingToMineral = true;
            returningCargo = false;
            waitingForMineral = false;
            return;
        }

        stop();
        manualOrder = true;
        isMoving = false;
        movingToMineral = false;
        waitingForMineral = false;
        mineralTarget.triggerHitEffect(new Color(130, 220, 255), 2, 3);
        harvestTimer--;
        if (harvestTimer > 0) return;

        int mined = mineralTarget.harvest(CARGO_SIZE);
        harvestTimer = HARVEST_TIME_TICKS;

        if (mined > 0) {
            carryMinerals = mined;
            mineralTarget.releaseActiveWorker(this);
            double[] returnPoint = getReturnApproachPoint();
            commandMove(returnPoint[0], returnPoint[1]);
            returningCargo = true;
        }
    }

    private void waitForMineral() {
        Point2D.Double waitPoint = mineralTarget.getApproachPoint(this, returnCenter);
        double distToWaitPoint = vectorMath.getDistance(x, y, waitPoint.x, waitPoint.y);
        if (distToWaitPoint > WAIT_RANGE) {
            commandMove(waitPoint.x, waitPoint.y);
            waitingForMineral = true;
            returningCargo = false;
            return;
        }

        stop();
        manualOrder = true;
        isMoving = false;
        waitingForMineral = true;
        movingToMineral = false;
        returningCargo = false;
    }

    private void moveToCenterAndReturn(GamePanel panel) {
        double distToEdge = distanceToBuildingEdge(returnCenter);
        if (distToEdge > RETURN_RANGE) {
            double[] returnPoint = getReturnApproachPoint();
            commandMove(returnPoint[0], returnPoint[1]);
            returningCargo = true;
            movingToMineral = false;
            waitingForMineral = false;
            return;
        }

        stop();
        manualOrder = true;
        isMoving = false;
        panel.addMinerals(team, carryMinerals);
        carryMinerals = 0;
        returningCargo = false;

        if (mineralTarget != null && !mineralTarget.isDepleted()) {
            mineralTarget.resumeWorker(this);
            approachCurrentMineral();
        } else {
            clearHarvestOrder();
        }
    }

    private void approachCurrentMineral() {
        if (mineralTarget == null) return;

        Point2D.Double point = loopAssigned
                ? mineralTarget.getApproachPoint(this, returnCenter)
                : mineralTarget.getHarvestPoint(this, returnCenter);
        commandMove(point.x, point.y);
        movingToMineral = true;
        waitingForMineral = false;
        returningCargo = false;
    }

    private Point2D.Double getJamEscapePoint(double distToPatch, double distToPoint) {
        if (mineralTarget == null) return null;

        double jamDistance = mineralTarget.getRadius() + size * 0.5 + JAM_ESCAPE_MARGIN;
        if (distToPatch >= jamDistance || distToPoint <= JAM_REPATH_DISTANCE) return null;

        double angle = Math.atan2(y - mineralTarget.getY(), x - mineralTarget.getX());
        if (Math.abs(x - mineralTarget.getX()) < 0.001 && Math.abs(y - mineralTarget.getY()) < 0.001) {
            angle = 0.0;
        }

        double escapeDistance = jamDistance + 6.0;
        return new Point2D.Double(
                mineralTarget.getX() + Math.cos(angle) * escapeDistance,
                mineralTarget.getY() + Math.sin(angle) * escapeDistance
        );
    }

    private boolean isNearMineral(MineralPatch patch, double range) {
        if (patch == null) return false;
        return vectorMath.getDistance(x, y, patch.getX(), patch.getY()) <= range;
    }

    private double[] getReturnApproachPoint() {
        if (returnCenter == null) return new double[]{x, y};

        double cx = returnCenter.getX();
        double cy = returnCenter.getY();
        double halfW = returnCenter.getWidth() / 2.0;
        double halfH = returnCenter.getHeight() / 2.0;

        double dx = x - cx;
        double dy = y - cy;

        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
            dx = 1.0;
        }

        double tx;
        double ty;
        if (Math.abs(dx) >= Math.abs(dy)) {
            tx = cx + Math.signum(dx) * (halfW + RETURN_APPROACH_MARGIN);
            ty = cy + clamp(dy, -halfH, halfH);
        } else {
            tx = cx + clamp(dx, -halfW, halfW);
            ty = cy + Math.signum(dy) * (halfH + RETURN_APPROACH_MARGIN);
        }

        return new double[]{tx, ty};
    }

    private double distanceToBuildingEdge(CommandCenter center) {
        if (center == null) return Double.MAX_VALUE;

        double dx = Math.max(Math.abs(x - center.getX()) - center.getWidth() / 2.0, 0.0);
        double dy = Math.max(Math.abs(y - center.getY()) - center.getHeight() / 2.0, 0.0);
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void commandMove(double tx, double ty) {
        this.commandState = 2;
        this.isMoving = true;
        this.manualOrder = true;
        this.target = null;
        setMoveTarget(tx, ty);
    }

    private void setMoveTarget(double tx, double ty) {
        this.destX = tx;
        this.destY = ty;
        this.targetX = tx;
        this.targetY = ty;
    }

    @Override
    protected double getLookAngle() {
        if (mineralTarget != null) {
            if (carryMinerals > 0 && returnCenter != null) {
                return Math.atan2(returnCenter.getY() - y, returnCenter.getX() - x);
            }
            return Math.atan2(mineralTarget.getY() - y, mineralTarget.getX() - x);
        }

        return super.getLookAngle();
    }

    @Override
    protected boolean shouldUseEmbeddedEscape() {
        return mineralTarget == null;
    }

    @Override
    protected boolean canPassThroughUnits() {
        return movingToMineral || returningCargo;
    }

    @Override
    public void attack(GamePanel panel) {
        int prevHp = (target != null) ? target.hp : 0;
        super.attack(panel);

        if (target != null && target.hp < prevHp) {
            target.hitEffectColor = new Color(255, 230, 150);
            target.hitEffectStyle = 2;
            target.hitEffectTimer = 4;
        }
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        // SCV has no separate projectile/attack draw effect.
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            if (deathTimer > 292) {
                int t = deathTimer - 292;
                int radius = 18 - t;
                g.setColor(new Color(255, 200, 90, 180));
                g.fillOval((int) x - radius / 2, (int) y - radius / 2, radius, radius);
                g.setColor(new Color(255, 140, 60, 200));
                g.fillOval((int) x - radius / 3, (int) y - radius / 3, radius / 2, radius / 2);
            }
            return;
        }

        double lookAngle = getLookAngle();

        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
        } else {
            g.setColor(team == 0 ? new Color(180, 180, 90) : new Color(170, 120, 80));
            g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }

        if (carryMinerals > 0) {
            g.setColor(new Color(130, 220, 255));
            g.fillOval((int) x + 3, (int) y - 9, 5, 5);
        }

        drawHitEffect(g);
        drawHealthBar(g);
    }
}
