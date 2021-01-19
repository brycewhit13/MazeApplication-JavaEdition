package gui;

import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import generation.CardinalDirection;
import generation.Distance;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * Class: Wizard 
 * 
 * Responsibilities:
 * Wizard can identify which neighbor brings it closer to the exit
 * Wizard can jump over interior walls
 * Wizard can use sensors to check for walls
 * Wizard’s sensors can break and be repaired at runtime
 * 
 * Collaborators:
 * BasicRobot: The Wizard driver needs a robot to drive, and this is provided by the BasicRobot class. Without this, the Wizard would not be able to see the pathlength
 * Controller: Although this is stored in the BasicRobot, this provides the maze for the Wizard to drive through
 * Distance: Allows the Wizard algorithm to see what neighbor is closest to the exit
 * 
 * @author Bryce Whitney
 */
public class Wizard implements RobotDriver {
	//Private Variables
	private Robot robot;
	private int width;
	private int height;
	private Distance distance;
	
	private float startingBattery;
	
	private Lock lock = new ReentrantLock(); //used to prevent errors in the program when threads are operating
	
	//Sensors
	private boolean activeSensorRight;
	private boolean activeSensorLeft;
	private boolean activeSensorForward;
	private boolean activeSensorBackward;
	
	/**
	 * Creates a Wizard robot driver that will operate with the default configuration
	 */
	public Wizard() {
		//initialize all fields with the default configurations
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
		robot = r;
		//set starting battery level as well
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
	 * Returns the driver's distance object
	 */
	public Distance getDistance() {
		return distance;
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
		while(!robot.isAtExit()) {
			//check if the robot has stopped
			if(robot.hasStopped())
				throw new Exception("Robot has stopped and is not functional");
			
			//get current position
			int x = robot.getCurrentPosition()[0];
			int y = robot.getCurrentPosition()[1];
			CardinalDirection currentDir = robot.getCurrentDirection();
			
			//find distances for the neighbors
			// [ North, East, South, West ] is how the array is organized
			int[] possibleMoves = this.getNeighborCloserToExit(x, y);
			
			//check which move makes the most sense, record the direction
			CardinalDirection directionToMove = findBestMove(possibleMoves, currentDir);
			
			//have the robot turn towards where it needs to go
			if(currentDir != directionToMove) {
				rotateTowardsDirection(currentDir, directionToMove);
			}
			
			//move or jump to the next spot
			if(this.wallDirectlyInFront())
				robot.jump();
			else
				robot.move(1, false);			
		}
		
		//check and return if the robot is at the exit
		return robot.isAtExit();
	}
	
	
	/**
	 * Gives a (x',y') neighbor for given (x,y) that is closer to exit
	 * if it exists. 
	 * @param x is on the horizontal axis, {@code 0 <= x < width}
	 * @param y is on the vertical axis, {@code 0 <= y < height}
	 * @return array with neighbor coordinates if neighbor exists, null otherwise
	 */
	public int[] getNeighborCloserToExit(int x, int y) {
		assert(x < width && x >= 0 && y < height && y >= 0) : "Invalid position";
		// corner case, (x,y) is exit position
		if (distance.isExitPosition(x, y))
			return null;
		
		// find best candidate
		int dNorth; int dSouth; int dEast; int dWest;
		//make sure values stay in bounds, set to max value otherwise
		//NORTH
		if(y != 0)
			dNorth = distance.getDistanceValue(x, y - 1);
		else
			dNorth = Integer.MAX_VALUE;
		
		//EAST
		if(x != width - 1)
			dEast = distance.getDistanceValue(x + 1, y);
		else
			dEast = Integer.MAX_VALUE;
		
		//SOUTH
		if(y != height - 1)
			dSouth = distance.getDistanceValue(x, y + 1);
		else
			dSouth = Integer.MAX_VALUE;
		
		//WEST
		if(x != 0)
			dWest = distance.getDistanceValue(x - 1, y);
		else
			dWest = Integer.MAX_VALUE;
		
		int[] result = {dNorth, dEast, dSouth, dWest} ;
		return result;
	}

	/**
	 * Moves robot to the direction it is going to jump or move
	 */
	private void rotateTowardsDirection(CardinalDirection currentDir, CardinalDirection endDir) {
		switch(currentDir) {
		//switch from the current direction to the desired direction
		case North:
			switch(endDir){
			case South:
				robot.rotate(Turn.AROUND);
				return;
			case East:
				robot.rotate(Turn.LEFT);
				return;
			case West:
				robot.rotate(Turn.RIGHT);
				return;
			}
		case South:
			switch(endDir){
			case North:
				robot.rotate(Turn.AROUND);
				return;
			case East:
				robot.rotate(Turn.RIGHT);
				return;
			case West:
				robot.rotate(Turn.LEFT);
				return;
			}
		case East:
			switch(endDir){
			case North:
				robot.rotate(Turn.RIGHT);
				return;
			case South:
				robot.rotate(Turn.LEFT);
				return;
			case West:
				robot.rotate(Turn.AROUND);
				return;
			}
		case West:
			switch(endDir){
			case South:
				robot.rotate(Turn.RIGHT);
				return;
			case North:
				robot.rotate(Turn.LEFT);
				return;
			case East:
				robot.rotate(Turn.AROUND);
				return;
			}
		}
	}
	
	
	/**
	 * Returns which direction the robot should move
	 * @param possibleMoves array of all the possible moves
	 * @return dir the direction the robot driver will move
	 */
	private CardinalDirection findBestMove(int[] possibleMoves, CardinalDirection currentDirection) {
		//add ten to any distance that requires a jump
		//to account for the extra energy that will be lost 
		//FRONT MOVE
		if(this.wallDirectlyInFront()) {
			switch(currentDirection) {
			case North:
				if(possibleMoves[0] == Integer.MAX_VALUE) //means there is a wall and will need to jump
					break;
				possibleMoves[0] += 10;
				break;
			case South:
				if(possibleMoves[2] == Integer.MAX_VALUE)
					break;
				possibleMoves[2] += 10;
				break;
			case East:
				if(possibleMoves[1] == Integer.MAX_VALUE)
					break;
				possibleMoves[1] += 10;
				break;
			case West:
				if(possibleMoves[3] == Integer.MAX_VALUE)
					break;
				possibleMoves[3] += 10;
				break;
			}
		}
		//RIGHT MOVE
		if(this.rightWallPresent()) {
			switch(currentDirection) {
			case North:
				if(possibleMoves[3] == Integer.MAX_VALUE)
					break;
				possibleMoves[3] += 10;
				break;
			case South:
				if(possibleMoves[1] == Integer.MAX_VALUE)
					break;
				possibleMoves[1] += 10;
				break;
			case East:
				if(possibleMoves[0] == Integer.MAX_VALUE)
					break;
				possibleMoves[0] += 10;
				break;
			case West:
				if(possibleMoves[2] == Integer.MAX_VALUE)
					break;
				possibleMoves[2] += 10;
				break;
			}
		}
		//LEFT MOVE
		if(this.leftWallPresent()) {
			switch(currentDirection) {
			case North:
				if(possibleMoves[1] == Integer.MAX_VALUE)
					break;
				possibleMoves[1] += 10;
				break;
			case South:
				if(possibleMoves[3] == Integer.MAX_VALUE)
					break;
				possibleMoves[3] += 10;
				break;
			case East:
				if(possibleMoves[2] == Integer.MAX_VALUE)
					break;
				possibleMoves[2] += 10;
				break;
			case West:
				if(possibleMoves[0] == Integer.MAX_VALUE)
					break;
				possibleMoves[0] += 10;
				break;
			}
		}
		//BACK MOVE
		if(this.backWallPresent()) {
			switch(currentDirection) {
			case North:
				if(possibleMoves[2] == Integer.MAX_VALUE)
					break;
				possibleMoves[2] += 10;
				break;
			case South:
				if(possibleMoves[0] == Integer.MAX_VALUE)
					break;
				possibleMoves[0] += 10;
				break;
			case East:
				if(possibleMoves[3] == Integer.MAX_VALUE)
					break;
				possibleMoves[3] += 10;
				break;
			case West:
				if(possibleMoves[1] == Integer.MAX_VALUE)
					break;
				possibleMoves[1] += 10;
				break;
			}
		}
		
		//give battery back for the sensing that shouldn't be counted --> 4 for the wall detection in each direction
		robot.setBatteryLevel(robot.getBatteryLevel() + 4);
		
		//find best move 
		int bestMoveIndex = 0;
		for(int i = 1; i < possibleMoves.length; i++) {
			if(possibleMoves[i] < possibleMoves[bestMoveIndex])
				bestMoveIndex = i;
		}
		
		//convert index to a direction
		switch(bestMoveIndex) {
		case 0:
			return CardinalDirection.North;
		case 1:
			return CardinalDirection.East;
		case 2:
			return CardinalDirection.South;
		default: //will always be one of the 4, just to make java happy
			return CardinalDirection.West;
		}
	}
	
	/**
	 * Checks if there is a wall directly in front of the robot
	 */
	private boolean wallDirectlyInFront() {
		boolean hasWall = false;
		int distanceToWall;
		
		if(activeSensorForward) {
			distanceToWall = robot.distanceToObstacle(Direction.FORWARD);
			hasWall = (distanceToWall == 0); //true if the wall is right there
		}
		else if(activeSensorLeft) {
			robot.rotate(Turn.RIGHT);
			distanceToWall = robot.distanceToObstacle(Direction.LEFT);
			hasWall = (distanceToWall == 0); 
			robot.rotate(Turn.LEFT);
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
		//energy consumed = starting energy - ending energy
		float energyConsumed = startingBattery - robot.getBatteryLevel();
		return energyConsumed;
	}
	
	/**
	 * Returns the starting battery for the driver
	 * mainly used for testing purposes
	 * @return startingBattery the battery the robot had to start
	 */
	public float getStartingEnergy() {
		return startingBattery;
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

	public Lock getLock() {
		return lock;
	}
}
