package gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Panel;
import java.awt.RenderingHints;

import generation.CardinalDirection;
import generation.Maze;

/**
 * Add functionality for double buffering to an AWT Panel class.
 * Used for drawing a maze.
 * 
 * @author Peter Kemper
 *
 */
public class MazePanel extends Panel  {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L; //NOT USED, JUST TO MAKE JAVA HAPPY
	/* Panel operates a double buffer see
	 * http://www.codeproject.com/Articles/2136/Double-buffer-in-standard-Java-AWT
	 * for details
	 */
	// bufferImage can only be initialized if the container is displayable,
	// uses a delayed initialization and relies on client class to call initBufferImage()
	// before first use
	private Image bufferImage;  
	private Graphics2D graphics; // obtained from bufferImage, 
	// graphics is stored to allow clients to draw on the same graphics object repeatedly
	// has benefits if color settings should be remembered for subsequent drawing operations
	private static Color color;
	
	final private int viewWidth = 400;
	final private int viewHeight = 400;
	final private int mapUnit = 128;
	final private int stepSize = mapUnit/4;
	
	/**
	 * Constructor. Object is not focusable.
	 */
	public MazePanel() {
		setFocusable(false);
		bufferImage = null; // bufferImage initialized separately and later
		graphics = null;	// same for graphics 
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}
	/**
	 * Method to draw the buffer image on a graphics object that is
	 * obtained from the superclass. 
	 * Warning: do not override getGraphics() or drawing might fail. 
	 */
	public void update() {
		paint(getGraphics());
	}
	
	/**
	 * Draws the buffer image to the given graphics object.
	 * This method is called when this panel should redraw itself.
	 * The given graphics object is the one that actually shows 
	 * on the screen.
	 */
	@Override
	public void paint(Graphics g) {
		if (null == g) {
			System.out.println("MazePanel.paint: no graphics object, skipping drawImage operation");
		}
		else {
			g.drawImage(bufferImage,0,0,null);	
		}
	}

	/**
	 * Obtains a graphics object that can be used for drawing.
	 * This MazePanel object internally stores the graphics object 
	 * and will return the same graphics object over multiple method calls. 
	 * The graphics object acts like a notepad where all clients draw 
	 * on to store their contribution to the overall image that is to be
	 * delivered later.
	 * To make the drawing visible on screen, one needs to trigger 
	 * a call of the paint method, which happens 
	 * when calling the update method. 
	 * @return graphics object to draw on, null if impossible to obtain image
	 */
	public Graphics getBufferGraphics() {
		// if necessary instantiate and store a graphics object for later use
		if (null == graphics) { 
			if (null == bufferImage) {
				bufferImage = createImage(Constants.VIEW_WIDTH, Constants.VIEW_HEIGHT);
				if (null == bufferImage)
				{
					System.out.println("Error: creation of buffered image failed, presumedly container not displayable");
					return null; // still no buffer image, give up
				}		
			}
			graphics = (Graphics2D) bufferImage.getGraphics();
			if (null == graphics) {
				System.out.println("Error: creation of graphics for buffered image failed, presumedly container not displayable");
			}
			else {
				// System.out.println("MazePanel: Using Rendering Hint");
				// For drawing in FirstPersonDrawer, setting rendering hint
				// became necessary when lines of polygons 
				// that were not horizontal or vertical looked ragged
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			}
		}
		return graphics;
	}
	
	//////////////////////////PROJECT 5//////////////////////////////
	public static Color getColor() {
		return color;
	}
	
	public void setColor(Color col) {
		graphics.setColor(col);
	}
	
