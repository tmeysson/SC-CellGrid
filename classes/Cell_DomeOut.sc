Cell_DomeOut {
	classvar def;

	var synths;

	*initClass {
		def = SynthDef('cellDomeOut', {|out = 0, in, vol = 0|
			Out.ar(out, In.ar(in) * vol);
		});
	}

	*addDef {
		def.add;
	}

	*new {|busses, volume, mult|
		^super.new.init(busses, volume, mult);
	}

	init {|busses, volume, mult|
		var vol = volume/busses.sum(_.size);
		var offsets = [0] ++ busses.collect(_.size).integrate[..1];

		synths = busses.collect {|lvl, l|
			var numOuts = lvl.size / mult;
			lvl.collect {|item, i|
				var pos = i/mult;
				var lowInd = i.floor;
				var highInd = i.ceil % numOuts;
				if (lowInd == highInd) {
					[Synth('cellDomeOut', ['out', offsets[l] + lowInd, 'in', item, 'vol', vol])]
				} {
					var mix = highInd - pos;
					[Synth('cellDomeOut', ['out', offsets[l] + lowInd, 'in', item, 'vol', vol * mix]),
						Synth('cellDomeOut', ['out', offsets[l] + highInd, 'in', item,
							'vol', vol * (1 - mix)])]
				};
			};
		}.flat;
	}

	free {
		synths.do({|item| item.free });
		^super.free;
	}
}
