package starcraft.objects.units.zerg;

import starcraft.core.GamePanel;
import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;
import java.awt.geom.QuadCurve2D;

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
        this.drawWidth = 21;
        this.drawHeight = 23;
        this.image = loadImage("/starcraft/res/hydralisk.png");
    }

    @Override
    public void attack(GamePanel panel) {
        int prevHp = (target != null) ? target.hp : 0;
        super.attack(panel);

        if (target != null && target.hp < prevHp) {
            target.hitEffectColor = new Color(110, 255, 120);
            target.hitEffectStyle = 1;
            target.hitEffectTimer = 4;
        }
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();

            double sx = x + Math.cos(lookAngle) * (size * 0.45);
            double sy = y + Math.sin(lookAngle) * (size * 0.45);
            double ex = x + Math.cos(lookAngle) * (size * 1.75);
            double ey = y + Math.sin(lookAngle) * (size * 1.75);

            double mx = (sx + ex) * 0.5;
            double my = (sy + ey) * 0.5;
            double nx = -Math.sin(lookAngle);
            double ny = Math.cos(lookAngle);
            double bend = 3.0;

            QuadCurve2D curve = new QuadCurve2D.Double(
                    sx, sy,
                    mx + nx * bend, my + ny * bend,
                    ex, ey
            );

            g2.setColor(new Color(110, 255, 120));
            g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(curve);

            int tipX = (int) ex;
            int tipY = (int) ey;
            int tipX2 = (int) (ex - Math.cos(lookAngle - 0.22) * 4);
            int tipY2 = (int) (ey - Math.sin(lookAngle - 0.22) * 4);
            int tipX3 = (int) (ex - Math.cos(lookAngle + 0.22) * 4);
            int tipY3 = (int) (ey - Math.sin(lookAngle + 0.22) * 4);
            g2.fillPolygon(new int[]{tipX, tipX2, tipX3}, new int[]{tipY, tipY2, tipY3}, 3);

            g2.setStroke(oldStroke);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(120, 40, 40, 150));
            g.fillOval((int) x - 12, (int) y - 6, 24, 12);
            return;
        }

        double lookAngle = getLookAngle();

        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g, image, x, y, getDrawWidth(), getDrawHeight(), lookX, lookY);
        } else {
            g.setColor(team == 0 ? new Color(80, 180, 255) : new Color(180, 255, 120));
            g.fillOval((int) Math.round(x - getDrawWidth() / 2.0), (int) Math.round(y - getDrawHeight() / 2.0), getDrawWidth(), getDrawHeight());
        }

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}
