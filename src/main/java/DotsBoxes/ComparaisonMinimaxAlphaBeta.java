package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.Player;
import DotsBoxes.player.ai.AlphaBetaActionStrategy;
import DotsBoxes.player.ai.MinimaxActionStrategy;
import DotsBoxes.player.automate.AutomatePlayer;

import java.util.List;

/**
 * Comparaison expérimentale entre Minimax et Alpha-Beta (question 4, partie 3).
 *
 * Quatre axes mesurés :
 * 
 *   Nombre de nœuds explorés à profondeur fixe
 *   Temps de calcul (ms) à profondeur fixe
 *   Profondeur maximale atteignable sous contrainte de temps
 *   Qualité des décisions (tournoi : Minimax vs Alpha-Beta)
 * 
 *
 * Usage :
 * 
 *   mvn compile exec:java -Dexec.mainClass="DotsBoxes.ComparaisonMinimaxAlphaBeta"
 * 
 */
public class ComparaisonMinimaxAlphaBeta {

    // -------------------------------------------------------------------
    // Paramètres globaux
    // -------------------------------------------------------------------

    /** Nombre de coups mesurés par configuration. */
    private static final int SAMPLE_MOVES = 6;

    /** Budget temps pour la recherche de profondeur maximale (ms). */
    private static final long TIME_BUDGET_MS = 500;

    /** Nombre de parties par match pour la qualité des décisions. */
    private static final int GAMES_PER_MATCH = 20;

    // -------------------------------------------------------------------
    // 1. Nœuds explorés à profondeur fixe
    // -------------------------------------------------------------------

    /**
     * Compare le nombre de nœuds explorés par Minimax et Alpha-Beta
     * sur les premiers 6 coups d'une partie, à profondeur fixe.
     *
     *  rows  lignes du plateau
     * cols  colonnes du plateau
     *  depth profondeur de recherche
     */
    private static void compareNodes(int rows, int cols, int depth) {
        System.out.printf("%n=== Nœuds explorés — plateau %dx%d — profondeur %d ===%n", rows, cols, depth);
        System.out.printf("%-6s  %-12s  %-12s  %-12s  %-10s%n",
                "Coup", "MM nœuds", "AB nœuds", "Coupes α+β", "Réduction");

        Board board = new Board(rows, cols);
        int player = 0;

        for (int move = 1; move <= SAMPLE_MOVES && !board.isFinished(); move++) {

            // --- Minimax ---
            // MinimaxActionStrategy n'expose pas d'observateur : on compte les nœuds
            // via notre propre implémentation récursive instrumentée (countMinimaxNodes),
            // et on joue le coup via l'implémentation officielle.
            int mmNodes = countMinimaxNodes(board, depth, player);
            Action mmAction = new MinimaxActionStrategy(depth).selectAction(board, player);

            // --- Alpha-Beta ---
            AlphaBetaPruningObserver abObs  = new AlphaBetaPruningObserver();
            NodeCounterObserver      abCnt  = new NodeCounterObserver();
            AlphaBetaActionStrategy  ab     = new AlphaBetaActionStrategy(depth, abObs, abCnt);
            ab.selectAction(board, player);
            int abNodes = abCnt.getCount();
            int cuts    = abObs.getAlphaCutCount() + abObs.getBetaCutCount();

            double reduction = mmNodes > 0 ? 100.0 * (mmNodes - abNodes) / mmNodes : 0;

            System.out.printf("%-6d  %-12d  %-12d  %-12d  %.1f%%%n",
                    move, mmNodes, abNodes, cuts, reduction);

            // Avance le plateau (on joue le coup de Minimax pour les deux)
            if (mmAction != null && board.isValid(mmAction)) {
                int closed = board.apply(mmAction, player);
                if (closed == 0) player = 1 - player;
            } else {
                break;
            }
        }
    }

    /**
     * Compte le nombre de nœuds explorés par Minimax via une implémentation
     * locale instrumentée (Minimax standard sans élagage).
     */
    private static int countMinimaxNodes(Board board, int depth, int playerId) {
        int[] counter = {0};
        minimaxCount(board, depth, playerId == 0, counter);
        return counter[0];
    }

