import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

/**
 * A text pane that displays output with clickable error locations.
 */
public class Output extends JTextPane {
    private final Consumer<ErrorParser.Location> locationClickHandler;
    private Consumer<String> inputHandler;
    private boolean waitingForInput = false;
    private int inputStartPosition = 0;

    private final SimpleAttributeSet normalAttributes = new SimpleAttributeSet();
    private final SimpleAttributeSet errorLocationAttributes = new SimpleAttributeSet();
    private final SimpleAttributeSet inputAttributes = new SimpleAttributeSet();

    private static final int MAX_BUFFER_SIZE = 500000;

    private boolean trimming = false;

    public Output(Consumer<ErrorParser.Location> locationClickHandler) {
        this.locationClickHandler = locationClickHandler;

        setEditable(false);
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        StyleConstants.setForeground(errorLocationAttributes, Color.RED);
        StyleConstants.setUnderline(errorLocationAttributes, true);

        StyleConstants.setForeground(inputAttributes, new Color(0, 180, 0));
        StyleConstants.setBold(inputAttributes, true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = viewToModel2D(e.getPoint());
                if (pos >= 0 && hasErrorLocationAttributeAt(pos)) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (waitingForInput && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    submitInput();
                    e.consume();
                }
            }
        });
    }

    public void setInputHandler(Consumer<String> inputHandler) {
        this.inputHandler = inputHandler;
    }

    public void startWaitingForInput() {
        System.out.println("[DEBUG] Output: startWaitingForInput called");
        SwingUtilities.invokeLater(() -> {
            System.out.println("[DEBUG] Output: Setting up input mode");
            waitingForInput = true;
            setEditable(true);
            inputStartPosition = getDocument().getLength();

            try {
                System.out.println("[DEBUG] Output: Adding input prompt");
                appendText("> ", inputAttributes);

                setCaretPosition(getDocument().getLength());
                requestFocusInWindow();
                System.out.println("[DEBUG] Output: Input mode setup complete");
            } catch (Exception e) {
                System.err.println("[ERROR] Output: Exception in startWaitingForInput: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void stopWaitingForInput() {
        SwingUtilities.invokeLater(() -> {
            waitingForInput = false;
            setEditable(false);
        });
    }

    private void submitInput() {
        if (inputHandler != null && waitingForInput) {
            try {
                String userInput = getText(inputStartPosition, getDocument().getLength() - inputStartPosition).replaceAll(">", "");
                appendText("\n", normalAttributes);
                inputHandler.accept(userInput);
                stopWaitingForInput();
            } catch (BadLocationException e) {
                System.err.println("Error getting input text: " + e.getMessage());
            }
        }
    }

    public void appendLine(String line) {
        ErrorParser.Location location = ErrorParser.parseLocation(line);

        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = getStyledDocument();

            if (doc.getLength() > MAX_BUFFER_SIZE && !trimming) {
                trimming = true;

                try {
                    if (!line.contains("--- Output limit reached")) {
                        doc.insertString(doc.getLength(),
                                "\n--- Document size limit reached, older content will be removed ---\n",
                                normalAttributes);
                    }

                    int trimPoint = doc.getLength() / 4;
                    doc.remove(0, trimPoint);

                } catch (BadLocationException e) {
                    System.err.println("Error trimming document: " + e.getMessage());
                } finally {
                    trimming = false;
                }
            }

            try {
                if (location != null) {
                    int locStart = line.indexOf(location.getFullMatch());
                    int locEnd = locStart + location.getFullMatch().length();

                    if (locStart > 0) {
                        doc.insertString(doc.getLength(), line.substring(0, locStart), normalAttributes);
                    }

                    AttributeSet locationAttributes = getErrorLocationAttributes(location);
                    doc.insertString(doc.getLength(), line.substring(locStart, locEnd), locationAttributes);

                    if (locEnd < line.length()) {
                        doc.insertString(doc.getLength(), line.substring(locEnd), normalAttributes);
                    }
                } else {
                    doc.insertString(doc.getLength(), line, normalAttributes);
                }

                doc.insertString(doc.getLength(), "\n", normalAttributes);
            } catch (BadLocationException e) {
                try {
                    doc.insertString(doc.getLength(), line + "\n", normalAttributes);
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
            scrollToBottom();
        });
    }

    public void appendText(String text, AttributeSet attributes) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = getStyledDocument();
                doc.insertString(doc.getLength(), text, attributes);
                scrollToBottom();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void scrollToBottom() {
        Document doc = getDocument();
        Rectangle visible = getVisibleRect();

        try {
            Rectangle lastRect = (Rectangle) modelToView2D(doc.getLength());
            if (lastRect != null &&
                    (lastRect.y - (visible.y + visible.height) > 100 ||
                            lastRect.y < visible.y)) {
                setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException e) {
            setCaretPosition(doc.getLength());
        }
    }

    public void clear() {
        setText("");
    }

    private void handleMouseClick(MouseEvent e) {
        StyledDocument doc = getStyledDocument();
        Element elem = doc.getCharacterElement(viewToModel2D(e.getPoint()));
        AttributeSet attrs = elem.getAttributes();

        ErrorParser.Location location =
                (ErrorParser.Location) attrs.getAttribute("location");

        if (location != null && locationClickHandler != null) {
            locationClickHandler.accept(location);
        }
    }

    private AttributeSet getErrorLocationAttributes(ErrorParser.Location location) {
        SimpleAttributeSet attrs = new SimpleAttributeSet(errorLocationAttributes);
        attrs.addAttribute("location", location);
        return attrs;
    }

    private boolean hasErrorLocationAttributeAt(int pos) {
        StyledDocument doc = getStyledDocument();
        if (pos >= 0 && pos < doc.getLength()) {
            Element elem = doc.getCharacterElement(pos);
            return elem.getAttributes().getAttribute("location") != null;
        }
        return false;
    }
}