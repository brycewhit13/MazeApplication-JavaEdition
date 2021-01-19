package generation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import generation.StubOrder.Builder;
import gui.Constants;

/**
 * A set of tests to test the MazeBuilderEller.java class
 * @author Bryce Whitney
 *
 */
public class MazeBuilderEllerTest extends MazeBuilderEller{

	//Private Variables
	private StubOrder order;
	private int width;
	private int height;
	private int[][] sets;
	private Thread buildThread;
	
	private MazeBuilderEller maze;
	
	@Before
	public void setup(){
		boolean deterministic = false;
		int skillLevel = 8;
		order = new StubOrder(skillLevel, Order.Builder.Eller, deterministic);
		height = Constants.SKILL_Y[skillLevel];
		width = Constants.SKILL_X[skillLevel];
		maze = new MazeBuilderEller(deterministic);
		maze.buildOrder(order);
		buildThread = new Thread(maze);
		try {
			buildThread.join();
		}
		catch(Exception e) {
			System.out.println("MazeBuilder.wailTillDelivered: join synchronization with builder thread lead to an exception");
		}
		sets = maze.getMazeSets();
	}
	
	/**
	 * Breaks everything down afterwards so the next test will be fresh
	 * @throws Exception
	 */
	@After public void tearDown() throws Exception {}
	
	/**
	 * Ensures the setup executed as planned
	 */
	@Test
	public void testSetup(){
		assertNotNull(maze);
	}
	
	/**
	 * Ensures that the constructors in MazeBuilderEller.java work properly
	 */
	@Test
	public void testConstructors() {
		MazeBuilderEller m1 = new MazeBuilderEller();
		MazeBuilderEller m2 = new MazeBuilderEller(true);
		MazeBuilderEller m3 = new MazeBuilderEller(true);
		//ensure they will be the same maze
		assertEquals("Deterministic maze's should be equal", m2.random, m3.random);
		//ensure they were created
		assertNotNull(m1);
		assertNotNull(m2);
		assertNotNull(m3);
	}
	
	/**
	 * Ensures the getMazeSets() method works properly
	 */
	public void testGetMazeSets() {
		maze.generatePathways();
		assertTrue(maze.getMazeSets() != null);
		int[][]sets = maze.getMazeSets();
		// same dimensions
		assertTrue("dimensions should be equal", sets.length == maze.getMazeSets().length && sets[0].length == maze.getMazeSets()[0].length);
		for(int i = 0; i < sets.length; i++) {
			for(int j = 0; j < sets[0].length; j++) {
				//same values all around
				assertTrue(sets[i][j] == maze.getMazeSets()[i][j]);
			}
		}
	}
	
	/**
	 * Tests to see that every cell is a part of the same set 
	 * by the end of the pathway generation
	 */
	@Test
	public void testGeneratePathways_SetsEqual() {
		maze.generatePathways();
		int[][] sets = maze.getMazeSets();
		for(int i = 0; i < width; i++) {
			for(int j = 0; j < height; j++) {
				//check every set is the same through the maze
				assertTrue(sets[0][0] == sets[i][j]);
			}
		}
	}
	
	/**
	 * Tests to see that the matrix containing the sets for each 
	 * cell is the proper size
	 */
	@Test
	public void testGeneratePathways_SizeOfSetMatrix() {
		maze.generatePathways();
		assertTrue("Number of columns should match the Width", maze.getMazeSets().length == width);
		assertTrue("umber of rows should math the height", maze.getMazeSets()[0].length == height);
	}
	
	/**
	 * Tests the shouldJoin method when nothing needs to be merged
	 */
	@Test
	public void testShouldJoin_NotNecessary() {
		maze.generatePathways();
		//set values of mazeSets to be equal
		maze.setMazeSets(0, 0, 10);
		maze.setMazeSets(1, 0, 10);
		maze.setRow(0); maze.setCol(0);
		//ensure false is returned because they are the same value
		boolean shouldJoin = maze.shouldJoinSets();
		assertTrue("Don't want to join the sets", shouldJoin == false);
	}
	
