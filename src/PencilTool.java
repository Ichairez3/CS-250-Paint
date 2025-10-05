import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class PencilTool implements Tool {
    private final List<Double> xs = new ArrayList<>();
    private final List<Double> ys = new ArrayList<>();
    private double minX, minY, maxX, maxY;

    @Override
    public void onPressed(MouseEvent e, ImageDocument doc) {
        xs.clear(); ys.clear();
        double x = e.getX(), y = e.getY();
        xs.add(x); ys.add(y);
        minX = maxX = x;
        minY = maxY = y;

        // Start fresh overlay for this stroke
        doc.clearOverlay();

        // Draw a tiny dot preview so taps show something immediately
        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        DocumentView.applyStrokeStyle(g, doc);
        g.strokeLine(x, y, x, y);
    }

    @Override
    public void onDragged(MouseEvent e, ImageDocument doc) {
        double x = e.getX(), y = e.getY();
        double lx = xs.get(xs.size() - 1), ly = ys.get(ys.size() - 1);

        // live preview on overlay
        GraphicsContext g = doc.getOverlay().getGraphicsContext2D();
        DocumentView.applyStrokeStyle(g, doc);
        g.strokeLine(lx, ly, x, y);

        xs.add(x); ys.add(y);
        if (x < minX) minX = x; if (x > maxX) maxX = x;
        if (y < minY) minY = y; if (y > maxY) maxY = y;
    }

    @Override
    public void onReleased(MouseEvent e, ImageDocument doc) {
        // include final point
        double x = e.getX(), y = e.getY();
        if (xs.isEmpty() || xs.get(xs.size()-1) != x || ys.get(ys.size()-1) != y) {
            xs.add(x); ys.add(y);
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
        }

        // Compute dirty rect with padding for stroke width
        int pad = doc.strokeWidthProperty().get() + 4;
        Rectangle2D dirty;
        if (xs.size() == 1) {
            // dot tap
            dirty = new Rectangle2D(Math.max(0, xs.get(0) - pad), Math.max(0, ys.get(0) - pad),
                    pad * 2.0, pad * 2.0);
        } else {
            dirty = new Rectangle2D(
                    Math.max(0, minX - pad), Math.max(0, minY - pad),
                    (maxX - minX) + 2.0 * pad, (maxY - minY) + 2.0 * pad
            );
        }

        // Commit the entire path to the backing image
        List<Double> px = new ArrayList<>(xs);
        List<Double> py = new ArrayList<>(ys);
        doc.commit(gc -> {
            DocumentView.applyStrokeStyle(gc, doc);
            // Round caps/joins for smoother pencil
            gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

            if (px.size() == 1) {
                double xx = px.get(0), yy = py.get(0);
                gc.strokeLine(xx, yy, xx, yy); // dot
            } else {
                double lx = px.get(0), ly = py.get(0);
                for (int i = 1; i < px.size(); i++) {
                    double cx = px.get(i), cy = py.get(i);
                    gc.strokeLine(lx, ly, cx, cy);
                    lx = cx; ly = cy;
                }
            }
        }, dirty);

    }
}
