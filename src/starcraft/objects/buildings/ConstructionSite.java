package starcraft.objects.buildings;

import starcraft.core.GamePanel;
import starcraft.objects.buildings.terran.Barracks;
import starcraft.objects.buildings.terran.CommandCenter;
import starcraft.objects.units.terran.SCV;

import java.awt.*;
import java.awt.geom.Point2D;

public class ConstructionSite extends Building {
    public enum Type {
        COMMAND_CENTER,
        BARRACKS
    }

    public enum ConstructionSize {
        LARGE,
        SMALL
    }

    private static final Image LARGE_STAGE_1_IMAGE = loadSharedImage("/starcraft/res/terran_construction_large_1.png");
    private static final Image LARGE_STAGE_2_IMAGE = loadSharedImage("/starcraft/res/terran_construction_large_2.png");
    private static final Image LARGE_STAGE_3_IMAGE = loadSharedImage("/starcraft/res/terran_construction_large_3.png");
    private static final Image SMALL_STAGE_1_IMAGE = loadSharedImage("/starcraft/res/terran_construction_small_1.png");
    private static final Image SMALL_STAGE_2_IMAGE = loadSharedImage("/starcraft/res/terran_construction_small_2.png");
    private static final Image SMALL_STAGE_3_IMAGE = loadSharedImage("/starcraft/res/terran_construction_small_3.png");

    private final Type type;
    private final ConstructionSize constructionSize;
    private final int buildTicksTotal;
    private int buildTicks;
    private SCV builder;
    private int buildPointIndex = 0;
    private int hitEffectTimer = 0;
    private Color hitEffectColor = new Color(255, 220, 140);

    public ConstructionSite(double x, double y, int team, Type type) {
        super(x, y, team, getBuildWidth(type), getBuildHeight(type), getBuildMaxHp(type));
        this.type = type;
        this.constructionSize = getConstructionSize(type);
        this.buildTicksTotal = getBuildTicks(type);
        this.hp = Math.max(1, maxHp / 5);
        this.image = loadImage(getFinalImagePath(type));
    }

    public Type getType() {
        return type;
    }

    public void setBuilder(SCV builder) {
        this.builder = builder;
    }

    public SCV getBuilder() {
        return builder;
    }

    public int getBuildTicksTotal() {
        return buildTicksTotal;
    }

    public int getBuildTicks() {
        return buildTicks;
    }

