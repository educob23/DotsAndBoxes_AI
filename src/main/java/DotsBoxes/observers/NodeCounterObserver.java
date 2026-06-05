package DotsBoxes.observers;

/**
 * Observateur utilitaire pour compter le nombre de nœuds visités
 * lors d'un processus de recherche ou d'évaluation.
 * <p>
 * Couramment utilisé dans les algorithmes comme Minimax ou Alpha-Beta
 * pour suivre les performances et estimer la complexité de l'exploration.
 * </p>
 * 
 * @version 1.0
 * @author L3 UPS
 * @see MinimaxActionStrategy
 * @see AlphaBetaActionStrategy
 */
public class NodeCounterObserver {

    /** Compteur du nombre de nœuds visités. */
    private int count = 0;

    /**
     * Réinitialise le compteur à zéro.
     * <p>
     * À appeler avant de démarrer une nouvelle exploration ou calcul.
     * </p>
     */
    public void reset() {
        count = 0;
    }

    /**
     * Incrémente le compteur d'une unité.
     * <p>
     * À appeler à chaque visite d'un nœud pendant le calcul.
     * </p>
     */
    public void increment() {
        count++;
    }

    /**
     * Retourne le compteur actuel de nœuds visités.
     *
     * @return nombre de nœuds visités jusqu'à présent
     */
    public int getCount() {
        return count;
    }
}