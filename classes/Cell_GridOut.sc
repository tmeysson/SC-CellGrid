Cell_GridOut {
	classvar def;

	var synths;
	var outGroup;

	*initClass {
		def = SynthDef('cellGridOut', {|out = 0, in, vol = 0|
			Out.ar(out, In.ar(in) * vol);
		});
	}

	*addDefs {
		def.add;
	}

	*new {|gateBus, busses, volume, grpSize|
		^super.new.init(gateBus, busses, volume, grpSize);
	}

	init {|gateBus, busses, volume, grpSize|
		var vol = volume/(grpSize**2);
		var xsize = (busses.size/grpSize).ceil.asInteger;
		// var ysize = (busses[0].size/grpSize).ceil.asInteger;

		outGroup = ParGroup();

		synths = busses.collect {|row, i|
			var x = (i/grpSize).floor.asInteger;
			row.collect {|item, j|
				var y = (j/grpSize).floor.asInteger;
				Synth('cellGridOut', ['out', gateBus.index + (x + (xsize * y)),
					'in', item, 'vol', vol], outGroup);
			};
		};
	}

	free {
		synths.flat.do(_.free);
		outGroup.free;
		^super.free;
	}
}