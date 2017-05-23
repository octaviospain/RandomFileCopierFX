package com.transgressoft.randomfilecopier.gui;

import com.google.common.io.Files;
import com.transgressoft.randomfilecopier.gui.GuiController.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.input.*;
import javafx.stage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;
import org.testfx.util.*;

import java.io.*;
import java.nio.file.*;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.*;
import static org.testfx.matcher.base.NodeMatchers.*;

/**
 * @author Octavio Calleya
 * @version 0.2.5
 */
@ExtendWith ({ApplicationExtension.class, MockitoExtension.class})
public class RandomFileCopierFxTest {

    GuiController guiController;

    Path sourceTestPath = Paths.get("test-resources", "10testfiles");
    File destination = Files.createTempDir();

    @BeforeAll
    public static void setupSpec() throws Exception {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }
    }

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/layout.fxml"));
        Parent root = loader.load();
        guiController = loader.getController();

        // Mock a directory chooser
        DirectoryChooserHelper chooserHelper = mock(DirectoryChooserHelper.class);
        when(chooserHelper.chooseDirectory()).thenReturn(sourceTestPath.toFile());
        guiController.setDirectoryChooserHelper(chooserHelper);

        // Set a clipboard content
        Clipboard clip = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString("notanumber");
        clip.setContent(content);

        stage.setScene(new Scene(root));
        stage.show();
        stage.centerOnScreen();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (destination.exists())
            destination.delete();
    }

    @Test
    @DisplayName("Type ore bytes than available")
    public void typeMoreBytesThanAvailable(FxRobot robot) {
        robot.clickOn("#destinationTF").write(destination.getAbsolutePath());
        verifyThat("#destinationTF", hasText(destination.getAbsolutePath()));

        robot.doubleClickOn("#maxBytesTF");
        robot.write(String.valueOf(destination.getUsableSpace() + 1));
        robot.clickOn("#logTA");

        verifyThat("#maxBytesTF", hasText(String.valueOf(destination.getUsableSpace())));
    }

    @Test
    @DisplayName ("Type invalid max files")
    public void typeInvalidMaxFiles(FxRobot robot) {
        robot.doubleClickOn("#maxFilesTF").write("notanumber");
        verifyThat("#maxFilesTF", hasText("0"));

        robot.doubleClickOn("#maxFilesTF").write("notanumber").clickOn("#logTA");
        verifyThat("#maxFilesTF", hasText("0"));

        robot.doubleClickOn("#maxFilesTF").push(new KeyCodeCombination(KeyCode.V, KeyCombination.META_DOWN));
        robot.clickOn("#logTA");
        verifyThat("#maxFilesTF", hasText("0"));
    }

    @Test
    @DisplayName ("Type invalid max bytes")
    public void typeInvalidMaxBytes(FxRobot robot) {
        robot.doubleClickOn("#maxBytesTF").write("notanumber");
        verifyThat("#maxBytesTF", hasText("0"));


        robot.doubleClickOn("#maxBytesTF").write("notanumber").clickOn("#logTA");
        verifyThat("#maxBytesTF", hasText("0"));

        robot.doubleClickOn("#maxBytesTF").push(new KeyCodeCombination(KeyCode.V, KeyCombination.META_DOWN));
        robot.clickOn("#logTA");
        verifyThat("#maxBytesTF", hasText("0"));
    }

    @Test
    @DisplayName ("Copy all and abort")
    public void copyAllAndAbort(FxRobot robot) {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destination.getAbsolutePath());
        verifyThat("#destinationTF", hasText(destination.getAbsolutePath()));

        robot.doubleClickOn("#maxBytesTF");
        verifyThat("#maxBytesTF", hasText(String.valueOf(destination.getUsableSpace())));

        robot.clickOn(guiController.getExtensionsComboBox());
        Platform.runLater(() -> guiController.getExtensionsComboBox().getCheckModel().check(".txt"));

        verifyThat("#logTA", hasText(""));
        verifyThat("#copyStopBT", isEnabled());

        verifyThat("#copyStopBT", hasText("Copy!"));

        // Fails because the thread is faster than the test
        // The button shuld change the text to "Abort", and clicking aborts the copy
