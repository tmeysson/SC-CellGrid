/*
Classe minimaliste qui encapsule un générateur et une chaîne d'effets
*/
Cell_Group : Group {
	// le générateur
	var gen;
	// la chaîne d'effets
	var <pipe;

	// constructeur: le groupe parallèle, la sortie demandée et les quatre entrées
	*new {|group, out, in1, in2, in3, in4|
		^super.new(group).init(out, in1, in2, in3, in4);
	}

	init {|out, in1, in2, in3, in4|
		// créer une chaine d'effets
		pipe = Cell_Pipe(this, out);
		// créer un générateur au dessus de la chaîne d'effets
		gen = Cell_Gen(this, pipe.in, in1, in2, in3, in4);
	}

	/*
	release {
		Routine({
			while({gen.isPlaying}, {0.25.wait});
			pipe.free;
		}).play;
		^super.release;
	}
	*/

	// suppression
	free {
		// il faut supprimer le générateur et la chaîne
		//gen.free;
		pipe.free;
		^super.free;
	}
}
		