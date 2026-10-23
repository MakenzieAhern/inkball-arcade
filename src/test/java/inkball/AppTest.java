package inkball;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;

import static org.junit.jupiter.api.Assertions.*;

/** Deterministic gameplay regressions: no native window or fake assertion-only tests. */
class AppTest implements PConstants {
    private FixtureApp app;

    /** Tiny playable level; production start/restart/update code is exercised unchanged. */
    private static class FixtureApp extends App {
        int configLoads;
        @Override public void loadConfig() {
            configLoads++;
            config = new JSONObject();
            JSONArray levels = new JSONArray();
            levels.append(new JSONObject());
            levels.append(new JSONObject());
            config.setJSONArray("levels", levels);
            levelData = new String[]{"                  ", "  S               ", "     B2           "};
            spawners.clear();
            spawners.add(new PVector(64, 96));
            holes.clear();
            bricks.clear();
            bricks.add(new Brick(320, 320, 32, "blue", null));
            ballColors = new String[]{"green", "orange", "yellow"};
            ballPositions = new int[]{10, 47, 84};
            timeLeft = 120;
            spawnInterval = 10;
            countdownTimer = 10;
        }
    }

    @BeforeEach void setUp() {
        app = new FixtureApp();
        app.width = 576;
        app.height = 640;
        app.restartLevel();
    }

    @Test void startsWithLayoutBallAndFirstQueuedBall() {
        assertEquals(2, app.balls.size());
        assertEquals("blue", app.balls.get(0).color);
        assertEquals(160, app.balls.get(0).x);
        assertEquals(128, app.balls.get(0).y);
        assertEquals("green", app.balls.get(1).color);
        assertEquals(1, app.ballsSpawnedIndex);
        assertEquals(1, app.configLoads);
    }

    @Test void restartRestoresLevelStateAndScoreRatherThanDuplicatingObjects() {
        app.score = 99;
        app.currentLine = new Line();
        app.drawnLines.add(new Line());
        app.levelWon = true;
        app.levelTimeUp = true;
        app.isPaused = true;
        app.yellowTiles.add(new PVector(0, 64));
        app.key = 'r';
        app.keyPressed();
        assertEquals(0, app.score);
        assertEquals(120, app.timeLeft);
        assertEquals(2, app.balls.size());
        assertEquals(1, app.bricks.size());
        assertEquals(1, app.ballsSpawnedIndex);
        assertTrue(app.drawnLines.isEmpty());
        assertTrue(app.yellowTiles.isEmpty());
        assertNull(app.currentLine);
        assertFalse(app.levelWon || app.levelTimeUp || app.isPaused || app.gameEnded);
    }

    @Test void restartKeepsScoreEarnedInEarlierLevels() {
        app.score = 75;
        app.advanceToNextLevel();
        assertEquals(1, app.currentLevel);
        assertEquals(2, app.configLoads, "Progression should load the next level once");
        app.score = 125;
        app.restartLevel();
        assertEquals(75, app.score);
        assertEquals(1, app.currentLevel);
    }

    @Test void restartAfterCampaignEndStartsFreshCampaign() {
        app.advanceToNextLevel();
        app.advanceToNextLevel();
        assertTrue(app.gameEnded);
        assertEquals(1, app.currentLevel, "Completed level index remains valid");
        app.score = 1000;
        app.restartLevel();
        assertEquals(0, app.currentLevel);
        assertEquals(0, app.score);
        assertFalse(app.gameEnded);
        assertEquals(2, app.balls.size());
    }

    @Test void thirtyFramesConsumeOneSecondAtThirtyFps() {
        for (int i = 0; i < 30; i++) app.updateCountdownTimer();
        assertEquals(119, app.timeLeft);
        assertEquals(9, app.countdownTimer, 0.0001);
    }

    @Test void timerUsesElapsedSecondsIndependentlyOfGlobalFrameCount() {
        app.frameCount = 60;
        app.updateCountdownTimer(0.4f);
        assertEquals(120, app.timeLeft);
        app.frameCount = 960;
        app.updateCountdownTimer(0.6f);
        assertEquals(119, app.timeLeft);
        assertEquals(9, app.countdownTimer, 0.0001);
    }

