/**
 * 
 */
package gui;

import generation.MazeFactory;
import generation.Order;
import gui.Constants.UserInput;
import gui.Robot.Direction;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 * This class is a wrapper class to startup the Maze game as a Java application
 * 
 * This code is refactored code from Maze.java by Paul Falstad, www.falstad.com, Copyright (C) 1998, all rights reserved
 * Paul Falstad granted permission to modify and use code for teaching purposes.
 * Refactored by Peter Kemper
 * 
 * TODO: use logger for output instead of Sys.out
 */
public class MazeApplication extends JFrame {

	// not used, just to make the compiler, static code checker happy
	private static final long serialVersionUID = 1L;
	
	private static JPanel startingScreen;
	private static JPanel gameButtons;
	private static JPanel gameButtons2;

	/**
	 * Constructor
	 */
	public MazeApplication() {
		init(null);
	}

	/**
	 * Constructor that loads a maze from a given file or uses a particular method to generate a maze
	 * @param parameter can identify a generation method (Prim, Kruskal, Eller)
     * or a filename that stores an already generated maze that is then loaded, or can be null
	 */
	public MazeApplication(String parameter) {
		init(parameter);
	} 

	/**
	 * Instantiates a controller with settings according to the given parameter.
	 * @param parameter can identify a generation method (Prim, Kruskal, Eller)
	 * or a filename that contains a generated maze that is then loaded,
	 * or can be null
	 * @return the newly instantiated and configured controller
	 */
	 Controller createController(String parameter) {
	    // need to instantiate a controller to return as a result in any case
	    Controller result = new Controller() ;
	    String msg = null; // message for feedback
	    // Case 1: no input
	    if (parameter == null) {
	        msg = "MazeApplication: maze will be generated with a randomized algorithm."; 
	    }
	    // Case 2: Prim
	    else if ("Prim".equalsIgnoreCase(parameter))
	    {
	        msg = "MazeApplication: generating random maze with Prim's algorithm.";
	        result.setBuilder(Order.Builder.Prim);
	    }
	    // Case 3 a and b: Eller, Kruskal or some other generation algorithm
	    else if ("Kruskal".equalsIgnoreCase(parameter))
	    {
	    	msg = "MazeApplication: generating random maze with Kruskal's algorithm.";
	        result.setBuilder(Order.Builder.Kruskal);
	    }
	    else if ("Eller".equalsIgnoreCase(parameter))
	    {
	    	// TODO: for P2 assignment, please add code to set the builder accordingly
	        msg = "MazeApplication: generating random maze with Eller's algorithm.";
	        result.setBuilder(Order.Builder.Eller);
	    }
	    // Case 4: a file
	    else {
	        File f = new File(parameter) ;
	        if (f.exists() && f.canRead())
	        {
	            msg = "MazeApplication: loading maze from file: " + parameter;
	            result.setFileName(parameter);
	            return result;
	        }
	        else {
	            // None of the predefined strings and not a filename either: 
	            msg = "MazeApplication: unknown parameter value: " + parameter + " ignored, operating in default mode.";
	        }
	    }
	    // controller instanted and attributes set according to given input parameter
	    // output message and return controller
	    System.out.println(msg);
	    return result;
	}

