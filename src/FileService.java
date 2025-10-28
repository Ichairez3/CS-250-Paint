import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Locale;

public class FileService {

    /** Open an image from disk. Returns null if user cancels. */
    public static ImageDocument open(Stage owner) throws Exception {
        FileChooser fc = buildOpenChooser();
        File file = fc.showOpenDialog(owner);
        if (file == null) return null;

        Image img = new Image(file.toURI().toString());
        if (img.isError() || img.getWidth() <= 0 || img.getHeight() <= 0) {
            throw new Exception("Unsupported or unreadable image file.");
        }
        return ImageDocument.fromImage(img, file);
    }

    /** Save to current file if known; otherwise falls back to Save As. Returns false if canceled. */
    public static boolean save(Stage owner, ImageDocument doc) throws Exception {
        File f = doc.getFile();
        if (f == null) return saveAs(owner, doc);
        writeToFile(doc, f);
        doc.markSaved(f);
        return true;
    }

    /** Ask for a path/format and save. Returns false if user cancels. */
    public static boolean saveAs(Stage owner, ImageDocument doc) throws Exception {
        FileChooser fc = buildSaveChooser();
        fc.setInitialFileName(suggestName(doc));
        File chosen = fc.showSaveDialog(owner);
        if (chosen == null) return false;

        FileChooser.ExtensionFilter sel = fc.getSelectedExtensionFilter();
        String ext = extensionOf(chosen);
        if (ext.isEmpty() && sel != null) {
            String def = defaultExtFor(sel);
            chosen = new File(chosen.getParentFile(), chosen.getName() + "." + def);
        }

        writeToFile(doc, chosen);
        doc.markSaved(chosen);
        return true;
    }

    /** AUTOSAVE: write a PNG snapshot to ~/PaintFXAutosaves (does NOT change doc’s saved state). */
    public static File autosave(ImageDocument doc) throws Exception {
        File dir = new File(System.getProperty("user.home"), "PaintFXAutosaves");
        dir.mkdirs();
        String base = (doc.getFile() != null) ? stripExt(doc.getFile().getName()) : stripExt(doc.getDisplayName());
        String safe = base.replaceAll("[^A-Za-z0-9._-]", "_");
        File out = new File(dir, safe + "-" + System.currentTimeMillis() + ".autosave.png");
        writeToFile(doc, out);
        return out;
    }

    /** Public so autosave/export paths can reuse it. Chooses format from file extension. */
    public static void writeToFile(ImageDocument doc, File file) throws Exception {
        String fmt = formatFor(file);
        WritableImage fxImg = doc.getBacking();
        if (fxImg == null) throw new Exception("No image data to save.");

        BufferedImage src = SwingFXUtils.fromFXImage(fxImg, null);

        if (fmt.equals("jpg")) {
            BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = rgb.createGraphics();
            try {
                g2.setColor(java.awt.Color.WHITE);
                g2.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g2.drawImage(src, 0, 0, null);
            } finally { g2.dispose(); }
            ImageIO.write(rgb, "jpg", file);
        } else if (fmt.equals("bmp")) {
            ImageIO.write(src, "bmp", file);
        } else {
            ImageIO.write(src, "png", file);
        }
    }

    //Helpers
    private static FileChooser buildOpenChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Image");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported (*.png, *.jpg, *.jpeg, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.bmp"),
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG (*.jpg;*.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("BMP (*.bmp)", "*.bmp")
        );
        return fc;
    }
    private static FileChooser buildSaveChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Image As");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG (*.png)", "*.png"),
                new FileChooser.ExtensionFilter("JPEG (*.jpg;*.jpeg)", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("BMP (*.bmp)", "*.bmp")
        );
        fc.setSelectedExtensionFilter(fc.getExtensionFilters().get(0));
        return fc;
    }
    private static String suggestName(ImageDocument doc) {
        File f = doc.getFile();
        if (f != null && f.getName() != null) {
            String n = f.getName(); int dot = n.lastIndexOf('.'); return (dot > 0 ? n.substring(0, dot) : n);
        }
        return "Untitled";
    }
    private static String extensionOf(File f) {
        String n = f.getName(); int dot = n.lastIndexOf('.'); if (dot < 0) return ""; return n.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
    private static String defaultExtFor(FileChooser.ExtensionFilter sel) {
        String desc = sel.getDescription().toLowerCase(Locale.ROOT);
        if (desc.contains("png")) return "png";
        if (desc.contains("jpeg") || desc.contains("jpg")) return "jpg";
        if (desc.contains("bmp")) return "bmp";
        return "png";
    }
    private static String formatFor(File f) {
        String ext = extensionOf(f);
        if (ext.equals("png")) return "png";
        if (ext.equals("jpg") || ext.equals("jpeg")) return "jpg";
        if (ext.equals("bmp")) return "bmp";
        return "png";
    }
    private static String stripExt(String n) {
        int dot = n.lastIndexOf('.'); return dot > 0 ? n.substring(0, dot) : n;
    }
}
