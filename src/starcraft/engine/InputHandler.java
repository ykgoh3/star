package starcraft.engine;

import starcraft.core.GamePanel;
import starcraft.objects.Unit;
import starcraft.objects.buildings.Building;
import starcraft.objects.logic.StopLogic;
import starcraft.objects.resources.MineralPatch;
import starcraft.objects.units.SCV;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class InputHandler extends MouseAdapter implements KeyListener {
    private final GamePanel panel;
    private final ArrayList<Unit> units;

    public Point startPoint, endPoint;
    public boolean isDragging = false;
    private boolean aKeyPressed = false;

    private final Cursor attackCursor;

    public InputHandler(GamePanel panel, ArrayList<Unit> units) {
        this.panel = panel;
        this.units = units;
        this.attackCursor = createAttackCursor();

        panel.setFocusable(true);
        panel.requestFocusInWindow();
        panel.addKeyListener(this);
    }

    private Cursor createAttackCursor() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(1.5F));
        g2d.drawOval(6, 6, 20, 15);
        g2d.fillOval(15, 12, 3, 3);

        g2d.dispose();
        return Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(16, 16), "AttackCursor");
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && panel.handleUiLeftClick(e.getX(), e.getY())) {
            panel.repaint();
            return;
        }

        if (panel.isInUiArea(e.getX(), e.getY())) {
            isDragging = false;
            startPoint = null;
            endPoint = null;
            panel.repaint();
            return;
        }

        int worldX = panel.screenToWorldX(e.getX());
        int worldY = panel.screenToWorldY(e.getY());

        Unit clickedUnit = findClickedUnit(worldX, worldY);
        Building clickedBuilding = panel.findBuildingAtWorld(worldX, worldY);
        MineralPatch clickedMineral = panel.findMineralAtWorld(worldX, worldY);

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (aKeyPressed) {
                for (Unit u : units) {
                    if (!u.isSelected) continue;
                    u.isMoving = true;
                    if (clickedUnit != null) {
                        u.commandState = 1;
                        u.target = clickedUnit;
                        u.manualOrder = true;
                    } else {
                        u.destX = worldX;
                        u.destY = worldY;
                        u.targetX = worldX;
                        u.targetY = worldY;
                        u.commandState = 1;
                        u.target = null;
                        u.manualOrder = false;
                    }
                }
                panel.clearBuildingSelection();
                aKeyPressed = false;
                panel.setCursor(Cursor.getDefaultCursor());
            } else {
                if (clickedBuilding != null) {
                    for (Unit u : units) u.isSelected = false;
                    panel.selectBuilding(clickedBuilding);
                    isDragging = false;
                    startPoint = null;
                    endPoint = null;
                } else {
                    panel.clearBuildingSelection();
                    startPoint = new Point(worldX, worldY);
                    endPoint = new Point(worldX, worldY);
                    isDragging = true;
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            panel.clearBuildingSelection();
            for (Unit u : units) {
                if (!u.isSelected) continue;
                u.isMoving = true;

                if (u instanceof SCV scv && clickedMineral != null) {
                    scv.startMining(clickedMineral, panel.findNearestCommandCenter(scv.x, scv.y, scv.team));
                    continue;
                }

                if (u instanceof SCV scv) {
                    scv.clearHarvestOrder();
                }

                if (clickedUnit != null && clickedUnit.team != u.team) {
                    u.commandState = 1;
                    u.target = clickedUnit;
                    u.manualOrder = true;
                } else {
                    u.destX = worldX;
                    u.destY = worldY;
                    u.targetX = worldX;
                    u.targetY = worldY;
                    u.commandState = 2;
                    u.target = null;
                    u.manualOrder = false;
                }
            }
            aKeyPressed = false;
            panel.setCursor(Cursor.getDefaultCursor());
        }
        panel.repaint();
    }

    private Unit findClickedUnit(int worldX, int worldY) {
        for (Unit u : units) {
            if (u.hp <= 0) continue;
            double dist = vectorMath.getDistance(worldX, worldY, u.x, u.y);
            if (dist <= u.size) {
                return u;
            }
        }
        return null;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && isDragging) {
            isDragging = false;
            selectUnits();
            startPoint = null;
            endPoint = null;
        }
        panel.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isDragging) {
            int clampedScreenY = Math.min(e.getY(), panel.getUiBarTop() - 1);
            int worldX = panel.screenToWorldX(e.getX());
            int worldY = panel.screenToWorldY(Math.max(0, clampedScreenY));
            endPoint = new Point(worldX, worldY);
        }
        panel.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_S) {
            for (Unit u : units) {
                if (u.isSelected) {
                    StopLogic.execute(u);
                    u.commandState = 0;
                }
            }
        } else if (e.getKeyCode() == KeyEvent.VK_A) {
            aKeyPressed = true;
            panel.setCursor(attackCursor);
        }
        panel.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void selectUnits() {
        if (startPoint == null || endPoint == null) return;
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(startPoint.x - endPoint.x);
        int height = Math.abs(startPoint.y - endPoint.y);
        Rectangle dragRect = new Rectangle(x, y, width, height);

        if (width < 5 && height < 5) {
            for (Unit u : units) u.isSelected = false;
            for (Unit u : units) {
                if (Math.sqrt(Math.pow(u.x - endPoint.x, 2) + Math.pow(u.y - endPoint.y, 2)) < u.size) {
                    u.isSelected = true;
                    panel.clearBuildingSelection();
                    break;
                }
            }
        } else {
            boolean friendFound = false;
            for (Unit u : units) {
                Rectangle unitRect = new Rectangle((int) u.x - u.size / 2, (int) u.y - u.size / 2, u.size, u.size);

                if (u.team == 0 && dragRect.intersects(unitRect)) {
                    u.isSelected = true;
                    friendFound = true;
                } else {
                    u.isSelected = false;
                }
            }
            if (!friendFound) {
                for (Unit u : units) {
                    Rectangle unitRect = new Rectangle((int) u.x - u.size / 2, (int) u.y - u.size / 2, u.size, u.size);
                    if (u.team != 0 && dragRect.intersects(unitRect)) {
                        u.isSelected = true;
                        break;
                    }
                }
            }
            if (friendFound) {
                panel.clearBuildingSelection();
            }
        }
    }
}
