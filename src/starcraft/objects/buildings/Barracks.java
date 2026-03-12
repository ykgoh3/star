package starcraft.objects.buildings;

import starcraft.objects.Unit;
import starcraft.objects.units.Marine;

import java.awt.*;

public class Barracks extends UnitFactoryBuilding {

    public Barracks(int x, int y, int team) {
        super(x, y, team, 105, 73, 1000, 30, 20, 24);
        this.image = loadImage("/starcraft/res/barracks.png");
    }

    public void enqueueMarine() {
        enqueueUnit();
    }

    @Override
    protected Unit createUnit(int x, int y, int team) {
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

        int progressWidth = (int) ((width - 8) * getProductionProgress());
        g.setColor(new Color(40, 40, 40));
        g.fillRect(drawX + 4, drawY + height - 8, width - 8, 4);
        g.setColor(new Color(90, 230, 120));
        g.fillRect(drawX + 4, drawY + height - 8, Math.max(0, progressWidth), 4);
    }
}