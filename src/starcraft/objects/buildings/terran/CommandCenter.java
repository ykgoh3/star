package starcraft.objects.buildings.terran;

import starcraft.objects.Unit;
import starcraft.objects.buildings.UnitFactoryBuilding;
import starcraft.objects.units.terran.SCV;

import java.awt.*;

public class CommandCenter extends UnitFactoryBuilding {
    public static final int BUILD_WIDTH = 117;
    public static final int BUILD_HEIGHT = 80;
    public static final String QUEUE_SCV = "scv";

    public CommandCenter(int x, int y, int team) {
        super(x, y, team, BUILD_WIDTH, BUILD_HEIGHT, 1500, 60, 20, 26);
        this.image = loadImage("/starcraft/res/command_center.png");
    }

    @Override
    public int getPathingBlockWidth() {
        return Math.max(20, width - 56);
    }

    @Override
    public int getPathingBlockHeight() {
        return Math.max(20, height - 32);
    }

    public void enqueueWorker() {
        enqueueUnit(QUEUE_SCV);
    }

    @Override
    protected Unit createUnit(String unitTypeId, int x, int y, int team) {
        return new SCV(x, y, team);
    }

    @Override
    public void draw(Graphics g) {
        int drawX = (int) (this.x - width / 2.0);
        int drawY = (int) (this.y - height / 2.0);

        g.setColor(new Color(0, 0, 0, 100));
        g.fillRect(drawX + 4, drawY + 6, width, height);

        if (image != null) {
            g.drawImage(image, drawX, drawY, width, height, null);
        } else {
            g.setColor(team == 0 ? new Color(90, 130, 220) : new Color(170, 90, 90));
            g.fillRect(drawX, drawY, width, height);
            g.setColor(new Color(40, 40, 40));
            g.drawRect(drawX, drawY, width, height);

            g.setColor(new Color(220, 220, 220));
            g.drawString("CC", drawX + width / 2 - 8, drawY + 20);
        }
    }
}
