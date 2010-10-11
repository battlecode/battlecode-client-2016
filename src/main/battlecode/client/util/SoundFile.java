package battlecode.client.util;

import java.io.*;
import java.net.URL;
import javax.sound.sampled.*;

public class SoundFile extends DataFile {

	private AudioFormat format;
	private byte[] data;
	private DataLine.Info info;

	public SoundFile(String pathname) {
		super(pathname);
	}

	public synchronized Clip createClip() {
		if (data != null) {
			try {
				Clip clip = (Clip)(AudioSystem.getLine(info));
				//clip.addLineListener(this);
				
				clip.open(format, data, 0, data.length);
				return clip;
			}
			catch (LineUnavailableException e) {
				System.out.println("Caught LineUnavailableException in SoundFile.createClip()");
				//e.printStackTrace();
			}
		}
		return null;
	}
	
	private Clip getClip(AudioFormat format, byte[] data) {
		try {
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
			for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
				Clip clip = null;
				try {
					Mixer mixer = AudioSystem.getMixer(mi);
					clip = (Clip)mixer.getLine(info);
					clip.open(format, data, 0, data.length);
					clip.start();
					return clip;
				}
				catch (Exception e) {}
				if (clip != null)
					try {
						clip.close();
					}
					catch (Exception e) {}
			}
		}
		catch (Exception e) {}

		return null;
	}


	protected synchronized void load(File file) {
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(file);
			format = ais.getFormat();
			info = new DataLine.Info(Clip.class, format);
			data = new byte[(int) ais.getFrameLength() * format.getFrameSize()];
			int bytesRead = ais.read(data);
			assert data.length == bytesRead:
				"Unexpected EOF with " + (data.length - bytesRead) + " remaining";
		}
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			data = null;
		}
		catch (IOException e) {
			e.printStackTrace();
			data = null;
		}
	}

	protected void reload(File file) {
		load(file);
	}

    @Override
    protected void load(URL url) {
        		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(url);
			format = ais.getFormat();
			info = new DataLine.Info(Clip.class, format);
			data = new byte[(int) ais.getFrameLength() * format.getFrameSize()];
			int bytesRead = ais.read(data);
			assert data.length == bytesRead:
				"Unexpected EOF with " + (data.length - bytesRead) + " remaining";
		}
		catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			data = null;
		}
		catch (IOException e) {
			e.printStackTrace();
			data = null;
		}
    }

    @Override
    protected void reload(URL url) {
        load(url);
    }
}
