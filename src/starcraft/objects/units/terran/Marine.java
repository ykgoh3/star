package starcraft.objects.units.terran;

import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Marine extends Unit {

    public Marine(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 40;
        this.hp = 40;
        this.armor = 0;
        this.damage = 6;
        this.attackDelay = 15;
        this.range = 128;
        this.speed = 2.2;
        this.size = 18;
        this.drawWidth = 17;
        this.drawHeight = 20;
        this.image = loadImage("/starcraft/res/marine.png");
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            int flashX = (int) (x + Math.cos(lookAngle) * (size * 0.8));
            int flashY = (int) (y + Math.sin(lookAngle) * (size * 0.8));

            g.setColor(new Color(255, 255, 150));
            g.fillOval(flashX - 2, flashY - 2, 4, 4);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(139, 0, 0, 150));
            g.fillOval((int) x - 10, (int) y - 5, 20, 10);
            return;
        }

        double lookAngle = getLookAngle();

        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g, image, x, y, getDrawWidth(), getDrawHeight(), lookX, lookY);
        } else {
            g.setColor(team == 0 ? Color.BLUE : Color.RED);
            g.fillOval((int) Math.round(x - getDrawWidth() / 2.0), (int) Math.round(y - getDrawHeight() / 2.0), getDrawWidth(), getDrawHeight());
        }

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}



