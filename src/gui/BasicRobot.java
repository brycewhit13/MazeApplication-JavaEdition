package gui;

import generation.CardinalDirection;
import gui.Constants.UserInput;
import gui.Robot.Direction;
import gui.Robot.Turn;

/**
 * Class: BasicRobot
 * 
 * Responsibilities:
 * - Robot can turn 90 degrees at a time to both the right and left
 * - Robot can use a sensor to check if there is an obstacle, exit, or anything else is in front of it
 * - The Robot has a battery level that gets depleted with different movements
 * 
 * Collaborators: 
 * - Controller: Provides a maze for the robot to work within
 * - RobotDriver: Operates the Robot using one of two driver algorithms: The wall-follower or wizard algorithm  
 * 
 * @author Bryce Whitney
 */
public class BasicRobot implements Robot{
	
	//Private Variables
	private final int SENSING_ENERGY = 1; //energy required to use a sensor
	private final int QUARTER_TURN_ENERGY = 3; //energy required to turn 90 degrees
	private final int MOVE_FORWARD_ENERGY = 5; //energy required to move forward
	private final int JUMP_WALL_ENERGY = 50; //energy required to jump
	
	//true if the robot has working sensors in the direction, false if they are broken
	private boolean activeSensorRight;
	private boolean activeSensorLeft;
	private boolean activeSensorForward;
	private boolean activeSensorBackward;
	
	private Controller controller; //provides the robot access to the maze
	private float batteryLevel;
	private int odometer; //how far the robot has traveled
	private boolean hasRoomSensor;
	private boolean hasStopped;
	private CardinalDirection currentDirection;
	private int[] currentPosition;
	
	/** 
	 * Describes all possible turns that a robot can do when it rotates on the spot.
	 * Left is 90 degrees left, right is 90 degrees right, turn around is 180 degrees.
	 */
	public enum Turn { LEFT, RIGHT, AROUND };
	
	/**
	 * Describes all possible directions from the point of view of the robot,
	 * i.e., relative to its current forward position.
	 * Mind the difference between the robot's point of view
	 * and cardinal directions in terms of north, south, east, west.
	 */
	public enum Direction { LEFT, RIGHT, FORWARD, BACKWARD };
	
	/**
	 * Provides the current position as (x,y) coordinates for the maze cell as an array of length 2 with [x,y].
	 * @postcondition 0 <= x < width, 0 <= y < height of the maze. 
	 * @return array of length 2, x = array[0], y=array[1]
	 * @throws Exception if position is outside of the maze
	 */
	public int[] getCurrentPosition() throws Exception {
		//get the position, width, and height
		int width = controller.getMazeConfiguration().getWidth();
		int height = controller.getMazeConfiguration().getHeight();
		int[] position = currentPosition;
		
		// check if position is invalid
		if(position[0] < 0 || position[0] >= height || position[1] < 0 || position[1] >= width)
			throw new Exception("Invalid position");
		//return the position
		return position;
	}
	
	/**
	 * Provides the current cardinal direction.
	 * @return cardinal direction is robot's current direction in absolute terms
	 */	
	public CardinalDirection getCurrentDirection() {
		return currentDirection;
	}
	
	/**
	 * Provides the robot with a reference to the controller to cooperate with.
	 * The robot memorizes the controller such that this method is most likely called only once
	 * and for initialization purposes. The controller serves as the main source of information
	 * for the robot about the current position, the presence of walls, the reaching of an exit.
	 * The controller is assumed to be in the playing state.
	 * @param controller is the communication partner for robot
	 * @precondition controller != null, controller is in playing state and has a maze
	 */
	public void setMaze(Controller controller) {	

		//initialize private variables
		this.controller = controller;
		batteryLevel = 3000;
		odometer = 0;
		hasStopped = false;
		activeSensorRight = true;
		activeSensorLeft = true;
		activeSensorForward = true;
		activeSensorBackward = true;
		hasRoomSensor = true;
		currentDirection = controller.getCurrentDirection();
		currentPosition = controller.getMazeConfiguration().getStartingPosition();
	}
	
