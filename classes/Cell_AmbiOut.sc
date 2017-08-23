// sortie AmbiSonic pour Cells
Cell_AmbiOut {
	classvar defs;

	var encoders;
	var decoder, decBus;
	var debug;
	var posBus, rotBus, zoomBus, posGen;

	*addDefs {
		// HOADecLebedev26.loadHrirFilters(Server.default,
		// "/home/perso/ambitools/FIR/hrir/hrir_ku100_lebedev50");

		defs = [
			// générateur de position
			SynthDef('ambi-posGen', {
				|pos, rot, size = #[1,1,1], linspeed, angspeed = #[1,1,1], zoom|
				var prd = ControlDur.ir;
				var alpha, beta, gamma, speed, zm;
				var u,v,w,p;
				var u1, u2, v1, v2, w1, w2;
				// détermination de la variation
				// possibilité de remplacer par des contrôles GamePad
				# alpha, beta, gamma = LFNoise1.kr(1) * angspeed * prd * 1pi;
				speed = LFNoise1.kr(1, 0.5, 0.5) * linspeed * prd;
				zm = (LFNoise1.kr(1/60, 0.5, 0.5) * (size.reduce('min')/*/2*/ - 1)) + 1;
				// lecture des vecteurs
				# u, v, w = In.kr(rot, 9).reshape(3,3);
				p = In.kr(pos, 3);
				// calcul du nouveau trièdre
				u1 = (u * cos(alpha)) + (v * sin(alpha));
				v1 = (v * cos(alpha)) - (u * sin(alpha));
				u2 = (u1 * cos(beta)) + (w * sin(beta));
				w1 = (w * cos(beta)) - (u1 * sin(beta));
				v2 = (v1 * cos(gamma)) + (w1 * sin(gamma));
				w2 = (w1 * cos(gamma)) - (v1 * sin(gamma));
				// calcul de la nouvelle position
				p = (p + (speed * u2)) % size;
				// écriture des valeurs
				Out.kr(pos, p);
				Out.kr(rot, u2 ++ v2 ++ w2);
				Out.kr(zoom, zm);
			}),

			// encodeur AmbiSonic (1 par cellule)
			SynthDef('cellAmbiEnc', {
				|in, out, vol, pos, rot, zoom, size  = #[1,1,1], ind = #[0,0,0]|
				var x,y,z,d,rp;
				var gain;
				var theta, phi;
				var zm = In.kr(zoom);
				// calcul de la position relative
				rp = (ind - In.kr(pos, 3)).collect {|e,i| var s = size[i]/2; e.wrap(s.neg, s)};
				// coordonnées relatives
				# x, y, z = ((rp!3) * In.kr(rot, 9).reshape(3,3)).collect(_.sum);
				// calcul du gain
				d = [x,y,z].squared.sum.sqrt;
				gain = max(0, (zm - d)) / zm;
				// normalisation
				# x, y, z = [x,y,z] / d;
				// calcul des coordonnées polaires
				phi = asin(z);
				theta = atan(y/x) + (1pi * (x<0));
				// encodage AmbiSonic
				Out.ar(out, HOAEncoder.ar(3, In.ar(in) * vol, theta, phi, gain.ampdb));
			}),

			// encodeur AmbiSonic (1 par cellule)
			// version FOA
			SynthDef('cellAmbiEncFOA', {
				|in, out, vol, pos, rot, zoom, size  = #[1,1,1], ind = #[0,0,0]|
				var x,y,z,d,rp;
				var gain;
				var theta, phi;
				var zm = In.kr(zoom);
				// calcul de la position relative
				rp = (ind - In.kr(pos, 3)).collect {|e,i| var s = size[i]/2; e.wrap(s.neg, s)};
				// coordonnées relatives
				# x, y, z = ((rp!3) * In.kr(rot, 9).reshape(3,3)).collect(_.sum);
				// calcul du gain
				d = [x,y,z].squared.sum.sqrt;
				gain = max(0, (zm - d)) / zm;
				// normalisation
				# x, y, z = [x,y,z] / d;
				// calcul des coordonnées polaires
				phi = asin(z);
				theta = atan(y/x) + (1pi * (x<0));
				// encodage AmbiSonic
				Out.ar(out, PanB.ar(In.ar(in) * vol, theta, phi, gain));
			}),

			// décodeur AmbiSonic (global)
			SynthDef('cellAmbiDec', {|out = 0, in, vol|
				Out.ar(out, HOADecLebedev26.ar(3, In.ar(in, 16), output_gains: vol.ampdb));
			}),

			// décodeur AmbiSonic (global)
			// version FOA
			SynthDef('cellAmbiDecFOA', {|out = 0, in|
				Out.ar(out, FoaDecode.ar(In.ar(in, 4), FoaDecoderMatrix.newStereo()));
			})
		];

		defs.do({|item| item.add });
	}

	*new {|gateBus, busses, volume, linspeed = 1, angspeed = #[1,1,1], decode, foa|
		^super.new.ambiOutInit(gateBus, busses, volume, linspeed, angspeed, decode, foa);
	}

	ambiOutInit {|gateBus, busses, volume, linspeed, angspeed, decode, foa|
		// taille de la grille
		var sizeX = busses.size;
		var sizeY = busses.first.size;
		var sizeZ = busses.first.first.size;

		// sortie configurable
		var outBus = gateBus;
		// créer les Bus
		posBus = Bus.control(numChannels: 3).setSynchronous(*[0,0,0]);
		rotBus = Bus.control(numChannels: 9).setSynchronous(*[1,0,0,0,1,0,0,0,1]);
		zoomBus = Bus.control(numChannels: 1).setSynchronous(1);
		if (foa.notNil) {
			// créer le décodeur si nécessaire
			if (decode.notNil)
			{
				decBus = Bus.audio(numChannels: 4);
				decoder = Synth('cellAmbiDecFOA', [out: gateBus, in: decBus, vol: volume]);
				outBus = decBus;
			};
			// créer les encodeurs (pour chaque Bus de sortie)
			encoders = busses.collect {|row, x|
				row.collect {|col, y|
					col.collect {|item, z|
						Synth('cellAmbiEncFOA', [in: item, out: outBus, vol: volume, pos: posBus, rot: rotBus,
							zoom: zoomBus, size: [sizeX, sizeY, sizeZ], ind: [x,y,z]]);
					}
				}
			};
		} {
			// créer le décodeur si nécessaire
			if (decode.notNil)
			{
				decBus = Bus.audio(numChannels: 16);
				decoder = Synth('cellAmbiDec', [out: gateBus, in: decBus, vol: volume]);
				outBus = decBus;
			};
			// créer les encodeurs (pour chaque Bus de sortie)
			encoders = busses.collect {|row, x|
				row.collect {|col, y|
					col.collect {|item, z|
						Synth('cellAmbiEnc', [in: item, out: outBus, vol: volume, pos: posBus, rot: rotBus,
							zoom: zoomBus, size: [sizeX, sizeY, sizeZ], ind: [x,y,z]]);
					}
				}
			};
		};
		// créer le générateur de position
		posGen = Synth('ambi-posGen', [pos: posBus, rot: rotBus, zoom: zoomBus,
		size: [sizeX, sizeY, sizeZ], linspeed: linspeed, angspeed: angspeed]);
	}

	free {
		decoder.free;
		encoders.flat.do(_.free);
		posGen.free;
		posBus.free; rotBus.free; zoomBus.free; /*decBus.free;*/
		^super.free;
	}
}
