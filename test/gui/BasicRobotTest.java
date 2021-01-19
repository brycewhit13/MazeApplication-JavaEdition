package gui;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import generation.CardinalDirection;
import generation.Floorplan;
import generation.MazeBuilder;
import generation.MazeBuilderEller;
import generation.MazeBuilderPrim;
import generation.MazeContainer;
import generation.MazeFactory;
import generation.Order;
import generation.StubOrder;
import gui.Robot.Turn;

/**
 * A set of tests to test the BasicRobot.java class
 * @author Bryce Whitney
 *
 */
public class BasicRobotTest {
	//private variables needed for testing
	private BasicRobot robot;
	private Controller controller;
	private StateGenerating generator;
	private MazeFactory mazeFactory;
	private MazeApplication maze;
	
	/**
	 * Set up everything needed for each test
	 */
	@Before
	public void setup() {
		controller = new Controller();
		robot = new BasicRobot();
		
		controller.start();
		controller.setPerfect(false); //want rooms
		controller.setBuilder(Order.Builder.Prim);
		generator = (StateGenerating) controller.states[1];
		generator.factory = new MazeFactory(true); //deterministic
		controller.switchFromTitleToGenerating(0); //want basic skill level
		generator.factory.waitTillDelivered();
		robot.setMaze(controller);
	}
	
	/**
	 * Breaks everything down to ensure a fresh test on the next go around
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception{}
	
	/**
	 * Test getCurrentPosition() with a valid position and checks the postcondition
	 */
	@Test
	public void testGetCurrentPosition_Valid() {
		int[] actualPosition = controller.getCurrentPosition();
		int[] position = new int[2]; 
		try{
			position = robot.getCurrentPosition();
		} catch(Exception e) {
			fail("Exception thrown");
		}
		assertTrue("positions are equal", position[0] == actualPosition[0] && position[1] == actualPosition[1]);
	}
	
	/**
	 * Tests getCurrentPosition() with an invalid position
	 */
	@Test
	public void testGetCurrentPosition_Invalid() {
		int[] position = {-5, Integer.MAX_VALUE};
		robot.setCurrentPosition(position);
		try {
			position = robot.getCurrentPosition();
			fail("Exception should be thrown");
		}catch(Exception e) {
			assertTrue("Exception thrown properly", true); // know this will be true if code block is reached
		}
	}
	
	/**
	 * Tests getCurrentDirection() for accuracy against a known direction
	 */
	@Test
	public void testGetCurrentDirection() {
		robot.setCurrentDirection(CardinalDirection.East);
		assertTrue("Direction should be East", robot.getCurrentDirection() == CardinalDirection.East);
		robot.setCurrentDirection(CardinalDirection.West);
		assertTrue("Direction should be West", robot.getCurrentDirection() == CardinalDirection.West);
	}
	
	
	/**
	 * Tests that setMaze() properly sets up the references with a valid Controller input
	 */
	@Test
	public void testSetMaze() {
		assertTrue("Controller is not null", robot.getController() != null);
		int[] currentPosition = new int[2];
		try {
			currentPosition = robot.getCurrentPosition();
		}catch(Exception e) {
			fail("No exception should be thrown");
		}
		assertTrue("Position is not null", currentPosition != null);
	}
	
	/**
	 * Tests that getBatteryLevel() works properly before and after moves
	 */
	@Test
	public void testGetBatteryLevel() {
		//test it gets the starting value --> doesn't rely on setBatteryLevel
		assertTrue("Proper battery level", robot.getBatteryLevel() == 3000);
	}
	
	/**
	 * Tests that the battery level is correct after setBatteryLevel() is called
	 */
	@Test
	public void testSetBatteryLevel() {
		//positive number
		robot.setBatteryLevel(15);
		assertTrue("proper battery level change", robot.getBatteryLevel() == 15);
		
		//negative number
		robot.setBatteryLevel(-30);
		assertTrue("proper battery level change", robot.getBatteryLevel() == -30);
	}
	
