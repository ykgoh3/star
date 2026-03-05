package starcraft.engine;

import starcraft.core.TerrainGrid;
import java.awt.Point;
import java.util.*;

public class PathFinder {
    // 8방향 이동 비용 (직선 1.0, 대각선 1.4)
    private static final double[][] DIRS = {{1,0,1}, {-1,0,1}, {0,1,1}, {0,-1,1}, {1,1,1.4}, {1,-1,1.4}, {-1,1,1.4}, {-1,-1,1.4}};

    public static List<Point> findPath(TerrainGrid terrain, Point start, Point goal, Set<Long> blockedCells, int maxVisit) {
        if (start.equals(goal)) return Collections.singletonList(goal);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<Integer, Double> closed = new HashMap<>();

        open.add(new Node(start.x, start.y, 0, dist(start, goal), null));

        int visited = 0;
        while (!open.isEmpty() && visited++ < maxVisit) {
            Node curr = open.poll();
            if (curr.x == goal.x && curr.y == goal.y) return rebuildPath(curr);

            int hash = curr.x * 1000 + curr.y;
            if (closed.getOrDefault(hash, Double.MAX_VALUE) <= curr.g) continue;
            closed.put(hash, curr.g);

            for (double[] d : DIRS) {
                int nx = curr.x + (int)d[0], ny = curr.y + (int)d[1];

                // [수정] 지형이 이동 가능하고, blockedCells(멈춘 유닛)에 포함되지 않은 경우만 이동
                long cellHash = (long)nx << 32 | (ny & 0xFFFFFFFFL);
                if (terrain.isWalkableCell(nx, ny) && (blockedCells == null || !blockedCells.contains(cellHash))) {
                    double ng = curr.g + d[2];
                    open.add(new Node(nx, ny, ng, ng + dist(new Point(nx, ny), goal), curr));
                }
            }
        }
        return Collections.emptyList();
    }

    private static double dist(Point a, Point b) { return Math.sqrt(Math.pow(a.x-b.x, 2) + Math.pow(a.y-b.y, 2)); }

    private static List<Point> rebuildPath(Node node) {
        LinkedList<Point> path = new LinkedList<>();
        while (node != null) { path.addFirst(new Point(node.x, node.y)); node = node.parent; }
        return path;
    }

    private static class Node {
        int x, y; double g, f; Node parent;
        Node(int x, int y, double g, double f, Node p) { this.x = x; this.y = y; this.g = g; this.f = f; this.parent = p; }
    }
}