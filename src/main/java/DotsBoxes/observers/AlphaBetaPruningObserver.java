package DotsBoxes.observers;

/**
 * Observateur responsable du suivi et de l'analyse de l'algorithme Alpha-Beta Pruning.
 * <p>
 * Enregistre les statistiques suivantes lors de l'exécution de l'algorithme :
 * <ul>
 *   <li>Nombre de nœuds visités (exploration)</li>
 *   <li>Nombre de coupes Alpha (optimisations branche Max)</li>
 *   <li>Nombre de coupes Beta (optimisations branche Min)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Ces statistiques permettent de mesurer l'efficacité de l'élagage alpha-beta
 * par rapport à un Minimax classique qui n'effectuerait pas d'optimisations.
 * </p>
 * 
 * @version 1.0
 * @author L3 UPS
 * @see AlphaBetaActionStrategy
 */
public class AlphaBetaPruningObserver {

    /** Nombre de coupes Alpha effectuées (branche de maximisation). */
    private int alphaCutCount = 0;
    
    /** Nombre de coupes Beta effectuées (branche de minimisation). */
    private int betaCutCount = 0;
    
    /** Nombre de nœuds totalement visités dans l'arbre de recherche. */
    private int nodeCount = 0;

    /**
     * Incrémente le compteur de nœuds visités.
     * <p>
     * À appeler à chaque visite d'un nœud dans l'arbre de recherche.
     * </p>
     */
    public void incrementNodeCount() {
        nodeCount++;
    }

    /**
     * Incrémente le compteur de coupes Alpha.
     * <p>
     * À appeler à chaque fois qu'une branche Max peut être élaguée
     * car une meilleure option a déjà été trouvée.
     * </p>
     */
    public void incrementAlphaCut() {
        alphaCutCount++;
    }

    /**
     * Incrémente le compteur de coupes Beta.
     * <p>
     * À appeler à chaque fois qu'une branche Min peut être élaguée
     * car une meilleure option a déjà été trouvée au niveau supérieur.
     * </p>
     */
    public void incrementBetaCut() {
        betaCutCount++;
    }

    /**
     * Affiche les statistiques dans la console.
     * <p>
     * Format : nombre de coupes alpha, coupes beta et nœuds visités.
     * </p>
     */
    public void printStats() {
        System.out.println("Alpha cuts: " + alphaCutCount);
        System.out.println("Beta cuts: " + betaCutCount);
        System.out.println("Nodes visited: " + nodeCount);
    }

    /**
     * Réinitialise tous les compteurs à zéro.
     * <p>
     * À appeler avant de démarrer une nouvelle recherche.
     * </p>
     */
    public void reset() {
        alphaCutCount = 0;
        betaCutCount = 0;
        nodeCount = 0;
    }

    /**
     * Retourne le nombre de coupes Alpha enregistrées.
     *
     * @return nombre de coupes Alpha
     */
    public int getAlphaCutCount() {
        return alphaCutCount;
    }

    /**
     * Retourne le nombre de coupes Beta enregistrées.
     *
     * @return nombre de coupes Beta
     */
    public int getBetaCutCount() {
        return betaCutCount;
    }

    /**
     * Retourne le nombre de nœuds visités.
     *
     * @return nombre total de nœuds explorés
     */
    public int getNodeCount() {
        return nodeCount;
    }
}