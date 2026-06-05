package DotsBoxes.player.human;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.Player;

import java.util.Scanner;

/**
 * Joueur humain pour le jeu Points et Cases (Dots and Boxes).
 * <p>
 * Permet à un utilisateur réel d'interagir avec le jeu en ligne de commande.
 * Le joueur saisit son choix de segment (type, ligne, colonne) au clavier.
 * </p>
 * 
 * <p>
 * Les entrées invalides sont détectées et l'utilisateur est invité à recommencer.
 * </p>
 * 
 * @version 1.0
 * @author L3 UPS
 */
public class HumanPlayer implements Player {

    /** Identifiant du joueur. */
    private final int id;
    
    /** Scanner pour lire les entrées utilisateur. */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Crée un nouveau joueur humain.
     *
     * @param id identifiant du joueur (0 ou 1)
     */
    public HumanPlayer(int id) {
        this.id = id;
    }

    /**
     * Demande interactivement au joueur humain de choisir un coup.
     * <p>
     * Boucle jusqu'à ce que l'utilisateur saisisse des coordonnées valides.
     * Valide l'orientation du segment (H ou V) et les indices de ligne/colonne.
     * </p>
     *
     * @param board état courant du plateau (utilisé pour l'affichage uniquement)
     * @return l'action choisie par le joueur
     */
    @Override
    public Action getAction(Board board) {

        while (true) {
            try {
                System.out.println("\nTour du joueur " + id);

                System.out.print("Type de segment (H = horizontal, V = vertical) : ");
                String input = scanner.next().toUpperCase();

                Action.Type type;
                if ("H".equals(input)) {
                    type = Action.Type.HORIZONTAL;
                } else if ("V".equals(input)) {
                    type = Action.Type.VERTICAL;
                } else {
                    System.out.println("Type invalide. Entrez H ou V.");
                    continue;
                }

                System.out.print("Ligne : ");
                int row = scanner.nextInt();

                System.out.print("Colonne : ");
                int col = scanner.nextInt();

                Action action = new Action(type, row, col);

                    return action;

            } catch (Exception e) {
                System.out.println("Entrée incorrecte, recommencez.");
                scanner.nextLine(); // nettoyage du buffer
            }
        }
    }

    /**
     * Retourne l'identifiant du joueur.
     *
     * @return identifiant du joueur (0 ou 1)
     */
    @Override
    public int getId() {
        return id;
    }
}