package starcraft.core;

import java.awt.Point;
//1. 모듈화 확실히 하기 (미리 기능별로 카테고리 분류) 별X30000개 -> 이거안할경우 하나고치면 하나망가지고 그거고치면 저거망가지고 저거고치면 이거망가진다
//2. 기능 별로 세션을 구분하는것도 괜찮다 (로그가 너무 많이 쌓이면 치매온다)
//3. 기능 구현후 최적화하기 (기능별로 미리 안하면 나중에 한번에 하기 힘들다)
//4. 버전 관리하기 (뭘 했는지, e.g. 스팀 업데이트목록)

//git status = 추적 상태보기
//빨간색 = 비추적
//초록색 = 추적
//주황색 = gitignore에서 항상 추적 제외시키기
//파란색 = 마지막 커밋했을때와 코드가 다름
//
//git add <file> = 특정 파일 추적
//git add . = 추적 제외시킨 파일빼고 모든 파일 추적
//
//git rm --cached <file> = 추적한 특정 파일 비추적
//git restore --staged <file> = 실수로 추적한 파일 되돌리기
//
//git commit -m "커밋할이름" = 세이브파일 만들기 (여러개 추가할 수 있음)
//git commit -a -m "커밋할이름" = 달라진 내용 add따로 안하고 한번에 커밋하기 (새로 만든거는 비추)
//
//git diff = 커밋 후 코드 수정했을때 차이점 (add하면 diff에 안뜸)
//git diff --staged = 커밋 후 코드 수정하고 add한 내용 차이점
//
//git branch 이름 = 새로운 루트 만들기
//git checkout 이름 = HEAD를 그 루트로 이동하기
//
//콘솔창에 : 이거뜨면 q누르고 나갈 수 있음
//콘솔창에 > 이거뜨면 Ctrl + c 로 나갈 수 있음

public class TerrainGrid {
    public final int cellSize, cols, rows;
    private final boolean[][] blocked;

    public TerrainGrid(int w, int h, int size) {
        this.cellSize = size;
        this.cols = w / size;
        this.rows = h / size;
        this.blocked = new boolean[rows][cols];
    }

    public boolean isWalkableCell(int x, int y) {
        return x >= 0 && x < cols && y >= 0 && y < rows && !blocked[y][x];
    }

    public Point worldToCell(double x, double y) {
        return new Point(clamp((int)(x/cellSize), 0, cols-1), clamp((int)(y/cellSize), 0, rows-1));
    }

    public double cellCenterX(int cx) { return cx * cellSize + cellSize / 2.0; }
    public double cellCenterY(int cy) { return cy * cellSize + cellSize / 2.0; }
    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
}

