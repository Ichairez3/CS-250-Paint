import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import javafx.geometry.Rectangle2D;
import javafx.scene.paint.Color;

public class ImageDocumentUndoRedoTest {
    @BeforeAll
    static void setup() { FxTestSupport.init(); }

    @Test
    void undoRedoRestoresPixels() {
        ImageDocument doc = ImageDocument.newBlank(50, 50);

        // Draw a 10x10 black block at (20,20)
        Rectangle2D dirty = new Rectangle2D(20, 20, 10, 10);
        doc.commit(gc -> {
            gc.setFill(Color.BLACK);
            gc.fillRect(20, 20, 10, 10);
        }, dirty);

        // Center point (25,25) should be black after commit
        Color after = doc.getBacking().getPixelReader().getColor(25, 25);
        Assertions.assertTrue(isNearly(after, Color.BLACK), "Expected black after commit");

        // Undo -> back to white
        doc.undo();
        Color afterUndo = doc.getBacking().getPixelReader().getColor(25, 25);
        Assertions.assertTrue(isNearly(afterUndo, Color.WHITE), "Expected white after undo");

        // Redo -> black again
        doc.redo();
        Color afterRedo = doc.getBacking().getPixelReader().getColor(25, 25);
        Assertions.assertTrue(isNearly(afterRedo, Color.BLACK), "Expected black after redo");
    }

    private static boolean isNearly(Color a, Color b) {
        double eps = 1e-6;
        return Math.abs(a.getRed() - b.getRed()) < eps
                && Math.abs(a.getGreen() - b.getGreen()) < eps
                && Math.abs(a.getBlue() - b.getBlue()) < eps
                && Math.abs(a.getOpacity() - b.getOpacity()) < eps;
    }
}
