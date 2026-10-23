package inkball;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * Represents a brick in the Inkball game, with properties for position, size, color, and image.
 * The brick can be hit by balls and will be destroyed after a certain number of hits.
 */
public class Brick {
    /** X-coordinate of the brick. */
    public int x;

    /** Y-coordinate of the brick. */
    public int y;

    /** Size (width and height) of the brick. */
    public int size;

    /** Color of the brick as a string. */
    public String color;

    /** Image representing the brick. */
    public PImage image;

    /** Remaining hits before the brick is destroyed. */
    public int hitsRemaining;

    /** Flag indicating if the brick has been destroyed. */
    public boolean destroyed = false;

    /**
     * Constructs a Brick with specified properties.
     *
     * @param x      X-coordinate of the brick.
     * @param y      Y-coordinate of the brick.
     * @param size   Size (width and height) of the brick.
     * @param color  Color of the brick.
     * @param image  Image representing the brick.
     */
    public Brick(int x, int y, int size, String color, PImage image) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color;
        this.image = image;
        this.hitsRemaining = 3; // Brick is destroyed after 3 hits
    }

    /**
     * Checks if the brick is destroyed based on the remaining hits.
     *
     * @return True if the brick is destroyed, false otherwise.
     */
    public boolean isDestroyed() {
        return destroyed || hitsRemaining <= 0;
    }

    /**
     * Sets the destroyed state of the brick.
     *
     * @param destroyed Boolean flag to set whether the brick is destroyed.
     */
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
        if (destroyed) {
            hitsRemaining = 0;
        } else if (hitsRemaining <= 0) {
            hitsRemaining = 3;
        }
    }

    /**
     * Decreases the hit count of the brick if hit by a ball of matching color or if the brick color is grey.
     *
     * @param ballColor Color of the ball hitting the brick.
     */
    public void hit(String ballColor) {
        if (!isDestroyed() && (ballColor.equals(color) || color.equals("grey"))) {
            hitsRemaining--;
            destroyed = hitsRemaining == 0;
        }
    }

    /**
     * Renders the brick on the provided PApplet if it is not destroyed.
     *
     * @param app The PApplet instance on which to render the brick.
     */
    public void render(PApplet app) {
        if (!isDestroyed() && image != null) {
            app.image(image, x, y, size, size);
        }
    }
}
