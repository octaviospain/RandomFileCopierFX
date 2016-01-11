/**
 * Copyright 2016 Octavio Calleya
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.randomfilecopier.gui;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.controlsfx.control.CheckComboBox;

import com.randomfilecopier.RandomFileCopier;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * @author Octavio Calleya
 *
 */
public class RandomFileCopierFX extends Application {
	
	private final String[] EXTENSIONS = {".txt", ".xml", ".pdf", ".mp3", ".wav", ".flac", ".m4a", ".jpg", ".png", ".bmp",
			 							 ".avi", ".mpg",".java", ".c", ".cpp", ".py", ".html", ".css", ".js"};
	
	@FXML
	private Button openSourceBT, openDestinationBT, copyStopBT;
	@FXML
	private TextField sourceTF, destinationTF, maxTF;
	@FXML
	private TextArea logTA;
	@FXML
	private AnchorPane rootAP;
	@FXML
	private GridPane optionsGP;
	private HBox extensionsHBox;
	private CheckComboBox<String> extensionsCCB;
	
	private Stage primaryStage;
	private File source, destination;
	private ObservableList<String> extensionsList;
	private RandomFileCopierThread copyThread;
	private PrintStream textAreaPrinter;
	boolean sourceChanged, destinationChanged;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("/layout.fxml"));
		rootAP = (AnchorPane) loader.load();
		
		Scene scene = new Scene(rootAP, 700, 400);
		primaryStage.setMinHeight(450);
		primaryStage.setMinWidth(700);
		primaryStage.setTitle("Random File Copier FX");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	@FXML
	public void initialize() {
		textAreaPrinter = new PrintStream(new CustomOutputStream(logTA));
		extensionsList = FXCollections.observableArrayList(EXTENSIONS);
		extensionsCCB = new CheckComboBox<>(extensionsList);
		extensionsCCB.setPrefWidth(80);
		Label extensionsLabel = new Label("Only extensions:");
		extensionsLabel.setPadding(new Insets(0, 10, 0, 10));
		extensionsHBox = new HBox(extensionsLabel, extensionsCCB);
		extensionsHBox.setAlignment(Pos.CENTER);
		HBox.setHgrow(extensionsCCB, Priority.SOMETIMES);
		HBox.setMargin(extensionsCCB, new Insets(0, 10, 0, 10));
		optionsGP.add(extensionsHBox, 2, 0);
		openSourceBT.setOnMouseClicked(event -> {
			source = chooseDirectory();
			if(source != null)
				sourceTF.setText(source.toString());
		});
		openDestinationBT.setOnMouseClicked(event -> {
			destination = chooseDirectory();
			if(destination != null)
				destinationTF.setText(destination.toString());
		});
		sourceTF.textProperty().addListener(l -> {if(!sourceTF.getText().equals("")) sourceChanged = true;});
		sourceTF.focusedProperty().addListener(l -> {
			if(sourceChanged) {
				File enteredSource = new File(sourceTF.getText());
				if(!enteredSource.isDirectory()) {
					sourceTF.setText("");
					sourceChanged = false;
					showWarningDialog("Source directory doesn't exist or is not a directory");
				}
				else
					source = enteredSource;
			}
		});
		destinationTF.textProperty().addListener(l -> {if(!destinationTF.getText().equals("")) destinationChanged = true;});
		destinationTF.focusedProperty().addListener(l -> {
			if(destinationChanged) {
				File enteredDestination = new File(destinationTF.getText());
				if(!enteredDestination.isDirectory()) {
					destinationTF.setText("");
					destinationChanged = false;
					showWarningDialog("Target directory doesn't exist or is not a directory");
				}
				else
					destination = enteredDestination;
			}			
		});
		copyStopBT.disableProperty().bind(Bindings.createBooleanBinding(() -> {
			return !(new File(sourceTF.textProperty().get()).isDirectory() && new File(destinationTF.textProperty().get()).isDirectory());
		}, sourceTF.textProperty(), destinationTF.textProperty()));
		copyStopBT.setOnMouseClicked(event -> {
			if(copyStopBT.getText().equals("Copy!")) {			
				copy();
				copyStopBT.setText("Abort");
			}
			else {
				abort();
				copyStopBT.setText("Copy!");
			}
		});
		maxTF.addEventFilter(KeyEvent.KEY_TYPED, event -> {
			if(!event.getCharacter().matches("[0-9]"))
				event.consume();
		});
		maxTF.focusedProperty().addListener(l -> {
			if(!maxTF.isFocused())
				try {
					Integer.parseInt(maxTF.getText());
				} catch (NumberFormatException e) {
					maxTF.setText("0");
				}
		});
	}
	
	private void showWarningDialog(String message) {
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Warning");
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	private File chooseDirectory() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Choose folder");
		return chooser.showDialog(primaryStage);
	}
	
	private void copy() {
		ObservableList<String> selectedExtensions = extensionsCCB.getCheckModel().getCheckedItems();
		String[] stringExtensions = new String[selectedExtensions.size()];
		int i = 0;
		for(String s: selectedExtensions)
			stringExtensions[i++] = s;
		copyThread = new RandomFileCopierThread(source, destination, Integer.parseInt(maxTF.getText()), stringExtensions);
		copyThread.start();
	}
	
	private void abort() {
		copyThread.interrupt();
	}
	
	private class RandomFileCopierThread extends Thread {
		
		private RandomFileCopier copier;
		
		public RandomFileCopierThread(File src, File tgt, int max, String[] extensions) {
			copier = new RandomFileCopier(src.toString(), tgt.toString(), max, textAreaPrinter);
			copier.setFilterExtensions(extensions);
			copier.setVerbose(true);
		}
		
		@Override
		public void run() {
			try {
				copier.randomCopy();
			} catch (IOException e) {
				Platform.runLater(() -> {showWarningDialog("Source/target directory doesn't exist or is corrupt"); logTA.appendText("ERROR");});
			}
			Platform.runLater(() -> copyStopBT.setText("Copy!"));
		}
	}
	
	private class CustomOutputStream extends OutputStream {

		private TextArea textArea;
		
		public CustomOutputStream(TextArea textArea) {
			this.textArea = textArea;
		}
		
		@Override
		public void write(int b) throws IOException {
			Platform.runLater(() -> {
				textArea.appendText(String.valueOf((char)b));
			});
		}
	}
}