Cell_TurtleOut {
	// la définition du Synth
	classvar def;
	// les Bus de la grille
	var busses;
	// dimensions de la grille
	var xSize, ySize;
	// sortie audio
	var synth;
	// entrées audio
	//var ins;
	var phase;
	// la fonction qui met à jour les entrées
	var callback;
	// position actuelle
	var xPos, yPos, angle;

	*addDef {
		def = SynthDef('cellTurtleOut', {|out = 0, vol = 0, speed = 0.25,
			in0 = #[0,0,0,0], in1 = #[0,0,0,0]|
			var trig = Impulse.kr(speed);
			var gate = Demand.kr(trig, 0, Dseq([1,0], inf));
			var select = EnvGen.kr(Env.asr(1/speed,1,1/speed,\lin), gate);
			SendTrig.kr(trig);
			Out.ar(out, (select * [In.ar(in0[0])+In.ar(in0[3]), In.ar(in0[1])+In.ar(in0[2])]) +
				((1-select) * [In.ar(in1[0])+In.ar(in1[3]), In.ar(in1[1])+In.ar(in1[2])]) * (vol/4));
		});

		def.add;
	}

	*new {|gridBusses, volume, speed|
		^super.new.init(gridBusses, volume, speed);
	}

	init {|gridBusses, volume, speed|
		busses = gridBusses;
		xSize = busses.size;
		ySize = busses[0].size;

		xPos = 0;
		yPos = 0;
		angle = 0;
		phase = 0;

		synth = Synth('cellTurtleOut', ['out', 0, 'vol', volume, 'speed', speed]);

		this.nextPos;
		this.nextPos;

		callback = OSCFunc({|msg, time|
			// modifier les Bus de sortie suivants
			this.nextPos;
			}, '/tr', Server.default.addr,
			argTemplate: [synth.nodeID, nil, nil]
		);
	}

	nextPos {
		var change = [0.25,0.5,0.25].windex - 1;
		var ctrl = if(phase==0, {'in0'}, {'in1'});

		synth.set(ctrl,
			Array.fill(4, {|i|
				busses[(xPos+((i+angle+1)%4).div(2))%xSize]
				[(yPos+((i+angle+2)%4).div(2))%ySize];
			})
		);

		("["+xPos+","+yPos+"] "+["N","E","S","W"][angle]).postln;

		if(change == 0,
			{
				xPos = (xPos + sin(angle*0.5pi).round) % xSize;
				yPos = (yPos + cos(angle*0.5pi).round) % ySize;
			}, {
				angle = (angle + change) % 4;
			}
		);

		phase = (phase + 1) % 2;
	}

	free {
		synth.free;
		callback.free;
		^super.free;
	}

}
