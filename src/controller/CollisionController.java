package controller;

import view.CollisionGUI;
import model.Collision;

public class CollisionController {

	public static CollisionGUI gui;
	public static Collision col;
	public static String[] startArgs;
	public static boolean graphicsFlag = false;
	
	public static void main( String[] args ) {
		startArgs = args;
		startCollisionGUI( );
	}
	public static void startCollisionGUI( ) {

		String[] args = startArgs;
		
		if( args.length < 4 )
		{
			System.out.println("Too few arguments: 4 are required, " + args.length + " were given.");
			Collision.usage();
			return;
		}
		
		if( args.length == 5 )
		{
			if( Integer.parseInt(args[4]) == 1)
				graphicsFlag = true;
		}
				
		init();
	}
	
	public static void init() {
		if(Integer.parseInt(startArgs[0]) == 1)
			col = new Collision( Integer.parseInt(startArgs[1]), Integer.parseInt(startArgs[2]), Integer.parseInt(startArgs[3]), graphicsFlag );
		else
			col = new Collision( Integer.parseInt(startArgs[0]), Integer.parseInt(startArgs[1]), Integer.parseInt(startArgs[2]), Integer.parseInt(startArgs[3]), graphicsFlag );

		if(startArgs.length == 6)
			col.csv = true;
		
		if(graphicsFlag)
		{
			gui = new CollisionGUI(Integer.parseInt(startArgs[1]), col, startArgs);
		}

		if(Integer.parseInt(startArgs[0]) == 1)
			col.sequentialStart( gui );
		else
			col.parallelStart( gui );
		
		//gui.updateCircles();
	}
}
