import javax.swing.*;
import java.awt.*;

public class ScriptWindow extends JFrame {
    private JTextPane editor;
    private JTextArea output;

    public ScriptWindow() {
        super("Script Executor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initComponents();
        setupLayout();

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

    }

    private void setupLayout() {
        JScrollPane editorScrollPane = new JScrollPane(editor);
        JScrollPane outputScrollPane = new JScrollPane(output);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, outputScrollPane);
        splitPane.setResizeWeight(0.66);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }
}