    @Test void spawnTimerPreservesFractionalRemainderAndQueuedOrder() {
        app.countdownTimer = 0.1f;
        app.updateCountdownTimer(0.25f);
        assertEquals(2, app.ballsSpawnedIndex);
        assertEquals("orange", app.balls.get(2).color);
        assertEquals(9.85, app.countdownTimer, 0.0001);
    }

    @Test void emptyQueueDisplaysZeroCountdown() {
        app.spawnBall();
        app.spawnBall();
        app.updateCountdownTimer(0.5f);
        assertEquals(0, app.countdownTimer);
        int count = app.balls.size();
        app.spawnBall();
        assertEquals(count, app.balls.size());
    }

    @Test void timeUpClampsAtZeroAndFreezesBallMotion() {
        app.timeLeft = 1;
        Ball ball = app.balls.get(0);
        float x = ball.x;
        app.updateGame(2.4f);
        assertEquals(0, app.timeLeft);
        assertTrue(app.levelTimeUp);
        assertEquals(x, ball.x);
        app.updateGame(10);
        assertEquals(0, app.timeLeft);
        assertEquals(x, ball.x);
    }

    @Test void pauseFreezesTimersAndPhysicsAndResumes() {
        app.key = ' ';
        app.keyPressed();
        Ball ball = app.balls.get(0);
        float x = ball.x;
        app.updateGame(50);
        assertEquals(120, app.timeLeft);
        assertEquals(10, app.countdownTimer);
        assertEquals(x, ball.x);
        app.keyPressed();
        app.updateGame(1);
        assertEquals(119, app.timeLeft);
        assertNotEquals(x, ball.x);
    }

    @Test void pauseDoesNotReactivateTimedOutOrEndedGame() {
        app.key = ' ';
        app.levelTimeUp = true;
        app.keyPressed();
        assertFalse(app.isPaused);
        app.levelTimeUp = false;
        app.gameEnded = true;
        app.keyPressed();
        assertFalse(app.isPaused);
    }

    @Test void pausedBallRemainsRenderable() {
        int[] draws = {0};
        app.balls.clear();
        app.balls.add(new Ball(100, 100, 1, 1, 32, "blue", null) {
            @Override public void render(PApplet canvas) { draws[0]++; }
        });
        app.isPaused = true;
        app.updateGame(1);
        app.renderBalls();
        assertEquals(1, draws[0]);
        assertEquals(100, app.balls.get(0).x);
    }

    @Test void winConvertsOneSecondToOnePointEvery67Milliseconds() {
        app.timeLeft = 3;
        app.handleWin();
        app.updateGame(0.066f);
        assertEquals(0, app.score);
        assertEquals(3, app.timeLeft);
        app.updateGame(0.001f);
        assertEquals(1, app.score);
        assertEquals(2, app.timeLeft);
        app.updateGame(0.134f);
        assertEquals(3, app.score);
        assertEquals(1, app.currentLevel);
        assertEquals(120, app.timeLeft);
        assertFalse(app.levelWon);
    }

    @Test void finalWinEndsCampaignWithoutOutOfRangeLevelRead() {
        app.advanceToNextLevel();
        app.timeLeft = 2;
        app.handleWin();
        app.updateGame(0.134f);
        assertEquals(2, app.score);
        assertTrue(app.gameEnded);
        assertEquals(1, app.currentLevel);
        app.updateGame(30);
        assertEquals(2, app.score);
    }

    @Test void completeOnlyAfterBothActiveBallsAndQueueAreEmpty() {
        app.balls.clear();
        assertFalse(app.checkLevelComplete());
        app.ballsSpawnedIndex = app.ballColors.length;
        assertTrue(app.checkLevelComplete());
        app.updateGame(0.1f);
        assertTrue(app.levelWon);
        assertEquals(120, app.timeLeft, "Winning frame must not consume ordinary countdown");
        assertEquals(2, app.yellowTiles.size());
    }

