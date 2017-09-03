// sortie AmbiSonic pour Cells
Cell_AmbiOut {
	classvar defs;

	var encoders;
	var decoderSynth, decBus;
	var debug;
	var posBus, rotBus, zoomBus, posGen;
	var joystick;

	*addDefs {|joyspec, encoder, decoder|
		var numOutChannels;

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
				if (joyspec.isNil) {
					# alpha, beta, gamma = ({LFNoise1.kr(1)} ! 3) * angspeed * prd * 1pi;
					speed = LFNoise1.kr(1, 0.5, 0.5) * linspeed * prd;
					zm = (LFNoise1.kr(1/60, 0.5, 0.5) * (size.reduce('min')/*/2*/ - 1)) + 1;
				} {
					# alpha, beta, gamma = ['zrot', 'yrot', 'xrot'].collect {|symb| In.kr(symb.ir)}
					* angspeed * prd * 1pi;
					speed = In.kr('spd'.ir) * linspeed * prd;
					zm = (MulAdd(In.kr('zoomin'.ir), 0.5, 0.5) * (size.reduce('min')/*/2*/ - 1)) + 1;
				};
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
				gain = max(0, (zm - d)) / (zm ** 2);
				// normalisation
				# x, y, z = [x,y,z] / d;
				// calcul des coordonnées polaires
				phi = asin(z);
				theta = atan(y/x) + (1pi * (x<0));
				// encodage AmbiSonic
				Out.ar(out,
					switch (encoder)
					{'foa'} {
						numOutChannels = 4;
						PanB.ar(In.ar(in) * vol, theta, phi, gain)
					}
					{'hoa'} {
						numOutChannels = 16;
						HOAEncoder.ar(3, In.ar(in) * vol, theta, phi, gain.ampdb)
					}
					{'amb_ladspa2'} {
						numOutChannels = 9;
						LADSPA.ar(16, 1967, In.ar(in) * vol, phi * (180/1pi), theta * (180/1pi));
					}
					{'amb_ladspa3'} {
						numOutChannels = 16;
						LADSPA.ar(16, 1965, In.ar(in) * vol, phi * (180/1pi), theta * (180/1pi));
					}
				);
			}),

			// // encodeur AmbiSonic (1 par cellule)
			// SynthDef('cellAmbiEnc', {
			// 	|in, out, vol, pos, rot, zoom, size  = #[1,1,1], ind = #[0,0,0]|
			// 	var x,y,z,d,rp;
			// 	var gain;
			// 	var theta, phi;
			// 	var zm = In.kr(zoom);
			// 	// calcul de la position relative
			// 	rp = (ind - In.kr(pos, 3)).collect {|e,i| var s = size[i]/2; e.wrap(s.neg, s)};
			// 	// coordonnées relatives
			// 	# x, y, z = ((rp!3) * In.kr(rot, 9).reshape(3,3)).collect(_.sum);
			// 	// calcul du gain
			// 	d = [x,y,z].squared.sum.sqrt;
			// 	gain = max(0, (zm - d)) / zm;
			// 	// normalisation
			// 	# x, y, z = [x,y,z] / d;
			// 	// calcul des coordonnées polaires
			// 	phi = asin(z);
			// 	theta = atan(y/x) + (1pi * (x<0));
			// 	// encodage AmbiSonic
			// 	Out.ar(out, HOAEncoder.ar(3, In.ar(in) * vol, theta, phi, gain.ampdb));
			// }),
			//
			// // encodeur AmbiSonic (1 par cellule)
			// // version FOA
			// SynthDef('cellAmbiEncFOA', {
			// 	|in, out, vol, pos, rot, zoom, size  = #[1,1,1], ind = #[0,0,0]|
			// 	var x,y,z,d,rp;
			// 	var gain;
			// 	var theta, phi;
			// 	var zm = In.kr(zoom);
			// 	// calcul de la position relative
			// 	rp = (ind - In.kr(pos, 3)).collect {|e,i| var s = size[i]/2; e.wrap(s.neg, s)};
			// 	// coordonnées relatives
			// 	# x, y, z = ((rp!3) * In.kr(rot, 9).reshape(3,3)).collect(_.sum);
			// 	// calcul du gain
			// 	d = [x,y,z].squared.sum.sqrt;
			// 	gain = max(0, (zm - d)) / zm;
			// 	// normalisation
			// 	# x, y, z = [x,y,z] / d;
			// 	// calcul des coordonnées polaires
			// 	phi = asin(z);
			// 	theta = atan(y/x) + (1pi * (x<0));
			// 	// encodage AmbiSonic
			// 	Out.ar(out, PanB.ar(In.ar(in) * vol, theta, phi, gain));
			// }),

			// décodeur AmbiSonic (global)
			// créer le décodeur demandé
			SynthDef('cellAmbiDec', {|out = 0, in, vol|
				var order = switch (encoder)
				{'foa'} {1}
				{'hoa'} {3}
				{'amb_ladspa2'} {2}
				{'amb_ladspa3'} {3};

				Out.ar(out,
					case
					// pas de décodeur
					{decoder.isNil} {Silent.ar}
					// décodeur HOA, avec ou sans filtres HRIR
					{decoder.first == 'hoa'}
					{
						var elt = decoder[1].ar(order, In.ar(in, numOutChannels),
							output_gains: vol.ampdb, hrir_Filters: decoder[2]);
						numOutChannels = elt.size;
						elt;
					}
					// décodeur FOA, méthode et paramètres au choix
					{decoder.first == 'foa'}
					{
						var elt = FoaDecode.ar(In.ar(in,4),
							FoaDecoderMatrix.performList(decoder[1], decoder[2..]));
						numOutChannels = elt.size;
						elt;
					}
				);
			})

			// // décodeur AmbiSonic (global)
			// SynthDef('cellAmbiDec', {|out = 0, in, vol|
			// 	Out.ar(out, HOADecLebedev26.ar(3, In.ar(in, 16), output_gains: vol.ampdb));
			// }),
			//
			// // décodeur AmbiSonic (global)
			// // version FOA
			// SynthDef('cellAmbiDecFOA', {|out = 0, in|
			// 	Out.ar(out, FoaDecode.ar(In.ar(in, 4), FoaDecoderMatrix.newStereo()));
			// })
		];

		defs.do({|item| item.add });

		if (joyspec.notNil) {Cell_Joystick.addDefs};

		^numOutChannels;
	}

	*new {|gateBus, busses, volume, speed, joyspec, encoder, decoder|
		^super.new.ambiOutInit(gateBus, busses, volume, speed, joyspec, encoder, decoder);
	}

	ambiOutInit {|gateBus, busses, volume, speed, joyspec, encoder, decoder|
		var linspeed, angspeed;
		// taille de la grille
		var sizeX = busses.size;
		var sizeY = busses.first.size;
		var sizeZ = busses.first.first.size;

		// sortie configurable
		var outBus = gateBus;

		var numOutChannels = switch (encoder)
		{'foa'} {4}
		{'hoa'} {16}
		{'amb_ladspa2'} {9}
		{'amb_ladspa3'} {16};

		# linspeed, angspeed = speed;

		// créer les Bus
		posBus = Bus.control(numChannels: 3).setSynchronous(*[0,0,0]);
		rotBus = Bus.control(numChannels: 9).setSynchronous(*[1,0,0,0,1,0,0,0,1]);
		zoomBus = Bus.control(numChannels: 1).setSynchronous(1);

		// créer le décodeur si nécessaire
		if (decoder.notNil)
		{
			decBus = Bus.audio(numChannels: numOutChannels);
			decoderSynth = Synth('cellAmbiDec', [out: gateBus, in: decBus, vol: volume]);
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

		// if (foa.notNil) {
		// 	// créer le décodeur si nécessaire
		// 	if (decode.notNil)
		// 	{
		// 		decBus = Bus.audio(numChannels: 4);
		// 		decoder = Synth('cellAmbiDecFOA', [out: gateBus, in: decBus, vol: volume]);
		// 		outBus = decBus;
		// 	};
		// 	// créer les encodeurs (pour chaque Bus de sortie)
		// 	encoders = busses.collect {|row, x|
		// 		row.collect {|col, y|
		// 			col.collect {|item, z|
		// 				Synth('cellAmbiEncFOA', [in: item, out: outBus, vol: volume, pos: posBus, rot: rotBus,
		// 				zoom: zoomBus, size: [sizeX, sizeY, sizeZ], ind: [x,y,z]]);
		// 			}
		// 		}
		// 	};
		// } {
		// 	// créer le décodeur si nécessaire
		// 	if (decode.notNil)
		// 	{
		// 		decBus = Bus.audio(numChannels: 16);
		// 		decoder = Synth('cellAmbiDec', [out: gateBus, in: decBus, vol: volume]);
		// 		outBus = decBus;
		// 	};
		// 	// créer les encodeurs (pour chaque Bus de sortie)
		// 	encoders = busses.collect {|row, x|
		// 		row.collect {|col, y|
		// 			col.collect {|item, z|
		// 				Synth('cellAmbiEnc', [in: item, out: outBus, vol: volume, pos: posBus, rot: rotBus,
		// 				zoom: zoomBus, size: [sizeX, sizeY, sizeZ], ind: [x,y,z]]);
		// 			}
		// 		}
		// 	};
		// };

		// créer le générateur de position
		if (joyspec.isNil) {
			posGen = Synth('ambi-posGen', [pos: posBus, rot: rotBus, zoom: zoomBus,
				size: [sizeX, sizeY, sizeZ], linspeed: linspeed, angspeed: angspeed]);
		} {
			var joyBusses;
			joystick = Cell_Joystick(joyspec);
			joyBusses = joystick.busses;
			posGen = Synth('ambi-posGen', [pos: posBus, rot: rotBus, zoom: zoomBus,
				size: [sizeX, sizeY, sizeZ], linspeed: linspeed, angspeed: angspeed,
				spd: joyBusses[0], zrot: joyBusses[1], yrot: joyBusses[2],
				xrot: joyBusses[3], zoomin: joyBusses[4]]);
		};
	}

	free {
		decoderSynth.free;
		encoders.flat.do(_.free);
		posGen.free;
		posBus.free; rotBus.free; zoomBus.free; decBus.free;
		joystick.free;
		^super.free;
	}
}