	/**
	 * Tests the shouldJoin method when it is sometimes necessary to join them
	 */
	@Test
	public void testShouldJoin_Necessary() {
		maze.generatePathways();
		//set values of mazeSets
		maze.setMazeSets(0, 0, 10);
		maze.setMazeSets(1, 0, 1);
		maze.setRow(0); maze.setCol(0);
		//need enough cases because is random chance
		boolean shouldJoin = false;
		for(int i = 0; i < 50; i++) { //50 chances, very small chance it would fail due to randomness
			shouldJoin = maze.shouldJoinSets();
			if(shouldJoin)
				break; //want to break when it works
		}	
		assertTrue("want to join the sets", shouldJoin == true);
	}
	
	/**
	 * Tests to make sure that if two cells in the same set try to merge, 
	 * they are in the same set still
	 */
	@Test
	public void testJoinSets_SameValue() {
		maze.generatePathways();
		//set values of mazeSets to be equal
		maze.setMazeSets(0, 0, 10);
		maze.setMazeSets(1, 0, 10);
		maze.setRow(0); maze.setCol(0);
		//should still be equal after joining
		maze.joinSets();
		int[][] sets = maze.getMazeSets();
		assertTrue("two set's values should not change if they were equal prior", sets[0][0] == sets[1][0] && sets[0][0] == 10);
	}
	
	/**
	 * Tests to make sure values sets that are different merge properly
	 */
	@Test
	public void testJoinSets_DifferentValues() {
		maze.generatePathways();
		//set values of mazeSets to be different
		maze.setMazeSets(0, 0, 10);
		maze.setMazeSets(1, 0, 1);
		maze.setRow(0); maze.setCol(0);
		//should be equal after joining, not before
		int[][] sets = maze.getMazeSets();
		assertTrue(sets[0][0] != sets[1][0]);
		maze.joinSets();
		sets = maze.getMazeSets();
		assertTrue("Set values should be equal after merging", sets[0][0] == sets[1][0] && sets[0][0] == 10);
	}
	
	/**
	 * Tests that the last row of sets is merged properly if needed
	 */
	@Test
	public void testJoinLastRow_NeedJoining() {
		maze.generatePathways();
		//mess up something 
		maze.setMazeSets(0, height - 1, 7);
		maze.setMazeSets(2, height - 1, 12);
		maze.setRow(width - 1); maze.setCol(0);
		int[][] sets = maze.getMazeSets();
		//shouldn't be true to start
		assertTrue("Shouldn't be true until after joinLastRow() is called", sets[2][height - 1] != sets[0][height - 1]);
		//should be after joining the last row
		maze.joinLastRow();
		sets = maze.getMazeSets();
		assertTrue("Set values should be equal", sets[0][height - 1] == sets[2][height - 1]);
		//should also be equal to any other set
		assertTrue("Last row should be equal to every other set value in the maze", sets[0][0] == sets[0][height - 1]);
	}
	
	/**
	 * Tests that the last row doesn't change, 
	 * even if the method is called unnecessarily
	 */
	@Test
	public void testJoinLastRow_AlreadyJoined() {
		maze.generatePathways();
		//mess up something 
		maze.setMazeSets(0, height - 1, 0);
		maze.setMazeSets(2, height - 1, 0);
		maze.setRow(width - 1); maze.setCol(0);
		int[][] sets = maze.getMazeSets();
		//should be true to start
		assertTrue("Set values should be equal to start", sets[2][height - 1] == sets[0][height - 1]);
		//should be after joining the last row
		maze.joinLastRow();
		sets = maze.getMazeSets();
		assertTrue("Last row should be equal to every other set value in the maze", sets[0][height - 1] == sets[2][height - 1]);
	}
	
	/**
	 * Tests that generateRows() operates properly
	 */
	@Test
	public void testGenerateRow() {
		maze.generatePathways();
		//simulate parts of untouched row
		maze.setMazeSets(2, 2, 0);
		maze.setMazeSets(1, 2, 0);
		maze.setRow(2); maze.setCol(0);
		//assert set up properly
		int[][] sets = maze.getMazeSets();
		assertTrue("The matrix containing the sets should be initialized properly", maze.getMazeSets()[1][2] == 0 && maze.getMazeSets()[2][2] == 0);
		//shouldn't be equal after generating the row
		maze.generateRow();
		sets = maze.getMazeSets();
		assertTrue("Each spot should have a new, unique set value", sets[1][2] != 0 && sets[2][2] != 0);
	}
}
