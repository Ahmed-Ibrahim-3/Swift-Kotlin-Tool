import Highlighters.KotlinHighlighter;
import Highlighters.ScriptHighlighter;
import Highlighters.SwiftHighlighter;
import Runners.KotlinRunner;
import Runners.ScriptRunner;
import Runners.SwiftRunner;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.FileWriter;
import java.io.IOException;

public class ScriptWindow extends JFrame {
    private JTextPane editor;
    private Output output;
    private JButton runButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JComboBox<String> languageSelector;

    private JTextField filePathField;
    private JButton saveButton;

    private ScriptRunner currentRunner;
    private SwiftRunner swiftRunner;
    private KotlinRunner kotlinRunner;

    private final SwiftHighlighter swiftHighlighter;
    private final KotlinHighlighter kotlinHighlighter;
    private ScriptHighlighter currentHighlighter;

    private static final Color DARK_BACKGROUND = new Color(43, 43, 43);
    private static final Color CONTROL_BAR_BG = new Color(60, 63, 65);
    private static final Color TEXT_COLOUR = new Color(169, 183, 198);
    private static final Color BORDER_COLOUR = new Color(53, 53, 53);
    private static final Color SELECTION_COLOUR = new Color(33, 66, 131);


    public ScriptWindow() {
        super();
        setForeground(TEXT_COLOUR);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
            getRootPane().putClientProperty("apple.awt.windowTitleVisible", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.windowAppearance", "NSAppearanceNameDarkAqua");
            getRootPane().putClientProperty("apple.awt.windowTitleForeground", TEXT_COLOUR);
        }

        UIManager.put("InternalFrame.titleForeground", TEXT_COLOUR);
        UIManager.put("TitlePane.foreground", TEXT_COLOUR);
        SwingUtilities.updateComponentTreeUI(this);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            UIManager.put("Panel.background", CONTROL_BAR_BG);
            UIManager.put("ComboBox.background", CONTROL_BAR_BG);
            UIManager.put("ComboBox.foreground", TEXT_COLOUR);
            UIManager.put("ComboBox.selectionBackground", SELECTION_COLOUR);
            UIManager.put("ComboBox.selectionForeground", TEXT_COLOUR);
            UIManager.put("Label.foreground", TEXT_COLOUR);
            UIManager.put("SplitPane.background", BORDER_COLOUR);
            UIManager.put("SplitPane.dividerSize", 4);
            UIManager.put("SplitPaneDivider.draggingColor", SELECTION_COLOUR);
        } catch (Exception e) {
            e.printStackTrace();
        }
        currentRunner = swiftRunner;

        swiftHighlighter = new SwiftHighlighter();
        kotlinHighlighter = new KotlinHighlighter();
        currentHighlighter = swiftHighlighter;

        initComponents();
        initRunners();
        setupLayout();
        setupListeners();

        setupMemorySettings();

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        applySyntaxHighlighting();

