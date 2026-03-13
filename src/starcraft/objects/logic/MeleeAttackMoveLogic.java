package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.util.List;

public class MeleeAttackMoveLogic {

    private static boolean checkSpace(Unit unit, double tx, double ty, List<Unit> allUnits, TerrainGrid terrain) {
        if (!unit.canMoveSolid(tx, ty, allUnits, terrain)) return false;

        for (Unit other : allUnits) {
            if (other == unit || other.hp <= 0 || other.team != unit.team) continue;

            if (other.isMoving && other.attackTimer <= 0) {
                double nextDist = vectorMath.getDistance(tx, ty, other.x, other.y);
                if (nextDist < (unit.size + other.size) * 0.6) {
                    double currentDist = vectorMath.getDistance(unit.x, unit.y, other.x, other.y);
                    if (nextDist < currentDist) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        if (unit.target != null && unit.target.isAlive()) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.getTargetX(), unit.target.getTargetY());
            double attackRange = unit.getAttackRangeAgainst(unit.target);
            double nearStopRange = attackRange + Math.max(2.0, unit.speed * 0.75);

            if (dist <= nearStopRange) {
                unit.velX = 0;
                unit.velY = 0;
                return;
            }

            double targetAng = Math.atan2(unit.target.getTargetY() - unit.y, unit.target.getTargetX() - unit.x);
            boolean closeToTarget = dist <= attackRange + Math.max(10.0, unit.speed * 3.0);

            if (unit.bypassDuration <= 0) {
                double[] findHoleAngles = closeToTarget
                        ? new double[]{0.35, -0.35, 0.7, -0.7, 1.04, -1.04}
                        : new double[]{0.52, -0.52, 1.04, -1.04, 1.57, -1.57, 2.09, -2.09, 2.61, -2.61};
                unit.bypassSide = 1;
                for (double ang : findHoleAngles) {
                    double tx = unit.x + Math.cos(targetAng + ang) * unit.speed;
                    double ty = unit.y + Math.sin(targetAng + ang) * unit.speed;
                    if (checkSpace(unit, tx, ty, allUnits, terrain)) {
                        unit.bypassSide = (ang > 0) ? 1 : -1;
                        break;
                    }
                }
            } else {
                unit.bypassDuration--;
            }

            double[] scanAngles;
            if (closeToTarget) {
                scanAngles = (unit.bypassSide == 1)
                        ? new double[]{0, 0.18, -0.18, 0.35, -0.35, 0.52, -0.52, 0.7, -0.7}
                        : new double[]{0, -0.18, 0.18, -0.35, 0.35, -0.52, 0.52, -0.7, 0.7};
            } else if (unit.bypassSide == 1) {
                scanAngles = new double[]{0, 0.52, 1.04, 1.57, 2.09, 2.61, -0.52, -1.04, -1.57, -2.09, -2.61};
            } else {
                scanAngles = new double[]{0, -0.52, -1.04, -1.57, -2.09, -2.61, 0.52, 1.04, 1.57, 2.09, 2.61};
            }

            boolean moved = false;

            for (double angOff : scanAngles) {
                double testAng = targetAng + angOff;
                double nX = unit.x + Math.cos(testAng) * unit.speed;
                double nY = unit.y + Math.sin(testAng) * unit.speed;

                if (checkSpace(unit, nX, nY, allUnits, terrain)) {
                    unit.x = nX;
                    unit.y = nY;
                    unit.velX = Math.cos(testAng);
                    unit.velY = Math.sin(testAng);
                    unit.resolveActiveOverlap(allUnits, terrain);
                    moved = true;
                    unit.bypassDuration = (Math.abs(angOff) > 0.1) ? (closeToTarget ? 8 : 25) : 0;
                    break;
                }
            }

            if (!moved) {
                for (double angOff : scanAngles) {
                    double testAng = targetAng + angOff;
                    double nX = unit.x + Math.cos(testAng) * unit.speed;
                    double nY = unit.y + Math.sin(testAng) * unit.speed;

                    if (unit.canMoveSolid(nX, nY, allUnits, terrain)) {
                        unit.x = nX;
                        unit.y = nY;
                        unit.velX = Math.cos(testAng);
                        unit.velY = Math.sin(testAng);
                        unit.resolveActiveOverlap(allUnits, terrain);
                        moved = true;
                        unit.bypassDuration = (Math.abs(angOff) > 0.1) ? (closeToTarget ? 4 : 10) : 0;
                        break;
                    }
                }
            }

            if (!moved) {
                unit.velX = 0;
                unit.velY = 0;
                unit.resolveActiveOverlap(allUnits, terrain);
            }

            unit.targetX = unit.target.getTargetX();
            unit.targetY = unit.target.getTargetY();

        } else {
            if (unit.autoRetaliating) {
                unit.stop();
                return;
            }
            unit.targetX = unit.destX;
            unit.targetY = unit.destY;
            MoveLogic.execute(unit, allUnits, terrain);
        }
    }
}
