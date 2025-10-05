import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** Tiny runtime-generated icons for tools (JavaFX only, no external assets). */
public final class IconFactory {
    private IconFactory() {}

    // public helpers (pick your size: 16..24 look best)
    public static Image pencil(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        // diagonal pencil body
        g.strokeLine(s*0.20, s*0.75, s*0.80, s*0.25);
        // tip
        g.strokeLine(s*0.78, s*0.27, s*0.88, s*0.17);
        g.strokeLine(s*0.80, s*0.25, s*0.90, s*0.15);
        // eraser cap
        g.strokeRect(s*0.16, s*0.71, s*0.08, s*0.08);
    });}

    public static Image line(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        g.strokeLine(s*0.20, s*0.75, s*0.80, s*0.25);
    });}

    public static Image rect(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        g.strokeRect(s*0.20, s*0.25, s*0.60, s*0.50);
    });}

    public static Image ellipse(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        g.strokeOval(s*0.20, s*0.25, s*0.60, s*0.50);
    });}

    public static Image triangle(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        double x1=s*0.50, y1=s*0.22;
        double x2=s*0.20, y2=s*0.75;
        double x3=s*0.80, y3=s*0.75;
        g.strokePolygon(new double[]{x1,x2,x3}, new double[]{y1,y2,y3}, 3);
    });}

    public static Image polygon(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        int n=5;
        double cx=s*0.50, cy=s*0.50, r=s*0.28;
        double[] xs=new double[n], ys=new double[n];
        for(int i=0;i<n;i++){
            double t = -Math.PI/2 + i*2*Math.PI/n;
            xs[i]=cx + r*Math.cos(t);
            ys[i]=cy + r*Math.sin(t);
        }
        g.strokePolygon(xs, ys, n);
    });}

    public static Image text(int s){ return draw(s, g -> {
        g.setFill(Color.BLACK);
        g.setStroke(Color.BLACK);
        g.setLineWidth(1.5);
        g.setFont(Font.font("System", FontWeight.BOLD, s*0.72));
        // center-ish "T"
        g.fillText("T", s*0.30, s*0.74);
    });}

    public static Image eyedropper(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        // bulb
        g.strokeOval(s*0.62, s*0.16, s*0.20, s*0.20);
        // stem
        g.strokeLine(s*0.30, s*0.70, s*0.70, s*0.30);
        // tip
        g.strokeLine(s*0.25, s*0.75, s*0.30, s*0.70);
    });}

    public static Image eraser(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        g.strokePolygon(
                new double[]{s*0.25, s*0.50, s*0.70, s*0.45},
                new double[]{s*0.65, s*0.40, s*0.55, s*0.80},
                4
        );
    });}

    public static Image marquee(int s){ return draw(s, g -> {
        g.setLineWidth(2);
        g.setStroke(Color.BLACK);
        g.setLineDashes(4,3);
        g.strokeRect(s*0.18, s*0.18, s*0.64, s*0.64);
        g.setLineDashes(0);
    });}

    //drawing core
    private interface Painter { void paint(GraphicsContext g); }
    private static Image draw(int s, Painter painter){
        Canvas c = new Canvas(s, s);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setFill(Color.TRANSPARENT);
        g.setStroke(Color.BLACK);
        painter.paint(g);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage out = new WritableImage(s, s);
        c.snapshot(sp, out);
        return out;
    }

    public static Image star(int s){
        return draw(s, g -> {
            g.setLineWidth(2);
            g.setStroke(Color.BLACK);
            int n = 5;
            double cx = s * 0.5, cy = s * 0.5, outer = s * 0.36, inner = outer * 0.5;
            double[] xs = new double[n*2], ys = new double[n*2];
            double a0 = -Math.PI/2;
            for (int i = 0; i < n*2; i++) {
                double r = (i % 2 == 0) ? outer : inner;
                double t = a0 + i * Math.PI / n;
                xs[i] = cx + r * Math.cos(t);
                ys[i] = cy + r * Math.sin(t);
            }
            g.strokePolygon(xs, ys, xs.length);
        });
    }

}
