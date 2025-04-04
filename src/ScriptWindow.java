import Highlighters.KotlinHighlighter;
import Highlighters.ScriptHighlighter;
import Highlighters.SwiftHighlighter;
import Runners.KotlinRunner;
import Runners.ScriptRunner;
import Runners.SwiftRunner;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;

public class ScriptWindow extends JFrame {
    private JTextPane editor;
    private Output output;
    private JButton runButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JComboBox<String> languageSelector;

    private ScriptRunner currentRunner;
    private SwiftRunner swiftRunner;
    private KotlinRunner kotlinRunner;

    private SwiftHighlighter swiftHighlighter;
    private KotlinHighlighter kotlinHighlighter;
    private ScriptHighlighter currentHighlighter;

    public ScriptWindow() {
        super("Script Executor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        swiftRunner = new SwiftRunner();
        kotlinRunner = new KotlinRunner();
        currentRunner = swiftRunner;

        swiftHighlighter = new SwiftHighlighter();
        kotlinHighlighter = new KotlinHighlighter();
        currentHighlighter = swiftHighlighter;

        initComponents();
        setupLayout();
        setupListeners();

        setupMemorySettings();

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        applyCurrentSyntaxHighlighting();

        editor.requestFocusInWindow();
    }

    private void setupMemorySettings() {
        System.setProperty("sun.java2d.nodraw", "true");
        System.setProperty("swing.aatext", "true");
    }

    private void initComponents() {
        editor = new JTextPane();
        editor.setFont(new Font("Monospaced", Font.PLAIN, 12));

        output = new Output(this::navigateToLocation);

        languageSelector = new JComboBox<>(new String[]{"Swift", "Kotlin"});
        runButton = new JButton("Run");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        statusLabel = new JLabel("Ready");
    }

    private void setupLayout() {
        JScrollPane editorScrollPane = new JScrollPane(editor);
        JScrollPane outputScrollPane = new JScrollPane(output);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(new JLabel("Language:"));
        controlPanel.add(languageSelector);
        controlPanel.add(runButton);
        controlPanel.add(stopButton);
        controlPanel.add(statusLabel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScrollPane, outputScrollPane);
        splitPane.setResizeWeight(0.6);

        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }

    private void setupListeners() {
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyCurrentSyntaxHighlighting();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyCurrentSyntaxHighlighting();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        languageSelector.addActionListener((ActionEvent e) -> {
            String selectedLanguage = (String) languageSelector.getSelectedItem();
            if ("Swift".equals(selectedLanguage)) {
                currentRunner = swiftRunner;
                currentHighlighter = swiftHighlighter;
            } else if ("Kotlin".equals(selectedLanguage)) {
                currentRunner = kotlinRunner;
                currentHighlighter = kotlinHighlighter;
            }
            applyCurrentSyntaxHighlighting();
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

    private void applyCurrentSyntaxHighlighting() {
        SwingUtilities.invokeLater(() -> {
            int caretPosition = editor.getCaretPosition();

            if (currentHighlighter != null) {
                currentHighlighter.highlight(editor.getStyledDocument());
            }

            try {
                editor.setCaretPosition(caretPosition);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        });
    }

    private void runScript() {
        output.clear();

        runButton.setEnabled(false);
        stopButton.setEnabled(true);
        statusLabel.setText("Running...");

        String scriptContent = editor.getText();

        new Thread(() -> {
            int exitCode = currentRunner.runScript(
                    scriptContent,
                    line -> SwingUtilities.invokeLater(() -> output.appendLine(line)),
                    error -> SwingUtilities.invokeLater(() -> output.appendLine("ERROR: " + error))
            );

            SwingUtilities.invokeLater(() -> {
                runButton.setEnabled(true);
                stopButton.setEnabled(false);
                statusLabel.setText("Finished (exit code: " + exitCode + ")");
                System.gc();
            });
        }).start();
    }

    private void stopScript() {
        if (currentRunner.isRunning()) {
            currentRunner.stopScript();
            output.appendLine("\nScript execution stopped manually.");
        }
    }

    private void navigateToLocation(ErrorParser.Location location) {
        try {
            int line = location.getLine() - 1;
            int column = location.getColumn() - 1;

            Document doc = editor.getDocument();

            int startOffset = 0;
            int lineCount = 0;

            String text = doc.getText(0, doc.getLength());
            String[] lines = text.split("\n", -1);

            if (line < lines.length) {
                for (int i = 0; i < line; i++) {
                    startOffset += lines[i].length() + 1;
                }

                int targetColumn = Math.min(column, lines[line].length());
                int targetOffset = startOffset + targetColumn;

                editor.setCaretPosition(targetOffset);
                editor.requestFocusInWindow();

                highlightErrorLocation(targetOffset);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    private void highlightErrorLocation(int offset) {
        Highlighter highlighter = editor.getHighlighter();
        Highlighter.HighlightPainter painter =
                new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 80, 80));

        try {
            final Object tag = highlighter.addHighlight(offset, offset + 1, painter);

            Timer timer = new Timer(1500, e -> {
                highlighter.removeHighlight(tag);
            });
            timer.setRepeats(false);
            timer.start();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}