package inkball;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONObject;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class BallTest {
    private Ball ball;
    private Hole hole;
    private JSONObject config;
    private int currentLevel;

    @BeforeEach
    public void setUp() {
        ball = new Ball(100, 100, 2, 2, 32, "blue", null);
        hole = new Hole(100, 100, 2, null); // 2 对应于 "blue"
        currentLevel = 0; // 假设测试的是第一个关卡

        // 创建模拟的 config 对象
        config = new JSONObject();

        // 设置 score_increase_from_hole_capture
        JSONObject scoreIncrease = new JSONObject();
        scoreIncrease.setInt("blue", 10);
        scoreIncrease.setInt("grey", 5);
        config.setJSONObject("score_increase_from_hole_capture", scoreIncrease);

        // 设置 score_decrease_from_wrong_hole
        JSONObject scoreDecrease = new JSONObject();
        scoreDecrease.setInt("blue", 5);
        scoreDecrease.setInt("grey", 3);
        config.setJSONObject("score_decrease_from_wrong_hole", scoreDecrease);

        // 设置 levels 数组
        JSONObject levelConfig = new JSONObject();
        levelConfig.setFloat("score_increase_from_hole_capture_modifier", 1.0f);
        levelConfig.setFloat("score_decrease_from_wrong_hole_modifier", 1.0f);
        config.setJSONArray("levels", new processing.data.JSONArray());
        config.getJSONArray("levels").append(levelConfig);
    }

    @Test
    public void testHandleScore_CorrectHole() {
        ball.handleScore(hole, config, currentLevel);
        assertTrue(ball.isCaptured);
        assertEquals(10, ball.scoreChange); // 根据模拟的 config，得分应增加 10
        assertFalse(ball.needsRespawn);
    }

    @Test
    public void testHandleScore_WrongHole() {
        Hole wrongHole = new Hole(100, 100, 1, null); // 1 对应于 "orange"
        ball.handleScore(wrongHole, config, currentLevel);
        assertTrue(ball.isCaptured);
        assertEquals(-5, ball.scoreChange); // 根据模拟的 config，得分应减少 5
        assertTrue(ball.needsRespawn);
    }

    @Test
    public void attractionScalesFromFullSizeWithoutMovingTheCenter() {
        ball = new Ball(144, 116, 0, 0, 32, "blue", null);
        PVector center = ball.getCenter(); // 28 pixels from the hole's center.

        for (int frame = 0; frame < 10; frame++) {
            ball.applyHoleAttraction(Collections.singletonList(hole), config, currentLevel);
            assertEquals(28, ball.size, "A stationary ball must not shrink again each frame");
            assertEquals(center.x, ball.getCenter().x, 0.001f);
            assertEquals(center.y, ball.getCenter().y, 0.001f);
            assertFalse(ball.isCaptured);
        }
        assertTrue(ball.vx < 0, "Attraction points toward the hole");
        assertEquals(0, ball.vy, 0.001f);
    }

    @Test
    public void leavingAttractionRestoresFullDiameterWithoutMovingTheCenter() {
        ball = new Ball(144, 116, 0, 0, 32, "blue", null);
        ball.applyHoleAttraction(Collections.singletonList(hole), config, currentLevel);
        ball.x += 100;
        PVector center = ball.getCenter();

        ball.applyHoleAttraction(Collections.singletonList(hole), config, currentLevel);

        assertEquals(32, ball.size);
        assertEquals(center.x, ball.getCenter().x, 0.001f);
        assertEquals(center.y, ball.getCenter().y, 0.001f);
        assertFalse(ball.isCaptured);
    }

    @Test
    public void captureIsBasedOnCenterDistance() {
        ball = new Ball(140, 116, 0, 0, 32, "blue", null);
        ball.applyHoleAttraction(Collections.singletonList(hole), config, currentLevel);
        assertTrue(ball.isCaptured);
        assertEquals(10, ball.scoreChange);
        assertFalse(ball.needsRespawn);
    }

    @Test
    public void nearbyHolesCaptureUsingTheNearestHole() {
        ball = new Ball(116, 116, 0, 0, 32, "blue", null);
        Hole fartherWrongHole = new Hole(120, 100, 1, null);

        ball.applyHoleAttraction(Arrays.asList(fartherWrongHole, hole), config, currentLevel);

        assertEquals(10, ball.scoreChange);
        assertFalse(ball.needsRespawn);
    }

    @Test
    public void capturedBallCannotBeScoredAgainOrMoved() {
        Hole wrongHole = new Hole(100, 100, 1, null);
        ball.handleScore(wrongHole, config, currentLevel);
        float oldX = ball.x;
        float oldY = ball.y;
        float oldVx = ball.vx;

        ball.handleScore(hole, config, currentLevel);
        ball.applyHoleAttraction(Collections.singletonList(hole), config, currentLevel);
        ball.update();

        assertEquals(-5, ball.scoreChange);
        assertTrue(ball.needsRespawn);
        assertEquals(oldX, ball.x);
        assertEquals(oldY, ball.y);
        assertEquals(oldVx, ball.vx);
    }

    @Test
    public void greyBallCanEnterAnyHole() {
        ball.changeColor("grey", null);
        ball.handleScore(hole, config, currentLevel);
        assertEquals(5, ball.scoreChange);
        assertFalse(ball.needsRespawn);
    }

    @Test
    public void greyHoleAcceptsAnyBallAndAppliesLevelModifier() {
        config.getJSONArray("levels").getJSONObject(0)
                .setFloat("score_increase_from_hole_capture_modifier", 1.5f);
        ball.handleScore(new Hole(100, 100, 0, null), config, currentLevel);
        assertEquals(15, ball.scoreChange);
        assertFalse(ball.needsRespawn);
    }

    @Test
    public void wrongHoleAppliesPenaltyModifier() {
        config.getJSONArray("levels").getJSONObject(0)
                .setFloat("score_decrease_from_wrong_hole_modifier", 1.5f);
        ball.handleScore(new Hole(100, 100, 1, null), config, currentLevel);
        assertEquals(-7, ball.scoreChange, "Fractional points are truncated toward zero");
        assertTrue(ball.needsRespawn);
    }

    @Test
    public void reflectionPreservesSpeedAndDoesNotMutateTheNormal() {
        ball.vx = 3;
        ball.vy = 4;
        PVector normal = new PVector(2, 0);
        ball.reflect(normal);
        assertEquals(-3, ball.vx, 0.001f);
        assertEquals(4, ball.vy, 0.001f);
        assertEquals(25, ball.vx * ball.vx + ball.vy * ball.vy, 0.001f);
        assertEquals(2, normal.x, 0.001f);
    }

    @Test
    public void zeroLengthNormalLeavesVelocityUnchanged() {
        ball.reflect(new PVector(0, 0));
        assertEquals(2, ball.vx);
        assertEquals(2, ball.vy);
    }

    @Test
    public void uncapturedBallMovesByItsVelocity() {
        ball.update();
        assertEquals(102, ball.x);
        assertEquals(102, ball.y);
    }


}
