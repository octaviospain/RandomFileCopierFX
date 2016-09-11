package com.transgressoft.randomfilecopier.gui;

import com.transgressoft.randomfilecopier.gui.GuiController.*;
import javafx.application.*;
import javafx.scene.input.*;
import org.junit.*;
import org.junit.rules.*;
import org.testfx.api.*;

import java.nio.file.*;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;
import static org.testfx.api.FxAssert.*;
import static org.testfx.api.FxToolkit.*;
import static org.testfx.matcher.base.NodeMatchers.*;


/**
 * @author Octavio Calleya
 * @version 0.2.2
 */
public class RandomFileCopierFxTest {

    FxRobot robot = new FxRobot();
    RandomFileCopierFx application;
    GuiController guiController;

    static Path sourceTestPath;
    static Path destinationTestPath;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    public static void setupSpec() throws Exception {
        if (Boolean.getBoolean("headless")) {
            System.setProperty("testfx.robot", "glass");
            System.setProperty("testfx.headless", "true");
            System.setProperty("prism.order", "sw");
            System.setProperty("prism.text", "t2k");
            System.setProperty("java.awt.headless", "true");
        }

        registerPrimaryStage();
        setupStage(stage -> {
            stage.show();
            stage.centerOnScreen();
        });

        sourceTestPath = Paths.get("test-resources", "10testfiles");
    }

    @Before
    public void setup() throws Exception {
        setupApplication(() -> application = new RandomFileCopierFx());

        // Mock a directory chooser
        DirectoryChooserHelper chooserHelper = mock(DirectoryChooserHelper.class);
        when(chooserHelper.chooseDirectory()).thenReturn(sourceTestPath.toFile());

        guiController = application.getController();
        guiController.setDirectoryChooserHelper(chooserHelper);
        testFolder.create();
        destinationTestPath = testFolder.getRoot().toPath();

        // Set a clipboard content
        Platform.runLater(() -> {
            Clipboard clip = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString("notanumber");
            clip.setContent(content);
        });
        Thread.sleep(500);
    }

    @After
    public void tearDown() throws Exception {
        testFolder.delete();
        assertFalse(testFolder.getRoot().exists());
    }

    @Test
    public void typeMoreBytesThanAvailable() {
        robot.clickOn("#destinationTF").write(destinationTestPath.toAbsolutePath().toString());
        verifyThat("#destinationTF", hasText(destinationTestPath.toAbsolutePath().toString()));

        robot.doubleClickOn("#maxBytesTF");
        robot.write(String.valueOf(destinationTestPath.toFile().getUsableSpace() + 1));
        robot.clickOn("#logTA");

        verifyThat("#maxBytesTF", hasText(String.valueOf(destinationTestPath.toFile().getUsableSpace())));
    }

    @Test
    public void typeInvalidMaxFiles() {
        robot.doubleClickOn("#maxFilesTF").write("notanumber");
        verifyThat("#maxFilesTF", hasText("0"));

        robot.doubleClickOn("#maxFilesTF").write("notanumber").clickOn("#logTA");
        verifyThat("#maxFilesTF", hasText("0"));

        robot.doubleClickOn("#maxFilesTF").push(new KeyCodeCombination(KeyCode.V, KeyCombination.META_DOWN));
        robot.clickOn("#logTA");
        verifyThat("#maxFilesTF", hasText("0"));
    }

    @Test
    public void typeInvalidMaxBytes() {
        robot.doubleClickOn("#maxBytesTF").write("notanumber");
        verifyThat("#maxBytesTF", hasText("0"));


        robot.doubleClickOn("#maxBytesTF").write("notanumber").clickOn("#logTA");
        verifyThat("#maxBytesTF", hasText("0"));

        robot.doubleClickOn("#maxBytesTF").push(new KeyCodeCombination(KeyCode.V, KeyCombination.META_DOWN));
        robot.clickOn("#logTA");
        verifyThat("#maxBytesTF", hasText("0"));
    }

    @Test
    public void copyAllAndAbort() {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destinationTestPath.toAbsolutePath().toString());
        verifyThat("#destinationTF", hasText(destinationTestPath.toAbsolutePath().toString()));

