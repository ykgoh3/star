package starcraft.engine;

import starcraft.core.GamePanel;
import starcraft.objects.logic.StopLogic;
import starcraft.objects.Unit;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class InputHandler extends MouseAdapter implements KeyListener {
    private GamePanel panel;
    private ArrayList<Unit> units;

    public Point startPoint, endPoint;
    public boolean isDragging = false;
    private boolean aKeyPressed = false;

    // [추가] A키 어택땅 전용 마우스 커서
    private Cursor attackCursor;

    public InputHandler(GamePanel panel, ArrayList<Unit> units) {
        this.panel = panel;
        this.units = units;

        // [추가] 커스텀 어택 커서 생성 로직 호출
        this.attackCursor = createAttackCursor();

        panel.setFocusable(true);
        panel.requestFocusInWindow();
        panel.addKeyListener(this);
    }

    // [추가] 초록색 동그라미에 점이 있는 어택 커서를 그리는 메서드
    private Cursor createAttackCursor() {
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(1.5F)); // 원의 굵기
        g2d.drawOval(6, 6, 20, 15); // 바깥쪽 초록색 동그라미
        g2d.fillOval(15, 12, 3, 3);   // 안쪽 초록색 점

        g2d.dispose();

        // 정중앙(16, 16)을 클릭 포인트로 하는 커서 생성
        return Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(16, 16), "AttackCursor");
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Unit clickedUnit = null;
        for (Unit u : units) {
            if (u.hp > 0) {
                double dist = vectorMath.getDistance(e.getX(), e.getY(), u.x, u.y);
                if (dist <= u.size) {
                    clickedUnit = u;
                    break;
                }
            }
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            if (aKeyPressed) {
                for (Unit u : units) {
                    if (u.isSelected) {
                        u.isMoving = true;
                        if (clickedUnit != null) {
                            u.commandState = 1;
                            u.target = clickedUnit;
                            u.manualOrder = true;
                        } else {
                            u.destX = e.getX(); u.destY = e.getY();
                            u.targetX = e.getX(); u.targetY = e.getY();
                            u.commandState = 1;
                            u.target = null;
                            u.manualOrder = false;
                        }
                    }
                }
                // [수정] 명령 하달 완료 후 A키 상태 해제 및 기본 커서로 복구
                aKeyPressed = false;
                panel.setCursor(Cursor.getDefaultCursor());
            } else {
                startPoint = e.getPoint();
                endPoint = e.getPoint();
                isDragging = true;
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            for (Unit u : units) {
                if (u.isSelected) {
                    u.isMoving = true;
                    if (clickedUnit != null && clickedUnit.team != u.team) {
                        u.commandState = 1;
                        u.target = clickedUnit;
                        u.manualOrder = true;
                    } else {
                        u.destX = e.getX(); u.destY = e.getY();
                        u.targetX = e.getX(); u.targetY = e.getY();
                        u.commandState = 2;
                        u.target = null;
                        u.manualOrder = false;
                    }
                }
            }
            // [수정] 우클릭 시에도 A명령 상태 취소 및 기본 커서 복구
            aKeyPressed = false;
            panel.setCursor(Cursor.getDefaultCursor());
        }
        panel.repaint();
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
        if (isDragging) endPoint = e.getPoint();
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
            // [수정] A키 누를 시 초록색 타겟팅 커서로 변경
            aKeyPressed = true;
            panel.setCursor(attackCursor);
        }
        panel.repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}

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
                    u.isSelected = true; break;
                }
            }
        } else {
            boolean friendFound = false;
            for (Unit u : units) {
                // [해결] 중심점이 아니라, 유닛의 크기만큼 생성된 가상의 사각형(unitRect)이 드래그 박스와 스치기만 해도(intersects) 선택되게 함
                Rectangle unitRect = new Rectangle((int)u.x - u.size/2, (int)u.y - u.size/2, u.size, u.size);

                if (u.team == 0 && dragRect.intersects(unitRect)) {
                    u.isSelected = true; friendFound = true;
                } else u.isSelected = false;
            }
            if (!friendFound) {
                for (Unit u : units) {
                    Rectangle unitRect = new Rectangle((int)u.x - u.size/2, (int)u.y - u.size/2, u.size, u.size);
                    if (u.team != 0 && dragRect.intersects(unitRect)) { u.isSelected = true; break; }
                }
            }
        }
    }
}
