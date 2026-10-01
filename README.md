# petit-jeu-detente

# Route Runner

Route Runner est un simulateur 2D de réseau de transport. Le joueur construit un réseau routier, place des stations et des dépôts, puis crée des lignes et affecte des véhicules.

## Fonctionnalités principales

- Tracer des routes par glisser-déposer, poser des stations et placer plusieurs dépôts avec leur voie d’accès.
- Construire une ligne en sélectionnant les stations dans l’ordre. Une station inaccessible depuis l’arrêt précédent est refusée; l’aperçu est vert, rouge ou turquoise selon le contexte.
- Gérer les lignes depuis le HUD : couleur, véhicules associés et ajout en série de plusieurs véhicules sans quitter l’écran d’affectation.
- Maintenir le clic droit au plus 2 secondes pour annuler l’action de placement active; au-delà, il est ignoré. Un déplacement droit d’au moins 5 pixels pan la caméra et n’annule jamais l’action.
- Consulter la liste des véhicules, dont le numéro reste stable après sa création, et libérer un véhicule affecté.
- Envoyer les véhicules sans ligne vers le dépôt accessible le plus proche.
- Placer aléatoirement des bâtiments producteurs dans une zone de départ plus grande que l'écran, sans les superposer ni les placer sur les routes initiales.
- Voir leur production et leur stock; chaque station donne accès aux ressources des bâtiments dans son rayon de capture.

## Fonctionnement technique

- **Carte** : `WorldMap` possède le graphe routier, les stations, intersections, dépôts et bâtiments. Elle initialise le terrain de départ, synchronise les entités dérivées du graphe et met à jour les ressources; le HUD, la caméra et les véhicules restent gérés séparément.
- **Graphe routier** : les points sont alignés sur une grille de 25 pixels. À chaque ajout de route, le graphe détecte les croisements et découpe les segments aux intersections et aux stations. Il conserve les connexions par point et augmente sa version après chaque reconstruction. Les trajets sont calculés avec Dijkstra, en prenant la longueur des routes comme coût; les véhicules en cours de route peuvent être repositionnés sur le graphe reconstruit.
- **Lignes** : une ligne contient une liste ordonnée de stations et un chemin pour chaque paire consécutive. Chaque segment doit être accessible. Le retour du terminus au départ est calculé indépendamment comme le plus court chemin, plutôt qu’en inversant l’aller. Quand le graphe change, les chemins sont recalculés; le véhicule conserve sa prochaine station cible et reprend depuis sa position courante.
- **Véhicules** : la vitesse est simulée à pas fixe de 1/60 seconde. La puissance est exprimée en watts; le modèle utilise une masse de référence de 1 000 kg et une échelle de 0,1 m par pixel. L’accélération diminue avec la vitesse, la vitesse maximale est plafonnée et le freinage utilise la même puissance pour s’arrêter à la destination.
- **Dépôts** : chaque dépôt est un bloc violet avec une courte sortie routière vers le bas. Les véhicules sans ligne recherchent le dépôt accessible le plus proche et s’y garent.
- **Ressources** : 12 bâtiments sont générés sur une grille dans une zone de 1,5 fois la largeur et la hauteur de la caméra initiale, avec au moins un bâtiment de chaque type. Nourriture, bois et métal ont chacun leur cadence et capacité de stockage. Chaque bâtiment affiche son code produit et son stock, puis le code coloré de la ressource attendue : N pour nourriture, B pour bois, M pour métal. Le cycle de demandes est N→M, B→N et M→B. Le cercle de capture d'une station (180 unités) apparaît en rouge ou vert pendant sa pose, puis en turquoise une fois construite. À côté d'une station, stocks produits et demandes capturées sont affichés séparément. À un arrêt de ligne, un véhicule décharge uniquement si un bâtiment capturé demande son type de cargaison; sinon il le conserve. Le chargement et le déchargement prennent chacun 0,5 seconde par unité. Un véhicule transporte jusqu'à 4 unités d'un seul type à la fois. Les bâtiments stockent au plus 4 unités entrantes et en consomment une toutes les 10 secondes, ce qui renouvelle leurs demandes.
- **HUD** : le menu principal et ses actions sont définis en XML. Les dialogues affichent les options par pages lorsque nécessaire. La validation de ligne est une action contextuelle à usage unique, dotée d’un callback d’annulation; choisir une autre action efface la sélection provisoire et quitte le mode ligne. La liste des véhicules utilise des numéros attribués par le gestionnaire à leur création.

## Technologies

Java 25, Gradle, LWJGL 3 (GLFW et OpenGL) et JUnit 5.
