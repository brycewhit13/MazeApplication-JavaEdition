# MazeApplication-JavaEdition

**Introduction**

This is an application where the user can create a maze of different sizes and then try to escape themseleves, or watch the computer try to escape. When you first load in you are presented with three dropdown menus:
1. Determine who will be playing (Manual = User, Wallfollower, or Wizard)
2. Algorithm to generate the Maze (DFS, Prim, Ellers)
3. Maze Difficulty/Size (Levels 0-15 in increasing difficulty)
Then the user can just hit start and the program will begin to generate the Maze

The program is run in `src/generation/MazeApplication.java` in case someone is running this from a commandline and not an IDE. 

**Rules**

The user (and robots) have 3000 energy to escape the maze. If they escape before their energy runs out, they win! If they are still in the mae and have no energy, they lose. Each movement has a different energy cost which is important to know for ones success.

SENSING_ENERGY = 1; //energy required to use a sensor\
QUARTER_TURN_ENERGY = 3; //energy required to turn 90 degrees\
MOVE_FORWARD_ENERGY = 5; //energy required to move forward\
JUMP_WALL_ENERGY = 50; //energy required to jump (Wizard Only) 

**Robot Algorithms**

There are two robot algorithms currently: Wall-Follower and Wizard

*Wall Follower*: Wall-Follower is the simeple algorithm, where it just follows the left wall. This means everytime there is an opportunity to turn left, it does. This is a classic method for escaping a maze. It is not always the most efficient, but guranteed to find the exit assuming you don't run out of energy first. 

*Wizard*: The Wizard Robot is more powerful than anyone else. It has the ability to jump over walls when it wants. This is more expensive as it costs 50 energy. The Wizard Robot knows the path to the exit, but its goal is to find the most efficient path for itself. This means it will jump over walls only when it is more cost effective than walking to the same position. 


**Controls**

*Arrow Keys*: Move and turn in the maze\
*Mouse*: Click sensor uttons to fix or reak sensors\
*M*: Toggle the local Map\
*Z*: Toggle the full Map\
*S*: Toggle solution on the map\
*+/-*: Zoom in or out of the map

**Sensors**

When playing the game you will see 8 buttons, one to fix the sensor of each direction and one to break it. This is only applicable when the robot algorithms are trying to escape the maze. For example if the left sensor is broken, the robot will not be able to see to its left. Instead it will have to turn left and look forward everytime it wants to see what is to the left. It will do this until the sensor is fixed. 
