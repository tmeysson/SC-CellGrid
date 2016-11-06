/*
Classe qui définit une chaîne d'effets sur un Bus de sortie
Les effets sont appliqués selon une certaine probabilité, dans l'ordre prédéfini
*/
Cell_Pipe {
	// les définitions des effets et leur poids probabiliste
	classvar defs, defWeights;
	// la chaîne d'effets, les modulateurs, et les Bus associés
	var chain, mods, busses;
	// l'entrée de la chaîne d'effets, rendue accessible pour le synthétiseur en entrée
	var <in;

	// définir les synthétiseurs qui correspondent aux différents effets
	*addDefs {|pipeParms|
		var expected,
		delFixMax, delAmtMax,
		pitchAmt,
		ampModFreqInt,
		distFact,
		revRoomInt, revDampInt;

		if(pipeParms.isNil, {pipeParms = Cell_Parms.pipe});
		expected = pipeParms[0];
		delFixMax = pipeParms[1];
		delAmtMax = pipeParms[2];
		pitchAmt = pipeParms[3];
		ampModFreqInt = pipeParms[4];
		distFact = pipeParms[5];
		revRoomInt = pipeParms[6];
		revDampInt = pipeParms[7];

		defs = [
			// out et in sont les Bus de sortie et d'entrée, mod est celui du modulateur
			// retard
			SynthDef('pipeDelay', {|out, in, mod|
				// retard fixe
				var fixed = Rand(0.0, delFixMax);
				// retard variable
				var amt = Rand(0.0, delAmtMax);
				Out.ar(out, DelayL.ar(In.ar(in), fixed + amt, fixed + (In.kr(mod) * amt)));
			}),
			// pitch shift
			SynthDef('pipePitch', {|out, in, mod|
				Out.ar(out, PitchShift.ar(In.ar(in), 0.05,
					// ratio sur 2**([-2, 2] * pitchAmt)
					2 ** ((In.kr(mod) -0.5) * 4 * pitchAmt), 0, 0.0005).clip(-1,1));
			}),
			// pitch shift quantifié
			SynthDef('pipeStairPitch', {|out, in, mod|
				// quantification sur  {0,..,4}
				var quant = (In.kr(mod) * 5).floor;
				// octave sur {-1,..,1} suivant les multiples de 2
				var oct = (quant/2).floor - 1;
				// quinte naturelle suivant le reste impair
				var fifth = quant % 2;
				// le ratio est la valeur d'octave sur {0.5, 1, 2}
				// multiplié par la valeur de quinte sur {1, 1.5}
				var ratio = (2**oct) * (1.5**fifth);
				Out.ar(out, PitchShift.ar(In.ar(in), 0.05, ratio, 0, 0.0005).clip(-1,1));
			}),
			// modulation d'amplitude
			SynthDef('pipeAmp', {|out, in, mod|
				// la fréquence de modulation varie exponentiellement sur 2**ampModFreqInt(Array)
				var freq = 2 ** Rand(ampModFreqInt[0], ampModFreqInt[1]);
				Out.ar(out, (In.ar(in) *
					(1 + (SinOsc.kr(freq).range(-1,0) * In.kr(mod)))).clip(-1,1));
			}),
			// distorsion
			SynthDef('pipeDist', {|out, in, mod|
				// on élève le signal à une puissance sur [2**(-distFact), 1]
				Out.ar(out, (In.ar(in) ** (2**(In.kr(mod).neg * distFact))).clip(-1,1));
			}),
			// reverb
			SynthDef('pipeRev', {|out, in, mod|
				Out.ar(out, FreeVerb.ar(In.ar(in), In.kr(mod),
					// taille de la pièce
					Rand(revRoomInt[0],revRoomInt[1]),
					// amortissement des aigüs
					Rand(revDampInt[0],revDampInt[1])).clip(-1,1));
			})
			// il faut inverser l'ordre car les effets sont ajoutés du dernier au premier
		].reverse;

		// les poids de probabilité des différents effets
		defWeights = [2, 1, 1, 2, 2, 2];
		// on calcule suivant l'espérance souhaitée
		defWeights = (defWeights * (expected/defWeights.sum)).reverse;

		// ajouter les effets dans le serveur
		defs.do({|item| item.add });
	}

	// création d'une chaîne d'effets
	*new {|group, out|
		^super.new.init(group, out);
	}

	// group est le groupe de la cellule
	// out est le Bus de sortie demandé
	init {|group, out|
		// le Bus de sortie au cours du calcul
		var curOut = out;
		//var numDefs = defs.size;

		// on utilise des List pour pouvoir appeler .add sans problème
		// la chaîne d'effets
		chain = List(0);
		// les modulateurs associés
		mods = List(0);
		// les Bus de liaison associés
		busses = List(0);

		// on itère sur les définitions
		defs.do({|def, i|
			// selon la probabilité associée, on crée ou non l'effet
			if(defWeights[i].coin, {
				// créer les Bus de liaison
				var newIn = Bus.audio;
				var modIn = Bus.control;

				// on ajoute les Bus à la liste
				busses.add(newIn);
				busses.add(modIn);

				// on ajoute le Synth correspondant dans le groupe de la cellule
				chain.add(Synth(def.name, ['out', curOut, 'in', newIn, 'mod', modIn], group)
				);

				// on ajoute également un modulateur
				mods.add(Cell_Mod(group, modIn));

				// le Bus courant devient le Bus d'entrée du premier effet
				curOut = newIn;
			});
		});

		// à la fin, le Bus d'entrée est l'entrée du premier effet
		in = curOut;
	}

	// supprimer la chaîne
	free {
		// libérer les Bus
		busses.do({|item| item.free });
		// les Synth (effets, modulateurs) sont pris en charge par Cell_Group.free
		// en conséquence cette méthode ne devrait pas être appellée autrement
		^super.free;
	}
}