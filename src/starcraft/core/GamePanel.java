package starcraft.core;

import starcraft.engine.InputHandler;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;
import starcraft.objects.buildings.Barracks;
import starcraft.objects.buildings.Building;
import starcraft.objects.buildings.CommandCenter;
import starcraft.objects.units.Hydralisk;
import starcraft.objects.units.Marine;
import starcraft.objects.units.Zergling;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    public static final int BASE_WIDTH = 800;
    public static final int BASE_HEIGHT = 600;
    public static final int UI_BAR_HEIGHT = 150;

    private static final int WORLD_WIDTH = 1800;
    private static final int WORLD_HEIGHT = 1200;
    private static final int EDGE_SCROLL_THRESHOLD = 20;
    private static final int EDGE_SCROLL_SPEED = 8;

    private final ArrayList<Unit> units = new ArrayList<>();
    private final ArrayList<Building> buildings = new ArrayList<>();
    private final InputHandler inputHandler;
    private final TerrainGrid terrain;
    private final Timer gameLoop;

    private Building selectedBuilding;
    private int cameraX = 0;
    private int cameraY = 0;
    private final Robot mouseLockRobot;
    private boolean mouseLockEnabled = true;

    public GamePanel() {
        setBackground(Color.BLACK);
        terrain = new TerrainGrid(WORLD_WIDTH, WORLD_HEIGHT, 20);
        inputHandler = new InputHandler(this, units);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);

        buildings.add(new CommandCenter(50,200,0));
        buildings.add(new Barracks(110, 110, 0));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int x = 150 + col * 25 - row * 25;
                int y = 200 - col * 25 - row * 25;
                units.add(new Marine(x, y, 0));
            }
        }

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
                boolean targetDead = (u.target != null) && (u.target.hp <= 0);
                boolean noValidTarget = (u.target == null) || targetDead;

                if (u.autoRetaliating) {
                    if (noValidTarget) {
                        Unit nearest = findNearestEnemyInRange(u, u.range + 200);
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
                    Unit nearest = findNearestEnemyInRange(u, u.range + 20);
                    u.target = nearest;

                    if (nearest != null) {
                        u.commandState = 1;
                        u.destX = u.x;
                        u.destY = u.y;
                    }
                } else if (u.commandState == 1 && noValidTarget) {
                    Unit nearest = findNearestEnemyInRange(u, u.range + 20);
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
            } else if (u.target != null && u.target.hp <= 0) {
                u.target = null;
            }

            if (u.hp <= 0) {
                u.deathTimer--;
                continue;
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
            if (building.isDestroyed()) continue;
            terrain.blockRectWorld(
                    building.getX(),
                    building.getY(),
                    building.getWidth(),
                    building.getHeight(),
                    building.getPathingPadding()
            );
        }
    }

    private Unit findNearestEnemyInRange(Unit me, double maxRange) {
        Unit closest = null;
        double minDist = maxRange;

        for (Unit enemy : units) {
            if (enemy.team != me.team && enemy.hp > 0) {
                double d = vectorMath.getDistance(me.x, me.y, enemy.x, enemy.y);
                if (d < minDist) {
                    minDist = d;
                    closest = enemy;
                }
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
        int gap = 10;
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

    public Building findBuildingAtWorld(int worldX, int worldY) {
        for (int i = buildings.size() - 1; i >= 0; i--) {
            Building b = buildings.get(i);
            if (!b.isDestroyed() && b.getBounds().contains(worldX, worldY)) {
                return b;
            }
        }
        return null;
    }

    public void selectBuilding(Building building) {
        this.selectedBuilding = building;
    }

    public void clearBuildingSelection() {
        this.selectedBuilding = null;
    }

    public boolean handleUiLeftClick(int mouseX, int mouseY) {
        if (!isInUiArea(mouseX, mouseY)) return false;
        if (!(selectedBuilding instanceof Barracks barracks)) return false;

        Rectangle marineButton = getMarineButtonBounds();
        if (marineButton.contains(mouseX, mouseY)) {
            barracks.enqueueMarine();
            return true;
        }
        return false;
    }

    private Rectangle getMarineButtonBounds() {
        Rectangle control = getControlPanelRect();
        return new Rectangle(control.x + 12, control.y + control.height / 2 - 14, control.width - 24, 30);
    }

    private void drawTopRightResources(Graphics g) {
        Rectangle r = getResourceRect();
        g.setColor(new Color(20, 20, 20, 200));
        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(new Color(140, 140, 140));
        g.drawRect(r.x, r.y, r.width, r.height);
        g.setColor(Color.WHITE);
        g.drawString("Minerals: 0   Gas: 0", r.x + 12, r.y + 21);
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
        g.setColor(new Color(28, 28, 28));
        g.fillRect(status.x, status.y, status.width, status.height);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(status.x, status.y, status.width, status.height);
        g.setColor(Color.WHITE);
        if (selectedBuilding instanceof Barracks barracks) {
            g.drawString("Barracks", status.x + 12, status.y + 20);
            g.drawString("Queue: " + barracks.getQueuedUnits(), status.x + 12, status.y + 40);
        } else {
            g.drawString("Status", status.x + 12, status.y + 20);
            g.drawString("Select unit/building", status.x + 12, status.y + 40);
        }

        Rectangle control = getControlPanelRect();
        g.setColor(new Color(28, 28, 28));
        g.fillRect(control.x, control.y, control.width, control.height);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(control.x, control.y, control.width, control.height);
        g.setColor(Color.WHITE);
        g.drawString("Control", control.x + 12, control.y + 20);

        if (selectedBuilding instanceof Barracks) {
            Rectangle marineButton = getMarineButtonBounds();
            g.setColor(new Color(60, 90, 180));
            g.fillRect(marineButton.x, marineButton.y, marineButton.width, marineButton.height);
            g.setColor(Color.WHITE);
            g.drawRect(marineButton.x, marineButton.y, marineButton.width, marineButton.height);
            g.drawString("Train Marine", marineButton.x + 36, marineButton.y + 20);
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

        for (Building building : buildings) {
            building.draw(gWorld);
            if (building == selectedBuilding) {
                Rectangle r = building.getBounds();
                gWorld.setColor(Color.YELLOW);
                gWorld.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
            }
        }

        units.sort((u1, u2) -> Double.compare(u1.y, u2.y));
        for (Unit u : units) {
            u.draw(gWorld);
        }

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