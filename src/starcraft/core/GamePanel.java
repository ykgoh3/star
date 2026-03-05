package starcraft.core;

import starcraft.objects.Unit;
import starcraft.objects.units.Marine;
import starcraft.engine.vectorMath;
import starcraft.engine.InputHandler;
import starcraft.objects.units.Zergling;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class GamePanel extends JPanel {
    private ArrayList<Unit> units = new ArrayList<>();
    private InputHandler inputHandler;
    private TerrainGrid terrain;
    private Timer gameLoop;

    public GamePanel() {
        setBackground(Color.BLACK);
        terrain = new TerrainGrid(800, 600, 20);
        inputHandler = new InputHandler(this, units);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);

        // 초기 유닛 생성

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                // 간격을 30에서 25로 좁혀 유닛들이 더 촘촘하게 붙어있게 함
                int x = 150 + col * 25 - row * 25;
                int y = 200 - col * 25 - row * 25;
                units.add(new Marine(x, y, 0));
            }
        }

        // [해결] 1팀 (남동쪽): 0팀과 대치하는 대각선 3열 4마리 밀집 배치 (총 12마리) [cite: 2026-02-28]
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                int x = 550 + col * 25 + row * 25;
                int y = 500 - col * 25 + row * 25;

                Zergling aiMarine = new Zergling(x, y, 1);

                // --- AI 초기 명령 주입 ---
//                aiMarine.commandState = 1;    // 1: 어택땅(Attack-Move) 상태로 시작
//                aiMarine.destX = 150;         // 플레이어(0팀) 기지 X 좌표
//                aiMarine.destY = 200;         // 플레이어(0팀) 기지 Y 좌표
//                aiMarine.targetX = 150;
//                aiMarine.targetY = 200;
//                aiMarine.isMoving = true;     // 이동 활성화
                // -----------------------

                units.add(aiMarine);
            }
        }


        gameLoop = new Timer(33, e -> updateGame());
        gameLoop.start();
    }

    private void updateGame() {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);

            // [수정] 동적 타겟팅 로직: 수동 명령 상태가 아니라면 매 순간 가장 가까운 적을 탐색합니다.
            // 만약 특정 적을 우클릭했다면(manualOrder), 그 타겟이 죽기 전까지는 변경하지 않습니다. [cite: 2026-02-27]
            if (!u.manualOrder || u.target == null || (u.target.hp <= 0 && u.target.deathTimer <= 290)) {
                Unit nearest = findNearestEnemy(u);

                // 이동 중에도 더 가까운 적이 발견되면 타겟을 즉시 교체합니다.
                if (nearest != null) {
                    u.target = nearest;
                } else if (u.target != null && u.target.hp <= 0) {
                    u.target = null;
                    u.manualOrder = false;
                }
            }

            // [추가] 체력이 0 이하인 유닛은 행동을 멈추고 핏자국 타이머만 감소
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

    private Unit findNearestEnemy(Unit me) {
        Unit closest = null;
        double minDist = Double.MAX_VALUE;
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

        // [검토 반영] Y좌표 기준으로 정렬하여 아래쪽 유닛이 위쪽 유닛을 덮게 함 (원근감)
        units.sort((u1, u2) -> Double.compare(u1.y, u2.y));

        for (Unit u : units) {
            u.draw(g);
        }

        // 드래그 박스 렌더링
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