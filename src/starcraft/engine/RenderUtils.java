package starcraft.engine;

import java.awt.*;

public class RenderUtils {
    public static void drawRotatedImage(Graphics g, Image image, double x, double y, int drawWidth, int drawHeight, double targetX, double targetY) {
        Graphics2D g2d = (Graphics2D) g.create();

        int drawX = (int) Math.round(x - drawWidth / 2.0);
        int drawY = (int) Math.round(y - drawHeight / 2.0);
        boolean faceRight = targetX >= x;

        if (faceRight) {
            g2d.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            g2d.drawImage(image, drawX + drawWidth, drawY, -drawWidth, drawHeight, null);
        }

        g2d.dispose();
    }

    public static void drawHealthBar(Graphics g, double x, double y, int drawWidth, int drawHeight, int hp, int maxHp, int team) {
        drawHealthBar(g, x, y, drawWidth, drawHeight, hp, maxHp, team, true);
    }

    public static void drawHealthBar(Graphics g, double x, double y, int drawWidth, int drawHeight, int hp, int maxHp, int team, boolean drawOval) {
        if (maxHp <= 0) return;

        int ovalWidth = (int) Math.round(drawWidth * 0.8);
        int ovalHeight = Math.max(5, (int) Math.round(drawHeight * 0.3));
        int barWidth = ovalWidth;
        int barHeight = drawOval ? Math.max(3, Math.min(5, drawHeight / 9)) : Math.max(2, Math.min(4, drawHeight / 10));
        int barX = (int) Math.round(x - barWidth / 2.0);
        int barY = (int) Math.round(y + drawHeight / 2.0 + (drawOval ? 3 : 5));

        if (drawOval) {
            g.setColor(team == 0 ? Color.GREEN : Color.RED);
            g.drawOval((int) Math.round(x - ovalWidth / 2.0), (int) Math.round(y + drawHeight * 0.2), ovalWidth, ovalHeight);
        }

        double hpRatio = Math.max(0.0, Math.min(1.0, (double) hp / maxHp));
        if (hpRatio > 0.7) g.setColor(Color.GREEN);
        else if (hpRatio > 0.3) g.setColor(Color.YELLOW);
        else g.setColor(Color.RED);

        g.fillRect(barX, barY, (int) Math.round(barWidth * hpRatio), barHeight);
        int hpSegmentTarget = Math.max(6, Math.min(28, maxHp / 50));
        int widthSegmentTarget = Math.max(6, Math.min(28, barWidth / 4));
        int segmentCount = Math.min(hpSegmentTarget, widthSegmentTarget);
        g.setColor(new Color(20, 20, 20, 140));
        for (int segment = 1; segment < segmentCount; segment++) {
            int segmentX = barX + (int) Math.round((barWidth * segment) / (double) segmentCount);
            g.drawLine(segmentX, barY, segmentX, barY + barHeight - 1);
        }

        g.setColor(new Color(40, 40, 40));
        g.drawRect(barX, barY, barWidth, barHeight);
    }
}


