package DotsBoxes.board;

/**
 * Représente une action élémentaire dans le jeu Points et Cases (Dots and Boxes).
 * <p>
 * Une action correspond au tracé d'un segment entre deux points adjacents
 * du plateau. Un segment peut être :
 * <ul>
 *   <li>horizontal : entre deux points situés sur la même ligne</li>
 *   <li>vertical   : entre deux points situés sur la même colonne</li>
 * </ul>
 *
 * <p>
 * Une action est entièrement définie par :
 * <ul>
 *   <li>son orientation ({@link Type})</li>
 *   <li>une position (ligne, colonne)</li>
 * </ul>
 *
 * <p>
 * Les instances de cette classe sont <b>immutables</b>.
 * Elles ne contiennent aucune logique de validation : la validité
 * d'une action est du ressort de la classe {@code Board}.
 * </p>
 */
public class Action {

    /**
     * Orientation du segment tracé.
     */
    public enum Type {
        /** Segment horizontal */
        HORIZONTAL,
        /** Segment vertical */
        VERTICAL
    }

    private final Type type;
    private final int row;
    private final int col;

    /**
     * Construit une nouvelle action.
     *
     * @param type orientation du segment (horizontal ou vertical)
     * @param row  ligne du segment dans la représentation interne du plateau
     * @param col  colonne du segment dans la représentation interne du plateau
     *
     * @throws NullPointerException si {@code type} est {@code null}
     */
    public Action(Type type, int row, int col) {
        if (type == null) {
            throw new NullPointerException("Le type de l'action ne peut pas être null.");
        }
        this.type = type;
        this.row = row;
        this.col = col;
    }

    /**
     * Retourne l'orientation du segment.
     *
     * @return le type de l'action
     */
    public Type getType() {
        return type;
    }

    /**
     * Retourne la ligne associée à cette action.
     *
     * @return indice de ligne
     */
    public int getRow() {
        return row;
    }

    /**
     * Retourne la colonne associée à cette action.
     *
     * @return indice de colonne
     */
    public int getCol() {
        return col;
    }

    /**
     * Fournit une représentation textuelle de l'action,
     * utile pour le débogage et les journaux d'exécution.
     *
     * @return une chaîne décrivant l'action
     */
    @Override
    public String toString() {
        return type + "(" + row + "," + col + ")";
    }

    /**
     * Teste l'égalité entre deux actions.
     * Deux actions sont égales si elles ont le même type
     * et la même position.
     *
     * @param obj l'objet à comparer
     * @return {@code true} si les deux actions sont équivalentes
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Action)) return false;
        Action other = (Action) obj;
        return type == other.type
                && row == other.row
                && col == other.col;
    }

    /**
     * Calcule le code de hachage de l'action.
     *
     * @return le hash code
     */
    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + row;
        result = 31 * result + col;
        return result;
    }
}