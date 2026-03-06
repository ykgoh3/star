package starcraft.objects.units;

import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class SCV extends Unit {

    public SCV(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 60;
        this.hp = 60;
        this.damage = 5;
        this.attackDelay = 18;
        this.range = 28;
        this.speed = 2.0;
        this.size = 24;
        this.image = loadImage("/starcraft/res/scv.png");
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            int sparkX = (int) (x + Math.cos(lookAngle) * (size * 0.75));
            int sparkY = (int) (y + Math.sin(lookAngle) * (size * 0.75));

            g.setColor(new Color(255, 230, 140));
            g.fillOval(sparkX - 2, sparkY - 2, 4, 4);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(120, 80, 60, 150));
            g.fillOval((int) x - 10, (int) y - 5, 20, 10);
            return;
        }

        double lookAngle = getLookAngle();

        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
        } else {
            g.setColor(team == 0 ? new Color(180, 180, 90) : new Color(170, 120, 80));
            g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}