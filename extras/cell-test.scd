/*
Constructeur: (les paramètres par défaut sont excessivement raisonnables)

size:        taille (carré de size*size)

volume:      [0,1]

renawalTime: change une cellule toutes les renewalTime secondes

outParms:    tableau qui précise le type de sortie:
- ['sum']:                     somme de toutes les sorties
- ['walk', speed]:             se déplace aléatoirement au maximum de speed cellules par seconde
\                              en écoutant les cellules voisines
- ['fly', speed, zSpeed]:      se déplace en écoutant sur un rayon déterminé par l'altitude
\                              qui change au maximum de zSpeed par seconde
- ['fly', speed, altCycle]:    l'altitude est déterminée par un cycle
\                              [[altitude, ...],[temps, ...]]
- ['flypan', speed, zSpeed]
- ['flypan', speed, altCycle]: idem avec pan stereo aleatoire
- ['flyturtle', linSpeed, angSpeed, zSpeed]
- ['flyturtle', linSpeed, angSpeed, altCycle]: idem avec déplacement polaire (vitesse
\                                              longitudinale et angulaire)

genParms:    soit nil (paramètres par défaut), soit un tableau:
- weights:   tableau des probabilités de modulation [pNoise, pFwd, pDel, pFM, pAmp]
- nseParms:  tableau des paramètres de bruitage [nseMax, nseLowOct, nseHighOct]
--- nseMax:     quantité maximale de bruit [0,1]
--- nseLowOct:  nombre d'octaves maximum en dessous de la fréquence de base
--- nseHighOct: nombre d'octaves maximum en dessus de la fréquence de base
- fwdMax:    quantité maximale de mélange de l'entrée auxiliaire [0,1]
- delMaxExp: temps maximal de retard (exponentiel à partir de 2**-14)
- fmOct:     nombre d'octaves maximal de la modulation de fréquence
- ampMax:    quantité maximale de modulation en anneau [0,1]

rec:         soit nil (pas d'enregistrement), soit [[fadeIn, fadeOut, time], path]
(l'enregistrement ajoute une marge de 4 secondes au début et à la fin)
- fadeIn:  temps de fondu d'entrée
- fadeOut: temps de fondu de sortie
- time:    temps total (y compris les fondus)
- path:    le chemin du fichier cible

stopAfter:  limite de temps (l'arrêt se déclenche automatiquement)
*/

// exemples de base
a = Cell_Matrix(2, 0.75, 2, Cell_Parms.outSum);
a = Cell_Matrix(4, 0.75, 1, Cell_Parms.outSum);

// avec déplacement
a = Cell_Matrix(4, 0.75, 1, Cell_Parms.outWalk(1));
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outWalk(1));
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outWalk(2));

// avec altitude
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outFly(2, 0.125));
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outFlyPan(2, 0.125));

// avec programmation
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outFlyCycle(2, [1,1,2,2,3,3,4,4],[112,8]));
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [1,1,2,2,3,3,4,4],[112,8]));
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [4,4,1],[30,8,352]));

// la tortue !!! (déplacement en coordonnées polaires)
a = Cell_Matrix(8, 0.2, 0.5, Cell_Parms.outTurtleCycle(0.5, 0.25, [1,1,2,2,3,3,4,4],[112,8]));

// de grosses grilles (40%-45% CPU sur Core i5 avec supernova)
a = Cell_Matrix(12, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [1,1,2,2,3,3,4,4,5,5,6,6],[52,8]));
(
a = Cell_Matrix(12, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [6,6,1,1,2,2,3,3,4,4,5,5],
	[[22]++(172!3)++[112,52],8!6].lace(12)));
)
a = Cell_Matrix(12, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [1,6],[352,8]));
a = Cell_Matrix(12, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [6,6,1],[30,8,352]));
// une très grosse grille (65-70% CPU, mêmes conditions)
a = Cell_Matrix(14, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [1,7],[412,8]));
// une grille encore plus grosse (80% CPU, mêmes conditions)
a = Cell_Matrix(16, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [1,8],[472,8]));

// avec tous les paramètres
// paramètres par defaut (sauf sortie audio)
(
a = Cell_Matrix(8, 0.75, 0.5,
	// sortie audio
	Cell_Parms.outFlyPanCycle(2, [4,4,1],[30,8,352]),
	// probabilités, bruit(max, oct-, oct+), aux, retard, FM, ringmod
	Cell_Parms.gen([0.5,0.5,0.5,0.5,0.5], 1, -3, 3, 1, 8, 1, 1),
	// chaîne d'effets
	Cell_Parms.pipe(2, 3, 1, 1, [0.0, 6.0], 2, [0.25,0.75], [0.25,0.75]),
	// modulateurs
	Cell_Parms.mod(0, 6),
	// enregistrement et terminaison
	nil, nil);
)

