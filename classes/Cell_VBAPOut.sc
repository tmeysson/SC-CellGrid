// sortie VBAP pour Cells
Cell_VBAPOut {
	classvar defs;

	var buffer;

	var sources, outGroup;
	var posBus, rotBus, zoomBus, posGen;
	var joystick;

	*addDefs {|joyspec, speakerArray|
		var numOutChannels = speakerArray.numSpeakers;

		defs = [
			// générateur de position
			SynthDef('vbap-posGen', {
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
					zm = (LFNoise1.kr(1/60, 0.25, 0.25) * (size.reduce('min') - 2)) + 1;
				} {
					# alpha, beta, gamma = ['zrot', 'yrot', 'xrot'].collect {|symb| In.kr(symb.ir)}
					* angspeed * prd * 1pi;
					speed = In.kr('spd'.ir) * linspeed * prd;
					zm = (MulAdd(In.kr('zoomin'.ir), 0.25, 0.25) * (size.reduce('min') - 2)) + 1;
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

			// sortie VBAP (1 par cellule)
			SynthDef('cellVBAPOut', {
				|in, out, vol, pos, rot, zoom, size  = #[1,1,1], ind = #[0,0,0], bufnum|
				var x,y,z,d,rp;
				var gain;
				var theta, phi;
				var zm = In.kr(zoom);
				// calcul de la position relative
				rp = (ind - In.kr(pos, 3)).collect {|e,i| var s = size[i]/2; e.wrap2(s)};
				// coordonnées relatives
				# x, y, z = ((rp!3) * In.kr(rot, 9).reshape(3,3)).collect(_.sum);
				// calcul du gain
				d = [x,y,z].squared.sum.sqrt.max(1e-32);
				gain = max(0, (zm - d)) / (zm ** 2);
				// normalisation
				# x, y, z = [x,y,z] / d;
				// calcul des coordonnées polaires (en degrés)
				phi = (asin(z) * (180 / 1pi)).clip2(90);
				theta = ((atan(y/(x.sign * x.abs.max(1e-24))) + (1pi * (x<0))) *
					(180 / 1pi)).wrap2(180);
				// création d'une source VBAP
				Out.ar(out, VBAP.ar(numOutChannels, In.ar(in) * gain * vol,
				bufnum, theta, phi, 0));
			}),
		];

		defs.do({|item| item.add });

		if (joyspec.notNil) {Cell_Joystick.addDefs};

		^numOutChannels;
	}

	*new {|gateBus, busses, volume, speed, joyspec, speakerArray|
		^super.new.vbapOutInit(gateBus, busses, volume, speed, joyspec, speakerArray);
	}

	vbapOutInit {|gateBus, busses, volume, speed, joyspec, speakerArray|
		var numOutChannels = speakerArray.numSpeakers;

		var linspeed, angspeed;
		// taille de la grille
		var sizeX = busses.size;
		var sizeY = busses.first.size;
		var sizeZ = busses.first.first.size;

		// sortie configurable
		var outBus = gateBus;

		// while {Server.default.serverRunning.not.postln} {1.wait};
		buffer = speakerArray.loadToBuffer;
		Server.default.sync;

		# linspeed, angspeed = speed;

		// créer les Bus
		posBus = Bus.control(numChannels: 3).setSynchronous(*[0.5,0.5,0.5]);
		// posBus = Bus.control(numChannels: 3).setSynchronous(*[0,0,0]);
		rotBus = Bus.control(numChannels: 9).setSynchronous(*[1,0,0,0,1,0,0,0,1]);
		zoomBus = Bus.control(numChannels: 1).setSynchronous(1);

		// créer les sources (pour chaque Bus de sortie)
		outGroup = ParGroup();
		sources = busses.collect {|row, x|
			row.collect {|col, y|
				col.collect {|item, z|
					Synth('cellVBAPOut', [in: item, out: outBus, vol: volume, pos: posBus, rot: rotBus,
						zoom: zoomBus, size: [sizeX, sizeY, sizeZ], ind: [x,y,z],
						bufnum: buffer.bufnum], outGroup);
				}
			}
		};

		// créer le générateur de position
		if (joyspec.isNil) {
			posGen = Synth('vbap-posGen', [pos: posBus, rot: rotBus, zoom: zoomBus,
				size: [sizeX, sizeY, sizeZ], linspeed: linspeed, angspeed: angspeed]);
		} {
			var joyBusses;
			joystick = Cell_Joystick(joyspec);
			joyBusses = joystick.busses;
			posGen = Synth('vbap-posGen', [pos: posBus, rot: rotBus, zoom: zoomBus,
				size: [sizeX, sizeY, sizeZ], linspeed: linspeed, angspeed: angspeed,
				spd: joyBusses[0], zrot: joyBusses[1], yrot: joyBusses[2],
			xrot: joyBusses[3], zoomin: joyBusses[4]]);
		};
	}

	free {
		sources.flat.do(_.free);
		posGen.free;
		posBus.free; rotBus.free; zoomBus.free;
		joystick.free;
		buffer.free;
		^super.free;
	}
}
