package inkball;

import org.junit.jupiter.api.Test;
import processing.core.PVector;

import static org.junit.jupiter.api.Assertions.*;

public class LineTest {

    /**
     * 测试点是否正确添加到线条中。
     */
    @Test
    public void testAddPoint() {
        Line line = new Line();
        line.addPoint(10, 20);
        line.addPoint(30, 40);
        assertEquals(2, line.points.size());
        assertEquals(new PVector(10, 20), line.points.get(0));
        assertEquals(new PVector(30, 40), line.points.get(1));
    }
}
