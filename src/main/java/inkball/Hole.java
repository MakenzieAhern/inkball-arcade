package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Represents a hole in the Inkball game, with properties for position, size, color, and image.
 * Balls are attracted to the hole based on their position and color.
 */
public class Hole {
    /** X-coordinate of the hole. */
    public int x;

    /** Y-coordinate of the hole. */
    public int y;

    /** Size (diameter) of the hole. */
    public int size;

    /** Color index of the hole, used to identify its color. */
    public int colorIndex;

    /** Image representing the hole. */
    public PImage image;

    /**
     * Constructs a Hole with specified properties.
     *
     * @param x          X-coordinate of the hole.
     * @param y          Y-coordinate of the hole.
     * @param colorIndex Index representing the color of the hole.
     * @param image      Image representing the hole.
     */
    public Hole(int x, int y, int colorIndex, PImage image) {
        this.x = x;
        this.y = y;
        this.size = 64; // Default size of the hole; can be adjusted as needed
        this.colorIndex = colorIndex;
        this.image = image;
    }

    /**
     * Returns the center position of the hole as a PVector.
     *
     * @return PVector representing the center of the hole.
     */
    public PVector getCenter() {
        return new PVector(x + size / 2f, y + size / 2f);
    }

    /**
     * Renders the hole on the provided PApplet.
     *
     * @param app The PApplet instance on which to render the hole.
     */
    public void render(PApplet app) {
        if (image != null) {
            app.image(image, x, y, size, size);
        }
    }

    /**
     * Returns the color of the hole as a string based on its color index.
     *
     * @return Color of the hole as a string (e.g., "grey", "orange").
     */
    public String getHoleColor() {
        switch (colorIndex) {
            case 0:
                return "grey";
            case 1:
                return "orange";
            case 2:
                return "blue";
            case 3:
                return "green";
            case 4:
                return "yellow";
            default:
                return "grey";
        }
    }
}
