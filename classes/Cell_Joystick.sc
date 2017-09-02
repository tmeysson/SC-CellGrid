Cell_Joystick {
	classvar defs;

	var device;
	var slots, synths, outBusses;
	var <busses;

	*addDefs {
		defs = [
			SynthDef('joystick_axispos', {|out, in|
				Out.kr(out, MulAdd(In.kr(in), 2, -1))
			}),
			SynthDef('joystick_axisneg', {|out, in|
				Out.kr(out, MulAdd(In.kr(in), -2, 1))
			}),
			SynthDef('joystick_diff', {|out, pos, neg|
				Out.kr(out, In.kr(pos) - In.kr(neg))
			}),
			SynthDef('joystick_summer', {|out, pos, neg|
				var pulse = Impulse.kr(100);
				var summer = Summer.kr(pulse * In.kr(pos), 0.01) - Summer.kr(pulse * In.kr(neg), 0.01);
				Out.kr(out, summer.clip2);
			})
		];

		defs.do(_.add);
	}

	*new {|joyspec|
		^super.new.joystickInit(joyspec);
	}

	joystickInit {|joyspec|
		{
			// liste des périphériques
			var list = LID.buildDeviceList.select {|e| e[1] != "could not open device"};
			// TODO: trouver l'équivalent OSX
			list.postln;
			// ouvrir le premier
			device = GeneralHID.open(list.first);
		}.defer;

		// attendre que le périphéique soit prêt
		while {device.isNil} {"WAIT".postln; 0.1.wait};

		// sauvegarder la specification
		// specification = joyspec;
		slots = List();
		outBusses = List();
		synths = List();
		// créer les Bus
		busses = joyspec.collect {|spec|
			var type, index;
			# type, index = spec;
			switch (type)
			{'axisp'} {
				var slot = device.slots[index[0]][index[1]];
				var out, synth;
				slots.add(slot);
				slot.createBus;
				out = Bus.control;
				outBusses.add(out);
				synth = Synth('joystick_axispos', [out: out, in: slot.bus]);
				synths.add(synth);
				out;
			}
			{'axisn'} {
				var slot = device.slots[index[0]][index[1]];
				var out, synth;
				slots.add(slot);
				slot.createBus;
				out = Bus.control;
				outBusses.add(out);
				synth = Synth('joystick_axisneg', [out: out, in: slot.bus]);
				synths.add(synth);
				out;
			}
			{'diff'} {
				var ind1, ind2;
				var slot1, slot2;
				var out, synth;
				# ind1, ind2 = index;
				slot1 = device.slots[ind1[0]][ind1[1]];
				slot2 = device.slots[ind2[0]][ind2[1]];
				slots.add(slot1); slots.add(slot2);
				slot1.createBus; slot2.createBus;
				out = Bus.control;
				outBusses.add(out);
				synth = Synth('joystick_diff', [out: out, pos: slot1.bus, neg: slot2.bus]);
				synths.add(synth);
				out;
			}
			{'summer'} {
				var ind1, ind2;
				var slot1, slot2;
				var out, synth;
				# ind1, ind2 = index;
				slot1 = device.slots[ind1[0]][ind1[1]];
				slot2 = device.slots[ind2[0]][ind2[1]];
				slots.add(slot1); slots.add(slot2);
				slot1.createBus; slot2.createBus;
				out = Bus.control;
				outBusses.add(out);
				synth = Synth('joystick_summer', [out: out, pos: slot1.bus, neg: slot2.bus]);
				synths.add(synth);
				out;
			};
		}
		/* {Bus.control} ! 5 */;
	}

	free {
		synths.do(_.free);
		slots.do(_.freeBus);
		outBusses.do(_.free);
		device.close;
	}
}