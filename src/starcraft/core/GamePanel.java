package starcraft.core;

import starcraft.engine.InputHandler;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;
import starcraft.objects.buildings.Barracks;
import starcraft.objects.buildings.Building;
import starcraft.objects.units.Hydralisk;
import starcraft.objects.units.Marine;
import starcraft.objects.units.Zergling;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {
    private final ArrayList<Unit> units = new ArrayList<>();
    private final ArrayList<Building> buildings = new ArrayList<>();
    private InputHandler inputHandler;
    private TerrainGrid terrain;
    private Timer gameLoop;

    private Building selectedBuilding;

    public GamePanel() {
        setBackground(Color.BLACK);
        terrain = new TerrainGrid(800, 600, 20);
        inputHandler = new InputHandler(this, units);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);

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
                int y = 500 - col * 25 + row * 25;

                if (row == 0) {
                    units.add(new Hydralisk(x, y, 1));
                } else {
                    units.add(new Zergling(x, y, 1));
                }
            }
        }

        refreshBuildingBlockers();

        gameLoop = new Timer(33, e -> updateGame());
        gameLoop.start();
    }

    private void updateGame() {
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

    public Building findBuildingAt(int x, int y) {
        for (int i = buildings.size() - 1; i >= 0; i--) {
            Building b = buildings.get(i);
            if (!b.isDestroyed() && b.getBounds().contains(x, y)) {
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
        if (!(selectedBuilding instanceof Barracks barracks)) return false;

        Rectangle marineButton = getMarineButtonBounds();
        if (marineButton.contains(mouseX, mouseY)) {
            barracks.enqueueMarine();
            return true;
        }
        return false;
    }

    private Rectangle getMarineButtonBounds() {
        int panelX = getWidth() - 180;
        int panelY = getHeight() - 100;
        return new Rectangle(panelX + 15, panelY + 40, 150, 30);
    }

    private void drawBuildingUI(Graphics g) {
        if (!(selectedBuilding instanceof Barracks barracks)) return;

        int panelX = getWidth() - 180;
        int panelY = getHeight() - 100;

        g.setColor(new Color(20, 20, 20, 220));
        g.fillRect(panelX, panelY, 170, 85);
        g.setColor(new Color(120, 120, 120));
        g.drawRect(panelX, panelY, 170, 85);

        g.setColor(Color.WHITE);
        g.drawString("Barracks", panelX + 12, panelY + 18);
        g.drawString("Queue: " + barracks.getQueuedUnits(), panelX + 95, panelY + 18);

        Rectangle marineButton = getMarineButtonBounds();
        g.setColor(new Color(60, 90, 180));
        g.fillRect(marineButton.x, marineButton.y, marineButton.width, marineButton.height);
        g.setColor(Color.WHITE);
        g.drawRect(marineButton.x, marineButton.y, marineButton.width, marineButton.height);
        g.drawString("Train Marine", marineButton.x + 36, marineButton.y + 20);
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

        for (Building building : buildings) {
            building.draw(g);
            if (building == selectedBuilding) {
                Rectangle r = building.getBounds();
                g.setColor(Color.YELLOW);
                g.drawRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
            }
        }

        units.sort((u1, u2) -> Double.compare(u1.y, u2.y));

        for (Unit u : units) {
            u.draw(g);
        }

        drawBuildingUI(g);

        if (inputHandler != null && inputHandler.isDragging && inputHandler.startPoint != null && inputHandler.endPoint != null) {
            g.setColor(Color.GREEN);
            int x = Math.min(inputHandler.startPoint.x, inputHandler.endPoint.x);
            int y = Math.min(inputHandler.startPoint.y, inputHandler.endPoint.y);
            int w = Math.abs(inputHandler.startPoint.x - inputHandler.endPoint.x);
            int h = Math.abs(inputHandler.startPoint.y - inputHandler.endPoint.y);
            g.drawRect(x, y, w, h);
        }
    }
}