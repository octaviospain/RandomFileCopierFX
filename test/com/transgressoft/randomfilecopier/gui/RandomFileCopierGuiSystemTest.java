package com.transgressoft.randomfilecopier.gui;

import javafx.scene.input.*;
import org.junit.*;
import org.testfx.api.*;

import static org.testfx.api.FxToolkit.*;

/**
 * @author Octavio Calleya
 */
public class RandomFileCopierGuiSystemTest {

    FxRobot robot = new FxRobot();

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
    }

    @Before
    public void setup() throws Exception {
        setupApplication(RandomFileCopierGui.class);
        Thread.sleep(2000);
    }

    @Test
    public void writeInvalidSource() {
        robot.clickOn("#sourceTF").write("rootfolderthatdoesntexist");

        robot.clickOn("#rootAP");

        robot.type(KeyCode.ENTER);
    }

    @Test
    public void writeInvalidTarget() {
        robot.clickOn("#destinationTF").write("targetfolderthatdoesntexist");

        robot.clickOn("#rootAP");

        robot.type(KeyCode.ENTER);
    }
}