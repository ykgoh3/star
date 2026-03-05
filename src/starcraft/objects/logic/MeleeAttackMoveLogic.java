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
                    // [해결 1] 이미 가까이 붙어있더라도, 서로 '멀어지는 방향'의 이동은 허용하여 엉킴을 풀어줍니다.
                    if (nextDist < currentDist) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        if (unit.target != null && unit.target.hp > 0) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.x, unit.target.y);

            // 1. 공격 사거리 도달 시 즉시 타격
            if (dist <= unit.range) {
                unit.velX = 0; unit.velY = 0;
                if (unit.attackTimer <= 0) unit.attack(null);
                return;
            }

            // 2. 사거리 밖이면 '개떼 포위(Swarm)' 기동 실시
            double targetAng = Math.atan2(unit.target.y - unit.y, unit.target.x - unit.x);

            if (unit.bypassDuration <= 0) {
                double[] findHoleAngles = {0.52, -0.52, 1.04, -1.04, 1.57, -1.57, 2.09, -2.09, 2.61, -2.61};
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
            if (unit.bypassSide == 1) {
                scanAngles = new double[]{0, 0.52, 1.04, 1.57, 2.09, 2.61, -0.52, -1.04, -1.57, -2.09, -2.61};
            } else {
                scanAngles = new double[]{0, -0.52, -1.04, -1.57, -2.09, -2.61, 0.52, 1.04, 1.57, 2.09, 2.61};
            }

            boolean moved = false;

            // [1차 시도] 아군과 동선이 겹치지 않는 '쾌적한 산개 길'을 우선적으로 찾습니다.
            for (double angOff : scanAngles) {
                double testAng = targetAng + angOff;
                double nX = unit.x + Math.cos(testAng) * unit.speed;
                double nY = unit.y + Math.sin(testAng) * unit.speed;

                if (checkSpace(unit, nX, nY, allUnits, terrain)) {
                    unit.x = nX; unit.y = nY;
                    unit.velX = Math.cos(testAng); unit.velY = Math.sin(testAng);
                    unit.resolveActiveOverlap(allUnits, terrain);
                    moved = true;
                    if (Math.abs(angOff) > 0.1) unit.bypassDuration = 25;
                    else unit.bypassDuration = 0;
                    break;
                }
            }

            // [핵심 2차 시도] 너무 빽빽하게 뭉쳐있어서 1차 시도가 실패했다면?
            // 쾌적함(0.6 반경)을 포기하고 물리적 충돌(canMoveSolid)만 피해서 일단 액체처럼 비집고 돌진합니다!
            if (!moved) {
                for (double angOff : scanAngles) {
                    double testAng = targetAng + angOff;
                    double nX = unit.x + Math.cos(testAng) * unit.speed;
                    double nY = unit.y + Math.sin(testAng) * unit.speed;

                    // checkSpace 대신 canMoveSolid 만 사용
                    if (unit.canMoveSolid(nX, nY, allUnits, terrain)) {
                        unit.x = nX; unit.y = nY;
                        unit.velX = Math.cos(testAng); unit.velY = Math.sin(testAng);
                        unit.resolveActiveOverlap(allUnits, terrain);
                        moved = true;
                        if (Math.abs(angOff) > 0.1) unit.bypassDuration = 10;
                        else unit.bypassDuration = 0;
                        break;
                    }
                }
            }

            // 그래도 벽이나 정지한 아군에게 완전히 막혔다면 밀어내기 연산만으로 틈을 만듦
            if (!moved) {
                unit.velX = 0; unit.velY = 0;
                unit.resolveActiveOverlap(allUnits, terrain);
            }

            // 3. 목적지 갱신
            unit.targetX = unit.target.x;
            unit.targetY = unit.target.y;

        } else {
            // 타겟이 없으면 일반 원거리 유닛과 동일하게 어택땅 목적지로 이동
            unit.targetX = unit.destX;
            unit.targetY = unit.destY;
            MoveLogic.execute(unit, allUnits, terrain);
        }
    }
}