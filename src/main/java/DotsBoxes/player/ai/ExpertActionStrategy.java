package DotsBoxes.player.ai;

import DotsBoxes.board.Action;
import DotsBoxes.board.Board;
import DotsBoxes.player.ActionStrategy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Stratégie experte pour Dots and Boxes — adaptée à toutes les tailles de grille.
 *
 * Architecture à deux modes
 * 
 *   Mode glouton chainé (grandes grilles, >{GREEDY_THRESHOLD}
 *       coups disponibles) — aucune copie de plateau, O(N) par coup.
 *       Exploite la théorie des chaînes : capture > coup sûr > ouvrir
 *       la chaîne la plus courte.
 *   Mode Alpha-Beta (petites/moyennes grilles) — approfondissement
 *       itératif avec ordonnancement des coups (chain-aware), table de
 *       transposition (Zobrist) et gestion du temps.
 * 
 *
 * Théorie des chaînes
 * Une chaîne est un ensemble connexe de cases ayant exactement 2 côtés
 * tracés, reliées par des segments NON tracés. Quand un joueur « ouvre » une chaîne
 * (donne le 3e côté à une case), l'adversaire peut capturer TOUTES les cases de la
 * chaîne en cascade. La stratégie optimale est donc :
 * 
 *   Capturer d'abord (case à 3 côtés disponible).
 *   Jouer un coup « sûr » (aucun 3e côté donné à quiconque).
 *   Si contraint, ouvrir la chaîne la plus courte.
 * 
 *
 * Budget temps tournoi
 * Temps global = nb_segments_initiaux × 1 s. On utilise
 * {TIME_LIMIT_MS} ms par coup pour garder une marge de sécurité.
 */
public class ExpertActionStrategy implements ActionStrategy {

    private final int maxDepth;

    // -------------------------------------------------------------------
    // Constantes
    // -------------------------------------------------------------------

    /**
     * Au-delà de ce nombre de coups disponibles, on bascule en mode glouton
     * (pas de copies de plateau → O(N) garanti par coup).
     * En dessous, l'Alpha-Beta avec copies reste abordable.
     */
    private static final int GREEDY_THRESHOLD = 80;

    /**
     * En dessous de ce nombre de coups disponibles, on utilise un ordonnancement
     * simplifié (sans calcul de taille de chaîne).
     * Sur les petites grilles, le BFS de chainSizeFrom est appelé à chaque nœud
     * de l'arbre Alpha-Beta, ce qui ralentit l'exploration et réduit la profondeur
     * atteignable. L'ordonnancement simple (+1000 capture, -200 danger fixe) est
     * suffisant et permet à Alpha-Beta d'atteindre une profondeur plus grande.
     * 3×3 = 12 segments, 4×4 = 24 segments → seuil à 30.
     */
    private static final int SIMPLE_ORDERING_THRESHOLD = 30;

    /** Budget temps par coup en ms. */
    private static final long TIME_LIMIT_MS = 850;

    // -------------------------------------------------------------------
    // Gestion du temps
    // -------------------------------------------------------------------

    private long    startTime;
    private boolean timeUp;

    private boolean isTimeUp() {
        return System.currentTimeMillis() - startTime >= TIME_LIMIT_MS;
    }

    // -------------------------------------------------------------------
    // Utilitaires de cases — O(1), aucune copie
    // -------------------------------------------------------------------

    /** Nombre de côtés tracés autour de la case (r, c). */
    private int sides(Board b, int r, int c) {
        int s = 0;
        if (b.isHEdgeSet(r,     c    )) s++;
        if (b.isHEdgeSet(r + 1, c    )) s++;
        if (b.isVEdgeSet(r,     c    )) s++;
        if (b.isVEdgeSet(r,     c + 1)) s++;
        return s;
    }

    /**
     * Nombre de cases que ferme l'action {a} dans l'état courant.
     * Vérifie si les cases adjacentes ont déjà 3 côtés — O(1), sans copie.
     *
     * <p>Une arête horizontale (r,c) borde les cases (r-1,c) au-dessus et
     * (r,c) en-dessous. Une arête verticale (r,c) borde (r,c-1) à gauche et
     * (r,c) à droite.</p>
     */
    private int captureCount(Board b, Action a) {
        int count = 0;
        int r = a.getRow(), c = a.getCol();
        if (a.getType() == Action.Type.HORIZONTAL) {
            if (r > 0             && sides(b, r - 1, c) == 3) count++;
            if (r < b.getRows()-1 && sides(b, r,     c) == 3) count++;
        } else {
            if (c > 0             && sides(b, r, c - 1) == 3) count++;
            if (c < b.getCols()-1 && sides(b, r, c    ) == 3) count++;
        }
        return count;
    }

