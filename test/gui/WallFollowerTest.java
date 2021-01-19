package gui;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import generation.Distance;
import generation.MazeFactory;
import generation.Order;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * Set of tests to test the WallFollower.java class
 * @author Bryce Whitney
 */
public class WallFollowerTest {
	//Private variables for testing
	private WallFollower driver;
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
		
		driver = new WallFollower();
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
		driver = new WallFollower();
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
		assertTrue("Robot should have changed", driver.getRobot() == c);
		
		//make sure robots are different
		assertTrue("Robots should be different objects", r != c);
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
	public void testGetEnergyConsumption_atExit() {
		//place robot at the exit
		int[] position = {10, 14};
		robot.setCurrentPosition(position);
		
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Robot should already be at the exit");
		}
		
		//check energy consumption
		assertTrue("Should cost nothing", driver.getEnergyConsumption() == 0);
	}
	
	/**
	 * Ensures the energy consumed is used after moves are used
	 */
	@Test
	public void testGetEnergyConsumption_movesNeeded() {
		//check energy for known maze
		int[] position = {12, 14};
		robot.setCurrentPosition(position);
		
		//face robot towards the exit
		robot.rotate(Turn.AROUND);
		robot.setBatteryLevel(robot.getBatteryLevel() + 6); //return battery that was used in this operation
		
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Robot should make it to the exit");
		}
		
		// 4 sense (forward and left for each move), 2 moves, and one sense to check if it is in a room at the beginning
		// 4(1) + 2(5) + 1 = 15
		assertTrue("15 energy should have been used to reach the exit", driver.getEnergyConsumption() == 15);
	} 
	
	/**
	 * Tests that getPathLength() returns the correct path length when called at the exit
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
	 * Tests the path length is correct when called after drive2Exit is completed
	 */
	@Test
	public void testGetPathLength_MovesRequired() {
		//place robot next to the exit
		int[] position = {12, 14};
		robot.setCurrentPosition(position);
		
		//face the robot towards the exit
		robot.rotate(Turn.AROUND);
		
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}
		
		//check path length
		assertTrue("Should be one move to the exit", driver.getPathLength() == 2);
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
	 * Tests drive2Exit works when already at the exit
	 */
	@Test
	public void testDrive2Exit_atExit() {
		boolean reachedExit = false; //only turns true if exit is reached
		
		//place robot next to the exit
		int[] position = {10, 14};
		robot.setCurrentPosition(position);

		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}

		//check exit is reached
		assertTrue("Exit should be reached", reachedExit);
	}

	/**
	 * Tests that drive to exit fails when the robot runs out of battery
	 */
	@Test
	public void testDrive2Exit_insufficientEnergy() {
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
	 * Tests drive2Exit when it should work properly
	 */
	@Test
	public void testDrive2Exit_fromBeginning() {
		boolean reachedExit = false; //only turns true if exit is reached

		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}

		//check exit is reached
		assertTrue("Exit should be reached", reachedExit);
	}
	
	/**
	 * Test drive2Exit for a really long run, but the battery is extremely high
	 */
	@Test
	public void testDrive2Exit_LongRun() {
		boolean reachedExit = false; //only turns true if exit is reached
		
		//place robot next to the exit
		int[] position = {11, 14};
		robot.setCurrentPosition(position);
		
		//large battery
		robot.setBatteryLevel(1000000);

		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Should reach the exit");
		}

		//check exit is reached
		assertTrue("Exit should be reached", reachedExit);
	}
	
	/**
	 * Tests drive2Exit when sensors fail as well
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
	 * Tests drive2Exit when sensors fail as well
	 */
	@Test
	public void testDrive2Exit_inRoom() {
		boolean reachedExit = false; //turns true if exit is reached
		
		//place robot in a room
		int[] position = {6, 5};
		robot.setCurrentPosition(position);
				
		try {
			reachedExit = driver.drive2Exit();
		} catch (Exception e) {
			fail("Exit should be reached");
		}

		assertTrue("The exit should have easily been reached", reachedExit);
	}
	
	
	/**
	 * Tests that the WallFollower algorithm minimizes the path length to the best
	 * of its ability when solving the maze. This is tested on a known maze
	 */
	@Test
	public void testUsesMinPathlength() {
		try {
			driver.drive2Exit();
		} catch (Exception e) {
			fail("Robot should reach the exit");
		}		
		
		//based off of known maze
		assertTrue(driver.getPathLength() == 133);
	}
}
