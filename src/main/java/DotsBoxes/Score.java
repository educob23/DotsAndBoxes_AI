package DotsBoxes;

/**
 * Classe utilitaire pour gérer le score d'un joueur au cours d'un tournoi.
 * <p>
 * Accumule les points selon un système de notation classique :
 * <ul>
 *   <li>3 points pour une victoire</li>
 *   <li>1 point pour un match nul</li>
 *   <li>0 point pour une défaite</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Enregistre aussi le nombre total de victoires, nulls et défaites
 * pour des statistiques complètes.
 * </p>
 * 
 * @version 1.0
 * @author L3 UPS
 */
class Score {
    
    /** Points totaux accumulés. */
    int points = 0;
    
    /** Nombre total de victoires. */
    int wins = 0;
    
    /** Nombre total de matchs nuls. */
    int draws = 0;
    
    /** Nombre total de défaites. */
    int losses = 0;

    /**
     * Enregistre une victoire (ajoute 3 points et incrémente le compteur).
     */
    void win() { points += 3; wins++; }
    
    /**
     * Enregistre un match nul (ajoute 1 point et incrémente le compteur).
     */
    void draw() { points += 1; draws++; }
    
    /**
     * Enregistre une défaite (incrémente le compteur des pertes).
     */
    void lose() { losses++; }
}