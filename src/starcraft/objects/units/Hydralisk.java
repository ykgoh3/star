package starcraft.objects.units;

import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Hydralisk extends Unit {

    public Hydralisk(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 80;
        this.hp = 80;
        this.damage = 10;
        this.attackDelay = 18;
        this.range = 130;
        this.speed = 2.8;
        this.size = 22;
        this.image = loadImage("/starcraft/res/hydralisk.png");
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            int spikeX = (int) (x + Math.cos(lookAngle) * (size * 0.9));
            int spikeY = (int) (y + Math.sin(lookAngle) * (size * 0.9));

            g.setColor(new Color(180, 255, 120));
            g.fillOval(spikeX - 2, spikeY - 2, 5, 5);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(120, 40, 40, 150));
            g.fillOval((int) x - 12, (int) y - 6, 24, 12);
            return;
        }

        double lookAngle = isMoving ? currentAngle : (target != null ? Math.atan2(target.y - y, target.x - x) : currentAngle);

        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
        } else {
            g.setColor(team == 0 ? new Color(80, 180, 255) : new Color(180, 255, 120));
            g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
        }

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}
