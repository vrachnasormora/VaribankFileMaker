/*
VaribankFileMaker:

Analyse a sound file and output a text file compatible with program VariBank of the 'CDP' suite

Usage:

1. loadSoundFile : Select file to analyze throug a dialog
2. writeParameterFile : Analyse the file and write parameters for analysis in text file.
3. createFilteredSoundFile : Create sound file by invoking Varibank through command line (needs a local CDP installation)
*/

VaribankFileMaker {
	var sampleFilterModelFilePath, filterBankParamFilePath, sampleInputFilePath, sampleOutputFilePath;
	var buffer, fftBuffer, file, fftSynth;

	loadSoundFile {
		FileDialog (
			{ arg path;
				sampleFilterModelFilePath = PathName(path[0]);

				if (sampleFilterModelFilePath.extension == "wav",
					{
						this.loadSampleBuffer;
					},
					{
						sampleFilterModelFilePath = nil;
						postln( "Please select a soundfile next time" );
					}
				);
			},
			{
				postln("Made no selection");
				if (sampleFilterModelFilePath.notNil && buffer.notNil,
					{postln(", but" + sampleFilterModelFilePath.fileName + "is still loaded")}
				);
			}
		)
	}



	loadSampleBuffer {

		buffer !? { buffer.clear };
		buffer = Buffer.read(Server.default, sampleFilterModelFilePath.fullPath);
		postln("SoundFile" + sampleFilterModelFilePath.fileName + "was selected (to filter another with)");
	}



	makefilterBankParamFile { arg fileName;

		filterBankParamFilePath = PathName.new(sampleFilterModelFilePath.pathOnly ++ fileName ++ ".txt");
		if (File.exists(filterBankParamFilePath.fullPath), {File.delete(filterBankParamFilePath.fullPath)});
		file = File(filterBankParamFilePath.fullPath, "w");
	}



	runFFTSynth {

		fftBuffer = Buffer.alloc(Server.default, 2048, 1);

		fftSynth = {
			var input, chain;
			input = PlayBuf.ar(buffer.numChannels, buffer, BufRateScale.kr(buffer));
			chain = FFT(fftBuffer, input, hop: 1/4, wintype: 0, winsize: (fftBuffer.numFrames/2).asInt);
			//chain = PV_MagSquared(chain);
		}.play;
	}



	get1AnalysisFrameContent {arg maxFreqNum, scalingCoef;

		var content = "";

		fftBuffer.loadToFloatArray
		(
			action:
			{arg currentFrame;
				var z, x;
				var magnitudes;

				z = currentFrame.clump(2).flop;
				z = [Signal.newFrom(z[0]), Signal.newFrom(z[1])];
				x = Complex(z[0], z[1]);
				magnitudes = x.magnitude[1..maxFreqNum]/scalingCoef;

				magnitudes.do
				{arg item, index;
					var ampInDB = item.ampdb;
					var threshold = -60;

					if ( ampInDB < threshold,
						{ampInDB = -inf},
						{ampInDB = ampInDB + 20}
					);

					content = content + (index+1*buffer.sampleRate/fftBuffer.numFrames).asString;
					content = content + ampInDB ++ "dB";
				};

				^content;
			}
		);
	}



	runWriteRoutine{arg initialTimeStamp;
		{
			var content = "";
			var timeStamp = initialTimeStamp;
			var fftSize = fftBuffer.numFrames;
			var fftSizeOver2 = (fftSize/2).asInt;
			var numFreqs = fftSizeOver2-1;
			var rate = 2*fftSize/buffer.sampleRate;
			var numFrames = ((buffer.numFrames / buffer.sampleRate) / rate).ceil.asInt;

			"Please, bear with me...".postln;

			if (timeStamp != 0.0,
				{
					content = "0.0";

					numFreqs.do
					{
						arg i;
						content = content + (i+1*buffer.sampleRate/fftSize).asString;
						content = content + "-infdB";

					};
					file.write(content);
					content = "\n";
				}
			);

			numFrames.do
			{
				content =  content ++ timeStamp;
				content = content + this.get1AnalysisFrameContent(numFreqs, fftSizeOver2);
				content = content + "\n";
				file.write(content);
				content ="\n";
				timeStamp = timeStamp + rate;
			};

			//wait a little more
			0.1.wait;
			file.close;
			fftSynth.free;
			fftBuffer.free;

			"See? I'm done!".postln;
		}.fork;
	}



	writeParameterFile { arg offset=0.0, name="variTextFile";
		if (sampleFilterModelFilePath.notNil,
			{
				var fileName;

				if (name == "variTextFile",
					{fileName = sampleFilterModelFilePath.fileNameWithoutExtension},
					{fileName = name}
				);

				this.makefilterBankParamFile(fileName);

				this.runFFTSynth;

				this.runWriteRoutine(offset);
			},
			{ postln("Please, select a soundfile to be analyzed")}
		);
	}



	createFilteredSoundFile { arg quality=5, volume=1, name="variFiltered";
		if (filterBankParamFilePath.notNil,
			{
				FileDialog (
					{ arg path;
						sampleInputFilePath = PathName(path[0]);

						if (sampleInputFilePath.extension == "wav",
							{
								var fileName;
								var command = "cd" + sampleInputFilePath.pathOnly.escapeChar($ ) ++ "; filter varibank 1" + sampleInputFilePath.fileName;

								postln(sampleInputFilePath.fileName + "was selected (to be filtered)");

								if (name == "variFiltered",
									{fileName = sampleInputFilePath.fileNameWithoutExtension ++ "_filtered"},
									{fileName = name}
								);

								sampleOutputFilePath = PathName.new(sampleInputFilePath.pathOnly ++ fileName ++ ".wav");
								if (File.exists(sampleOutputFilePath.fullPath), {File.delete(sampleOutputFilePath.fullPath)});

								command = command + sampleOutputFilePath.fileName + filterBankParamFilePath.fullPath.escapeChar($ ) + quality + volume;
								command.runInTerminal;
							},
							{sampleInputFilePath = nil; postln( "Please select a (.wav) soundfile next time" )}
						);
					},
					{
						postln("Made no selection");
						if (sampleInputFilePath.notNil,
							{postln("," + sampleInputFilePath.fileName + "is your previously filtered sound")}
						);
					}
				)
			},
			{ postln("Please, first analyze a sound")}
		);
	}

}