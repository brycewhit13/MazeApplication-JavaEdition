package generation;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import generation.StubOrder.Builder;
import gui.Constants;

/**
 * Tests the MazeBuilder.java class
 * @author Bryce Whitney
 *
 */
public class MazeFactoryTest {
	
	//Private Variables
	private MazeFactory mazeFactory;
	private StubOrder stubOrder;
	private Maze maze;
	private Maze ellerMaze;
	
	/**
	 * construction of general maze to use for the tests
	 * One of each method
	 */
	@Before
	public void setup(){
		//initialize parameters to construct Order object, making it easy to change as well 
		int skillLevel = 4;
		boolean deterministic = false;
		Order.Builder builder = Order.Builder.Eller;
		
		mazeFactory = new MazeFactory(deterministic);
		stubOrder = new StubOrder(skillLevel, builder, deterministic);
		mazeFactory.order(stubOrder);
		mazeFactory.waitTillDelivered();
		maze = stubOrder.getMaze();
	}
	
	/**
	 * gets rid of everything so the next test will be fresh
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	/**
	 * Ensures nothing is set to null
	 */
	@Test
	public void testSetup() {
		assertNotNull(maze);
		assertNotNull(stubOrder);
		assertNotNull(mazeFactory);
	}
	
	/**
	 * Ensures both constructors create a mazes
	 */
	@Test
	public void testConstructors() {
		//no-args contructor
		MazeBuilder m1 = new MazeBuilder();
		assertNotNull(m1);
		//deterministic constructor
		MazeBuilder m2 = new MazeBuilder(true);
		MazeBuilder m3 = new MazeBuilder(true);
		MazeBuilder m4 = new MazeBuilder(false);
		//Should generate the same Maze with same fields
		assertTrue(m2.width == m3.width && m2.height == m3.height && m2.startx == m3.startx && 
				m2.starty == m3.starty && m2.floorplan == m3.floorplan && m2.dists == m3.dists && m2.random == m3.random);
		assertTrue(m2.random == m3.random);
		assertNotNull(m4);
	}
	
	/**
	 * Ensures there is one and only one exit to the Maze
	 */
	@Test
	public void testHasOneExit() {
		// initialize variables
		int numExits = 0;
		int distance = 0;
		Distance mazeDist = maze.getMazedists();
		
		//Search each position, checking to see if it is an exit position
		for(int i = 0; i < maze.getWidth(); i++) {
			for(int j = 0; j < maze.getHeight(); j++) {
				distance = mazeDist.getDistanceValue(i, j);
				if(distance == 1)
					numExits++; //increment if exit is found
			}
		}
		
		//Ensures there is only one exit, no more
		assertTrue("There should be one and only one exit", numExits == 1);
	}
	
	/**
	 * Ensures the exit is accessible
	 */
	@Test
	public void testExitIsAccessible() {
		Distance mazeDist = maze.getMazedists();
		//check if exit is accessible from every point
		for(int i = 0; i < maze.getWidth(); i++) {
			for(int j = 0; j < maze.getHeight(); j++) {
				assertNotNull(mazeDist.getDistanceValue(i, j));
				//If this is not null, that means there is a path from the point to the exit location
				//meaning it is accessible
			}
		}
	}

	/**
	 * Ensures exit is the farthest point from the starting point
	 */
	@Test
	public void testExitIsMaxDistance() {
		Distance mazeDist = maze.getMazedists();
		//find starting position
		int[] startPos = mazeDist.getStartPosition();
		mazeDist.computeDistances(maze.getFloorplan());
		assertTrue("The exit position should be as far away from the start as possible", mazeDist.getMaxDistance() == mazeDist.getDistanceValue(startPos[0], startPos[1])); 
		//checks that the max distance is the distance between start and exit position
	}
	
	/**
	 * Ensures there aren't too little walls to make the game boring. Goes off the
	 * assumption that if there were too many walls, the exit wouldn't be reachable
	 */
	@Test
	public void testSufficientWalls() {
		int numHorizontalWalls = 0;
		int numVerticalWalls = 0;
		int totalPossibleWalls = ((maze.getHeight() - 1) * maze.getWidth()) * 2;
		//count number of horizontal walls
		for(int i = 1; i < maze.getWidth(); i++) {
			for(int j = 0; j < maze.getHeight(); j++) {
				if(maze.getFloorplan().hasWall(i,  j, CardinalDirection.South)) // could have chosen any arbitrary direction
					numHorizontalWalls++;
			}
		}
		
		//count number of vertical walls
				for(int i = 0; i < maze.getWidth(); i++) {
					for(int j = 1; j < maze.getHeight(); j++) {
						if(maze.getFloorplan().hasWall(i,  j, CardinalDirection.West)) // could have chosen any arbitrary direction
							numVerticalWalls++;
					}
				}
		
		double percentageOfWalls = ((double)(numVerticalWalls + numHorizontalWalls) / totalPossibleWalls) * 100; // cast so integer division doesn't make it zero
		assertTrue("More than 30% of walls should be in the maze", percentageOfWalls > 30); // I feel at least 30% of possible walls should be present for the game to be fun and interesting
	}
	
	/**
	 * Ensures the dimensions of the maze are correct according to the skill level chosen
	 */
	@Test
	public void testDimensionsCorrect(){
		// get correct dimensions
		int width = Constants.SKILL_X[stubOrder.getSkillLevel()];
		int height = Constants.SKILL_Y[stubOrder.getSkillLevel()];
		// compare to actual dimensions
		assertTrue("Width should be correct", maze.getWidth() == width);
		assertTrue("Height should be correct", maze.getHeight() == height);
	}
	
	/**
	 * Ensures all the border walls are up
	 */
	@Test
	public void testBordersPresent() {
		int numBorders = 0;
		int totalBorders = 2*maze.getWidth() + 2*maze.getHeight();
		Wallboard wallboard; //define for later
		
		//check top and bottom
		for(int i = 0; i < maze.getWidth(); i++) {
			wallboard = new Wallboard(i, 0, CardinalDirection.North);
			if(maze.getFloorplan().isPartOfBorder(wallboard)) {
				numBorders++;
			}
			wallboard = new Wallboard(i, maze.getHeight() - 1, CardinalDirection.South);
			if(maze.getFloorplan().isPartOfBorder(wallboard)) {
				numBorders++;
			}
		}
		
		//check left and right
		for(int i = 0; i < maze.getHeight(); i++) {
			wallboard = new Wallboard(0, i, CardinalDirection.West);
			if(maze.getFloorplan().isPartOfBorder(wallboard)) {
				numBorders++;
			}
			wallboard = new Wallboard(maze.getWidth() - 1, i, CardinalDirection.East);
			if(maze.getFloorplan().isPartOfBorder(wallboard)) {
				numBorders++;
			}
		}
		
		//numBorders should be equal to the total number of borders
		assertTrue("All the boarders should be around the maze", numBorders == totalBorders);
	}
}
