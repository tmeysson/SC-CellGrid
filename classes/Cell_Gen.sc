/*
Cell_Gen est le générateur cellulaire de Cell Grid
Il s'agit d'un oscillateur à quatre entrées:
- modulation FM
- modulation RingMod
- modulation par retard variable
- une quatrième entrée mélangée telle quelle
Les quantités de modulation sont aléatoires (ou nulles suivant une probabilité définie)
Le résultat est envoyé sur le Bus demandé (entrée d'une chaîne d'effets)
L'arrêt (après fade out) est déclenché par le paramètre gate,
et déclenche l'arrêt du groupe tout entier
*/
Cell_Gen : Synth {
	// la définition du synthétiseur
	classvar defs, defWeights;

	// ajouter la définition du synthétiseur
	*addDefs {|weights = #[0.5, 0.5, 0.5, 0.5, 0.5], nseParms = #[1.0, -3.0, 3.0],
		fwdMax = 1.0, delMaxExp = 8.0, fmOct = 1.0, ampMax = 1.0|
		var nseMax = nseParms[0], nseLowOct = nseParms[1], nseHighOct = nseParms[2];

		defs = Array.newClear(32);
		defWeights = Array.newClear(32);

		/*
		'cellGenXXXXX': nom de la définition
		out:            le Bus de sortie
		freqMod:        le générateur FM
		ampMod:         le générateur de modulation en anneau
		delay:          le générateur de variation de retard
		forward:        le générateur de signal mélangé
		gate:           paramètre permettant de déclencher l'arrêt par fondu

		On génère toutes les combinaisons possibles de définitions,
		suivant la présence ou l'absence des modificateurs
		*/

		2.do({|i|
			// bruitage
			var p1 = if(i>0, {weights[0]}, {1-weights[0]});
			var fNoise = if(i>0, {
				{|body, freq|
					// mélange de bruit: quantité sur [0,nseMax]
					var noiseAmt = Rand(0.0, nseMax);
					// bande de bruit: fréquences de coupure basse et haute
					var noiseLow = freq * (2**Rand(nseLowOct,0.0));
					var noiseHigh = freq * (2**Rand(0.0,nseHighOct));
					// mélange de l'oscillateur
					((1 - noiseAmt) * body +
						// mélange de bruit
						(noiseAmt * LPF.ar(HPF.ar((
							WhiteNoise.ar), noiseLow), noiseHigh).clip(-1,1))) }
				}, {
					{|body, freq| body }
			});
			2.do({|j|
				// mélange d'entrée auxiliaire
				var p2 = if(j>0, {weights[1]}, {1-weights[1]});
				var fMix = if(j>0, {
				{|body|
						// quantité de mélange de l'entrée: sur [0,fwdMax]
						var fwdAmt = Rand(0.0, fwdMax);
						// contrôle d'entrée auxiliaire
						var forward = 'forward'.ir;
						// coefficient de mélange sur l'oscillateur
						(((1 - fwdAmt) * body) +
							// coefficient de mélange sur l'entrée mélangée
							(fwdAmt * InFeedback.ar(forward))) }
					}, {
					{|body| body }
			});
				2.do({|k|
					// retard variable
					var p3 = if(k>0, {weights[2]}, {1-weights[2]});
					var fDelay = if(k>0, {
						{|body|
							// quantité de retard: exponentielle sur [2**-14,2**(delMaxExp-14)]
							var delAmt = (2**Rand(-14.0, delMaxExp - 14.0));
							// entrée de retard variable
							var delay = 'delay'.ir;
							DelayL.ar(body,
								// retard maximum et retard instantané (projeté sur [0, delAmt])
								delAmt, delAmt * ((InFeedback.ar(delay)+1)*0.5)) }
						}, {
							{|body| body }
					});
					2.do({|l|
						// modulation de fréquence
						var p4 = if(l>0, {weights[3]}, {1-weights[3]});
						var fFM = if(l>0, {
							{|freq|
								// quantité de modulation de fréquence: [0,fmOct]
								var freqAmt = Rand(0.0, fmOct);
								// contrôle de modulation de fréquence
								var freqMod = 'freqMod'.ir;
								freq * (2 ** (InFeedback.ar(freqMod) * freqAmt)) }
							}, {
								{|freq| freq }
						});
						2.do({|m|
						// modulation d'amplitude
							var p5 = if(m>0, {weights[4]}, {1-weights[4]});
							var fAM = if(m>0, {
								{|body|
									// quantité de modulation d'amplitude: [0,ampMax]
									var ampAmt = Rand(0.0, ampMax);
									// contrôle de modulation d'amplitude
									var ampMod = 'ampMod'.ir;
									body * ((1 - ampAmt) + (InFeedback.ar(ampMod) * ampAmt)) }
								}, {
									{|body| body }
							});

							// mes récurfrères, mes récurseurs, reprenez avec moi tous en choeur !
							var index = (i*16)+(j*8)+(k*4)+(l*2)+m;
							// le poids probabiliste de la définition
							// la factorisation donne: defWeights.sum == 1
							defWeights[index] = p1 * p2 * p3 * p4 * p5;
							// la définition résultante
							defs[index] = SynthDef(("cellGen"+i+j+k+l+m).asSymbol,
								{|out, gate = 1|
									// fréquence de l'oscillateur (avec modulation)
									var freq = 2 ** Rand(7.0, 11.0);
									Out.ar(out,
										// enveloppe dynamique:
										// attaque: 1s, chute: 1s, entretien: 100%;
										// continue jusqu'à ce que gate devienne 0
										// arrête le groupe tout entier à la fin
										EnvGen.kr(Env.asr(1, 1, 1, 'lin'), gate, doneAction: 2) *
										fDelay.value(
											// modulation en anneau
											fAM.value(
												fNoise.value(
													fMix.value(
														// oscillateur sinusoïdal
														SinOsc.ar(
															// modulation de fréquence
															fFM.value(freq)
															// clip sur [-1,1]
														).clip(-1,1)
													), freq
												)
											)
										)
									);
								}
							);
						})
					})
				})
			})
		});

		// ajouter les définitions
		defs.do({|item| item.add });
	}

	/* constructeur
	group: le groupe de la cellule
	out: Bus d'entree de la chaîne d'effets
	freqMod, ampMod, delay, forward: paramètres du synthétiseur 'cellGen'
	*/
	*new {|group, out, freqMod, ampMod, delay, forward|
		^super.new(defs[defWeights.windex].name,
			// 'out' est l'entrée de la chaîne d'effets
			// les autres paramètres sont les Bus de sortie des cellules voisines
			['out', out, 'freqMod', freqMod, 'ampMod', ampMod,
				'delay', delay, 'forward', forward],
			// on ajoute dans le groupe de la cellule
			group).onFree({group.free});
	}

	// on utilise également les méthodes free et release de Synth, sans les modifier
}
				