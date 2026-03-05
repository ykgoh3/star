package starcraft.objects.logic;

import starcraft.core.TerrainGrid;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;

import java.util.List;

public class IdleLogic {
    public static void execute(Unit unit, List<Unit> allUnits, TerrainGrid terrain) {
        unit.velX = 0;
        unit.velY = 0; // ?熬곣뫗???筌?

        // ???대겳逾??????띠룆흮? ??嶺뚯빖留????ㅻ???(嶺뚮씭큔???
        if (unit.target != null && unit.target.hp > 0) {
            double dist = vectorMath.getDistance(unit.x, unit.y, unit.target.x, unit.target.y);
            if (dist <= unit.range) {
            }
        }
        // [???㏉뜖] resolveActiveOverlap ?筌뤾쑵????????琉우뿰 嶺뚣볥?椰??꾩럾??洹먮맧裕??熬곣뫕留??熬곣몿????蹂ㅽ깴
    }
}