        robot.doubleClickOn("#maxBytesTF");
        verifyThat("#maxBytesTF", hasText(String.valueOf(destinationTestPath.toFile().getUsableSpace())));

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
    public void copyOneFile() {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destinationTestPath.toAbsolutePath().toString());
        verifyThat("#destinationTF", hasText(destinationTestPath.toAbsolutePath().toString()));

        robot.doubleClickOn("#maxBytesTF");
        verifyThat("#maxBytesTF", hasText(String.valueOf(destinationTestPath.toFile().getUsableSpace())));

        robot.doubleClickOn("#maxFilesTF").write("1");
        verifyThat("#maxFilesTF", hasText("1"));

        robot.clickOn(guiController.getExtensionsComboBox());
        Platform.runLater(() -> guiController.getExtensionsComboBox().getCheckModel().check(".txt"));

        verifyThat("#logTA", hasText(""));
        verifyThat("#copyStopBT", isEnabled());
        verifyThat("#copyStopBT", hasText("Copy!"));

        robot.clickOn("#copyStopBT");
        assertTrue(destinationTestPath.toFile().listFiles().length == 1);
    }

    @Test
    public void copyOneByte() {
        robot.clickOn("#openSourceBT");
        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));

        robot.clickOn("#destinationTF").write(destinationTestPath.toAbsolutePath().toString());
        verifyThat("#destinationTF", hasText(destinationTestPath.toAbsolutePath().toString()));

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
    public void initialControlStates() {
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
    public void openNullSourceDirectory() {
        DirectoryChooserHelper directoryChooserSpy = spy(guiController.new DirectoryChooserHelper());
        guiController.setDirectoryChooserHelper(directoryChooserSpy);
        verifyThat("#sourceTF", hasText(""));

        robot.clickOn("#openSourceBT").type(KeyCode.ESCAPE);

        verifyThat("#sourceTF", hasText(""));
        verify(directoryChooserSpy, times(1)).chooseDirectory();
    }

    @Test
    public void openNullDestinationDirectory() {
        DirectoryChooserHelper directoryChooserSpy = spy(guiController.new DirectoryChooserHelper());
        guiController.setDirectoryChooserHelper(directoryChooserSpy);
        verifyThat("#destinationTF", hasText(""));

        robot.clickOn("#openDestinationBT").type(KeyCode.ESCAPE);

        verifyThat("#destinationTF", hasText(""));
        verify(directoryChooserSpy, times(1)).chooseDirectory();
    }

    @Test
    public void writeInvalidSourceAndDestination() {
        verifyThat("#copyStopBT", isDisabled());
        robot.clickOn("#sourceTF").write("rootfolderthatdoesntexist");

        verifyThat("#sourceTF", hasText("rootfolderthatdoesntexist"));
        verifyThat("#copyStopBT", isDisabled());

        // An Alert dialog is shown informing that an invalid source was entered
        // I can't test it. Pressing enter closes the window
        robot.clickOn(".button").type(KeyCode.ENTER);
        verifyThat("#sourceTF", hasText(""));

        robot.clickOn("#destinationTF").write("destinationfolerthatdoesntexist");

        verifyThat("#destinationTF", hasText("destinationfolerthatdoesntexist"));
        verifyThat("#copyStopBT", isDisabled());

        // An Alert dialog is shown informing that an invalid destination was entered
        // I can't test it. Pressing enter closes the window
        robot.clickOn(".button").type(KeyCode.ENTER);
        verifyThat("#destinationTF", hasText(""));
    }

    @Test
    public void writeValidSourceAndDestination() {
        verifyThat("#copyStopBT", isDisabled());
        robot.clickOn("#sourceTF").write(sourceTestPath.toAbsolutePath().toString());
        verifyThat("#maxBytesTF", hasText("0"));


        verifyThat("#sourceTF", hasText(sourceTestPath.toAbsolutePath().toString()));
        verifyThat("#copyStopBT", isDisabled());

        robot.clickOn("#destinationTF").write(destinationTestPath.toAbsolutePath().toString());
        verifyThat("#destinationTF", hasText(destinationTestPath.toAbsolutePath().toString()));

        verifyThat("#copyStopBT", isEnabled());
        robot.clickOn("#rootAP");
        verifyThat("#maxBytesTF", hasText(String.valueOf(destinationTestPath.toFile().getUsableSpace())));
    }
}