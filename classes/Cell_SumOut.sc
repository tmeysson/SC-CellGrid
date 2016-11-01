Cell_SumOut {
	classvar def;

	var synths;

	*initClass {
		def = SynthDef('cellSumOut', {|out = 0, in, vol = 0|
			Out.ar(out, (In.ar(in) * vol) ! 2);
		});
	}

	*addDef {
		def.add;
	}

	*new {|busses, volume|
		^super.new.init(busses, volume);
	}

	init {|busses, volume|
		var vol = volume/busses.size;

		synths = busses.collect({|item|
			Synth('cellSumOut', ['out', 0, 'in', item, 'vol', vol]);
		});
	}

	free {
		synths.do({|item| item.free });
		^super.free;
	}
}