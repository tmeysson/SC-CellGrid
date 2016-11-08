Cell_MapOut {
	classvar defs;

	var synths;
	var <posBus, posGen;

	*addDefs {|altCycle = nil|
		defs = [
			SynthDef('cellMapOut', {|out = 0, in, vol = 0,
				xpos, ypos, xsize, ysize, pos|
				var dx = xpos - In.kr(pos);
				var dy = ypos - In.kr(pos+1);
				var dt, dn;
				var scope = In.kr(pos+2);
				var phi = In.kr(pos+3);
				var dist;
				// enrouler les distances au travers des limites de la grille
				dx = dx.wrap2(xsize/2);
				dy = dy.wrap2(ysize/2);
				// distance Euclidienne
				dist = (dx.squared + dy.squared).sqrt;
				// distance normale et tangentielle
				dt = (dx * cos(phi)) + (dy * sin(phi));
				// attention, dans la représentation graphique
				// la droite et la gauche sont inversées (!)
				dn = (dx * sin(phi).neg) + (dy * cos(phi));
				Out.ar(out, Pan2.ar(In.ar(in) * vol * (scope ** -1.25) *
					max(0, 1 - (dist/scope)),
					dn / max(max(dn, dt), 1.0))
				);
			}),
			if(altCycle.notNil,
			{
					SynthDef('genPos', {|pos, linspeed, angspeed, zspeed, xsize, ysize|
						var size = max(xsize, ysize);
						// calcul de l'angle phi
						var phi = LFSaw.kr(
							LFNoise1.kr(angspeed).range(angspeed.neg, angspeed)
						).range(0, 2pi);
						// calcul de la vitesse
						var speed = LFNoise1.kr(linspeed).range(0, linspeed/size);
						Out.kr(pos, [
							// calcul de la position x
							LFSaw.kr(speed * cos(phi)).range(0, xsize),
							// calcul de la position y
							LFSaw.kr(speed * sin(phi)).range(0, ysize),
							// générateur d'altitude
							// cycle programmé
							max(1, min((min(xsize,ysize)/2)+0.5,
								EnvGen.kr(Env.circle(altCycle[0], altCycle[1])))),
							// sortie de phi
							phi
						]);
					})
				}, {
					SynthDef('genPos', {|pos, linspeed, angspeed, zspeed, xsize, ysize|
						var maxSize = max(xsize, ysize);
						var minSize = min(xsize, ysize);
						// calcul de l'angle phi
						var phi = LFSaw.kr(
							LFNoise1.kr(angspeed).range(angspeed.neg, angspeed)
						).range(0, 2pi);
						// calcul de la vitesse
						var speed = LFNoise1.kr(linspeed).range(0, linspeed/maxSize);
						var valRange = zspeed/minSize;
						Out.kr(pos, [
							// calcul de la position x
							LFSaw.kr(speed * cos(phi)).range(0, xsize),
							// calcul de la position y
							LFSaw.kr(speed * sin(phi)).range(0, ysize),
							// générateur d'altitude
							// altitude aléatoire
							SinOsc.kr(
								LFNoise1.kr(speed).range(valRange.neg, valRange)
							).range(1,(minSize/2)+0.5),
							// sortie de phi
							phi
						]);
					})
				}
			)
		];

		defs.do({|item| item.add });
	}

	*new {|busses, volume, linSpeed, angSpeed, altParm|
		^super.new.init(busses, volume, linSpeed, angSpeed, altParm);
	}

	init {|busses, volume, linSpeed, angSpeed, altParm|
		var xSize = busses.size;
		var ySize = busses[0].size;

		posBus = Bus.control(numChannels: 4).setSynchronous(0,0,1,0);

		posGen = Synth('genPos', ['pos', posBus,
			'linspeed', linSpeed, 'angspeed', angSpeed,
			'zspeed', if(altParm.isNumber, {altParm}, {nil}),
			'xsize', xSize, 'ysize', ySize]);

		synths = busses.collect({|row, x|
			row.collect({|item, y|
				Synth('cellMapOut', ['out', 0, 'in', item, 'vol', volume,
					'xpos', x, 'ypos', y, 'xsize', xSize, 'ysize', ySize,
					'pos', posBus]);
			});
		});
	}

	free {
		synths.do({|row| row.do({|item| item.free })});
		posGen.free;
		posBus.free;
		^super.free;
	}
}