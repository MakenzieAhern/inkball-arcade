package inkball;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GameResourcesTest {
    @TempDir
    Path temporaryDirectory;

    private String previousConfig;

    @BeforeEach
    void useBundledResources() {
        previousConfig = System.clearProperty(GameResources.CONFIG_PROPERTY);
    }

    @AfterEach
    void restoreConfigSelection() {
        if (previousConfig == null) {
            System.clearProperty(GameResources.CONFIG_PROPERTY);
        } else {
            System.setProperty(GameResources.CONFIG_PROPERTY, previousConfig);
        }
    }

    @Test
    void bundledConfigReferencesThreeReadableLevels() {
        JSONObject config = GameResources.loadConfig();
        JSONArray levels = config.getJSONArray("levels");
        assertEquals(3, levels.size());
        for (int index = 0; index < levels.size(); index++) {
            String[] layout = GameResources.loadLevel(levels.getJSONObject(index).getString("layout"));
            assertEquals(18, layout.length);
            assertTrue(String.join("", layout).contains("S"));
            assertTrue(String.join("", layout).contains("H"));
        }
    }

    @Test
    void externalLevelPathsResolveBesideSelectedConfig() throws Exception {
        Path layouts = Files.createDirectory(temporaryDirectory.resolve("layouts"));
        Files.write(layouts.resolve("custom.txt"), "XXXX\nX S X\nXXXX\n".getBytes(StandardCharsets.UTF_8));
        Path configFile = temporaryDirectory.resolve("custom.json");
        Files.write(configFile, "{\"name\":\"Custom pack\",\"layout\":\"layouts/custom.txt\"}".getBytes(StandardCharsets.UTF_8));
        System.setProperty(GameResources.CONFIG_PROPERTY, configFile.toString());

        JSONObject config = GameResources.loadConfig();
        assertEquals("Custom pack", config.getString("name"));
        assertArrayEquals(new String[]{"XXXX", "X S X", "XXXX"}, GameResources.loadLevel(config.getString("layout")));
    }

    @Test
    void externalLayoutPreservesSpacesAndUtf8() throws Exception {
        Path configFile = temporaryDirectory.resolve("config.json");
        Path layoutFile = temporaryDirectory.resolve("layout.txt");
        Files.write(layoutFile, "  X  \nX café X\n".getBytes(StandardCharsets.UTF_8));
        System.setProperty(GameResources.CONFIG_PROPERTY, configFile.toString());
        assertArrayEquals(new String[]{"  X  ", "X café X"}, GameResources.loadLevel(layoutFile.toString()));
    }

    @Test
    void missingBundledLayoutReportsResourceName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> GameResources.loadLevel("missing-layout.txt"));
        assertTrue(error.getMessage().contains("missing-layout.txt"));
    }

    @Test
    void missingExternalConfigDoesNotFallBackToBundledData() {
        Path missing = temporaryDirectory.resolve("missing.json");
        System.setProperty(GameResources.CONFIG_PROPERTY, missing.toString());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, GameResources::loadConfig);
        assertTrue(error.getMessage().contains(missing.toString()));
    }

    @Test
    void malformedJsonReportsConfigPath() throws Exception {
        Path configFile = temporaryDirectory.resolve("broken.json");
        Files.write(configFile, "[]".getBytes(StandardCharsets.UTF_8));
        System.setProperty(GameResources.CONFIG_PROPERTY, configFile.toString());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, GameResources::loadConfig);
        assertTrue(error.getMessage().contains(configFile.toString()));
    }

    @Test
    void emptyOrRelativeConfigPropertyIsRejected() {
        System.setProperty(GameResources.CONFIG_PROPERTY, "");
        assertThrows(IllegalArgumentException.class, GameResources::loadConfig);
        System.setProperty(GameResources.CONFIG_PROPERTY, "config.json");
        assertThrows(IllegalArgumentException.class, GameResources::loadConfig);
    }

    @Test
    void missingLayoutNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> GameResources.loadLevel(null));
        assertThrows(IllegalArgumentException.class, () -> GameResources.loadLevel("  "));
    }
}
