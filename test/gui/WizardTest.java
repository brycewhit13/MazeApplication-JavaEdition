package gui;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import generation.Distance;
import generation.MazeFactory;
import generation.Order;
import gui.Robot.Direction;

/**
 * Set of tests to test the Wizard.java class
 * @author Bryce Whitney
 */
public class WizardTest {
	//Private variables for testing
	private Wizard driver;
	private BasicRobot robot;
	private Controller controller;
	private StateGenerating generator;
	
	/**
	 * Sets everything up that is needed to perform the tests
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
		controller.switchFromTitleToGenerating(2); //want basic skill level
		generator.factory.waitTillDelivered();
		robot.setMaze(controller);
		
		driver = new Wizard();
		driver.setRobot(robot);
		driver.setDimensions(controller.getMazeConfiguration().getWidth(), controller.getMazeConfiguration().getHeight());
		driver.setDistance(controller.getMazeConfiguration().getMazedists());
	}
	
	/**
	 * Breaks everything down to ensure a fresh test on the next go around
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception{}
	
	/**
	 * Check to see that the fields are set to the default values when
	 * the no-args constructor is called
	 */
	@Test
	public void testConstructor() {
		//Check variables are initialized correctly
		driver = new Wizard();
		assertTrue("Sensor should be true to start", driver.getFrontSensor() == true);
		assertTrue("Sensor should be true to start", driver.getLeftSensor() == true);
		assertTrue("Sensor should be true to start", driver.getRightSensor() == true);
		assertTrue("Sensor should be true to start", driver.getBackwardSensor() == true);
	}
	
	/**
	 * Tests that the setRobot() method works properly
	 */
	@Test
	public void testSetRobot() {
		Robot r = new BasicRobot();
		Robot c = new BasicRobot();
		//make sure it is different than r to start
		assertTrue("Should be the default driver robot", driver.getRobot() != r);
		driver.setRobot(r);
		assertTrue("Driver's robot should have been changed", driver.getRobot() == r);
		driver.setRobot(c);
		assertTrue("Driver's robot should have changed", driver.getRobot() == c);
		
		//make sure it actually changed
		assertTrue("Robot's should be different objects", r!= c);
	}
	
	/**
	 * Tests that the setDimensions() method works properly
	 * where both the dimensions are greater than 0
	 */
	@Test
	public void testSetDimensions() {
		//change dimensions and ensure they are implemented
		driver.setDimensions(10, 10);
		assertTrue("Dimensions should be 10 x 10", driver.getDimensions()[0] == 10 && driver.getDimensions()[1] == 10);
		driver.setDimensions(2, 30);
		assertTrue("Dimensions should be 2 x 30", driver.getDimensions()[0] == 2 && driver.getDimensions()[1] == 30);

	}
	
	/**
	 * Tests that the setDistance() method works properly
	 * assuming the distance method != null
	 */
	@Test
	public void testSetDistance() {
		//initialize distance objects
		Distance d1 = new Distance(2, 3);
		Distance d2 = new Distance(5, 5);
		assertTrue(d1 != d2); //can't be equal if tests below are going to work

		//make changes, ensure they are implemented
		driver.setDistance(d1);
		assertTrue("Distance object should be d1", driver.getDistance() == d1);
		driver.setDistance(d2);
		assertTrue("Distance object should be d2", driver.getDistance() == d2);
		
		//make sure it actually changed
		assertTrue("Distance objects should be different", d1 != d2);
	}
	
	/**
	 * Tests that the triggerUpdateSensor() method works properly
	 */
	@Test
	public void testTriggerUpdateSensor() {
		//true to false
		driver.setForwardSensor(true);
		driver.getRobot().triggerSensorFailure(Direction.FORWARD);
		driver.triggerUpdateSensorInformation();
		assertTrue("Forward sensor should be down", driver.getFrontSensor() == false);
		
		//false to false --> no change
		driver.getRobot().triggerSensorFailure(Direction.FORWARD);
		driver.triggerUpdateSensorInformation();
		assertTrue("Forward sensor should still be false", driver.getFrontSensor() == false);
		
		//false to true --> doesn't update until method is called
		driver.getRobot().repairFailedSensor(Direction.FORWARD);
		assertTrue("Forward sensor shouldn't update until told to", driver.getFrontSensor() == false);
		driver.triggerUpdateSensorInformation();
		assertTrue("Sensor should have updated", driver.getFrontSensor() == true);
	}
	
