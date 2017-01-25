package com.andrewlee.filesorter;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.scene.control.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.filechooser.*;
import org.json.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.beans.value.*;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.Alert.*;


public class FileSorter extends Application {
    private TreeMap<String, TreeSet<String>> fileCategories = new TreeMap<String, TreeSet<String>>();
    private TreeMap<String, TreeSet<String>> fileCategoriesAll = new TreeMap<String, TreeSet<String>>();
	
    private String moveFromPath;
    private String moveToPath;
    private ScrollPane fileCategorySelector;
    private VBox rightPane;
    
    public void execute() {
        File workDirectory = new File(moveFromPath);
        File[] allFiles = workDirectory.listFiles();

        for(int i = 0; i < allFiles.length; i++) {			            
            String currentFileExtension = getFileExtension(allFiles[i].getName());
            System.out.println(allFiles[i].getName() + " " + currentFileExtension);
            // Iterate all categories
            for(Map.Entry<String, TreeSet<String>> entry: fileCategories.entrySet()) {
                // Check if the current extension belongs to this category
                if(entry.getValue().contains(currentFileExtension)) {
                    // Prepare the file and folder for moving
                    File folder = new File(moveToPath + "\\" + entry.getKey());
                    folder.mkdir();
                    Path source = allFiles[i].toPath();
                    Path destination = folder.toPath();
                    destination = destination.resolve(allFiles[i].getName());
                    try {
                            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
                    }
                    catch(IOException e) {
                            System.out.println("Can't move the file");
                            return;
                    }
                    break;
                }
            }
        }		
    }

    private String getFileExtension(String name) {
            String result = "";
            for(int i = name.length() - 1; i >= 0; i--) {
                    // The the rightmost occurence of '.' is considered the beginning of extension
                    if(name.charAt(i) == '.') {
                            result = name.substring(i).toLowerCase();
                            break;
                    }
            }
            return result;
    }
    
    @Override
    public void start(Stage primaryStage) {        
        initializeSettings();     
        
        SplitPane mainPane = new SplitPane();
        mainPane.setOrientation(Orientation.HORIZONTAL);
        mainPane.getItems().addAll(addDirectorySelectorGrid(), rightPane = addRightPane());
        mainPane.setDividerPosition(0, 0.45);
        Scene scene = new Scene(mainPane, 1000, 600);
        
        primaryStage.setTitle("File Sorter");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }   
    
    /**
     * The FileCategorySelector is a scroll pane that handles all possible 
     * settings related to file categories, such as adding, removing and editing. 
     * 
     * fileCategories should contain only the categories that are being 'sorted'.
     * fileCategoeiesAll should contain all the categories.
     * (not the most efficient way, but should not be crucial for a reasonable 
     * number of categories)
     * 
     * This function is supposed to maintain the above maps up to date.
     *
     * @return Returns the ScrollPane that manages the file categories
     */
    private ScrollPane addFileCategorySelector() {
        // The core of this is a grid pane. 
        // The first column corresponds to the name of a category
        // The second column corresponds to the extensions of a category
        // The third column determines if this category will be considered in the sorting process
        // The top row is labels
        // The bottom row is always empty text field used for creating new categories
        
        // Set up the grid pane settings
        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new Insets(10, 10, 10, 10));
        gp.setMaxWidth(Double.MAX_VALUE);
        gp.setPrefWidth(Double.MAX_VALUE);
        // Create the top row
        Label nameButton = new Label("Name");
        Label extButton = new Label("Extensions");
        nameButton.setMaxWidth(Double.MAX_VALUE);
        nameButton.setPrefWidth(2000);
        extButton.setMaxWidth(Double.MAX_VALUE);
        extButton.setPrefWidth(2000);
        Label checkButton = new Label("?");
        gp.add(nameButton, 0, 0);
        gp.add(extButton, 1, 0);
        gp.add(checkButton, 2, 0);
        
