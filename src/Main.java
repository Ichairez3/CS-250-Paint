    import javafx.application.Application;
    import javafx.stage.Stage;
    import javafx.scene.Scene;
    public class Main extends Application{
        @Override
        public void start(Stage primaryStage) {
            PaintFX paintFX = new PaintFX(primaryStage);

            Scene scene = new Scene(paintFX.getRoot(), 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("PaintFX");
            primaryStage.show();

        }
        public static void main(String[] args) {
            launch(args);
        }
    }
