package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;
import DotsBoxes.player.Player;
import DotsBoxes.player.ai.AlphaBetaActionStrategy;
import DotsBoxes.player.ai.MctsActionStrategy;
import DotsBoxes.player.ai.MinimaxActionStrategy;
import DotsBoxes.player.automate.AutomatePlayer;

/**
 * Comparaison expérimentale de MCTS face à Minimax et Alpha-Beta.
 *
 * approche: 
 * 
 *  Budget temps égal— chaque algorithme dispose du même temps
 *       par coup. On calibre d'abord l'itération MCTS et la profondeur
 *       Minimax/Alpha-Beta qui correspondent au même budget, puis on
 *       fait jouer les configurations calibrées l'une contre l'autre.
 *   
 *
 * Usage :
 * 
 *   mvn compile exec:java -Dexec.mainClass="DotsBoxes.ComparaisonMcts"
 * </pre>
 */
public class ComparaisonMcts {

    // -------------------------------------------------------------------
    // Paramètres globaux
    // -------------------------------------------------------------------

    private static final int  GAMES       = 30;   // parties par match
    private static final long BUDGET_MS   = 500;  // budget temps par coup (ms)
    private static final int  ROWS        = 4;
    private static final int  COLS        = 4;

    // -------------------------------------------------------------------
    // Utilitaires
    // -------------------------------------------------------------------

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

    /**
     * Résultat d'un match aller-retour entre deux stratégies.
     * On alterne qui commence à chaque partie pour neutraliser l'avantage
     * du premier joueur.
     *
     * retourne int[] { victoires strat1, victoires strat2, nuls }
     */
    private static int[] runMatch(int rows, int cols, int games,
                                  ActionStrategy strat1, ActionStrategy strat2) {
        int w1 = 0, w2 = 0, d = 0;
        for (int g = 0; g < games; g++) {
            Player p0, p1;
            // Alterne : jeux pairs → strat1 commence, jeux impairs → strat2 commence
            if (g % 2 == 0) {
                p0 = new AutomatePlayer(0, strat1);
                p1 = new AutomatePlayer(1, strat2);
            } else {
                p0 = new AutomatePlayer(0, strat2);
                p1 = new AutomatePlayer(1, strat1);
            }
            int diff = playGame(rows, cols, p0, p1);
            // diff = score(p0) - score(p1) → ramener au point de vue de strat1
            int margin = (g % 2 == 0) ? diff : -diff;
            if (margin > 0) w1++; else if (margin < 0) w2++; else d++;
        }
        return new int[]{w1, w2, d};
    }

    private static void printMatchResult(String name1, String name2, int[] res, int games) {
        System.out.printf("  %-22s vs %-22s : %d V / %d D / %d N  (%.0f%% / %.0f%% / %.0f%%)%n",
                name1, name2,
                res[0], res[1], res[2],
                100.0 * res[0] / games,
                100.0 * res[1] / games,
                100.0 * res[2] / games);
    }

    // -------------------------------------------------------------------
    // Axe 1 : Calibration du budget temps
    // -------------------------------------------------------------------

    /**
     * Mesure combien d'itérations MCTS et quelle profondeur Minimax/AlphaBeta
     * s'exécutent en un temps ≤ 1000 ms sur un plateau vierge.
     *
     * rows lignes
     *  cols colonnes
     * retourne int[] { itersMcts, depthMM, depthAB }
     */
    private static int[] calibrate(int rows, int cols) {
        Board board = new Board(rows, cols);

        // --- Calibrage MCTS : doublement jusqu'au dépassement ---
        int iters = 50;
        while (true) {
            long t = System.nanoTime();
            new MctsActionStrategy(iters).selectAction(board, 0);
            long ms = (System.nanoTime() - t) / 1_000_000;
            if (ms > BUDGET_MS || iters > 200_000) break;
            iters *= 2;
        }
        int calibIters = Math.max(iters / 2, 50); //on revient en arrière parce qu'on a dépassé

        // --- Calibrage Minimax ---
        int mmDepth = 1;
        while (mmDepth <= 15) {
            long t = System.nanoTime();
            new MinimaxActionStrategy(mmDepth).selectAction(board, 0);
            long ms = (System.nanoTime() - t) / 1_000_000;
            if (ms > BUDGET_MS) { mmDepth--; break; }
            mmDepth++;
        }
        mmDepth = Math.max(mmDepth, 1);

        // --- Calibrage AlphaBeta ---
        int abDepth = 1;
        while (abDepth <= 15) {
            long t = System.nanoTime();
            new AlphaBetaActionStrategy(abDepth, new AlphaBetaPruningObserver(),
                    new NodeCounterObserver()).selectAction(board, 0);
            long ms = (System.nanoTime() - t) / 1_000_000;
            if (ms > BUDGET_MS) { abDepth--; break; }
            abDepth++;
        }
        abDepth = Math.max(abDepth, 1);

        return new int[]{calibIters, mmDepth, abDepth};
    }

    private static void compareBudgetEqual(int rows, int cols) {
        System.out.printf("%n=== Budget-temps égal (%dms/coup) — plateau %dx%d ===%n",
                BUDGET_MS, rows, cols);

        int[] calib = calibrate(rows, cols);
        int iters   = calib[0];
        int mmDepth = calib[1];
        int abDepth = calib[2];

        System.out.printf("  Calibration : MCTS=%d iter | Minimax=prof.%d | Alpha-Beta=prof.%d%n",
                iters, mmDepth, abDepth);

        // Noms affichables
        String nameMcts = "MCTS(" + iters + ")";
        String nameMM   = "Minimax(d=" + mmDepth + ")";
        String nameAB   = "AlphaBeta(d=" + abDepth + ")";

        // Match MCTS vs Minimax
        int[] r1 = runMatch(rows, cols, GAMES,
                new MctsActionStrategy(iters),
                new MinimaxActionStrategy(mmDepth));
        printMatchResult(nameMcts, nameMM, r1, GAMES);

        // Match MCTS vs AlphaBeta
        int[] r2 = runMatch(rows, cols, GAMES,
                new MctsActionStrategy(iters),
                new AlphaBetaActionStrategy(abDepth,
                        new AlphaBetaPruningObserver(), new NodeCounterObserver()));
        printMatchResult(nameMcts, nameAB, r2, GAMES);

    }

   
    // -------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------

    public static void main(String[] args) {
        
        compareBudgetEqual(ROWS, COLS);


        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("Analyse terminée.");
    }
}