	public void setColor(int rgbValue) {
		graphics.setColor(new Color(rgbValue));
	}
	
	
	/**
	 * Draws a black and a grey rectangle to provide a background.
	 * Note that this also erases previous drawings of maze or map.
	 * @param graphics to draw on, must be not null
	 */
	public void drawBackground(Graphics graphics) {
		// black rectangle in upper half of screen
		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, viewWidth, viewHeight/2);
		// grey rectangle in lower half of screen
		graphics.setColor(Color.darkGray);
		graphics.fillRect(0, viewHeight/2, viewWidth, viewHeight/2);
	}

	public void fillPolygon(int[] xps, int[] yps, int i) {
		graphics.fillPolygon(xps, yps, i);
	}
	
	/**
	 * Helper method for draw, called if map_mode is true, i.e. the users wants to see the overall map.
	 * The map is drawn only on a small rectangle inside the maze area such that only a part of the map is actually shown.
	 * Of course a part covering the current location needs to be displayed.
	 * The current cell is (px,py). There is a viewing direction (view_dx, view_dy).
	 * @param g graphics handler to manipulate screen
	 * @param px current position, x index
	 * @param py current position, y index 
	 */
	public void drawMap(Map map, Maze maze, int px, int py, int walkStep, 
			int viewDX, int viewDY, boolean showMaze, boolean showSolution) {
		// dimensions of the maze in terms of cell ids
		final int mazeWidth = maze.getWidth() ;
		final int mazeHeight = maze.getHeight() ;
		
		graphics.setColor(Color.white);
		
		// note: 1/2 of width and height is the center of the screen
		// the whole map is centered at the current position
		final int offsetX = getOffset(px, walkStep, viewDX, viewWidth);
		final int offsetY = getOffset(py, walkStep, viewDY, viewHeight);
		
		// We need to calculate bounds for cell indices to consider
		// for drawing. Since not the whole maze may be visible
		// for the given screen size and the current position (px,py)
		// is fixed to the center of the drawing area, we need
		// to find the min and max indices for cells to consider.
		// compute minimum for x,y
		final int minX = getMinimum(offsetX);
		final int minY = getMinimum(offsetY);
		// compute maximum for x,y
		final int maxX = getMaximum(offsetX, viewWidth, mazeWidth);
		final int maxY = getMaximum(offsetY, viewHeight, mazeHeight);
		
		// iterate over integer grid between min and max of x,y indices for cells
		for (int y = minY; y <= maxY; y++)
			for (int x = minX; x <= maxX; x++) {
				// starting point of line
				int startX = mapToCoordinateX(x, offsetX);
				int startY = mapToCoordinateY(y, offsetY);
				
				// draw horizontal line
				boolean theCondition = (x >= mazeWidth) ? false : ((y < mazeHeight) ?
						maze.hasWall(x,y, CardinalDirection.North) :
							maze.hasWall(x,y-1, CardinalDirection.South));

				graphics.setColor(map.getSeenWalls().hasWall(x,y, CardinalDirection.North) ? Color.white : Color.gray);
				if ((map.getSeenWalls().hasWall(x,y, CardinalDirection.North) || showMaze) && theCondition)
					graphics.drawLine(startX, startY, startX + Map.getMapScale(), startY); // y coordinate same
				
				// draw vertical line
				theCondition = (y >= mazeHeight) ? false : ((x < mazeWidth) ?
						maze.hasWall(x,y, CardinalDirection.West) :
							maze.hasWall((x-1),y, CardinalDirection.East));

				graphics.setColor(map.getSeenWalls().hasWall(x,y, CardinalDirection.West) ? Color.white : Color.gray);
				if ((map.getSeenWalls().hasWall(x,y, CardinalDirection.West) || showMaze) && theCondition)
					graphics.drawLine(startX, startY, startX, startY - Map.getMapScale()); // x coordinate same
			}
		
		if (showSolution) {
			drawSolution(maze, offsetX, offsetY, px, py) ;
		}
	}
	
	public void drawCurrentLocation(int viewDX, int viewDY) {
		graphics.setColor(Color.red);
		// draw oval of appropriate size at the center of the screen
		int centerX = viewWidth/2; // center x
		int centerY = viewHeight/2; // center y
		int diameter = Map.getMapScale()/2; // circle size
		// we need the top left corner of a bounding box the circle is in
		// and its width and height to draw the circle
		// top left corner is (centerX-radius, centerY-radius)
		// width and height is simply the diameter
		graphics.fillOval(centerX-diameter/2, centerY-diameter/2, diameter, diameter);
		// draw a red arrow with the oval to show current direction
		drawArrow(viewDX, viewDY, centerX, centerY);
	}
	
	private void drawArrow(int viewDX, int viewDY, 
		final int startX, final int startY) {
		// calculate length and coordinates for main line
		final int arrowLength = Map.getMapScale()*7/16; // arrow length, about 1/2 map_scale
		final int tipX = startX + mapToOffset(arrowLength, viewDX);
		final int tipY = startY - mapToOffset(arrowLength, viewDY);
		// draw main line, goes from starting (x,y) to end (tipX,tipY)
		graphics.drawLine(startX, startY, tipX, tipY);
		// calculate length and positions for 2 lines pointing towards (tipX,tipY)
		// find intermediate point (tmpX,tmpY) on main line
		final int length = Map.getMapScale()/4;
		final int tmpX = startX + mapToOffset(length, viewDX);
		final int tmpY = startY - mapToOffset(length, viewDY);
		// find offsets at intermediate point for 2 points orthogonal to main line
		// negative sign used for opposite direction
		// note the flip between x and y for view_dx and view_dy
		/*
		final int offsetX = -(length * view_dy) >> 16;
		final int offsetY = -(length * view_dx) >> 16;
		*/
		final int offsetX = mapToOffset(length, -viewDY);
		final int offsetY = mapToOffset(length, -viewDX);
		// draw two lines, starting at tip of arrow
		graphics.drawLine(tipX, tipY, tmpX + offsetX, tmpY + offsetY);
		graphics.drawLine(tipX, tipY, tmpX - offsetX, tmpY - offsetY);
	}
	
	private void drawSolution(Maze maze, int offsetX, int offsetY, int px, int py) {
		// current position on the solution path (sx,sy)
		int sx = px;
		int sy = py;
		int distance = maze.getDistanceToExit(sx, sy);
		
		graphics.setColor(Color.yellow);
		
		// while we are more than 1 step away from the final position
		while (distance > 1) {
			// find neighbor closer to exit (with no wallboard in between)
			int[] neighbor = maze.getNeighborCloserToExit(sx, sy) ;
			if (null == neighbor)
				return ; // error
			// scale coordinates, original calculation:
			// x-coordinates
			// nx1     == sx*map_scale + offx + map_scale/2;
			// nx1+ndx == sx*map_scale + offx + map_scale/2 + dx*map_scale == (sx+dx)*map_scale + offx + map_scale/2;
			// y-coordinates
			// ny1     == view_height-1-(sy*map_scale + offy) - map_scale/2;
			// ny1+ndy == view_height-1-(sy*map_scale + offy) - map_scale/2 + -dy * map_scale == view_height-1 -((sy+dy)*map_scale + offy) - map_scale/2
			// current position coordinates
			//int nx1 = sx*map_scale + offx + map_scale/2;
			//int ny1 = view_height-1-(sy*map_scale + offy) - map_scale/2;
			//
			// we need to translate the cell indices x and y into
			// coordinates for drawing, the yellow lines is centered
			// so 1/2 of the size of the cell needs to be added to the
			// top left corner of a cell which is + or - map_scale/2.
			int nx1 = mapToCoordinateX(sx,offsetX) + Map.getMapScale()/2;
			int ny1 = mapToCoordinateY(sy,offsetY) - Map.getMapScale()/2;
			// neighbor position coordinates
			//int nx2 = neighbor[0]*map_scale + offx + map_scale/2;
			//int ny2 = view_height-1-(neighbor[1]*map_scale + offy) - map_scale/2;
			int nx2 = mapToCoordinateX(neighbor[0],offsetX) + Map.getMapScale()/2;
			int ny2 = mapToCoordinateY(neighbor[1],offsetY) - Map.getMapScale()/2;
			graphics.drawLine(nx1, ny1, nx2, ny2);
			
			// update loop variables for current position (sx,sy)
			// and distance d for next iteration
			sx = neighbor[0];
			sy = neighbor[1];
			distance = maze.getDistanceToExit(sx, sy) ;
		}
	}
	
	/**
	 * Obtains the maximum for a given offset
	 * @param offset either in x or y direction
	 * @param viewLength is either viewWidth or viewHeight
	 * @param mazeLength is either mazeWidth or mazeHeight
	 * @return maximum that is bounded by mazeLength
	 */
	private int getMaximum(int offset, int viewLength, int mazeLength) {
		int result = (viewLength-offset)/Map.getMapScale()+1;
		if (result >= mazeLength)  
			result = mazeLength;
		return result;
	}

	/**
	 * Obtains the minimum for a given offset
	 * @param offset either in x or y direction
	 * @return minimum that is greater or equal 0
	 */
	private int getMinimum(final int offset) {
		final int result = -offset/Map.getMapScale();
		return (result < 0) ? 0 : result;
	}

	/**
	 * Calculates the offset in either x or y direction
	 * @param coordinate is either x or y coordinate of current position
	 * @param walkStep
	 * @param viewDirection is either viewDX or viewDY
	 * @param viewLength is either viewWidth or viewHeight
	 * @return the offset
	 */
	private int getOffset(int coordinate, int walkStep, int viewDirection, int viewLength) {
		final int tmp = coordinate*mapUnit + mapUnit/2 + mapToOffset((stepSize*walkStep),viewDirection);
		return -tmp*Map.getMapScale()/mapUnit + viewLength/2;
	}
	
	/**
	 * Maps the y index for some cell (x,y) to a y coordinate
	 * for drawing.
	 * @param cellY, {@code 0 <= cellY < height}
	 * @param offsetY
	 * @return y coordinate for drawing
	 */
	private int mapToCoordinateY(int cellY, int offsetY) {
		// TODO: bug suspect: inversion with height is suspect for upside down effect on directions
		// note: (cellY*map_scale + offsetY) same as for mapToCoordinateX
		return viewHeight-1-(cellY*Map.getMapScale() + offsetY);
	}

	/**
	 * Maps the x index for some cell (x,y) to an x coordinate
	 * for drawing. 
	 * @param cellX is the index of some cell, {@code 0 <= cellX < width}
	 * @param offsetX
	 * @return x coordinate for drawing
	 */
	private int mapToCoordinateX(int cellX, int offsetX) {
		return cellX*Map.getMapScale() + offsetX;
	}
	
	/**
	 * Maps a given length and direction into an offset for drawing coordinates.
	 * @param length
	 * @param direction
	 * @return offset
	 */
	private int mapToOffset(final int length, final int direction) {
		// Signed bit shift to the right performs a division by 2^16
		// preserves the sign
		// discards the remainder as the result is int
		return (length * direction) >> 16;
	}
	
	/**
     * Default minimum value for RGB values.
     */
    private static final int RGB_DEF = 20;
    /**
     * Determine and set the color for this wall.
     *
     * @param distance
     *            to exit
     * @param cc
     *            obscure
     * @return 
     */
    public static int initColor(final int rgbValue, final int distance, final int cc) {
        final int d = distance / 4;
        switch (((d >> 3) ^ cc) % 6) {
        case 0:
            return new Color(rgbValue, RGB_DEF, RGB_DEF).getRGB();
        case 1:
            return new Color(RGB_DEF, rgbValue, RGB_DEF).getRGB();
        case 2:
            return new Color(RGB_DEF, RGB_DEF, rgbValue).getRGB();
        case 3:
            return new Color(rgbValue, rgbValue, RGB_DEF).getRGB();
        case 4:
            return new Color(RGB_DEF, rgbValue, rgbValue).getRGB();
        case 5:
            return new Color(rgbValue, RGB_DEF, rgbValue).getRGB();
        default:
            return new Color(RGB_DEF, RGB_DEF, RGB_DEF).getRGB();
        }
    }
}


