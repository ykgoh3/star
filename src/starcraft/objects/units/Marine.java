package starcraft.objects.units;

import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Marine extends Unit {

    public Marine(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 40;
        this.hp = 40;
        this.damage = 6;
        this.attackDelay = 15;
        this.range = 140;
        this.speed = 2.2;
        this.size = 20;
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
            RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
        } else {
            g.setColor(team == 0 ? Color.BLUE : Color.RED);
            g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}
