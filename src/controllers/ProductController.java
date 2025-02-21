package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import utils.CSVImporter;
import java.io.File;

public class ProductController {

    @FXML private Button btnImport;

    @FXML
    public void initialize() {
        btnImport.setOnAction(e -> {
            System.out.println("Import Button Clicked");
            importCSV();
        });
    }

    private void importCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            CSVImporter.importCSV(selectedFile.getAbsolutePath());
        } else {
            System.out.println("No file selected.");
        }
    }
}
