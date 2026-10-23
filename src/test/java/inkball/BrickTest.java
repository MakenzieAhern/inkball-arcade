package inkball;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BrickTest {

    Brick brick;

    @BeforeEach
    public void setUp() {
        brick = new Brick(100, 100, 32, "blue", null);
    }

    /**
     * 测试砖块在创建时是否未被销毁。
     */
    @Test
    public void testIsNotDestroyedInitially() {
        assertFalse(brick.isDestroyed());
    }

    /**
     * 测试击中砖块是否会减少剩余击中次数。
     */
    @Test
    public void testHit() {
        brick.hit("blue");
        assertEquals(2, brick.hitsRemaining);
    }

    /**
     * 测试砖块在足够次数的击中后是否被销毁。
     */
    @Test
    public void testBrickDestruction() {
        brick.hit("blue");
        brick.hit("blue");
        brick.hit("blue");
        assertTrue(brick.isDestroyed());
    }

    /**
     * 测试用错误的颜色击中砖块是否不会减少击中次数。
     */
    @Test
    public void testHitWithWrongColor() {
        brick.hit("orange");
        assertEquals(3, brick.hitsRemaining);
    }

    @Test
    public void destroyedBrickDoesNotAcceptMoreHits() {
        for (int hit = 0; hit < 10; hit++) {
            brick.hit("blue");
        }
        assertEquals(0, brick.hitsRemaining);
        assertTrue(brick.isDestroyed());
        assertTrue(brick.destroyed);
    }

    @Test
    public void explicitDestructionStopsCollisionsAndDamage() {
        brick.setDestroyed(true);
        assertTrue(brick.isDestroyed());
        assertEquals(0, brick.hitsRemaining);
        brick.hit("blue");
        assertEquals(0, brick.hitsRemaining);
    }

    @Test
    public void clearingDestroyedStateRestoresDestroyedBrick() {
        brick.setDestroyed(true);
        brick.setDestroyed(false);
        assertFalse(brick.isDestroyed());
        assertEquals(3, brick.hitsRemaining);
    }

    @Test
    public void greyBrickAcceptsEveryBallColor() {
        Brick greyBrick = new Brick(0, 0, 32, "grey", null);
        greyBrick.hit("orange");
        greyBrick.hit("blue");
        greyBrick.hit("green");
        assertTrue(greyBrick.isDestroyed());
    }
}
