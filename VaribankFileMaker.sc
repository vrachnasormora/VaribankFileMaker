VaribankFileMaker {
	var pathName1, pathName2, pathName3, pathName4;
	var buffer;



	loadSoundFile {
		FileDialog (
			{ arg path;
				pathName1 = PathName(path[0]);

				if (pathName1.extension == "wav",
					{
						if (buffer.notNil,
							{buffer.clear}
						);
						buffer = Buffer.read(Server.default, pathName1.fullPath);

						postln("SoundFile" + pathName1.fileName + "was selected (to filter another with)");
					},
					{
						pathName1 = nil;
						postln( "Please select a soundfile next time" );

					}
				);
			},
			{
				postln("Made no selection");
				if (pathName1.notNil && buffer.notNil,
					{postln(", but" + pathName1.fileName + "is still loaded")}
				);
			}
		)
	}

	writeTextFile { arg offset=0.0, name="variTextFile";
		if (pathName1.notNil,
			{
				var fileName;
				var file;
				var fftSize = 2048;
				var fftSizeOver2 = (fftSize/2).asInt;
				var hop = 1/4;
				var rate = 2*fftSize/buffer.sampleRate;
				var step = offset;
				var content = "";
				var magnitudes;
				var duration = buffer.numFrames / buffer.sampleRate;
				var iterations = (duration / rate).ceil.asInt;
				var fftBuffer;
				var theFFT;
				var theWriter;

				if (name == "variTextFile",
					{fileName = pathName1.fileNameWithoutExtension},
					{fileName = name}
				);

				pathName2 = PathName.new(pathName1.pathOnly ++ fileName ++ ".txt");
				if (File.exists(pathName2.fullPath), {File.delete(pathName2.fullPath)});
				file = File(pathName2.fullPath, "w");

				if (offset != 0.0,
					{
						content = "0.0";

						(fftSizeOver2-1).do
						{
							arg i;
							content = content + (i+1*buffer.sampleRate/fftSize).asString;
							content = content + "-infdB";

						};
						file.write(content);
						content = "\n";
					}
				);

				fftBuffer = Buffer.alloc(Server.default,fftSize,1);

				theFFT = { var input, chain;
					input = PlayBuf.ar(buffer.numChannels, buffer, BufRateScale.kr(buffer));
					chain = FFT(fftBuffer, input, hop: hop, wintype: 0, winsize: fftSizeOver2);
					//chain = PV_MagSquared(chain);
				}.play;

				theWriter = Routine {

					"Please, bare with me...".postln;

					iterations.do
					{
						arg i;
						fftBuffer.loadToFloatArray
						(
							action:
							{
								arg array;
								var z, x, m;

								content =  content ++ step;
								z = array.clump(2).flop;

								z = [Signal.newFrom(z[0]), Signal.newFrom(z[1])];
								x = Complex(z[0], z[1]);

								//~magnitudes = x.magnitude * x.magnitude;

								magnitudes = x.magnitude[1..fftSizeOver2-1]/fftSizeOver2;

								magnitudes.do
								{
									arg item, index;
									var ampInDB = item.ampdb;
									var threshold = -60;

									if ( ampInDB < threshold,
										{ampInDB = -inf},
										{ampInDB = ampInDB + 20}
									);

									content = content + (index+1*buffer.sampleRate/fftSize).asString;
									content = content + ampInDB ++ "dB";
								};
								content = content + "\n";
								file.write(content);
								content ="\n";

								//{ ~magnitudes.plot('Initial', Rect(200, 600-(200*i), 700, 200)) }.defer
							}
						);
						step = step + rate;
					};

					//wait a little more
					0.1.wait;
					file.close;
					theFFT.free;
					fftBuffer.free;

					"See? I'm done!".postln;

				}.play;
			},
			{ postln("Please, select a soundfile to be analyzed")}
		);
	}

	createFilteredSoundFile { arg quality=5, volume=1, name="variFiltered";
		if (pathName2.notNil,
			{
				FileDialog (
					{ arg path;
						pathName3 = PathName(path[0]);

						if (pathName3.extension == "wav",
							{
								var fileName;
								var command = "cd" + pathName3.pathOnly.escapeChar($ ) ++ "; filter varibank 1" + pathName3.fileName;

								postln(pathName3.fileName + "was selected (to be filtered)");

								if (name == "variFiltered",
									{fileName = pathName3.fileNameWithoutExtension ++ "_filtered"},
									{fileName = name}
								);

								pathName4 = PathName.new(pathName3.pathOnly ++ fileName ++ ".wav");
								if (File.exists(pathName4.fullPath), {File.delete(pathName4.fullPath)});

								command = command + pathName4.fileName + pathName2.fullPath.escapeChar($ ) + quality + volume;
								command.runInTerminal;
							},
							{pathName3 = nil; postln( "Please select a (.wav) soundfile next time" )}
						);
					},
					{
						postln("Made no selection");
						if (pathName3.notNil,
							{postln("," + pathName3.fileName + "is your previously filtered sound")}
						);
					}
				)
			},
			{ postln("Please, first analyze a sound")}
		);
	}
}
