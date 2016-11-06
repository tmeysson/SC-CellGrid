Cell_Record {
	// FROM Server.sc
	var <recordNode, recordBuf,
	recHeaderFormat, recSampleFormat, recChannels;

	*new {|fadeIn, fadeOut, time, path|
		^super.new.init(fadeIn, fadeOut, time, path);
	}

	init {|fadeIn, fadeOut, time, path|
		recHeaderFormat = "WAV";
		recSampleFormat = "int16";
		recChannels = 2;
		this.prepareForRecord;
		Server.default.sync;
		this.record(fadeIn, fadeOut, time, path);
		4.wait;
	}

	// FROM Server.sc
	// recording output
	record { |fadeIn=1, fadeOut=1, time=10, path=nil|
		if(recordBuf.isNil){
			this.prepareForRecord(path);
			Routine({
				Server.default.sync;
				this.record;
			}).play;
		}{
			if(recordNode.isNil){
				recordNode = Synth.tail(RootNode(Server.default), "server-record",
						[\bufnum, recordBuf.bufnum,
						\fadeIn, fadeIn, \fadeOut, fadeOut, \time, time]
				).onFree({this.stopRecording});
				NodeWatcher.register(recordNode);
				CmdPeriod.doOnce {
					recordNode = nil;
					if (recordBuf.notNil) { recordBuf.close {|buf| buf.freeMsg }; recordBuf = nil; };
				}
			} {
				recordNode.run(true)
			};
			"Recording: %\n".postf(recordBuf.path);
		};
	}

	pauseRecording {
		recordNode.notNil.if({ recordNode.run(false); "Paused".postln }, { "Not Recording".warn });
	}

	stopRecording {
		if(recordNode.notNil) {
			if(recordNode.isPlaying, {recordNode.free});
			recordNode = nil;
			recordBuf.close({ |buf| buf.freeMsg });
			"Recording Stopped: %\n".postf(recordBuf.path);
			recordBuf = nil;
		} {
			"Not Recording".warn
		};
	}

	prepareForRecord { arg path;
		if (path.isNil) {
			if(File.exists(thisProcess.platform.recordingsDir).not) {
				thisProcess.platform.recordingsDir.mkdir
			};

			// temporary kludge to fix Date's brokenness on windows
			if(thisProcess.platform.name == \windows) {
				path = thisProcess.platform.recordingsDir +/+ "SC_" ++
				Main.elapsedTime.round(0.01) ++ "." ++ recHeaderFormat;
			} {
				path = thisProcess.platform.recordingsDir +/+ "SC_" ++
				Date.localtime.stamp ++ "." ++ recHeaderFormat;
			};
		};
		recordBuf = Buffer.alloc(Server.default, 65536, recChannels,
			{arg buf; buf.writeMsg(path, recHeaderFormat, recSampleFormat, 0, 0, true);},
			// prevent buffer conflicts by using reserved bufnum
			Server.default.options.numBuffers + 1);
		recordBuf.path = path;
		SynthDef("server-record", { arg bufnum, fadeIn, fadeOut, time;
			DiskOut.ar(bufnum, In.ar(0, recChannels) *
				EnvGen.kr(Env([0,0,1,1,0,0],[4,fadeIn, time - (fadeIn + fadeOut), fadeOut,4]),
					doneAction: 2))
		}).send(Server.default);
	}

	free {
		this.stopRecording;
		^super.free;
	}
}