	/**
	 * Returns the current battery level.
	 * The robot has a given battery level (energy level) 
	 * that it draws energy from during operations. 
	 * The particular energy consumption is device dependent such that a call 
	 * for distance2Obstacle may use less energy than a move forward operation.
	 * If battery level <= 0 then robot stops to function and hasStopped() is true.
	 * @return current battery level, level is > 0 if operational. 
	 */
	public float getBatteryLevel() {
		return batteryLevel;
	}
	
	/**
	 * Sets the current battery level.
	 * The robot has a given battery level (energy level) 
	 * that it draws energy from during operations. 
	 * The particular energy consumption is device dependent such that a call 
	 * for distance2Obstacle may use less energy than a move forward operation.
	 * If battery level <= 0 then robot stops to function and hasStopped() is true.
	 * @param level is the current battery level
	 * @precondition level >= 0 
	 */
	public void setBatteryLevel(float level) {
		batteryLevel = level;
	}
	
	/** 
	 * Gets the distance traveled by the robot.
	 * The robot has an odometer that calculates the distance the robot has moved.
	 * Whenever the robot moves forward, the distance 
	 * that it moves is added to the odometer counter.
	 * The odometer reading gives the path length if its setting is 0 at the start of the game.
	 * The counter can be reset to 0 with resetOdomoter().
	 * @return the distance traveled measured in single-cell steps forward
	 */
	public int getOdometerReading() {
		return odometer;
	}
	
	/** 
     * Resets the odomoter counter to zero.
     * The robot has an odometer that calculates the distance the robot has moved.
     * Whenever the robot moves forward, the distance 
     * that it moves is added to the odometer counter.
     * The odometer reading gives the path length if its setting is 0 at the start of the game.
     */
	public void resetOdometer() {
		odometer = 0;
	}
	
	/**
	 * Gives the energy consumption for a full 360 degree rotation.
	 * Scaling by other degrees approximates the corresponding consumption. 
	 * @return energy for a full rotation
	 */
	public float getEnergyForFullRotation() {
		return 4 * QUARTER_TURN_ENERGY;
	}
	
	/**
	 * Gives the energy consumption for moving forward for a distance of 1 step.
	 * For simplicity, we assume that this equals the energy necessary 
	 * to move 1 step backwards and that scaling by a larger number of moves is 
	 * approximately the corresponding multiple.
	 * @return energy for a single step forward
	 */
	public float getEnergyForStepForward() {
		return MOVE_FORWARD_ENERGY;
	}
	
	
	///////////////////////////////////////////////////////////////////
	/////////////////// Sensors   /////////////////////////////////////
	///////////////////////////////////////////////////////////////////
	
	
	/**
	 * Tells if current position (x,y) is right at the exit but still inside the maze. 
	 * Used to recognize termination of a search.
	 * @return true if robot is at the exit, false otherwise
	 */
	public boolean isAtExit() {
		//get current position
		int[] position = currentPosition;
		
		//Checks if the current position is the exit position
		if(controller.getMazeConfiguration().getFloorplan().isExitPosition(position[0], position[1]))
				return true;
		return false;
	}
	
