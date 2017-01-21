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

import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/**
 * Graphic user interface class.
 *
 * @author Octavio Calleya
 * @version 0.2.1
 */
public class RandomFileCopierFx extends Application {

	private GuiController controller;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/layout.fxml"));
		AnchorPane rootAP = loader.load();
		controller = loader.getController();

		Scene scene = new Scene(rootAP, 700, 400);
		primaryStage.setMinHeight(450);
		primaryStage.setMinWidth(700);
		primaryStage.setTitle("Random File Copier FX");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	protected GuiController getController() {
		return controller;
	}
}
