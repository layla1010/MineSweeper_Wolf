package control;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.ToggleButton;
import model.SysData;

public class FiltersControllerTest {

    private FiltersController controller;
    private ToggleButton timerToggle;

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> {}); // initialize JavaFX toolkit once
        } catch (IllegalStateException e) {
            // toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        SysData.resetToDefaults();

        controller = new FiltersController();
        timerToggle = new ToggleButton();

        // inject timerToggle into controller (because it's @FXML private)
        Field f = FiltersController.class.getDeclaredField("timerToggle");
        f.setAccessible(true);
        f.set(controller, timerToggle);
    }

    

    @Test
    void testOnTimerToggled_WhenSelectedTrue_TimerEnabledBecomesTrue() throws Exception {
        SysData.setTimerEnabled(false);    
        timerToggle.setSelected(true);

        runOnFxThreadAndWait(() -> {
            try {
                Method m = FiltersController.class.getDeclaredMethod("onTimerToggled");
                m.setAccessible(true);
                m.invoke(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(SysData.isTimerEnabled());
    }
    // helper: run code on JavaFX thread and wait
    private static void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException[] error = new RuntimeException[1];

        Platform.runLater(() -> {
            try { action.run(); }
            catch (RuntimeException e) { error[0] = e; }
            finally { latch.countDown(); }
        });

        latch.await();
        if (error[0] != null) throw error[0];
    }

}
