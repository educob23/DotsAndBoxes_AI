package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Stratégie MCTS (Monte Carlo Tree Search) pour le jeu Dots and Boxes.
 *
 * Quatre phases classiques :
 * 
 *  Sélection  – descend dans l'arbre via UCT jusqu'à trouver
 *       un nœud non entièrement développé ou terminal.
 *   Expansion  – ajoute un enfant en jouant un coup non encore tenté.
 *   Simulation– joue aléatoirement jusqu'à la fin de la partie.
 *   Rétropropagation– remonte le résultat (score0 − score1)
 *       dans tous les ancêtres.
 * 
 *
 * La règle de rejeu (on rejoue si on ferme une case) est respectée
 * dans l'expansion et dans la simulation.
 *
 * Le meilleur coup à la racine est déterminé par le nombre de visites
 * .
 */
public class MctsActionStrategy implements ActionStrategy {

    /** Constante d'exploration UCT. */
    private static final double C = Math.sqrt(2);

    private final int iterations;
    private final Random random;

    // ------------------------------------------------------------------
    // Constructeurs
    // ------------------------------------------------------------------

    public MctsActionStrategy(int iterations) {
        if (iterations <= 0) throw new IllegalArgumentException("iterations must be > 0");
        this.iterations = iterations;
        this.random = new Random();
    }

    public MctsActionStrategy(int iterations, long seed) {
        if (iterations <= 0) throw new IllegalArgumentException("iterations must be > 0");
        this.iterations = iterations;
        this.random = new Random(seed);
    }

    // ------------------------------------------------------------------
    // Nœud de l'arbre MCTS
    // ------------------------------------------------------------------

    private static class Node {

        /** État du plateau à ce nœud. */
        final Board board;
        /** Action qui a mené à cet état (null pour la racine). */
        final Action action;
        /** Identifiant du joueur qui doit jouer depuis cet état. */
        final int playerId;
        /** Nœud parent (null pour la racine). */
        final Node parent;

        final List<Node>   children        = new ArrayList<>();
        /** Coups pas encore essayés depuis cet état. */
        final List<Action> untriedActions;

        int    visits     = 0;
        /** Somme des (score0 − score1) remontés par les simulations. */
        double totalScore = 0.0;

        Node(Board board, Action action, int playerId, Node parent) {
            this.board    = board;
            this.action   = action;
            this.playerId = playerId;
            this.parent   = parent;
            if (board.isFinished()) {
                this.untriedActions = new ArrayList<>();
            } else {
                this.untriedActions = new ArrayList<>(board.getAvailableActions());
            }
        }

        boolean isFullyExpanded() { return untriedActions.isEmpty(); }
        boolean isTerminal()      { return board.isFinished(); }
    }

    // ------------------------------------------------------------------
    // Phase 1 : Sélection (UCT)
    // ------------------------------------------------------------------

    /**
     * Descend dans l'arbre en suivant la politique UCT jusqu'à atteindre
     * un nœud non entièrement développé ou un nœud terminal.
     */
    private Node select(Node root) {
        Node node = root;
        while (!node.isTerminal() && node.isFullyExpanded()) {
            node = bestUctChild(node);
        }
        return node;
    }

    /**
     * Valeur UCT d'un enfant, du point de vue du joueur courant à ce nœud.
     * Le terme d'exploitation est positif quand l'enfant est favorable au
     * joueur dont c'est le tour ({@code node.playerId}).
     */
    private Node bestUctChild(Node node) {
        Node best      = null;
        double bestVal = Double.NEGATIVE_INFINITY;
        double logParent = Math.log(node.visits);

        for (Node child : node.children) { //recherche meilleur enfant
            // exploit : vu du joueur courant
            double exploit;
            if (node.playerId == 0) {
                exploit = child.totalScore / child.visits;
            } else {
                exploit = -child.totalScore / child.visits;
            }
            double explore = C * Math.sqrt(logParent / child.visits);
            double uct = exploit + explore;

            if (uct > bestVal) { bestVal = uct; best = child; } 
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Phase 2 : Expansion
    // ------------------------------------------------------------------

    /**
     * Choisit aléatoirement un coup non encore essayé, crée le nœud fils
     * correspondant et l'ajoute à l'arbre.
     * La règle de rejeu est prise en compte pour déterminer le prochain joueur.
     */
    private Node expand(Node node) {
        int idx    = random.nextInt(node.untriedActions.size()); //choisit coup aléatoire
        Action act = node.untriedActions.remove(idx);

        Board child = new Board(node.board);
        int closed  = child.apply(act, node.playerId);

        // rejeu si case fermée
        int next;
        if (closed > 0) {
            next = node.playerId;
        } else {
            next = 1 - node.playerId;
        }

        Node childNode = new Node(child, act, next, node);
        node.children.add(childNode);
        return childNode;
    }

    // ------------------------------------------------------------------
    // Phase 3 : Simulation (rollout aléatoire)
    // ------------------------------------------------------------------

    /**
     * Joue aléatoirement  jusqu'à la fin de la partie.
     *
     * return score0 − score1 au terme de la simulation
     */
    private double simulate(Board board, int currentPlayer) {
        Board sim   = new Board(board);
        int player  = currentPlayer;

        while (!sim.isFinished()) {
            List<Action> actions = sim.getAvailableActions();
            Action a = actions.get(random.nextInt(actions.size())); //choisit coup aléaotoire
            int closed = sim.apply(a, player);
            if (closed == 0) player = 1 - player;
        }

        return sim.getScore(0) - sim.getScore(1);
    }

    // ------------------------------------------------------------------
    // Phase 4 : Rétropropagation
    // ------------------------------------------------------------------

    private void backpropagate(Node node, double result) {
        while (node != null) {
            node.visits++;
            node.totalScore += result;
            node = node.parent;
        }
    }

    // ------------------------------------------------------------------
    // Interface publique
    // ------------------------------------------------------------------

    @Override
    public Action selectAction(Board board, int playerId) {
        if (board.isFinished()) return null;

        Node root = new Node(new Board(board), null, playerId, null);

        for (int i = 0; i < iterations; i++) {
            // 1. Sélection
            Node node = select(root);

            // 2. Expansion (si nœud non terminal)
            if (!node.isTerminal()) {
                node = expand(node);
            }

            // 3. Simulation
            double result = simulate(node.board, node.playerId);

            // 4. Rétropropagation
            backpropagate(node, result);
        }

        // Retourne le coup de l'enfant le plus visité (critère qui est plus solide que celui du
        //gain moyen car plus robuste au bruit)
        Node best = null;
        int  bestVisits = -1;
        for (Node child : root.children) {
            if (child.visits > bestVisits) {
                bestVisits = child.visits;
                best = child;
            }
        }

        if (best != null) {
            return best.action;
        } else {
            return board.getAvailableActions().get(0);
        }
    }

    @Override
    public String getName() {
        return "MCTS";
    }
}
