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

    private final SimpleAttributeSet normalAttributes = new SimpleAttributeSet();

    private final SimpleAttributeSet errorLocationAttributes = new SimpleAttributeSet();

    private static final int MAX_BUFFER_SIZE = 500000;

    private boolean trimming = false;

    public Output(Consumer<ErrorParser.Location> locationClickHandler) {
        this.locationClickHandler = locationClickHandler;

        setEditable(false);
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        StyleConstants.setForeground(errorLocationAttributes, Color.RED);
        StyleConstants.setUnderline(errorLocationAttributes, true);

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