	/**
	 * Tells if a sensor can identify the exit in the given direction relative to 
	 * the robot's current forward direction from the current position.
	 * @return true if the exit of the maze is visible in a straight line of sight
	 * @throws UnsupportedOperationException if robot has no sensor in this direction
	 */
	public boolean canSeeThroughTheExitIntoEternity(Robot.Direction direction) throws UnsupportedOperationException {
		if(batteryLevel < SENSING_ENERGY) {
			hasStopped = true;
			return false;
		}
		
		//get position and cardinal direction
		CardinalDirection cardinalDirection = currentDirection;
		int[] position = currentPosition;
		
		//check if robot has sensor for each direction
		switch(direction) {
		case LEFT:
			if(activeSensorLeft) {
				//90 degree turn to get the direction to the left
				cardinalDirection = cardinalDirection.rotateClockwise();

				//check  if exit is present and deplete battery
				batteryLevel -= SENSING_ENERGY;
				return isExitInFrontOfRobot(cardinalDirection, position);
			}
			else
				throw new UnsupportedOperationException("No left sensor");
		case RIGHT:
			if(activeSensorRight) {
				//270 degree turn to get the direction to the right
				cardinalDirection = cardinalDirection.rotateClockwise().oppositeDirection();
				
				//check  if exit is present and deplete battery
				batteryLevel -= SENSING_ENERGY;
				return isExitInFrontOfRobot(cardinalDirection, position);
			}
			else
				throw new UnsupportedOperationException("No right sensor");
		case FORWARD:
			if(activeSensorForward) {
				//check  if exit is present and deplete battery
				batteryLevel -= SENSING_ENERGY;
				return isExitInFrontOfRobot(cardinalDirection, position);
			}
			else
				throw new UnsupportedOperationException("No forward sensor");
		case BACKWARD:
			if(activeSensorBackward) {
				//180 degree turn to get the direction backwards
				cardinalDirection = cardinalDirection.oppositeDirection();

				//check  if exit is present and deplete battery
				batteryLevel -= SENSING_ENERGY;
				return isExitInFrontOfRobot(cardinalDirection, position);
			}
			else
				throw new UnsupportedOperationException("No backward sensor");
		}
		
		return false; // no exit if not one of the four directions
	}
	
	/**
	 * Helper method for canSeeThroughTheExitIntoEternity(Direction direction) to see if the 
	 * exit is straight ahead of the robot at the given time
	 * @param cardinalDirection direction of interest to check for the exit
	 * @param position current position of the robot
	 * @return isPresent true if the exit is in front of the robot
	 */
	private boolean isExitInFrontOfRobot(CardinalDirection cardinalDirection, int[] position) {
		//iterate through maze, varies differently bases on the direction
		switch(cardinalDirection) {
		case East:
			for(int i  = position[0]; i < controller.getMazeConfiguration().getWidth(); i++) {
				if(controller.getMazeConfiguration().getFloorplan().isExitPosition(i, position[1]))
					if(!controller.getMazeConfiguration().isValidPosition(i+1, position[1]))
						return true; //return true if exit is present
			}
			break;
		case West:
			for(int i  = position[0]; i >= 0; i--) {
				if(controller.getMazeConfiguration().getFloorplan().isExitPosition(i, position[1]))
					if(!controller.getMazeConfiguration().isValidPosition(i-1, position[1]))
						return true; //return true if exit is present
			}
			break;
		case North:
			for(int i  = position[1]; i >= 0; i--) {
				if(controller.getMazeConfiguration().getFloorplan().isExitPosition(position[0], i))
					if(!controller.getMazeConfiguration().isValidPosition(position[0], i-1))
						return true; //return true if exit is present
			}
			break;
		case South:
			for(int i  = position[1]; i < controller.getMazeConfiguration().getHeight(); i++) {
				if(controller.getMazeConfiguration().getFloorplan().isExitPosition(position[0], i))
					if(!controller.getMazeConfiguration().isValidPosition(position[0], i+1))
						return true; //return true if exit is present
			}
			break;
		}
		return false; // no exit was found
	}
	
