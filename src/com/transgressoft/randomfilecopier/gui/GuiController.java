/******************************************************************************
 * Copyright 2016, 2017 Octavio Calleya                                       *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 * http://www.apache.org/licenses/LICENSE-2.0                                 *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 ******************************************************************************/

package com.transgressoft.randomfilecopier.gui;

import com.transgressoft.randomfilecopier.*;
import javafx.application.*;
import javafx.beans.binding.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import org.controlsfx.control.*;

import java.io.*;

/**
 * @author Octavio Calleya
 * @version 0.2.2
 */
public class GuiController {

    private static final String[] EXTENSIONS = {".txt", ".xml", ".pdf", ".mp3", ".wav", ".flac", ".m4a", ".jpg", ".png",
                                                ".bmp", ".avi", ".mpg", ".java", ".c", ".cpp", ".py", ".html", ".css",
                                                ".js"};

    private static final String COPY_TEXT = "Copy!";
    private static final String ABORT_TEXT = "Abort";
    private static final String DIRECTORY_ERROR_TEXT = "Source/target directory doesn't exist or is corrupt";
    private static final String TARGET_WARNING_TEXT = "Target directory doesn't exist or is not a directory";
    private static final String SOURCE_WARNING_TEXT = "Source directory doesn't exist or is not a directory";

    @FXML
    private Button openSourceBT;
    @FXML
    private Button openDestinationBT;
    @FXML
    private Button copyStopBT;
    @FXML
    private TextField sourceTF;
    @FXML
    private TextField destinationTF;
    @FXML
    private TextField maxFilesTF;
    @FXML
    private TextField maxBytesTF;
    @FXML
    private TextArea logTA;
    @FXML
    private GridPane optionsGP;
    private HBox extensionsHBox;
    private CheckComboBox<String> extensionsCCB;

    private File source;
    private File destination;
    private ObservableList<String> extensionsList;
    private RandomFileCopierThread copyThread;
    private PrintStream textAreaPrinter;
    private boolean sourceChanged;
    private boolean destinationChanged;
    private DirectoryChooserHelper directoryChooserHelper;
    private RandomFileCopier copier;

    @FXML
    public void initialize() {
        textAreaPrinter = new PrintStream(new CustomOutputStream(logTA));
        addExtensionsCheckComboBox();
        setButtonActions();
        configureSourceTextField();
        configureDestinationTextField();
        configureMaxFilesTextField();
        configureMaxBytesTextField();
        setDirectoryChooserHelper(new DirectoryChooserHelper());
    }

    CheckComboBox<String> getExtensionsComboBox() {
        return extensionsCCB;
    }

    void setDirectoryChooserHelper(DirectoryChooserHelper directoryChooserHelper) {
        this.directoryChooserHelper = directoryChooserHelper;
    }

    private void addExtensionsCheckComboBox() {
        extensionsList = FXCollections.observableArrayList(EXTENSIONS);
        extensionsCCB = new CheckComboBox<>(extensionsList);
        extensionsCCB.setPrefWidth(90);

        Label extensionsLabel = new Label("Extensions:");
        extensionsLabel.setPadding(new Insets(0, 10, 0, 10));

        extensionsHBox = new HBox(extensionsLabel, extensionsCCB);
        extensionsHBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(extensionsCCB, Priority.SOMETIMES);
        HBox.setMargin(extensionsCCB, new Insets(0, 10, 0, 10));
        optionsGP.add(extensionsHBox, 2, 0);
    }

    private void setButtonActions() {
        openSourceBT.setOnMouseClicked(event -> {
            source = chooseDirectory();
            if (source != null)
                sourceTF.setText(source.getAbsolutePath());
        });
        openDestinationBT.setOnMouseClicked(event -> {
            destination = chooseDirectory();
            if (destination != null)
                destinationTF.setText(destination.getAbsolutePath());
        });

        copyStopBT.disableProperty().bind(copyStopDisableBinding());

        copyStopBT.setOnMouseClicked(event -> {
            if (copyStopBT.getText().equals(COPY_TEXT)) {
                copyStopBT.setText(ABORT_TEXT);
                copy();
            }
            else {
                copyStopBT.setText(COPY_TEXT);
                abort();
            }
        });
    }

