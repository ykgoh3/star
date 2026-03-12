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
        g.setColor(team == 0 ? Color.GREEN : Color.RED);
        int ovalWidth = (int) Math.round(drawWidth * 0.8);
        int ovalHeight = Math.max(5, (int) Math.round(drawHeight * 0.3));
        g.drawOval((int) Math.round(x - ovalWidth / 2.0), (int) Math.round(y + drawHeight * 0.2), ovalWidth, ovalHeight);

        double hpRatio = (double) hp / maxHp;
        if (hpRatio > 0.7) g.setColor(Color.GREEN);
        else if (hpRatio > 0.3) g.setColor(Color.YELLOW);
        else g.setColor(Color.RED);

        int barWidth = ovalWidth;
        int barHeight = 2;
        int barY = (int) Math.round(y + drawHeight / 2.0 + 5);

        g.fillRect((int) Math.round(x - barWidth / 2.0), barY, (int) Math.round(barWidth * hpRatio), barHeight);
        g.setColor(Color.GRAY);
        g.drawRect((int) Math.round(x - barWidth / 2.0), barY, barWidth, barHeight);
    }
}