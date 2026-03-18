package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class MoveLogic {

    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        double currentDist = vectorMath.getDistance(unit.x, unit.y, unit.targetX, unit.targetY);
        boolean ignoreUnitCollision = unit.ignoresUnitCollision();

        if (currentDist < unit.size * 3.0) {
            if (currentDist < unit.speed * 0.5) {
                StopLogic.execute(unit);
                return;
            }

            if (!ignoreUnitCollision) {
                for (Unit other : allUnits) {
                    if (other == unit || other.hp <= 0 || other.isMoving) continue;

                    double distToOther = vectorMath.getDistance(unit.x, unit.y, other.x, other.y);
                    if (distToOther < (unit.size + other.size) * 0.5) {
                        double otherDistToTarget = vectorMath.getDistance(other.x, other.y, unit.targetX, unit.targetY);
                        if (otherDistToTarget < currentDist) {
                            StopLogic.execute(unit);
                            return;
                        }
                    }
                }
            }

            double targetAng = Math.atan2(unit.targetY - unit.y, unit.targetX - unit.x);
            double[] scanAngles = {0, Math.PI / 6, -Math.PI / 6, Math.PI / 3, -Math.PI / 3};

            boolean moved = false;
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
                    break;
                }
            }

            if (!moved) {
                StopLogic.execute(unit);
            }
            return;
        }

        double actualMoved = vectorMath.getDistance(unit.x, unit.y, unit.lastX, unit.lastY);
        if (unit.isMoving && actualMoved < unit.speed * 0.1) unit.stagnationTimer++;
        else unit.stagnationTimer = Math.max(0, unit.stagnationTimer - 2);

        if (unit.isMoving && actualMoved < unit.speed * 0.2) unit.stuckTimer += 5;
        else unit.stuckTimer = Math.max(0, unit.stuckTimer - 1);

        unit.lastX = unit.x;
        unit.lastY = unit.y;
        unit.lastDistToTarget = currentDist;

        if (unit.stagnationTimer >= 40 || (unit.stuckTimer > 6 && !unit.isBypassing)) {
            if (!unit.isBypassing) {
                unit.isBypassing = true;
                unit.bypassDuration = 60;
            }
            unit.bypassSide = (unit.stagnationTimer >= 40) ? (unit.bypassSide == -1 ? 1 : -1) : unit.bypassSide;
            unit.stuckTimer = 0;
            unit.stagnationTimer = 0;
            unit.sideRecheckTimer = 45;
            unit.path = Collections.emptyList();
        }

        if (unit.bypassDuration > 0) unit.bypassDuration--;
        else {
            unit.isBypassing = false;
            unit.bypassSide = 0;
        }

        Point node = unit.getNextPathNode(terrain);
        double targetAng = (node == null)
                ? Math.atan2(unit.targetY - unit.y, unit.targetX - unit.x)
                : Math.atan2(terrain.cellCenterY(node.y) - unit.y, terrain.cellCenterX(node.x) - unit.x);

        double mX = Math.cos(targetAng);
        double mY = Math.sin(targetAng);

        if (unit.isBypassing) {
            unit.handleBypassDirection(mX, mY, allUnits, terrain);
            double finalAngle = Math.atan2(mY, mX) + (unit.bypassSide * Math.PI / 3.0);
            mX = Math.cos(finalAngle);
            mY = Math.sin(finalAngle);
        }

        unit.velX = unit.velX * 0.2 + mX * 0.8;
        unit.velY = unit.velY * 0.2 + mY * 0.8;
        double mag = Math.sqrt(unit.velX * unit.velX + unit.velY * unit.velY);
        if (mag > 0.01) {
            unit.velX /= mag;
            unit.velY /= mag;
        }

        double nX = unit.x + unit.velX * unit.speed;
        double nY = unit.y + unit.velY * unit.speed;

        if (unit.canMoveSolid(nX, nY, allUnits, terrain)) {
            unit.x = nX;
            unit.y = nY;
            unit.resolveActiveOverlap(allUnits, terrain);
        } else {
            double baseAng = Math.atan2(unit.velY, unit.velX);
            double s = (unit.bypassSide != 0) ? unit.bypassSide : 1;
            double[] scanAngles = {s * 0.52, s * 1.04, -s * 0.52, -s * 1.04};

            for (double scanAng : scanAngles) {
                double tx = unit.x + Math.cos(baseAng + scanAng) * unit.speed;
                double ty = unit.y + Math.sin(baseAng + scanAng) * unit.speed;
                if (unit.canMoveSolid(tx, ty, allUnits, terrain)) {
                    unit.x = tx;
                    unit.y = ty;
                    unit.velX = Math.cos(baseAng + scanAng);
                    unit.velY = Math.sin(baseAng + scanAng);
                    unit.resolveActiveOverlap(allUnits, terrain);
                    break;
                }
            }
        }
        unit.currentAngle = Math.atan2(unit.velY, unit.velX);
    }
}

