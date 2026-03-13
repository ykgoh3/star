package starcraft.objects.units.terran;

import starcraft.core.GamePanel;
import starcraft.engine.RenderUtils;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;
import starcraft.objects.buildings.ConstructionSite;
import starcraft.objects.buildings.terran.CommandCenter;
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
    private static final int CONSTRUCTION_TICK_INTERVAL = 6;
    private static final int CONSTRUCTION_MOVE_INTERVAL = 30;
    private static final double CONSTRUCTION_RANGE = 10.0;

    private MineralPatch requestedMineral;
    private MineralPatch mineralTarget;
    private CommandCenter returnCenter;
    private int carryMinerals = 0;
    private int harvestTimer = HARVEST_TIME_TICKS;
    private boolean movingToMineral = false;
    private boolean returningCargo = false;
    private boolean waitingForMineral = false;
    private boolean loopAssigned = false;

    private ConstructionSite constructionSite;
    private ConstructionSite.Type pendingConstructionType;
    private double pendingConstructionX;
    private double pendingConstructionY;
    private int pendingBuildPointIndex = 0;
    private int constructionTickTimer = 0;
    private int constructionMoveTimer = 0;

    public SCV(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 60;
        this.hp = 60;
        this.armor = 0;
        this.damage = 5;
        this.attackDelay = 15;
        this.range = 10;
        this.speed = 2.0;
        this.size = 23;
        this.drawWidth = 23;
        this.drawHeight = 23;
        this.image = loadImage("/starcraft/res/scv.png");
    }

    public void startConstruction(ConstructionSite.Type type, double buildX, double buildY) {
        if (type == null) return;

        clearConstructionOrder();
        clearHarvestOrder();
        this.pendingConstructionType = type;
        this.pendingConstructionX = buildX;
        this.pendingConstructionY = buildY;
        this.pendingBuildPointIndex = ConstructionSite.getNearestBuildPointIndex(type, buildX, buildY, x, y);
        this.constructionTickTimer = CONSTRUCTION_TICK_INTERVAL;
        this.constructionMoveTimer = CONSTRUCTION_MOVE_INTERVAL;
        this.target = null;
        this.manualOrder = true;
        this.commandState = 2;
        this.isMoving = true;

        Point2D.Double buildPoint = ConstructionSite.getBuildPoint(type, buildX, buildY, pendingBuildPointIndex);
        commandMove(buildPoint.x, buildPoint.y);
    }

    public void startConstruction(ConstructionSite site) {
        if (site == null || site.isDestroyed() || site.isConstructionComplete()) return;

        clearConstructionOrder();
        clearHarvestOrder();
        this.constructionSite = site;
        this.pendingConstructionType = null;
        this.pendingConstructionX = 0.0;
        this.pendingConstructionY = 0.0;
        this.pendingBuildPointIndex = ConstructionSite.getNearestBuildPointIndex(site.getType(), site.getX(), site.getY(), x, y);
        this.constructionTickTimer = CONSTRUCTION_TICK_INTERVAL;
        this.constructionMoveTimer = CONSTRUCTION_MOVE_INTERVAL;
        this.target = null;
        this.manualOrder = true;
        this.commandState = 2;
        this.isMoving = true;
        site.setBuilder(this);
        site.setBuildPointIndex(pendingBuildPointIndex);

        Point2D.Double buildPoint = site.getCurrentBuildPoint();
        commandMove(buildPoint.x, buildPoint.y);
    }

    public boolean isConstructing(ConstructionSite site) {
        return site != null && constructionSite == site;
    }

    public void clearConstructionOrder() {
        this.constructionSite = null;
        this.pendingConstructionType = null;
        this.pendingConstructionX = 0.0;
        this.pendingConstructionY = 0.0;
        this.pendingBuildPointIndex = 0;
        this.constructionTickTimer = 0;
        this.constructionMoveTimer = 0;
    }

    public void updateConstruction(GamePanel panel) {
        if (panel == null) return;

        if (constructionSite == null) {
            if (pendingConstructionType == null) return;

            Point2D.Double firstBuildPoint = ConstructionSite.getBuildPoint(
                    pendingConstructionType,
                    pendingConstructionX,
                    pendingConstructionY,
                    pendingBuildPointIndex
            );
            double distToStart = vectorMath.getDistance(x, y, firstBuildPoint.x, firstBuildPoint.y);
            if (distToStart > CONSTRUCTION_RANGE) {
                commandMove(firstBuildPoint.x, firstBuildPoint.y);
                return;
            }

            stop();
            manualOrder = true;
            isMoving = false;
            constructionSite = panel.beginConstructionSite(
                    pendingConstructionType,
                    pendingConstructionX,
                    pendingConstructionY,
                    team,
                    this
            );
            if (constructionSite == null) {
                clearConstructionOrder();
                return;
            }
            constructionSite.setBuildPointIndex(pendingBuildPointIndex);
            pendingConstructionType = null;
        }

        if (constructionSite.isDestroyed() || constructionSite.isConstructionComplete()) {
            clearConstructionOrder();
            return;
        }

        Point2D.Double buildPoint = constructionSite.getCurrentBuildPoint();
        double dist = vectorMath.getDistance(x, y, buildPoint.x, buildPoint.y);
        if (dist > CONSTRUCTION_RANGE) {
            commandMove(buildPoint.x, buildPoint.y);
            return;
        }

        stop();
        manualOrder = true;
        isMoving = false;
        constructionTickTimer--;
        constructionMoveTimer--;

        if (constructionTickTimer <= 0) {
            constructionTickTimer = CONSTRUCTION_TICK_INTERVAL;
            hitEffectColor = new Color(255, 230, 150);
            hitEffectStyle = 2;
            hitEffectTimer = 3;
            constructionSite.triggerHitEffect(new Color(255, 220, 140), 2, 3);
        }

        if (constructionMoveTimer <= 0) {
            Point2D.Double nextPoint = constructionSite.advanceBuildPoint();
            constructionMoveTimer = CONSTRUCTION_MOVE_INTERVAL;
            commandMove(nextPoint.x, nextPoint.y);
        }
    }

    public void startMining(MineralPatch target, CommandCenter center, GamePanel panel) {
        if (target == null || target.isDepleted()) {
            clearConstructionOrder();
            clearHarvestOrder();
            return;
        }

        clearConstructionOrder();
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
        if (constructionSite != null || pendingConstructionType != null) {
            updateConstruction(panel);
            return;
        }

        if (panel == null || mineralTarget == null) return;

        if (mineralTarget.isDepleted()) {
            clearConstructionOrder();
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
            clearConstructionOrder();
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
        double halfW = returnCenter.getPathingBlockWidth() / 2.0;
        double halfH = returnCenter.getPathingBlockHeight() / 2.0;

        double dx = x - cx;
        double dy = y - cy;

        if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
            dy = 1.0;
        }

        double tx = cx + clamp(dx, -halfW * 0.45, halfW * 0.45);
        double ty = cy + Math.signum(dy) * (halfH + RETURN_APPROACH_MARGIN);
        return new double[]{tx, ty};
    }

    private double distanceToBuildingEdge(CommandCenter center) {
        if (center == null) return Double.MAX_VALUE;

        double dropOffHalfW = Math.max(center.getPathingBlockWidth() / 2.0, center.getWidth() / 2.0 - 10.0);
        double dropOffHalfH = Math.max(center.getPathingBlockHeight() / 2.0, center.getHeight() / 2.0 - 8.0);
        double dx = Math.max(Math.abs(x - center.getX()) - dropOffHalfW, 0.0);
        double dy = Math.max(Math.abs(y - center.getY()) - dropOffHalfH, 0.0);
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
        if (constructionSite != null) {
            Point2D.Double buildPoint = constructionSite.getCurrentBuildPoint();
            return Math.atan2(buildPoint.y - y, buildPoint.x - x);
        }

        if (pendingConstructionType != null) {
            Point2D.Double buildPoint = ConstructionSite.getBuildPoint(
                    pendingConstructionType,
                    pendingConstructionX,
                    pendingConstructionY,
                    pendingBuildPointIndex
            );
            return Math.atan2(buildPoint.y - y, buildPoint.x - x);
        }

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
        int prevHp = (target != null) ? target.getHp() : 0;
        super.attack(panel);

        if (target != null && target.getHp() < prevHp) {
            target.triggerHitEffect(new Color(255, 230, 150), 2, 4);
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
            RenderUtils.drawRotatedImage(g, image, x, y, getDrawWidth(), getDrawHeight(), lookX, lookY);
        } else {
            g.setColor(team == 0 ? new Color(180, 180, 90) : new Color(170, 120, 80));
            g.fillOval((int) Math.round(x - getDrawWidth() / 2.0), (int) Math.round(y - getDrawHeight() / 2.0), getDrawWidth(), getDrawHeight());
        }

        if (carryMinerals > 0) {
            g.setColor(new Color(130, 220, 255));
            g.fillOval((int) x + 3, (int) y - 9, 5, 5);
        }

        drawHitEffect(g);
        drawHealthBar(g);
    }
}



