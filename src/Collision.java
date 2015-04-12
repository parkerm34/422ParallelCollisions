import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class Collision {
	// why public???
	public int numBodies;
	public int bodySize;
	public int numTimeSteps;
	
	final double G = 6.67e-11;
	int numCollisions;
	Body p[], v[], f[];
	double m[];
		
	public static void main(String[] args) {
		
		if( args.length != 4 )
		{
			System.out.println("Too few arguments: 4 are required, " + args.length + " were given.");
			usage();
			return;
		}
		new Collision( Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]) );
	}
	
	public Collision( int w, int b, int s, int t )
	{
		long startTime, endTime;
		File file;
		FileOutputStream fileOut;
		BufferedWriter buffer;
		
		numBodies = b;
		bodySize = s;
		numTimeSteps = t;
		
		p = new Body[b];
		v = new Body[b];
		f = new Body[b];
		m = new double[b];
		numCollisions = 0;
		endTime = 0;
		
		startTime = System.currentTimeMillis();
		
		for(int i = 0; i < numTimeSteps; i++)
		{
			calculateForces();
			moveBodies();
		}
		
		endTime = System.currentTimeMillis();
		
		try {
		
			file = new File("results.txt");
			fileOut = new FileOutputStream(file);
			buffer = new BufferedWriter(new OutputStreamWriter(fileOut));
			
			buffer.write("Final Positions:\n");
			for(int i = 0; i < numBodies; i++)
				buffer.write("Body " + i + ": (" + p[i].x + ", " + p[i].y +")\n");
			
			buffer.write("Final Velocities:\n");
			for(int i = 0; i < numBodies; i++)
				buffer.write("Body " + i + ": (" + v[i].x + ", " + v[i].y +")\n");
				
			buffer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("computation time = " + (endTime - startTime) / 1000 + " seconds " +
				(endTime - startTime) % 1000 + " milliseconds");
		System.out.println("number of collisions detected = " + numCollisions);
		
		System.exit(0);
		
	}

	private void moveBodies() {
		double distance, magnitude;
		Body direction;
		
		for(int i = 0; i < numBodies-1; i++)
		{
			int j = i + 1;
			distance = Math.sqrt((p[i].x - p[j].x) * (p[i].x - p[j].x) + (p[i].y - p[j].y) * (p[i].y - p[j].y));
			//more
		}
	}

	private void calculateForces() {
		// TODO Auto-generated method stub
		
	}
	
	public static void usage()
	{
		System.out.println("Collisions Usage\n");
		System.out.println("\tjava Collision w b s t\n");
		System.out.println("\tw - Number of workers, 1 to 16. Ignored by sequential program.");
		System.out.println("\tb - number of bodies.");
		System.out.println("\ts - size of each body.");
		System.out.println("\tt - number of time steps.");		
	}
	
}
