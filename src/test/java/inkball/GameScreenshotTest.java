package inkball;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import processing.core.PApplet;

import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Optional desktop smoke test: renders real game frames and exports README images. */
@EnabledIfSystemProperty(named = "inkball.captureScreenshots", matches = "true")
class GameScreenshotTest {
    @Test
    void renderPlayableAndPausedFrames() throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless(), "Screenshot capture needs a desktop display or Xvfb");
        Path outputDirectory = Paths.get("docs", "images").toAbsolutePath();
        Files.createDirectories(outputDirectory);
        CaptureSketch sketch = new CaptureSketch(outputDirectory);
        Throwable testFailure = null;
        try {
            PApplet.runSketch(new String[]{CaptureSketch.class.getName()}, sketch);
            assertTrue(sketch.finished.await(30, TimeUnit.SECONDS), "Processing did not finish rendering in 30 seconds");
            if (sketch.failure.get() != null) {
                throw new AssertionError("Processing screenshot failed", sketch.failure.get());
            }
            for (String filename : new String[]{"level-1.png", "level-2-paused.png"}) {
                Path file = outputDirectory.resolve(filename);
                assertTrue(Files.size(file) > 1_000, "Expected a rendered PNG: " + filename);
                BufferedImage image = ImageIO.read(file.toFile());
                assertEquals(576 * sketch.pixelDensity, image.getWidth());
                assertEquals(640 * sketch.pixelDensity, image.getHeight());
            }
        } catch (Exception | Error exception) {
            testFailure = exception;
            throw exception;
        } finally {
            try {
                sketch.noLoop();
                if (sketch.getSurface() != null) {
                    sketch.getSurface().setVisible(false);
                    sketch.getSurface().stopThread();
                    sketch.dispose();
                }
            } catch (RuntimeException cleanupFailure) {
                if (testFailure != null) {
                    testFailure.addSuppressed(cleanupFailure);
                } else {
                    throw cleanupFailure;
                }
            }
        }
    }

    public static class CaptureSketch extends App {
        final CountDownLatch finished = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final Path outputDirectory;

        CaptureSketch(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
        }

        @Override
        public void setup() {
            try {
                randomSeed(42);
                super.setup();
                drawRoute(new int[][]{{100, 380}, {140, 360}, {180, 350}, {210, 360}});
            } catch (Throwable exception) {
                finishWithFailure(exception);
            }
        }

        @Override
        public void draw() {
            if (failure.get() != null) {
                return;
            }
            try {
                super.draw();
                if (frameCount == 3) {
                    assertFalse(drawnLines.isEmpty(), "The input-created demonstration barrier should remain visible");
                    saveCanvas("level-1.png");
                    currentLevel = 1;
                    restartLevel();
                    drawRoute(new int[][]{{220, 335}, {250, 370}, {290, 390}, {330, 380}});
                    key = ' ';
                    keyPressed();
                } else if (frameCount == 5) {
                    assertTrue(isPaused, "Space should pause the second scene");
                    saveCanvas("level-2-paused.png");
                    noLoop();
                    finished.countDown();
                }
            } catch (Throwable exception) {
                finishWithFailure(exception);
            }
        }

        private void drawRoute(int[][] points) {
            mouseButton = LEFT;
            mouseX = points[0][0];
            mouseY = points[0][1];
            mousePressed();
            for (int index = 1; index < points.length; index++) {
                mouseX = points[index][0];
                mouseY = points[index][1];
                mouseDragged();
            }
            mouseReleased();
        }

        private void saveCanvas(String filename) {
            if (!get().save(outputDirectory.resolve(filename).toString())) {
                throw new IllegalStateException("Could not save " + filename);
            }
        }

        private void finishWithFailure(Throwable exception) {
            failure.set(exception);
            noLoop();
            finished.countDown();
        }
    }
}
