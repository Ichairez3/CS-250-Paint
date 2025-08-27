import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import javafx.scene.layout.StackPane;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;


public class PaintFX {
    private MenuBar menuBar;
    private VBox root;
    private ImageView imageView;
    private File currentFile;

    public PaintFX(Stage stage){
        menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save");
        MenuItem saveAsItem = new MenuItem("Save As");
        MenuItem exitItem = new MenuItem("Exit");
        MenuItem openItem = new MenuItem("Open Image");
        fileMenu.getItems().addAll(saveItem, saveAsItem, exitItem, openItem);

        menuBar.getMenus().addAll(fileMenu);

        root = new VBox(menuBar);

        exitItem.setOnAction(e -> System.exit(0));

        //image selection
        imageView = new ImageView();
        root.getChildren().add(imageView);

        openItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Image File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"));
            File selectedFile = fileChooser.showOpenDialog(stage);

            StackPane imageContainer = new StackPane(imageView);
            root.getChildren().add(imageContainer);

            if (selectedFile != null) {
                currentFile = selectedFile;
                Image img = new Image(selectedFile.toURI().toString());
                imageView.setImage(img);
                imageView.setFitWidth(600);
                imageView.setPreserveRatio(true);
            }
        });

        //save funcion
        saveItem.setOnAction(e -> {
            if (currentFile != null) {
                saveImage(currentFile);
            } else {
                saveAs(stage); // if no file yet, fallback to Save As
            }
        });


        //save as function
        saveAsItem.setOnAction(e -> saveAs(stage));
    }
    private void saveImage(File file) {
        try {
            WritableImage writableImage = imageView.snapshot(null, null);

            // figure out format from file extension
            String name = file.getName().toLowerCase();
            String format = "png"; // default
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                format = "jpg";
            } else if (name.endsWith(".gif")) {
                format = "gif";
            }

            ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), format, file);

            System.out.println("Saved image to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void saveAs(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image As");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"));

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getAbsolutePath() + ".png");
            }
            currentFile = file;
            saveImage(file);
        }
    }


    public VBox getRoot() {
        return root;
    }


}
