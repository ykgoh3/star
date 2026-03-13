package starcraft.objects.units.terran;

import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Firebat extends Unit {

    public Firebat(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 50;
        this.hp = 50;
        this.damage = 8;
        this.attackDelay = 18;
        this.range = 45;
        this.speed = 2.1;
        this.size = 19;
        this.drawWidth = 19;
        this.drawHeight = 20;
        this.image = null;
        this.image = loadImage("/starcraft/res/firebat.png");
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            int flameX = (int) (x + Math.cos(lookAngle) * (size * 0.9));
            int flameY = (int) (y + Math.sin(lookAngle) * (size * 0.9));
            g2.setColor(new Color(255, 170, 40, 220));
            g2.fillOval(flameX - 5, flameY - 4, 10, 8);
            g2.setColor(new Color(255, 230, 120, 210));
            g2.fillOval(flameX - 3, flameY - 2, 6, 4);
            g2.dispose();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(139, 0, 0, 150));
            g.fillOval((int) x - 11, (int) y - 5, 22, 10);
            return;
        }

        double lookAngle = getLookAngle();
        int bodyW = Math.max(1, (int) Math.round(getDrawWidth() * 0.9));
        int bodyH = Math.max(1, (int) Math.round(getDrawHeight() *0.95));
        int drawX = (int) Math.round(x - bodyW / 2.0);
        int drawY = (int) Math.round(y - bodyH / 2.0);

        Graphics2D g2 = (Graphics2D) g.create();
        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g2, image, x, y, bodyW, bodyH, lookX, lookY);
        } else {
            g2.setColor(team == 0 ? new Color(190, 90, 40) : new Color(180, 60, 60));
            g2.fillRoundRect(drawX, drawY, bodyW, bodyH, 6, 6);
            g2.setColor(new Color(40, 40, 40));
            g2.drawRoundRect(drawX, drawY, bodyW, bodyH, 6, 6);
            g2.setColor(new Color(255, 180, 70));
            g2.fillRect(drawX + bodyW / 2 - 2, drawY + 3, 4, bodyH - 6);
        }
        g2.dispose();

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}




