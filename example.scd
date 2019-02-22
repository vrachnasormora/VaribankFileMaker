//create a VB file maker
a = VaribankFileMaker.new;

//choose a sound to be analysed
a.loadSoundFile;

//execute the analysis and write the CDP compatible text file
a.writeTextFile;

//(optional step-needs a local CDP installation)
//choose a sound to be filtered by the analysed sound,
//then invoke CDP for the filtering procedure and file rendering.
//1st arg: Filter Q, 2nd arg: Overall Gain
a.createFilteredSoundFile(500, 100);