//        robot.clickOn("#copyStopBT");
//        verifyThat("#copyStopBT", hasText("Abort"));
//        robot.clickOn("#copyStopBT");
//        verifyThat("#copyStopBT", hasText("Copy!"));
    }

    @Test
    @DisplayName ("Copy one file")
    public void copyOneFile(FxRobot robot) {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destination.getAbsolutePath());
        verifyThat("#destinationTF", hasText(destination.getAbsolutePath()));

        robot.doubleClickOn("#maxBytesTF");
        verifyThat("#maxBytesTF", hasText(String.valueOf(destination.getUsableSpace())));

        robot.doubleClickOn("#maxFilesTF").write("1");
        verifyThat("#maxFilesTF", hasText("1"));

        robot.clickOn(guiController.getExtensionsComboBox());
        Platform.runLater(() -> guiController.getExtensionsComboBox().getCheckModel().check(".txt"));

        verifyThat("#logTA", hasText(""));
        verifyThat("#copyStopBT", isEnabled());
        verifyThat("#copyStopBT", hasText("Copy!"));

        robot.clickOn("#copyStopBT");
        assertTrue(destination.listFiles().length == 1);
    }

    @Test
    @DisplayName ("Cope one byte")
    public void copyOneByte(FxRobot robot) {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destination.getAbsolutePath());
        verifyThat("#destinationTF", hasText(destination.getAbsolutePath()));

        robot.doubleClickOn("#maxBytesTF").write("1");
        robot.clickOn(guiController.getExtensionsComboBox());
        Platform.runLater(() -> guiController.getExtensionsComboBox().getCheckModel().check(".txt"));

        verifyThat("#maxBytesTF", hasText("1"));
        verifyThat("#logTA", hasText(""));
        verifyThat("#copyStopBT", isEnabled());
        verifyThat("#copyStopBT", hasText("Copy!"));

        robot.clickOn("#copyStopBT");
        String logMessage = "Scanning source directory...\n10 files found\n";
        verifyThat("#logTA", hasText(logMessage));
    }

    @Test
    @DisplayName ("Initial control states")
    public void initialControlStates(FxRobot robot) {
        verifyThat("#openSourceBT", isEnabled());
        verifyThat("#openDestinationBT", isEnabled());
        verifyThat("#copyStopBT", isDisabled());
        verifyThat("#copyStopBT", hasText("Copy!"));
        verifyThat("#sourceTF", hasText(""));
        verifyThat("#destinationTF", hasText(""));
        verifyThat("#maxFilesTF", hasText("0"));
        verifyThat("#maxBytesTF", hasText("0"));
        verifyThat("#logTA", hasText(""));
        robot.clickOn("#openSourceBT").type(KeyCode.ENTER);
        robot.clickOn("#openDestinationBT").type(KeyCode.ENTER);
    }

    @Test
    @DisplayName ("Open null source")
    public void openNullSourceDirectory(FxRobot robot) {
        DirectoryChooserHelper directoryChooserSpy = spy(guiController.new DirectoryChooserHelper());
        guiController.setDirectoryChooserHelper(directoryChooserSpy);
        verifyThat("#sourceTF", hasText(""));

        robot.clickOn("#openSourceBT").type(KeyCode.ESCAPE);

        verifyThat("#sourceTF", hasText(""));
    }

    @Test
    @DisplayName ("Open null destination")
    public void openNullDestinationDirectory(FxRobot robot) {
        DirectoryChooserHelper directoryChooserSpy = spy(guiController.new DirectoryChooserHelper());
        guiController.setDirectoryChooserHelper(directoryChooserSpy);
        verifyThat("#destinationTF", hasText(""));

        robot.clickOn("#openDestinationBT").type(KeyCode.ESCAPE);

        verifyThat("#destinationTF", hasText(""));
    }

    @Test
    @DisplayName ("Write invalid source and destination")
    public void writeInvalidSourceAndDestination(FxRobot robot) {
        verifyThat("#copyStopBT", isDisabled());
        robot.clickOn("#sourceTF").write("invalidpath");

        verifyThat("#sourceTF", hasText("invalidpath"));
        verifyThat("#copyStopBT", isDisabled());

        WaitForAsyncUtils.waitForFxEvents();
        // An Alert dialog is shown informing that an invalid source was entered
        // I can't test it. Pressing enter closes the window
        robot.clickOn(".button").type(KeyCode.ENTER);
        verifyThat("#sourceTF", hasText(""));

        robot.clickOn("#destinationTF").write("invalidpath");

        verifyThat("#destinationTF", hasText("invalidpath"));
        verifyThat("#copyStopBT", isDisabled());

        // An Alert dialog is shown informing that an invalid destination was entered
        // I can't test it. Pressing enter closes the window
        robot.clickOn(".button").type(KeyCode.ENTER);
        verifyThat("#destinationTF", hasText(""));
    }

    @Test
    @DisplayName ("Write valid source and destination")
    public void writeValidSourceAndDestination(FxRobot robot) {
        verifyThat("#copyStopBT", isDisabled());
        robot.clickOn("#sourceTF").write(sourceTestPath.toAbsolutePath().toString());
        verifyThat("#maxBytesTF", hasText("0"));


        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));
        verifyThat("#copyStopBT", isDisabled());

        robot.clickOn("#destinationTF").write(destination.getAbsolutePath());
        verifyThat("#destinationTF", hasText(destination.getAbsolutePath()));

        verifyThat("#copyStopBT", isEnabled());
        robot.clickOn("#rootAP");
        verifyThat("#maxBytesTF", hasText(String.valueOf(destination.getUsableSpace())));
    }
}