    private static double minimaxCount(Board board, int depth, boolean isMax, int[] counter) {
        counter[0]++;
        if (board.isFinished()) return board.getScore(0) - board.getScore(1);
        if (depth == 0)         return board.getScore(0) - board.getScore(1);

        List<Action> actions = board.getAvailableActions();
        if (isMax) {
            double best = Double.NEGATIVE_INFINITY;
            for (Action a : actions) {
                Board copy   = new Board(board);
                int closed   = copy.apply(a, 0);
                boolean next = closed > 0;
                best = Math.max(best, minimaxCount(copy, depth - 1, next, counter));
            }
            return best;
        } else {
            double best = Double.POSITIVE_INFINITY;
            for (Action a : actions) {
                Board copy   = new Board(board);
                int closed   = copy.apply(a, 1);
                boolean next = closed == 0;
                best = Math.min(best, minimaxCount(copy, depth - 1, next, counter));
            }
            return best;
        }
    }

    // -------------------------------------------------------------------
    // 2. Temps de calcul à profondeur fixe
    // -------------------------------------------------------------------

    /**
     * Mesure le temps moyen  de sélection d'un coup pour Minimax et Alpha-Beta
     * sur un plateau vierge, pour plusieurs profondeurs.
     *
     * rows lignes
     *  cols colonnes
     */
    private static void compareTimes(int rows, int cols) {
        System.out.printf("%n=== Temps de calcul — plateau %dx%d ===%n", rows, cols);
        System.out.printf("%-10s  %-14s  %-14s  %-10s%n",
                "Prof.", "Minimax (ms)", "Alpha-Beta (ms)", "Accélération");

        for (int depth = 1; depth <= 6; depth++) {
            Board board = new Board(rows, cols);

            // Minimax
            long t0 = System.nanoTime();
            for (int i = 0; i < 3; i++) new MinimaxActionStrategy(depth).selectAction(board, 0);
            long mmMs = (System.nanoTime() - t0) / 3 / 1_000_000;

            // Alpha-Beta
            t0 = System.nanoTime();
            for (int i = 0; i < 3; i++)
                new AlphaBetaActionStrategy(depth, new AlphaBetaPruningObserver(), new NodeCounterObserver())
                        .selectAction(board, 0);
            long abMs = (System.nanoTime() - t0) / 3 / 1_000_000;

            String speedup = (abMs > 0) ? String.format("x%.1f", (double) mmMs / abMs) : "N/A";

            System.out.printf("%-10d  %-14d  %-14d  %-10s%n", depth, mmMs, abMs, speedup);

            // Arrête dès que Minimax dépasse 30 s (évite de bloquer)
            if (mmMs > 30_000) {
                System.out.println("  [Minimax trop lent au-delà, arrêt anticipé]");
                break;
            }
        }
    }

    // -------------------------------------------------------------------
    // 3. Profondeur maximale atteignable sous contrainte de temps
    // -------------------------------------------------------------------

    /**
     * Cherche la profondeur maximale que chaque algorithme peut atteindre
     * en restant sous 500 millisecondes par coup.
     *
     * rows lignes
     *  cols colonnes
     */
    private static void compareMaxDepth(int rows, int cols) {
        System.out.printf("%n=== Profondeur maximale sous %dms — plateau %dx%d ===%n",
                TIME_BUDGET_MS, rows, cols);

        Board board = new Board(rows, cols);

        int mmMax = maxDepthUnderBudget(board, false);
        int abMax = maxDepthUnderBudget(board, true);

        System.out.printf("Minimax    : profondeur max = %d%n", mmMax);
        System.out.printf("Alpha-Beta : profondeur max = %d%n", abMax);
        if (abMax > mmMax)
            System.out.printf("→ Alpha-Beta atteint %d niveaux de plus dans le même budget.%n", abMax - mmMax);
    }