	/**
	 * Initializes some internals and puts the game on display.
	 * @param parameter can identify a generation method (Prim, Kruskal, Eller)
     * or a filename that contains a generated maze that is then loaded, or can be null
	 */
	private void init(String parameter) {
		startingScreen = new JPanel();
		startingScreen.setLayout(new BoxLayout(startingScreen, BoxLayout.PAGE_AXIS));
		gameButtons = new JPanel();
		gameButtons.setLayout(new FlowLayout());
		gameButtons2 = new JPanel();
		gameButtons2.setLayout(new FlowLayout());
		
		// instantiate a game controller and add it to the JFrame
	    Controller controller = createController(parameter);
		// instantiate a key listener that feeds keyboard input into the controller
		// and add it to the JFrame
		KeyListener kl = new SimpleKeyListener(this, controller) ;
		addKeyListener(kl) ;
		// set the frame to a fixed size for its width and height and put it on display
		setSize(400, 400) ;
		setVisible(true) ;
		// focus should be on the JFrame of the MazeApplication and not on the maze panel
		// such that the SimpleKeyListener kl is used
		setFocusable(true) ;
		Order.Builder[] builders = {Order.Builder.DFS, Order.Builder.Prim, Order.Builder.Eller};
		String[] driverStrings = {"Manual", "Wizard", "WallFollower"};
	    String[] algorithmStrings = {"DFS", "Prim", "Eller"};
	    String[] skillLevels = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", 
	    		"11", "12", "13", "14", "15",};
	    // instantiate combo boxes and start button
	    JComboBox drivers = new JComboBox(driverStrings);
	    JComboBox algorithms = new JComboBox(algorithmStrings);
	    JComboBox skillLevel = new JComboBox(skillLevels);
	    JButton startButton = new JButton("Start");
	    //add JComboBoxes to the screen
	    startingScreen.add(drivers);
	    drivers.setVisible(true);
	    startingScreen.add(algorithms);
	    algorithms.setVisible(true);
	    startingScreen.add(skillLevel);
	    skillLevel.setVisible(true);
	    //create ActionListener for the start button
	    ActionListener startGeneratingMaze = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		//add buttons to the screen
	    		add(controller.getPanel());
	    		add(gameButtons, BorderLayout.PAGE_START);
	    		add(gameButtons2, BorderLayout.PAGE_END);
	    		//attach driver if one is chosen and robot to the controller
	    		Robot robot = new BasicRobot();
	    		//solves an issue where couldn't do manual every again after selecting a algorithm once
	    		controller.setRobotAndDriver(robot, null);
	    		RobotDriver robotDriver;
	    		if(drivers.getSelectedIndex() == 1) {
	    			robotDriver = new Wizard();
	    			robotDriver.setRobot(robot);
	    			controller.setRobotAndDriver(robot, robotDriver);
	    		}
	    		else if(drivers.getSelectedIndex() == 2) {
	    			robotDriver = new WallFollower();
	    			robotDriver.setRobot(robot);
	    			controller.setRobotAndDriver(robot, robotDriver);
	    		}
	    		
