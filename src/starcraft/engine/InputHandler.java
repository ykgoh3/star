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

import java.util.Comparator;
public class InputHandler extends MouseAdapter implements KeyListener {
    private final GamePanel panel;
    private static final int DOUBLE_CLICK_SELECT_LIMIT = 12;
    private static final double DOUBLE_CLICK_SELECT_RADIUS = 140.0;
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
                panel.clearSelections();
                aKeyPressed = false;
                panel.setCursor(Cursor.getDefaultCursor());
            } else {
                if (clickedBuilding != null) {
                    for (Unit u : units) u.isSelected = false;
                    panel.selectBuilding(clickedBuilding);
                    isDragging = false;
                    startPoint = null;
                    endPoint = null;
                } else if (clickedMineral != null) {
                    for (Unit u : units) u.isSelected = false;
                    panel.selectMineral(clickedMineral);
                    isDragging = false;
                    startPoint = null;
                    endPoint = null;
                } else if (clickedUnit != null && e.getClickCount() >= 2) {
                    selectNearbySameTypeUnits(clickedUnit);
                    isDragging = false;
                    startPoint = null;
                    endPoint = null;
                } else {
                    panel.clearSelections();
                    startPoint = new Point(worldX, worldY);
                    endPoint = new Point(worldX, worldY);
                    isDragging = true;
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            panel.clearSelections();
            for (Unit u : units) {
                if (!u.isSelected) continue;
                u.isMoving = true;

                if (u instanceof SCV scv && clickedMineral != null) {
                    clickedMineral.triggerCommandRing();
                    scv.startMining(clickedMineral, panel.findNearestCommandCenter(scv.x, scv.y, scv.team), panel);
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
            if (u.containsPoint(worldX, worldY)) {
                return u;
            }
        }
        return null;
    }

    private void selectNearbySameTypeUnits(Unit clickedUnit) {
        if (clickedUnit == null || clickedUnit.hp <= 0) return;

        panel.clearSelections();
        for (Unit u : units) {
            u.isSelected = false;
        }

        if (clickedUnit.team != 0) {
            clickedUnit.isSelected = true;
            return;
        }

        ArrayList<Unit> matches = new ArrayList<>();
        for (Unit u : units) {
            if (u.hp <= 0) continue;
            if (u.team != clickedUnit.team) continue;
            if (u.getClass() != clickedUnit.getClass()) continue;

            double dist = vectorMath.getDistance(clickedUnit.x, clickedUnit.y, u.x, u.y);
            if (dist <= DOUBLE_CLICK_SELECT_RADIUS) {
                matches.add(u);
            }
        }

        matches.sort(Comparator.comparingDouble(u ->
                vectorMath.getDistance(clickedUnit.x, clickedUnit.y, u.x, u.y)));

        int limit = Math.min(DOUBLE_CLICK_SELECT_LIMIT, matches.size());
        for (int j = 0; j < limit; j++) {
            matches.get(j).isSelected = true;
        }
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
                    if (u instanceof SCV scv) {
                        scv.clearHarvestOrder();
                    }
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

        panel.clearSelections();

        if (width < 5 && height < 5) {
            for (Unit u : units) u.isSelected = false;
            for (Unit u : units) {
                if (u.containsPoint(endPoint.x, endPoint.y)) {
                    u.isSelected = true;
                    break;
                }
            }
        } else {
            ArrayList<Unit> friendlyMatches = new ArrayList<>();
            ArrayList<Unit> enemyMatches = new ArrayList<>();

            for (Unit u : units) {
                u.isSelected = false;
                if (u.hp <= 0) continue;

                Rectangle unitRect = u.getSelectionBounds();
                if (!dragRect.intersects(unitRect)) continue;

                if (u.team == 0) {
                    friendlyMatches.add(u);
                } else {
                    enemyMatches.add(u);
                }
            }

            if (!friendlyMatches.isEmpty()) {
                friendlyMatches.sort(Comparator.comparingDouble(u -> vectorMath.getDistance(endPoint.x, endPoint.y, u.x, u.y)));
                int limit = Math.min(DOUBLE_CLICK_SELECT_LIMIT, friendlyMatches.size());
                for (int j = 0; j < limit; j++) {
                    friendlyMatches.get(j).isSelected = true;
                }
            } else if (!enemyMatches.isEmpty()) {
                enemyMatches.sort(Comparator.comparingDouble(u -> vectorMath.getDistance(endPoint.x, endPoint.y, u.x, u.y)));
                enemyMatches.get(0).isSelected = true;
            }
        }
    }
}
