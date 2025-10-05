import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public class TriangleTool implements Tool {
    double sx, sy, cx, cy;

    @Override public void onPressed(MouseEvent e, ImageDocument doc) {
        sx = cx = e.getX(); sy = cy = e.getY();
        doc.clearOverlay();
    }

    @Override public void onDragged(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        drawPreview(doc, e.isShiftDown());
    }

    @Override public void onReleased(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        boolean right = e.isShiftDown();

        // Compute vertices
        double[] xs, ys;
        if (right) {
            xs = new double[]{ sx, cx, cx };
            ys = new double[]{ sy, sy, cy };
        } else {
            double x1 = Math.min(sx, cx), x2 = Math.max(sx, cx);
            double y1 = Math.min(sy, cy), y2 = Math.max(sy, cy);
            double midX = (x1 + x2) / 2.0;
            // isosceles with base on bottom (y2)
            xs = new double[]{ x1, x2, midX };
            ys = new double[]{ y2, y2, y1 };
        }

        // Dirty rect
        double minX = Math.min(xs[0], Math.min(xs[1], xs[2]));
        double maxX = Math.max(xs[0], Math.max(xs[1], xs[2]));
        double minY = Math.min(ys[0], Math.min(ys[1], ys[2]));
        double maxY = Math.max(ys[0], Math.max(ys[1], ys[2]));
        int pad = doc.strokeWidthProperty().get() + 4;
        Rectangle2D dirty = new Rectangle2D(Math.max(0, minX-pad), Math.max(0, minY-pad),
                (maxX-minX)+2*pad, (maxY-minY)+2*pad);

        double[] fx = xs.clone(), fy = ys.clone();
        doc.commit(gc -> {
            DocumentView.applyStrokeStyle(gc, doc);
            gc.strokePolygon(fx, fy, 3);
        }, dirty);
    }

    private void drawPreview(ImageDocument doc, boolean right) {
        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        doc.clearOverlay();
        DocumentView.applyStrokeStyle(g, doc);

        if (right) {
            double[] xs = new double[]{ sx, cx, cx };
            double[] ys = new double[]{ sy, sy, cy };
            g.strokePolygon(xs, ys, 3);
        } else {
            double x1 = Math.min(sx, cx), x2 = Math.max(sx, cx);
            double y1 = Math.min(sy, cy), y2 = Math.max(sy, cy);
            double midX = (x1 + x2) / 2.0;
            double[] xs = new double[]{ x1, x2, midX };
            double[] ys = new double[]{ y2, y2, y1 };
            g.strokePolygon(xs, ys, 3);
        }
    }
}
