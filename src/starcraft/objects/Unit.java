package starcraft.objects;

import starcraft.core.GamePanel;
import starcraft.core.TerrainGrid;
import starcraft.engine.RenderUtils;
import starcraft.engine.vectorMath;
import starcraft.objects.logic.AttackMoveLogic;
import starcraft.objects.logic.IdleLogic;
import starcraft.objects.logic.MeleeAttackMoveLogic;
import starcraft.objects.logic.MoveLogic;
import starcraft.objects.logic.StopLogic;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Unit {
    private static final double RENDER_SCALE = 1.2;
    public int commandState = 0; // 0: idle, 1: attack-move, 2: move
    public double destX, destY;
    public boolean aKeyPressed = false;

    public double x, y, targetX, targetY;
    public int team, size, drawWidth, drawHeight, hp, maxHp, damage, attackDelay, attackTimer;
    public double speed, range;
    public boolean isSelected = false, isMoving = false, manualOrder = false;
    public boolean autoRetaliating = false;
    public Unit target;

    public double lastDistToTarget = 99999;
    public double velX = 0, velY = 0;
    public int bypassDuration = 0;
    public int bypassSide = 0;
    public int stagnationTimer = 0;
    public int pathSkipTimer = 0;
    public int sideRecheckTimer = 0;

    public int deathTimer = 300;
    public int postAttackDelayTimer = 0;

    public double lastX, lastY;
    public int stuckTimer = 0;
    public double currentAngle = 0;
    public boolean isBypassing = false;
    public int escapeMoveTicks = 0;
    public double escapeDirX = 0;
    public double escapeDirY = 0;

    public List<Point> path = Collections.emptyList();
    public int pathIndex = 0;

    protected int attackEffectTimer = 0;
    public int hitEffectTimer = 0;
    public Color hitEffectColor = new Color(255, 255, 150);
    public int hitEffectStyle = 0; // 0: ring, 1: thin diagonal slash, 2: welding sparks
    public Image image;

    public boolean idleFacingRight = true;
    public int idleLookInterval = randomIdleLookInterval();
    public int idleLookTimer = idleLookInterval;

    public Unit(int x, int y, int team) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        this.team = team;
        this.lastX = x;
        this.lastY = y;
    }

    public int getDrawWidth() {
        int baseWidth = (drawWidth > 0) ? drawWidth : size;
        return Math.max(1, (int) Math.round(baseWidth * RENDER_SCALE));
    }

    public int getDrawHeight() {
        int baseHeight = (drawHeight > 0) ? drawHeight : size;
        return Math.max(1, (int) Math.round(baseHeight * RENDER_SCALE));
    }

    public Rectangle getSelectionBounds() {
        int width = getDrawWidth();
        int height = getDrawHeight();
        return new Rectangle((int) Math.round(x - width / 2.0), (int) Math.round(y - height / 2.0), width, height);
    }

    public boolean containsPoint(int worldX, int worldY) {
        double dx = (worldX - x) / Math.max(1.0, getDrawWidth() / 2.0);
        double dy = (worldY - y) / Math.max(1.0, getDrawHeight() / 2.0);
        return dx * dx + dy * dy <= 1.0;
    }

    public void update(List<Unit> allUnits, TerrainGrid terrain) {
        if (attackTimer > 0) attackTimer--;
        if (attackEffectTimer > 0) attackEffectTimer--;
        if (hitEffectTimer > 0) hitEffectTimer--;
        if (postAttackDelayTimer > 0) postAttackDelayTimer--;

        if (postAttackDelayTimer > 0) {
            velX = 0;
            velY = 0;
            return;
        }

        if (shouldUseEmbeddedEscape() && handleEmbeddedEscape(allUnits, terrain)) {
            updateIdleLook();
            return;
        }

        if (commandState == 0) {
            IdleLogic.execute(this, allUnits, terrain);
        } else if (commandState == 1) {
            if (this.range <= 50) {
                MeleeAttackMoveLogic.execute(this, allUnits, terrain);
            } else {
                AttackMoveLogic.execute(this, allUnits, terrain);
            }
        } else if (commandState == 2) {
            MoveLogic.execute(this, allUnits, terrain);
        }

        updateIdleLook();
    }

    public void handleBypassDirection(double mX, double mY, List<Unit> allUnits, TerrainGrid terrain) {
        if (bypassSide != 0 && sideRecheckTimer-- > 0) return;

        sideRecheckTimer = 45;
        double baseAngle = Math.atan2(mY, mX);
        double destAngle = Math.atan2(targetX - x, targetY - y);

        double checkDist = size * 1.8;
        boolean L = !canMoveSolid(x + Math.cos(baseAngle - 1.04) * checkDist, y + Math.sin(baseAngle - 1.04) * checkDist, allUnits, terrain);
        boolean R = !canMoveSolid(x + Math.cos(baseAngle + 1.04) * checkDist, y + Math.sin(baseAngle + 1.04) * checkDist, allUnits, terrain);

        if (L && !R) bypassSide = 1;
        else if (!L && R) bypassSide = -1;
        else bypassSide = (Math.abs(calculateAngleDiff(baseAngle - 1.04, destAngle)) <= Math.abs(calculateAngleDiff(baseAngle + 1.04, destAngle))) ? -1 : 1;
    }

    public double calculateAngleDiff(double a1, double a2) {
        double diff = a1 - a2;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return diff;
    }

    public void resolveActiveOverlap(List<Unit> allUnits, TerrainGrid terrain) {
        if (canPassThroughUnits()) return;

        for (Unit other : allUnits) {
            if (other == this || other.hp <= 0 || !other.isMoving || other.attackTimer > 0) continue;

            double d = vectorMath.getDistance(x, y, other.x, other.y);
            double minDist = (size + other.size) * 0.5;

            if (d < minDist && d > 0) {
                double push = (minDist - d) * 0.5;
                double pushX = (x - other.x) / d * push;
                double pushY = (y - other.y) / d * push;
                if (terrain.isWalkableCell((int) ((x + pushX) / terrain.cellSize), (int) ((y + pushY) / terrain.cellSize))) {
                    x += pushX;
                    y += pushY;
                }
            }
        }
    }

    public boolean canMoveSolid(double nextX, double nextY, List<Unit> allUnits, TerrainGrid terrain) {
        double r = size * 0.5;
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4.0;
            if (!terrain.isWalkableCell((int) ((nextX + Math.cos(ang) * r) / terrain.cellSize),
                    (int) ((nextY + Math.sin(ang) * r) / terrain.cellSize))) return false;
        }

        if (!canPassThroughUnits()) {
            for (Unit other : allUnits) {
                if (other == this || other.hp <= 0) continue;
                if (!other.isMoving || other.attackTimer > 0) {
                    if (vectorMath.getDistance(nextX, nextY, other.x, other.y) < (size + other.size) * 0.48) return false;
                }
            }
        }
        return true;
    }

    public void attack(GamePanel panel) {
        if (commandState == 2) return;
        if (Math.abs(velX) > 0.05 || Math.abs(velY) > 0.05) return;

        if (attackTimer > 0 || target == null) return;
        if (target.hp <= 0) return;

        double dist = vectorMath.getDistance(x, y, target.x, target.y);
        if (dist <= range + 5) {
            target.hp -= damage;

            if (target.hp > 0 && target.team != this.team) {
                if (canAutoRetaliate(target)) {
                    target.target = this;
                    target.manualOrder = false;
                    target.commandState = 1;
                    target.isMoving = false;
                    target.destX = target.x;
                    target.destY = target.y;
                    target.autoRetaliating = true;
                }
                alertNearbyAllies(target, panel);
            }

            attackTimer = attackDelay;

            postAttackDelayTimer = 8;
            attackEffectTimer = 4;
            target.hitEffectColor = new Color(255, 255, 150);
            target.hitEffectStyle = 0;
            target.hitEffectTimer = 4;
        }
    }

    protected void alertNearbyAllies(Unit damagedUnit, GamePanel panel) {
        if (panel == null || damagedUnit == null || damagedUnit.hp <= 0) return;
        if (damagedUnit.team == this.team) return;

        final double helpRange = 140.0;
        for (Unit ally : panel.getUnits()) {
            if (ally == null || ally == damagedUnit || ally == this) continue;
            if (ally.hp <= 0 || ally.team != damagedUnit.team) continue;
            if (ally.manualOrder) continue;
            if (!canAutoRetaliate(ally)) continue;

            double d = vectorMath.getDistance(ally.x, ally.y, damagedUnit.x, damagedUnit.y);
            if (d <= helpRange) {
                ally.target = this;
                ally.manualOrder = false;
                ally.commandState = 1;
                ally.isMoving = false;
                ally.destX = ally.x;
                ally.destY = ally.y;
                ally.autoRetaliating = true;
            }
        }
    }

    public boolean ignoresUnitCollision() {
        return canPassThroughUnits();
    }

    protected boolean canPassThroughUnits() {
        return false;
    }

    protected boolean canAutoRetaliate(Unit unit) {
        return unit != null && unit.hp > 0 && unit.commandState == 0 && !unit.isMoving && !unit.manualOrder;
    }

    public Point getNextPathNode(TerrainGrid terrain) {
        if (path == null || path.isEmpty() || pathIndex >= path.size()) return null;

        if (pathSkipTimer-- <= 0) {
            pathSkipTimer = 10;
            for (int i = path.size() - 1; i >= pathIndex; i--) {
                Point p = path.get(i);
                if (isPathClear(x, y, terrain.cellCenterX(p.x), terrain.cellCenterY(p.y), terrain)) {
                    pathIndex = i;
                    break;
                }
            }
        }

        Point node = path.get(pathIndex);
        if (vectorMath.getDistance(x, y, terrain.cellCenterX(node.x), terrain.cellCenterY(node.y)) < speed * 1.5) pathIndex++;
        return (pathIndex < path.size()) ? path.get(pathIndex) : null;
    }

    public boolean isPathClear(double x1, double y1, double x2, double y2, TerrainGrid terrain) {
        double d = vectorMath.getDistance(x1, y1, x2, y2);
        int steps = (int) (d / (terrain.cellSize / 2.0));
        for (int i = 1; i <= steps; i++) {
            double ratio = (double) i / steps;
            double cx = x1 + (x2 - x1) * ratio;
            double cy = y1 + (y2 - y1) * ratio;
            if (!terrain.isWalkableCell((int) (cx / terrain.cellSize), (int) (cy / terrain.cellSize))) return false;
            if (!terrain.isWalkableCell((int) ((cx + size * 0.3) / terrain.cellSize), (int) (cy / terrain.cellSize))) return false;
            if (!terrain.isWalkableCell((int) ((cx - size * 0.3) / terrain.cellSize), (int) (cy / terrain.cellSize))) return false;
        }
        return true;
    }

        public void stop() {
        StopLogic.execute(this);
    }

    protected boolean handleEmbeddedEscape(List<Unit> allUnits, TerrainGrid terrain) {
        if (!isEmbedded(allUnits, terrain)) {
            escapeMoveTicks = 0;
            return false;
        }

        if (escapeMoveTicks <= 0 || !canEscapeStep(escapeDirX, escapeDirY, allUnits, terrain)) {
            chooseEscapeDirection(allUnits, terrain);
        }

        if (escapeMoveTicks <= 0) {
            velX = 0;
            velY = 0;
            return true;
        }

        double nextX = x + escapeDirX * speed;
        double nextY = y + escapeDirY * speed;
        if (!canEscapeStep(escapeDirX, escapeDirY, allUnits, terrain)) {
            escapeMoveTicks = 0;
            velX = 0;
            velY = 0;
            return true;
        }

        x = nextX;
        y = nextY;
        velX = escapeDirX;
        velY = escapeDirY;
        escapeMoveTicks--;
        resolveActiveOverlap(allUnits, terrain);
        return true;
    }

    protected boolean isEmbedded(List<Unit> allUnits, TerrainGrid terrain) {
        return overlapsBlockedTerrain(terrain) || overlapsAnyUnit(allUnits);
    }

    protected boolean overlapsBlockedTerrain(TerrainGrid terrain) {
        if (terrain == null) return false;

        double r = size * 0.5;
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4.0;
            int cellX = (int) ((x + Math.cos(ang) * r) / terrain.cellSize);
            int cellY = (int) ((y + Math.sin(ang) * r) / terrain.cellSize);
            if (!terrain.isWalkableCell(cellX, cellY)) {
                return true;
            }
        }

        return false;
    }

    protected boolean overlapsAnyUnit(List<Unit> allUnits) {
        if (canPassThroughUnits()) return false;
        if (allUnits == null) return false;

        for (Unit other : allUnits) {
            if (other == this || other.hp <= 0) continue;
            if (vectorMath.getDistance(x, y, other.x, other.y) < (size + other.size) * 0.48) {
                return true;
            }
        }

        return false;
    }

    protected void chooseEscapeDirection(List<Unit> allUnits, TerrainGrid terrain) {
        double currentOverlap = getEmbeddedOverlapScore(x, y, allUnits, terrain);
        double baseAngle = ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0);
        double bestDirX = 0.0;
        double bestDirY = 0.0;
        double bestScore = currentOverlap;
        int bestTicks = 0;

        for (int i = 0; i < 20; i++) {
            double angle = baseAngle + ThreadLocalRandom.current().nextDouble(-Math.PI, Math.PI);
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            EscapeCandidate candidate = evaluateEscapeDirection(dx, dy, allUnits, terrain, currentOverlap);
            if (candidate.accepted && candidate.bestScore < bestScore) {
                bestDirX = dx;
                bestDirY = dy;
                bestScore = candidate.bestScore;
                bestTicks = candidate.suggestedTicks;
            }
        }

        double[] fallbackAngles = {0.0, Math.PI / 6.0, -Math.PI / 6.0, Math.PI / 3.0, -Math.PI / 3.0, Math.PI / 2.0, -Math.PI / 2.0, Math.PI};
        for (double angle : fallbackAngles) {
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            EscapeCandidate candidate = evaluateEscapeDirection(dx, dy, allUnits, terrain, currentOverlap);
            if (candidate.accepted && candidate.bestScore < bestScore) {
                bestDirX = dx;
                bestDirY = dy;
                bestScore = candidate.bestScore;
                bestTicks = candidate.suggestedTicks;
            }
        }

        if (bestTicks > 0) {
            escapeDirX = bestDirX;
            escapeDirY = bestDirY;
            escapeMoveTicks = bestTicks;
            return;
        }

        escapeMoveTicks = 0;
        escapeDirX = 0;
        escapeDirY = 0;
    }

    protected boolean canEscapeStep(double dirX, double dirY, List<Unit> allUnits, TerrainGrid terrain) {
        if (terrain == null) return false;
        if (Math.abs(dirX) < 0.001 && Math.abs(dirY) < 0.001) return false;

        double currentOverlap = getEmbeddedOverlapScore(x, y, allUnits, terrain);
        EscapeCandidate candidate = evaluateEscapeDirection(dirX, dirY, allUnits, terrain, currentOverlap);
        if (!candidate.accepted) {
            return false;
        }

        double nextX = x + dirX * speed;
        double nextY = y + dirY * speed;
        double nextOverlap = getEmbeddedOverlapScore(nextX, nextY, allUnits, terrain);
        return nextOverlap <= currentOverlap + 0.05;
    }

    protected EscapeCandidate evaluateEscapeDirection(double dirX, double dirY, List<Unit> allUnits, TerrainGrid terrain, double currentOverlap) {
        double bestScore = currentOverlap;
        int bestStep = 0;

        for (int step = 1; step <= 8; step++) {
            double sampleX = x + dirX * speed * step;
            double sampleY = y + dirY * speed * step;
            double score = getEmbeddedOverlapScore(sampleX, sampleY, allUnits, terrain);
            if (score < bestScore) {
                bestScore = score;
                bestStep = step;
                if (score <= 0.01) {
                    break;
                }
            }
        }

        boolean accepted = bestStep > 0 && bestScore + 0.01 < currentOverlap;
        int suggestedTicks = accepted ? Math.max(4, bestStep) : 0;
        return new EscapeCandidate(accepted, bestScore, suggestedTicks);
    }

    protected double getEmbeddedOverlapScore(double sampleX, double sampleY, List<Unit> allUnits, TerrainGrid terrain) {
        double score = 0.0;
        double r = size * 0.5;

        if (terrain != null) {
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4.0;
                int cellX = (int) ((sampleX + Math.cos(ang) * r) / terrain.cellSize);
                int cellY = (int) ((sampleY + Math.sin(ang) * r) / terrain.cellSize);
                if (!terrain.isWalkableCell(cellX, cellY)) {
                    score += 1.0;
                }
            }
        }

        if (!canPassThroughUnits() && allUnits != null) {
            for (Unit other : allUnits) {
                if (other == this || other.hp <= 0) continue;
                double minDist = (size + other.size) * 0.48;
                double dist = vectorMath.getDistance(sampleX, sampleY, other.x, other.y);
                if (dist < minDist) {
                    score += 1.0 + (minDist - dist) / Math.max(1.0, minDist);
                }
            }
        }

        return score;
    }

    protected static final class EscapeCandidate {
        final boolean accepted;
        final double bestScore;
        final int suggestedTicks;

        EscapeCandidate(boolean accepted, double bestScore, int suggestedTicks) {
            this.accepted = accepted;
            this.bestScore = bestScore;
            this.suggestedTicks = suggestedTicks;
        }
    }

    protected boolean shouldUseEmbeddedEscape() {
        return true;
    }

    public abstract void draw(Graphics g);

    protected abstract void drawAttackEffect(Graphics g, double lookAngle);

    protected void drawHitEffect(Graphics g) {
        if (hitEffectTimer <= 0) return;

        Color effect = (hitEffectColor != null) ? hitEffectColor : new Color(255, 255, 150);
        Graphics2D g2 = (Graphics2D) g;

        if (hitEffectStyle == 1) {
            Stroke oldStroke = g2.getStroke();
            g2.setColor(effect);
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) x - 6, (int) y + 5, (int) x + 6, (int) y - 5);
            g2.drawLine((int) x - 4, (int) y + 7, (int) x + 8, (int) y - 3);
            g2.drawLine((int) x - 8, (int) y + 3, (int) x + 4, (int) y - 7);
            g2.setStroke(oldStroke);
            return;
        }

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
            return;
        }

        g2.setColor(effect);
        int innerSize = 6;
        int outerSize = 10;
        g2.drawOval((int) x - innerSize / 2, (int) y - innerSize / 2, innerSize, innerSize);
        g2.drawOval((int) x - outerSize / 2, (int) y - outerSize / 2, outerSize, outerSize);
    }

    protected void drawHealthBar(Graphics g) {
        if (this.isSelected) {
            RenderUtils.drawHealthBar(g, x, y, getDrawWidth(), getDrawHeight(), hp, maxHp, this.team);
        }
    }

    protected void updateIdleLook() {
        if (isMoving) return;
        if (isEngagingTarget()) return;
        if (attackEffectTimer > 0 || postAttackDelayTimer > 0) return;

        if (idleLookTimer <= 0) {
            idleFacingRight = !idleFacingRight;
            idleLookInterval = randomIdleLookInterval();
            idleLookTimer = idleLookInterval;
        } else {
            idleLookTimer--;
        }
    }

    protected int randomIdleLookInterval() {
        return ThreadLocalRandom.current().nextInt(100, 151);
    }

    protected boolean isEngagingTarget() {
        if (target == null || target.hp <= 0) return false;
        double dist = vectorMath.getDistance(x, y, target.x, target.y);
        return dist <= range + 20;
    }

    protected double getLookAngle() {
        if (isEngagingTarget()) {
            return Math.atan2(target.y - y, target.x - x);
        }

        if (isMoving && (Math.abs(velX) > 0.01 || Math.abs(velY) > 0.01)) {
            return Math.atan2(velY, velX);
        }

        if ((attackEffectTimer > 0 || postAttackDelayTimer > 0) && target != null && target.hp > 0) {
            return Math.atan2(target.y - y, target.x - x);
        }

        return idleFacingRight ? 0.0 : Math.PI;
    }

    protected Image loadImage(String path) {
        try {
            return javax.imageio.ImageIO.read(getClass().getResource(path));
        } catch (Exception e) {
            return null;
        }
    }
}





