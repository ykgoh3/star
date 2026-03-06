package starcraft.objects.buildings;

import starcraft.core.GamePanel;

import java.awt.*;

public abstract class Building {
    protected final int team;
    protected final int width;
    protected final int height;
    protected double x;
    protected double y;
    protected int hp;
    protected int maxHp;

    protected Building(double x, double y, int team, int width, int height, int maxHp) {
        this.x = x;
        this.y = y;
        this.team = team;
        this.width = width;
        this.height = height;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public int getTeam() {
        return team;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getPathingPadding() {
        return 0;
    }

    public boolean isDestroyed() {
        return hp <= 0;
    }

    public abstract void update(GamePanel panel);

    public abstract void draw(Graphics g);
}