    @Test void capturedBallsAreRemovedAndScoreAppliedOnce() {
        app.balls.clear();
        Ball ball = new Ball(200, 200, 1, 1, 32, "blue", null);
        ball.isCaptured = true;
        ball.scoreChange = 50;
        app.balls.add(ball);
        app.updateBalls();
        assertEquals(50, app.score);
        assertTrue(app.balls.isEmpty());
        app.updateBalls();
        assertEquals(50, app.score);
    }

    @Test void wrongCaptureRequeuesWithoutReplacingPendingColors() {
        app.balls.clear();
        Ball ball = new Ball(200, 200, 0, 0, 32, "blue", null);
        ball.isCaptured = true;
        ball.needsRespawn = true;
        ball.scoreChange = -25;
        app.balls.add(ball);
        app.updateBalls();
        assertEquals(0, app.score);
        assertArrayEquals(new String[]{"green", "orange", "yellow", "blue"}, app.ballColors);
        assertEquals(1, app.ballsSpawnedIndex);
        app.spawnBall();
        assertEquals("orange", app.balls.get(0).color);
    }

    @Test void layoutBallWrongCaptureCanBeRetriedWithNoPreviouslySpawnedQueue() {
        app.balls.clear();
        app.ballColors = new String[0];
        app.ballPositions = new int[0];
        app.ballsSpawnedIndex = 0;
        app.countdownTimer = 0;
        Ball ball = new Ball(200, 200, 0, 0, 32, "yellow", null);
        ball.isCaptured = true;
        ball.needsRespawn = true;
        app.balls.add(ball);
        app.updateBalls();
        assertArrayEquals(new String[]{"yellow"}, app.ballColors);
        assertEquals(10, app.countdownTimer);
        assertFalse(app.checkLevelComplete());
        app.spawnBall();
        assertEquals("yellow", app.balls.get(0).color);
    }

    @Test void boundaryCollisionReflectsOnlyIncomingVelocity() {
        Ball ball = new Ball(0, 64, 2, 2, 32, "blue", null);
        app.checkWindowBoundaryCollision(ball);
        assertEquals(2, ball.vx);
        assertEquals(2, ball.vy);
        ball.vx = -2;
        ball.vy = -2;
        app.checkWindowBoundaryCollision(ball);
        assertEquals(2, ball.vx);
        assertEquals(2, ball.vy);
        assertEquals(0, ball.x);
        assertEquals(64, ball.y);
    }

    @Test void rightAndBottomBoundariesKeepBallInsideBoard() {
        Ball ball = new Ball(550, 620, 2, 3, 32, "blue", null);
        app.checkWindowBoundaryCollision(ball);
        assertEquals(544, ball.x);
        assertEquals(608, ball.y);
        assertEquals(-2, ball.vx);
        assertEquals(-3, ball.vy);
    }

    @Test void wallCollisionReflectsAndRecolorsBall() {
        app.levelData = new String[]{"     ", "  3  "};
        Ball ball = new Ball(40, 96, 2, 0, 32, "blue", null);
        app.checkWallCollision(ball);
        assertEquals(-2, ball.vx);
        assertEquals(32, ball.x);
        assertEquals("green", ball.color);
    }

    @Test void holeAndBallColorTokensAreNotWalls() {
        app.levelData = new String[]{"     ", " H2  ", " B3  ", " b4  "};
        Ball ball = new Ball(64, 96, 2, 0, 32, "blue", null);
        app.checkWallCollision(ball);
        assertEquals(64, ball.x);
        assertEquals(2, ball.vx);
        assertEquals("blue", ball.color);
    }

    @Test void brickCollisionActuallyOverlapsReflectsAndCountsHit() {
        Brick brick = app.bricks.get(0);
        Ball ball = new Ball(300, 320, 2, 0, 32, "blue", null);
        app.checkBrickCollision(ball);
        assertEquals(-2, ball.vx);
        assertEquals(288, ball.x);
        assertFalse(brick.isDestroyed());
        for (int i = 0; i < 2; i++) {
            ball.x = 300;
            ball.vx = 2;
            app.checkBrickCollision(ball);
        }
        assertTrue(brick.isDestroyed());
    }

