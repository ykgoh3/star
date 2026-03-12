package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.util.List;

public class AttackMoveLogic {
    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        if (unit.target != null && unit.target.isAlive()) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.getTargetX(), unit.target.getTargetY());

            if (dist <= unit.range) {
                unit.velX = 0;
                unit.velY = 0;
                return;
            } else {
                // Keep chasing a live target regardless of leash distance.
                unit.targetX = unit.target.getTargetX();
                unit.targetY = unit.target.getTargetY();
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
