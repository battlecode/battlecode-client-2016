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

}