    @Test void drawnLineReflectsOnceThenIsConsumed() {
        Line line = new Line();
        line.addPoint(100, 150);
        line.addPoint(200, 150);
        app.drawnLines.add(line);
        Ball ball = new Ball(140, 118, 0, 2, 32, "blue", null);
        app.checkLineCollision(ball);
        assertEquals(-2, ball.vy);
        assertTrue(app.drawnLines.isEmpty());
        app.checkLineCollision(ball);
        assertEquals(-2, ball.vy);
    }

    @Test void mouseDragCreatesPreviewAndFinishedLine() {
        app.mouseButton = LEFT;
        app.mouseX = 100;
        app.mouseY = 100;
        app.mousePressed();
        assertNotNull(app.currentLine);
        app.mouseX = 150;
        app.mouseY = 140;
        app.mouseDragged();
        assertEquals(2, app.currentLine.points.size());
        app.mouseReleased();
        assertNull(app.currentLine);
        assertEquals(1, app.drawnLines.size());
    }

    @Test void clickWithoutDragDoesNotLeaveInvisibleCollisionPoint() {
        app.mouseButton = LEFT;
        app.mouseX = 100;
        app.mouseY = 100;
        app.mousePressed();
        app.mouseReleased();
        assertTrue(app.drawnLines.isEmpty());
    }

    @Test void drawingInHudAndWhilePausedIsIgnored() {
        app.mouseButton = LEFT;
        app.mouseX = 100;
        app.mouseY = 20;
        app.mousePressed();
        assertNull(app.currentLine);
        app.mouseY = 100;
        app.isPaused = true;
        app.mousePressed();
        assertNull(app.currentLine);
    }

    @Test void rightClickRemovesOnlyNearbyLine() {
        Line line = new Line();
        line.addPoint(100, 100);
        line.addPoint(200, 100);
        app.drawnLines.add(line);
        app.mouseButton = RIGHT;
        app.mouseX = 150;
        app.mouseY = 130;
        app.mousePressed();
        assertEquals(1, app.drawnLines.size());
        app.mouseY = 104;
        app.mousePressed();
        assertTrue(app.drawnLines.isEmpty());
    }

    @Test void controlClickAlsoRemovesLine() {
        Line line = new Line();
        line.addPoint(100, 100);
        line.addPoint(200, 100);
        app.drawnLines.add(line);
        app.mouseButton = LEFT;
        app.keyPressed = true;
        app.key = CODED;
        app.keyCode = CONTROL;
        app.mouseX = 150;
        app.mouseY = 100;
        app.mousePressed();
        assertTrue(app.drawnLines.isEmpty());
        assertNull(app.currentLine);
    }

    @Test void queueAnimationStopsAtTargetsWithoutOvershoot() {
        app.ballSpeed = 5;
        for (int i = 0; i < 20; i++) app.updateBallQueueMovement();
        assertEquals(10, app.ballPositions[1]);
        assertEquals(47, app.ballPositions[2]);
        assertFalse(app.ballsAreMoving);
    }

    @Test void pointDistanceHandlesSegmentEndsAndZeroLength() {
        assertEquals(5, app.pointToSegmentDistance(new PVector(3, 4), new PVector(), new PVector()), 0.0001);
        assertEquals(5, app.pointToSegmentDistance(new PVector(15, 0), new PVector(), new PVector(10, 0)), 0.0001);
        assertEquals(4, app.pointToSegmentDistance(new PVector(5, 4), new PVector(), new PVector(10, 0)), 0.0001);
    }

    @Test void invalidElapsedTimeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> app.updateGame(-1));
        assertThrows(IllegalArgumentException.class, () -> app.updateGame(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> app.updateGame(Float.POSITIVE_INFINITY));
    }

    @Test void bundledLevelsAllInitializeWithTheirConfiguredBalls() {
        App bundled = new App();
        bundled.width = 576;
        bundled.height = 640;
        for (int level = 0; level < 3; level++) {
            bundled.currentLevel = level;
            bundled.loadConfig();
            assertEquals(18, bundled.levelData.length);
            assertFalse(bundled.spawners.isEmpty());
            assertFalse(bundled.holes.isEmpty());
            assertTrue(bundled.ballColors.length > 0);
            assertTrue(bundled.timeLeft > 0);
        }
    }
}
