import database.DatabaseHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        DatabaseHelper.initializeDatabase();
        DatabaseHelper.createTables();

        URL fxmlUrl = getClass().getResource("/views/ProductView.fxml");
        if (fxmlUrl == null) {
            throw new RuntimeException("Cannot find ProductView.fxml in /views/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        TabPane root = loader.load();
        
        Scene scene = new Scene(root, 1280, 720);

        primaryStage.setTitle("Scottys - Analytics");
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
