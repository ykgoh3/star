package starcraft.main;

import starcraft.core.GamePanel;

import javax.swing.*;

public class StarMain {
    public static void main(String[] args) {
        JFrame frame = new JFrame(); //게임을 듬을 빈 윈도우 창 만들기
        GamePanel panel = new GamePanel(); //GamePanel 생성
        frame.add(panel); //윈도우창에 panel넣기

        frame.setBounds(0,0,800,600);
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);
    }
}
