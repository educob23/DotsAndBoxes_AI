package DotsBoxes.player;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;

/**
 * Représente un joueur du jeu Points et Cases (Dots and Boxes).
 * <p>
 * Un joueur peut être humain ou artificiel (IA). Son rôle est de
 * choisir une {@link Action} valide à partir de l'état courant du plateau.
 * </p>
 *
 * <p>
 * Cette interface ne contient aucune logique de validation des règles :
 * la validité des coups est du ressort du {@link Board} et de l'arbitre.
 * </p>
 */
public interface Player {

    /**
     * Demande au joueur de choisir un coup à jouer.
     *
     * <p>
     * Le plateau fourni est l'état courant du jeu. Le joueur doit
     * retourner une action correspondant au tracé d'un segment non encore joué.
     * </p>
     *
     * @param board l'état courant du plateau (lecture seule)
     * @return l'action choisie par le joueur
     */
    Action getAction(Board board);

    /**
     * Retourne l'identifiant du joueur.
     *
     * @return identifiant du joueur (ex. 0 ou 1)
     */
    int getId();
}