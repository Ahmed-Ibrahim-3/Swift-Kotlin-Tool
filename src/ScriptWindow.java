import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ScriptWindow extends JFrame {
    private JTextPane editor;
    private JTextArea output;
    private JButton runButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JComboBox<String> languageSelector;

    private ScriptRunner curRunner;
    private SwiftRunner swiftRunner;
    private KotlinRunner kotlinRunner;
    String[] languages = {"Swift", "Kotlin"};


    public ScriptWindow() {
        super("Script Executor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        swiftRunner = new SwiftRunner();
        kotlinRunner = new KotlinRunner();
        curRunner = swiftRunner;

        initComponents();
        setupLayout();
        setupListeners();

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        editor.requestFocusInWindow();
    }

    private void initComponents() {
        editor = new JTextPane();
        editor.setFont(new Font("Monospaced", Font.PLAIN, 12));

        output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font("Monospaced", Font.PLAIN, 12));

        languageSelector = new JComboBox<>(languages);
        runButton = new JButton("Run");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        statusLabel = new JLabel("Ready");

    }

    private void setupLayout() {
        JScrollPane editorScrollPane = new JScrollPane(editor);
        JScrollPane outputScrollPane = new JScrollPane(output);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Language: "));
        controlPanel.add(languageSelector);
        controlPanel.add(runButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, outputScrollPane);
        splitPane.setResizeWeight(0.66);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void setupListeners() {

        languageSelector.addActionListener((ActionEvent e) -> {
            String selectedLanguage = (String) languageSelector.getSelectedItem();
            assert selectedLanguage != null;
            if (selectedLanguage.equals("Swift")) {
                curRunner = swiftRunner;
            } else if (selectedLanguage.equals("Kotlin")) {
                curRunner = kotlinRunner;
            }
        });

        runButton.addActionListener((ActionEvent e) -> {
            runScript();
        });

        stopButton.addActionListener((ActionEvent e) -> {
            stopScript();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                swiftRunner.shutdown();
                kotlinRunner.shutdown();
            }
        });
    }

    private void runScript() {
        output.setText("");

        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Running...");

        String scriptContent = editor.getText();

        new Thread(() -> {
            int exitCode = curRunner.runScript(
                    scriptContent,
                    line -> SwingUtilities.invokeLater(() -> appendOutput(line)),
                    error -> SwingUtilities.invokeLater(() -> appendOutput("ERROR: " + error))
            );

            SwingUtilities.invokeLater(() -> {
                runButton.setEnabled(true);
                stopButton.setEnabled(false);
                statusLabel.setText("Finished (exit code: " + exitCode + ")");
            });
        }).start();
    }

    private void stopScript() {
        if (curRunner.isRunning()) {
            curRunner.stopScript();
            statusLabel.setText("Stopped");
            appendOutput("Script stopped manually");
        }
    }

    private void appendOutput(String line) {
        output.append(line + "\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

}