// avec terminaison
a = Cell_Matrix(8, 0.75, 0.5, Cell_Parms.outFlyPanCycle(2, [4,4,1,1,2,2,3,3],[22,8]), stopAfter: 142);

// avec enregistrement
(
// paramètres standard
a = Cell_Matrix(12, 0.2, 0.5, Cell_Parms.outTurtleCycle(0.2, 0.05,
	// paramètres de programmation d'altitude
	[6,6,1,1,2,2,3,3,4,4,5,5,6],[30]++[8!6, 82!6].lace(12)),
	// paramètres d'enregistrement
	rec: Cell_Parms.rec(5, 5, 570), stopAfter: 570);
)

// avec représentation graphique
a = Cell_Matrix(8, 0.2, 0.5, Cell_Parms.outMapViewCycle(0.5, 0.25, [1,1,2,2,3,3,4,4],[112,8]));
a = Cell_Matrix(10, 0.2, 0.5, Cell_Parms.outMapViewCycle(0.5, 0.25, [1,1,2,2,3,3,4,4,5,5],[112,8]));
a = Cell_Matrix(12, 0.2, 0.5, Cell_Parms.outMapViewCycle(0.5, 0.25, [1,1,2,2,3,3,4,4,5,5,6,6],[112,8]));
// representation circulaire
a = Cell_Matrix(8, 0.2, 0.5, Cell_Parms.outCircleView(0.2, 0.05, 0.125));
a = Cell_Matrix(8, 0.2, 0.5, Cell_Parms.outCircleViewCycle(0.2, 0.05, [1,1,4,4],[30,300,30,8]));
a = Cell_Matrix(12, 0.2, 0.5, Cell_Parms.outCircleViewCycle(0.2, 0.05, [1,1,6,6],[30,300,30,8]));

// dôme (première version)
a = Cell_Dome([[4,6,8],4], 0.2);

// dôme (version ambisonique)
(
"SC_JACK_DEFAULT_OUTPUTS".setenv(nil); // couper la sortie pour ne pas avoir du b-format en direct
a = Cell_Matrix([4,4,4], 0.05, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'hoa'));
)

// sortie sur casque
// démarrer JConvolver d'abord

// de nombreux XRuns si on utilise des encodeurs HOA (trop nombreux ?)
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("jconvolver");
a = Cell_Matrix([4,4,4], 0.1, 4,
	Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa', 'jconvolver'));
)

// arrête la simulation
a.free;

// FOA

// impossible de faire marcher les décodeurs FOA Kernel, donc pas de sortie sur casque
// ici, sortie stéreo
// solution standard, après détour par 'ambdec'

(
"SC_JACK_DEFAULT_OUTPUTS".setenv("system");
a = Cell_Matrix(4!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa', 'stereo'));
)

// 40% CPU (i5)
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("system");
a = Cell_Matrix(5!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa', 'stereo'));
)
// 70% CPU (i5)
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("system");
a = Cell_Matrix(6!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa', 'stereo'));
)

// arrête la simulation
a.free;

// par contre on doit pouvoir sortir sur un schema standard de periphonie, à voir

// sortie vers le décodeur binaural ambitools
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("hoa_decoder_lebedev50_binaural");
a = Cell_Matrix(4!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa'));
)
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("hoa_decoder_lebedev50_binaural");
a = Cell_Matrix(5!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa'));
)

// arrête la simulation
a.free;

// en utilisant l'encodeur d'ordre 3 de AMB-LADSPA
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("hoa_decoder_lebedev50_binaural");
a = Cell_Matrix(4!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'amb_ladspa2'));
)
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("hoa_decoder_lebedev50_binaural");
a = Cell_Matrix(4!3, 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'amb_ladspa3'));
)

// toujours le même problème avec le HOA
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("ambdec");
a = Cell_Matrix([4,4,4], 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'hoa'));
)

// --> peut-être sortir directement les 64/125/216 canaux + les paramètres de spatialisation
// sur un encodeur à N sources (p.ex. créé par AmbiTools)

// avec ambdec, en mode stéreo p.ex.
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("ambdec");
a = Cell_Matrix([4,4,4], 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa'));
)
// grille de 5^3: 50% des ressources @i5
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("ambdec");
a = Cell_Matrix([5,5,5], 0.1, 4, Cell_Parms.outAmbi(1, 0.25, 0.125, 0.0625, 'foa'));
)

// arrête la simulation
a.free;

// Contrôle par joystick
(
"SC_JACK_DEFAULT_OUTPUTS".setenv("system");
a = Cell_Matrix(4!3, 0.25, 4, Cell_Parms.outAmbiJoy(1, 0.25, 0.125, 0.0625,
	[['axisn', [3,5]], ['axisn', [3,0]], ['axisp', [3,1]],
		['diff', [[1, 293],[1, 292]]], ['counter', [[1, 295], [1,294]]]],
	'foa', 'stereo'));
)
