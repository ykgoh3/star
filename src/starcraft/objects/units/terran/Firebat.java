package starcraft.objects.units.terran;

import starcraft.core.GamePanel;
import starcraft.engine.RenderUtils;
import starcraft.engine.vectorMath;
import starcraft.objects.Attackable;
import starcraft.objects.Unit;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class Firebat extends Unit {
    private static final double FLAME_LENGTH = 62.0;
    private static final double NEAR_HALF_WIDTH = 19.0;
    private static final double FAR_HALF_WIDTH = 13.0;
    private static final double MIN_SPLASH_MULTIPLIER = 0.25;

    public Firebat(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 50;
        this.hp = 50;
        this.armor = 1;
        this.damage = 8;
        this.attackDelay = 22;
        this.range = 32;
        this.speed = 2.1;
        this.size = 19;
        this.drawWidth = 19;
        this.drawHeight = 20;
        this.image = null;
        this.image = loadImage("/starcraft/res/firebat.png");
    }

    @Override
    public void attack(GamePanel panel) {
        if (commandState == 2) return;
        if (Math.abs(velX) > 0.05 || Math.abs(velY) > 0.05) return;
        if (attackTimer > 0 || target == null) return;
        if (!target.isAlive()) return;

        double dist = vectorMath.getDistance(x, y, target.getTargetX(), target.getTargetY());
        if (dist > range + 5) return;

        double lookAngle = Math.atan2(target.getTargetY() - y, target.getTargetX() - x);
        double forwardX = Math.cos(lookAngle);
        double forwardY = Math.sin(lookAngle);

        attackTimer = attackDelay;
        postAttackDelayTimer = 8;
        attackEffectTimer = 5;

        Set<Unit> hitUnits = new HashSet<>();
        applyFireDamage(target, damage, panel, hitUnits);

        for (Unit enemy : panel.getUnits()) {
            if (enemy == null || enemy == this || enemy.hp <= 0 || enemy.team == team) continue;
            if (hitUnits.contains(enemy)) continue;

            double dx = enemy.x - x;
            double dy = enemy.y - y;
            double forward = dx * forwardX + dy * forwardY;
            if (forward < 3.0 || forward > FLAME_LENGTH) continue;

            double widthRatio = Math.min(1.0, forward / FLAME_LENGTH);
            double halfWidth = lerp(NEAR_HALF_WIDTH, FAR_HALF_WIDTH, widthRatio) + enemy.size * 0.2;
            double lateral = Math.abs(-dx * forwardY + dy * forwardX);
            if (lateral > halfWidth) continue;

            double forwardFalloff = 1.0 - Math.min(1.0, forward / FLAME_LENGTH) * 0.35;
            double lateralFalloff = 1.0 - Math.min(1.0, lateral / Math.max(1.0, halfWidth)) * 0.6;
            double multiplier = Math.max(MIN_SPLASH_MULTIPLIER, forwardFalloff * lateralFalloff);
            int splashDamage = Math.max(1, (int) Math.round(damage * multiplier));
            applyFireDamage(enemy, splashDamage, panel, hitUnits);
        }
    }

    private void applyFireDamage(Attackable victim, int amount, GamePanel panel, Set<Unit> hitUnits) {
        if (victim == null || amount <= 0 || !victim.isAlive()) return;

        int hpBefore = victim.getHp();
        victim.receiveDamage(amount);
        if (hpBefore > 0 && victim.getHp() <= 0) {
            killCount++;
        }

        if (victim instanceof Unit unitVictim) {
            hitUnits.add(unitVictim);
            unitVictim.triggerHitEffect(new Color(255, 170, 60), 2, 5);
            if (unitVictim.getHp() > 0 && unitVictim.getTeam() != this.team) {
                if (canAutoRetaliate(unitVictim)) {
                    unitVictim.target = this;
                    unitVictim.manualOrder = false;
                    unitVictim.commandState = 1;
                    unitVictim.isMoving = false;
                    unitVictim.destX = unitVictim.x;
                    unitVictim.destY = unitVictim.y;
                    unitVictim.autoRetaliating = true;
                }
                alertNearbyAllies(unitVictim, panel);
            }
        } else {
            victim.triggerHitEffect(new Color(255, 170, 60), 2, 5);
        }
    }

    private double lerp(double start, double end, double ratio) {
        return start + (end - start) * ratio;
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            Graphics2D g2 = (Graphics2D) g.create();
            int baseX = (int) Math.round(x + Math.cos(lookAngle) * (size * 0.65));
            int baseY = (int) Math.round(y + Math.sin(lookAngle) * (size * 0.65));

            g2.rotate(lookAngle, baseX, baseY);

            g2.setColor(new Color(255, 110, 25, 170));
            g2.fillRoundRect(baseX - 1, baseY - 10, 28, 8, 8, 8);
            g2.fillRoundRect(baseX - 1, baseY + 2, 28, 8, 8, 8);

            g2.setColor(new Color(255, 175, 65, 210));
            g2.fillRoundRect(baseX + 5, baseY - 9, 23, 6, 6, 6);
            g2.fillRoundRect(baseX + 5, baseY + 3, 23, 6, 6, 6);

            g2.setColor(new Color(255, 235, 150, 195));
            g2.fillRoundRect(baseX + 11, baseY - 8, 14, 4, 4, 4);
            g2.fillRoundRect(baseX + 11, baseY + 4, 14, 4, 4, 4);

            g2.dispose();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(139, 0, 0, 150));
            g.fillOval((int) x - 11, (int) y - 5, 22, 10);
            return;
        }

        double lookAngle = getLookAngle();
        int bodyW = Math.max(1, (int) Math.round(getDrawWidth() * 0.9));
        int bodyH = Math.max(1, (int) Math.round(getDrawHeight() * 0.95));
        int drawX = (int) Math.round(x - bodyW / 2.0);
        int drawY = (int) Math.round(y - bodyH / 2.0);

        Graphics2D g2 = (Graphics2D) g.create();
        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g2, image, x, y, bodyW, bodyH, lookX, lookY);
        } else {
            g2.setColor(team == 0 ? new Color(190, 90, 40) : new Color(180, 60, 60));
            g2.fillRoundRect(drawX, drawY, bodyW, bodyH, 6, 6);
            g2.setColor(new Color(40, 40, 40));
            g2.drawRoundRect(drawX, drawY, bodyW, bodyH, 6, 6);
            g2.setColor(new Color(255, 180, 70));
            g2.fillRect(drawX + bodyW / 2 - 2, drawY + 3, 4, bodyH - 6);
        }
        g2.dispose();

        drawAttackEffect(g, lookAngle);
        drawHitEffect(g);
        drawHealthBar(g);
    }
}



