
public class Collision {

	public int numWorkers;
	public int numBodies;
	public int bodySize;
	public int numTimeSteps;
	
	
	public Collision( int w, int b, int s, int t )
	{
		numWorkers = w;
		numBodies = b;
		bodySize = s;
		numTimeSteps = t;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if( args.length != 4 )
		{
			usage();
			return;
		}
		
		new Collision( Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]) );
	}

	public static void usage()
	{
		System.out.print("Collisions Usage\n\n");
		System.out.print("\tjava Collision w b s t\n\n");
		System.out.print("\tw - Number of workers, 1 to 16. Ignored by sequential program.\n");
		System.out.print("\tb - number of bodies.\n");
		System.out.print("\ts - size of each body.\n");
		System.out.print("\tt - number of time steps.\n");		
	}
	
}