	    		//set builder
	    		controller.setBuilder(builders[algorithms.getSelectedIndex()]);
	    		//switch to generating and make sure robot is not null
	    		controller.switchFromTitleToGenerating(skillLevel.getSelectedIndex());
	    		//get rid of the title screen
	    		startingScreen.setVisible(false);
	    	}
	    };
	    startButton.addActionListener(startGeneratingMaze);
	    startingScreen.add(startButton);
	    startButton.setVisible(true);
	    
	    //create in game buttons
	    //RIGHT
	    JButton breakRight = new JButton("Fail Right");
	    ActionListener failRight = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		failSensor(Direction.RIGHT, controller);
	    	}
	    };
	    breakRight.setFocusable(false);
	    
	    JButton fixRight = new JButton("Fix Right");
	    ActionListener fixRightSide = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		fixSensor(Direction.RIGHT, controller);
	    	}
	    };
	    fixRight.setFocusable(false);
	    
	    //LEFT
	    JButton breakLeft = new JButton("Fail Left");
	    ActionListener failLeft = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		failSensor(Direction.LEFT, controller);
	    	}
	    };
	    breakLeft.setFocusable(false);
	    
	    JButton fixLeft = new JButton("Fix Left");
	    ActionListener fixLeftSide = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		fixSensor(Direction.LEFT, controller);
	    	}
	    };
	    fixLeft.setFocusable(false);
	    
	    //FORWARD
	    JButton breakForward = new JButton("Fail Forward");
	    ActionListener failForward = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		failSensor(Direction.FORWARD, controller);
	    	}
	    };
	    breakForward.setFocusable(false);
	    
	    JButton fixFront = new JButton("Fix Forward");
	    ActionListener fixForward = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		fixSensor(Direction.FORWARD, controller);
	    	}
	    };
	    fixFront.setFocusable(false);
	    
	    //BACKWARD
	    JButton breakBackward = new JButton("Fail Back");
	    ActionListener failBackward = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		failSensor(Direction.BACKWARD, controller);
	    	}
	    };
	    breakBackward.setFocusable(false);
	    
	    JButton fixBack = new JButton("Fix Back");
	    ActionListener fixBackward = new ActionListener() {
	    	public void actionPerformed(ActionEvent e) {
	    		fixSensor(Direction.BACKWARD, controller);
	    	}
	    };
	    fixBack.setFocusable(false);
	    
	    //Add Action Listeners
	    breakRight.addActionListener(failRight);
	    breakLeft.addActionListener(failLeft);
	    breakForward.addActionListener(failForward);
	    breakBackward.addActionListener(failBackward);
	    fixBack.addActionListener(fixBackward);
	    fixRight.addActionListener(fixRightSide);
	    fixLeft.addActionListener(fixLeftSide);
	    fixFront.addActionListener(fixForward);
	    
	    //Add buttons to the JPanel and make them visible
	    gameButtons.add(breakLeft);
	    breakLeft.setVisible(true);
	    gameButtons.add(breakForward);
	    breakForward.setVisible(true);
	    gameButtons.add(breakRight);
	    breakRight.setVisible(true);
	    gameButtons.add(breakBackward);
	    breakBackward.setVisible(true);
	    
	    gameButtons2.add(fixLeft);
	    fixLeft.setVisible(true);
	    gameButtons2.add(fixFront);
	    fixFront.setVisible(true);
	    gameButtons2.add(fixRight);
	    fixRight.setVisible(true);
	    gameButtons2.add(fixBack);
	    fixBack.setVisible(true);
	    
	    //add to JFrame
	    add(startingScreen);
		// start the game, hand over control to the game controller
		controller.start();
		
		startingScreen.setVisible(true);
		gameButtons.setVisible(true);
		gameButtons2.setVisible(true);
		setVisible(true);
		gameButtons.setBounds(0, 0, 10, 10);
		gameButtons2.setBounds(0, 0, 390, 390);
		add(gameButtons);
		setVisible(true);
	}
	
	private void failSensor(Direction dir, Controller controller) {
		//create a thread
		Thread thread = new Thread() {
			public void run() {
				try {
					sleep(3000);
				}catch(Exception e) {
					//ignore exception
				}
				
				//lock the sensors from being seen when it would cause an error
				controller.getDriver().getLock().lock();
				try {
					//fail sensors
					controller.getRobot().triggerSensorFailure(dir);
					controller.getDriver().triggerUpdateSensorInformation();
				}catch(Exception e) {
					//ignore exception
				}finally {
					controller.getDriver().getLock().unlock();
				}
			}
		};
		//start the thread
		thread.start();
	}
	
	private void fixSensor(Direction dir, Controller controller) {
		//create a thread
		Thread t = new Thread() {
			public void run() {
				//sleep for 3 seconds before updating it
				try {
					sleep(3000);
				}catch(Exception e) {
					//ignore
				}
				
				//lock
				controller.getDriver().getLock().lock();
				
				//repair sensors
				try {
					controller.getRobot().repairFailedSensor(dir);
					controller.getDriver().triggerUpdateSensorInformation();
				}catch(Exception e) {
					//ignore
				}finally {
					controller.getDriver().getLock().unlock();
				}
			}
		};
		//start the thread
		t.start();
	}
	
	/**
	 * Returns to the title screen if the screen exits
	 * this is useful after somebody completes the maze
	 */
	public static void toTitleScreen() {
		if(startingScreen != null)
			startingScreen.setVisible(true);	
	}
	
	/**
	 * Main method to launch Maze game as a java application.
	 * The application can be operated in three ways. 
	 * 1) The intended normal operation is to provide no parameters
	 * and the maze will be generated by a randomized DFS algorithm (default). 
	 * 2) If a filename is given that contains a maze stored in xml format. 
	 * The maze will be loaded from that file. 
	 * This option is useful during development to test with a particular maze.
	 * 3) A predefined constant string is given to select a maze
	 * generation algorithm, currently supported is "Prim".
	 * @param args is optional, first string can be a fixed constant like Prim or
	 * the name of a file that stores a maze in XML format
	 */
	public static void main(String[] args) {
	    JFrame app ; 
		switch (args.length) {
		case 1 : app = new MazeApplication(args[0]);
		break ;
		case 0 : 
		default : app = new MazeApplication() ;
		break ;
		}
		app.repaint() ;
	}

}
