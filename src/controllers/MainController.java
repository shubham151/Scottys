package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private TabPane mainTabPane;
    @FXML private Tab productTab;
    @FXML private Tab categoryTab;
    @FXML private Tab salesTab;

    @FXML
    public void initialize() {
        System.out.println("MainController initialized. Tabs are set up.");
        mainTabPane.setPrefWidth(Double.MAX_VALUE);
        mainTabPane.setPrefHeight(Double.MAX_VALUE);
    }
}
