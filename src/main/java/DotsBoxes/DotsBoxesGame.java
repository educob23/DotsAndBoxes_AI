package DotsBoxes;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.Player;
import DotsBoxes.player.automate.AutomatePlayer;
import DotsBoxes.player.automate.GloutonActionStrategy;
import DotsBoxes.player.automate.RandomActionStrategy;
import DotsBoxes.player.human.HumanPlayer;
import DotsBoxes.player.ai.AIPlayer;
import DotsBoxes.player.ai.MinimaxActionStrategy;
import DotsBoxes.player.ai.AlphaBetaActionStrategy;
import DotsBoxes.player.ai.ExpertActionStrategy;
import DotsBoxes.observers.AlphaBetaPruningObserver;
import DotsBoxes.observers.NodeCounterObserver;
import DotsBoxes.referee.Referee;

import java.util.Scanner;

/**
 * Moteur principal du jeu Points et Cases (Dots and Boxes).
 * <p>
 * Gère la boucle principale de jeu, l'alternance entre les joueurs,
 * l'affichage du plateau et des scores, ainsi que la détermination du gagnant.
 * </p>
 * 
 * <p>
 * Supporte plusieurs types de joueurs :
 * <ul>
 *   <li>Joueurs humains (entrée utilisateur)</li>
 *   <li>Joueurs automatiques simples (Glouton, Random)</li>
 *   <li>Joueurs IA intelligents (Minimax, Alpha-Beta, Expert)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Utilise le pattern Singleton pour l'Arbitre et implémente
 * le fonctionnement complet du jeu selon les règles standards.
 * </p>
 * 
 * @version 1.0
 * @author L3 UPS
 * @see Player
 * @see Board
 * @see Referee
 */
public class DotsBoxesGame {

    /** Plateau de jeu. */
    private final Board board;
    
    /** Premier joueur. */
    private final Player player1;
    
    /** Deuxième joueur. */
    private final Player player2;
    
    /** Joueur actuel (celui dont c'est le tour). */
    private Player currentPlayer;
    
    /** Arbitre singleton pour gérer le score et les règles. */
    private final Referee referee;

    /**
     * Crée une nouvelle partie de Dots and Boxes.
     *
     * @param rows nombre de lignes de points du plateau
     * @param cols nombre de colonnes de points du plateau
     * @param p1   joueur 1 (commence en bleu)
     * @param p2   joueur 2 (commence en rouge)
     */
    public DotsBoxesGame(int rows, int cols, Player p1, Player p2) {
        this.board = new Board(rows, cols);
        this.player1 = p1;
        this.player2 = p2;
        this.currentPlayer = player1;
        this.referee = Referee.getInstance();
        referee.init(player1, player2, board);
    }

    /**
     * Lance et exécute la partie.
     * <p>
     * Boucle principale du jeu :
     * <ol>
     *   <li>Affiche le plateau et les scores</li>
     *   <li>Demande une action au joueur courant</li>
     *   <li>Valide et applique l'action (pénalité -1 si coup invalide)</li>
     *   <li>Change de joueur si aucune case n'a été fermée</li>
     *   <li>Boucle jusqu'à ce que le plateau soit plein</li>
     * </ol>
     * </p>
     *
     * @return le joueur gagnant, ou {@code null} en cas d'égalité
     */
    public Player play() {

        while (!board.isFinished()) {

            displayBoard();
            displayScores();

            System.out.println("\nTour du joueur " + currentPlayer.getId());

            Action action = currentPlayer.getAction(board);

            if (action == null || !board.isValid(action)) {
                referee.applyInvalidMovePenalty(currentPlayer);
                System.out.println("❌ Coup invalide — pénalité -1 et tour perdu");
                switchPlayer();
                continue;
            }

            int gainedBoxes = referee.applyAction(currentPlayer, action);

            // Règle essentielle : on rejoue si on ferme une ou plusieurs cases
            if (gainedBoxes == 0) {
                switchPlayer();
            }
        }

        displayBoard();
        displayScores();

        Player winner = referee.getWinner();
        if (winner == null) {
            System.out.println("\n🤝 Match nul !");
        } else {
            System.out.println("\n🏆 Joueur " + winner.getId() + " gagne !");
        }

        return winner;
    }

