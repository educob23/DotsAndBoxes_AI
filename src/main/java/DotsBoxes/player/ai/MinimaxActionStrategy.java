package DotsBoxes.player.ai;

import java.util.ArrayList;
import java.util.List;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

/*
 * Implémentation de Minimax avec profondeur limite. On tient en compte que le joueur rejoue s'il ferme une casse.
 * On a implémenté une heuristique pour calculer un score plus précis sur les feuilles
*/
public class MinimaxActionStrategy implements ActionStrategy {

    private final int maxDepth;

    private List<int[]> getVoisins(Board board, int r, int c) {
        //donne les voisins d'une casse
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
        // dit le nombre de côtés de la casse
        int sides = 0;
        if (board.isHEdgeSet(r, c)) sides++;
        if (board.isHEdgeSet(r + 1, c)) sides++;
        if (board.isVEdgeSet(r, c)) sides++;
        if (board.isVEdgeSet(r, c + 1)) sides++;
        return sides;
    }

    private boolean potentialChaine(Board board, int r, int c) {
        // dit si la casse est adjacente à une casse de trois côtés et donc potentiellement fait partie d'une châine
        if (countSides(board, r, c) == 2) {
            List<int[]> voisins = getVoisins(board, r, c);
            for (int[] v : voisins) {
                if (countSides(board, v[0], v[1]) == 3) return true;
            }
            return false;
        } else {
            return false;
        }
    }

    private boolean adjacentToTwoSided(Board board, int r, int c) {
        // dit si la casse est adjacente à une casse de deux côtés
        List<int[]> voisins = getVoisins(board, r, c);
        for (int[] v : voisins) {
            if (countSides(board, v[0], v[1]) == 2) return true;
        }
        return false;
    }

    private int parcoursChaine(Board board, int r, int c, boolean[][] visited) {
        // retourne la taille de la chaîne commencée à la casse (r, c) qui est la première casse de la chaîne
        int count = 1;
        List<int[]> voisins = getVoisins(board, r, c);
        int i = 0;
        while(i < voisins.size()) {
            int[] v = voisins.get(i);
            i ++;
            if (!visited[v[0]][v[1]] && countSides(board, v[0], v[1]) == 2){
                visited[v[0]][v[1]] = true;
                count ++;
                voisins.clear();
                voisins.addAll(getVoisins(board, v[0], v[1]));
                i = 0;
            }
        }
        return count;

    }

    private double evaluate(Board board, boolean isMaxPlayer){
        double score = board.getScore(0) - board.getScore(1);

        // si c'est le tour du joueur 0 avoir des casses à gagner lui est bénéfique, sinon elles sont mauvaises
        double signeHeuristique = isMaxPlayer ? +1.0 : -1.0;

        int rows = board.getRows() - 1;
        int cols = board.getCols() - 1;
        boolean[][] visited = new boolean[rows][cols];

        // on attribue des points aux cases de 2 et 3 côtés
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sides = countSides(board, r, c);
                if (sides == 3 && !adjacentToTwoSided(board, r, c)) {
                    visited[r][c] = true;
                    score += signeHeuristique * 1;
                } else if (sides == 2 && !potentialChaine(board, r, c)) {
                    // l'adversaire peut faire une erreur est donner en cadeau une case si elle est de 2 côtés donc une case de 2côtés est légerement mieux 
                    score += 0.25;
                }
            }
        }

        // on reparcourt pour pénaliser les chaînes
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int sides = countSides(board, r, c);
                if (!visited[r][c] && sides == 3) {
                    visited[r][c] = true;
                    score += signeHeuristique * 1;
                    List<int[]> voisins = getVoisins(board, r, c);
                    for (int[] v : voisins) {
                        if(!visited[v[0]][v[1]] && countSides(board, v[0], v[1]) == 2) {
                            visited[v[0]][v[1]] = true;
                            score += signeHeuristique * parcoursChaine(board, v[0], v[1], visited);
                        }
                    }
                }
            }
        }

        return score;
    }

    // on va créer une classe record pour que minmax nou renvoie les actions aussi 
    private record minmaxResult(Action action, double value) {}

    public MinimaxActionStrategy(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    private minmaxResult minmax(Board board, int maxDepth, boolean isMaxPlayer){
        // si la partie est finie on renvoi le score exact car on le connait
        if (board.isFinished()){
            return new minmaxResult(null, board.getScore(0) - board.getScore(1));
        }
        // mais si on atteint la profondeur max on va utiliser l'heuristique
        if (maxDepth == 0){
            return new minmaxResult(null, evaluate(board, isMaxPlayer));
        }

        // on récupère les actions valides 
        List<Action> actionsValides = board.getAvailableActions();

        if (isMaxPlayer){
            // on applique la recherche du max si on est sur un niveau max
            double max = Double.NEGATIVE_INFINITY;
            Action meilleure = null;
            for(Action a : actionsValides){
                Board copie = new Board(board);
                int nbClosed = copie.apply(a, 0);
                if (nbClosed > 0){
                    double minmax = minmax(copie, maxDepth -1, true).value();
                    if (max < minmax){
                        max = minmax;
                        meilleure = a;
                    }
                } else {
                    double minmax = minmax(copie, maxDepth -1, false).value();
                    if (max < minmax){
                        max = minmax;
                        meilleure = a;
                    }
                }
            }
            return new minmaxResult(meilleure, max);
        } else {
            // recherche du min si un niveau min            
            double min = Double.POSITIVE_INFINITY;
            Action meilleure = null;
            for(Action a : actionsValides){
                Board copie = new Board(board);
                int nbClosed = copie.apply(a, 1);
                if (nbClosed > 0){
                    double minmax = minmax(copie, maxDepth -1, false).value();
                    if (min > minmax){
                        min = minmax;
                        meilleure = a;
                    }
                } else {
                    double minmax = minmax(copie, maxDepth -1, true).value();
                    if (min > minmax){
                        min = minmax;
                        meilleure = a;
                    }
                }
            }
            return new minmaxResult(meilleure, min);
        }
    }

    @Override
    public Action selectAction(Board board, int playerId) {
        // on récupère les actions valides 
        List<Action> actionsValides = board.getAvailableActions();

        // s'il en a pas on renvoi null
        if (actionsValides.isEmpty()) {
            return null;
        }

        // comme minmax donne l'action qui maximise ou qui minimise selon le cas on peut l'extraire directement
        return minmax(board, this.maxDepth, playerId == 0).action();
    }

    @Override
    public String getName() {
        return "Minimax";
    }
}