	/**
	 * Tests that the Odometer reading returns correctly
	 */
	@Test
	public void testGetOdometerReading() {
		//test at beginning
		assertTrue("Proper odometer reading",robot.getOdometerReading() == 0);
		
		//test after a move
		robot.move(1, false);
		assertTrue("Proper odometer reading", robot.getOdometerReading() == 1);
	}
	
	/**
	 * Ensures the odometer is 0
	 */
	@Test
	public void testResetOdometerReading() {
		robot.resetOdometer();
		assertTrue("Odometer shouldn't change if already zero", robot.getOdometerReading() == 0);
		robot.move(4, false);
		robot.resetOdometer();
		assertTrue("Odometer should be zero after a reset", robot.getOdometerReading() == 0);
	}
	
	/**
	 * Tests that the robot can identify when it is at the exit and when it isn't
	 */
	@Test
	public void testIsAtExit_atExit() {
		//test robot not at exit --> doesn't start at exit
		assertTrue("robot doesn't start the exit", robot.isAtExit() == false);
		
		//test robot at the exit
		int[] position = new int[2];
		try{position = robot.getCurrentPosition();
		
		}catch(Exception e) {
			fail("Exception thrown trying to get the position");
		}
		controller.getMazeConfiguration().getFloorplan().setExitPosition(position[0], position[1]);
		assertTrue("robot is at the exit", robot.isAtExit() == true);
	}
	
	/**
	 * Tests that the method doesn't work with a failed sensor
	 */
	@Test
	public void testCanSeeThroughTheExitIntoEternity_UnsupportedSensor() {
		//fail left sensor, then try to call it
		robot.triggerSensorFailure(Robot.Direction.LEFT);
		try {
			robot.canSeeThroughTheExitIntoEternity(Robot.Direction.LEFT);
			fail("Exception should be thrown with a failed sensor"); //shouldn't reach this line
		}catch(UnsupportedOperationException e) {
			assertTrue("Exception should be thrown for failed sensor", true); //will be true if code is reached
		}
	}
	
	@Test
	public void testCanSeeThroughTheExitIntoEternity() {
		//have robot face exit
		int[] exitPosition = {0, 3};
		robot.setCurrentPosition(exitPosition);
		robot.setCurrentDirection(CardinalDirection.West);
		assertTrue("Should see into eternity", robot.canSeeThroughTheExitIntoEternity(Robot.Direction.FORWARD) == true);
		assertTrue("should not see into eternity", robot.canSeeThroughTheExitIntoEternity(Robot.Direction.BACKWARD) == false);
	}
	
	/**
	 * Tests that the robot can't use the room sensor when it is inactive
	 */
	@Test
	public void testIsInSideRoom_UnsupportedOperation() {
		robot.setRoomSensor(false);
		try {
			robot.isInsideRoom();
			fail("Exception should have been thrown");
		}catch(UnsupportedOperationException e) {
			assertTrue("Exception was thrown", true); //must be true if this code is reached
		}
	}
	
	/**
	 * Test the room sensor works outside a room
	 */
	@Test
	public void testIsInsideRoom_false() {
		//place robot outside a room --> starts outside it
		assertTrue("Robot is not inside a room", robot.isInsideRoom() == false);
		//battery should deplete
		robot.setBatteryLevel(10);
		robot.isInsideRoom();
		assertTrue("Battery should deplete", robot.getBatteryLevel() == 9);
	}
	
	/**
	 * Tests the room sensor is properly identified as working or not
	 */
	@Test
	public void testHasRoomSensor() {
		//should have room sensor to start
		assertTrue("Room sensor should start as active", robot.hasRoomSensor() == true);
		
		//test after changes were made
		robot.setRoomSensor(false);
		assertTrue("Room sensor should be inactive", robot.hasRoomSensor() == false);
		robot.setRoomSensor(true);
		assertTrue("Room sensor should be active", robot.hasRoomSensor() == true);
	}
	