    private static int maxDepthUnderBudget(Board board, boolean useAB) {
        int depth = 1;
        while (depth <= 20) {
            long t0 = System.nanoTime();
            if (useAB) {
                new AlphaBetaActionStrategy(depth, new AlphaBetaPruningObserver(), new NodeCounterObserver())
                        .selectAction(board, 0);
            } else {
                new MinimaxActionStrategy(depth).selectAction(board, 0);
            }
            long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
            if (elapsedMs > TIME_BUDGET_MS) return depth - 1;
            depth++;
        }
        return depth - 1;
    }

    // -------------------------------------------------------------------
    // 4. Qualité des décisions (tournoi)
    // -------------------------------------------------------------------

    /**
     * Fait jouer Minimax contre Alpha-Beta (et inversement) sur 20
     * parties, à différentes profondeurs, et affiche les résultats.
     *
     *  rows lignes
     * cols colonnes
     */
    private static void compareQuality(int rows, int cols) {
        System.out.printf("%n=== Qualité des décisions — plateau %dx%d (%d parties) ===%n",
                rows, cols, GAMES_PER_MATCH);
        System.out.printf("%-14s  %-14s  %-8s  %-8s  %-8s  %-12s%n",
                "MM depth", "AB depth", "V(MM)", "V(AB)", "Egal.", "Marge moy.");

        int[] depths = {2, 3, 4};
        for (int mmDepth : depths) {
            for (int abDepth : depths) {
                int wMM = 0, wAB = 0, draws = 0;
                int totalMargin = 0;

                for (int g = 0; g < GAMES_PER_MATCH; g++) {
                    // On alterne qui commence pour neutraliser l'avantage du premier joueur
                    Player p0, p1;
                    if (g % 2 == 0) {
                        p0 = new AutomatePlayer(0, new MinimaxActionStrategy(mmDepth));
                        p1 = new AutomatePlayer(1, new AlphaBetaActionStrategy(abDepth,
                                new AlphaBetaPruningObserver(), new NodeCounterObserver()));
                    } else {
                        p0 = new AutomatePlayer(0, new AlphaBetaActionStrategy(abDepth,
                                new AlphaBetaPruningObserver(), new NodeCounterObserver()));
                        p1 = new AutomatePlayer(1, new MinimaxActionStrategy(mmDepth));
                    }

                    int diff = playGame(rows, cols, p0, p1);
                    // diff = score(joueur0) - score(joueur1) ; on ramène au point de vue MM
                    int mmMargin = (g % 2 == 0) ? diff : -diff;

                    if (mmMargin > 0) { wMM++; totalMargin += mmMargin; }
                    else if (mmMargin < 0) { wAB++; totalMargin += mmMargin; }
                    else draws++;
                }

                double avgMargin = (double) totalMargin / GAMES_PER_MATCH;
                System.out.printf("%-14d  %-14d  %-8d  %-8d  %-8d  %+.2f%n",
                        mmDepth, abDepth, wMM, wAB, draws, avgMargin);
            }
        }
    }

    /** Joue une partie complète et retourne score(p0) − score(p1). */
    private static int playGame(int rows, int cols, Player p0, Player p1) {
        Board board    = new Board(rows, cols);
        Player current = p0, other = p1;
        int s0 = 0, s1 = 0;

        while (!board.isFinished()) {
            Action a = current.getAction(board);
            if (a == null || !board.isValid(a)) {
                if (current.getId() == 0) s0--; else s1--;
                Player tmp = current; current = other; other = tmp;
                continue;
            }
            int closed = board.apply(a, current.getId());
            if (closed > 0) {
                if (current.getId() == 0) s0 += closed; else s1 += closed;
            } else {
                Player tmp = current; current = other; other = tmp;
            }
        }
        return s0 - s1;
    }

  
    // -------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------

    public static void main(String[] args) {

        // --- Axe 1 : nœuds explorés ---
        compareNodes(3, 3, 3);
        compareNodes(4, 4, 3);

        // --- Axe 2 : temps de calcul ---
        compareTimes(3, 3);
        compareTimes(4, 4);

        // --- Axe 3 : profondeur max sous budget ---
        compareMaxDepth(3, 3);
        compareMaxDepth(4, 4);

        // --- Axe 4 : qualité des décisions ---
        compareQuality(3, 3);
        compareQuality(4, 4);


    

        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("Analyse terminée.");
    }
}