        editor.requestFocusInWindow();
    }

    private void setupMemorySettings() {
        System.setProperty("sun.java2d.nodraw", "true");
        System.setProperty("swing.aatext", "true");
    }

    private void initComponents() {
        editor = new JTextPane();
        editor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        editor.setText("/* Enter your code here */");
        editor.setBackground(DARK_BACKGROUND);
        editor.setForeground(TEXT_COLOUR);
        editor.setCaretColor(TEXT_COLOUR);
        editor.setSelectionColor(SELECTION_COLOUR);
        editor.setSelectedTextColor(TEXT_COLOUR);
        editor.setMargin(new Insets(10, 10, 10, 10));

        output = new Output(this::navigateToLocation);
        output.setBackground(DARK_BACKGROUND);
        output.setForeground(TEXT_COLOUR);
        output.setCaretColor(TEXT_COLOUR);
        output.setSelectionColor(SELECTION_COLOUR);
        output.setSelectedTextColor(TEXT_COLOUR);
        output.setMargin(new Insets(10, 10, 10, 10));

        output.setInputHandler(this::handleUserInput);

        languageSelector = new JComboBox<>(new String[]{"Swift", "Kotlin"});
        languageSelector.setForeground(TEXT_COLOUR);
        languageSelector.setBackground(CONTROL_BAR_BG);
        languageSelector.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton();
                button.setBackground(CONTROL_BAR_BG);
                button.setBorder(BorderFactory.createEmptyBorder());
                button.setIcon(new Icon() {
                    @Override
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int[] xPoints = {x, x + 8, x + 4};
                        int[] yPoints = {y, y, y + 4};
                        g2d.setColor(TEXT_COLOUR);
                        g2d.fillPolygon(xPoints, yPoints, 3);
                        g2d.dispose();
                    }

                    @Override
                    public int getIconWidth() {
                        return 8;
                    }

                    @Override
                    public int getIconHeight() {
                        return 4;
                    }
                });
                return button;
            }
        });

        languageSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                comp.setBackground(isSelected ? SELECTION_COLOUR : CONTROL_BAR_BG);
                comp.setForeground(TEXT_COLOUR);
                return comp;
            }
        });
        languageSelector.setFont(new Font("SF Pro", Font.PLAIN, 14));

        runButton = new JButton();
        runButton.setIcon(Start());
        runButton.setToolTipText("Run Script");
        runButton.setFocusPainted(false);
        runButton.setBorderPainted(false);
        runButton.setContentAreaFilled(false);
        runButton.setMargin(new Insets(5, 5, 5, 5));

        stopButton = new JButton();
        stopButton.setIcon(Stop());
        stopButton.setToolTipText("Stop Execution");
        stopButton.setEnabled(false);
        stopButton.setFocusPainted(false);
        stopButton.setBorderPainted(false);
        stopButton.setContentAreaFilled(false);
        stopButton.setMargin(new Insets(5, 5, 5, 5));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT_COLOUR);
        statusLabel.setFont(new Font("SF Pro", Font.PLAIN, 12));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 0, 5));

        filePathField = new JTextField();
        filePathField.setColumns(30);
        filePathField.setBackground(CONTROL_BAR_BG);
        filePathField.setForeground(TEXT_COLOUR);
        filePathField.setCaretColor(TEXT_COLOUR);
        filePathField.setToolTipText("Enter file path to save script");

        saveButton = new JButton("Save");
        saveButton.setBackground(SELECTION_COLOUR);
        saveButton.setForeground(TEXT_COLOUR);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setContentAreaFilled(true);
        saveButton.setOpaque(true);
        saveButton.addActionListener(e -> saveScript());
    }

    private Icon Start() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fillColor = new Color(0, 180, 0);
                Color outlineColor = new Color(0, 120, 0);

                int width = 18;
                int height = 18;

                Path2D path = new Path2D.Float();

                int x1 = x + 2;
                int y1 = y + 2;
                int x2 = x + width - 2;
                int y2 = y + height / 2;
                int x3 = x + 2;
                int y3 = y + height - 2;

                int cornerRadius = 2;

                path.moveTo(x1 + cornerRadius, y1);

                path.lineTo(x2 - cornerRadius, y2 - cornerRadius);
                path.quadTo(x2, y2, x2 - cornerRadius, y2 + cornerRadius);

                path.lineTo(x1 + cornerRadius, y3);

                path.quadTo(x1, y3, x1, y3 - cornerRadius);
                path.lineTo(x1, y1 + cornerRadius);
                path.quadTo(x1, y1, x1 + cornerRadius, y1);

                g2d.setStroke(new BasicStroke(.75f));
                g2d.setColor(outlineColor);
                g2d.draw(path);

                g2d.setColor(fillColor);
                g2d.fill(path);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    private Icon Stop() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color fillColor = new Color(220, 0, 0);
                Color outlineColor = new Color(150, 0, 0);

                int size = 18;
                int cornerRadius = size / 2;

                g2d.setColor(fillColor);
                g2d.fillRoundRect(x + 1, y + 1, size - 2, size - 2, cornerRadius - 1, cornerRadius - 1);

                g2d.setStroke(new BasicStroke(.75f));
                g2d.setColor(outlineColor);
                g2d.drawRoundRect(x, y, size, size, cornerRadius, cornerRadius);

                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return 14;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };
    }

    private void setupLayout() {
        JScrollPane editorScrollPane = new JScrollPane(editor);
        JScrollPane outputScrollPane = new JScrollPane(output);

        editorScrollPane.getViewport().setBackground(DARK_BACKGROUND);
        editorScrollPane.setBorder(BorderFactory.createEmptyBorder());
        outputScrollPane.getViewport().setBackground(DARK_BACKGROUND);
        outputScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel controlPanel = new JPanel(new BorderLayout(10, 0));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        controlPanel.setBackground(CONTROL_BAR_BG);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 100, 8));
        leftPanel.setBackground(CONTROL_BAR_BG);
        leftPanel.add(languageSelector);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(CONTROL_BAR_BG);
        rightPanel.add(runButton);
        rightPanel.add(stopButton);
        rightPanel.add(statusLabel);

        controlPanel.add(leftPanel, BorderLayout.WEST);
        controlPanel.add(rightPanel, BorderLayout.EAST);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorScrollPane, outputScrollPane);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerSize(4);
        splitPane.setBackground(BORDER_COLOUR);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        Container contentPane = getContentPane();
        contentPane.setBackground(BORDER_COLOUR);
        setLayout(new BorderLayout());
        add(controlPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        JPanel savePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        savePanel.setBackground(CONTROL_BAR_BG);
        savePanel.add(new JLabel("Save to: "));
        savePanel.add(filePathField);
        savePanel.add(saveButton);
        add(savePanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySyntaxHighlighting();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySyntaxHighlighting();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        swiftRunner.setInputRequiredCallback(() -> output.startWaitingForInput());
        kotlinRunner.setInputRequiredCallback(() -> output.startWaitingForInput());

        languageSelector.addActionListener((ActionEvent e) -> {
            String selectedLanguage = (String) languageSelector.getSelectedItem();
            if ("Swift".equals(selectedLanguage)) {
                currentRunner = swiftRunner;
                currentHighlighter = swiftHighlighter;
            } else if ("Kotlin".equals(selectedLanguage)) {
                currentRunner = kotlinRunner;
                currentHighlighter = kotlinHighlighter;
            }
            applySyntaxHighlighting();
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

    private void initRunners() {
        swiftRunner = new SwiftRunner();
        kotlinRunner = new KotlinRunner();
        currentRunner = swiftRunner;

        swiftRunner.setInputRequiredCallback(() -> {
            System.out.println("[DEBUG] ScriptWindow: Swift input callback triggered");
            SwingUtilities.invokeLater(() -> {
                output.startWaitingForInput();
                statusLabel.setText("Waiting for input...");
            });
        });

        kotlinRunner.setInputRequiredCallback(() -> {
            System.out.println("[DEBUG] ScriptWindow: Kotlin input callback triggered");
            SwingUtilities.invokeLater(() -> {
                output.startWaitingForInput();
                statusLabel.setText("Waiting for input...");
            });
        });

        output.setInputHandler((input) -> {
            System.out.println("[DEBUG] ScriptWindow: Input handler called with: " + input);
            if (currentRunner != null && currentRunner.isRunning()) {
                currentRunner.sendInput(input);
            }
        });
    }

    private void applySyntaxHighlighting() {
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

    private void handleUserInput(String input) {
        if (currentRunner != null && currentRunner.isRunning()) {
            currentRunner.sendInput(input);
        }
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
                output.stopWaitingForInput();
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

    private void saveScript() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            statusLabel.setText("Please enter a file path.");
            return;
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(editor.getText());
            statusLabel.setText("Script saved successfully.");
        } catch (IOException ex) {
            statusLabel.setText("Error saving script: " + ex.getMessage());
        }
    }
}

