package voiceCommandInterface;

import javax.sound.sampled.*;

public class MicrophoneFix {

    public static TargetDataLine getCompatibleMic() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(info)) {
                TargetDataLine line = (TargetDataLine) mixer.getLine(info);
                line.open(format);
                return line;
            }
        }
        throw new LineUnavailableException("No compatible microphone found for 16 kHz mono 16-bit format.");
    }
}