        // Create the rows corresponding to existing categories
        int currentRow = 1;
        for(Map.Entry<String, TreeSet<String>> entry: fileCategoriesAll.entrySet()) {
            // Create the text field for the first column
            TextField nameField = new TextField(entry.getKey());
            // Construct the string for the extension column, then create the field
            String ext = "";
            for(String s: entry.getValue()) {
                ext += s + ", ";
            }
            ext = ext.substring(0, ext.length() - 2);            
            TextField extField = new TextField(ext);
            
            // Create the checkbox
            CheckBox cb = new CheckBox();
            if(fileCategories.containsKey(entry.getKey())) 
                cb.setSelected(true);
            else
                cb.setSelected(false);            
            
            // Whenever this name field goes out of focus - process the new value appropriately
            nameField.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override 
                public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                    if(!newPropertyValue) {                        
                        // Went out of focus
                        if(nameField.getText().isEmpty()) {
                            // The entry is invalidated - delete it
                            fileCategoriesAll.remove(entry.getKey());
                            fileCategories.remove(entry.getKey());                    
                        }
                        else {
                            // The entry is renamed - replace it
                            if(fileCategories.containsKey(entry.getKey())) {                                
                                fileCategories.remove(entry.getKey());
                                fileCategories.put(nameField.getText(), entry.getValue());
                            }
                            fileCategoriesAll.remove(entry.getKey());
                            fileCategoriesAll.put(nameField.getText(), entry.getValue());
                        }         
                        // Update the entire pane
                        updateFileCategories();        
                    }
                }
            });
            
            // Whenever this extension field goes out of focus - process the new value appropriately
            extField.focusedProperty().addListener(new ChangeListener<Boolean>() {
                @Override 
                public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                    if(!newPropertyValue) {    
                        // Went out of focus
                        if(extField.getText().isEmpty()) {
                            // The entry is invalidated - delete it
                            fileCategoriesAll.remove(entry.getKey());
                            fileCategories.remove(entry.getKey());                    
                        }
                        else {                            
                            String[] extensions = splitExtensionString(extField.getText());
                            if(extensions.length != 0) {
                                // New string is valid, replace it
                                TreeSet<String> ts = new TreeSet<String>();
                                for(String s: extensions)
                                    ts.add(s);
                                fileCategoriesAll.remove(entry.getKey());
                                if(fileCategories.containsKey(entry.getKey())) {
                                    fileCategories.remove(entry.getKey());
                                    fileCategories.put(entry.getKey(), ts);
                                }
                                fileCategoriesAll.put(entry.getKey(), ts);
                            }
                        }                            
                        updateFileCategories();        
                    }
                }
            });
            
            // Whenever the checkbox changes state, remove/add it to fileCategories
            cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override 
                public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                    if(newPropertyValue) {
                        // The new value is 'checked', add the corresponding category to fileCategories                        
                        fileCategories.put(entry.getKey(), fileCategoriesAll.get(entry.getKey()));
                    }
                    else {
                        // Otherwise delete it from fileCategories
                        fileCategories.remove(entry.getKey());
                    }
                }
            });
            
            // Add the elements to the grid
            gp.add(nameField, 0, currentRow);
            gp.add(extField, 1, currentRow);
            gp.add(cb, 2, currentRow);
            currentRow++;
        }
        
        // Those are the empty text field for creating
        TextField addNewName = new TextField();
        TextField addNewExt = new TextField();        
        
        // Whenever the empty fields at the bottom go out of focus,
        // try to add a new category.
        addNewExt.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override 
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if(!newPropertyValue && !addNewName.getText().isEmpty()) {      
                    // Went out of focus && the name is not empty
                    String name = addNewName.getText(), extensions = addNewExt.getText();
                    String[] extArray = splitExtensionString(addNewExt.getText());
                    if(extArray.length == 0) // The string is invalid, just exit
                        return;
                    if(!fileCategoriesAll.containsKey(name)) {
                        // If a category with this name does not exist, create it
                        fileCategoriesAll.put(name, new TreeSet<String>());
                        fileCategories.put(name, new TreeSet<String>());
                    }                        
                    for(String e: extArray) {
                        // Add each extension to the maps
                        if(e.isEmpty())
                            continue;
                        fileCategoriesAll.get(name).add(e);
                        fileCategories.get(name).add(e);
                    }
                    updateFileCategories();
                }
            }
        });
        addNewName.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override 
            public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
                if(!newPropertyValue && !addNewName.getText().isEmpty()) {      
                    // Went out of focus && the name is not empty
                    String name = addNewName.getText(), extensions = addNewExt.getText();
                    String[] extArray = splitExtensionString(addNewExt.getText());
                    if(extArray.length == 0) // The extension string is invalid, just exit
                        return;
                    if(!fileCategoriesAll.containsKey(name)) {
                        // If a category with this name does not exist, create it
                        fileCategoriesAll.put(name, new TreeSet<String>());
                        fileCategories.put(name, new TreeSet<String>());
                    }                        
                    for(String e: extArray) {
                        // Add each extension to the maps
                        if(e.isEmpty())
                            continue;
                        fileCategoriesAll.get(name).add(e);
                        fileCategories.get(name).add(e);
                    }
                    updateFileCategories();
                }
            }
        });
        
        // Add the last row to the grid
        gp.add(addNewName, 0, currentRow);
        gp.add(addNewExt, 1, currentRow);
        
        // Put the grid inside a scroll pane
        ScrollPane sp = new ScrollPane();       
        sp.setMaxHeight(Double.MAX_VALUE);
        sp.setPrefHeight(2000);
        sp.setFitToWidth(true);
        sp.setContent(gp);
        return sp;
    }    
    
    /**
     * Returns the entire right side of the window.
     * Contains the FileCategorySelector pane, Save settings and Run buttons.
     * @return VBox containing the right pane.
     */
    private VBox addRightPane() {                
        VBox vbox = new VBox(8); // The core VBox
        vbox.setPadding(new Insets(0, 0, 10, 0));
        // Buttons are arranged in an anchor pane attached to lower left/right 
        // corners
        AnchorPane buttonPane = new AnchorPane();
        Button runButton = new Button("Run");
        Button saveButton = new Button("Save");
        runButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Run Sorting");
                alert.setHeaderText("Run Sorting");
                alert.setContentText("Are you sure you want to start sorting?");
                ButtonType yesButton = new ButtonType("Yes");
                ButtonType noButton = new ButtonType("No");
                alert.getButtonTypes().setAll(yesButton, noButton);
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == yesButton) {
                    // user chose ok -> save settings
                    execute();
                }
            }
        }); 
        saveButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Alert alert = new Alert(AlertType.CONFIRMATION);
                alert.setTitle("Save Settings");
                alert.setHeaderText("Save Current Settings");
                alert.setContentText("Are you sure you want to save the current settings as default?");
                ButtonType yesButton = new ButtonType("Yes");
                ButtonType noButton = new ButtonType("No");
                alert.getButtonTypes().setAll(yesButton, noButton);
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == yesButton) {
                    // user chose ok -> save settings
                    saveSettings();
                }
            }
        }); 
        AnchorPane.setRightAnchor(runButton, 10.);
        AnchorPane.setTopAnchor(runButton, 10.);
        AnchorPane.setTopAnchor(saveButton, 10.);
        AnchorPane.setLeftAnchor(saveButton, 10.);
        buttonPane.getChildren().addAll(runButton, saveButton);  
        // Add FileCategorySelector and buttons to the VBox
        fileCategorySelector = addFileCategorySelector();
        vbox.getChildren().add(fileCategorySelector);
        vbox.getChildren().add(buttonPane);
        return vbox;
    }
    
    /**
     * Returns the entire left side of the window.
     * Includes the means of selecting the working directories and a text area with
     * the instructions on how to use the app.
     * @return GridPane containing the left pane
     */
    private GridPane addDirectorySelectorGrid() {
        // Set up the core pane
        GridPane gp = new GridPane();
        gp.setHgap(10);
        gp.setVgap(10);
        gp.setPadding(new Insets(10, 10, 10, 10));
        
        // Create and add text labels
        Text moveFromText = new Text("Move from:");
        Text moveToText = new Text("Move to:");
        gp.add(moveFromText, 0, 0);
        gp.add(moveToText, 0, 1);
        
        // Create and setup the current path indicators
        TextField moveFromTextField = new TextField(moveFromPath);
        moveFromTextField.setFocusTraversable(false);
        moveFromTextField.setEditable(false);
        moveFromTextField.setMaxWidth(Double.MAX_VALUE);
        moveFromTextField.setPrefWidth(2000);
        TextField moveToTextField = new TextField(moveToPath);
        moveToTextField.setFocusTraversable(false);
        moveToTextField.setEditable(false);
        moveToTextField.setMaxWidth(Double.MAX_VALUE);
        moveToTextField.setPrefWidth(2000);
        gp.add(moveFromTextField, 1, 0);
        gp.add(moveToTextField, 1, 1);
        
        // Create the buttons that should trigger the browse directory dialog
        Button moveFromBtn = new Button("...");
        Button moveToBtn = new Button("...");
        moveFromBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setInitialDirectory(new File(moveFromPath));
                File selectedDir = dc.showDialog(moveFromBtn.getScene().getWindow());
                if(selectedDir != null) {
                    moveFromPath = selectedDir.getAbsolutePath();
                    gp.getChildren().remove(moveFromTextField);
                    moveFromTextField.setText(moveFromPath);
                    gp.add(moveFromTextField, 1, 0);
                }
            }
        }); 
        moveToBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                DirectoryChooser dc = new DirectoryChooser();
                dc.setInitialDirectory(new File(moveToPath));
                File selectedDir = dc.showDialog(moveToBtn.getScene().getWindow());
                if(selectedDir != null) {
                    moveToPath = selectedDir.getAbsolutePath();
                    gp.getChildren().remove(moveToTextField);
                    moveToTextField.setText(moveToPath);
                    gp.add(moveToTextField, 1, 1);
                }
            }
        });
        gp.add(moveFromBtn, 2, 0);
        gp.add(moveToBtn, 2, 1);
        
        // The instruction manual area
        TextArea help = new TextArea();
        help.setWrapText(true);
        help.setText(
             "File Sorter separates files from a directory into different categories. Each file from the 'Move from' directory will be moved to 'Move to' directory and put inside a folder with the corresponding category name.\n" +
"\n" +
"The categories can be defined on the right. Uncheck a category if you do not want it to be present in sorting. When specifying extensions make sure they are in the following format: \".ext1, .ext2, .ext3\" (no quotes). You can remove existing categories by fully erasing its name/extension string. Editing is also possible provided the extension string is valid."
        );
        help.setEditable(false);
        help.setMaxWidth(Double.MAX_VALUE);
        help.setMaxHeight(Double.MAX_VALUE);
        help.setPrefWidth(Double.MAX_VALUE);
        help.setPrefHeight(Double.MAX_VALUE);
        gp.add(help, 0, 2, 3, 3);
        
        gp.setPrefHeight(Double.MAX_VALUE); // Make sure the pane expands downwards
        SplitPane.setResizableWithParent(gp, Boolean.FALSE); // This pane should not resize with the window
        
        return gp;
    }
    
    /**
     * Handles the settings and makes sets up all fields with appropriate values
     */
    private void initializeSettings() {
        moveFromPath = FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath();
        moveToPath = moveFromPath;
        File settingsFile = new File("settings.json");
        if(!settingsFile.exists()) {
            try {
                settingsFile.createNewFile();
            }
            catch(IOException e) {
                System.out.println("Error while creating the settings file");
            }            
        }        
        JSONObject jsonObject = readSettingsFile();   
        
        if(jsonObject == null)
            jsonObject = new JSONObject();
        
        Boolean changed = false;
        // Check if settings specify moveFromPath
        try {
            String temp = jsonObject.getString("moveFromPath");
            moveFromPath = temp;
        }
        catch(JSONException e ) {
            changed = true;
            try {
                jsonObject.put("moveFromPath", moveFromPath);
            }
            catch(JSONException je) {
                System.out.println("Error while putting moveFromPath in json");
            }
        }
        // Check if settings specify moveToPath
        try {
            String temp = jsonObject.getString("moveToPath");
            moveToPath = temp;
        }
        catch(JSONException e) {
            changed = true;
            try {
                jsonObject.put("moveToPath", moveToPath);
            }
            catch(JSONException je) {
                System.out.println("Error while putting moveToPath in json");
            }
        }
        
        /*
         * Each file type is a class that consists of the following fields:
         * - 'name': name of the file type (incidentally this file type's folder name)
         * - 'extensions': array of extensions ('.extension1', '.extension2', ...)
         * - 'sorted': indicates whether the files of this type should be moved.
         * 
         * The following code iterates through all file types and adds the ones
         * with sorted=1 to the tree map of file categories.
         */
        try {
            JSONArray fileTypesArray = jsonObject.getJSONArray("fileTypes");
            for(int i = 0; i < fileTypesArray.length(); i++) {
                try {
                    JSONObject currentFileType = fileTypesArray.getJSONObject(i);
                    JSONArray currentExtensions = currentFileType.getJSONArray("extensions");
                    String name = currentFileType.getString("name");
                    if(currentFileType.getString("sorted").equals("1")) {
                        for(int j = 0; j < currentExtensions.length(); j++) {
                            if(!fileCategories.containsKey(name))
                                fileCategories.put(name, new TreeSet<String>());
                            fileCategories.get(name).add(currentExtensions.getString(j).toLowerCase());
                        }
                    }
                    for(int j = 0; j < currentExtensions.length(); j++) {
                        if(!fileCategoriesAll.containsKey(name)) 
                            fileCategoriesAll.put(name, new TreeSet<String>());
                        fileCategoriesAll.get(name).add(currentExtensions.getString(j).toLowerCase());
                    }
                }
                catch(JSONException je) {
                    System.out.println(je.getMessage());
                }
            }
        }
        catch(JSONException e) {
            System.out.println(e.getMessage());
        }
        
        if(changed)
            writeSettingsFile(jsonObject);
    }
    
    /**
     * Saves current settings to settings.json
     */
    private void saveSettings() {
        JSONObject result = new JSONObject();
        try 
        {
            result.put("moveToPath", moveToPath);
            result.put("moveFromPath", moveFromPath);
            JSONArray types = new JSONArray();
            for(Map.Entry<String, TreeSet<String>> entry: fileCategoriesAll.entrySet()) {
                JSONObject currentType = new JSONObject();
                currentType.put("name", entry.getKey());
                JSONArray extensions = new JSONArray();
                for(String s: entry.getValue())
                    extensions.put(s);
                currentType.put("extensions", extensions);
                if(fileCategories.containsKey(entry.getKey()))
                    currentType.put("sorted", "1");
                else
                    currentType.put("sorted", "0");
                types.put(currentType);
            }
            result.put("fileTypes", types);
            writeSettingsFile(result);
        }
        catch(JSONException e) {
            System.out.println("I'm supposed to be saving settings but this one exception keeps kicking my ass!");
        }
    }
    
    /**
     * Writes the jsonObject passed into settings.json
     * @param jsonObject the object to write
     */
    private void writeSettingsFile(JSONObject jsonObject) {
        try {
            String text = jsonObject.toString();
            File settingsFile = new File("settings.json");
            FileWriter fileWriter = new FileWriter(settingsFile);
            fileWriter.write(text);
            fileWriter.flush();
            fileWriter.close();
        }
        catch(Exception e) {
            System.out.println("Error while writing the settings file");
        }
    }
    
    /** 
     * Returns a JSONObject read from settings.json
     * @return JSONObject from settings.json
     */
    private JSONObject readSettingsFile() {
        try {
            String text = new String(Files.readAllBytes(Paths.get("settings.json")));
            JSONObject jsonObject = new JSONObject(text);
            return jsonObject;
        }
        catch(Exception e) {
            System.out.println("Error while opening the settings file");
            return null;
        }
    }
    
    /**
     * 
     * @param ext Raw string with extensions
     * @return Tokenized array of strings with extenstion if ext is valid, otherwise an empty array
     */
    private String[] splitExtensionString(String ext) {
        ext = ext.replaceAll("\\s", "");
        ext = ext.toLowerCase();
        String[] array = ext.split(",");
        for(String s: array) {
            if(s.isEmpty() || s.charAt(0) != '.' || s.substring(1).matches("^.*[^a-zA-Z0-9 ].*$"))
                return new String[0];            
        }
        return array;
    }
    
    /**
     * Updates the right pane according to fileCategoriesAll and fileCategories.
     */
    private void updateFileCategories() {        
        rightPane.getChildren().remove(fileCategorySelector);
        fileCategorySelector = addFileCategorySelector();
        rightPane.getChildren().add(0, fileCategorySelector);
    }            
}
