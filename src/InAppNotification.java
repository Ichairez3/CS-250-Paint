import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/** Simple popup shown near the bottom-right of the main window. */
public final class InAppNotification {
    private InAppNotification(){}

    public static void show(Stage owner, String message, int millis) {
        if (owner == null || !owner.isShowing()) return;

        Label label = new Label(message);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-background-color: rgba(30,30,30,0.92); -fx-background-radius: 8; -fx-padding: 10 14; -fx-font-size: 12px;");

        StackPane root = new StackPane(label);
        root.setPadding(new Insets(8));
        root.setPickOnBounds(false);
        StackPane.setAlignment(label, Pos.BOTTOM_RIGHT);

        Stage toast = new Stage();
        toast.initOwner(owner);
        toast.initStyle(StageStyle.TRANSPARENT);
        toast.initModality(Modality.NONE);
        toast.setAlwaysOnTop(true);
        toast.setResizable(false);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toast.setScene(scene);

        // position near bottom-right of owner
        double x = owner.getX() + owner.getWidth() - 300;
        double y = owner.getY() + owner.getHeight() - 120;
        toast.setX(x);
        toast.setY(y);
        toast.show();

        // fade in, wait, fade out, close
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), root);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();

        PauseTransition wait = new PauseTransition(Duration.millis(millis));
        wait.setOnFinished(ev -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), root);
            fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> toast.close());
            fadeOut.play();
        });
        wait.play();
    }
}
