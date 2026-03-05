package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.util.List;

public class AttackMoveLogic {
    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        if (unit.target != null && unit.target.hp > 0) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.x, unit.target.y);

            if (dist <= unit.range) {
                // [해결] 사거리 안이면 이동 벡터를 즉시 0으로 만들고 사격 (밀어내기 안함)
                unit.velX = 0; unit.velY = 0;
                if (unit.attackTimer <= 0) unit.attack(null);
                return;
            } else if (dist <= unit.range + 150) {
                // 사거리보다 조금 멀면 적을 추적
                unit.targetX = unit.target.x;
                unit.targetY = unit.target.y;
            } else {
                // 적이 너무 멀어지면 포기하고 원래 가던 길 감
                unit.target = null;
                unit.targetX = unit.destX;
                unit.targetY = unit.destY;
            }
        } else {
            // 타겟이 없으면 최종 목적지로 이동
            unit.targetX = unit.destX;
            unit.targetY = unit.destY;
        }

        // 전투 중이 아닐 때만 실제 이동 로직 수행
        MoveLogic.execute(unit, allUnits, terrain);
    }
}