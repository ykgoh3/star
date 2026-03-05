package starcraft.objects.units;

import starcraft.core.GamePanel;
import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Zergling extends Unit {
    public Zergling(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 35; this.hp = 350; this.damage = 5;
        this.attackDelay = 15; this.range = 30; this.speed = 3.5; this.size = 18;
        this.image = loadImage("/starcraft/res/zergling.png");
    }

    @Override
    public void attack(GamePanel panel) {
        // 공격하기 전 타겟의 체력을 기억해 둡니다.
        int prevHp = (target != null) ? target.hp : 0;

        // 부모(Unit)의 기본 공격 로직을 먼저 수행합니다.
        super.attack(panel);

        // [해결 1] 저글링의 공격이 성공해 적의 체력이 깎였다면, 적에게 생긴 피격 이펙트 타이머를 즉시 지워버립니다!
        if (target != null && target.hp < prevHp) {
            target.hitEffectTimer = 0;
        }
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            // [해결 2] 짝수/홀수 진동을 삭제하고, 타이머가 3~4일 때는 찌르고(4.0) 1~2일 때는 원위치(0.0)로 한 번만 타격감을 줍니다.
            double pushOffset = (attackEffectTimer > 2) ? 4.0 : 0.0;

            int drawX = (int) (x + Math.cos(lookAngle) * pushOffset);
            int drawY = (int) (y + Math.sin(lookAngle) * pushOffset);

            if (image != null) {
                RenderUtils.drawRotatedImage(g, image, drawX, drawY, size, targetX, targetY);
            } else {
                g.setColor(team == 0 ? Color.ORANGE : Color.MAGENTA);
                g.fillOval(drawX - size / 2, drawY - size / 2, size, size);
            }
        }
    }

    // [해결 3] Zergling 전용 drawHitEffect 오버라이딩을 완전히 삭제했습니다.
    // 이제 저글링이 맞을 때도 부모 클래스(Unit.java)에 있는 마린의 총알 스파크(동심원)가 정상적으로 출력됩니다.

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(205, 50, 50, 150));
            g.fillOval((int)x - 15, (int)y - 8, 25, 10);
            g.fillOval((int)x - 5, (int)y - 12, 15, 5);
            return; // 여기서 종료
        }

        if (attackEffectTimer <= 0) {
            if (image != null) {
                double lookX = isMoving ? targetX : (target != null ? target.x : x + 1);
                double lookY = isMoving ? targetY : (target != null ? target.y : y);
                RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
            } else {
                g.setColor(team == 0 ? Color.ORANGE : Color.MAGENTA);
                g.fillOval((int)x - size / 2, (int)y - size / 2, size, size);
            }
        }

        drawAttackEffect(g, isMoving ? currentAngle : (target != null ? Math.atan2(target.y - y, target.x - x) : currentAngle));
        drawHitEffect(g); // 이제 다시 큰 원 안의 작은 원(총알 피격)이 그려집니다.
        drawHealthBar(g);
    }
}