package voiceCommandInterface;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import javax.swing.*;
import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;

public class VoiceAssistantGUI extends JFrame {

    private JLabel statusLabel;
    private JLabel commandLabel;
    private JButton startButton;
    private JButton stopButton;

    private LiveSpeechRecognizer recognizer;
    private Thread recognitionThread;
    private volatile boolean listening = false;

    public VoiceAssistantGUI() {
        setTitle("🎙️ Voice Command Assistant");
        setSize(550, 670);
        setLayout(new BorderLayout(12, 12));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.white);

        // === HEADER ===
        JLabel title = new JLabel("Voice Command Interface", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 144, 255));
        title.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        add(title, BorderLayout.NORTH);

        // === CENTER PANEL ===
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 8, 8));
        centerPanel.setBackground(Color.white);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        statusLabel = new JLabel("Press Start to Begin Listening...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        commandLabel = new JLabel("Recognized Command: None", SwingConstants.CENTER);
        commandLabel.setFont(new Font("Consolas", Font.BOLD, 16));
        commandLabel.setForeground(new Color(60, 179, 113));

        // === COMMAND BOX ===
        JPanel commandBox = new JPanel();
        commandBox.setLayout(new BoxLayout(commandBox, BoxLayout.Y_AXIS));
        commandBox.setBackground(new Color(245, 248, 250));
        commandBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 2, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JLabel boxHeading = new JLabel("🗣️ Try These Commands:");
        boxHeading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        boxHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea commandList = new JTextArea(
                "• open chrome\n" +
                "• close chrome\n" +
                "• open gmail\n" +
                "• close gmail\n" +
                "• exit"
        );
        commandList.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        commandList.setEditable(false);
        commandList.setOpaque(false);
        commandList.setFocusable(false);
        commandList.setAlignmentX(Component.LEFT_ALIGNMENT);

        commandBox.add(boxHeading);
        commandBox.add(Box.createRigidArea(new Dimension(0, 5)));
        commandBox.add(commandList);

        centerPanel.add(statusLabel);
        centerPanel.add(commandLabel);
        centerPanel.add(commandBox);
        add(centerPanel, BorderLayout.CENTER);

        // === BUTTONS PANEL ===
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.white);

        startButton = new JButton("🎧 Start Listening");
        stopButton = new JButton("🛑 Stop");

        styleButton(startButton, new Color(50, 205, 50));
        styleButton(stopButton, new Color(220, 20, 60));
        stopButton.setEnabled(false);

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        add(buttonPanel, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startListening());
        stopButton.addActionListener(e -> stopListening());
    }

    private void styleButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(170, 40));
    }

    private void startListening() {
        if (listening) return;
        listening = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Listening... 🎤");
        commandLabel.setText("Recognized Command: None");

        recognitionThread = new Thread(() -> {
            try {
                Configuration config = new Configuration();

                config.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
                config.setDictionaryPath("src/voiceCommandInterface/dic.dic");
                config.setLanguageModelPath("src/voiceCommandInterface/lm.lm");

                recognizer = new LiveSpeechRecognizer(config);
                recognizer.startRecognition(true);

                SpeechResult result;
                while (listening && (result = recognizer.getResult()) != null) {
                    String command = result.getHypothesis();
                    SwingUtilities.invokeLater(() -> commandLabel.setText("Recognized Command: " + command));
                    System.out.println("Command: " + command);
                    handleCommand(command);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> statusLabel.setText("Error: " + ex.getMessage()));
            } finally {
                if (recognizer != null) recognizer.stopRecognition();
                listening = false;
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Stopped Listening 💤");
                    startButton.setEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        });

        recognitionThread.setDaemon(true);
        recognitionThread.start();
    }

    private void stopListening() {
        listening = false;
        stopButton.setEnabled(false);
        startButton.setEnabled(true);
        statusLabel.setText("Stopping...");
        if (recognizer != null) {
            try {
                recognizer.stopRecognition();
            } catch (Exception ignored) {}
        }
    }

    private void handleCommand(String voiceCommand) {
        if (voiceCommand == null || voiceCommand.trim().isEmpty()) return;
        String cmd = voiceCommand.trim().toLowerCase();

        try {
            if (isWordRunning()) {
                typeTextIntoWord(voiceCommand);
                SwingUtilities.invokeLater(() ->
                    statusLabel.setText("Typed into Word: " + voiceCommand));
                return;
            }

            if (cmd.equals("open chrome")) {
                Runtime.getRuntime().exec("cmd.exe /c start chrome");
            } else if (cmd.equals("close chrome")) {
                Runtime.getRuntime().exec("cmd.exe /c TASKKILL /IM chrome.exe /F");
            } else if (cmd.equals("open gmail")) {
                Runtime.getRuntime().exec("cmd.exe /c start chrome https://mail.google.com");
            } else if (cmd.equals("close gmail")) {
                Runtime.getRuntime().exec("cmd.exe /c TASKKILL /IM chrome.exe /F");
            } else if (cmd.equals("exit")) {
                listening = false;
                if (recognizer != null) recognizer.stopRecognition();
                SwingUtilities.invokeLater(() -> statusLabel.setText("Voice Assistant Closed 👋"));
            } else {
                SwingUtilities.invokeLater(() -> statusLabel.setText("Heard: " + voiceCommand));
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            SwingUtilities.invokeLater(() -> statusLabel.setText("Command failed: " + ioe.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isWordRunning() throws IOException {
        Process process = Runtime.getRuntime().exec("tasklist");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("WINWORD.EXE")) {
                return true;
            }
        }
        return false;
    }

    private void typeTextIntoWord(String text) throws Exception {
        Robot robot = new Robot();
        for (char c : text.toCharArray()) {
            boolean upperCase = Character.isUpperCase(c);
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode == KeyEvent.VK_UNDEFINED) continue;
            if (upperCase) robot.keyPress(KeyEvent.VK_SHIFT);
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
            if (upperCase) robot.keyRelease(KeyEvent.VK_SHIFT);
            Thread.sleep(15);
        }
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_SPACE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VoiceAssistantGUI().setVisible(true));
    }
}
