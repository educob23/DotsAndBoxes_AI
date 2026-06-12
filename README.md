# Dots and Boxes — IA de jeu (Minimax, Alpha-Beta, MCTS, Expert)
 
Implémentation en Java du jeu **Dots and Boxes** (aussi appelé Pipopipette), avec un accent sur l'**intelligence artificielle de jeu** : recherche adversariale, recherche arborescente Monte-Carlo, et une stratégie experte fondée sur la théorie des chaînes.
 
## Présentation
 
Dots and Boxes est un jeu combinatoire à information parfaite : deux joueurs tracent à tour de rôle un segment sur une grille de points. Fermer le 4ᵉ côté d'une case rapporte un point et donne un coup supplémentaire au joueur. Malgré des règles très simples, le jeu cache une combinatoire redoutable (théorie des chaînes, sacrifices, doubles-croix), ce qui en fait un terrain d'expérimentation classique en intelligence artificielle de jeu.
 
Ce projet implémente le moteur de jeu complet ainsi que plusieurs stratégies d'IA, des plus simples (aléatoire, gloutonne) aux plus avancées (Minimax, Alpha-Beta, Monte Carlo Tree Search, et une stratégie experte hybride).
 
## Prérequis & Installation
 
**Prérequis :**
- Java 17
- Maven 3.8+
- Un IDE Java (IntelliJ IDEA, VS Code + Extension Pack Java, Eclipse) — optionnel mais recommandé
Vérifier l'environnement :
 
```bash
java -version
mvn -version
```
 
**Cloner le dépôt :**
 
```bash
git clone https://github.com/educob23/SAI_Project.git
cd SAI_Project
```
 
**Ouvrir le projet :**
 
- *Avec IntelliJ IDEA (recommandé)* : `File > Open`, sélectionner le dossier du projet. Ouvrir `pom.xml` comme projet Maven si demandé, puis attendre l'import des dépendances.
- *Avec VS Code* : ouvrir le dossier avec l'Extension Pack Java installé — le projet Maven est détecté automatiquement.
**Compiler le projet :**
 
```bash
mvn clean compile
```
 
## Stratégies d'IA implémentées
 
| Stratégie | Description |
|---|---|
| **Random / Glouton / FirstValid** | Stratégies de référence (baselines) pour évaluer les IA plus avancées |
| **Minimax** | Recherche à profondeur limitée avec une heuristique personnalisée évaluant les chaînes de cases et les sacrifices |
| **Alpha-Beta** | Minimax avec élagage alpha-beta, instrumenté via des observateurs comptant les nœuds visités et les coupes effectuées |
| **MCTS** | Monte Carlo Tree Search (sélection UCT, expansion, simulation aléatoire, rétropropagation), règle de rejeu respectée |
| **Expert** | Stratégie hybride basée sur la théorie des chaînes : capture immédiate → coup sûr → sacrifice de chaîne minimal, avec bascule entre mode glouton (grandes grilles, O(N)) et Alpha-Beta à approfondissement itératif (petites/moyennes grilles) |
 
Toutes les stratégies tiennent compte de la règle de rejeu : un joueur qui ferme une case rejoue immédiatement.
 
## Outil de comparaison
 
Le projet inclut un framework de benchmarking (`Comparaison`, `ComparaisonExpert`, `ComparaisonMcts`, `ComparaisonMinimaxAlphaBeta`) qui fait s'affronter deux stratégies sur N parties, en alternant qui commence pour garantir l'équité, et rapporte le taux de victoire et la marge moyenne.
 
```bash
java -cp target/classes DotsBoxes.Comparaison 4 4 100 random glouton
```
 
## Interfaces
 
- **Texte** : `DotsBoxes.DotsBoxesGame`
- **Graphique (Swing)** : `DotsBoxes.ui.DotsBoxesSwingUI`
Lancer ces classes directement depuis l'IDE.
 
## Architecture
 
```
src/main/java/DotsBoxes/
├── board/        # Plateau, actions, règles du jeu
├── observers/    # Comptage de nœuds, coupes alpha-beta
├── player/
│   ├── ai/       # Minimax, Alpha-Beta, MCTS, Expert
│   ├── automate/ # Stratégies simples (random, glouton...)
│   └── human/    # Joueur humain
├── referee/      # Arbitre / moteur de partie
└── ui/           # Interfaces texte et Swing
```
 
## Tests
 
Le projet utilise JUnit 5 et Mockito.
 
```bash
mvn test
```
 
## Documentation
 
- Javadoc générée via `mvn javadoc:javadoc` (sortie dans `target/site/apidocs/index.html`)
- Rapport détaillé : [Rapport_SAI.pdf](Rapport_SAI.pdf)
## Auteurs
 
- Eduardo Cobos Fernandez
- Jeremy Rakotonirina
 
