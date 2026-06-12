package DotsBoxes.player.ai;

import java.util.ArrayList;
import java.util.List;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;

/**
 ** Implémentation de Minimax avec élagage Alpha-Beta à profondeur limitée.
 */


public class AlphaBetaActionStrategy implements ActionStrategy {

    private final int maxDepth;
    private final AlphaBetaPruningObserver observer;
    private final NodeCounterObserver nodeCounter;

    public AlphaBetaActionStrategy(int maxDepth, AlphaBetaPruningObserver observer, NodeCounterObserver nodeCounter) {
        this.maxDepth = maxDepth;
        this.observer = observer;
        this.nodeCounter = nodeCounter;
    }

    // --- Heuristique (identique à MinimaxActionStrategy) ---

    private List<int[]> getVoisins(Board board, int r, int c) {
        int rows = board.getRows() - 1;
        int cols = board.getCols() - 1;
        List<int[]> voisins = new ArrayList<>();
        int[][] candidats = {{r-1,c},{r+1,c},{r,c-1},{r,c+1}};
        for (int[] v : candidats) {
            if (v[0] >= 0 && v[0] < rows && v[1] >= 0 && v[1] < cols)
                voisins.add(v);
        }
        return voisins;
    }

    private int countSides(Board board, int r, int c) {
        int sides = 0;
        if (board.isHEdgeSet(r, c))     sides++;
        if (board.isHEdgeSet(r + 1, c)) sides++;
        if (board.isVEdgeSet(r, c))     sides++;
        if (board.isVEdgeSet(r, c + 1)) sides++;
        return sides;
    }

    private boolean potentialChaine(Board board, int r, int c) {
        if (countSides(board, r, c) == 2) {
            for (int[] v : getVoisins(board, r, c))
                if (countSides(board, v[0], v[1]) == 3) return true;
            return false;
        }
        return false;
    }

    private boolean adjacentToTwoSided(Board board, int r, int c) {
        for (int[] v : getVoisins(board, r, c))
            if (countSides(board, v[0], v[1]) == 2) return true;
        return false;
    }

    private int parcoursChaine(Board board, int r, int c, boolean[][] visited) {
        int count = 1;
        List<int[]> voisins = getVoisins(board, r, c);
        int i = 0;
        while (i < voisins.size()) {
            int[] v = voisins.get(i++);
            if (!visited[v[0]][v[1]] && countSides(board, v[0], v[1]) == 2) {
                visited[v[0]][v[1]] = true;
                count++;
                voisins.clear();
                voisins.addAll(getVoisins(board, v[0], v[1]));
                i = 0;
            }
        }
        return count;
    }

    private double evaluate(Board board, boolean isMaxPlayer) {
        double score = board.getScore(0) - board.getScore(1);
        double signe = isMaxPlayer ? +1.0 : -1.0;

        int rows = board.getRows() - 1;
        int cols = board.getCols() - 1;
        boolean[][] visited = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sides = countSides(board, r, c);
                if (sides == 3 && !adjacentToTwoSided(board, r, c)) {
                    visited[r][c] = true;
                    score += signe;
                } else if (sides == 2 && !potentialChaine(board, r, c)) {
                    score += 0.25;
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!visited[r][c] && countSides(board, r, c) == 3) {
                    visited[r][c] = true;
                    score += signe;
                    for (int[] v : getVoisins(board, r, c)) {
                        if (!visited[v[0]][v[1]] && countSides(board, v[0], v[1]) == 2) {
                            visited[v[0]][v[1]] = true;
                            score += signe * parcoursChaine(board, v[0], v[1], visited);
                        }
                    }
                }
            }
        }

        return score;
    }

    // --- Alpha-Beta ---

    private record ABResult(Action action, double value) {}

    private ABResult alphaBeta(Board board, int depth, double alpha, double beta, boolean isMaxPlayer) {
        nodeCounter.increment();
        observer.incrementNodeCount();

        if (board.isFinished()) {
            return new ABResult(null, board.getScore(0) - board.getScore(1));
        }
        if (depth == 0) {
            return new ABResult(null, evaluate(board, isMaxPlayer));
        }

        List<Action> actions = board.getAvailableActions();

        if (isMaxPlayer) {
            double max = Double.NEGATIVE_INFINITY;
            Action best = null;
            for (Action a : actions) {
                Board copie = new Board(board);
                int closed = copie.apply(a, 0);
                // rejeu si fermeture de case
                boolean stayMax = closed > 0;
                double val = alphaBeta(copie, depth - 1, alpha, beta, stayMax).value();
                if (val > max) { max = val; best = a; }
                if (max > alpha) alpha = max;
                if (alpha >= beta) { observer.incrementBetaCut(); break; }
            }
            return new ABResult(best, max);
        } else {
            double min = Double.POSITIVE_INFINITY;
            Action best = null;
            for (Action a : actions) {
                Board copie = new Board(board);
                int closed = copie.apply(a, 1);
                boolean stayMin = closed > 0;
                double val = alphaBeta(copie, depth - 1, alpha, beta, !stayMin).value();
                if (val < min) { min = val; best = a; }
                if (min < beta) beta = min;
                if (alpha >= beta) { observer.incrementAlphaCut(); break; }
            }
            return new ABResult(best, min);
        }
    }

    @Override
    public Action selectAction(Board board, int playerId) {
        List<Action> actions = board.getAvailableActions();
        if (actions.isEmpty()) return null;

        observer.reset();
        nodeCounter.reset();

        return alphaBeta(board, maxDepth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, playerId == 0).action();
    }

    @Override
    public String getName() {
        return "Alpha-Beta";
    }
}
