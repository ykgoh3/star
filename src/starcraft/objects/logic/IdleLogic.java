package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.util.List;

public class IdleLogic {
    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        unit.velX = 0;
        unit.velY = 0; // 완전 정지

        // 사거리 내 적 감지 시 즉시 공격 (말뚝딜)
        if (unit.target != null && unit.target.hp > 0) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.x, unit.target.y);
            if (dist <= unit.range) {
                if (unit.attackTimer <= 0) unit.attack(null);
            }
        }
        // [해결] resolveActiveOverlap 호출을 삭제하여 찔끔 밀리는 현상 완벽 제거
    }
}