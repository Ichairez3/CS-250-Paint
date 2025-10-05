import javafx.application.Platform;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.image.BufferedImage;

/** OS notifications via SystemTray with a JavaFX toast fallback. */
public class NotificationService {
    private volatile boolean enabled = true;

    private TrayIcon trayIcon;
    private boolean trayReady = false;
    private Stage ownerForToasts; // main window for positioning toasts

    public void init(Stage owner) {
        this.ownerForToasts = owner;
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                if (trayIcon == null) {
                    trayIcon = new TrayIcon(makeIcon(), "PaintFX");
                    trayIcon.setImageAutoSize(true);
                    tray.add(trayIcon);
                }
                trayReady = true;
            } else {
                trayReady = false;
            }
        } catch (Exception ex) {
            trayReady = false;
        }
    }

    public void shutdown() {
        try {
            if (trayIcon != null) {
                SystemTray.getSystemTray().remove(trayIcon);
                trayIcon = null;
            }
        } catch (Exception ignored) {}
    }

    //public API

    public void setEnabled(boolean on) { enabled = on; }
    public boolean isEnabled() { return enabled; }

    public void notifyAutosaveEnabled() {
        notify("Autosave", "Autosave enabled");
    }

    public void notifyAutosaveDisabled() {
        notify("Autosave", "Autosave disabled");
    }

    public void notifyAutosaveFired(int savedDocs) {
        if (savedDocs <= 0) return; // be quiet if nothing was saved
        String msg = (savedDocs == 1) ? "Saved 1 tab" : ("Saved " + savedDocs + " tabs");
        notify("Autosave", msg);
    }

    //internals

    private void notify(String title, String message) {
        if (!enabled) return;
        // try OS tray first
        if (trayReady && trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
                return;
            } catch (Exception ignored) { /* fall through to toast */ }
        }
        // fallback toast in the app window
        final String m = (title == null || title.isEmpty()) ? message : (title + ": " + message);
        Platform.runLater(() -> InAppNotification.show(ownerForToasts, m, 2500));
    }

    private static Image makeIcon() {
        // tiny 16x16 “PF” badge
        int s = 16;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(32, 140, 240)); g.fillRoundRect(0,0,s,s,6,6);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString("PF", 2, 12);
        g.dispose();
        return img;
    }
    public void show(String title, String message) {
        notify(title, message);
    }

}
