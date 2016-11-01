Cell_EndOut {
	classvar defs;
	classvar totalTime;

	var synths;
	var xBus, yBus, zBus, xPosGen, yPosGen, zPosGen;
	var followLvl, waitStop;

	*addDefs {|program|
		defs = [
			SynthDef('cellEndOut', {|out = 0, in, vol = 0,
				xpos, ypos, xbus, ybus, zbus, xsize, ysize|
				var dx = abs(xpos - In.kr(xbus));
				var dy = abs(ypos - In.kr(ybus));
				var scope = In.kr(zbus);
				totalTime = program[1].sum;
				Out.ar(out, Pan2.ar(In.ar(in) * vol * (1/(scope**2)) *
					EnvGen.kr(Env([1,1,0],[totalTime, 5])) *
					(max(0, min(1, (scope - dx))) + max(0, min(1, (scope - (xsize - dx))))) *
					(max(0, min(1, (scope - dy))) + max(0, min(1, (scope - (ysize - dy))))),
					DemandEnvGen.kr(Dwhite(-1,1),Dwhite(1,16)))
				);
			}),
			SynthDef('genPos', {|out, speed, size|
				var valRange = speed/size;
				Out.kr(out, LFSaw.kr(
					LFNoise1.kr(speed).range(valRange.neg, valRange)
				).range(0,size));
			}),
			SynthDef('genAlt', {|out, size|
				Out.kr(out, max(1, min((size/2)+0.5,
					EnvGen.kr(Env(program[0], program[1])))));
			})
		];

		defs.do({|item| item.add });
	}

	*new {|busses, volume, caller, speed, program|
		^super.new.init(busses, volume, caller, speed, program);
	}

	init {|busses, volume, caller, speed, program|
		var xSize = busses.size;
		var ySize = busses[0].size;

		xBus = Bus.control;
		yBus = Bus.control;
		zBus = Bus.control;

		synths = busses.collect({|row, x|
			row.collect({|item, y|
				Synth('cellEndOut', ['out', 0, 'in', item, 'vol', volume,
					'xpos', x, 'ypos', y, 'xbus', xBus, 'ybus', yBus, 'zbus', zBus,
					'xsize', xSize, 'ysize', ySize]);
			});
		});

		xPosGen = Synth('genPos', ['out', xBus, 'speed', speed, 'size', xSize]);
		yPosGen = Synth('genPos', ['out', yBus, 'speed', speed, 'size', ySize]);
		zPosGen = Synth('genAlt', ['out', zBus, 'size', min(xSize, ySize)]);

		followLvl = Routine({
			var curLvl, getLvl;
			zBus.get({|val| curLvl = val.round(0.5) });
			{
				2.wait;
				zBus.get({|val| getLvl = val.round(0.5) });
				if(getLvl != curLvl, {("LEVEL: "++getLvl).postln; curLvl = getLvl;});
			}.loop;
		}).play;

		waitStop = Routine({
			(totalTime + 7).wait;
			caller.free;
		}).play;
	}

	free {
		synths.do({|row| row.do({|item| item.free })});
		xPosGen.free; yPosGen.free; zPosGen.free;
		followLvl.stop; waitStop.stop;
		xBus.free; yBus.free; zBus.free;
		^super.free;
	}
}