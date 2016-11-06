/*
Classe principale de Cell Grid
Produit une grille carrée de cellules (renouvellées périodiquement),
qui prennent leurs entrées sur les sorties de leurs voisines
Produit également un module de sortie au choix
Possibilité d'enregistrer la sortie
*/
Cell_Matrix {
	// grille des Bus de sortie des cellules
	var busses;
	// grille des générateurs des cellules et groupe parallèle englobant
	var cells, cellParGroup;
	// module de sortie
	var out;
	// processus principal (en raison du mode synchrone)
	var thread;
	// processus de renouvellement des cellules
	var renew;
	// processus de terminaison
	var end;
	// taille de la grille (carré de gridSize*gridSize)
	var gridSize;
	// module d'enregistrement, et variable indiquant si l'enregistrement est activé
	var recorder, isRec;

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
	- ['end', speed, program]:     idem sauf que l'altitude est déterminée par un programme
	\                              [[altinit, altitude, ...],[temps, ...]] (s'arrête à la fin)

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
	*/
	*new {|size = 2, volume = 0, renewalTime = 4,
		outParms = #['sum'], genParms, pipeParms, modParms,
		rec, stopAfter|
		^super.new.init(size, volume, renewalTime,
			outParms, genParms, pipeParms, modParms,
			rec, stopAfter);
	}

	init {|size, volume, renewalTime, outParms, genParms, pipeParms, modParms, rec, stopAfter|
		// initialisation de la taille de la grille
		gridSize = size;

		// création du fil d'exécution
		thread = Routine({
			// vérifier la nécessité d'augmenter les ressources (taille mémoire, nombre de Bus audio)
			if((Server.default.options.numAudioBusChannels < 2048) ||
				(Server.default.options.memSize < (128 * 1024)) ||
				(Server.default.options.maxNodes < 2048) ||
				(Server.program == "exec scsynth"), {
					Server.default.quit;
					Server.supernova;
					Server.default.options.maxNodes = 2048;
					Server.default.options.numAudioBusChannels = 2048;
					Server.default.options.memSize = 128 * 1024;
			});
			// démarrer le serveur et attendre la synchro
			Server.default.bootSync;

			// ajouter les définitions de modules (générateur, chaîne d'effets, modulateurs)
			Cell_Gen.addDefs(genParms);
			Cell_Pipe.addDefs(pipeParms);
			Cell_Mod.addDefs(modParms);
			// suivant le type de sortie choisie, ajouter les définitions correspondantes
			switch(outParms[0],
				// somme des sorties
				'sum', { Cell_SumOut.addDef },
				// déplacement à l'intérieur de la grille
				'walk', { Cell_WalkOut.addDefs },
				// déplacement avec portée (altitude) variable (aléatoire ou cyclique)
				'fly', { Cell_FlyOut.addDefs(
					if(outParms[2].isSequenceableCollection, {outParms[2]}, {nil})
				)},
				// idem avec pan stéréo
				'flypan', { Cell_FlyPanOut.addDefs(
					if(outParms[2].isSequenceableCollection, {outParms[2]}, {nil})
				)},
				// idem avec altitude et fin programmée
				'end', { Cell_EndOut.addDefs(outParms[2]) },
			);
			// attendre la synchro après ajout des définitions
			Server.default.sync;

			// dans le cas où on demande l'enregistrement
			if (rec.notNil, {
				// noter le fait que l'enregistrement est activé
				isRec = true;
				// créer un module d'enregistrement (fadeIn, fadeOut, time, path)
				recorder = Cell_Record(rec[0][0], rec[0][1], rec[0][2], rec[1]);
				// dans le cas contraire noter que l'enregistrement est inactif
				}, { isRec = false; }
			);

			// créer les Bus de sortie
			busses = Array.fill2D(gridSize, gridSize, { Bus.audio });

			// on procède de la fin de la chaîne vers le début
			// de façon à assurer la causalité du calcul
			// (et donc la présence effective des valeurs sur les Bus audio)

			// créer le module de sortie
			switch(outParms[0],
				'sum', {out = Cell_SumOut(busses.inject([],{|acc, item| acc ++ item}), volume)},
				'walk', {out = Cell_WalkOut(busses, volume, outParms[1])},
				'fly', { out = Cell_FlyOut(busses, volume, outParms[1], outParms[2])},
				'flypan', { out = Cell_FlyPanOut(busses, volume, outParms[1], outParms[2])},
				'end', { out = Cell_EndOut(busses, volume, this, outParms[1], outParms[2])},
			);

			// créer un groupe parallèle pour les cellules
			cellParGroup = ParGroup();
			// créer les cellules (générateurs, chaînes d'effets et modulateurs)
			cells = Array.fill2D(gridSize, gridSize, {|x, y|
				this.newCell(x, y);
			});

			renew = Routine({
				// démarrer le renouvellement des cellules
				{
					// adresse de la cellule à renouveller
					var x, y;
					// attendre la période demandée
					renewalTime.wait;
					// choisir une adresse au hasard (entier sur [0, gridSize-1])
					x = gridSize.rand;
					y = gridSize.rand;
					// arrêter la cellule (la méthode release permet de déclencher la chute)
					cells[x][y].release;
					// créer une nouvelle cellule
					cells[x][y] = this.newCell(x, y);
					// boucle infinie
				}.loop;
				// lancer le processus principal
			}).play;

			if(stopAfter.notNil,
				{
					end = Routine({
						stopAfter.wait;
						if(isRec, {4.wait});
						this.free;
					}).play;
				}
			);
		}).play;

	}

	// créer une cellule à l'adresse indiquée
	newCell {|x, y|
		// Bus d'entrée: les sorties des cellules voisines, en ordre aléatoire
		var inBusses = [
			busses[(x+1)%gridSize][y],
			busses[x][(y+1)%gridSize],
			busses[(x-1)%gridSize][y],
			busses[x][(y-1)%gridSize]
		].scramble;
		// on appelle simplement le générateur de cellules, avec l'adresse et les Bus d'entrée
		^Cell_Group(cellParGroup, busses[x][y], inBusses[0], inBusses[1], inBusses[2], inBusses[3]);
	}

	// arrêt de la grille
	free {
		// arrêt du renouvellement et de la terminaison
		renew.stop;

		Routine({
			// arrêter les cellules (en déclenchant la chute)
			cells.do({|row| row.do({|item| item.release})});
			// attendre l'arrêt
			2.wait;
			// supprimer le groupe parallèle
			cellParGroup.free;
			// supprimer la sortie
			out.free;
			// supprimer les Bus
			busses.do({|row| row.do({|item| item.free})});
			// si l'enregistrement est actif, l'arrêter
			if (isRec, { recorder.free });
			// si la terminaison n'est pas encore atteinte, la supprimer
			if(end.notNil, {end.stop});
		}).play;

		^super.free;
	}
}