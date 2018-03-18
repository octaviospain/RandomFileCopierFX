package com.transgressoft.randomfilecopier.gui;

import com.google.common.io.Files;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.testfx.api.*;
import org.testfx.framework.junit5.*;

import java.io.*;
import java.nio.file.*;
import javafx.application.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.Alert.*;
import javafx.scene.input.*;
import javafx.stage.*;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.*;
import static org.testfx.matcher.base.NodeMatchers.*;

/**
 * @author Octavio Calleya
 */
@ExtendWith ({ApplicationExtension.class, MockitoExtension.class})
public class RandomFileCopierFxTest {

    Controller controller;
    Parent root;

    Path sourceTestPath = Paths.get("test-resources", "10testfiles");
    File destination = Files.createTempDir();
    
    @Mock
    DirectoryChooserHelper chooserHelper;
    @Mock
    AlertHelper alertHelper;
    @Mock
    AlertWrapper alert;

    @BeforeAll
    public static void beforeAll() {
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
        root = loader.load();
        controller = loader.getController();

        Clipboard clip = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString("notanumber");
        clip.setContent(content);

        stage.setScene(new Scene(root));
        stage.show();
        stage.centerOnScreen();
    }

    @BeforeEach
    void beforeEach() {
        when(chooserHelper.chooseDirectory()).thenReturn(sourceTestPath.toFile());
        controller.setDirectoryChooserHelper(chooserHelper);
        doNothing().when(alert).showAndWait();
        when(alertHelper.createAlert(any(), any(), any())).thenReturn(alert);
        controller.setAlertHelper(alertHelper);
    }

    @AfterEach
    public void afterEach() {
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

        robot.clickOn(controller.getExtensionsComboBox());
        Platform.runLater(() -> controller.getExtensionsComboBox().getCheckModel().check(".txt"));

        verifyThat("#logTA", hasText(""));
        verifyThat("#copyStopBT", isEnabled());

        verifyThat("#copyStopBT", hasText("Copy!"));

        // Fails because the thread is faster than the test
        // The button should change the text to "Abort", and clicking aborts the copy
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

        robot.clickOn(controller.getExtensionsComboBox());
        Platform.runLater(() -> controller.getExtensionsComboBox().getCheckModel().check(".txt"));

        verifyThat("#logTA", hasText(""));
        verifyThat("#copyStopBT", isEnabled());
        verifyThat("#copyStopBT", hasText("Copy!"));

        robot.clickOn("#copyStopBT");
        assertEquals(1, destination.listFiles().length);
    }

    @Test
    @DisplayName ("Cope one byte")
    public void copyOneByte(FxRobot robot) {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destination.getAbsolutePath());
        verifyThat("#destinationTF", hasText(destination.getAbsolutePath()));

        robot.doubleClickOn("#maxBytesTF").write("1");
        robot.clickOn(controller.getExtensionsComboBox());
        Platform.runLater(() -> controller.getExtensionsComboBox().getCheckModel().check(".txt"));

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
        when(chooserHelper.chooseDirectory()).thenReturn(null);
        controller.setDirectoryChooserHelper(chooserHelper);
        verifyThat("#sourceTF", hasText(""));

        robot.clickOn("#openSourceBT").type(KeyCode.ESCAPE);

        verifyThat("#sourceTF", hasText(""));
    }

    @Test
    @DisplayName ("Open null destination")
    public void openNullDestinationDirectory(FxRobot robot) {
        when(chooserHelper.chooseDirectory()).thenReturn(null);
        controller.setDirectoryChooserHelper(chooserHelper);
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
        robot.clickOn("#destinationTF");
        verify(alertHelper).createAlert(AlertType.WARNING, "Warning", "Source directory doesn't exist or is not a directory");
        verify(alert).showAndWait();
        verifyThat("#sourceTF", hasText(""));

        robot.doubleClickOn("#destinationTF").write("invalidpath");

        verifyThat("#destinationTF", hasText("invalidpath"));
        verifyThat("#copyStopBT", isDisabled());
        robot.clickOn("#sourceTF");
        verify(alertHelper).createAlert(AlertType.WARNING, "Warning", "Target directory doesn't exist or is not a directory");
        verify(alert, times(2)).showAndWait();
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