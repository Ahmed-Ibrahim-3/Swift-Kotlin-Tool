import javax.swing.*;

public class Main{
    public static void main(String[] args) {
        System.setProperty("sun.awt.keepWorkingSetOnMinimize","true");
        SwingUtilities.invokeLater(ScriptWindow::new);
    }
}