package starcraft.engine;

import java.awt.*;

public class RenderUtils {
    public static void drawRotatedImage(Graphics g, Image image, double x, double y, int size, double targetX, double targetY) {
        Graphics2D g2d = (Graphics2D) g.create();

        // Rotate 대신 좌/우 반전만 적용
        int drawX = (int) x - size / 2;
        int drawY = (int) y - size / 2;
        boolean faceRight = targetX >= x;

        if (faceRight) {
            g2d.drawImage(image, drawX, drawY, size, size, null);
        } else {
            // Negative width draws a horizontally flipped image.
            g2d.drawImage(image, drawX + size, drawY, -size, size, null);
        }

        g2d.dispose();
    }

    public static void drawHealthBar(Graphics g, double x, double y, int size, int hp, int maxHp, int team) {
        g.setColor(team == 0 ? Color.GREEN : Color.RED);
        int ovalWidth = (int) (size * 0.8);
        int ovalHeight = (int) (size * 0.3);
        g.drawOval((int) x - ovalWidth / 2, (int) y + size / 3, ovalWidth, ovalHeight);

        double hpRatio = (double) hp / maxHp;
        if (hpRatio > 0.7) g.setColor(Color.GREEN);
        else if (hpRatio > 0.3) g.setColor(Color.YELLOW);
        else g.setColor(Color.RED);

        int barWidth = (int) (size * 0.8);
        int barHeight = 2;
        int barY = (int) y + (size / 2) + 5;

        g.fillRect((int) x - barWidth / 2, barY, (int) (barWidth * hpRatio), barHeight);
        g.setColor(Color.GRAY);
        g.drawRect((int) x - barWidth / 2, barY, barWidth, barHeight);
    }
}
