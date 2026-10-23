package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Represents a ball in the Inkball game with properties like position, velocity, size, color, and image.
 * This class handles the movement, rendering, collision reflection, color changes, and scoring logic of the ball.
 */
public class Ball {
    /** X-coordinate of the ball. */
    public float x;

    /** Y-coordinate of the ball. */
    public float y;

    /** Velocity in the x-direction. */
    public float vx;

    /** Velocity in the y-direction. */
    public float vy;

    /** Size (diameter) of the ball. */
    public int size;

    /** Full diameter used when the ball is outside a hole's attraction radius. */
    private final int initialSize;

    /** Image representing the ball. */
    public PImage image;

    /** Flag indicating if the ball has been captured by a hole. */
    public boolean isCaptured = false;

    /** Color of the ball as a string. */
    public String color;

    /** Change in score associated with this ball when captured. */
    public int scoreChange = 0;

    /** Flag indicating if the ball needs to respawn after scoring incorrectly. */
    public boolean needsRespawn = false;

    /**
     * Constructs a Ball with specified properties.
     *
     * @param x     Initial x-coordinate.
     * @param y     Initial y-coordinate.
     * @param vx    Velocity in the x-direction.
     * @param vy    Velocity in the y-direction.
     * @param size  Size of the ball.
     * @param color Color of the ball.
     * @param image Image representing the ball.
     */
    public Ball(float x, float y, float vx, float vy, int size, String color, PImage image) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.initialSize = size;
        this.color = color;
        this.image = image;
    }

    /**
     * Returns the center position of the ball as a PVector.
     *
     * @return PVector representing the center of the ball.
     */
    public PVector getCenter() {
        return new PVector(x + size / 2f, y + size / 2f);
    }

    /**
     * Updates the position of the ball by adding its velocity to its coordinates.
     */
    public void update() {
        if (!isCaptured) {
            x += vx;
            y += vy;
        }
    }

    /**
     * Renders the ball on the provided PApplet if it is not captured.
     *
     * @param app The PApplet instance on which to render the ball.
     */
    public void render(PApplet app) {
        if (!isCaptured && image != null) {
            app.image(image, x, y, size, size);
        }
    }

    /**
     * Reflects the velocity of the ball off a surface with a given normal vector.
     *
     * @param normal The normal vector of the surface the ball is reflecting off.
     */
    public void reflect(PVector normal) {
        normal = normal.copy();
        normal.normalize();

        PVector velocity = new PVector(vx, vy);
        float dotProduct = velocity.dot(normal);
        PVector reflection = PVector.sub(velocity, PVector.mult(normal, 2 * dotProduct));

        vx = reflection.x;
        vy = reflection.y;
    }

    /**
     * Changes the color and image of the ball.
     *
     * @param color    New color of the ball.
     * @param newImage New image representing the ball.
     */
    public void changeColor(String color, PImage newImage) {
        this.color = color;
        this.image = newImage;
    }

    /**
     * Applies an attraction force toward nearby holes if within a certain range.
     *
     * @param holes       List of holes to check for attraction.
     * @param config      Game configuration object for score settings.
     * @param currentLevel Current level to apply specific score modifiers.
     */
    public void applyHoleAttraction(List<Hole> holes, JSONObject config, int currentLevel) {
        if (isCaptured) {
            return;
        }

        PVector center = getCenter();
        Hole nearestHole = null;
        float nearestDistance = Float.POSITIVE_INFINITY;
        for (Hole hole : holes) {
            float distance = PVector.dist(center, hole.getCenter());
            if (distance <= 32 && distance < nearestDistance) {
                nearestHole = hole;
                nearestDistance = distance;
            }
        }

        if (nearestHole == null) {
            resizeAroundCenter(initialSize, center);
            return;
        }

        PVector direction = PVector.sub(nearestHole.getCenter(), center);
        PVector attraction = direction.mult(PApplet.map(nearestDistance, 0, 32, 0.2f, 0.05f));
        vx += attraction.x;
        vy += attraction.y;

        // Scale from the full diameter rather than shrinking the previous frame's size.
        resizeAroundCenter((int) (initialSize * nearestDistance / 32f), center);
        if (nearestDistance <= 24) {
            handleScore(nearestHole, config, currentLevel);
        }
    }

    private void resizeAroundCenter(int newSize, PVector center) {
        size = newSize;
        x = center.x - size / 2f;
        y = center.y - size / 2f;
    }

    /**
     * Determines the score impact based on the color of the hole the ball enters.
     * If the ball color matches the hole or is grey, score increases. Otherwise, score decreases.
     *
     * @param hole         Hole that the ball is interacting with.
     * @param config       Game configuration object for score settings.
     * @param currentLevel Current level to apply specific score modifiers.
     */
    public void handleScore(Hole hole, JSONObject config, int currentLevel) {
        if (isCaptured) {
            return;
        }
        String currentBallColor = this.color.toLowerCase(Locale.ROOT);
        String holeColor = hole.getHoleColor();

        if (holeColor.equals("grey") || currentBallColor.equals("grey") || currentBallColor.equals(holeColor)) {
            int scoreIncrease = config.getJSONObject("score_increase_from_hole_capture").getInt(currentBallColor);
            float scoreModifier = config.getJSONArray("levels").getJSONObject(currentLevel)
                    .getFloat("score_increase_from_hole_capture_modifier");
            scoreChange = (int) (scoreIncrease * scoreModifier);
            isCaptured = true;
        } else {
            int scoreDecrease = config.getJSONObject("score_decrease_from_wrong_hole").getInt(currentBallColor);
            float scoreModifier = config.getJSONArray("levels").getJSONObject(currentLevel)
                    .getFloat("score_decrease_from_wrong_hole_modifier");
            scoreChange = -(int) (scoreDecrease * scoreModifier);
            isCaptured = true;
            needsRespawn = true;
        }
    }
}