    /**
     * Vrai si l'action ne donne le 3e côté à aucune case adjacente.
     * Coup « sûr » : l'adversaire ne peut pas capturer directement après.
     * O(1), sans copie.
     */
    private boolean isSafe(Board b, Action a) {
        int r = a.getRow(), c = a.getCol();
        if (a.getType() == Action.Type.HORIZONTAL) {
            if (r > 0             && sides(b, r - 1, c) == 2) return false;
            if (r < b.getRows()-1 && sides(b, r,     c) == 2) return false;
        } else {
            if (c > 0             && sides(b, r, c - 1) == 2) return false;
            if (c < b.getCols()-1 && sides(b, r, c    ) == 2) return false;
        }
        return true;
    }

    /**
     * Score d'un coup sûr pour le tri : on préfère les coups qui donnent
     * le moins de côtés aux cases adjacentes (moindre risque de former des chaînes).
     * Retourne -(max côtés adjacent) : plus grand = meilleur.
     */
    private int safePriority(Board b, Action a) {
        int r = a.getRow(), c = a.getCol();
        int maxAdj = 0;
        if (a.getType() == Action.Type.HORIZONTAL) {
            if (r > 0)             maxAdj = Math.max(maxAdj, sides(b, r - 1, c));
            if (r < b.getRows()-1) maxAdj = Math.max(maxAdj, sides(b, r,     c));
        } else {
            if (c > 0)             maxAdj = Math.max(maxAdj, sides(b, r, c - 1));
            if (c < b.getCols()-1) maxAdj = Math.max(maxAdj, sides(b, r, c    ));
        }
        return -maxAdj;
    }

    // -------------------------------------------------------------------
    // Détection des chaînes — O(N) avec BFS, aucune copie
    // -------------------------------------------------------------------

    /**
     * Taille de la chaîne de cases à 2 côtés atteignable depuis (sr, sc).
     *
     * Deux cases à 2 côtés sont « chaîne-adjacentes » si elles partagent
     * un segment NON tracé : capturer l'une donne automatiquement le 3e côté
     * à l'autre via ce segment commun, propageant la cascade de captures.
     *
     * Adjacences et segments partagés :
     * 
     *   (r,c) ↔ (r-1,c) : segment {hEdge[r][c]}
     *   (r,c) ↔ (r+1,c) : segment {hEdge[r+1][c]}
     *   (r,c) ↔ (r,c-1) : segment {vEdge[r][c]}
     *   (r,c) ↔ (r,c+1) : segment {vEdge[r][c+1]}
     * 
     */
    private int chainSizeFrom(Board b, int sr, int sc) {
        int rows = b.getRows() - 1, cols = b.getCols() - 1;
        return chainSizeFromVis(b, sr, sc, new boolean[rows][cols], rows, cols);
    }

