package controller;

import view.CollisionGUI;
import model.Collision;

public class CollisionController {

	public static CollisionGUI gui;
	public static Collision col;
	
	public static void main( String[] args ) {
	
		if( args.length != 4 )
		{
			System.out.println("Too few arguments: 4 are required, " + args.length + " were given.");
			Collision.usage();
			return;
		}
		
		
		if(Integer.parseInt(args[0]) == 1)
			col = new Collision( Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]) );
		else
			col = new Collision( Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]) );

		gui = new CollisionGUI(Integer.parseInt(args[1]), col);

		if(Integer.parseInt(args[0]) == 1)
			col.sequentialStart( gui );
		else
			col.parallelStart( gui );
		
		//gui.updateCircles();
	}
}
