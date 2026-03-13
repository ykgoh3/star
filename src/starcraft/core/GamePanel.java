package starcraft.core;

import starcraft.engine.InputHandler;
import starcraft.engine.vectorMath;
import starcraft.objects.Attackable;
import starcraft.objects.Unit;
import starcraft.objects.buildings.ConstructionSite;
import starcraft.objects.buildings.terran.Barracks;
import starcraft.objects.buildings.Building;
import starcraft.objects.buildings.UnitFactoryBuilding;
import starcraft.objects.buildings.terran.CommandCenter;
import starcraft.objects.resources.MineralPatch;
import starcraft.objects.units.zerg.Hydralisk;
import starcraft.objects.units.terran.Marine;
import starcraft.objects.units.terran.SCV;
import starcraft.objects.units.zerg.Zergling;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.List;

public class GamePanel extends JPanel {
    private enum BuildPlacementType {
        NONE,
        COMMAND_CENTER,
        BARRACKS
    }

    public static final int BASE_WIDTH = 800;
    public static final int BASE_HEIGHT = 600;
    public static final int UI_BAR_HEIGHT = 150;

    private static final int WORLD_WIDTH = 1800;
    private static final int WORLD_HEIGHT = 1200;
    private static final int EDGE_SCROLL_THRESHOLD = 20;
    private static final int EDGE_SCROLL_SPEED = 8;
    private static final double MINERAL_CLUSTER_RANGE = 180.0;

    private final ArrayList<Unit> units = new ArrayList<>();
    private final ArrayList<Unit> selectedUnitOrder = new ArrayList<>();
    private final ArrayList<Building> buildings = new ArrayList<>();
    private final ArrayList<MineralPatch> mineralPatches = new ArrayList<>();
    private final InputHandler inputHandler;
    private final TerrainGrid terrain;
    private final Timer gameLoop;

    private Building selectedBuilding;
    private MineralPatch selectedMineral;
    private boolean scvBuildMenuOpen = false;
    private BuildPlacementType buildPlacementType = BuildPlacementType.NONE;
    private final Image previewCommandCenterImage = loadUiImage("/starcraft/res/command_center.png");
    private final Image previewBarracksImage = loadUiImage("/starcraft/res/barracks.png");
    private int uiHoverX = -1;
    private int uiHoverY = -1;
    private int cameraX = 0;
    private int cameraY = 0;
    private final int[] minerals = new int[]{500, 500};
    private final Robot mouseLockRobot;
    private boolean mouseLockEnabled = true;

    public GamePanel() {
        setBackground(Color.BLACK);
        terrain = new TerrainGrid(WORLD_WIDTH, WORLD_HEIGHT, 20);
        inputHandler = new InputHandler(this, units);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);

        buildings.add(new CommandCenter(100, 200, 0));
        buildings.add(new Barracks(110, 110, 0));

        mineralPatches.add(new MineralPatch(250, 126, 1500));
        mineralPatches.add(new MineralPatch(258, 156, 1500));
        mineralPatches.add(new MineralPatch(268, 181, 1500));
        mineralPatches.add(new MineralPatch(262, 219, 1500));
        mineralPatches.add(new MineralPatch(264, 244, 1500));
        mineralPatches.add(new MineralPatch(248, 274, 1500));

        units.add(new SCV(50, 300, 0));
        units.add(new SCV(75, 300, 0));
        units.add(new SCV(100, 300, 0));
        units.add(new SCV(125, 300, 0));

//        for (int row = 0; row < 3; row++) {
//            for (int col = 0; col < 4; col++) {
//                int x = 150 + col * 25 - row * 25;
//                int y = 200 - col * 25 - row * 25;
//                units.add(new Marine(x, y, 0));
//            }
//        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int x = 550 + col * 25 + row * 25;
                int y = 390 - col * 25 + row * 25;