	/**
	 * Tests the hasStopped() method
	 */
	@Test
	public void testHasStopped() {
		//test when it shouldn't have stopped --> at the beginning
		assertTrue("Robot should not be stopped at the beginning", robot.hasStopped() == false);
		
		//test when battery is depleted
		robot.setBatteryLevel(0);
		assertTrue("Robot should stop when battery is empty", robot.hasStopped() == true);
		
		//test when robot is at exit position
		int[] position = new int[2];
		try{position = robot.getCurrentPosition();
		
		}catch(Exception e) {
			fail("Exception thrown trying to get the position");
		}
		//set exit position to robot position
		controller.getMazeConfiguration().getFloorplan().setExitPosition(position[0], position[1]);
		assertTrue("Robot should stop when the exit is reached", robot.hasStopped() == true);
		
	}
	
	/**
	 * Tests distanceToObstacle() works when looking at a wall
	 */
	@Test
	public void testDistanceToObstacle_visibleObstacle() {
		//starting position is 1 away from a wall
		assertTrue("Should be 1", robot.distanceToObstacle(Robot.Direction.FORWARD) == 1);
	}
	
	/**
	 * Tests distanceToObstacle() doesn't function with a failed sensor
	 */
	@Test
	public void testDistanceToObstacle_unsupportedSensor() {
		robot.triggerSensorFailure(Robot.Direction.LEFT); //fail left sensor
		try {
			robot.distanceToObstacle(Robot.Direction.LEFT);
			fail("Exception should be thrown");
		}catch(UnsupportedOperationException e) {
			assertTrue("Exception thrown properly", true);
		}
	}
	
	/**
	 * Tests that the distanceToObstacle() works properly when looking through the exit
	 */
	@Test
	public void testDistanceToObstacle_invisibleObstacle() {
		//invisible object
		int[] exitPosition = {0, 3};
		robot.setCurrentPosition(exitPosition);
		robot.setCurrentDirection(CardinalDirection.West);
		assertTrue("Should be max value while looking through exit", robot.distanceToObstacle(Robot.Direction.FORWARD)== Integer.MAX_VALUE);
	
		//insufficient energy
		robot.setBatteryLevel(0);
		assertTrue("Shouldn't be enough energy", robot.distanceToObstacle(Robot.Direction.RIGHT) == 0);
	}
	
