package com.test;

import com.test.datatypes.Inventory;
import com.test.datatypes.Kind;
import com.test.datatypes.Material;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MainApp extends Application {

    private static final String INVENTORY_DIR = "saved/inventories";

    private  final ComboBox<String> inventorySelector = new ComboBox<>();
    private final Button createInventoryButton = new Button("New Inventory");
    private final Button deleteInventoryButton = new Button("Delete Inventory");

    private final TextField nameField = new TextField();
    private final ComboBox<Kind> typeCombo = new ComboBox<>(FXCollections.observableArrayList(Kind.values()));
    private final TextField quantityField = new TextField();
    private final Button addButton = new Button("Add Equipment");
    private final Button saveButton = new Button("Save Current Inventory");

    private final TextField removeQuantityField = new TextField();
    private final Button removeButton = new Button("Remove Equipment");

    private final ListView<Material> equipmentList = new ListView<>();
    private final ObservableList<Material> equipmentItems = FXCollections.observableArrayList();

    private final Map<String, Inventory> inventories = new HashMap<>();
    private Inventory currentInventory;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        loadInventories();

        inventorySelector.setPrefWidth(200);
        inventorySelector.setPromptText("Select Inventory");
        inventorySelector.getItems().addAll(inventories.keySet());
        if (!inventories.isEmpty()) {
            inventorySelector.getSelectionModel().selectFirst();
            currentInventory = inventories.get(inventorySelector.getValue());
            refreshEquipmentList();
        }

        inventorySelector.setOnAction(e -> {
            String selected = inventorySelector.getValue();
            currentInventory = inventories.get(selected);
            refreshEquipmentList();
        });

        createInventoryButton.setOnAction(e -> createNewInventory());
        deleteInventoryButton.setOnAction(e -> deleteCurrentInventory());

        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(10);
        inputGrid.setVgap(10);
        inputGrid.addRow(0, new Label("Name:"), nameField);
        inputGrid.addRow(1, new Label("Type:"), typeCombo);
        inputGrid.addRow(2, new Label("Quantity:"), quantityField);

        HBox actionButtons = new HBox(10, addButton, saveButton);
        actionButtons.setPadding(new Insets(10, 0, 10, 0));

        HBox removeBox = new HBox(10, new Label("Remove Quantity:"), removeQuantityField, removeButton);
        removeBox.setPadding(new Insets(0, 0, 10, 0));
        removeBox.setAlignment(Pos.CENTER_LEFT);

        equipmentList.setItems(equipmentItems);
        equipmentList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Material item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int qty = currentInventory.getMaterialCount(item);
                    setText(String.format("%s x%d", item.toString(), qty));
                    switch (item.getMaterialKind()) {
                        case MEDICAL -> setTextFill(Color.RED);
                        case STAFF_EPI -> setTextFill(Color.BLUE);
                        case CLIENT_EPI -> setTextFill(Color.GREEN);
                        case MAINTENANCE -> setTextFill(Color.ORANGE);
                        case OTHER -> setTextFill(Color.GRAY);
                        default -> setTextFill(Color.BLACK);
                    }
                }
            }
        });

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.getChildren().addAll(inventorySelector, createInventoryButton, deleteInventoryButton);

        VBox mainContent = new VBox(10, inputGrid, actionButtons, removeBox, equipmentList);
        mainContent.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(mainContent);

        addButton.setOnAction(e -> addEquipmentToCurrentInventory());
        saveButton.setOnAction(e -> saveCurrentInventory());
        removeButton.setOnAction(e -> removeEquipmentFromCurrentInventory());

        primaryStage.setScene(new Scene(root, 600, 550));
        primaryStage.setTitle("Equipment Manager with Multiple Inventories");
        primaryStage.show();
    }

    private void addEquipmentToCurrentInventory() {
        if (currentInventory == null) {
            showAlert("No Inventory Selected", "Please select or create an inventory first.");
            return;
        }

        String name = nameField.getText().trim();
        Kind kind = typeCombo.getValue();
        int quantity = Integer.valueOf(quantityField.getText().trim());

        if (name.isEmpty()) {
            showAlert("Invalid Input", "Name cannot be empty.");
            return;
        }
        if (kind == null) {
            showAlert("Invalid Input", "Please select a type.");
            return;
        }

        int materialId = Objects.hash(name, kind);
        Material material = new Material(name, materialId, kind);
        currentInventory.addMaterial(material, quantity);

        refreshEquipmentList();

        nameField.clear();
        typeCombo.getSelectionModel().clearSelection();
        quantityField.clear();
    }

    private void removeEquipmentFromCurrentInventory() {
        if (currentInventory == null) {
            showAlert("No Inventory Selected", "Please select or create an inventory first.");
            return;
        }

        Material selected = equipmentList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select an equipment item to remove.");
            return;
        }

        int removeQty = Integer.valueOf(removeQuantityField.getText().trim());
        int currentQty = currentInventory.getMaterialCount(selected);

        if (removeQty <= 0) {
            showAlert("Invalid Quantity", "Remove quantity must be at least 1.");
            return;
        }

        if (removeQty > currentQty) {
            showAlert("Invalid Quantity", "Remove quantity exceeds current quantity (" + currentQty + ").");
            return;
        }

        int newQty = currentQty - removeQty;
        if (newQty > 0) {
            currentInventory.updateMaterialQuantity(selected, newQty);
        } else {
            currentInventory.removeMaterial(selected);
        }

        refreshEquipmentList();
    }

    private void refreshEquipmentList() {
        equipmentItems.clear();
        if (currentInventory == null) return;
        equipmentItems.addAll(currentInventory.getMaterials().keySet());
    }

    private void saveCurrentInventory() {
        if (currentInventory == null) {
            showAlert("No Inventory Selected", "Please select or create an inventory first.");
            return;
        }

        try {
            Files.createDirectories(Paths.get(INVENTORY_DIR));
            String path = INVENTORY_DIR + File.separator + currentInventory.getName() + ".json";
            currentInventory.saveToFile(path);
            showAlert("Success", "Inventory saved successfully.");
        } catch (IOException e) {
            showAlert("Error", "Failed to save inventory: " + e.getMessage());
        }
    }

    private void loadInventories() {
        inventories.clear();
        File dir = new File(INVENTORY_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File file : files) {
            try {
                Inventory inv = Inventory.loadFromFile(file.getPath());
                inventories.put(inv.getName(), inv);
            } catch (IOException e) {
                System.err.println("Failed to load inventory from " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void createNewInventory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create New Inventory");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter inventory name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showAlert("Invalid Name", "Inventory name cannot be empty.");
            } else if (inventories.containsKey(name)) {
                showAlert("Duplicate Name", "An inventory with this name already exists.");
            } else {
                Inventory newInv = new Inventory(name.trim());
                inventories.put(name.trim(), newInv);
                inventorySelector.getItems().add(name.trim());
                inventorySelector.getSelectionModel().select(name.trim());
                currentInventory = newInv;
                refreshEquipmentList();
            }
        });
    }

    private void deleteCurrentInventory() {
        if (currentInventory == null) {
            showAlert("No Inventory Selected", "Please select an inventory to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Inventory");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete inventory '" + currentInventory.getName() + "'?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            File file = new File(INVENTORY_DIR + File.separator + currentInventory.getName() + ".json");
            if (file.exists() && !file.delete()) {
                showAlert("Error", "Failed to delete inventory file.");
                return;
            }

            inventories.remove(currentInventory.getName());
            inventorySelector.getItems().remove(currentInventory.getName());

            if (!inventories.isEmpty()) {
                String first = inventorySelector.getItems().get(0);
                inventorySelector.getSelectionModel().select(first);
                currentInventory = inventories.get(first);
            } else {
                currentInventory = null;
                equipmentItems.clear();
                inventorySelector.getSelectionModel().clearSelection();
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
