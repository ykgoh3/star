package starcraft.objects;

import starcraft.core.GamePanel;
import starcraft.core.TerrainGrid;
import starcraft.engine.RenderUtils;
import starcraft.engine.vectorMath;
import starcraft.objects.logic.*;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public abstract class Unit {
    public int commandState = 0; // 0: 대기, 1: 어택땅, 2: 순수이동
    public double destX, destY;
    public boolean aKeyPressed = false;

    public double x, y, targetX, targetY;
    public int team, size, hp, maxHp, damage, attackDelay, attackTimer;
    public double speed, range;
    public boolean isSelected = false, isMoving = false, manualOrder = false;
    public Unit target;

    public double lastDistToTarget = 99999;
    public double velX = 0, velY = 0;
    public int bypassDuration = 0;
    public int bypassSide = 0;
    public int stagnationTimer = 0;
    public int pathSkipTimer = 0;
    public int sideRecheckTimer = 0;

    // [추가] 핏자국 유지 타이머 (약 10초 = 300프레임)
    public int deathTimer = 300;

    // [추가] 공격 직후 이동을 멈추게 하는 후딜레이 타이머
    public int postAttackDelayTimer = 0;

    public double lastX, lastY;
    public int stuckTimer = 0;
    public double currentAngle = 0;
    public boolean isBypassing = false;

    public List<Point> path = Collections.emptyList();
    public int pathIndex = 0;

    protected int attackEffectTimer = 0;
    public int hitEffectTimer = 0;
    public Image image;

    public Unit(int x, int y, int team) {
        this.x = x; this.y = y;
        this.targetX = x; this.targetY = y;
        this.team = team;
        this.lastX = x; this.lastY = y;
    }

    public void update(List<Unit> allUnits, TerrainGrid terrain) {
        if (attackTimer > 0) attackTimer--;
        if (attackEffectTimer > 0) attackEffectTimer--;
        if (hitEffectTimer > 0) hitEffectTimer--;
        if (postAttackDelayTimer > 0) postAttackDelayTimer--;

        // 사격 직후 후딜레이 처리
        if (postAttackDelayTimer > 0) {
            velX = 0;
            velY = 0;
            return;
        }

        // 상태 머신 (취소했던 복잡한 양보 로직을 모두 제거하고 원래의 깔끔한 상태로 복구)
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
        for (Unit other : allUnits) {
            if (other == this || other.hp <= 0 || !other.isMoving || other.attackTimer > 0) continue;

            double d = vectorMath.getDistance(x, y, other.x, other.y);
            double minDist = (size + other.size) * 0.5;

            if (d < minDist && d > 0) {
                double push = (minDist - d) * 0.5;
                double pushX = (x - other.x) / d * push;
                double pushY = (y - other.y) / d * push;
                if (terrain.isWalkableCell((int)((x + pushX) / terrain.cellSize), (int)((y + pushY) / terrain.cellSize))) {
                    x += pushX; y += pushY;
                }
            }
        }
    }

    public boolean canMoveSolid(double nextX, double nextY, List<Unit> allUnits, TerrainGrid terrain) {
        double r = size * 0.5;
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4.0;
            if (!terrain.isWalkableCell((int)((nextX + Math.cos(ang) * r) / terrain.cellSize),
                    (int)((nextY + Math.sin(ang) * r) / terrain.cellSize))) return false;
        }

        for (Unit other : allUnits) {
            if (other == this || other.hp <= 0) continue;
            if (!other.isMoving || other.attackTimer > 0) {
                if (vectorMath.getDistance(nextX, nextY, other.x, other.y) < (size + other.size) * 0.48) return false;
            }
        }
        return true;
    }

    public void attack(GamePanel panel) {
        if (commandState == 2) return;
        if (Math.abs(velX) > 0.05 || Math.abs(velY) > 0.05) return;

        if (attackTimer > 0 || target == null) return;

        // [해결 2] 오버킬: 타겟이 죽은 지 10프레임(deathTimer <= 290)이 지났을 때만 사격을 포기합니다.
        // 방금 죽은 시체(deathTimer > 290)라면 사격을 진행하여 총알을 낭비합니다!
        if (target.hp <= 0 && target.deathTimer <= 290) return;

        double dist = vectorMath.getDistance(x, y, target.x, target.y);
        if (dist <= range + 5) {
            target.hp -= damage; // 죽은 적의 체력을 음수로 더 깎으며 내 공격기회 소비
            attackTimer = attackDelay;

            postAttackDelayTimer = 8;
            attackEffectTimer = 4;
            target.hitEffectTimer = 4;
        }
    }

    public Point getNextPathNode(TerrainGrid terrain) {
        if (path == null || path.isEmpty() || pathIndex >= path.size()) return null;

        if (pathSkipTimer-- <= 0) {
            pathSkipTimer = 10;
            for (int i = path.size() - 1; i >= pathIndex; i--) {
                Point p = path.get(i);
                if (isPathClear(x, y, terrain.cellCenterX(p.x), terrain.cellCenterY(p.y), terrain)) {
                    pathIndex = i; break;
                }
            }
        }

        Point node = path.get(pathIndex);
        if (vectorMath.getDistance(x, y, terrain.cellCenterX(node.x), terrain.cellCenterY(node.y)) < speed * 1.5) pathIndex++;
        return (pathIndex < path.size()) ? path.get(pathIndex) : null;
    }

    public boolean isPathClear(double x1, double y1, double x2, double y2, TerrainGrid terrain) {
        double d = vectorMath.getDistance(x1, y1, x2, y2);
        int steps = (int)(d / (terrain.cellSize / 2.0));
        for (int i = 1; i <= steps; i++) {
            double ratio = (double)i / steps;
            double cx = x1 + (x2 - x1) * ratio;
            double cy = y1 + (y2 - y1) * ratio;
            if (!terrain.isWalkableCell((int)(cx/terrain.cellSize), (int)(cy/terrain.cellSize))) return false;
            if (!terrain.isWalkableCell((int)((cx + size*0.3)/terrain.cellSize), (int)(cy/terrain.cellSize))) return false;
            if (!terrain.isWalkableCell((int)((cx - size*0.3)/terrain.cellSize), (int)(cy/terrain.cellSize))) return false;
        }
        return true;
    }

    public void stop() {
        StopLogic.execute(this);
    }

    public abstract void draw(Graphics g);
    protected abstract void drawAttackEffect(Graphics g, double lookAngle);

    protected void drawHitEffect(Graphics g) {
        if (hitEffectTimer > 0) {
            g.setColor(new Color(255, 255, 150));
            int innerSize = 6;
            int outerSize = 10;
            g.drawOval((int)x - innerSize / 2, (int)y - innerSize / 2, innerSize, innerSize);
            g.drawOval((int)x - outerSize / 2, (int)y - outerSize / 2, outerSize, outerSize);
        }
    }

    protected void drawHealthBar(Graphics g) {
        // [핵심 수정] 선택된(isSelected) 유닛만 체력바와 선택 원을 그리며, 팀(team) 정보를 넘겨 색상을 구분합니다!
        if (this.isSelected) {
            RenderUtils.drawHealthBar(g, x, y, size, hp, maxHp, this.team);
        }
    }
    protected Image loadImage(String path) { try { return javax.imageio.ImageIO.read(getClass().getResource(path)); } catch (Exception e) { return null; } }
}