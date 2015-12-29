package battlecode.client.util;

import battlecode.client.resources.ResourceLoader;

import javax.sound.sampled.*;
import java.io.IOException;

public class SoundFile {

    private AudioFormat format;
    private byte[] data;
    private DataLine.Info info;

    public SoundFile(String pathname) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(ResourceLoader.getUrl(pathname));
            format = ais.getFormat();
            info = new DataLine.Info(Clip.class, format);
            data = new byte[(int) ais.getFrameLength() * format.getFrameSize()];
            int bytesRead = ais.read(data);
            assert data.length == bytesRead :
                    "Unexpected EOF with " + (data.length - bytesRead) + " remaining";
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            data = null;
        }
    }

    public synchronized Clip createClip() {
        if (data != null) {
            try {
                Clip clip = (Clip) (AudioSystem.getLine(info));

                clip.open(format, data, 0, data.length);
                return clip;
            } catch (LineUnavailableException e) {
                System.out.println("Caught LineUnavailableException in " +
                        "SoundFile.createClip()");
            }
        }
        return null;
    }

    private Clip getClip(AudioFormat format, byte[] data) {
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                    format);
            for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
                Clip clip = null;
                try {
                    Mixer mixer = AudioSystem.getMixer(mi);
                    clip = (Clip) mixer.getLine(info);
                    clip.open(format, data, 0, data.length);
                    clip.start();
                    return clip;
                } catch (Exception e) {
                }
                if (clip != null)
                    try {
                        clip.close();
                    } catch (Exception e) {
                    }
            }
        } catch (Exception e) {
        }

        return null;
    }
}
