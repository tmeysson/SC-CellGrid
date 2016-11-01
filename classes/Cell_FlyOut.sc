Cell_FlyOut {
	classvar defs;

	var synths;
	var xBus, yBus, zBus, xPosGen, yPosGen, zPosGen;
	var followLvl;

	*addDefs {|altCycle = nil|
		defs = [
			SynthDef('cellFlyOut', {|out = 0, in, vol = 0,
				xpos, ypos, xbus, ybus, zbus, xsize, ysize|
				var dx = abs(xpos - In.kr(xbus));
				var dy = abs(ypos - In.kr(ybus));
				var scope = In.kr(zbus);
				Out.ar(out, (In.ar(in) * vol * (1/(scope**2)) *
					(max(0, min(1, (scope - dx))) + max(0, min(1, (scope - (xsize - dx))))) *
					(max(0, min(1, (scope - dy))) + max(0, min(1, (scope - (ysize - dy)))))
				) ! 2);
			}),
			SynthDef('genPos', {|out, speed, size|
				var valRange = speed/size;
				Out.kr(out, LFSaw.kr(
					LFNoise1.kr(speed).range(valRange.neg, valRange)
				).range(0,size));
			}),
			if(altCycle.notNil,
				{
					SynthDef('genAlt', {|out, size|
						Out.kr(out, max(1, min((size/2)+0.5,
							EnvGen.kr(Env.circle(altCycle[0], altCycle[1])))));
					});
				}, {
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

	*new {|busses, volume, speed, altParm|
		^super.new.init(busses, volume, speed, altParm);
	}

	init {|busses, volume, speed, altParm|
		var xSize = busses.size;
		var ySize = busses[0].size;

		xBus = Bus.control;
		yBus = Bus.control;
		zBus = Bus.control;

		synths = busses.collect({|row, x|
			row.collect({|item, y|
				Synth('cellFlyOut', ['out', 0, 'in', item, 'vol', volume,
					'xpos', x, 'ypos', y, 'xbus', xBus, 'ybus', yBus, 'zbus', zBus,
					'xsize', xSize, 'ysize', ySize]);
			});
		});

		xPosGen = Synth('genPos', ['out', xBus, 'speed', speed, 'size', xSize]);
		yPosGen = Synth('genPos', ['out', yBus, 'speed', speed, 'size', ySize]);
		zPosGen = Synth('genAlt', ['out', zBus, 'speed',
			if(altParm.isNumber, {altParm}, {nil}), 'size', min(xSize, ySize)]);

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
		xPosGen.free; yPosGen.free; zPosGen.free;
		followLvl.stop;
		xBus.free; yBus.free; zBus.free;
		^super.free;
	}
}