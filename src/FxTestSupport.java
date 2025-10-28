import javafx.embed.swing.JFXPanel;

/** Ensures the JavaFX toolkit is started once for headless tests. */
public final class FxTestSupport {
    private static boolean inited = false;
    public static synchronized void init() {
        if (inited) return;
        // Creating a JFXPanel boots the JavaFX runtime.
        new JFXPanel();
        inited = true;
    }
    private FxTestSupport() {}
}