                if (row == 0) {
                    units.add(new Hydralisk(x, y, 1));
                } else {
                    units.add(new Zergling(x, y, 1));
                }
            }
        }

        refreshBuildingBlockers();

        mouseLockRobot = createMouseLockRobot();

        gameLoop = new Timer(33, e -> updateGame());
        gameLoop.start();
    }

    private void updateGame() {
        enforceMouseLock();
        updateCameraFromMouse();
        refreshBuildingBlockers();

        for (Building building : buildings) {
            building.update(this);
        }

        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);

            if (!u.manualOrder) {
                boolean targetDead = (u.target != null) && !u.target.isAlive();
                boolean noValidTarget = (u.target == null) || targetDead;

                if (u.autoRetaliating) {
                    if (noValidTarget) {
                        Attackable nearest = findNearestEnemyInRange(u, u.range + 200);
                        if (nearest == null) {
                            nearest = findNearestEnemyInRange(u, Double.MAX_VALUE);
                        }
                        if (nearest != null) {
                            u.target = nearest;
                        } else {
                            u.stop();
                            u.autoRetaliating = false;
                        }
                    }
                } else if (u.commandState == 0) {
                    Attackable nearest = findNearestEnemyInRange(u, u.range + 20);
                    u.target = nearest;

                    if (nearest != null) {
                        u.commandState = 1;
                        u.destX = u.x;
                        u.destY = u.y;
                    }
                } else if (u.commandState == 1 && noValidTarget) {
                    Attackable nearest = findNearestEnemyInRange(u, u.range + 20);
                    if (nearest != null) {
                        u.target = nearest;
                    } else if (targetDead) {
                        u.stop();
                    } else {
                        double distToDest = vectorMath.getDistance(u.x, u.y, u.destX, u.destY);
                        if (distToDest <= Math.max(4.0, u.size * 0.5)) {
                            u.stop();
                        } else {
                            u.target = null;
                        }
                    }
                }
            } else if (u.target != null && !u.target.isAlive()) {
                u.target = null;
            }

            if (u.hp <= 0) {
                u.deathTimer--;
                continue;
            }

            if (u instanceof SCV scv) {
                scv.updateHarvest(this);
            }

            u.update(units, terrain);
            u.attack(this);
        }

        units.removeIf(u -> u.hp <= 0 && u.deathTimer <= 0);
        repaint();
    }

    private void updateCameraFromMouse() {
        Point mouse = getMousePosition();
        if (mouse == null) return;

        if (mouse.x <= EDGE_SCROLL_THRESHOLD) {
            cameraX -= EDGE_SCROLL_SPEED;
        } else if (mouse.x >= getWidth() - EDGE_SCROLL_THRESHOLD) {
            cameraX += EDGE_SCROLL_SPEED;
        }

        if (mouse.y <= EDGE_SCROLL_THRESHOLD) {
            cameraY -= EDGE_SCROLL_SPEED;
        } else if (mouse.y >= getHeight() - EDGE_SCROLL_THRESHOLD) {
            cameraY += EDGE_SCROLL_SPEED;
        }

        int maxCameraX = Math.max(0, WORLD_WIDTH - getWidth());
        int maxCameraY = Math.max(0, WORLD_HEIGHT - getUiBarTop());

        cameraX = Math.max(0, Math.min(cameraX, maxCameraX));
        cameraY = Math.max(0, Math.min(cameraY, maxCameraY));
    }

    private Robot createMouseLockRobot() {
        try {
            return new Robot();
        } catch (AWTException e) {
            return null;
        }
    }

    private void enforceMouseLock() {
        if (!mouseLockEnabled || mouseLockRobot == null) return;
        if (!isShowing()) return;

        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null || !window.isActive()) return;

        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return;

        Point mouseScreen = pointerInfo.getLocation();
        Point panelScreen;
        try {
            panelScreen = getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            return;
        }

        int minX = panelScreen.x + 1;
        int minY = panelScreen.y + 1;
        int maxX = panelScreen.x + getWidth() - 2;
        int maxY = panelScreen.y + getHeight() - 2;

        int clampedX = Math.max(minX, Math.min(mouseScreen.x, maxX));
        int clampedY = Math.max(minY, Math.min(mouseScreen.y, maxY));

        if (clampedX != mouseScreen.x || clampedY != mouseScreen.y) {
            mouseLockRobot.mouseMove(clampedX, clampedY);
        }
    }

    private void refreshBuildingBlockers() {
        terrain.clearBlocked();
        for (Building building : buildings) {
            if (building.isDestroyed() || building instanceof ConstructionSite) continue;
            terrain.blockRectWorld(
                    building.getX(),
                    building.getY(),
                    building.getPathingBlockWidth(),
                    building.getPathingBlockHeight(),
                    building.getPathingPadding()
            );
        }

        for (MineralPatch patch : mineralPatches) {
            if (patch.isDepleted()) continue;
            terrain.blockCircleWorld(patch.getX(), patch.getY(), patch.getPathingRadius());
        }
    }

    private Attackable findNearestEnemyInRange(Unit me, double maxRange) {
        Attackable closest = null;
        double closestDistance = maxRange;
        boolean closestIsUnit = false;
        final double unitPrioritySlack = 18.0;

        for (Unit enemy : units) {
            if (enemy.team == me.team || enemy.hp <= 0) continue;
            double d = vectorMath.getDistance(me.x, me.y, enemy.x, enemy.y);
            if (d > maxRange) continue;
            if (closest == null || d < closestDistance || (!closestIsUnit && d <= closestDistance + unitPrioritySlack)) {
                closest = enemy;
                closestDistance = d;
                closestIsUnit = true;
            }
        }

        for (Building building : buildings) {
            if (building.getTeam() == me.team || building.isDestroyed()) continue;
            double d = vectorMath.getDistance(me.x, me.y, building.getX(), building.getY());
            if (d > maxRange) continue;
            if (closest == null || (!closestIsUnit && d < closestDistance)) {
                closest = building;
                closestDistance = d;
                closestIsUnit = false;
            }
        }

        return closest;
    }

    public int getUiBarTop() {
        return getHeight() - UI_BAR_HEIGHT;
    }

    public boolean isInUiArea(int x, int y) {
        return y >= getUiBarTop();
    }

    public int screenToWorldX(int screenX) {
        return screenX + cameraX;
    }

    public int screenToWorldY(int screenY) {
        return screenY + cameraY;
    }

    public Rectangle getMinimapRect() {
        int top = getUiBarTop();
        int pad = 8;
        int size = UI_BAR_HEIGHT - pad * 2;
        return new Rectangle(pad, top + pad, size, size);
    }

    public Rectangle getControlPanelRect() {
        int top = getUiBarTop();
        int pad = 8;
        int size = UI_BAR_HEIGHT - pad * 2;
        return new Rectangle(getWidth() - size - pad, top + pad, size, size);
    }

    public Rectangle getStatusPanelRect() {
        Rectangle mini = getMinimapRect();
        Rectangle control = getControlPanelRect();
        int gap = 12;
        int x = mini.x + mini.width + gap;
        int width = Math.max(120, control.x - gap - x);

        int sideTop = mini.y;
        int sideBottom = mini.y + mini.height;
        int y = sideTop + 10;
        int height = Math.max(40, (sideBottom - 10) - y);

        return new Rectangle(x, y, width, height);
    }

    public Rectangle getResourceRect() {
        return new Rectangle(getWidth() - 220, 10, 210, 32);
    }

    private Unit getPrimarySelectedUnit() {
        for (Unit unit : units) {
            if (unit.isSelected && unit.hp > 0) {
                return unit;
            }
        }
        return null;
    }


    private List<Unit> getSelectedUnits() {
        ArrayList<Unit> selected = new ArrayList<>();
        for (Unit unit : selectedUnitOrder) {
            if (unit != null && unit.isSelected && unit.hp > 0 && !selected.contains(unit)) {
                selected.add(unit);
            }
        }
        for (Unit unit : units) {
            if (unit.isSelected && unit.hp > 0 && !selected.contains(unit)) {
                selected.add(unit);
            }
        }
        return selected;
    }

    public void captureSelectedUnitOrder() {
        selectedUnitOrder.clear();
        for (Unit unit : units) {
            if (unit.isSelected && unit.hp > 0) {
                selectedUnitOrder.add(unit);
            }
        }
    }

    public void updateUiHoverPoint(int mouseX, int mouseY) {
        this.uiHoverX = mouseX;
        this.uiHoverY = mouseY;
    }

    private void drawUnitPortrait(Graphics g, Rectangle box, Unit unit) {
        if (unit == null || box.width <= 4 || box.height <= 4) return;

        BufferedImage mask = new BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = mask.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spriteW = Math.max(1, unit.getDrawWidth());
        int spriteH = Math.max(1, unit.getDrawHeight());
        double scale = Math.min((box.width - 8) / (double) spriteW, (box.height - 8) / (double) spriteH);
        int drawW = Math.max(1, (int) Math.round(spriteW * scale));
        int drawH = Math.max(1, (int) Math.round(spriteH * scale));
        int drawX = (box.width - drawW) / 2;
        int drawY = (box.height - drawH) / 2;

        if (unit.image != null) {
            pg.drawImage(unit.image, drawX, drawY, drawW, drawH, null);
        } else {
            pg.setColor(Color.WHITE);
            pg.fillOval(drawX, drawY, drawW, drawH);
        }
        pg.dispose();

        Color baseColor = getPortraitBaseColor(unit);
        BufferedImage portrait = new BufferedImage(box.width, box.height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < box.height; y++) {
            for (int x = 0; x < box.width; x++) {
                int alpha = (mask.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha == 0) continue;

                double nx = (x - box.width * 0.35) / box.width;
                double ny = (y - box.height * 0.3) / box.height;
                double radial = Math.max(0.0, 1.0 - Math.sqrt(nx * nx + ny * ny) * 2.0);
                double topLight = Math.max(0.0, 1.0 - (double) y / box.height);
                double leftLight = Math.max(0.0, 1.0 - (double) x / box.width);
                double shade = 0.2 + radial * 0.45 + topLight * 0.25 + leftLight * 0.1;
                shade = Math.max(0.1, Math.min(1.0, shade));

                int r = (int) Math.round(baseColor.getRed() * (0.45 + 0.55 * shade));
                int gCol = (int) Math.round(baseColor.getGreen() * (0.45 + 0.55 * shade));
                int b = (int) Math.round(baseColor.getBlue() * (0.45 + 0.55 * shade));
                portrait.setRGB(x, y, (alpha << 24) | (r << 16) | (gCol << 8) | b);
            }
        }

        g.drawImage(portrait, box.x, box.y, null);
    }

    private void drawMultiSelectedUnits(Graphics g, Rectangle status, List<Unit> selectedUnits) {
        if (selectedUnits == null || selectedUnits.isEmpty()) return;

        int availableHeight = Math.max(24, status.height - 20);
        int slotSize = Math.min(availableHeight / 2 - 6, 44);
        if (slotSize <= 8) return;

        int startX = status.x + 12;
        int startY = status.y + 10;
        int rows = 2;

        for (int iSel = 0; iSel < selectedUnits.size(); iSel++) {
            Unit unit = selectedUnits.get(iSel);
            int col = iSel / rows;
            int row = iSel % rows;
            int cellX = startX + col * (slotSize + 8);
            int cellY = startY + row * (slotSize + 8);
            Rectangle cell = new Rectangle(cellX, cellY, slotSize, slotSize);

            g.setColor(new Color(40, 80, 180));
            g.drawRect(cell.x, cell.y, cell.width, cell.height);
            drawUnitPortrait(g, cell, unit);
        }
    }
    private String getUnitDisplayName(Unit unit) {
        if (unit instanceof SCV) return "SCV";
        if (unit instanceof Marine) return "Marine";
        if (unit instanceof Hydralisk) return "Hydralisk";
        if (unit instanceof Zergling) return "Zergling";
        return "Unit";
    }

    private String getBuildingDisplayName(Building building) {
        if (building instanceof CommandCenter) return "Command Center";
        if (building instanceof Barracks) return "Barracks";
        if (building instanceof ConstructionSite site) return site.getDisplayName();
        return "Building";
    }

    private Color getBuildingPortraitBaseColor(Building building) {
        double hpRatio = (building == null || building.getMaxHp() <= 0) ? 1.0 : Math.max(0.0, Math.min(1.0, (double) building.getHp() / building.getMaxHp()));
        if (hpRatio > 0.66) return new Color(70, 185, 25);
        if (hpRatio > 0.33) return new Color(210, 175, 30);
        return new Color(195, 45, 30);
    }

    private void drawSelectedBuildingSilhouette(Graphics g, Rectangle status, Building building) {
        if (building == null) return;

        int boxSize = Math.min(status.height - 8, 86);
        if (boxSize <= 12) return;

        int boxX = status.x + 12;
        int boxY = status.y + (status.height - boxSize) / 2;

        BufferedImage mask = new BufferedImage(boxSize, boxSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = mask.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spriteW = Math.max(1, building.getWidth());
        int spriteH = Math.max(1, building.getHeight());
        double scale = Math.min((boxSize - 4) / (double) spriteW, (boxSize - 4) / (double) spriteH);
        int drawW = Math.max(1, (int) Math.round(spriteW * scale));
        int drawH = Math.max(1, (int) Math.round(spriteH * scale));
        int drawX = (boxSize - drawW) / 2;
        int drawY = (boxSize - drawH) / 2;

        if (building.getImage() != null) {
            pg.drawImage(building.getImage(), drawX, drawY, drawW, drawH, null);
        } else {
            pg.setColor(Color.WHITE);
            pg.fillRect(drawX, drawY, drawW, drawH);
        }
        pg.dispose();

        Color baseColor = getBuildingPortraitBaseColor(building);
        BufferedImage portrait = new BufferedImage(boxSize, boxSize, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < boxSize; y++) {
            for (int x = 0; x < boxSize; x++) {
                int alpha = (mask.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha == 0) continue;

                double nx = (x - boxSize * 0.35) / boxSize;
                double ny = (y - boxSize * 0.3) / boxSize;
                double radial = Math.max(0.0, 1.0 - Math.sqrt(nx * nx + ny * ny) * 2.0);
                double topLight = Math.max(0.0, 1.0 - (double) y / boxSize);
                double leftLight = Math.max(0.0, 1.0 - (double) x / boxSize);
                double shade = 0.2 + radial * 0.45 + topLight * 0.25 + leftLight * 0.1;
                shade = Math.max(0.1, Math.min(1.0, shade));

                int r = (int) Math.round(baseColor.getRed() * (0.45 + 0.55 * shade));
                int gCol = (int) Math.round(baseColor.getGreen() * (0.45 + 0.55 * shade));
                int b = (int) Math.round(baseColor.getBlue() * (0.45 + 0.55 * shade));
                portrait.setRGB(x, y, (alpha << 24) | (r << 16) | (gCol << 8) | b);
            }
        }

        g.drawImage(portrait, boxX, boxY, null);
    }

    private Color getPortraitBaseColor(Unit unit) {
        double hpRatio = (unit == null || unit.maxHp <= 0) ? 1.0 : Math.max(0.0, Math.min(1.0, (double) unit.hp / unit.maxHp));
        if (hpRatio > 0.66) return new Color(70, 185, 25);
        if (hpRatio > 0.33) return new Color(210, 175, 30);
        return new Color(195, 45, 30);
    }


    private void drawSelectedUnitSilhouette(Graphics g, Rectangle status, Unit unit) {
        if (unit == null) return;

        int boxSize = Math.min(status.height - 10, 84);
        if (boxSize <= 12) return;

        int boxX = status.x + 12;
        int boxY = status.y + (status.height - boxSize) / 2;



        BufferedImage mask = new BufferedImage(boxSize, boxSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = mask.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int spriteW = Math.max(1, unit.getDrawWidth());
        int spriteH = Math.max(1, unit.getDrawHeight());
        double scale = Math.min((boxSize - 6) / (double) spriteW, (boxSize - 6) / (double) spriteH);
        int drawW = Math.max(1, (int) Math.round(spriteW * scale));
        int drawH = Math.max(1, (int) Math.round(spriteH * scale));
        int drawX = (boxSize - drawW) / 2;
        int drawY = (boxSize - drawH) / 2;

        if (unit.image != null) {
            pg.drawImage(unit.image, drawX, drawY, drawW, drawH, null);
        } else {
            pg.setColor(Color.WHITE);
            pg.fillOval(drawX, drawY, drawW, drawH);
        }
        pg.dispose();

        Color baseColor = getPortraitBaseColor(unit);
        BufferedImage portrait = new BufferedImage(boxSize, boxSize, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < boxSize; y++) {
            for (int x = 0; x < boxSize; x++) {
                int alpha = (mask.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha == 0) continue;

                double nx = (x - boxSize * 0.35) / boxSize;
                double ny = (y - boxSize * 0.3) / boxSize;
                double radial = Math.max(0.0, 1.0 - Math.sqrt(nx * nx + ny * ny) * 2.0);
                double topLight = Math.max(0.0, 1.0 - (double) y / boxSize);
                double leftLight = Math.max(0.0, 1.0 - (double) x / boxSize);
                double shade = 0.2 + radial * 0.45 + topLight * 0.25 + leftLight * 0.1;
                shade = Math.max(0.1, Math.min(1.0, shade));

                int r = (int) Math.round(baseColor.getRed() * (0.45 + 0.55 * shade));
                int gCol = (int) Math.round(baseColor.getGreen() * (0.45 + 0.55 * shade));
                int b = (int) Math.round(baseColor.getBlue() * (0.45 + 0.55 * shade));
                int rgba = (alpha << 24) | (r << 16) | (gCol << 8) | b;
                portrait.setRGB(x, y, rgba);
            }
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(portrait, boxX, boxY, null);
        g2.dispose();
    }
    private Rectangle getArmorIconBounds(Rectangle status) {
        int iconSize = 39;
        int gap = 12;
        int totalWidth = iconSize * 2 + gap;
        int startX = status.x + (status.width - totalWidth) / 2;
        int y = status.y + status.height - iconSize - 16;
        return new Rectangle(startX, y, iconSize, iconSize);
    }

    private Rectangle getAttackIconBounds(Rectangle status) {
        Rectangle armor = getArmorIconBounds(status);
        return new Rectangle(armor.x + armor.width + 10, armor.y, armor.width, armor.height);
    }

    private void drawStatIconBox(Graphics2D g2, Rectangle rect) {
        g2.setColor(new Color(44, 44, 44));
        g2.setColor(new Color(110, 110, 110));

        int lineY = rect.y + rect.height - 11;
        int diagonalStartX = rect.x + rect.width - 10;
        int diagonalEndX = rect.x + rect.width - 23;
        int diagonalEndY = rect.y + rect.height - 1;
        g2.setColor(new Color(90, 90, 90));
        g2.drawLine(rect.x + rect.width - 1, lineY, diagonalStartX, lineY);
        g2.drawLine(diagonalStartX, lineY, diagonalEndX, diagonalEndY);
    }

    private void drawUpgradeLevel(Graphics2D g2, Rectangle rect, int level) {
        String text = Integer.toString(level);
        Font originalFont = g2.getFont();
        g2.setFont(originalFont.deriveFont(Font.BOLD, 10f));
        FontMetrics fm = g2.getFontMetrics();
        int textX = rect.x + rect.width - fm.stringWidth(text) - 4;
        int textY = rect.y + rect.height - 2;
        g2.setColor(Color.BLACK);
        g2.drawString(text, textX + 1, textY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(text, textX, textY);
        g2.setFont(originalFont);
    }

    private void drawArmorIcon(Graphics2D g2, Rectangle rect) {
        drawStatIconBox(g2, rect);
        int cx = rect.x + rect.width / 2 - 4;
        int top = rect.y + 7;
        Polygon shield = new Polygon(
                new int[]{cx, rect.x + rect.width - 15, rect.x + rect.width - 17, cx, rect.x + 10, rect.x + 8},
                new int[]{top, top + 5, rect.y + rect.height - 13, rect.y + rect.height - 7, rect.y + rect.height - 13, top + 5},
                6
        );
        g2.setColor(new Color(110, 190, 255));
        g2.fillPolygon(shield);
        g2.setColor(new Color(210, 240, 255));
        g2.drawPolygon(shield);
        g2.drawLine(cx, top + 4, cx, rect.y + rect.height - 10);
        drawUpgradeLevel(g2, rect, 0);
    }

    private void drawAttackIcon(Graphics2D g2, Rectangle rect) {
        drawStatIconBox(g2, rect);
        int x1 = rect.x + 10;
        int y1 = rect.y + rect.height - 10;
        int x2 = rect.x + rect.width - 15;
        int y2 = rect.y + 10;
        g2.setColor(new Color(255, 170, 70));
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);
        g2.setColor(new Color(255, 225, 150));
        Polygon tip = new Polygon(new int[]{x2, x2 + 6, x2 - 5}, new int[]{y2 - 6, y2, y2 + 5}, 3);
        g2.fillPolygon(tip);
        g2.setColor(new Color(160, 110, 60));
        g2.drawLine(x1 - 2, y1 + 2, x1 + 5, y1 + 9);
        drawUpgradeLevel(g2, rect, 0);
    }

    private void drawStatTooltip(Graphics g, Rectangle anchor, String label, int value) {
        String text = label + ": " + value;
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(text) + 14;
        int height = 22;
        int x = anchor.x + anchor.width / 2 - width / 2;
        int y = anchor.y - height - 6;
        Rectangle status = getStatusPanelRect();
        if (x < status.x + 4) x = status.x + 4;
        if (x + width > status.x + status.width - 4) x = status.x + status.width - width - 4;
        if (y < status.y + 4) y = anchor.y + anchor.height + 6;

        g.setColor(new Color(18, 18, 18));
        g.fillRect(x, y, width, height);
        g.setColor(new Color(170, 170, 170));
        g.drawRect(x, y, width, height);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 7, y + 15);
    }

    private void drawUnitStatIcons(Graphics g, Rectangle status, Unit unit) {
        if (unit == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle armorRect = getArmorIconBounds(status);
        Rectangle attackRect = getAttackIconBounds(status);
        drawArmorIcon(g2, armorRect);
        drawAttackIcon(g2, attackRect);
        g2.dispose();

        Point hover = new Point(uiHoverX, uiHoverY);
        if (armorRect.contains(hover)) {
            drawStatTooltip(g, armorRect, "Armor", unit.armor);
        } else if (attackRect.contains(hover)) {
            drawStatTooltip(g, attackRect, "Damage", unit.damage);
        }
    }
    public Building findBuildingAtWorld(int worldX, int worldY) {
        for (int i = buildings.size() - 1; i >= 0; i--) {
            Building b = buildings.get(i);
            if (!b.isDestroyed() && b.getBounds().contains(worldX, worldY)) {
                return b;
            }
        }
        return null;
    }

    public MineralPatch findMineralAtWorld(int worldX, int worldY) {
        for (int i = mineralPatches.size() - 1; i >= 0; i--) {
            MineralPatch patch = mineralPatches.get(i);
            if (!patch.isDepleted() && patch.contains(worldX, worldY)) {
                return patch;
            }
        }
        return null;
    }

    public MineralPatch findBestMineralLoopAssignment(MineralPatch origin, Unit worker) {
        if (origin == null || origin.isDepleted() || worker == null) return null;

        int bestLoopCount = Integer.MAX_VALUE;
        for (MineralPatch patch : mineralPatches) {
            if (!isInMineralCluster(origin, patch)) continue;
            bestLoopCount = Math.min(bestLoopCount, patch.getAssignedWorkerCount());
        }

        if (bestLoopCount == Integer.MAX_VALUE) {
            return null;
        }

        if (origin.getAssignedWorkerCount() == bestLoopCount) {
            return origin;
        }

        MineralPatch bestPatch = null;
        double bestDistance = Double.MAX_VALUE;
        for (MineralPatch patch : mineralPatches) {
            if (!isInMineralCluster(origin, patch)) continue;
            if (patch.getAssignedWorkerCount() != bestLoopCount) continue;

            double workerDistance = vectorMath.getDistance(worker.x, worker.y, patch.getX(), patch.getY());
            if (workerDistance < bestDistance) {
                bestDistance = workerDistance;
                bestPatch = patch;
            }
        }

        return bestPatch;
    }

    public MineralPatch findRedistributionMineralPatch(MineralPatch origin, MineralPatch current, Unit worker) {
        return findBestMineralLoopAssignment(origin, worker);
    }

    public MineralPatch findEmptyRedistributionMineralPatch(MineralPatch origin, MineralPatch current, Unit worker) {
        if (origin == null || origin.isDepleted() || worker == null) return null;

        MineralPatch bestPatch = null;
        double bestDistance = Double.MAX_VALUE;
        for (MineralPatch patch : mineralPatches) {
            if (!isInMineralCluster(origin, patch)) continue;
            if (patch.getAssignedWorkerCount() != 0) continue;

            double workerDistance = vectorMath.getDistance(worker.x, worker.y, patch.getX(), patch.getY());
            if (workerDistance < bestDistance) {
                bestDistance = workerDistance;
                bestPatch = patch;
            }
        }
        return bestPatch;
    }

    private boolean isInMineralCluster(MineralPatch origin, MineralPatch candidate) {
        if (origin == null || candidate == null || candidate.isDepleted()) return false;
        return vectorMath.getDistance(origin.getX(), origin.getY(), candidate.getX(), candidate.getY()) <= MINERAL_CLUSTER_RANGE;
    }

    public CommandCenter findNearestCommandCenter(double worldX, double worldY, int team) {
        CommandCenter nearest = null;
        double minDistSq = Double.MAX_VALUE;

        for (Building building : buildings) {
            if (!(building instanceof CommandCenter center)) continue;
            if (center.isDestroyed() || center.getTeam() != team) continue;

            double dx = center.getX() - worldX;
            double dy = center.getY() - worldY;
            double distSq = dx * dx + dy * dy;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = center;
            }
        }
        return nearest;
    }

    public int getMinerals(int team) {
        if (team < 0 || team >= minerals.length) return 0;
        return minerals[team];
    }

    public void addMinerals(int team, int amount) {
        if (team < 0 || team >= minerals.length || amount <= 0) return;
        minerals[team] += amount;
    }

    public boolean spendMinerals(int team, int amount) {
        if (team < 0 || team >= minerals.length || amount < 0) return false;
        if (minerals[team] < amount) return false;
        minerals[team] -= amount;
        return true;
    }

    public void selectBuilding(Building building) {
        this.selectedBuilding = building;
        this.selectedMineral = null;
        this.selectedUnitOrder.clear();
    }

    public void selectMineral(MineralPatch mineralPatch) {
        this.selectedMineral = mineralPatch;
        this.selectedBuilding = null;
        this.selectedUnitOrder.clear();
    }

    public void clearBuildingSelection() {
        this.selectedBuilding = null;
    }

    public void clearMineralSelection() {
        this.selectedMineral = null;
    }

    public void clearSelections() {
        clearSelections(true);
    }

    public void clearSelections(boolean clearUnitOrder) {
        this.selectedBuilding = null;
        this.selectedMineral = null;
        if (clearUnitOrder) {
            this.selectedUnitOrder.clear();
        }
    }

    public boolean handleUiLeftClick(int mouseX, int mouseY) {
        if (!isInUiArea(mouseX, mouseY)) return false;

        if (selectedBuilding instanceof UnitFactoryBuilding factory && handleProductionQueueClick(mouseX, mouseY, factory)) {
            return true;
        }

        if (selectedBuilding instanceof Barracks barracks) {
            Rectangle marineButton = getMarineButtonBounds();
            if (marineButton.contains(mouseX, mouseY) && barracks.getQueuedUnits() < UnitFactoryBuilding.MAX_QUEUE_SIZE && spendMinerals(0, 50)) {
                barracks.enqueueMarine();
                return true;
            }
        } else if (selectedBuilding instanceof CommandCenter center) {
            Rectangle scvButton = getScvButtonBounds();
            if (scvButton.contains(mouseX, mouseY) && center.getQueuedUnits() < UnitFactoryBuilding.MAX_QUEUE_SIZE && spendMinerals(0, 50)) {
                center.enqueueWorker();
                return true;
            }
        } else {
            Unit selectedUnit = getPrimarySelectedUnit();
            if (selectedUnit instanceof SCV) {
                if (scvBuildMenuOpen) {
                    if (getScvCommandCenterBuildButtonBounds().contains(mouseX, mouseY)) {
                        scvBuildMenuOpen = false;
                        buildPlacementType = BuildPlacementType.COMMAND_CENTER;
                        return true;
                    }
                    if (getScvBarracksBuildButtonBounds().contains(mouseX, mouseY)) {
                        scvBuildMenuOpen = false;
                        buildPlacementType = BuildPlacementType.BARRACKS;
                        return true;
                    }
                } else if (getScvBuildButtonBounds().contains(mouseX, mouseY)) {
                    scvBuildMenuOpen = true;
                    return true;
                }
            }
        }

        return false;
    }
    private Rectangle getControlCellBounds(int col, int row) {
        Rectangle control = getControlPanelRect();

        int gridX = control.x + 1;
        int gridY = control.y + 1;
        int gridW = Math.max(0, control.width - 2);
        int gridH = Math.max(0, control.height - 2);

        int cellW = gridW / 3;
        int cellH = gridH / 3;

        int x = gridX + col * cellW + 2;
        int y = gridY + row * cellH + 2;
        int w = Math.max(0, cellW - 4);
        int h = Math.max(0, cellH - 4);
        return new Rectangle(x, y, w, h);
    }

    private Rectangle getMarineButtonBounds() {
        return getControlCellBounds(0, 0);
    }

    private Rectangle getScvButtonBounds() {
        return getControlCellBounds(0, 0);
    }

    private Rectangle getScvBuildButtonBounds() {
        return getControlCellBounds(0, 2);
    }

    private Rectangle getScvCommandCenterBuildButtonBounds() {
        return getControlCellBounds(0, 0);
    }

    private Rectangle getScvBarracksBuildButtonBounds() {
        return getControlCellBounds(0, 1);
    }

    private void drawTopRightResources(Graphics g) {
        Rectangle r = getResourceRect();
        g.setColor(new Color(20, 20, 20, 200));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(new Color(140, 140, 140));
        g.drawRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.WHITE);
        g.drawString("Minerals: " + getMinerals(0) + "   Gas: 0", r.x + 12, r.y + 21);
    }

    private void drawUnitControlGrid(Graphics g, Rectangle control) {
        int gridX = control.x + 1;
        int gridY = control.y + 1;
        int gridW = Math.max(0, control.width - 2);
        int gridH = Math.max(0, control.height - 2);

        if (gridW <= 0 || gridH <= 0) return;

        int cellW = gridW / 3;
        int cellH = gridH / 3;

        g.setColor(new Color(36, 36, 36));
        g.fillRect(gridX, gridY, gridW, gridH);
        g.setColor(new Color(120, 120, 120));

        for (int i = 1; i < 3; i++) {
            int x = gridX + cellW * i;
            int y = gridY + cellH * i;
            g.drawLine(x, gridY, x, gridY + gridH);
            g.drawLine(gridX, y, gridX + gridW, y);
        }
    }



    private String getProductionQueueLabel(UnitFactoryBuilding factory) {
        if (factory instanceof CommandCenter) return "SCV";
        if (factory instanceof Barracks) return "Marine";
        return "Unit";
    }

    private Color getProductionQueueColor(UnitFactoryBuilding factory) {
        if (factory instanceof CommandCenter) return new Color(60, 140, 180);
        if (factory instanceof Barracks) return new Color(60, 90, 180);
        return new Color(70, 100, 165);
    }
    private Rectangle getProductionPanelRect(Rectangle status) {
        int panelWidth = Math.min(196, Math.max(140, status.width - 220));
        int panelHeight = Math.min(status.height - 14, 84);
        int panelX = status.x + 116;
        int panelY = status.y + 28;
        return new Rectangle(panelX, panelY, panelWidth, panelHeight);
    }

    private Rectangle getProductionSlotBounds(Rectangle status, int slotIndex) {
        Rectangle panel = getProductionPanelRect(status);
        int bottomSize = Math.max(28, Math.min(32, Math.min((panel.width - 32) / 4, panel.height - 24)));
        int gap = Math.max(4, Math.min(7, (panel.width - 16 - 4 * bottomSize) / 3));
        int bottomY = panel.y + panel.height - bottomSize - 2;
        int twoX = panel.x + 8;
        if (slotIndex == 0) {
            return new Rectangle(twoX, bottomY - bottomSize - gap, bottomSize, bottomSize);
        }
        if (slotIndex == 1) {
            return new Rectangle(twoX, bottomY, bottomSize, bottomSize);
        }
        int x = twoX + (bottomSize + gap) * (slotIndex - 1);
        return new Rectangle(x, bottomY, bottomSize, bottomSize);
    }

    private int getProductionCancelCost(UnitFactoryBuilding factory) {
        if (factory instanceof CommandCenter) return 50;
        if (factory instanceof Barracks) return 50;
        return 0;
    }

    private boolean handleProductionQueueClick(int mouseX, int mouseY, UnitFactoryBuilding factory) {
        if (factory == null || factory.getQueuedUnits() <= 0) return false;
        Rectangle status = getStatusPanelRect();
        for (int i = 0; i < Math.min(UnitFactoryBuilding.MAX_QUEUE_SIZE, factory.getQueuedUnits()); i++) {
            Rectangle slot = getProductionSlotBounds(status, i);
            if (!slot.contains(mouseX, mouseY)) continue;
            if (factory.cancelQueuedUnitAt(i)) {
                addMinerals(factory.getTeam(), getProductionCancelCost(factory));
                return true;
            }
            return false;
        }
        return false;
    }

    private void drawProductionPanel(Graphics g, Rectangle status, UnitFactoryBuilding factory) {
        if (factory == null || factory.getQueuedUnits() <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        int panelWidth = Math.min(196, Math.max(140, status.width - 220));
        int panelHeight = Math.min(status.height - 14, 84);
        int panelX = status.x + 116;
        int panelY = status.y + 28;
        Rectangle panel = new Rectangle(panelX, panelY, panelWidth, panelHeight);

        g2.setColor(new Color(14, 14, 14, 0));

        String queueLabel = getProductionQueueLabel(factory);
        Color queueColor = getProductionQueueColor(factory);

        int bottomSize = Math.max(28, Math.min(32, Math.min((panel.width - 32) / 4, panel.height - 24)));
        int gap = Math.max(4, Math.min(7, (panel.width - 16 - 4 * bottomSize) / 3));
        int bottomY = panel.y + panel.height - bottomSize - 2;
        int twoX = panel.x + 8;
        int oneSize = bottomSize;
        int oneX = twoX;
        int oneY = bottomY - oneSize - gap;
        int threeX = twoX + bottomSize + gap;
        int fiveRight = twoX + (bottomSize + gap) * 3 + bottomSize;
        int progressX = twoX + bottomSize + 3;
        int progressHeight = Math.max(8, Math.min(10, bottomSize / 2));
        int progressY = bottomY - progressHeight - 3;
        int progressWidth = Math.max(20, fiveRight - progressX);

        drawProductionSlot(g2, new Rectangle(oneX, oneY, oneSize, oneSize), factory.getQueuedUnits() >= 1 ? queueLabel : "1", queueColor, factory.getQueuedUnits() >= 1, true);
        drawProductionSlot(g2, new Rectangle(twoX, bottomY, bottomSize, bottomSize), factory.getQueuedUnits() >= 2 ? queueLabel : "2", queueColor, factory.getQueuedUnits() >= 2, false);

        for (int i = 0; i < 3; i++) {
            int x = twoX + (bottomSize + gap) * (i + 1);
            drawProductionSlot(g2, new Rectangle(x, bottomY, bottomSize, bottomSize), factory.getQueuedUnits() >= i + 3 ? queueLabel : String.valueOf(i + 3), queueColor, factory.getQueuedUnits() >= i + 3, false);
        }

        g2.setColor(new Color(36, 36, 36));
        g2.fillRoundRect(progressX, progressY, progressWidth, progressHeight, progressHeight, progressHeight);
        g2.setColor(new Color(95, 95, 95));
        g2.drawRoundRect(progressX, progressY, progressWidth, progressHeight, progressHeight, progressHeight);
        int filledWidth = (int) Math.round(progressWidth * factory.getProductionProgress());
        if (filledWidth > 0) {
            g2.setColor(new Color(80, 220, 120));
            g2.fillRoundRect(progressX + 1, progressY + 1, Math.max(1, filledWidth - 2), Math.max(1, progressHeight - 2), progressHeight, progressHeight);
        }

        g2.dispose();
    }

    private void drawProductionSlot(Graphics2D g2, Rectangle rect, String label, Color slotColor, boolean active, boolean primary) {
        g2.setColor(active ? slotColor : new Color(24, 24, 24, 50));
        g2.fillRect(rect.x, rect.y, rect.width, rect.height);
        g2.setColor(active ? new Color(210, 210, 210) : new Color(90, 90, 90));
        g2.drawRect(rect.x, rect.y, rect.width, rect.height);

        if (label == null || label.isEmpty()) return;

        Font oldFont = g2.getFont();
        int fontSize = label.length() > 3 ? Math.max(12, primary ? rect.height - 6 : rect.height - 8) : Math.max(20, primary ? rect.height + 2 : rect.height);
        Font fittedFont = oldFont.deriveFont(Font.BOLD, (float) fontSize);
        FontMetrics fm = g2.getFontMetrics(fittedFont);
        int maxTextWidth = Math.max(8, rect.width - 6);
        while (fontSize > 9 && fm.stringWidth(label) > maxTextWidth) {
            fontSize--;
            fittedFont = oldFont.deriveFont(Font.BOLD, (float) fontSize);
            fm = g2.getFontMetrics(fittedFont);
        }

        g2.setFont(fittedFont);
        int textX = rect.x + (rect.width - fm.stringWidth(label)) / 2;
        int textY = rect.y + ((rect.height - fm.getHeight()) / 2) + fm.getAscent();
        g2.setColor(active ? Color.WHITE : new Color(100, 100, 100));
        g2.drawString(label, textX, textY);
        g2.setFont(oldFont);
    }

    private void drawBottomUiBar(Graphics g) {
        int top = getUiBarTop();
        g.setColor(new Color(18, 18, 18));
        g.fillRect(0, top, getWidth(), UI_BAR_HEIGHT);
        g.setColor(new Color(70, 70, 70));
        g.drawLine(0, top, getWidth(), top);

        Rectangle minimap = getMinimapRect();
        g.setColor(new Color(28, 28, 28));
        g.fillRect(minimap.x, minimap.y, minimap.width, minimap.height);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(minimap.x, minimap.y, minimap.width, minimap.height);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Minimap", minimap.x + 8, minimap.y + 16);

        Rectangle status = getStatusPanelRect();
        List<Unit> selectedUnits = getSelectedUnits();
        Unit selectedUnit = selectedUnits.isEmpty() ? null : selectedUnits.get(0);
        g.setColor(new Color(28, 28, 28));
        g.fillRect(status.x, status.y, status.width, status.height);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(status.x, status.y, status.width, status.height);
        g.setColor(Color.WHITE);
        UnitFactoryBuilding selectedFactory = selectedBuilding instanceof UnitFactoryBuilding factory ? factory : null;
        if (selectedUnits.size() > 1) {
            drawMultiSelectedUnits(g, status, selectedUnits);
        } else if (selectedUnit != null) {
            int statusCenterX = status.x + status.width / 2;
            String unitName = getUnitDisplayName(selectedUnit);
            String killsText = "Kills: " + selectedUnit.killCount;
            FontMetrics fm = g.getFontMetrics();
            g.drawString(unitName, statusCenterX - fm.stringWidth(unitName) / 2, status.y + 20);
            g.drawString(killsText, statusCenterX - fm.stringWidth(killsText) / 2, status.y + 42);
            drawSelectedUnitSilhouette(g, status, selectedUnit);
            g.drawString("HP: " + selectedUnit.hp + " / " + selectedUnit.maxHp, status.x + 12, status.y + status.height - 12);
            drawUnitStatIcons(g, status, selectedUnit);
        } else if (selectedBuilding instanceof Barracks barracks) {
            int statusCenterX = status.x + status.width / 2;
            String buildingName = getBuildingDisplayName(barracks);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(buildingName, statusCenterX - fm.stringWidth(buildingName) / 2, status.y + 20);
            drawSelectedBuildingSilhouette(g, status, barracks);
            g.drawString("HP: " + barracks.getHp() + " / " + barracks.getMaxHp(), status.x + 12, status.y + status.height - 12);
        } else if (selectedBuilding instanceof CommandCenter center) {
            int statusCenterX = status.x + status.width / 2;
            String buildingName = getBuildingDisplayName(center);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(buildingName, statusCenterX - fm.stringWidth(buildingName) / 2, status.y + 20);
            drawSelectedBuildingSilhouette(g, status, center);
            g.drawString("HP: " + center.getHp() + " / " + center.getMaxHp(), status.x + 12, status.y + status.height - 12);
        } else if (selectedBuilding instanceof ConstructionSite site) {
            int statusCenterX = status.x + status.width / 2;
            String buildingName = getBuildingDisplayName(site);
            String buildText = "Build: " + (int) Math.round(site.getBuildProgress() * 100) + "%";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(buildingName, statusCenterX - fm.stringWidth(buildingName) / 2, status.y + 20);
            g.drawString(buildText, statusCenterX - fm.stringWidth(buildText) / 2, status.y + 42);
            drawSelectedBuildingSilhouette(g, status, site);
            g.drawString("HP: " + site.getHp() + " / " + site.getMaxHp(), status.x + 12, status.y + status.height - 12);
        } else if (selectedMineral != null && !selectedMineral.isDepleted()) {
            g.drawString("Mineral Patch", status.x + 12, status.y + 20);
            g.drawString("Remaining: " + selectedMineral.getRemaining(), status.x + 12, status.y + 40);
        } else {
            g.drawString("Status", status.x + 12, status.y + 20);
            g.drawString("Select unit/building", status.x + 12, status.y + 40);
        }


        drawProductionPanel(g, status, selectedFactory);
        Rectangle control = getControlPanelRect();
        g.setColor(new Color(28, 28, 28));
        g.fillRect(control.x, control.y, control.width, control.height);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(control.x, control.y, control.width, control.height);
        drawUnitControlGrid(g, control);

        if (selectedBuilding instanceof Barracks) {
            Rectangle cell = getMarineButtonBounds();
            g.setColor(new Color(60, 90, 180));
            g.fillRect(cell.x, cell.y, cell.width, cell.height);
            g.setColor(Color.WHITE);
            g.drawRect(cell.x, cell.y, cell.width, cell.height);
            g.drawString("Marine", cell.x + 6, cell.y + Math.max(14, cell.height / 2 + 4));
        } else if (selectedBuilding instanceof CommandCenter) {
            Rectangle cell = getScvButtonBounds();
            g.setColor(new Color(60, 140, 180));
            g.fillRect(cell.x, cell.y, cell.width, cell.height);
            g.setColor(Color.WHITE);
            g.drawRect(cell.x, cell.y, cell.width, cell.height);
            g.drawString("SCV", cell.x + 10, cell.y + Math.max(14, cell.height / 2 + 4));
        } else if (selectedUnit instanceof SCV) {
            if (scvBuildMenuOpen) {
                Rectangle commandCell = getScvCommandCenterBuildButtonBounds();
                g.setColor(new Color(70, 100, 165));
                g.fillRect(commandCell.x, commandCell.y, commandCell.width, commandCell.height);
                g.setColor(Color.WHITE);
                g.drawRect(commandCell.x, commandCell.y, commandCell.width, commandCell.height);
                g.drawString("CC", commandCell.x + 18, commandCell.y + Math.max(14, commandCell.height / 2 + 4));

                Rectangle barracksCell = getScvBarracksBuildButtonBounds();
                g.setColor(new Color(70, 100, 165));
                g.fillRect(barracksCell.x, barracksCell.y, barracksCell.width, barracksCell.height);
                g.setColor(Color.WHITE);
                g.drawRect(barracksCell.x, barracksCell.y, barracksCell.width, barracksCell.height);
                g.drawString("Rax", barracksCell.x + 11, barracksCell.y + Math.max(14, barracksCell.height / 2 + 4));
            } else {
                Rectangle cell = getScvBuildButtonBounds();
                g.setColor(new Color(70, 100, 165));
                g.fillRect(cell.x, cell.y, cell.width, cell.height);
                g.setColor(Color.WHITE);
                g.drawRect(cell.x, cell.y, cell.width, cell.height);
                g.drawString("Build", cell.x + 7, cell.y + Math.max(14, cell.height / 2 + 4));
            }
        }
    }

    public boolean isBuildPlacementActive() {
        return buildPlacementType != BuildPlacementType.NONE;
    }

    public void cancelBuildPlacement() {
        buildPlacementType = BuildPlacementType.NONE;
        scvBuildMenuOpen = false;
    }

    private int getPlacementPreviewWidth() {
        return switch (buildPlacementType) {
            case COMMAND_CENTER -> CommandCenter.BUILD_WIDTH;
            case BARRACKS -> Barracks.BUILD_WIDTH;
            default -> 0;
        };
    }

    private int getPlacementPreviewHeight() {
        return switch (buildPlacementType) {
            case COMMAND_CENTER -> CommandCenter.BUILD_HEIGHT;
            case BARRACKS -> Barracks.BUILD_HEIGHT;
            default -> 0;
        };
    }

    private Image getPlacementPreviewImage() {
        return switch (buildPlacementType) {
            case COMMAND_CENTER -> previewCommandCenterImage;
            case BARRACKS -> previewBarracksImage;
            default -> null;
        };
    }

    private int getPlacementPreviewOffsetX() {
        return 0;
    }

    private int getPlacementPreviewOffsetY() {
        return 0;
    }
    private Rectangle getPlacementPreviewRect(int worldX, int worldY) {
        int width = getPlacementPreviewWidth();
        int height = getPlacementPreviewHeight();
        int cellSize = terrain.cellSize;
        int rawX = (int) Math.round(worldX - width / 2.0);
        int rawY = (int) Math.round(worldY - height / 2.0);
        int snappedX = (int) Math.round(rawX / (double) cellSize) * cellSize;
        int snappedY = (int) Math.round(rawY / (double) cellSize) * cellSize;
        return new Rectangle(snappedX, snappedY, width, height);
    }


    private ConstructionSite.Type getCurrentConstructionType() {
        return switch (buildPlacementType) {
            case COMMAND_CENTER -> ConstructionSite.Type.COMMAND_CENTER;
            case BARRACKS -> ConstructionSite.Type.BARRACKS;
            default -> null;
        };
    }

    public boolean canPlaceCurrentBuildAt(int worldX, int worldY) {
        if (!isBuildPlacementActive()) return false;

        Rectangle previewRect = getPlacementPreviewRect(worldX, worldY);
        int cellSize = terrain.cellSize;
        int minCellX = Math.max(0, (int) Math.floor(previewRect.x / (double) cellSize));
        int maxCellX = Math.min(terrain.cols - 1, (int) Math.floor((previewRect.x + previewRect.width - 1) / (double) cellSize));
        int minCellY = Math.max(0, (int) Math.floor(previewRect.y / (double) cellSize));
        int maxCellY = Math.min(terrain.rows - 1, (int) Math.floor((previewRect.y + previewRect.height - 1) / (double) cellSize));

        for (int cy = minCellY; cy <= maxCellY; cy++) {
            for (int cx = minCellX; cx <= maxCellX; cx++) {
                Rectangle cellRect = new Rectangle(cx * cellSize, cy * cellSize, cellSize, cellSize);
                Rectangle overlap = previewRect.intersection(cellRect);
                if (overlap.isEmpty()) continue;
                if (isPlacementBlocked(cellRect, cx, cy)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean placeCurrentBuild(int worldX, int worldY) {
        if (!isBuildPlacementActive()) return false;
        Unit selectedUnit = getPrimarySelectedUnit();
        if (!(selectedUnit instanceof SCV scv)) return false;
        ConstructionSite.Type constructionType = getCurrentConstructionType();
        if (constructionType == null) return false;
        if (!canPlaceCurrentBuildAt(worldX, worldY)) return false;

        int cost = ConstructionSite.getBuildCost(constructionType);
        if (getMinerals(scv.team) < cost) {
            return false;
        }

        Rectangle previewRect = getPlacementPreviewRect(worldX, worldY);
        double buildCenterX = previewRect.x + previewRect.width / 2.0;
        double buildCenterY = previewRect.y + previewRect.height / 2.0;
        scv.startConstruction(constructionType, buildCenterX, buildCenterY);
        buildPlacementType = BuildPlacementType.NONE;
        scvBuildMenuOpen = false;
        selectedBuilding = null;
        selectedMineral = null;
        return true;
    }

    public ConstructionSite beginConstructionSite(ConstructionSite.Type type, double centerX, double centerY, int team, SCV builder) {
        if (type == null) return null;

        int cost = ConstructionSite.getBuildCost(type);
        if (!spendMinerals(team, cost)) {
            return null;
        }

        ConstructionSite site = new ConstructionSite(centerX, centerY, team, type);
        site.setBuilder(builder);
        buildings.add(site);
        return site;
    }

    public void finishConstruction(ConstructionSite site, Building completedBuilding) {
        if (site == null || completedBuilding == null) return;
        int index = buildings.indexOf(site);
        if (index < 0) return;
        buildings.set(index, completedBuilding);
        if (selectedBuilding == site) {
            selectedBuilding = completedBuilding;
        }
        SCV builder = site.getBuilder();
        if (builder != null) {
            builder.clearConstructionOrder();
            builder.stop();
        }
    }
    private boolean isPlacementBlocked(Rectangle overlap, int cellX, int cellY) {
        if (cellX < 0 || cellX >= terrain.cols || cellY < 0 || cellY >= terrain.rows) {
            return true;
        }

        for (Building building : buildings) {
            if (building != null && !building.isDestroyed() && building.getBounds().intersects(overlap)) {
                return true;
            }
        }

        for (MineralPatch patch : mineralPatches) {
            if (patch == null || patch.isDepleted()) continue;
            Rectangle patchBounds = new Rectangle(
                    (int) Math.round(patch.getX() - patch.getDrawWidth() / 2.0),
                    (int) Math.round(patch.getY() - patch.getDrawHeight() / 2.0),
                    patch.getDrawWidth(),
                    patch.getDrawHeight()
            );
            if (patchBounds.intersects(overlap)) {
                return true;
            }
        }

        for (Unit unit : units) {
            if (unit instanceof SCV && unit.isSelected) {
                continue;
            }
            if (unit != null && unit.hp > 0 && unit.getSelectionBounds().intersects(overlap)) {
                return true;
            }
        }

        return !terrain.isWalkableCell(cellX, cellY);
    }
    private void drawBuildPlacementPreview(Graphics2D gWorld) {
        if (!isBuildPlacementActive()) return;

        Point mouse = getMousePosition();
        if (mouse == null || isInUiArea(mouse.x, mouse.y)) return;

        int worldX = screenToWorldX(mouse.x);
        int worldY = screenToWorldY(mouse.y);
        Rectangle previewRect = getPlacementPreviewRect(worldX, worldY);

        Image previewImage = getPlacementPreviewImage();
        Composite oldComposite = gWorld.getComposite();
        gWorld.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        if (previewImage != null) {
            gWorld.drawImage(previewImage, previewRect.x, previewRect.y, previewRect.width, previewRect.height, null);
        }
        gWorld.setComposite(oldComposite);

        int cellSize = terrain.cellSize;
        int minCellX = Math.max(0, (int) Math.floor(previewRect.x / (double) cellSize));
        int maxCellX = Math.min(terrain.cols - 1, (int) Math.floor((previewRect.x + previewRect.width - 1) / (double) cellSize));
        int minCellY = Math.max(0, (int) Math.floor(previewRect.y / (double) cellSize));
        int maxCellY = Math.min(terrain.rows - 1, (int) Math.floor((previewRect.y + previewRect.height - 1) / (double) cellSize));

        boolean allClear = true;
        for (int cy = minCellY; cy <= maxCellY; cy++) {
            for (int cx = minCellX; cx <= maxCellX; cx++) {
                Rectangle cellRect = new Rectangle(cx * cellSize, cy * cellSize, cellSize, cellSize);
                Rectangle overlap = previewRect.intersection(cellRect);
                if (overlap.isEmpty()) continue;

                boolean blocked = isPlacementBlocked(overlap, cx, cy);
                if (blocked) {
                    allClear = false;
                    gWorld.setColor(new Color(210, 50, 50, 110));
                } else {
                    gWorld.setColor(new Color(70, 200, 90, 90));
                }
                gWorld.fillRect(overlap.x, overlap.y, overlap.width, overlap.height);
                gWorld.setColor(blocked ? new Color(220, 80, 80, 170) : new Color(120, 230, 140, 150));
                gWorld.drawRect(overlap.x, overlap.y, overlap.width, overlap.height);
            }
        }

        gWorld.setColor(allClear ? new Color(120, 230, 140) : new Color(220, 90, 90));
        gWorld.drawRect(previewRect.x, previewRect.y, previewRect.width, previewRect.height);
    }

    private Image loadUiImage(String path) {
        try {
            return javax.imageio.ImageIO.read(GamePanel.class.getResource(path));
        } catch (Exception e) {
            return null;
        }
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public TerrainGrid getTerrain() {
        return terrain;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D gWorld = (Graphics2D) g.create();
        gWorld.setClip(0, 0, getWidth(), getUiBarTop());
        gWorld.translate(-cameraX, -cameraY);

        for (MineralPatch patch : mineralPatches) {
            if (patch == selectedMineral && !patch.isDepleted()) {
                Stroke oldStroke = gWorld.getStroke();
                gWorld.setColor(Color.YELLOW);
                gWorld.setStroke(new BasicStroke(1f));
                int ovalWidth = (int) Math.round(patch.getDrawWidth() * 1.1);
                int ovalHeight = (int) Math.round(patch.getDrawHeight() * 1.45);
                int ovalX = (int) Math.round(patch.getX() - ovalWidth / 2.0);
                int ovalY = (int) Math.round(patch.getY() - ovalHeight / 2.0 + 3);
                gWorld.drawOval(ovalX, ovalY, ovalWidth, ovalHeight);
                gWorld.setStroke(oldStroke);
            }
            patch.draw(gWorld);
        }

        for (Building building : buildings) {
            if (building == selectedBuilding) {
                Rectangle r = building.getBounds();
                Stroke oldStroke = gWorld.getStroke();
                gWorld.setColor(building.getTeam() == 0 ? Color.GREEN : Color.RED);
                gWorld.setStroke(new BasicStroke(1.0f));
                int ovalWidth = (int) Math.round(r.width * 1.08);
                int ovalHeight = Math.max(16, (int) Math.round(r.height * 0.52));
                int ovalX = (int) Math.round(building.getX() - ovalWidth / 2.0);
                int ovalY = (int) Math.round(building.getY() - ovalHeight / 2.0 + r.height * 0.22);
                gWorld.drawOval(ovalX, ovalY, ovalWidth, ovalHeight);
                gWorld.setStroke(oldStroke);
            }
            building.draw(gWorld);
            if (building == selectedBuilding) {
                starcraft.engine.RenderUtils.drawHealthBar(gWorld, building.getX(), building.getY(), building.getWidth(), building.getHeight(), building.getHp(), building.getMaxHp(), building.getTeam(), false);
            }
        }

        units.sort((u1, u2) -> Double.compare(u1.y, u2.y));
        for (Unit u : units) {
            u.draw(gWorld);
        }

        drawBuildPlacementPreview(gWorld);

        if (inputHandler != null && inputHandler.isDragging && inputHandler.startPoint != null && inputHandler.endPoint != null) {
            gWorld.setColor(Color.GREEN);
            int x = Math.min(inputHandler.startPoint.x, inputHandler.endPoint.x);
            int y = Math.min(inputHandler.startPoint.y, inputHandler.endPoint.y);
            int w = Math.abs(inputHandler.startPoint.x - inputHandler.endPoint.x);
            int h = Math.abs(inputHandler.startPoint.y - inputHandler.endPoint.y);
            gWorld.drawRect(x, y, w, h);
        }

        gWorld.dispose();

        drawTopRightResources(g);
        drawBottomUiBar(g);
    }
}
















