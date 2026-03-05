package starcraft.objects.logic;

import starcraft.objects.Unit;

import java.util.Collections;

public class StopLogic {
    public static void execute(Unit unit) {
        unit.isMoving = false;
        unit.targetX = unit.x;
        unit.targetY = unit.y;
        unit.manualOrder = false;
        unit.path = Collections.emptyList();
        unit.velX = 0;
        unit.velY = 0;
        unit.stuckTimer = 0;
        unit.isBypassing = false;
        unit.bypassSide = 0;
        unit.lastDistToTarget = 99999;
        unit.target = null;
        // [해결] 정지 시 상태를 대기(0)로 변경하여 자동 공격(말뚝딜)을 허용합니다.
        unit.commandState = 0;
    }
}