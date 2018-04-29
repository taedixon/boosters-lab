package ca.noxid.lab.rsrc;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class BlSound {
	
	private static Clip clip;
	private static Sequencer sequencer;

	public static void playSample(URL url) {
		try {
			clip = AudioSystem.getClip();
	        // getAudioInputStream() also accepts a File or InputStream
	        AudioInputStream ais = AudioSystem.
	            getAudioInputStream( url );
	        clip.open(ais);
	        clip.start();
		} catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void playMidi(URL url) {
		try {
			Sequence sequence = MidiSystem.getSequence(url);
			sequencer = MidiSystem.getSequencer();
	        sequencer.open();
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
	        sequencer.setSequence(sequence);
	        sequencer.start();
		} catch (MidiUnavailableException | InvalidMidiDataException | IOException e) {
			e.printStackTrace();
		}

	}
}