	/**
	 * Tests if the robot can properly read if it has a operating sensor or not
	 */
	@Test
	public void testHasOperationalSensor() {
		//all four should be active
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.LEFT) == true);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.RIGHT) == true);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.FORWARD) == true);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.BACKWARD) == true);
		
		//deactivate all four
		robot.triggerSensorFailure(Robot.Direction.LEFT);
		robot.triggerSensorFailure(Robot.Direction.RIGHT);
		robot.triggerSensorFailure(Robot.Direction.FORWARD);
		robot.triggerSensorFailure(Robot.Direction.BACKWARD);
		
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.LEFT) == false);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.RIGHT) == false);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.FORWARD) == false);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.BACKWARD) == false);
	}
	
	/**
	 * Tests that the sensor fails when triggerSensorFailure() is called
	 */
	@Test
	public void testTriggerSensorFailure() {
		//make sure it changes properly
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.FORWARD) == true);
		robot.triggerSensorFailure(Robot.Direction.FORWARD);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.FORWARD) == false);
	}
	
	/**
	 * Tests that repairFailedSensor() works properly
	 */
	@Test
	public void testRepairFailedSensor() { 
		//already operational
		robot.repairFailedSensor(Robot.Direction.BACKWARD);
		assertTrue("Sensor should be active", robot.hasOperationalSensor(Robot.Direction.BACKWARD) == true);
		
		//inactive to start
		robot.triggerSensorFailure(Robot.Direction.RIGHT);
		robot.repairFailedSensor(Robot.Direction.RIGHT);
		assertTrue("Sensor should be repaired", robot.hasOperationalSensor(Robot.Direction.RIGHT) == true);

	}
	
	/**
	 * Fully tests the rotate() method
	 */
	@Test
	public void testRotate() {
		//test sufficient energy
		robot.setCurrentDirection(CardinalDirection.North);
		robot.rotate(Turn.LEFT);
		assertTrue("Robot should have turned", robot.getCurrentDirection() == CardinalDirection.East);
		robot.rotate(Turn.RIGHT);
		assertTrue("Robot should have turned", robot.getCurrentDirection() == CardinalDirection.North);
		robot.rotate(Turn.AROUND);
		assertTrue("Robot should have turned", robot.getCurrentDirection() == CardinalDirection.South);
		//make sure battery depleted
		assertTrue("Battery should have depleted", robot.getBatteryLevel() == 2988);
		
		//test insufficient energy
		robot.setBatteryLevel(2);
		CardinalDirection currentDirection = robot.getCurrentDirection();
		robot.rotate(Turn.LEFT);
		assertTrue("robot should not trun b/c energy is low", robot.getCurrentDirection() == currentDirection);

		
		robot.setBatteryLevel(5);
		robot.rotate(Turn.AROUND);
		assertTrue("robot should not trun b/c energy is low", robot.getCurrentDirection() == currentDirection);
		
	}
	
	/**
	 * Tests the move() method fully
	 */
	@Test
	public void testMove() {
		int[] currentPosition = new int[2];
		//test where it works and battery depletes
		robot.setCurrentDirection(CardinalDirection.North);
		int[] newPosition = {2, 3};
		robot.setCurrentPosition(newPosition);
		robot.move(2, false);
		try {
			currentPosition = robot.getCurrentPosition();
		}catch(Exception e) {
			fail("No exception should be thrown");
		}
		assertTrue("Should have moved", currentPosition[0] == 2 && currentPosition[1] == 1);
		assertTrue("Battery should have depleted", robot.getBatteryLevel() == 2990);
		
		//test where it needs to stop
		robot.setBatteryLevel(10);
		robot.setCurrentDirection(CardinalDirection.East);
		int[] position = {1, 3};
		robot.setCurrentPosition(position);
		robot.move(2, false);
		assertTrue("should fail", robot.hasStopped() == true);
		
		//test insufficient energy
		robot.setBatteryLevel(0);
		int[] Position = {0, 0};
		robot.setCurrentPosition(Position);
		robot.move(1, false);
		try {
			currentPosition = robot.getCurrentPosition();
		}catch(Exception e) {
			fail("No exception should be thrown");
		}
		assertTrue("Robot should not move, not enough energy", currentPosition[0] == 0 && currentPosition[1] == 0);		
	}
	
	
	/**
	 * Tests the jump() method fully
	 */
	@Test
	public void testJump() {
		//test normally
		robot.setCurrentDirection(CardinalDirection.East);
		robot.move(1, false);
		try {
			robot.jump();
		}catch(Exception e){
			fail("No exception should be thrown");
		}
		int[] idealPosition = {3, 3};
		int[] currentPosition = new int[2];
		try {
			currentPosition = robot.getCurrentPosition();
		}catch(Exception e) {
			fail("No exception should be thrown");
		}
		assertTrue("Position should be on other side of the wall", currentPosition[0] == idealPosition[0] && currentPosition[1] == idealPosition[1]);
		
		//test battery depleted
		robot.setBatteryLevel(3000);
		int[] newPosition = {2, 3};
		robot.setCurrentPosition(newPosition);
		try{
			robot.jump();
		}catch(Exception e) {
			fail("Exception should not be thrown");
		}
		assertTrue("Battery level should deplete", robot.getBatteryLevel() == 2950);
		
		//test on exterior wall
		int[] position = {1, 0};
		robot.setCurrentDirection(CardinalDirection.North);
		robot.setCurrentPosition(position);
		try{
			robot.jump();
			fail("Exception should be thrown");
		}catch(Exception e) {
			assertTrue("Exception properly thrown", true);
		}
		
		//test with insufficient energy
		robot.setBatteryLevel(0);
		robot.setCurrentDirection(CardinalDirection.South);
		try{robot.jump();
		}catch(Exception e) {
			fail("No exception should be thrown");
		}
		try {
			currentPosition = robot.getCurrentPosition();
		}catch(Exception e) {
			fail("No exception should be thrown");
		}
		assertTrue("Position shouldn't change", currentPosition[0] == 1 && currentPosition[1] == 0);
	}
}
