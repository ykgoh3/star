// vectorMath.java
package starcraft.engine;

public class vectorMath {
    public static double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    }

    public static double[] getDirection(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dist = getDistance(x1, y1, x2, y2);
        if (dist == 0) return new double[]{0, 0};
        return new double[]{dx / dist, dy / dist};
    }

    // [추가] 두 각도 사이의 최단 차이를 계산하는 도구 (좌우 판단용)
    public static double getAngleDiff(double a1, double a2) {
        double diff = a1 - a2;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return diff;
    }

    public static double[] getSurroundPoint(double targetX, double targetY, double angle, double distance) {
        double px = targetX + Math.cos(angle) * distance;
        double py = targetY + Math.sin(angle) * distance;
        return new double[]{px, py};
    }
}