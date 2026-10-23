package inkball;

import processing.data.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Loads bundled game data, or an explicitly selected external level pack. */
final class GameResources {
    static final String CONFIG_PROPERTY = "inkball.config";

    private GameResources() {
    }

    static JSONObject loadConfig() {
        Path externalConfig = externalConfig();
        String source = externalConfig == null ? "bundled config.json" : externalConfig.toString();
        String text = String.join("\n", readLines("config.json", externalConfig));
        try {
            JSONObject config = JSONObject.parse(text);
            if (config == null) {
                throw new IllegalArgumentException("Expected a JSON object");
            }
            return config;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid game configuration: " + source, exception);
        }
    }

    static String[] loadLevel(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("A level layout filename is required");
        }
        Path externalConfig = externalConfig();
        Path externalLevel = externalConfig == null
                ? null : externalConfig.getParent().resolve(filename).normalize();
        return readLines(filename, externalLevel);
    }

    private static Path externalConfig() {
        String configured = System.getProperty(CONFIG_PROPERTY);
        if (configured == null) {
            return null;
        }
        if (configured.trim().isEmpty()) {
            throw new IllegalArgumentException("-D" + CONFIG_PROPERTY + " must name an absolute config.json path");
        }
        Path path = Paths.get(configured).normalize();
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("-D" + CONFIG_PROPERTY + " must be an absolute path: " + configured);
        }
        return path;
    }

    private static String[] readLines(String resourceName, Path externalFile) {
        String source = externalFile == null ? "bundled resource /" + resourceName : externalFile.toString();
        try (InputStream stream = externalFile == null
                ? GameResources.class.getResourceAsStream("/" + resourceName)
                : Files.newInputStream(externalFile)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing game data: " + source);
            }
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            return lines.toArray(new String[0]);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot read game data: " + source, exception);
        }
    }
}
