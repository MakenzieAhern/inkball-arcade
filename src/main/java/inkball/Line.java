package inkball;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;

/**
 * Represents a line drawn by the player in the Inkball game.
 * The line is defined by a series of points and can be rendered on the game screen.
 */
public class Line {
    /** List of points that make up the line. */
    public ArrayList<PVector> points = new ArrayList<>();

    /**
     * Adds a new point to the line at the specified coordinates.
     *
     * @param x X-coordinate of the point to add.
     * @param y Y-coordinate of the point to add.
     */
    public void addPoint(float x, float y) {
        points.add(new PVector(x, y));
    }

    /**
     * Renders the line on the provided PApplet by connecting all points in the list.
     *
     * @param app The PApplet instance on which to render the line.
     */
    public void render(PApplet app) {
        app.stroke(0);          // Set line color to black
        app.strokeWeight(10);    // Set line thickness
        app.noFill();            // No fill for the line
        app.beginShape();
        for (PVector point : points) {
            app.vertex(point.x, point.y); // Draw vertex for each point
        }
        app.endShape();
    }
}
