import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public class EllipseTool implements Tool {
    double sx, sy, cx, cy;

    @Override public void onPressed(MouseEvent e, ImageDocument doc) {
        sx = cx = e.getX(); sy = cy = e.getY();
        doc.clearOverlay();
    }

    @Override public void onDragged(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        boolean circle = e.isShiftDown(); // Hold Shift for circle
        double x = Math.min(sx, cx);
        double y = Math.min(sy, cy);
        double w = Math.abs(cx - sx);
        double h = Math.abs(cy - sy);
        if (circle) { double m = Math.max(w, h); w = h = m; if (cx < sx) x = sx - m; if (cy < sy) y = sy - m; }

        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        doc.clearOverlay();
        DocumentView.applyStrokeStyle(g, doc);
        g.strokeOval(x, y, w, h);
    }

    @Override public void onReleased(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        boolean circle = e.isShiftDown();
        double x = Math.min(sx, cx);
        double y = Math.min(sy, cy);
        double w = Math.abs(cx - sx);
        double h = Math.abs(cy - sy);
        if (circle) { double m = Math.max(w, h); w = h = m; if (cx < sx) x = sx - m; if (cy < sy) y = sy - m; }

        Rectangle2D dirty = new Rectangle2D(x, y, w, h);
        int pad = doc.strokeWidthProperty().get() + 4;
        dirty = new Rectangle2D(Math.max(0, dirty.getMinX()-pad), Math.max(0, dirty.getMinY()-pad),
                dirty.getWidth()+2*pad, dirty.getHeight()+2*pad);

        double fx = x, fy = y, fw = w, fh = h;
        doc.commit(gc -> {
            DocumentView.applyStrokeStyle(gc, doc);
            gc.strokeOval(fx, fy, fw, fh);
        }, dirty);
    }
}
