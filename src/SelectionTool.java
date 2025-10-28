import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public class SelectionTool implements Tool {
    double sx, sy, cx, cy;
    boolean dragging = false;

    @Override public void onPressed(MouseEvent e, ImageDocument doc) {
        dragging = true;
        sx = cx = e.getX(); sy = cy = e.getY();
        doc.clearOverlay();
        doc.setSelection(null);
    }

    @Override public void onDragged(MouseEvent e, ImageDocument doc) {
        if (!dragging) return;
        cx = e.getX(); cy = e.getY();

        double x = Math.min(sx, cx);
        double y = Math.min(sy, cy);
        double w = Math.abs(cx - sx);
        double h = Math.abs(cy - sy);

        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        doc.clearOverlay();
        g.setLineWidth(1.0);
        g.setStroke(javafx.scene.paint.Color.BLACK);
        g.setLineDashes(6, 4);
        g.strokeRect(x, y, w, h);
        g.setLineDashes(0); // reset

        // Save tentative selection (integers)
        doc.setSelection(new Rectangle2D(Math.max(0, Math.floor(x)), Math.max(0, Math.floor(y)),
                Math.max(1, Math.ceil(w)), Math.max(1, Math.ceil(h))));
    }

    @Override public void onReleased(MouseEvent e, ImageDocument doc) {
        dragging = false;
        // Keep the dashed box on overlay until user changes tool or clears selection
        // (nothing else to do)
    }
}
