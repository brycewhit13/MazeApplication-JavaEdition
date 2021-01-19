package generation;

public class StubOrder implements Order{
	//private variables
	private int progress;
	private int skillLevel;
	private boolean isPerfect;
	private Maze mazeConfig;
	private Order.Builder builder;
	
	/**
	 * constructs a StubOrder object, initializes all the fields except the progress
	 * @param skill, desired skill level
	 * @param builder, desired algorithm to develop the maze
	 * @param perfect, whether the maze should be a perfect maze or not
	 */
	public StubOrder(int skill, Order.Builder builder, boolean perfect) {
		this.skillLevel = skill;
		this.builder = builder ;
		this.isPerfect = perfect ;
		this.mazeConfig = new MazeContainer() ;
	}
	
	/**
	 * Lists all maze generation algorithms that are supported
	 * by the maze factory (Eller needs to be implemented for P2)
	 *
	 */
	enum Builder {DFS, Prim, Kruskal, Eller} ;
	
	/**
	 * Gives the required skill level, range of values 0,1,2,...,15
	 */
	public int getSkillLevel() {
		return skillLevel;
	}
	
	/**
	 * sets the skill level to the desired level indicated by the parameter
	 * @param skill, the desired skill level
	 */
	public void setSkillLevel(int skill) {
		skillLevel = skill;
	}
	

	/** 
	 * Gives the requested builder algorithm, possible values 
	 * are listed in the Builder enum type.
	 */
	public Order.Builder getBuilder() {
		return builder;
	}
	
	/**
	 * sets the builder algorithm to the desired one
	 * @param builder, the desired builder algorithm
	 */
	public void setBuilder(Order.Builder builder) {
		this.builder = builder;
	}
	
	/**
	 * Describes if the ordered maze should be perfect, i.e. there are 
	 * no loops and no isolated areas, which also implies that 
	 * there are no rooms as rooms can imply loops
	 */
	public boolean isPerfect() {
		return isPerfect;
	}
	
	/**
	 * returns whether the maze is perfect
	 * @param perfect whether the maze should be perfect or not
	 */
	public void setIsPerfect(boolean perfect) {
		isPerfect = perfect;
	}
	
	/**
	 * Delivers the produced maze. 
	 * This method is called by the factory to provide the 
	 * resulting maze as a MazeConfiguration.
	 * @param the maze
	 */
	public void deliver(Maze mazeConfig) {
		this.mazeConfig = mazeConfig;
	}
	
	/**
	 * @return mazeConfig the resulting maze as a MazeConfiguration
	 */
	public Maze getMaze() {
		return mazeConfig;
	}
	
	/**
	 * Provides an update on the progress being made on 
	 * the maze production. This method is called occasionally
	 * during production, there is no guarantee on particular values.
	 * Percentage will be delivered in monotonously increasing order,
	 * the last call is with a value of 100 after delivery of product.
	 * @param current percentage of job completion
	 */
	public void updateProgress(int percentage) {
		progress = percentage;
	}
	
	/**
	 * returns the amount of progress made on the maze production
	 * @return progress
	 */
	public int getProgress() {
		return progress;
	}
}