	/**
	 * Tests that getEnergyConsumption() returns the
	 * correct amount of energy used when called
	 */
	@Test
	public void testGetEnergyConsumption() {
		//check energy for known maze
		int[] position = {12, 14};
		robot.setCurrentPosition(position);
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Robot should make it to the exit");
		}
		
		// 2 sense, 2 turns and 2 move --> 2(1) + 2(3) + 2(5) = 18
		assertTrue("18 energy should have been used to reach the exit", driver.getEnergyConsumption() == 18);
	}
	
	/**
	 * Tests that getPathLength() returns the correct path length when called at the exit position
	 */
	@Test
	public void testGetPathLength_atExit() {
		//place it at the to the exit
		int[] position = {10, 14};
		robot.setCurrentPosition(position);
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}

		assertTrue("Should be one move to the exit", driver.getPathLength() == 0);
	}
	
	/**
	 * Tests the getPathLength() method updates properly after a move
	 */
	@Test
	public void testGetPathLength_NextToExit() {
		//place robot next to the exit
		int[] position = {11, 14};
		robot.setCurrentPosition(position);
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}
		
		assertTrue("Should be one move to the exit", driver.getPathLength() == 1);
	}
	
	/**
	 * Tests the path length is recorded properly when a jump occurs
	 */
	@Test
	public void testGetPathLength_WithJump() {
		int[] position = {9, 14};
		robot.setCurrentPosition(position);
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}
		
		assertTrue("Should be one move to the exit", driver.getPathLength() == 1);
	}
	/**
	 * Tests that the set methods for the sensors in each direction works properly
	 */
	@Test
	public void testSetSensors() {
		driver.setBackwardSensor(false);
		driver.setForwardSensor(false);
		driver.setLeftSensor(false);
		driver.setRightSensor(false);
		assertTrue("All sensors should be false", driver.getBackwardSensor() == false && driver.getFrontSensor() == false &&
				driver.getLeftSensor() == false && driver.getRightSensor() == false);
	}
	
	/**
	 * Tests that the robot reaches the exit when drive2Exit() is called
	 */
	@Test
	public void testDrive2Exit_NoJumpNecessary() {
		boolean reachedExit = false; //turns true if exit is reached
		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Exit should be reached");
		}
		
		assertTrue("The exit should have easily been reached", reachedExit);
	}
	
	/**
	 * Tests that the robot can still reach the exit with broken sensors
	 */
	@Test
	public void testDrive2Exit_BrokenSensors() {
		//cause sensors to be turned off
		robot.triggerSensorFailure(Direction.FORWARD);
		robot.triggerSensorFailure(Direction.RIGHT);
		driver.triggerUpdateSensorInformation();
		
		boolean reachedExit = false; //turns true if exit is reached
		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Exit should be reached");
		}
		
		assertTrue("The exit should have easily been reached", reachedExit);
	}
	
	/**
	 * Tests that the wizard utilizes the ability to jump when it needs to
	 */
	@Test
	public void testDrive2Exit_JumpNeeded() {
		boolean reachedExit = false; //will only become true if the exit is reached
		//force a jump
		int[] position = {9, 14};
		robot.setCurrentPosition(position);
		
		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Robot should reach the exit");
		}
		
		assertTrue("Robot should have made it to the exit", reachedExit);
		//make sure the path length is short because of the jump
		assertTrue("Path length should be shortened due to the jump", driver.getPathLength() == 1);
	}
	
	/**
	 * Tests that an exception is thrown when the robot runs out of battery
	 */
	@Test
	public void testDrive2Exit_FailureInsufficientBattery() {
		boolean reachedExit = false; //turns true iff exit is reached
		//make the battery low
		robot.setBatteryLevel(10);
		
		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			assertTrue("Exception was thrown properly", true);
			return; //end method before it is failed below
		}
		
		//fail if exception is not thrown
		fail("Exception should have been thrown due to a lack of energy");
	}
	
	/**
	 * Tests that the WallFollower algorithm minimizes the path length to the best
	 * of its ability when solving the maze. This is tested on a known maze
	 */
	@Test
	public void testUsesMinPathlength() {
		boolean madeToExit = false;
		try {
			madeToExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Robot should make it to the exit, not stop");
		}
		
		//should easily make it to the exit
		assertTrue(madeToExit = true);
		//path length should be 37
		assertTrue("Wizard should save time by jumping", driver.getPathLength() == 37);
	}
}
