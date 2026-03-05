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
                unit.velX = 0;
                unit.velY = 0;
                return;
            } else if (dist <= unit.range + 150) {
                unit.targetX = unit.target.x;
                unit.targetY = unit.target.y;
            } else {
                unit.target = null;
                if (unit.autoRetaliating) {
                    unit.stop();
                    return;
                }
                unit.targetX = unit.destX;
                unit.targetY = unit.destY;
            }
        } else {
            if (unit.autoRetaliating) {
                unit.stop();
                return;
            }
            unit.targetX = unit.destX;
            unit.targetY = unit.destY;
        }

        MoveLogic.execute(unit, allUnits, terrain);
    }
}
