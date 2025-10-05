import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public class LineTool implements Tool {
    double sx, sy, cx, cy;

    @Override public void onPressed(MouseEvent e, ImageDocument doc) {
        sx = cx = e.getX(); sy = cy = e.getY();
    }

    @Override public void onDragged(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        doc.clearOverlay();
        DocumentView.applyStrokeStyle(g, doc);
        g.strokeLine(sx, sy, cx, cy); // live preview (can be dashed if toggled)
    }

    @Override public void onReleased(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        Rectangle2D dirty = DocumentView.dirtyFrom(sx,sy,cx,cy, doc.strokeWidthProperty().get()+6);
        doc.commit(gc -> {
            DocumentView.applyStrokeStyle(gc, doc);
            gc.strokeLine(sx, sy, cx, cy);
        }, dirty);
    }
}
