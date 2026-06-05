# DotsBoxes (Maven)

Ce projet est un support de TP pour le jeu **Dots and Boxes (Points et Cases)**.

## Informations du groupe (a completer)

Travail de groupe : **4 etudiants par groupe**.

Remplir cette section avant le rendu :

- Groupe : `G1`
- Etudiant 1 (Nom Prenom) : `Cobos Fernandez Eduardo`
- Etudiant 2 (Nom Prenom) : `Rakotonirina Jeremy`

## 1. Pre-requis

- Java 17
- Maven 3.8+
- Un IDE Java (IntelliJ IDEA, VS Code + Extension Pack Java, Eclipse)

Verifier votre environnement :

```bash
java -version
mvn -version
```

## 2. Ouvrir le projet

### Option A - IntelliJ IDEA

1. `File > Open` puis selectionner le dossier du projet.
2. Ouvrir `pom.xml` comme projet Maven si demande.
3. Attendre l'import des dependances.

### Option B - Terminal

```bash
cd ./DotsBoxes
```

## 3. Compiler le projet

```bash
mvn clean compile
```

## 4. Comprendre la structure avec la Javadoc

La Javadoc est le point d'entree pour comprendre l'architecture (packages, classes, responsabilites, API publiques).

Generer la Javadoc :

```bash
mvn javadoc:javadoc
```

La documentation est generee dans :

- `target/site/apidocs/index.html`

Ouvrir ce fichier dans un navigateur.

Demarche recommandee :

1. Lire les packages principaux.
2. Identifier les classes centrales (`Board`, `Action`, `Referee`, `DotsBoxesGame`).
3. Lire les signatures publiques avant d'implementer.

## 5. Tests unitaires et demarche TDD

L'idee est d'avancer en petites etapes :

1. Ecrire ou choisir un test qui decrit le comportement attendu.
2. Lancer le test (il echoue).
3. Ecrire le minimum de code pour le faire passer.
4. Refactoriser sans casser les tests.

Lancer tous les tests :

```bash
mvn test
```

Lancer un test precis :

```bash
mvn -Dtest=NomDeLaClasseTest test
```

### Lancer les tests depuis l'IDE

#### IntelliJ IDEA

1. Ouvrir le panneau `Maven` (a droite).
2. Dans `Lifecycle`, lancer `test` (double clic).
3. Pour un test unique : ouvrir une classe de test dans `src/test/java`, puis cliquer sur l'icone `Run` a cote de la classe ou de la methode.

#### VS Code (Extension Pack Java)

1. Ouvrir la vue `Testing`.
2. Lancer tous les tests (bouton global) ou un test/classe specifique.
3. Utiliser le terminal integre si besoin :

```bash
mvn test
```

## 6. Interface pour jouer au jeu

Vous pouvez jouer de deux manieres :

1. Interface texte via la classe principale `DotsBoxes.DotsBoxesGame`.
2. Interface graphique Swing via `DotsBoxes.ui.DotsBoxesSwingUI`.

Execution conseillee : lancer ces classes directement depuis l'IDE.

## 7. Structure utile du projet

- `src/main/java` : code source principal
- `src/test/java` : tests unitaires
- `pom.xml` : configuration Maven

## 8. Commandes utiles pendant le TP

Compiler rapidement sans tests :

```bash
mvn -DskipTests compile
```

Lancer un test precis :

```bash
mvn -Dtest=MinimaxActionStrategyTest test
```

Nettoyer les builds :

```bash
mvn clean
```

## 9. Problemes frequents

- **`java: release version 17 not supported`**
  - Votre JDK actif n'est pas Java 17.
- **Dependances Maven non telechargees**
  - Verifier la connexion reseau, puis relancer `mvn clean compile`.

## 10. Conseils de travail

- Consulter la Javadoc avant d'ecrire le code.
- Avancer test par test (TDD).
- Compiler souvent (`mvn compile`).
- Lancer les tests regulierement (`mvn test`).
- Committer par petites etapes.
