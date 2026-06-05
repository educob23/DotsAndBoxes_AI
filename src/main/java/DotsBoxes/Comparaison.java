/*
 * Fichier qui contient la classe comparaison permettant de comparer les automates entre eux, ce référer au 
 * autres fichiers de comparaison pour les comparaison entre IAs
*/


package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;
import DotsBoxes.player.Player;
import DotsBoxes.player.automate.AutomatePlayer;
import DotsBoxes.player.automate.FirstValidActionStrategy;
import DotsBoxes.player.automate.GloutonActionStrategy;
import DotsBoxes.player.automate.RandomActionStrategy;

import java.util.concurrent.*;

public class Comparaison {
    /*
        Lance N parties entre deux stratégies et affiche les statistiques.
        Usage : mvn compile exec:java -Dexec.mainClass="DotsBoxes.Comparaison" -Dexec.args="[lignes] [colonnes] [N] [stratégie1] [stratégie2]"
        Stratégies disponibles : random, glouton. (ppour les autres stratégies se référer au autres fichiers de comparaison)
     */

    private static final long TIMEOUT_MS = 1000; // timeout par coup en millisecondes

    private static Action getActionWithTimeout(Player player, Board board) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Action> future = executor.submit(() -> player.getAction(board));
        try {
            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.err.printf("[TIMEOUT] %s a dépassé %dms%n", player.getClass().getSimpleName(), TIMEOUT_MS);
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            executor.shutdownNow();
        }
    }

    private static int play_without_display(int r, int c, Player p1, Player p2) {
        /*
            joue une partie sans affichage entre deux joueurs et retourne la différence de score : score(p1) - score(p2)
        */
        Board board = new Board(r, c);
        Player current = p1;
        Player next = p2;

        int score0 = 0;
        int score1 = 0;

        while (!board.isFinished()) {
            Action action = getActionWithTimeout(current, board);

            if (action == null || !board.isValid(action)) {
                // on pénalise une IA qui dépasse le timeout ou joue un coup invalide
                if (current.getId() == 0) score0--; else score1--;
                Player tmp = current; current = next; next = tmp;
                continue;
            }

            int closed = board.apply(action, current.getId());
            if (closed > 0) {
                if (current.getId() == 0) score0 += closed; else score1 += closed;
            } else {
                Player tmp = current; current = next; next = tmp;
            }
        }

        return score0 - score1;
    }


    private static ActionStrategy createStrategy(String name) {
        /*
        décide la stratégie à suivre à partir du nom donnée
        */
        return switch (name.toLowerCase()) {
            case "random"    -> new RandomActionStrategy();
            case "glouton"   -> new GloutonActionStrategy();
            case "firstvalid"   -> new FirstValidActionStrategy();
            case "minimax"   -> throw new UnsupportedOperationException("mauvais fichier de comparaison"); //new MinimaxActionStrategy(3);
            case "alphabeta" -> throw new UnsupportedOperationException("mauvais fichier de comparaison"); //new AlphaBetaActionStrategy(3, new AlphaBetaPruningObserver(), new NodeCounterObserver());
            case "expert"    -> throw new UnsupportedOperationException("mauvais fichier de comparaison");
            default -> throw new IllegalArgumentException("Not implemented : " + name);
        };
    }

    public static void main(String[] args) {
        // on récupère les paramètres
        int rows     = Integer.parseInt(args[0]);
        int cols     = Integer.parseInt(args[1]);
        int nbGames  = Integer.parseInt(args[2]);
        String name1 = args[3];
        String name2 = args[4];

        int wins1 = 0, wins2 = 0, draws = 0;
        int totalMargin1 = 0; // la marge de victoire de p1 pour le mode détail

        for (int i = 0; i < nbGames; i++) {
            // pour être le plus juste possible : 50% des fois la startégie 1 commence, 50% des fois la stratégie 2 commence
            Player p1, p2;
            if ((i % 2 == 0)) {
                // p1 = stratégie1
                p1 = new AutomatePlayer(0, createStrategy(name1));
                p2 = new AutomatePlayer(1, createStrategy(name2));
            } else {
                // p1 = stratégie2
                p1 = new AutomatePlayer(0, createStrategy(name2));
                p2 = new AutomatePlayer(1, createStrategy(name1));
            }

            // margin = score(p1) - score(p2)
            int margin = play_without_display(rows, cols, p1, p2);

            // on fait en sorte que la marge soit donnée en fonction de la stratégie utilisée et non le joueur du moment
            int marginStrat1;
            if(i % 2 == 0)
                marginStrat1 = margin;
            else {
                marginStrat1 = -margin;
            }

            if (marginStrat1 > 0) {
                wins1++;
                totalMargin1 += marginStrat1;
            } else if (marginStrat1 < 0) {
                wins2++;
                totalMargin1 += marginStrat1;
            } else {
                draws++;
            }
        }

        System.out.printf("Comparaison %s vs %s (%dx%d, %d parties)%n", name1, name2, rows, cols, nbGames);
        System.out.printf("%-12s : %d victoires (%.1f%%)%n", name1, wins1, 100.0 * wins1 / nbGames);
        System.out.printf("%-12s : %d victoires (%.1f%%)%n", name2, wins2, 100.0 * wins2 / nbGames);
        System.out.printf("%-12s : %d (%.1f%%)%n", "Egalites", draws, 100.0 * draws / nbGames);

        System.out.printf("Marge moyenne de victoire de %-12s : +%.2f cases%n", name1, (double) totalMargin1 / nbGames);
    }
}
