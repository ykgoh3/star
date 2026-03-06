package starcraft.objects.units;

import starcraft.core.GamePanel;
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
    public void attack(GamePanel panel) {
        int prevHp = (target != null) ? target.hp : 0;
        super.attack(panel);

        if (target != null && target.hp < prevHp) {
            target.hitEffectColor = new Color(255, 230, 150);
            target.hitEffectStyle = 2;
            target.hitEffectTimer = 4;
        }
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        // SCV has no separate projectile/attack draw effect.
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            // One-shot death burst only. No persistent blood stain.
            if (deathTimer > 292) {
                int t = deathTimer - 292;
                int radius = 18 - t;
                g.setColor(new Color(255, 200, 90, 180));
                g.fillOval((int) x - radius / 2, (int) y - radius / 2, radius, radius);
                g.setColor(new Color(255, 140, 60, 200));
                g.fillOval((int) x - radius / 3, (int) y - radius / 3, radius / 2, radius / 2);
            }
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

        drawHitEffect(g);
        drawHealthBar(g);
    }
}