package starcraft.core;

import starcraft.engine.InputHandler;
import starcraft.engine.vectorMath;
import starcraft.objects.Unit;
import starcraft.objects.units.Hydralisk;
import starcraft.objects.units.Marine;
import starcraft.objects.units.Zergling;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel {
    private final ArrayList<Unit> units = new ArrayList<>();
    private InputHandler inputHandler;
    private TerrainGrid terrain;
    private Timer gameLoop;

    public GamePanel() {
        setBackground(Color.BLACK);
        terrain = new TerrainGrid(800, 600, 20);
        inputHandler = new InputHandler(this, units);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);

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

        gameLoop = new Timer(33, e -> updateGame());
        gameLoop.start();
    }

    private void updateGame() {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);

            // Auto-acquire only nearby enemies. Otherwise keep target null while idle.
            if (!u.manualOrder) {
                u.target = findNearestEnemyInRange(u, u.range + 20);
            } else if (u.target != null && u.target.hp <= 0 && u.target.deathTimer <= 290) {
                u.target = null;
                u.manualOrder = false;
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        units.sort((u1, u2) -> Double.compare(u1.y, u2.y));

        for (Unit u : units) {
            u.draw(g);
        }

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
