package starcraft.objects.buildings.terran;

import starcraft.objects.Unit;
import starcraft.objects.buildings.UnitFactoryBuilding;
import starcraft.objects.units.terran.Firebat;
import starcraft.objects.units.terran.Marine;

import java.awt.*;

public class Barracks extends UnitFactoryBuilding {
    public static final int BUILD_WIDTH = 100;
    public static final int BUILD_HEIGHT = 80;
    public static final String QUEUE_MARINE = "marine";
    public static final String QUEUE_FIREBAT = "firebat";

    public Barracks(int x, int y, int team) {
        super(x, y, team, BUILD_WIDTH, BUILD_HEIGHT, 1000, 30, 20, 24);
        this.image = loadImage("/starcraft/res/barracks.png");
    }

    public void enqueueMarine() {
        enqueueUnit(QUEUE_MARINE);
    }

    public void enqueueFirebat() {
        enqueueUnit(QUEUE_FIREBAT);
    }

    @Override
    protected Unit createUnit(String unitTypeId, int x, int y, int team) {
        if (QUEUE_FIREBAT.equals(unitTypeId)) {
            return new Firebat(x, y, team);
        }
        return new Marine(x, y, team);
    }

    @Override
    public void draw(Graphics g) {
        int drawX = (int) (this.x - width / 2.0);
        int drawY = (int) (this.y - height / 2.0);

        g.setColor(new Color(0, 0, 0, 90));
        g.fillRect(drawX + 3, drawY + 4, width, height);

        if (image != null) {
            g.drawImage(image, drawX, drawY, width, height, null);
        } else {
            g.setColor(team == 0 ? new Color(70, 110, 230) : new Color(170, 70, 70));
            g.fillRect(drawX, drawY, width, height);

            g.setColor(new Color(40, 40, 40));
            g.drawRect(drawX, drawY, width, height);

            g.setColor(new Color(220, 220, 220));
            g.drawLine(drawX + 10, drawY + 8, drawX + width - 10, drawY + 8);
            g.drawLine(drawX + 10, drawY + 16, drawX + width - 10, drawY + 16);
        }
    }
}
