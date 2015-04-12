import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class Collision {
	private int numBodies;
	private int bodySize;
	private int numTimeSteps;
	private double DT = 1.0f;
	private double mass = 1.0f;
	
	private boolean debug = false;
	
	final double G = 6.67e-11;
	int numCollisions;
	public Body[] bodies;
		
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
		
		BufferedReader readBuffer;
		String currLine;
		
		
		numBodies = b;
		bodySize = s;
		numTimeSteps = t;
		
		try {
			readBuffer = new BufferedReader(new FileReader("points.dat"));
			
			if((currLine = readBuffer.readLine()) != null)
			{
				int count = 0;
				String[] tokens;
				numBodies = Integer.parseInt(currLine);
				bodies = new Body[numBodies];
				for(int i = 0; i < numBodies; i++)
					bodies[i] = new Body();

				while((currLine = readBuffer.readLine()) != null)
				{
					tokens = currLine.split(" ");
					bodies[count].setXPos(Double.valueOf(tokens[0]));
					bodies[count].setYPos(Double.valueOf(tokens[1]));
					bodies[count].setXVel(Double.valueOf(tokens[2]));
					bodies[count].setYVel(Double.valueOf(tokens[3]));
					bodies[count].setRadius(bodySize);
					bodies[count].setMass(mass);
					count++;
				}
			}
			else
			{
				bodies = new Body[numBodies];
				for(int i = 0; i < numBodies; i++)
					bodies[i] = new Body();

			}
			
		} catch (FileNotFoundException e1) {
			System.out.println("points.dat couldnt be opened.");
		} catch (IOException e) {
		}
		
		
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
				buffer.write("Body " + i + ": (" + bodies[i].getXPos() + ", " + bodies[i].getYPos() +")\n");
			
			buffer.write("Final Velocities:\n");
			for(int i = 0; i < numBodies; i++)
				buffer.write("Body " + i + ": (" + bodies[i].getXVel() + ", " + bodies[i].getYVel() +")\n");
				
			buffer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("computation time: " + (endTime - startTime) / 1000 + " seconds " +
				(endTime - startTime) % 1000 + " milliseconds");
		System.out.println("number of collisions detected = " + numCollisions);
		
		System.exit(0);
		
	}

	private void moveBodies() {
		
		for(int i = 0; i < numBodies; i++)
		{
			System.out.println("x: " + bodies[i].getXPos() + " y: " + bodies[i].getYPos());
			bodies[i].setXPos(bodies[i].getXPos() + (bodies[i].getXVel() * DT));
			bodies[i].setYPos(bodies[i].getYPos() + (bodies[i].getYVel() * DT));
			if(debug)
				System.out.println("x: " + bodies[i].getXPos() + " y: " + bodies[i].getYPos() + "\n");
		}
	}

	private void calculateForces() {
/*			int j = i + 1;
		distance = Math.sqrt((bodies[i].getXPos() - bodies[j].getXPos()) * 
							 (bodies[i].getXPos() - bodies[j].getXPos()) +
							 (bodies[i].getYPos() - bodies[j].getYPos()) *
							 (bodies[i].getYPos() - bodies[j].getYPos()));
*/			//more
		
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
