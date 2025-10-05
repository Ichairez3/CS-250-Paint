import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import java.util.function.Supplier;

public class StarTool implements Tool {

    private final Supplier<Integer> pointsSupplier; // e.g., () -> app.getStarPoints()
    private static final double INNER_RATIO = 0.5;  // fixed spikiness

    private double cx, cy, mx, my;
    private boolean dragging = false;

    public StarTool(Supplier<Integer> pointsSupplier) {
        this.pointsSupplier = pointsSupplier;
    }

    // >>> Match your Tool interface signatures <<<

    @Override
    public void onPressed(MouseEvent e, ImageDocument doc) {
        cx = mx = e.getX();
        cy = my = e.getY();
        dragging = true;
        preview(doc);
    }

    @Override
    public void onDragged(MouseEvent e, ImageDocument doc) {
        mx = e.getX();
        my = e.getY();
        preview(doc);
    }

    @Override
    public void onReleased(MouseEvent e, ImageDocument doc) {
        if (!dragging) return;
        dragging = false;
        mx = e.getX();
        my = e.getY();

        int n = clampPoints(pointsSupplier.get());
        double outerR = Math.hypot(mx - cx, my - cy);
        if (outerR < 0.5) { doc.clearOverlay(); return; }

        StarGeom g = computeStar(cx, cy, outerR, INNER_RATIO, n);
        double w = doc.strokeWidthProperty().get();
        Rectangle2D dirty = expand(boundsOf(g), w * 0.5 + 2);

        doc.commit(gc -> {
            gc.setLineWidth(w);
            gc.setStroke(doc.strokeColorProperty().get());
            if (doc.dashedProperty().get()) gc.setLineDashes(8, 6);
            gc.strokePolygon(g.x, g.y, g.x.length);
            gc.setLineDashes(0);
        }, dirty);

        doc.clearOverlay();
        doc.redrawBase();
    }

    //helpers

    private void preview(ImageDocument doc) {
        doc.clearOverlay();
        int n = clampPoints(pointsSupplier.get());
        double outerR = Math.hypot(mx - cx, my - cy);
        if (outerR < 0.5) return;

        StarGeom g = computeStar(cx, cy, outerR, INNER_RATIO, n);
        GraphicsContext og = doc.getOverlay().getGraphicsContext2D();
        og.setLineWidth(doc.strokeWidthProperty().get());
        og.setStroke(doc.strokeColorProperty().get());
        if (doc.dashedProperty().get()) og.setLineDashes(8, 6);
        og.strokePolygon(g.x, g.y, g.x.length);
        og.setLineDashes(0);
    }

    private static int clampPoints(Integer v) {
        if (v == null) return 5;
        return Math.max(4, Math.min(64, v));
    }

    private static class StarGeom { double[] x, y; }
    private static StarGeom computeStar(double cx, double cy, double outerR, double innerRatio, int points) {
        int total = points * 2;
        double[] xs = new double[total];
        double[] ys = new double[total];
        double innerR = outerR * innerRatio;
        double angle0 = -Math.PI / 2.0;

        for (int i = 0; i < total; i++) {
            boolean outer = (i % 2 == 0);
            double r = outer ? outerR : innerR;
            double t = angle0 + (i * Math.PI / points);
            xs[i] = cx + r * Math.cos(t);
            ys[i] = cy + r * Math.sin(t);
        }
        StarGeom g = new StarGeom(); g.x = xs; g.y = ys; return g;
    }

    private static Rectangle2D boundsOf(StarGeom g) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < g.x.length; i++) {
            minX = Math.min(minX, g.x[i]);
            minY = Math.min(minY, g.y[i]);
            maxX = Math.max(maxX, g.x[i]);
            maxY = Math.max(maxY, g.y[i]);
        }
        return new Rectangle2D(minX, minY, Math.max(1, maxX - minX), Math.max(1, maxY - minY));
    }

    private static Rectangle2D expand(Rectangle2D r, double pad) {
        return new Rectangle2D(r.getMinX() - pad, r.getMinY() - pad,
                r.getWidth() + pad * 2, r.getHeight() + pad * 2);
    }
}
