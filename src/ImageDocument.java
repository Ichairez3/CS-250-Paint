import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Holds the image being edited plus drawing state shared across tools.
 * Backing pixels live in {@link #backing}. The base {@link #canvas} shows the
 * current image; tools draw onto this canvas via {@link #commit(Consumer, Rectangle2D)}.
 * A transparent {@link #overlay} sits above for live previews.
 *
 * Features:
 * - Undo/Redo using full-image snapshots (simple & reliable)
 * - Selection rectangle & in-app image clipboard
 * - Stroke color/width & dashed properties bound to UI
 * - Dirty tracking & display name
 */
public class ImageDocument {

    //Canvas & pixel state
    private final Canvas canvas;
    private final Canvas overlay;
    private WritableImage backing;

    private File file;             // null if untitled
    private boolean dirty = false;

    //Drawing properties
    private final ObjectProperty<Color> strokeColor = new SimpleObjectProperty<>(Color.BLACK);
    private final IntegerProperty strokeWidth = new SimpleIntegerProperty(4);
    private final BooleanProperty dashed = new SimpleBooleanProperty(false);

    //Undo/Redo
    private final Deque<WritableImage> undoStack = new ArrayDeque<>();
    private final Deque<WritableImage> redoStack = new ArrayDeque<>();
    private static final int MAX_HISTORY = 50;

    //Selection & clipboard
    private Rectangle2D selectionRect = null;
    private WritableImage clipboard = null;

    //Constructors

    /** Create a blank white document of size (w x h). */
    public static ImageDocument newBlank(int w, int h) {
        return new ImageDocument(w, h, /*fillWhite=*/true, null);
    }

    /** Create a document from an existing image (drawn at 0,0). */
    public static ImageDocument fromImage(Image img, File srcFile) {
        int w = (int) Math.ceil(img.getWidth());
        int h = (int) Math.ceil(img.getHeight());
        ImageDocument d = new ImageDocument(w, h, /*fillWhite=*/false, srcFile);
        GraphicsContext g = d.canvas.getGraphicsContext2D();
        g.drawImage(img, 0, 0);
        d.resyncBackingFromCanvas();
        d.dirty = false;
        return d;
    }

    private ImageDocument(int w, int h, boolean fillWhite, File file) {
        this.file = file;
        this.canvas = new Canvas(w, h);
        this.overlay = new Canvas(w, h);
        this.backing = new WritableImage(w, h);

        GraphicsContext g = canvas.getGraphicsContext2D();
        if (fillWhite) {
            g.setFill(Color.WHITE);
            g.fillRect(0, 0, w, h);
        }
        // Ensure backing matches canvas initially
        resyncBackingFromCanvas();
        this.dirty = fillWhite;
    }

    //Core operations

    /**
     * Execute a drawing operation on the base canvas and record it for undo/redo.
     * The overlay is cleared automatically after commit.
     * @param painter Consumer that receives the GraphicsContext to draw onto.
     * @param dirtyRect Region affected (currently informational; kept for future optimization).
     */
    public void commit(Consumer<GraphicsContext> painter, Rectangle2D dirtyRect) {
        // 1) Save current state to undo
        pushUndoSnapshot();
        // 2) Apply drawing
        painter.accept(canvas.getGraphicsContext2D());
        // 3) Resync pixels
        resyncBackingFromCanvas();
        // 4) Mark dirty, clear overlay
        dirty = true;
        clearOverlay();
    }

    /** Undo last change, if any. */
    public void undo() {
        if (undoStack.isEmpty()) return;
        // Push current state to redo
        WritableImage current = snapshotCanvas();
        redoStack.push(current);
        // Pop previous from undo and show it
        WritableImage prev = undoStack.pop();
        drawImageToCanvas(prev);
        backing = prev;
        dirty = true;
        clearOverlay();
    }

    /** Redo last undone change, if any. */
    public void redo() {
        if (redoStack.isEmpty()) return;
        // Push current to undo
        pushUndoSnapshot();
        // Apply redo image
        WritableImage next = redoStack.pop();
        drawImageToCanvas(next);
        backing = next;
        dirty = true;
        clearOverlay();
    }

    /** Clear the live preview overlay. */
    public void clearOverlay() {
        GraphicsContext g = overlay.getGraphicsContext2D();
        g.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
    }

    /** Redraw the base canvas from the backing image. */
    public void redrawBase() {
        drawImageToCanvas(backing);
    }

    //Undo/Redo helpers

    private void pushUndoSnapshot() {
        WritableImage snap = snapshotCanvas();
        undoStack.push(snap);
        while (undoStack.size() > MAX_HISTORY) undoStack.removeLast();
        // any new drawing invalidates redo history
        redoStack.clear();
    }

    private WritableImage snapshotCanvas() {
        WritableImage shot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        SnapshotParameters params = new SnapshotParameters();
        // (No transforms; canvas->image 1:1)
        canvas.snapshot(params, shot);
        return shot;
    }

    private void resyncBackingFromCanvas() {
        backing = snapshotCanvas();
    }

    private void drawImageToCanvas(Image img) {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.drawImage(img, 0, 0);
    }

    //Selection & Clipboard

    /** Current selection rectangle in image coordinates, or null if none. */
    public Rectangle2D getSelection() { return selectionRect; }

    /** Set current selection rectangle (use null to clear). */
    public void setSelection(Rectangle2D r) { selectionRect = r; }

    /** Convenience: clear the selection. */
    public void clearSelection() { selectionRect = null; }

    /** In-app image clipboard (used by Copy/Cut/Paste), or null if empty. */
    public WritableImage getClipboard() { return clipboard; }

    /** Replace the in-app clipboard image (set null to clear). */
    public void setClipboard(WritableImage img) { clipboard = img; }

    //Accessors

    public Canvas getCanvas() { return canvas; }

    public Canvas getOverlay() { return overlay; }

    /** Latest pixel image of the document for read operations (e.g., eyedropper, copy). */
    public WritableImage getBacking() { return backing; }

    /** True if there are unsaved changes. */
    public boolean isDirty() { return dirty; }

    /** Mark the document clean after a successful save, and remember the file. */
    public void markSaved(File f) {
        this.file = f;
        this.dirty = false;
    }

    /** Set file without changing dirty flag (used by open). */
    public void setFile(File f) { this.file = f; }

    public File getFile() { return file; }

    /** Tab title / display name. Shows '*' if dirty. */
    public String getDisplayName() {
        String base = (file != null) ? file.getName() : "Untitled";
        return dirty ? (base + "*") : base;
    }



    // Stroke color/width/dashed properties (bound to UI)
    public ObjectProperty<Color> strokeColorProperty() { return strokeColor; }
    public IntegerProperty strokeWidthProperty() { return strokeWidth; }
    public BooleanProperty dashedProperty() { return dashed; }

}
