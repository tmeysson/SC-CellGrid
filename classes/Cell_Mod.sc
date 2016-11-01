/*
Classe qui définit un modulateur standard, choisi au hasard parmi les définitions
*/
Cell_Mod : Synth {
	// les définitions de modulateurs
	classvar defs;

	// ajouter les définitions
	*addDefs {
		defs = [
			// constante
			SynthDef('modConst', {|out|
				Out.kr(out, DC.kr(Rand(0,1)));
			}),
			// oscillateur à basse fréquence (exponentielle sur [1, 64])
			SynthDef('modLFO', {|out|
				Out.kr(out, SinOsc.kr(2 ** Rand(0,6)).range(0,1));
			}),
			// ligne brisée aléatoire (fréquence idem)
			SynthDef('modLine', {|out|
				Out.kr(out, LFNoise1.kr(2 ** Rand(0,6)).range(0,1));
			}),
			// paliers aléatoires successifs (fréquence idem)
			SynthDef('modStair', {|out|
				Out.kr(out, LFNoise0.kr(2 ** Rand(0,6)).range(0,1));
			})
		];

		// ajouter les définitions
		defs.do({|item| item.add });
	}

	// constructeur: prend le groupe cible et la sortie demandée en paramètre
	*new {|group, out|
		^super.new(defs.choose.name, ['out', out], group);
	}
}