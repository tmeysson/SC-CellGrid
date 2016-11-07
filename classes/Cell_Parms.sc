Cell_Parms : Array {

	*gen {|weights = #[0.5, 0,5, 0.5, 0.5, 0.5],
		nseMax = 1.0, nseLowOct = -3.0, nseHighOct = 3.0,
		fwdMax = 1.0,
		delMaxExp = 8.0,
		fmOct = 1.0,
		ampMax = 1.0|
		^super.with(weights, [nseMax, nseLowOct, nseHighOct],
			fwdMax, delMaxExp, fmOct, ampMax);
	}

	*pipe {|expected = 2, delFixMax = 3.0, delAmtMax = 1.0, pitchAmt = 1.0,
		ampModFreqInt = #[0.0, 6.0], distFact = 2.0,
		revRoomInt = #[0.25, 0.75], revDampInt = #[0.25, 0.75]|
		^super.with(expected, delFixMax, delAmtMax, pitchAmt,
			ampModFreqInt, distFact, revRoomInt, revDampInt);
	}

	*mod {|freqMin = 0.0, freqMax = 6.0|
		^super.with(freqMin, freqMax);
	}

	*rec {|fadeIn = 5, fadeOut = 5, totalTime = 30, path|
		^super.with([fadeIn, fadeOut, totalTime], path);
	}

	*outSum{
		^super.with('sum');
	}

	*outWalk{|speed|
		^super.with('walk', speed);
	}

	*outFly{|speed, zSpeed|
		^super.with('fly', speed, zSpeed);
	}

	*outFlyCycle{|speed, levels, times|
		^super.with('fly', speed, [levels, times]);
	}

	*outFlyPan{|speed, zSpeed|
		^super.with('flypan', speed, zSpeed);
	}

	*outFlyPanCycle{|speed, levels, times|
		^super.with('flypan', speed, [levels, times]);
	}

	*outFlyTurtle{|linSpeed, angSpeed, zSpeed|
		^super.with('flyturtle', linSpeed, angSpeed, zSpeed);
	}

	*outFlyTurtleCycle{|linSpeed, angSpeed, levels, times|
		^super.with('flyturtle', linSpeed, angSpeed, [levels, times]);
	}

	*outTurtle{|speed = 0.25|
		^super.with('turtle', speed);
	}
}