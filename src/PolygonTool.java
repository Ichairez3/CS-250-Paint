import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

import java.util.function.Supplier;

public class PolygonTool implements Tool {
    private final Supplier<Integer> sidesSupplier;
    double sx, sy, cx, cy;

    public PolygonTool(Supplier<Integer> sidesSupplier) {
        this.sidesSupplier = sidesSupplier;
    }

    @Override public void onPressed(MouseEvent e, ImageDocument doc) {
        sx = cx = e.getX(); sy = cy = e.getY();
        doc.clearOverlay();
    }

    @Override public void onDragged(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        drawPreview(doc);
    }

    @Override public void onReleased(MouseEvent e, ImageDocument doc) {
        cx = e.getX(); cy = e.getY();
        int n = Math.max(3, sidesSupplier.get());

        // Build vertices of regular polygon in drag bounds
        double x = Math.min(sx, cx), y = Math.min(sy, cy);
        double w = Math.abs(cx - sx), h = Math.abs(cy - sy);
        // Use the smaller dimension as radius to fit
        double r = Math.min(w, h) / 2.0;
        double cxm = x + r;
        double cym = y + r;

        double[] xs = new double[n];
        double[] ys = new double[n];
        // Start pointing "up" for nicer feel
        double startAngle = -Math.PI / 2.0;
        for (int i = 0; i < n; i++) {
            double a = startAngle + i * (2 * Math.PI / n);
            xs[i] = cxm + r * Math.cos(a);
            ys[i] = cym + r * Math.sin(a);
        }

        // Dirty rect with padding
        double minX = xs[0], maxX = xs[0], minY = ys[0], maxY = ys[0];
        for (int i = 1; i < n; i++) {
            minX = Math.min(minX, xs[i]); maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]);
        }
        int pad = doc.strokeWidthProperty().get() + 4;
        Rectangle2D dirty = new Rectangle2D(Math.max(0, minX-pad), Math.max(0, minY-pad),
                (maxX-minX)+2*pad, (maxY-minY)+2*pad);

        double[] fx = xs.clone(), fy = ys.clone();
        doc.commit(gc -> {
            DocumentView.applyStrokeStyle(gc, doc);
            gc.strokePolygon(fx, fy, n);
        }, dirty);
    }

    private void drawPreview(ImageDocument doc) {
        int n = Math.max(3, sidesSupplier.get());

        double x = Math.min(sx, cx), y = Math.min(sy, cy);
        double w = Math.abs(cx - sx), h = Math.abs(cy - sy);
        double r = Math.min(w, h) / 2.0;
        double cxm = x + r;
        double cym = y + r;

        double[] xs = new double[n];
        double[] ys = new double[n];
        double startAngle = -Math.PI / 2.0;
        for (int i = 0; i < n; i++) {
            double a = startAngle + i * (2 * Math.PI / n);
            xs[i] = cxm + r * Math.cos(a);
            ys[i] = cym + r * Math.sin(a);
        }

        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        doc.clearOverlay();
        DocumentView.applyStrokeStyle(g, doc);
        g.strokePolygon(xs, ys, n);
    }
}
