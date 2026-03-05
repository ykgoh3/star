package starcraft.engine;

import java.awt.*;
//유닛 각종 유틸클래스

public class RenderUtils { //유닛 회전
    public static void drawRotatedImage(Graphics g, Image image, double x, double y, int size, double targetX, double targetY) {
        Graphics2D g2d = (Graphics2D) g.create();

        // 1. 정확한 각도 계산 (라디안)
        double rawAngle = Math.atan2(targetY - y, targetX - x);

        // 2. 32방향으로 각도 스냅 (11.25도 단위)
        double snappedAngle = snapAngle(rawAngle, 32);

        g2d.rotate(snappedAngle, x, y);
        g2d.drawImage(image, (int) x - size / 2, (int) y - size / 2, size, size, null);
        g2d.dispose();
    }

    private static double snapAngle(double angle, int directions) {
        // 라디안을 0 ~ 2PI 범위로 정규화
        double twoPi = 2 * Math.PI;
        double normalizedAngle = (angle % twoPi + twoPi) % twoPi;

        // 방향의 크기 (32방향이면 360/32 = 11.25도)
        double step = twoPi / directions;

        // 가장 가까운 방향 인덱스 계산 (반올림)
        long index = Math.round(normalizedAngle / step);

        // 다시 라디안으로 변환
        return index * step;
    }

    public static void drawHealthBar(Graphics g, double x, double y, int size, int hp, int maxHp, int team) {
        // ... 기존 코드 유지
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