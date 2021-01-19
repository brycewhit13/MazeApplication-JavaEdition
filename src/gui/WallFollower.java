package gui;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * Class: WallFollower
 * 
 * Responsibilities:
 * WallFollower has sensors to check if there is a wall in front and/or to the side
 * WallFollower can sense if it is in the exit position
 * WallFollower can adapt strategy if certain sensors fail or are repaired
 * 
 * Collaborators:
 * BasicRobot: The WallFollower driver needs a robot to drive, and this is provided by the BasicRobot class.
 * Controller: Although this is stored in the BasicRobot, this provides the maze for the WallFollower to drive through
 * 
 * @author Bryce Whitney
 */
public class WallFollower implements RobotDriver {
	//Private Variables
	private Robot robot; //reference to the robot
	private int width; //width of maze
	private int height; //height of maze
	private Distance distance;
	
	private float startingBattery; //the battery that the robot starts with
	
	private Lock lock = new ReentrantLock(); //used to prevent errors when using threads
	
	//Sensors --> true if the sensor works in the given direction, false otherwise
	private boolean activeSensorRight;
	private boolean activeSensorLeft;
	private boolean activeSensorForward;
	private boolean activeSensorBackward;
	
	/**
	 * Operates the robot with the default configurations
	 */
	public WallFollower() {
		activeSensorRight = true;
		activeSensorLeft = true;
		activeSensorForward = true;
		activeSensorBackward = true;
	}
	
	/**
	 * Assigns a robot platform to the driver. 
	 * The driver uses a robot to perform, this method provides it with this necessary information.
	 * @param r robot to operate
	 */
	@Override
	public void setRobot(Robot r) {
		robot  = r;
		//set starting battery as well
		startingBattery = 3000;
	}
	
	/**
	 * Returns the robot used by the driver
	 */
	public Robot getRobot() {
		return robot;
	}
	
	/**
	 * Provides the robot driver with information on the dimensions of the 2D maze
	 * measured in the number of cells in each direction.
	 * @param width of the maze
	 * @param height of the maze
	 * @precondition 0 <= width, 0 <= height of the maze.
	 */
	@Override
	public void setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	/**
	 * Returns the driver's dimensions
	 */
	public int[] getDimensions() {
		int[] dimensions = {width, height};
		return dimensions;
	}

	/**
	 * Provides the robot driver with information on the distance to the exit.
	 * Only some drivers such as the wizard rely on this information to find the exit.
	 * @param distance gives the length of path from current position to the exit.
	 * @precondition null != distance, a full functional distance object for the current maze.
	 */
	@Override
	public void setDistance(Distance distance) {
		this.distance = distance;
	}
	
	/**
	 * Returns the driver's distance object
	 */
	public Distance getDistance() {
		return distance;
	}

	/**
	 * Tells the driver to check its robot for operational sensor. 
	 * If one or more of the robot's distance sensor become 
	 * operational again after a repair operation, this method
	 * allows to make the robot driver aware of this change 
	 * and to bring its understanding of which sensors are operational
	 * up to date.  
	 */
	@Override
	public void triggerUpdateSensorInformation() {
		//check robot's sensor for each direction, update the fields accordingly
		//FORWARD
		if(robot.hasOperationalSensor(Direction.FORWARD))
			activeSensorForward = true;
		else
			activeSensorForward = false;
		
		//BACKWARD
		if(robot.hasOperationalSensor(Direction.BACKWARD))
			activeSensorBackward = true;
		else
			activeSensorBackward = false;
		
		//LEFT
		if(robot.hasOperationalSensor(Direction.LEFT))
			activeSensorLeft = true;
		else
			activeSensorLeft = false;
		
		//RIGHT
		if(robot.hasOperationalSensor(Direction.RIGHT))
			activeSensorRight = true;
		else
			activeSensorRight = false;
	}