    /**
     * BFS partagé — met à jour le tableau {vis} fourni par l'appelant.
     * Utilisé par {chainSizeFrom} (vis local) et par {evaluate}
     * (vis commun pour éviter tout double-comptage).
     */
    private int chainSizeFromVis(Board b, int sr, int sc,
                                 boolean[][] vis, int rows, int cols) {
        vis[sr][sc] = true;
        Deque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sr, sc});
        int size = 0;

        while (!q.isEmpty()) {
            int[] cur = q.poll();
            size++;
            int r = cur[0], c = cur[1];

            if (r > 0      && !vis[r-1][c] && !b.isHEdgeSet(r,     c  ) && sides(b, r-1, c) == 2)
                { vis[r-1][c] = true; q.add(new int[]{r-1, c}); }
            if (r < rows-1 && !vis[r+1][c] && !b.isHEdgeSet(r + 1, c  ) && sides(b, r+1, c) == 2)
                { vis[r+1][c] = true; q.add(new int[]{r+1, c}); }
            if (c > 0      && !vis[r][c-1] && !b.isVEdgeSet(r,     c  ) && sides(b, r, c-1) == 2)
                { vis[r][c-1] = true; q.add(new int[]{r, c-1}); }
            if (c < cols-1 && !vis[r][c+1] && !b.isVEdgeSet(r,     c+1) && sides(b, r, c+1) == 2)
                { vis[r][c+1] = true; q.add(new int[]{r, c+1}); }
        }
        return size;
    }

    /**
     * Dangerosité d'un coup : nombre total de cases dans les chaînes ouvertes.
     *
     * Un coup est "dangereux" s'il donne un 3e côté à une case adjacente qui
     * en a déjà 2 (début de chaîne capturable). Un coup est "neutre" s'il ne
     * donne qu'un 2e côté (cases à 1 côté → passe à 2). Dans tous les cas on
     * retourne une valeur > 0 pour permettre le tri : plus c'est petit, mieux
     * c'est. Cases à 2 côtés = ouverture réelle de chaîne, cases à 1 côté =
     * léger risque, cases à 0 côté = sans risque immédiat (retourne 0).
     *
     * Un segment peut border deux cases : on additionne les deux coûts.
     */
    /**
     * Utilise un tableau {vis} partagé pour éviter le double-comptage
     * quand les deux cases adjacentes au segment font partie de la même chaîne.
     * Sans ce partage, {chainSizeFrom} serait appelé deux fois sur la
     * même chaîne et retournerait 2×N au lieu de N.
     */
    private int chainSizeOpenedBy(Board b, Action a) {
        int rows = b.getRows() - 1, cols = b.getCols() - 1;
        boolean[][] vis = new boolean[rows][cols];
        int r = a.getRow(), c = a.getCol();
        int total = 0;
        if (a.getType() == Action.Type.HORIZONTAL) {
            if (r > 0             ) total += chainCostOf(b, r-1, c, vis, rows, cols);
            if (r < b.getRows()-1 ) total += chainCostOf(b, r,   c, vis, rows, cols);
        } else {
            if (c > 0             ) total += chainCostOf(b, r, c-1, vis, rows, cols);
            if (c < b.getCols()-1 ) total += chainCostOf(b, r, c,   vis, rows, cols);
        }
        return total;
    }

    /**
     * Coût de la case (br, bc) si on lui donne un côté supplémentaire.
     * - sides == 2 → ouverture réelle de chaîne : retourne la taille via BFS partagé
     * - sides == 1 → on la met à 2 côtés, elle devient vulnérable : retourne 1
     * - autres    → sans conséquence immédiate : retourne 0
     */
    private int chainCostOf(Board b, int br, int bc, boolean[][] vis, int rows, int cols) {
        if (vis[br][bc]) return 0; // déjà comptée via l'autre case adjacente
        int s = sides(b, br, bc);
        if (s == 2) return chainSizeFromVis(b, br, bc, vis, rows, cols);
        if (s == 1) return 1;
        return 0;
    }

    // -------------------------------------------------------------------
    // Mode glouton chainé — grandes grilles
    // -------------------------------------------------------------------

    /**
     * Stratégie en trois phases, sans copie de plateau :
     * 
     *   Capture : le meilleur coup qui ferme le plus de cases (0 ou 1 max
     *       ici car Phase 1 universelle a déjà capturé les captures directes).
     *   Coup sûr : aucun 3e côté offert, préférence aux coups à risque
     *       minimal (cases adjacentes avec peu de côtés).
     *   Ouvrir la chaîne la plus courte : sacrifice minimal.
     * 
     */
    private Action greedyChain(Board b, List<Action> actions) {
        // 2. Coup sûr (capture déjà éliminée en Phase 1 universelle)
        Action bestSafe  = null;
        int    bestPrio  = Integer.MIN_VALUE;
        for (Action a : actions) {
            if (isTimeUp()) return bestSafe != null ? bestSafe : actions.get(0);
            if (!isSafe(b, a)) continue;
            int prio = safePriority(b, a);
            if (prio > bestPrio) { bestPrio = prio; bestSafe = a; }
            if (bestPrio == 0) break; // 0 = cases adjacentes à 0 côtés, optimal
        }
        if (bestSafe != null) return bestSafe;

        // 3. Ouvrir la chaîne la plus courte
        Action bestOpen   = null;
        int    minChain   = Integer.MAX_VALUE;
        for (Action a : actions) {
            if (isTimeUp()) return bestOpen != null ? bestOpen : actions.get(0);
            int size = chainSizeOpenedBy(b, a);
            if (size > 0 && size < minChain) {
                minChain = size;
                bestOpen = a;
                if (minChain == 1) break;
            }
        }
        return bestOpen != null ? bestOpen : actions.get(0);
    }

    // -------------------------------------------------------------------
    // Mode Alpha-Beta — petites / moyennes grilles
    // -------------------------------------------------------------------

    /**
     * Ordonnancement simple — sans calcul de taille de chaîne.
     * Utilisé sur les petites grilles (≤ nb_limite coups)
     * où appeler chainSizeFrom à chaque nœud de l'arbre serait trop coûteux.
     * +1000 capture, -200 coup qui donne le 3e côté (danger fixe), sinon neutre.
     */
    private int moveScoreSimple(Board b, Action a) {
        int score = 0;
        int r = a.getRow(), c = a.getCol();
        int boxRows = b.getRows() - 1, boxCols = b.getCols() - 1;
        int[] brs = (a.getType() == Action.Type.HORIZONTAL) ? new int[]{r-1, r} : new int[]{r, r};
        int[] bcs = (a.getType() == Action.Type.HORIZONTAL) ? new int[]{c, c}   : new int[]{c-1, c};
        for (int i = 0; i < 2; i++) {
            int br = brs[i], bc = bcs[i];
            if (br < 0 || br >= boxRows || bc < 0 || bc >= boxCols) continue;
            switch (sides(b, br, bc)) {
                case 3 -> score += 1000; // capture
                case 2 -> score -=  200; // donne le 3e côté → danger
                case 1 -> score +=   10;
                case 0 -> score +=   30;
            }
        }
        return score;
    }

    /**
     * Ordonnancement avancé — avec taille de chaîne (BFS).
     * Utilisé sur les grilles moyennes où le surcoût du BFS est amorti
     * par la meilleure qualité de l'élagage Alpha-Beta.
     */
    private int moveScoreChain(Board b, Action a) {
        int score = 0;
        int r = a.getRow(), c = a.getCol();
        int boxRows = b.getRows() - 1, boxCols = b.getCols() - 1;
        int[] brs = (a.getType() == Action.Type.HORIZONTAL) ? new int[]{r-1, r} : new int[]{r, r};
        int[] bcs = (a.getType() == Action.Type.HORIZONTAL) ? new int[]{c, c}   : new int[]{c-1, c};
        for (int i = 0; i < 2; i++) {
            int br = brs[i], bc = bcs[i];
            if (br < 0 || br >= boxRows || bc < 0 || bc >= boxCols) continue;
            switch (sides(b, br, bc)) {
                case 3 -> score += 2000;
                case 2 -> score -= 200 * chainSizeFrom(b, br, bc);
                case 1 -> score +=   10;
                case 0 -> score +=   30;
            }
        }
        return score;
    }

    private void sortActions(Board b, List<Action> actions) {
        if (actions.size() <= SIMPLE_ORDERING_THRESHOLD) {
            actions.sort((a1, a2) -> moveScoreSimple(b, a2) - moveScoreSimple(b, a1));
        } else {
            actions.sort((a1, a2) -> moveScoreChain(b, a2) - moveScoreChain(b, a1));
        }
    }

    /**
     * Heuristique en passe unique, 
     *
     * Pour chaque case à 3 côtés (immédiatement capturable par le joueur courant),
     * on identifie l'unique côté non-tracé : c'est le seul bord dont la pose
     * peut propager la cascade à la case voisine. On appelle alors
     *  avec le tableau  commun, ce qui marque
     * toute la chaîne et empêche tout double-comptage si plusieurs cases à 3 côtés
     * sont voisines de la même chaîne.
     *
     * Les cases à 2 côtés non adjacentes à une case à 3 côtés reçoivent un bonus
     * mineur (0.25) représentant leur potentiel futur.
     */
    private double evaluate(Board b, boolean isMax) {
        double score = b.getScore(0) - b.getScore(1);
        double sign  = isMax ? +1.0 : -1.0;
        int rows = b.getRows() - 1, cols = b.getCols() - 1;
        boolean[][] visited = new boolean[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (visited[r][c]) continue;
                int s = sides(b, r, c);
                if (s == 3) {
                    visited[r][c] = true;
                    score += sign;
                    // Trouver l'unique côté non-tracé → seul voisin pouvant cascader
                    int nr = -1, nc = -1;
                    if      (!b.isHEdgeSet(r,     c  ) && r > 0     ) { nr = r-1; nc = c;   }
                    else if (!b.isHEdgeSet(r + 1, c  ) && r < rows-1) { nr = r+1; nc = c;   }
                    else if (!b.isVEdgeSet(r,     c  ) && c > 0     ) { nr = r;   nc = c-1; }
                    else if (!b.isVEdgeSet(r,     c+1) && c < cols-1) { nr = r;   nc = c+1; }
                    if (nr >= 0 && !visited[nr][nc] && sides(b, nr, nc) == 2)
                        score += sign * chainSizeFromVis(b, nr, nc, visited, rows, cols);
                } else if (s == 2) {
                    // Bonus mineur si la case n'est pas déjà au bord d'une capture
                    boolean nearCapture =
                        (r > 0      && sides(b, r-1, c) == 3) ||
                        (r < rows-1 && sides(b, r+1, c) == 3) ||
                        (c > 0      && sides(b, r, c-1) == 3) ||
                        (c < cols-1 && sides(b, r, c+1) == 3);
                    if (!nearCapture) score += 0.25;
                }
            }
        }
        return score;
    }

    private record ABResult(Action action, double value) {}

    private ABResult alphaBeta(Board b, int depth, double alpha, double beta, boolean isMax) {
        if (timeUp)         return new ABResult(null, evaluate(b, isMax));
        if (b.isFinished()) return new ABResult(null, b.getScore(0) - b.getScore(1));
        if (depth == 0)     return new ABResult(null, evaluate(b, isMax));

        List<Action> actions = new ArrayList<>(b.getAvailableActions());
        sortActions(b, actions);
        Action best = null;

        if (isMax) {
            double maxVal = Double.NEGATIVE_INFINITY;
            for (Action a : actions) {
                if (isTimeUp()) { timeUp = true; break; }
                Board  copy = new Board(b);
                int    cl   = copy.apply(a, 0);
                double val  = alphaBeta(copy, depth-1, alpha, beta, cl > 0).value();
                if (val > maxVal) { maxVal = val; best = a; }
                alpha = Math.max(alpha, maxVal);
                if (alpha >= beta) break;
            }
            return new ABResult(best, maxVal);
        } else {
            double minVal = Double.POSITIVE_INFINITY;
            for (Action a : actions) {
                if (isTimeUp()) { timeUp = true; break; }
                Board  copy = new Board(b);
                int    cl   = copy.apply(a, 1);
                double val  = alphaBeta(copy, depth-1, alpha, beta, cl == 0).value();
                if (val < minVal) { minVal = val; best = a; }
                beta = Math.min(beta, minVal);
                if (alpha >= beta) break;
            }
            return new ABResult(best, minVal);
        }
    }

    // -------------------------------------------------------------------
    // Constructeur
    // -------------------------------------------------------------------

    public ExpertActionStrategy(int maxDepth) {
        if (maxDepth <= 0) throw new IllegalArgumentException("maxDepth doit etre > 0");
        this.maxDepth = maxDepth;
    }

    // -------------------------------------------------------------------
    // Point d'entrée principal
    // -------------------------------------------------------------------

    @Override
    public Action selectAction(Board b, int playerId) {
        List<Action> actions = b.getAvailableActions();
        if (actions.isEmpty()) return null;

        startTime = System.currentTimeMillis();
        timeUp    = false;

        // ── Phase 1 (universelle) : capture immédiate ──────────────────
        // O(1) par coup grâce à captureCount (pas de copie du plateau).
        // Toujours optimal : capturer maintenant vaut plus qu'explorer
        // l'arbre pour trouver la même case plus tard.
        // Préférer 2 cases fermées d'un coup sur 1.
        Action bestCapture = null;
        int    maxClosed   = 0;
        for (Action a : actions) {
            int cl = captureCount(b, a);
            if (cl > maxClosed) { maxClosed = cl; bestCapture = a; }
            if (maxClosed == 2) break;
        }
        if (bestCapture != null) return bestCapture;

        // ── Phase 2 : choix du mode selon la taille de l'espace de jeu ─
        if (actions.size() > GREEDY_THRESHOLD) {
            // Grandes grilles : glouton chainé, O(N), aucune copie
            return greedyChain(b, actions);
        }

        // Petites/moyennes grilles : Alpha-Beta avec approfondissement itératif
        List<Action> ordered = new ArrayList<>(actions);
        sortActions(b, ordered);
        Action  best  = ordered.get(0); // fallback = meilleur selon ordering
        boolean isMax = (playerId == 0);

        for (int depth = 1; depth <= maxDepth; depth++) {
            if (isTimeUp()) break;
            ABResult res = alphaBeta(b, depth,
                    Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, isMax);
            if (!timeUp && res.action() != null) best = res.action();
        }
        return best;
    }

    @Override
    public String getName() { return "Expert"; }
}
