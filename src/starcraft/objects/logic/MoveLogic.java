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

        // 1. 타겟 중심 원형 밀집 및 '빠른 정착(Fast Parking)' 로직 [cite: 2026-02-28]
        if (currentDist < unit.size * 3.0) {
            if (currentDist < unit.speed * 0.5) { StopLogic.execute(unit); return; }

            // [핵심 추가] 내 앞에 이미 주차된 아군이 있다면, 더 비비지 않고 그 뒤에 즉시 멈춥니다!
            for (Unit other : allUnits) {
                if (other == unit || other.hp <= 0 || other.isMoving) continue;

                double distToOther = vectorMath.getDistance(unit.x, unit.y, other.x, other.y);
                // 이미 정지한 아군과 부딪혔을 때
                if (distToOther < (unit.size + other.size) * 0.5) {
                    double otherDistToTarget = vectorMath.getDistance(other.x, other.y, unit.targetX, unit.targetY);
                    // 그 아군이 나보다 타겟에 더 가깝다면 (즉, 대열의 앞줄이라면) 즉시 주차 완료! [cite: 2026-02-28]
                    if (otherDistToTarget < currentDist) {
                        StopLogic.execute(unit);
                        return;
                    }
                }
            }

            double targetAng = Math.atan2(unit.targetY - unit.y, unit.targetX - unit.x);

            // [해결] 파고드는 각도를 90도에서 60도(PI/3)로 줄여, 무리한 진입을 포기하고 빨리 멈추게 유도 [cite: 2026-02-28]
            double[] scanAngles = {0, Math.PI/6, -Math.PI/6, Math.PI/3, -Math.PI/3};

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

            // 60도 범위 내에 들어갈 틈이 없으면 깔끔하게 정지 (대열 완성 속도 증가)
            if (!moved) {
                StopLogic.execute(unit);
            }
            return;
        }

        // --- 2. 장거리 이동 및 최단 시간 우회 로직 (기능 100% 유지) --- [cite: 2026-02-28]
        double actualMoved = vectorMath.getDistance(unit.x, unit.y, unit.lastX, unit.lastY);
        if (unit.isMoving && actualMoved < unit.speed * 0.1) unit.stagnationTimer++;
        else unit.stagnationTimer = Math.max(0, unit.stagnationTimer - 2);

        if (unit.isMoving && actualMoved < unit.speed * 0.2) unit.stuckTimer += 5;
        else unit.stuckTimer = Math.max(0, unit.stuckTimer - 1);

        unit.lastX = unit.x; unit.lastY = unit.y; unit.lastDistToTarget = currentDist;

        if (unit.stagnationTimer >= 40 || (unit.stuckTimer > 6 && !unit.isBypassing)) {
            if (!unit.isBypassing) { unit.isBypassing = true; unit.bypassDuration = 60; }
            unit.bypassSide = (unit.stagnationTimer >= 40) ? (unit.bypassSide == -1 ? 1 : -1) : unit.bypassSide;
            unit.stuckTimer = 0; unit.stagnationTimer = 0; unit.sideRecheckTimer = 45;
            unit.path = Collections.emptyList();
        }

        if (unit.bypassDuration > 0) unit.bypassDuration--;
        else { unit.isBypassing = false; unit.bypassSide = 0; }

        Point node = unit.getNextPathNode(terrain);
        double targetAng = (node == null)
                ? Math.atan2(unit.targetY - unit.y, unit.targetX - unit.x)
                : Math.atan2(terrain.cellCenterY(node.x) - unit.y, terrain.cellCenterX(node.x) - unit.x);

        double mX = Math.cos(targetAng), mY = Math.sin(targetAng);

        if (unit.isBypassing) {
            unit.handleBypassDirection(mX, mY, allUnits, terrain);
            double finalAngle = Math.atan2(mY, mX) + (unit.bypassSide * Math.PI / 3.0);
            mX = Math.cos(finalAngle); mY = Math.sin(finalAngle);
        }

        unit.velX = unit.velX * 0.2 + mX * 0.8;
        unit.velY = unit.velY * 0.2 + mY * 0.8;
        double mag = Math.sqrt(unit.velX * unit.velX + unit.velY * unit.velY);
        if (mag > 0.01) { unit.velX /= mag; unit.velY /= mag; }

        double nX = unit.x + unit.velX * unit.speed;
        double nY = unit.y + unit.velY * unit.speed;

        if (unit.canMoveSolid(nX, nY, allUnits, terrain)) {
            unit.x = nX; unit.y = nY;
            unit.resolveActiveOverlap(allUnits, terrain);
        } else {
            double baseAng = Math.atan2(unit.velY, unit.velX);
            double s = (unit.bypassSide != 0) ? unit.bypassSide : 1;
            double[] scanAngles = {s * 0.52, s * 1.04, -s * 0.52, -s * 1.04};

            for (double scanAng : scanAngles) {
                double tx = unit.x + Math.cos(baseAng + scanAng) * unit.speed;
                double ty = unit.y + Math.sin(baseAng + scanAng) * unit.speed;
                if (unit.canMoveSolid(tx, ty, allUnits, terrain)) {
                    unit.x = tx; unit.y = ty;
                    unit.velX = Math.cos(baseAng + scanAng); unit.velY = Math.sin(baseAng + scanAng);

                    // [해결] 빈 공간을 비집고 들어갈 때도 겹침 해소를 호출해 유닛 간 겹침 방지
                    unit.resolveActiveOverlap(allUnits, terrain);
                    break;
                }
            }
        }
        unit.currentAngle = Math.atan2(unit.velY, unit.velX);
    }
}