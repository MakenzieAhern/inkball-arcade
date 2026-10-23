package inkball;

import org.junit.jupiter.api.Test;
import processing.core.PVector;
import processing.core.PImage;

import static org.junit.jupiter.api.Assertions.*;

public class HoleTest {

    /**
     * 测试洞的中心是否正确计算。
     */
    @Test
    public void testGetCenter() {
        Hole hole = new Hole(100, 100, 2, null);
        PVector center = hole.getCenter();
        assertEquals(132, center.x, 0.01); // x + size/2 (100 + 64/2)
        assertEquals(132, center.y, 0.01);
    }

    /**
     * 测试洞是否返回正确的颜色。
     */
    @Test
    public void testGetHoleColor() {
        Hole hole = new Hole(100, 100, 2, null);
        assertEquals("blue", hole.getHoleColor());
    }
}