	/**
	 * Drives the robot towards the exit given it exists and 
	 * given the robot's energy supply lasts long enough. 
	 * @return true if driver successfully reaches the exit, false otherwise
	 * @throws exception if robot stopped due to some problem, e.g. lack of energy
	 */
	@Override
	public boolean drive2Exit() throws Exception {
		boolean wallInFront; boolean leftWallPresent;
		//base case where is at the exit
		if(robot.isAtExit())
			return true;
		
		//check if robot starts in a room without a wall on the left
		if(robot.isInsideRoom()) {
				// if no walls around, go to closest one
				if(this.noWallsAnySide()) {
					while(!this.wallDirectlyInFront())
						robot.move(1, false);
					robot.rotate(Turn.RIGHT); //get wall on the left side
				}
		}
		
		while(!robot.isAtExit()) {
			//check if it has stopped
			if(robot.hasStopped())
				throw new Exception("Robot has stopped and is not functional");
			
			//check if there is a wall on the left and in front
			leftWallPresent = this.leftWallPresent();
			wallInFront = this.wallDirectlyInFront();

			//turn or move forward depending if there is a wall in the way
			if(leftWallPresent)
				if(wallInFront)
					robot.rotate(Turn.RIGHT);
				else
					robot.move(1, false);
			else {
				robot.rotate(Turn.LEFT);
				robot.move(1, false);
			}
		}
		
		//return true if the robot reaches the exit, otherwise return false
		return robot.isAtExit();
		
	}
	
	/**
	 * Checks if there are any walls around the driver
	 * @return true if there are no walls on all four sides, false if there is atleast one
	 */
	private boolean noWallsAnySide() {
		//if wall in front or left, return false
		if(this.leftWallPresent() || this.wallDirectlyInFront() || this.rightWallPresent() || this.backWallPresent())
			return false;
		return true; //if false wasn't returned when a wall was found 
	}
	