	/**
	 * Tells if current position is inside a room. 
	 * @return true if robot is inside a room, false otherwise
	 * @throws UnsupportedOperationException if not supported by robot
	 */	
	public boolean isInsideRoom() throws UnsupportedOperationException {
		//get current position
		int[] position = currentPosition; 
		
		//check if the position is in the room, as long as the robot supports the operation
		if(hasRoomSensor) {
			batteryLevel -= SENSING_ENERGY;
			hasStopped(); // terminates if battery level is depleted
			
			//return true if the robot is in a room, false if it is not
			return controller.getMazeConfiguration().getFloorplan().isInRoom(position[0], position[1]);
		}
		else
			throw new UnsupportedOperationException("Room sensor not present"); //throw exception if sensor is broken
	}
	
	/**
	 * Tells if the robot has a room sensor.
	 */
	public boolean hasRoomSensor() {
		return hasRoomSensor;
	}
	
	/**
	 * Tells if the robot has stopped for reasons like lack of energy, hitting an obstacle, etc.
	 * @return true if the robot has stopped, false otherwise
	 */
	public boolean hasStopped() {
		//check if battery it out
		if(batteryLevel <= 0) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Tells the distance to an obstacle (a wall) 
	 * in the given direction.
	 * The direction is relative to the robot's current forward direction.
	 * Distance is measured in the number of cells towards that obstacle, 
	 * e.g. 0 if the current cell has a wallboard in this direction, 
	 * 1 if it is one step forward before directly facing a wallboard,
	 * Integer.MaxValue if one looks through the exit into eternity.
	 * @param direction specifies the direction of the sensor
	 * @return number of steps towards obstacle if obstacle is visible 
	 * in a straight line of sight, Integer.MAX_VALUE otherwise
	 * @throws UnsupportedOperationException if the robot does not have
	 * an operational sensor for this direction
	 */
	public int distanceToObstacle(Robot.Direction direction) throws UnsupportedOperationException {
		if(batteryLevel < SENSING_ENERGY) {
			hasStopped = true;
			return 0;
		}
		
		//initialize counter to track steps
		int numSteps = 0;
		
		//get position and cardinal direction
		int[] position = currentPosition;
		CardinalDirection cardinalDirection = currentDirection;
		
		//check if robot has sensor for this direction --> throw exception if not
		switch(direction) {
		case LEFT:
			//90 degree turn to get direction to the left
			cardinalDirection = cardinalDirection.rotateClockwise(); 
			
			//check if sensor is active
			if(activeSensorLeft == false)
				throw new UnsupportedOperationException("No left sensor");
			
			//calculate number of steps to the obstacle and deplete battery
			batteryLevel -= SENSING_ENERGY;
			numSteps = getStepsToObstacle(cardinalDirection, position);
			return numSteps;
			
		case RIGHT:
			//270 degree turn to get the direction to the right
			cardinalDirection = cardinalDirection.rotateClockwise().oppositeDirection();
			
			//check if sensor is active
			if(activeSensorRight == false)
				throw new UnsupportedOperationException("No right sensor");
			
			//calculate number of steps to the obstacle and deplete battery
			batteryLevel -= SENSING_ENERGY;
			numSteps = getStepsToObstacle(cardinalDirection, position);
			return numSteps;
			
		case FORWARD:
			//check if sensor is active
			if(activeSensorForward == false)
				throw new UnsupportedOperationException("No forward sensor");
			
			//calculate number of steps to the obstacle and deplete battery
			batteryLevel -= SENSING_ENERGY;
			numSteps = getStepsToObstacle(cardinalDirection, position);
			return numSteps;
			
		case BACKWARD:
			//get the opposite direction
			cardinalDirection = cardinalDirection.oppositeDirection();
			
			//check if sensor is active
			if(activeSensorBackward = false)
				throw new UnsupportedOperationException("No backward sensor");
			
			//calculate number of steps to the obstacle and deplete battery
			batteryLevel -= SENSING_ENERGY;
			numSteps = getStepsToObstacle(cardinalDirection, position);
			return numSteps;
		}
		
		//Return max value if for some reason the switch statement isn't entered
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Helper method for getDistanceToObstacle(Direction direction) to calculate the number of steps to the obstacle
	 * @param cardinalDirection the direction the robot is facing in the maze
	 * @param position current position in the maze
	 * @return numSteps the number of steps to the obstacle
	 */
	private int getStepsToObstacle(CardinalDirection cardinalDirection, int[] position) {
		int numSteps = 0;
		
		//iterate through maze, varies differently bases on the direction
		switch(cardinalDirection) {
		case East:
			for(int i  = position[0]; i < controller.getMazeConfiguration().getWidth(); i++) {
				if(controller.getMazeConfiguration().hasWall(i, position[1], cardinalDirection))
					return numSteps; //return the number of steps to reach the wall
				numSteps++;
			}
			break;
		case West:
			for(int i  = position[0]; i >= 0; i--) {
				if(controller.getMazeConfiguration().hasWall(i, position[1],cardinalDirection))
					return numSteps; //return the number of steps to reach the wall
				numSteps++;
			}
			break;
		case North:
			for(int i  = position[1]; i >= 0; i--) {
				if(controller.getMazeConfiguration().hasWall(position[0], i, cardinalDirection))
					return numSteps; //return the number of steps to reach the wall
				numSteps++;
			}
			break;
		case South:
			for(int i  = position[1]; i < controller.getMazeConfiguration().getHeight(); i++) {
				if(controller.getMazeConfiguration().hasWall(position[0], i, cardinalDirection))
					return numSteps; //return the number of steps to reach the wall
				numSteps++;
			}
			break;
		}
		
		//if it never reaches an obstacle, return Integer.MAX_VALUE
		return Integer.MAX_VALUE;
	}
	
	/**
	 * Tells if the robot has an operational distance sensor for the given direction.
	 * The interface is generic and may be implemented with robots 
	 * that are more or less equipped with sensor or have sensors that
	 * are subject to failures and repairs. 
	 * The purpose is to allow for a flexible robot driver to adapt
	 * its driving strategy according the features it
	 * finds supported by a robot.
	 * @param direction specifies the direction of the sensor
	 * @return true if robot has operational sensor, false otherwise
	 */
	public boolean hasOperationalSensor(Robot.Direction direction) {
		//determine which sensor to check
		switch(direction) {
		case LEFT:
			return activeSensorLeft;
		case RIGHT:
			return activeSensorRight;
		case FORWARD:
			return activeSensorForward;
		case BACKWARD:
			return activeSensorBackward;
		}
		
		//if isn't one of the four directions, definitely doesn't have a sensor for it
		return false;
	}
	
	/**
	 * Makes the robot's distance sensor for the given direction fail.
	 * Subsequent calls to measure the distance to an obstacle in 
	 * this direction will return with an exception.
	 * If the robot does not have a sensor in this direction, 
	 * the method does not have any effect.
	 * Only distance sensors can fail, the room sensor and exit
	 * sensor if installed are always operational.
	 * @param direction specifies the direction of the sensor
	 */
	public void triggerSensorFailure(Robot.Direction direction) {
		//figure out which direction needs to be flagged
		switch(direction) {
		case LEFT:
			activeSensorLeft = false;
			break;
		case RIGHT:
			activeSensorRight = false;
			break;
		case FORWARD:
			activeSensorForward = false;
			break;
		case BACKWARD:
			activeSensorBackward = false;
			break;
		}
	}
	
	/**
	 * Makes the robot's distance sensor for the given direction
	 * operational again. 
	 * A method call for an already operational sensor has no effect
	 * but returns true as the robot has an operational sensor
	 * for this direction.
	 * A method call for a sensor that the robot does not have
	 * has not effect and the method returns false.
	 * @param direction specifies the direction of the sensor
	 * @return true if robot has operational sensor, false otherwise
	 */
	public boolean repairFailedSensor(Robot.Direction direction) {
		//Make sensor operational again, return true
		switch(direction) {
		case LEFT:
			activeSensorLeft = true;
			return true;
		case RIGHT:
			activeSensorRight = true;
			return true;
		case FORWARD:
			activeSensorForward = true;
			return true;
		case BACKWARD:
			activeSensorBackward = true;
			return true;
		}
		
		//if robot doesn't have sensor
		return false;
	}
	
	
	///////////////////////////////////////////////////////////////////
	/////////////////// Actuators /////////////////////////////////////
	///////////////////////////////////////////////////////////////////
	
	
	/**
	 * Turn robot on the spot for amount of degrees. 
	 * If robot runs out of energy, it stops, 
	 * which can be checked by hasStopped() == true and by checking the battery level. 
	 * @param direction to turn and relative to current forward direction. 
	 */
	public void rotate(Robot.Turn turn) {
		//checks if there is sufficient energy
		if(((turn == Robot.Turn.RIGHT || turn == Robot.Turn.LEFT) && batteryLevel < QUARTER_TURN_ENERGY) ||
				(turn == Robot.Turn.AROUND && batteryLevel < QUARTER_TURN_ENERGY * 2)) {
			hasStopped = true;
			return;
		}
		
		switch(turn) {
		case LEFT:
			
			//change direction
			currentDirection = currentDirection.rotateClockwise();
			
			//turn robot on screen
			controller.states[2].keyDown(UserInput.Left, 0);
			
			//deplete battery life
			batteryLevel -= QUARTER_TURN_ENERGY;
			if(batteryLevel <= 0)
				hasStopped = true; // stop robot
			break;
		case RIGHT:
			//turn robot on screen
			controller.states[2].keyDown(UserInput.Right, 0);
			
			//change direction
			currentDirection = currentDirection.rotateClockwise().oppositeDirection();
			
			//deplete battery life
			batteryLevel -= QUARTER_TURN_ENERGY;
			if(batteryLevel <= 0)
				hasStopped = true;
			break;
		case AROUND:
			//turn robot on screen
			controller.states[2].keyDown(UserInput.Right, 0);
			controller.states[2].keyDown(UserInput.Right, 0);
			
			//change direction
			currentDirection = currentDirection.oppositeDirection();
			
			//deplete battery life
			batteryLevel -= QUARTER_TURN_ENERGY * 2;
			if(batteryLevel <= 0)
				hasStopped = true;
			break;
		}
	}
	
	/**
	 * Moves robot forward a given number of steps. A step matches a single cell.
	 * If the robot runs out of energy somewhere on its way, it stops, 
	 * which can be checked by hasStopped() == true and by checking the battery level. 
	 * If the robot hits an obstacle like a wall, it depends on the mode of operation
	 * what happens. If an algorithm drives the robot, it remains at the position in front 
	 * of the obstacle and also hasStopped() == true as this is not supposed to happen.
	 * This is also helpful to recognize if the robot implementation and the actual maze
	 * do not share a consistent view on where walls are and where not.
	 * If a user manually operates the robot, this behavior is inconvenient for a user,
	 * such that in case of a manual operation the robot remains at the position in front
	 * of the obstacle but hasStopped() == false and the game can continue.
	 * @param distance is the number of cells to move in the robot's current forward direction 
	 * @param manual is true if robot is operated manually by user, false otherwise
	 * @precondition distance >= 0
	 */
	public void move(int distance, boolean manual) {
		while(!hasStopped() && distance > 0) {
			//check for insufficient battery
			if(batteryLevel < MOVE_FORWARD_ENERGY) {
				hasStopped = true;
				return;
			}
			
			//check if it should stop without worrying about sensors being available or not
			if(controller.getMazeConfiguration().hasWall(currentPosition[0], currentPosition[1], currentDirection) 
					|| batteryLevel < MOVE_FORWARD_ENERGY) {
				//only stop if it is an automatic run
				if(!manual)
					hasStopped = true;
			}
			else {
				//make the robot move on screen
				controller.states[2].keyDown(UserInput.Up, 0);
				//make changes to position based on the cardinal direction
				switch(currentDirection) {
				case East:
					currentPosition[0]++;
					odometer++;
					distance--;
					batteryLevel-=MOVE_FORWARD_ENERGY;
					break;
				case West:
					currentPosition[0]--;
					odometer++;
					distance--;
					batteryLevel-=MOVE_FORWARD_ENERGY;
					break;
				case North:
					currentPosition[1]--;
					odometer++;
					distance--;
					batteryLevel-=MOVE_FORWARD_ENERGY;
					break;
				case South:
					currentPosition[1]++;
					odometer++;
					distance--;
					batteryLevel-=MOVE_FORWARD_ENERGY;
					break;
				}
			}
		}
	}
	
	/**
	 * Makes robot move in a forward direction even if there is a wall
	 * in front of it. In this sense, the robot jumps over the wall
	 * if necessary. The distance is always 1 step and the direction
	 * is always forward.
	 * @throws Exception is thrown if the chosen wall is an exterior wall 
	 * and the robot would land outside of the maze that way. 
	 * The current location remains set at the last position, 
	 * same for direction but the game is supposed
	 * to end with a failure.
	 */
	public void jump() throws Exception {
		//check if battery is too low
		if(batteryLevel < JUMP_WALL_ENERGY) {
			hasStopped = true;
			return;
		}
		
		//jump on screen
		controller.states[2].keyDown(UserInput.Jump, 0);
		
		switch(currentDirection) {
		case East:
			//check if the wall is an exterior wall
			if(currentPosition[0] == controller.getMazeConfiguration().getWidth() - 1) {
				hasStopped = true;
				throw new Exception("Can't jump an exterior wall");
			}
			else {
				currentPosition[0]++;
				odometer++;
				//deplete energy, but restore it from the move operation
				batteryLevel -= JUMP_WALL_ENERGY;
			}
			break;
		case West:
			//check if the wall is an exterior wall
			if(currentPosition[0] == 0) {
				hasStopped = true;
				throw new Exception("Can't jump an exterior wall");
			}
			else {
				currentPosition[0]--;
				odometer++;
				//deplete energy, but restore it from the move operation
				batteryLevel -= JUMP_WALL_ENERGY;
			}
			break;
		case North:
			//check if the wall is an exterior wall
			if(currentPosition[1] == 0) {
				hasStopped = true;
				throw new Exception("Can't jump an exterior wall");
			}
			else {
				currentPosition[1]--;
				odometer++;
				//deplete energy, but restore it from the move operation
				batteryLevel -= JUMP_WALL_ENERGY;
			}
			break;
		case South:
			//check if the wall is an exterior wall
			if(currentPosition[1] == controller.getMazeConfiguration().getHeight() - 1) {
				hasStopped = true;
				throw new Exception("Can't jump an exterior wall");
			}
			else {
				currentPosition[1]++;
				odometer++;
				//deplete energy, but restore it from the move operation
				batteryLevel -= JUMP_WALL_ENERGY;
			}
			break;
		}
	}
	
/////////////////////////METHODS FOR TESTING PURPOSES//////////////////////////
	
	/**
	 * Get the controller for the robot for testing purposes
	 */
	protected Controller getController(){
		return controller;
	}
	
	/**
	 * Set position for testing purposes
	 */
	protected void setCurrentPosition(int[] position) {
		currentPosition[0] = position[0];
		currentPosition[1] = position[1];
	}
	
	/**
	 * Set cardinal direction for testing purposes
	 * @param direction
	 */
	protected void setCurrentDirection(CardinalDirection direction) {
		currentDirection = direction;
	}
	
	/**
	 * set hasRoomSensor for testing purposes
	 * @param isActive true if the room sensor is active, false if it is broken
	 */
	protected void setRoomSensor(boolean isActive) {
		hasRoomSensor = isActive;
	}
}