    private BooleanBinding copyStopDisableBinding() {
        return Bindings.createBooleanBinding(
                () -> ! new File(sourceTF.textProperty().get()).isDirectory() ||
                        ! new File(destinationTF.textProperty().get()).isDirectory(),
                sourceTF.textProperty(),
                destinationTF.textProperty());
    }

    private void configureSourceTextField() {
        sourceTF.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null)
                sourceChanged = true;
        });
        sourceTF.focusedProperty().addListener(l -> {
            if (sourceChanged) {
                File enteredSource = new File(sourceTF.getText());
                if (! enteredSource.isDirectory()) {
                    sourceTF.clear();
                    sourceChanged = false;
                    showWarningDialog(SOURCE_WARNING_TEXT);
                }
                else
                    source = enteredSource;
            }
        });
    }

    private void configureDestinationTextField() {
        destinationTF.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null)
                destinationChanged = true;
        });
        destinationTF.focusedProperty().addListener(l -> {
            if (destinationChanged) {
                File enteredDestination = new File(destinationTF.getText());
                if (! enteredDestination.isDirectory()) {
                    destinationTF.clear();
                    destinationChanged = false;
                    maxBytesTF.setText(String.valueOf(0));
                    maxBytesTF.setText(Integer.toString(0));
                    showWarningDialog(TARGET_WARNING_TEXT);
                }
                else {
                    destination = enteredDestination;
                    maxBytesTF.setText(Long.toString(destination.getUsableSpace()));
                }
            }
        });
    }

    private void configureMaxFilesTextField() {
        maxFilesTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (! event.getCharacter().matches("[0-9]"))
                event.consume();
        });
        maxFilesTF.focusedProperty().addListener(l -> {
            if (! maxFilesTF.isFocused()) {
                try {
                    Integer.parseInt(maxFilesTF.getText());
                }
                catch (NumberFormatException e) {
                    maxFilesTF.setText("0");
                }
            }
        });
    }

    private void configureMaxBytesTextField() {
        maxBytesTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (! event.getCharacter().matches("[0-9]"))
                event.consume();
        });
        maxBytesTF.focusedProperty().addListener(l -> {
            if (! maxBytesTF.isFocused()) {
                try {
                    long enteredMaxBytes = Long.parseLong(maxBytesTF.getText());
                    if (destination != null && enteredMaxBytes > destination.getUsableSpace())
                        maxBytesTF.setText(String.valueOf(destination.getUsableSpace()));
                }
                catch (NumberFormatException e) {
                    maxBytesTF.setText(destination == null ? "0" : String.valueOf(destination.getUsableSpace()));
                }
            }
        });
    }

    protected File chooseDirectory() {
        return directoryChooserHelper.chooseDirectory();
    }

    private void copy() {
        int maxFiles = Integer.parseInt(maxFilesTF.getText());
        ObservableList<String> selectedExtensions = extensionsCCB.getCheckModel().getCheckedItems();
        String[] stringExtensions = selectedExtensions.stream().map(s -> s.substring(1)).toArray(String[]::new);

        copier = new RandomFileCopier(source.toPath(), destination.toPath(), maxFiles, textAreaPrinter);
        copier.setMaxBytesToCopy(Long.parseLong(maxBytesTF.getText()));
        copier.setFilterExtensions(stringExtensions);
        copier.setVerbose(true);
        copyThread = new RandomFileCopierThread();
        copyThread.start();
    }

    private void abort() {
        copyThread.interrupt();
    }

    private void showWarningDialog(String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private class RandomFileCopierThread extends Thread {

        @Override
        public void run() {
            try {
                copier.randomCopy();
            }
            catch (IOException exception) {
                Platform.runLater(() -> {
                    showWarningDialog(DIRECTORY_ERROR_TEXT);
                    logTA.appendText("ERROR: " + exception.getMessage());
                });
            }
            Platform.runLater(() -> copyStopBT.setText(COPY_TEXT));
        }
    }

    private class CustomOutputStream extends OutputStream {

        private TextArea textArea;

        public CustomOutputStream(TextArea textArea) {
            super();
            this.textArea = textArea;
        }

        @Override
        public void write(int b) throws IOException {
            Platform.runLater(() -> textArea.appendText(String.valueOf((char) b)));
        }
    }

    protected class DirectoryChooserHelper {

        protected File chooseDirectory() {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose folder");
            return chooser.showDialog(logTA.getScene().getWindow());
        }
    }
}