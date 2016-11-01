Cell_WalkOut {
	classvar defs;

	var synths;
	var xBus, yBus, xPosGen, yPosGen;

	*initClass {
		defs = [
			SynthDef('cellWalkOut', {|out = 0, in, vol = 0,
				xpos, ypos, xbus, ybus, xsize, ysize|
				var dx = abs(xpos - In.kr(xbus));
				var dy = abs(ypos - In.kr(ybus));
				Out.ar(out, (In.ar(in) * vol *
					(max(0, (1 - dx)) + max(0, (1 - (xsize - dx)))) *
					(max(0, (1 - dy)) + max(0, (1 - (ysize - dy))))
				) ! 2);
			}),
			SynthDef('genPos', {|out, speed, size|
				var valRange = speed/size;
				Out.kr(out, LFSaw.kr(
					LFNoise1.kr(speed).range(valRange.neg, valRange)
				).range(0,size));
			})
		];
	}

	*addDefs {
		defs.do({|item| item.add });
	}

	*new {|busses, volume, speed|
		^super.new.init(busses, volume, speed);
	}

	init {|busses, volume, speed|
		var xSize = busses.size;
		var ySize = busses[0].size;

		xBus = Bus.control;
		yBus = Bus.control;

		synths = busses.collect({|row, x|
			row.collect({|item, y|
				Synth('cellWalkOut', ['out', 0, 'in', item, 'vol', volume,
					'xpos', x, 'ypos', y, 'xbus', xBus, 'ybus', yBus,
					'xsize', xSize, 'ysize', ySize]);
			});
		});

		xPosGen = Synth('genPos', ['out', xBus, 'speed', speed, 'size', xSize]);
		yPosGen = Synth('genPos', ['out', yBus, 'speed', speed, 'size', ySize]);
	}

	free {
		synths.do({|row| row.do({|item| item.free })});
		xPosGen.free; yPosGen.free;
		xBus.free; yBus.free;
		^super.free;
	}
}