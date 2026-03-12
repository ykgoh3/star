package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.util.List;

public class IdleLogic {
    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        unit.velX = 0;
        unit.velY = 0; // ??ш끽維???嶺?

        // ????寃녜???????좊즴?? ??癲ル슣鍮뽳쭕????????(癲ル슢??걫???
        if (unit.target != null && unit.target.isAlive()) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.getTargetX(), unit.target.getTargetY());
            if (dist <= unit.range) {
            }
        }
        // [????됰쐳] resolveActiveOverlap ?嶺뚮ㅎ?????????筌뚯슦肉?癲ル슔?蹂?濾??袁⑸읈??域밸Ŧ留㎬짆???ш끽維뺧쭕???ш끽紐????癰귙끋源?
    }
}