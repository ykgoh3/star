package starcraft.objects.units;

import starcraft.core.GamePanel;
import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Zergling extends Unit {

    public Zergling(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 35;
        this.hp = 35;
        this.damage = 5;
        this.attackDelay = 15;
        this.range = 30;
        this.speed = 3.5;
        this.size = 18;
        this.image = loadImage("/starcraft/res/zergling.png");
    }

    @Override
    public void attack(GamePanel panel) {
        int prevHp = (target != null) ? target.hp : 0;
        super.attack(panel);

        if (target != null && target.hp < prevHp) {
            target.hitEffectTimer = 0;
        }
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            double pushOffset = (attackEffectTimer > 2) ? 4.0 : 0.0;

            int drawX = (int) (x + Math.cos(lookAngle) * pushOffset);
            int drawY = (int) (y + Math.sin(lookAngle) * pushOffset);

            if (image != null) {
                double lookX = drawX + Math.cos(lookAngle);
                double lookY = drawY + Math.sin(lookAngle);
                RenderUtils.drawRotatedImage(g, image, drawX, drawY, size, lookX, lookY);
            } else {
                g.setColor(team == 0 ? Color.ORANGE : Color.MAGENTA);
                g.fillOval(drawX - size / 2, drawY - size / 2, size, size);
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(205, 50, 50, 150));
            g.fillOval((int) x - 15, (int) y - 8, 25, 10);
            g.fillOval((int) x - 5, (int) y - 12, 15, 5);
            return;
        }

        double lookAngle = getLookAngle();

        if (attackEffectTimer <= 0) {
            if (image != null) {
                double lookX = x + Math.cos(lookAngle);
                double lookY = y + Math.sin(lookAngle);
                RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
            } else {
                g.setColor(team == 0 ? Color.ORANGE : Color.MAGENTA);
                g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
            }
        }

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}