    /**
     * Passe à l'autre joueur.
     */
    private void switchPlayer() {
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }

    /**
     * Affiche le plateau en mode ASCII colorisé.
     */
    private void displayBoard() {
        System.out.println("\n================ Plateau ================");
        board.printAscii();
        System.out.println("=========================================");
    }

    /** Code ANSI pour réinitialiser la couleur du terminal. */
    private static final String ANSI_RESET = "\u001B[0m";
    /** Code ANSI pour afficher en bleu (joueur 0). */
    private static final String ANSI_BLUE  = "\u001B[34m";
    /** Code ANSI pour afficher en rouge (joueur 1). */
    private static final String ANSI_RED   = "\u001B[31m";

    /**
     * Affiche le score courant des deux joueurs avec code couleur.
     */
    private void displayScores() {
        System.out.println("\nScores :");

        int score1 = referee.getScore(player1);
        int score2 = referee.getScore(player2);

        System.out.println("Joueur 0 : " + ANSI_BLUE + score1 + ANSI_RESET);
        System.out.println("Joueur 1 : " + ANSI_RED + score2 + ANSI_RESET);
    }

    /* ======================================================
       =============== POINT D’ENTRÉE ======================
       ====================================================== */

    /**
     * Point d'entrée en ligne de commande.
     * <p>
     * Demande la taille du plateau, le type des deux joueurs, puis lance
     * une partie complète.
     * </p>
     *
     * @param args arguments de ligne de commande (non utilisés)
     */
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Dots and Boxes ===");
        System.out.println("Taille du plateau (rows cols), ex: 4 4 : ");
        int rows = scanner.nextInt();
        int cols = scanner.nextInt();

        // Choix joueur 1
        System.out.println("\nType du joueur 1 :");
        System.out.println("1 = Humain");
        System.out.println("2 = Automate (Glouton)");
        System.out.println("3 = Automate (Random)");
        System.out.println("4 = IA (Minimax)");
        System.out.println("5 = IA (Alpha-Beta)");
        System.out.println("6 = IA (Expert)");
        int type1 = scanner.nextInt();
        Player player1 = createPlayer(0, type1);

        // Choix joueur 2
        System.out.println("\nType du joueur 2 :");
        System.out.println("1 = Humain");
        System.out.println("2 = Automate (Glouton)");
        System.out.println("3 = Automate (Random)");
        System.out.println("4 = IA (Minimax)");
        System.out.println("5 = IA (Alpha-Beta)");
        System.out.println("6 = IA (Expert)");
        int type2 = scanner.nextInt();
        Player player2 = createPlayer(1, type2);

        // Lancement du jeu
        DotsBoxesGame game = new DotsBoxesGame(rows, cols, player1, player2);
        game.play();
    }

    /**
     * Fabrique un joueur à partir d'un identifiant et d'un type sélectionné.
     *
     * @param id identifiant du joueur ({@code 0} ou {@code 1})
     * @param type type de joueur choisi au menu
     *             ({@code 1}=Humain, {@code 2}=Glouton, {@code 3}=Random,
     *             {@code 4}=Minimax, {@code 5}=Alpha-Beta, {@code 6}=Expert)
     * @return instance de joueur correspondant au type demandé
     * @throws IllegalArgumentException si le type n'est pas reconnu
     */
    private static Player createPlayer(int id, int type) {

        return switch (type) {
            case 1 -> new HumanPlayer(id);

            case 2 -> new AutomatePlayer(
                    id,
                    new GloutonActionStrategy()
            );

            case 3 -> new AutomatePlayer(
                    id,
                    new RandomActionStrategy()
            );

            case 4 -> new AIPlayer(
                    id,
                    new MinimaxActionStrategy(3)
            );

            case 5 -> new AIPlayer(
                    id,
                    new AlphaBetaActionStrategy(
                            3,
                            new AlphaBetaPruningObserver(),
                            new NodeCounterObserver()
                    )
            );

            case 6 -> new AIPlayer(
                    id,
                    new ExpertActionStrategy(6)
            );

            default -> throw new IllegalArgumentException(
                    "Type de joueur invalide : " + type
            );
        };
    }
}
