package voiceCommandInterface;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.sound.sampled.*;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

public class VoiceAssistant {

    private static LiveSpeechRecognizer recognizer;
    private static boolean dictationMode = false;

    public static void main(String[] args) {
        try {
            startRecognizer(false);
            System.out.println("🎤 Voice Assistant is now listening...");

            SpeechResult result;
            while ((result = recognizer.getResult()) != null) {
                String command = result.getHypothesis().trim().toLowerCase();
                System.out.println("🗣️ Recognized: " + command);

                if (!dictationMode) {
                    // --- Command Mode ---
                    if (command.equals("open chrome")) {
                        Runtime.getRuntime().exec("cmd.exe /c start chrome");
                    } 
                    else if (command.equals("close chrome")) {
                        Runtime.getRuntime().exec("cmd.exe /c TASKKILL /IM chrome.exe /F");
                    } 
                    else if (command.equals("open gmail")) {
                        Runtime.getRuntime().exec("cmd.exe /c start chrome https://mail.google.com");
                    } 
                    else if (command.equals("close gmail")) {
                        Runtime.getRuntime().exec("cmd.exe /c TASKKILL /IM chrome.exe /F");
                    } 
                    else if (command.equals("edit file")) {
                        System.out.println("📝 Opening Microsoft Word...");
                        Runtime.getRuntime().exec("cmd.exe /c start winword");
                        Thread.sleep(5000);
                        if (isWordRunning()) {
                            System.out.println("✅ Microsoft Word detected — Dictation Mode ON");
                            dictationMode = true;
                            restartRecognizer(true);
                        } else {
                            System.out.println("❌ Word not detected. Try again.");
                        }
                    } 
                    else if (command.equals("exit")) {
                        System.out.println("👋 Exiting voice assistant...");
                        recognizer.stopRecognition();
                        System.exit(0);
                    } 
                    else {
                        System.out.println("⚠️ Unknown command: " + command);
                    }
                } else {
                    // --- Dictation Mode ---
                    if (command.equals("stop dictation")) {
                        System.out.println("🛑 Dictation mode OFF.");
                        dictationMode = false;
                        restartRecognizer(false);
                    } else {
                        if (isWordRunning()) {
                            typeTextIntoWord(command);
                        } else {
                            System.out.println("⚠️ Word closed. Switching back to command mode...");
                            dictationMode = false;
                            restartRecognizer(false);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** ✅ Initialize the recognizer */
    private static void startRecognizer(boolean isDictation) throws IOException {
        // Allow Java sound engine explicitly
        System.setProperty("com.sun.media.sound.disableAudioEngine", "false");

        Configuration config = new Configuration();

        if (isDictation) {
            // Large model for free speech
            config.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
            config.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
            config.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
        } else {
            // Your small grammar model for commands
            config.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
            config.setDictionaryPath("src/voiceCommandInterface/dic.dic");
            config.setLanguageModelPath("src/voiceCommandInterface/lm.lm");
        }

        recognizer = new LiveSpeechRecognizer(config);

        try {
            recognizer.startRecognition(true);
        } catch (IllegalStateException e) {
            System.err.println("⚠️ Default mic format (16kHz mono) not supported. Trying fallback...");
            try {
                AudioFormat fallback = new AudioFormat(44100.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, fallback);
                TargetDataLine mic = (TargetDataLine) AudioSystem.getLine(info);
                mic.open(fallback);
                mic.start();
                recognizer.stopRecognition();
                recognizer.startRecognition(true);
                System.out.println("✅ Using fallback microphone format (44.1kHz mono).");
            } catch (Exception ex) {
                System.err.println("❌ Could not open fallback microphone: " + ex.getMessage());
                throw new IOException("No supported microphone format found!");
            }
        }
    }

    /** 🔁 Restart recognizer with chosen mode */
    private static void restartRecognizer(boolean dictationMode) throws Exception {
        recognizer.stopRecognition();
        startRecognizer(dictationMode);
    }

    /** 🪟 Check if Word is running */
    private static boolean isWordRunning() throws IOException {
        Process process = Runtime.getRuntime().exec("tasklist");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("WINWORD.EXE")) return true;
        }
        return false;
    }

    /** ⌨️ Type dictated text into Word */
    private static void typeTextIntoWord(String text) throws Exception {
        Robot robot = new Robot();
        for (char c : text.toCharArray()) {
            boolean upper = Character.isUpperCase(c);
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode == KeyEvent.VK_UNDEFINED) continue;
            if (upper) robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
            if (upper) robot.keyRelease(KeyEvent.VK_SHIFT);
            Thread.sleep(20);
        }
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_SPACE);
    }
}
