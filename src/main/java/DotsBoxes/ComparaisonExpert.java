package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.player.ActionStrategy;
import DotsBoxes.player.Player;
import DotsBoxes.player.ai.AlphaBetaActionStrategy;
import DotsBoxes.player.ai.ExpertActionStrategy;
import DotsBoxes.player.automate.AutomatePlayer;

/**
 * Comparaison Expert vs Alpha-Beta sur plusieurs tailles de plateau.
 *
 * Pour chaque configuration (taille × profondeur AB), on joue {10 GAMES}
 * parties aller-retour avec contrainte de 1 seconde par coup.
 * On mesure : victoires, défaites, nuls, score moyen, et temps moyen par coup.
 *
 * Usage :
 *   mvn compile exec:java -Dexec.mainClass="DotsBoxes.ComparaisonExpert"
 */
public class ComparaisonExpert {

    private static final int  GAMES        = 10;    // parties par match
    private static final int  EXPERT_DEPTH = 20;    // profondeur max Expert (limité par temps)
    private static final long TIME_LIMIT_MS = 1000; // contrainte tournoi

    // Configurations testées : { rows, cols, profondeur AB }
    private static final int[][] CONFIGS = {
        {4, 4,  6},   // 3×3 cases, 24 segments — petite grille
        {5, 5,  5},   // 4×4 cases, 40 segments — petite grille
        {6, 6,  4},   // 5×5 cases, 60 segments — moyenne grille
        {7, 7,  3},   // 6×6 cases, 84 segments — grande grille (mode glouton Expert)
        {8, 8,  3},   // 7×7 cases, 112 segments — grande grille
    };

    // -------------------------------------------------------------------
    // Jeu d'une partie avec mesure du temps par coup
    // -------------------------------------------------------------------

    private static GameResult playGame(int rows, int cols, Player p0, Player p1) {
        Board board    = new Board(rows, cols);
        Player current = p0, other = p1;
        int s0 = 0, s1 = 0;
        long totalTime0 = 0, totalTime1 = 0;
        int  moves0 = 0, moves1 = 0;
        boolean timeViolation = false;

        while (!board.isFinished()) {
            long t0 = System.currentTimeMillis();
            Action a = current.getAction(board);
            long elapsed = System.currentTimeMillis() - t0;

            if (current.getId() == 0) { totalTime0 += elapsed; moves0++; }
            else                      { totalTime1 += elapsed; moves1++; }

            if (elapsed > TIME_LIMIT_MS) timeViolation = true;

            if (a == null || !board.isValid(a)) {
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

        double avg0 = moves0 > 0 ? (double) totalTime0 / moves0 : 0;
        double avg1 = moves1 > 0 ? (double) totalTime1 / moves1 : 0;
        return new GameResult(s0, s1, avg0, avg1, timeViolation);
    }

    private record GameResult(int s0, int s1, double avgMs0, double avgMs1,
                              boolean timeViolation) {}

    // -------------------------------------------------------------------
    // Match aller-retour
    // -------------------------------------------------------------------

    private static void runMatch(int rows, int cols, int abDepth) {
        ActionStrategy expert = new ExpertActionStrategy(EXPERT_DEPTH);
        ActionStrategy ab     = new AlphaBetaActionStrategy(abDepth,
                new AlphaBetaPruningObserver(), new NodeCounterObserver());

        int wExpert = 0, wAb = 0, draws = 0;
        int totalScore = 0;
        double totalTimeExpert = 0, totalTimeAb = 0;
        int violations = 0;

        for (int g = 0; g < GAMES; g++) {
            Player p0, p1;
            boolean expertIsP0 = (g % 2 == 0);
            if (expertIsP0) {
                p0 = new AutomatePlayer(0, expert);
                p1 = new AutomatePlayer(1, ab);
            } else {
                p0 = new AutomatePlayer(0, ab);
                p1 = new AutomatePlayer(1, expert);
            }

            GameResult r = playGame(rows, cols, p0, p1);
            if (r.timeViolation()) violations++;

            // Ramener au point de vue Expert
            int margin = expertIsP0 ? (r.s0() - r.s1()) : (r.s1() - r.s0());
            totalScore += margin;
            if      (margin > 0) wExpert++;
            else if (margin < 0) wAb++;
            else                 draws++;

            totalTimeExpert += expertIsP0 ? r.avgMs0() : r.avgMs1();
            totalTimeAb     += expertIsP0 ? r.avgMs1() : r.avgMs0();
        }

        int boxRows = rows - 1, boxCols = cols - 1;
        int segments = rows * (cols - 1) + (rows - 1) * cols;
        System.out.printf("%n  Plateau %dx%d (%d segments) — AB profondeur %d%n",
                boxRows, boxCols, segments, abDepth);
        System.out.printf("    Expert  : %2d V / %2d D / %2d N  (%.0f%%)  |  temps moy: %.0f ms%n",
                wExpert, wAb, draws, 100.0 * wExpert / GAMES,
                totalTimeExpert / GAMES);
        System.out.printf("    AlphaBeta: %2d V / %2d D / %2d N  (%.0f%%)  |  temps moy: %.0f ms%n",
                wAb, wExpert, draws, 100.0 * wAb / GAMES,
                totalTimeAb / GAMES);
        System.out.printf("    Score moyen Expert : %+.1f cases/partie%n",
                (double) totalScore / GAMES);
        if (violations > 0)
            System.out.printf("    ⚠ Dépassements >1s détectés : %d fois%n", violations);
    }

    // -------------------------------------------------------------------
    // Point d'entrée
    // -------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println("  Expert vs Alpha-Beta — contrainte 1 s/coup — " + GAMES + " parties/config");
        System.out.println("══════════════════════════════════════════════════════════════");

        for (int[] cfg : CONFIGS) {
            runMatch(cfg[0], cfg[1], cfg[2]);
        }

        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("  Analyse terminée.");
        System.out.println("══════════════════════════════════════════════════════════════");
    }
}