	/**
	 * Checks if there is a wall directly in front of the robot
	 */
	private boolean wallDirectlyInFront() {
		boolean hasWall = false;
		int distanceToWall;
		
		//Ensure only active sensors are used
		if(activeSensorForward) {
			distanceToWall = robot.distanceToObstacle(Direction.FORWARD);
			hasWall = (distanceToWall == 0); //true if the wall is right there
		}
		else if(activeSensorLeft) {
			robot.rotate(Turn.RIGHT); //turn robot to check if the wall is there
			distanceToWall = robot.distanceToObstacle(Direction.LEFT);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.LEFT); //turn robot back so it will move in the right direction after
		}
		else if(activeSensorRight){
			robot.rotate(Turn.LEFT);
			distanceToWall = robot.distanceToObstacle(Direction.RIGHT);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.RIGHT);
		}
		else {
			robot.rotate(Turn.AROUND);
			distanceToWall = robot.distanceToObstacle(Direction.BACKWARD);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.AROUND);
		}
		
		return hasWall;
	}
	
	/**
	 * Checks if there is a wall directly to the left of the robot
	 */
	private boolean leftWallPresent() {
		boolean hasLeftWall = false;
		int distanceToWall;
		
		if(activeSensorLeft) {
			distanceToWall = robot.distanceToObstacle(Direction.LEFT);
			hasLeftWall = (distanceToWall == 0); //true if the wall is right there
		}
		else if(activeSensorForward) {
			robot.rotate(Turn.LEFT);
			distanceToWall = robot.distanceToObstacle(Direction.FORWARD);
			hasLeftWall = (distanceToWall == 0); 
			robot.rotate(Turn.RIGHT);
		}
		else if(activeSensorBackward){
			robot.rotate(Turn.RIGHT);
			distanceToWall = robot.distanceToObstacle(Direction.BACKWARD);
			hasLeftWall = (distanceToWall == 0); 
			robot.rotate(Turn.LEFT);
		}
		else {
			robot.rotate(Turn.AROUND);
			distanceToWall = robot.distanceToObstacle(Direction.RIGHT);
			hasLeftWall = (distanceToWall == 0); 
			robot.rotate(Turn.AROUND);
		}
		
		return hasLeftWall;
	}
	
	/**
	 * Checks if there is a wall to the right of the robot
	 * @return true if there is a wall, false otherwise
	 */
	private boolean rightWallPresent() {
		boolean hasWall = false;
		int distanceToWall;
		
		if(activeSensorRight) {
			distanceToWall = robot.distanceToObstacle(Direction.RIGHT);
			hasWall = (distanceToWall == 0); //true if the wall is right there
		}
		else if(activeSensorForward) {
			robot.rotate(Turn.RIGHT);
			distanceToWall = robot.distanceToObstacle(Direction.FORWARD);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.LEFT);
		}
		else if(activeSensorBackward){
			robot.rotate(Turn.LEFT);
			distanceToWall = robot.distanceToObstacle(Direction.BACKWARD);
			hasWall = (distanceToWall == 0);
			robot.rotate(Turn.RIGHT);
		}
		else {
			robot.rotate(Turn.AROUND);
			distanceToWall = robot.distanceToObstacle(Direction.LEFT);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.AROUND);
		}
		
		return hasWall;
	}
	
	/**
	 * Checks if there is a wall behind the robot
	 * @return true if there is a wall, false if there isn't
	 */
	private boolean backWallPresent() {
		boolean hasWall = false;
		int distanceToWall;
		
		if(activeSensorBackward) {
			distanceToWall = robot.distanceToObstacle(Direction.BACKWARD);
			hasWall = (distanceToWall == 0); //true if the wall is right there
		}
		else if(activeSensorRight) {
			robot.rotate(Turn.RIGHT);
			distanceToWall = robot.distanceToObstacle(Direction.RIGHT);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.LEFT);
		}
		else if(activeSensorLeft){
			robot.rotate(Turn.LEFT);
			distanceToWall = robot.distanceToObstacle(Direction.LEFT);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.RIGHT);
		}
		else {
			robot.rotate(Turn.AROUND);
			distanceToWall = robot.distanceToObstacle(Direction.FORWARD);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.AROUND);
		}
		
		return hasWall;
	}

	/**
	 * Returns the total energy consumption of the journey, i.e.,
	 * the difference between the robot's initial energy level at
	 * the starting position and its energy level at the exit position. 
	 * This is used as a measure of efficiency for a robot driver.
	 */
	@Override
	public float getEnergyConsumption() {
		//get starting and ending values of energy
		float initialEnergy = startingBattery;
		float endEnergy = robot.getBatteryLevel();
		
		//energy consumed = initial energy - ending energy
		float energyConsumed = initialEnergy - endEnergy;
		
		return energyConsumed;
	}

	/**
	 * Returns the total length of the journey in number of cells traversed. 
	 * Being at the initial position counts as 0. 
	 * This is used as a measure of efficiency for a robot driver.
	 */
	@Override
	public int getPathLength() {
		return robot.getOdometerReading();
	}
	
	/**
	 * Updates the driver's understanding of the right sensor
	 * @param isActive true if the sensor is active, false otherwise
	 */
	public void setRightSensor(boolean isActive) {
		activeSensorRight = isActive;
	}
	
	/**
	 * Updates the driver's understanding of the left sensor
	 * @param isActive true if the sensor is active, false otherwise
	 */
	public void setLeftSensor(boolean isActive) {
		activeSensorLeft = isActive;
	}

	/**
	 * Updates the driver's understanding of the forward sensor
	 * @param isActive true if the sensor is active, false otherwise
	 */
	public void setForwardSensor(boolean isActive) {
		activeSensorForward = isActive;
	}

	/**
	 * Updates the driver's understanding of the backwards sensor
	 * @param isActive true if the sensor is active, false otherwise
	 */
	public void setBackwardSensor(boolean isActive) {
		activeSensorBackward = isActive;
	}
	
	// ACCESSOR METHODS TO READ IF THE SENSORS ARE ACTIVE --> True if they are
	
	public boolean getFrontSensor() {
		return activeSensorForward;
	}
	
	public boolean getLeftSensor() {
		return activeSensorLeft;
	}
	
	public boolean getRightSensor() {
		return activeSensorRight;
	}
	
	public boolean getBackwardSensor() {
		return activeSensorBackward;
	}

	@Override
	public Lock getLock() {
		return lock;
	}
}
