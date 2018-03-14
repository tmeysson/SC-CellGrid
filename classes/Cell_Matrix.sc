/*
Classe principale de Cell Grid
Produit une grille carrée de cellules (renouvellées périodiquement),
qui prennent leurs entrées sur les sorties de leurs voisines
Produit également un module de sortie au choix
Possibilité d'enregistrer la sortie
*/
Cell_Matrix {
	// fade in/out global
	var gateSynth, gateBus;
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
	// dimensions de la grille (gridSize[0]*gridSize[1]*...)
	var gridSize;
	// module d'enregistrement, et variable indiquant si l'enregistrement est activé
	var recorder, isRec;
	// module de visualisation
	var view, viewMap, viewRefresh;

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
		rec, stopAfter, useNova = true, initWait = 0|
		^super.new.init(size, volume, renewalTime,
			outParms, genParms, pipeParms, modParms,
			rec, stopAfter, useNova, initWait);
	}

	init {|size, volume, renewalTime, outParms, genParms, pipeParms, modParms,
		rec, stopAfter, useNova, initWait|
		// nombre de sorties, suivant les sorties système
		// ou bien le nombre spécifié (pour les installations type dôme)
		var numOutChannels =
		if("jack_lsp|grep system:playback|wc -l".unixCmdGetStdOut.asInteger >= 4)
		{ 4 } { 2 };

		// initialisation de la taille de la grille
		gridSize = size;
		// gestion simple des tailles en 2D
		if (gridSize.isNumber) {gridSize = gridSize ! 2};

		// création du fil d'exécution
		thread = Routine({
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
				// idem avec tortue (progression en coord polaires)
				'turtle', { Cell_TurtleOut.addDefs(
					if(outParms[3].isSequenceableCollection, {outParms[3]}, {nil})
				)},
				// idem avec représentation graphique
				'mapview', { Cell_TurtleOut.addDefs(
					if(outParms[3].isSequenceableCollection, {outParms[3]}, {nil}));
					viewMap = Array.fillND(gridSize, { nil });
				},
				'circleview', { Cell_TurtleOut.addDefs(
					if(outParms[3].isSequenceableCollection, {outParms[3]}, {nil}));
					viewMap = Array.fillND(gridSize, { nil });
				},
				'ambi', { numOutChannels = Cell_AmbiOut.addDefs(
					*outParms[2..]
				)},
				'vbap', { numOutChannels = Cell_VBAPOut.addDefs(
					*outParms[3..]
				)}

			);

			// ajouter le fade in/out global
			SynthDef('globalGate', {|out, in, gate = 1|
				Out.ar(out,
					// enveloppe dynamique:
					// attaque: 5s, chute: 5s, entretien: 100%;
					// continue jusqu'à ce que gate devienne 0
					// arrête le groupe tout entier à la fin
					EnvGen.kr(Env.asr(5, 1, 5, 'lin'), gate, doneAction: 2) *
					In.ar(in, numOutChannels));
			}).add;

			// vérifier la nécessité d'augmenter les ressources (taille mémoire, nombre de Bus audio)
			if((Server.default.options.numAudioBusChannels < 2048) ||
				(Server.default.options.memSize < (128 * 1024)) ||
				(Server.default.options.maxNodes < 2048) ||
				(useNova != (Server.program == "exec supernova")) ||
				(Server.default.options.numOutputBusChannels < numOutChannels)) {
				Server.default.quit;
				// !! Ne fonctionne pas avec HOAEncoder !!
				// utiliser useNova = false
				if (useNova) {Server.supernova} {Server.scsynth};
				Server.default.options.numOutputBusChannels = numOutChannels;
				Server.default.options.maxNodes = 2048;
				Server.default.options.numAudioBusChannels = 2048;
				Server.default.options.memSize = 128 * 1024;
			};

			// if(Server.default.options.numOutputBusChannels < numOutChannels)
			// {
			// 	Server.default.quit;
			// 	Server.default.options.numOutputBusChannels = numOutChannels;
			// };
			// démarrer le serveur et attendre la synchro
			Server.default.bootSync;

			// attendre la synchro après ajout des définitions
			// Server.default.sync;

			// dans le cas où on demande l'enregistrement
			if (rec.notNil, {
				// noter le fait que l'enregistrement est activé
				isRec = true;
				// créer un module d'enregistrement (fadeIn, fadeOut, time, path)
				recorder = Cell_Record(rec[0][0], rec[0][1], rec[0][2], rec[1]);
				// dans le cas contraire noter que l'enregistrement est inactif
				}, { isRec = false; }
			);

			// créer le Bus du fade in/out
			// le nombre de canaux dépend du nombre de sorties
			gateBus = Bus.audio(numChannels: numOutChannels);

			// créer les Bus de sortie
			busses = Array.fillND(gridSize, {Bus.audio});

			// on procède de la fin de la chaîne vers le début
			// de façon à assurer la causalité du calcul
			// (et donc la présence effective des valeurs sur les Bus audio)

			// créer le fade in/out
			gateSynth = Synth('globalGate', ['out', 0, 'in', gateBus]);

			// créer le module de sortie
			switch(outParms[0],
				'sum', {out = Cell_SumOut(busses.inject([],{|acc, item| acc ++ item}), volume)},
				'walk', {out = Cell_WalkOut(busses, volume, outParms[1])},
				'fly', { out = Cell_FlyOut(busses, volume, outParms[1], outParms[2])},
				'flypan', { out = Cell_FlyPanOut(busses, volume, outParms[1], outParms[2])},
				'turtle', { out = Cell_TurtleOut(gateBus, busses, volume,
					outParms[1], outParms[2], outParms[3])},
				'mapview', { out = Cell_TurtleOut(gateBus, busses, volume,
					outParms[1], outParms[2], outParms[3])},
				'circleview', { out = Cell_TurtleOut(gateBus, busses, volume,
					outParms[1], outParms[2], outParms[3])},
				'ambi', { out = Cell_AmbiOut(gateBus, busses, volume, *outParms[1..])},
				'vbap', { out = Cell_VBAPOut(gateBus, busses, volume, *outParms[1..])}
			);

			// créer un groupe parallèle pour les cellules
			cellParGroup = ParGroup();

			// créer les cellules (générateurs, chaînes d'effets et modulateurs)
			cells = Array.fillND(gridSize, {|... indexes|
				initWait.wait;
				this.newCell(indexes);
			});

			renew = Routine({
				// démarrer le renouvellement des cellules
				{
					// adresse de la cellule à renouveller
					// choisir une adresse au hasard (liste d'entiers sur [0,...],gridSize-[1,...])
					var ind = gridSize.rand;
					// attendre la période demandée
					renewalTime.wait;
					// arrêter la cellule (la méthode release permet de déclencher la chute)
					cells.atND(ind).release;
					// créer une nouvelle cellule
					cells.putND(ind, this.newCell(ind));
					// boucle infinie
				}.loop;
				// lancer le processus principal
			}).play;

			// si requis, créer la vue
			switch(outParms[0],
				'mapview', {
					{view = Cell_Map(viewMap)}.defer;
					viewRefresh = Routine({
						{
							0.1.wait;
							{view.refresh}.defer;
							out.posBus.getn(4, {|pos| view.setPos(pos)});
						}.loop;
					}).play;
				},
				'circleview', {
					{view = Cell_CircleMap(viewMap)}.defer;
					viewRefresh = Routine({
						{
							0.1.wait;
							{view.refresh}.defer;
							out.posBus.getn(4, {|pos| view.setPos(pos)});
						}.loop;
					}).play;
				}
			);

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
	newCell {|indexes|
		// Bus d'entrée: les sorties des cellules voisines
		var rec = {|t,i,d|
			var first = i.first;
			var size = d.first;
			if (i.size > 1) {
				rec.(t[first], i[1..], d[1..]) ++
				[t[first-1%size].atND(i[1..])] ++
				[t[first+1%size].atND(i[1..])]
			} {
				[t[first-1%size], t[first+1%size]]
			}
		};

		var inBusses = rec.(busses, indexes, gridSize);

		// ordre aléatoire des directions
		var shuffle = (0..gridSize.size*2-1).scramble[..3];
		// on appelle le générateur de cellules, avec l'adresse
		// et les Bus d'entrée en ordre aléatoire
		var cell = Cell_Group(cellParGroup, busses.atND(indexes),
			*inBusses[shuffle]);
		// si la vue est activée, ajouter les informations dans la carte
		if(viewMap.notNil, {
			viewMap.putND(indexes,[cell.gen.mode, shuffle]);
		});
		// retourner la cellule
		^cell;
	}

	// arrêt de la grille
	free {
		// arrêt du renouvellement et de la terminaison
		renew.stop;

		Routine({
			// arrêter la sortie globale avec .release
			gateSynth.release;
			// gateSynth.set('gate', 0);
			// attendre l'arrêt
			5.wait;
			// arrêter les cellules (en déclenchant la chute)
			cells.flat.do {|item| item.release};
			// attendre l'arrêt
			2.wait;
			// si la vue est active, l'arrêter
			if(view.notNil, {
				viewRefresh.stop;
				{view.close}.defer;
			});
			// supprimer le groupe parallèle
			cellParGroup.free;
			// supprimer la sortie
			out.free;
			// supprimer les Bus
			gateBus.free;
			busses.flat.do {|item| item.free};
			// si l'enregistrement est actif, l'arrêter
			if (isRec, { recorder.free });
			// si la terminaison n'est pas encore atteinte, la supprimer
			if(end.notNil, {end.stop});
		}).play;

		^super.free;
	}
}