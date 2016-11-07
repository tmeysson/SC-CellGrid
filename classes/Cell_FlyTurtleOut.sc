Cell_FlyTurtleOut {
	classvar defs;

	var synths;
	var xBus, yBus, phiBus, zBus, hPosGen, zPosGen;
	var followLvl;

	*addDefs {|altCycle = nil|
		defs = [
			SynthDef('cellFlyTurtleOut', {|out = 0, in, vol = 0,
				xpos, ypos, xsize, ysize, xbus, ybus, phibus, zbus|
				var dx = xpos - In.kr(xbus);
				var dy = ypos - In.kr(ybus);
				var dn, dt;
				var scope = In.kr(zbus);
				var phi = In.kr(phibus);
				var dist;
				// enrouler les distances au travers des limites de la grille
				dx = dx.wrap2(xsize/2);
				dy = dy.wrap2(ysize/2);
				// distance Euclidienne
				dist = (dx.squared + dy.squared).sqrt;
				// distance normale et tangentielle
				dn = (dx * cos(phi)) + (dy * sin(phi));
				dt = (dx * sin(phi)) + (dy * cos(phi).neg);
				Out.ar(out, Pan2.ar(In.ar(in) * vol * (1/(scope**2)) *
					max(0, 1 - (dist/scope)),
					dt / max(max(dn, dt), 1.0))
				);
			}),
			SynthDef('genPos', {|xbus, ybus, phibus, linspeed, angspeed, size|
				// calcul de l'angle phi
				var phi = LFSaw.kr(
					LFNoise1.kr(angspeed).range(angspeed.neg, angspeed)
				).range(0, 2pi);
				// calcul de la vitesse
				var speed = LFNoise1.kr(linspeed).range(0, linspeed/size);
				// sortie de phi
				Out.kr(phibus, phi);
				// calcul de la position x
				Out.kr(xbus, LFSaw.kr(speed * cos(phi)));
				// calcul de la position y
				Out.kr(ybus, LFSaw.kr(speed * sin(phi)));
			}),
			// générateur d'altitude
			if(altCycle.notNil,
				{
					// cycle programmé
					SynthDef('genAlt', {|out, size|
						Out.kr(out, max(1, min((size/2)+0.5,
							EnvGen.kr(Env.circle(altCycle[0], altCycle[1])))));
					});
				}, {
					// altitude aléatoire
					SynthDef('genAlt', {|out, speed, size|
						var valRange = speed/size;
						Out.kr(out, SinOsc.kr(
							LFNoise1.kr(speed).range(valRange.neg, valRange)
						).range(1,(size/2)+0.5));
					});
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

		xBus = Bus.control;
		yBus = Bus.control;
		phiBus = Bus.control;
		zBus = Bus.control.setSynchronous(1);

		hPosGen = Synth('genPos', ['xbus', xBus, 'ybus', yBus, 'phibus', phiBus,
			'linspeed', linSpeed, 'angspeed', angSpeed, 'size', max(xSize, ySize)]);
		zPosGen = Synth('genAlt', ['out', zBus, 'speed',
			if(altParm.isNumber, {altParm}, {nil}), 'size', min(xSize, ySize)]);

		synths = busses.collect({|row, x|
			row.collect({|item, y|
				Synth('cellFlyTurtleOut', ['out', 0, 'in', item, 'vol', volume,
					'xpos', x, 'ypos', y, 'xsize', xSize, 'ysize', ySize,
					'xbus', xBus, 'ybus', yBus, 'phibus', phiBus, 'zbus', zBus]);
			});
		});

		followLvl = Routine({
			var curLvl, getLvl;
			zBus.get({|val| curLvl = val.round(0.5) });
			{
				2.wait;
				zBus.get({|val| getLvl = val.round(0.5) });
				if(getLvl != curLvl, {("LEVEL: "++getLvl).postln; curLvl = getLvl;});
			}.loop;
		}).play;
	}

	free {
		synths.do({|row| row.do({|item| item.free })});
		hPosGen.free; zPosGen.free;
		followLvl.stop;
		xBus.free; yBus.free; phiBus.free; zBus.free;
		^super.free;
	}
}