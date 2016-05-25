package applicationtest;

import com.transgressoft.randomfilecopier.gui.*;
import javafx.scene.input.*;
import org.junit.*;
import org.testfx.api.*;

import static org.testfx.api.FxToolkit.*;

/**
 * @author Octavio Calleya
 */
public class RandomCopyTests {

    FxRobot fx;

    @BeforeClass
    public static void setupSpec() throws Exception {
        registerPrimaryStage();
        setupStage(stage -> {
            stage.show();
            stage.centerOnScreen();
        });
    }

    @Before
    public void setup() throws Exception {
        setupApplication(RandomFileCopierFX.class);
        fx = new FxRobot();
        Thread.sleep(2000);
    }

    @Test
    public void writeInvalidSource() {
        fx.clickOn("#sourceTF").write("rootfolderthatdoesntexist");

        fx.clickOn("#rootAP");

        fx.type(KeyCode.ENTER);
    }

    @Test
    public void writeInvalidTarget() {
        fx.clickOn("#destinationTF").write("rootfolderthatdoesntexist");

        fx.clickOn("#rootAP");

        fx.type(KeyCode.ENTER);
    }
}