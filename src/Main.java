
import database.DatabaseHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        DatabaseHelper.initializeDatabase();
        DatabaseHelper.createTable();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProductView.fxml"));
        AnchorPane root = loader.load();

        Scene scene = new Scene(root);
        primaryStage.setTitle("Scottys - Analytics");

        // Set full screen
//        primaryStage.setFullScreen(true);
        primaryStage.setMaximized(true);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