    public double getBuildProgress() {
        if (buildTicksTotal <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0, (double) buildTicks / buildTicksTotal));
    }

    public int getBuildStage() {
        double progress = getBuildProgress();
        if (progress >= 1.0) return 4;
        if (progress >= 0.66) return 3;
        if (progress >= 0.33) return 2;
        return 1;
    }

    public void advanceConstruction(int ticks) {
        if (ticks <= 0) return;
        buildTicks = Math.min(buildTicksTotal, buildTicks + ticks);
        hp = Math.max(1, (int) Math.round(maxHp * Math.max(0.2, getBuildProgress())));
    }

    public boolean isConstructionComplete() {
        return buildTicks >= buildTicksTotal;
    }

    public Point2D.Double getCurrentBuildPoint() {
        Point2D.Double[] points = getBuildPoints();
        return points[buildPointIndex % points.length];
    }

    public Point2D.Double advanceBuildPoint() {
        Point2D.Double[] points = getBuildPoints();
        buildPointIndex = (buildPointIndex + 1) % points.length;
        return points[buildPointIndex];
    }

    public void setBuildPointIndex(int index) {
        Point2D.Double[] points = getBuildPoints();
        buildPointIndex = Math.floorMod(index, points.length);
    }

    private Point2D.Double[] getBuildPoints() {
        double halfW = width / 2.0;
        double halfH = height / 2.0;
        double margin = 10.0;
        return new Point2D.Double[]{
                new Point2D.Double(x - halfW - margin, y - halfH * 0.55),
                new Point2D.Double(x + halfW + margin, y - halfH * 0.25),
                new Point2D.Double(x + halfW * 0.35, y + halfH + margin),
                new Point2D.Double(x - halfW - margin, y + halfH * 0.35)
        };
    }

    public void triggerHitEffect(Color color, int style, int duration) {
        this.hitEffectColor = (color != null) ? color : new Color(255, 220, 140);
        this.hitEffectTimer = Math.max(1, duration);
    }

    public Building createCompletedBuilding() {
        return switch (type) {
            case COMMAND_CENTER -> new CommandCenter((int) Math.round(x), (int) Math.round(y), team);
            case BARRACKS -> new Barracks((int) Math.round(x), (int) Math.round(y), team);
        };
    }

    public String getDisplayName() {
        return switch (type) {
            case COMMAND_CENTER -> "Command Center";
            case BARRACKS -> "Barracks";
        };
    }

    @Override
    public void update(GamePanel panel) {
        if (panel == null) return;
        if (builder != null && builder.hp > 0 && builder.isConstructing(this)) {
            advanceConstruction(1);
        }
        if (isConstructionComplete()) {
            panel.finishConstruction(this, createCompletedBuilding());
        }
    }

    private double getStageImageScale() {
        return switch (getBuildStage()) {
            case 1 -> 0.58;
            case 2 -> 0.74;
            case 3 -> 0.92;
            default -> 1.0;
        };
    }

    private Image getConstructionStageImage() {
        int stage = getBuildStage();
        if (stage >= 4) return image;

        return switch (constructionSize) {
            case LARGE -> switch (stage) {
                case 1 -> LARGE_STAGE_1_IMAGE;
                case 2 -> LARGE_STAGE_2_IMAGE;
                case 3 -> LARGE_STAGE_3_IMAGE;
                default -> image;
            };
            case SMALL -> switch (stage) {
                case 1 -> SMALL_STAGE_1_IMAGE;
                case 2 -> SMALL_STAGE_2_IMAGE;
                case 3 -> SMALL_STAGE_3_IMAGE;
                default -> image;
            };
        };
    }

    @Override
    public void draw(Graphics g) {
        int drawX = (int) Math.round(x - width / 2.0);
        int drawY = (int) Math.round(y - height / 2.0);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRect(drawX + 4, drawY + 6, width, height);

        Image stageImage = getConstructionStageImage();
        float alpha = 0.5f + (float) getBuildProgress() * 0.32f;
        if (stageImage != null) {
            Composite oldComposite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            double scale = getStageImageScale();
            int stageDrawWidth = Math.max(1, (int) Math.round(width * scale));
            int stageDrawHeight = Math.max(1, (int) Math.round(height * scale));
            int stageDrawX = drawX + (width - stageDrawWidth) / 2;
            int stageDrawY = drawY + (height - stageDrawHeight) / 2;
            g2.drawImage(stageImage, stageDrawX, stageDrawY, stageDrawWidth, stageDrawHeight, null);
            g2.setComposite(oldComposite);
        } else {
            g2.setColor(team == 0 ? new Color(90, 130, 220, 120) : new Color(170, 90, 90, 120));
            g2.fillRect(drawX, drawY, width, height);
        }

        if (hitEffectTimer > 0) {
            g2.setColor(hitEffectColor);
            g2.drawOval((int) Math.round(x) - 6, (int) Math.round(y) - 6, 12, 12);
            hitEffectTimer--;
        }

        int progressWidth = (int) Math.round((width - 8) * getBuildProgress());
        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(drawX + 4, drawY + height - 8, width - 8, 4);
        g2.setColor(new Color(90, 230, 120));
        g2.fillRect(drawX + 4, drawY + height - 8, Math.max(0, progressWidth), 4);
        g2.dispose();
    }

    public static int getBuildWidth(Type type) {
        return switch (type) {
            case COMMAND_CENTER -> CommandCenter.BUILD_WIDTH;
            case BARRACKS -> Barracks.BUILD_WIDTH;
        };
    }

    public static int getBuildHeight(Type type) {
        return switch (type) {
            case COMMAND_CENTER -> CommandCenter.BUILD_HEIGHT;
            case BARRACKS -> Barracks.BUILD_HEIGHT;
        };
    }

    public static ConstructionSize getConstructionSize(Type type) {
        return switch (type) {
            case COMMAND_CENTER, BARRACKS -> ConstructionSize.LARGE;
        };
    }

    public static Point2D.Double getBuildPoint(Type type, double centerX, double centerY, int index) {
        Point2D.Double[] points = getBuildPoints(type, centerX, centerY);
        return points[Math.floorMod(index, points.length)];
    }

    public static int getNearestBuildPointIndex(Type type, double centerX, double centerY, double workerX, double workerY) {
        Point2D.Double[] points = getBuildPoints(type, centerX, centerY);
        int bestIndex = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i < points.length; i++) {
            Point2D.Double point = points[i];
            double dx = workerX - point.x;
            double dy = workerY - point.y;
            double distSq = dx * dx + dy * dy;
            if (distSq < bestDist) {
                bestDist = distSq;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static Point2D.Double[] getBuildPoints(Type type, double centerX, double centerY) {
        double halfW = getBuildWidth(type) / 2.0;
        double halfH = getBuildHeight(type) / 2.0;
        double margin = 10.0;
        return new Point2D.Double[]{
                new Point2D.Double(centerX - halfW - margin, centerY - halfH * 0.55),
                new Point2D.Double(centerX + halfW + margin, centerY - halfH * 0.25),
                new Point2D.Double(centerX + halfW * 0.35, centerY + halfH + margin),
                new Point2D.Double(centerX - halfW - margin, centerY + halfH * 0.35)
        };
    }

    public static int getBuildCost(Type type) {
        return switch (type) {
            case COMMAND_CENTER -> 400;
            case BARRACKS -> 150;
        };
    }

    private static int getBuildMaxHp(Type type) {
        return switch (type) {
            case COMMAND_CENTER -> 1500;
            case BARRACKS -> 1000;
        };
    }

    private static int getBuildTicks(Type type) {
        return switch (type) {
            case COMMAND_CENTER -> 240;
            case BARRACKS -> 180;
        };
    }

    private static Image loadSharedImage(String path) {
        try {
            return javax.imageio.ImageIO.read(ConstructionSite.class.getResource(path));
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFinalImagePath(Type type) {
        return switch (type) {
            case COMMAND_CENTER -> "/starcraft/res/command_center.png";
            case BARRACKS -> "/starcraft/res/barracks.png";
        };
    }
}
