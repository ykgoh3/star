package starcraft.objects.units;

import starcraft.engine.RenderUtils;
import starcraft.objects.Unit;

import java.awt.*;

public class Marine extends Unit {

    public Marine(int x, int y, int team) {
        super(x, y, team);
        this.maxHp = 40;
        this.hp = 40; // [수정] 400에서 스타 1 실제 데이터인 40으로 수정
        this.damage = 6;
        this.attackDelay = 15; // Fastest 기준 15프레임 최적화 [cite: 2026-02-27]
        this.range = 140;
        this.speed = 2.2;
        this.size = 20;
        this.image = loadImage("/starcraft/res/marine.png");
    }

    @Override
    protected void drawAttackEffect(Graphics g, double lookAngle) {
        if (attackEffectTimer > 0) {
            // [수정] 총구 화염 크기를 4x4로 작게 축소
            int flashX = (int)(x + Math.cos(lookAngle) * (size * 0.8));
            int flashY = (int)(y + Math.sin(lookAngle) * (size * 0.8));

            g.setColor(new Color(255, 255, 150));
            g.fillOval(flashX - 2, flashY - 2, 4, 4);
        }
    }

    @Override
    public void draw(Graphics g) {
        if (hp <= 0) {
            g.setColor(new Color(139, 0, 0, 150)); // 반투명 다크레드
            g.fillOval((int)x - 10, (int)y - 5, 20, 10);
            return; // 여기서 종료하여 본체와 체력바는 그리지 않음
        }
        // 1. [복구] 유닛이 바라보는 각도에 맞춰 본체 및 이미지 렌더링
        double lookAngle = isMoving ? currentAngle : (target != null ? Math.atan2(target.y - y, target.x - x) : currentAngle);

        if (image != null) {
            double lookX = x + Math.cos(lookAngle);
            double lookY = y + Math.sin(lookAngle);
            RenderUtils.drawRotatedImage(g, image, x, y, size, lookX, lookY);
        } else {
            g.setColor(team == 0 ? Color.BLUE : Color.RED);
            g.fillOval((int)x - size / 2, (int)y - size / 2, size, size);
        }

        // 2. 이펙트 및 인터페이스 출력
        drawAttackEffect(g, lookAngle);
        drawHitEffect(g); // Unit에서 구현한 이중 원 피격 효과 호출
        drawHealthBar(g);
    }
}