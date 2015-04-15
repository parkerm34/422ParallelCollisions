package model;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.Semaphore;

import view.CollisionGUI;

public class Collision {
	private final double G = 6.67e-11;
	private final double DT = 0.01f;
	private final double MASS = 1.0f;
	
	private int numCollisions;
	private int numBodies;
	private double bodySize;
	private int numTimeSteps;
	private int numWorkers;
	private int[][] workerBodies;
	private Body[] bodies;
	protected CollisionGUI gui;
	
	private int numArrived;
	private Semaphore mutex;
	private Semaphore[] barrier;
	
	private File output;
	public boolean guiFlag = false;
	public boolean debug = false;
	public boolean csv = false;
			
	// Parallel constructor
	public Collision( int w, int b, double s, int t, boolean guiFlag)
	{
		if(debug)
			System.out.println("start parallel");

		numWorkers = w;
		numBodies = b;
		bodySize = s;
		numTimeSteps = t;
		this.guiFlag = guiFlag;
		workerBodies = new int[w][];
		numCollisions = 0;
		numArrived = 1;
		barrier = new Semaphore[3];
		barrier[0] = new Semaphore(0);
		barrier[1] = new Semaphore(0);
		barrier[2] = new Semaphore(0);
		mutex = new Semaphore(1);
		
		parseBodies();
		readPoints();
	}
	
	// Sequential constructor
	public Collision( int b, double s, int t, boolean guiFlag )
	{
		if(debug)
			System.out.println("start sequential");
		
		numBodies = b;
		bodySize = s;
		numTimeSteps = t;
		this.guiFlag = guiFlag;
		workerBodies = new int[1][b];
		numCollisions = 0;
		
		for(int i = 0; i < b; i ++)
			workerBodies[0][i] = i;
		
		readPoints();
	}
	
	public void parallelStart( CollisionGUI gui ) {
		long startTime, endTime;
		long seconds, millis;
		
		if(guiFlag)
			this.gui = gui;
		
		endTime = 0;

		CollisionWorker[] threads = new CollisionWorker[numWorkers];
		for(int i = 0; i < numWorkers; i++)
			threads[i] = new CollisionWorker(i, this);

		startTime = System.currentTimeMillis();
		
		for(int i = 0; i < numWorkers; i++)
			threads[i].start();
		
		for(int i = 0; i < numWorkers; i++)
		{
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		endTime = System.currentTimeMillis();
		
		endCollision();
		
		seconds = (endTime - startTime) / 1000;
		millis = (endTime - startTime) % 1000;
		
		System.out.println("computation time: " + seconds + " seconds " +
				millis + " milliseconds");
		System.out.println("number of collisions detected = " + numCollisions);
		
		if(csv)
		{	
			FileOutputStream tempOut;
			BufferedWriter bufferOut;
			
			try {
				
				output = new File("output.txt");
				tempOut = new FileOutputStream(output);
				bufferOut = new BufferedWriter(new OutputStreamWriter(tempOut));
				
				bufferOut.write(seconds + "," + millis + "\n");
				
				bufferOut.close();
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}
		
		if(!guiFlag)
			System.exit(0);
	}
	
	public void sequentialStart( CollisionGUI gui ) {
		long startTime, endTime;
		long seconds, millis;
		
		if(guiFlag)
			this.gui = gui;
		
		endTime = 0;
		startTime = 0;
		
		startTime = System.currentTimeMillis();
		
		for(int i = 0; i < getNumTimeSteps(); i++)
		{
			if(debug)
			{
				System.out.println("Before TR " + i + ": Number of collisions: " + numCollisions);
				for(int j = 0; j < numBodies; j++)
				{
					System.out.println("Body: " + j);
					System.out.println(" - Before move: xPos: " + bodies[j].getXPos() + " yPos: " + bodies[j].getYPos());
					System.out.println(" - Before move: xVel: " + bodies[j].getXVel() + " yVel: " + bodies[j].getYVel());
				}
			}
			
			calculateForces();
			moveBodies();
			detectCollisions();
			if(guiFlag)
				this.gui.updateCircles();
			
			if(debug)
			{
				for(int j = 0; j < numBodies; j++)
				{
					System.out.println("Body: " + j);
					System.out.println(" - After move: xPos: " + bodies[j].getXPos() + " yPos: " + bodies[j].getYPos());
					System.out.println(" - After move: xVel: " + bodies[j].getXVel() + " yVel: " + bodies[j].getYVel());
				}
				System.out.println();
			}
		}
		
		endTime = System.currentTimeMillis();
		
		seconds = (endTime - startTime) / 1000;
		millis = (endTime - startTime) % 1000;
		
		System.out.println("computation time: " + seconds + " seconds " +
				millis + " milliseconds");
		System.out.println("number of collisions detected = " + numCollisions);
		
		if(csv)
		{	
			FileOutputStream tempOut;
			BufferedWriter bufferOut;
			
			try {
				
				output = new File("output.txt");
				tempOut = new FileOutputStream(output);
				bufferOut = new BufferedWriter(new OutputStreamWriter(tempOut));
				
				bufferOut.write(seconds + "," + millis + "\n");
				
				bufferOut.close();
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		}
		
		if(!guiFlag)
			System.exit(0);
	}
	
	
	
	// This function is used to separate the bodies via reverse striping into the workers
	private void parseBodies()
	{
		int curr;
		int division = numBodies / numWorkers;
		boolean iterateFirst = division % 2 == 0;
		
		for(int i = 0; i < numWorkers; i++)
		{
			if(iterateFirst && i < numBodies % numWorkers)
				workerBodies[i] = new int[division + 1];
			else if(!iterateFirst && i >= (numWorkers - numBodies % numWorkers))
					workerBodies[i] = new int[division + 1];
			else
				workerBodies[i] = new int[division];
		}
		
		for(int i = 0; i < numWorkers; i++)
		{
			curr = 0;
			for(int j = 0; j < workerBodies[i].length; j++)
			{
				workerBodies[i][j] = curr + (j % 2 == 0 ? i : numWorkers - (i + 1));
				curr += numWorkers;
			}
		}
	}
	
	// This code would be repeated in the constructors, so we pulled it out to make
	// the code more modular. This function writes to the output file and closes it.
	private void endCollision() {
		File file;
		FileOutputStream fileOut;
		BufferedWriter buffer;
		
		try {
			
			file = new File("results.txt");
			fileOut = new FileOutputStream(file);
			buffer = new BufferedWriter(new OutputStreamWriter(fileOut));
			
			buffer.write("Final Positions:\n");
			for(int i = 0; i < numBodies; i++)
				buffer.write("Body " + i + ": (" + String.format("%.4f", bodies[i].getXPos()) + ", " + 
						String.format("%.4f", bodies[i].getYPos()) + ")\n");
			
			buffer.write("\nFinal Velocities:\n");
			for(int i = 0; i < numBodies; i++)
				buffer.write("Body " + i + ": (" + String.format("%.4f", bodies[i].getXVel()) + ", " +
						String.format("%.4f", bodies[i].getYVel()) + ")\n");
				
			buffer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// This code would be repeated in the constructors, so we pulled it out to make
	// the code more modular. This code reads an input file and initializes points
	// from the file, giving their initial position and velocity in terms of x and y.
	private void readPoints() {
		int count;
		BufferedReader readBuffer;
		String currLine;
		String[] tokens;
		
		count = 0;
		
		bodies = new Body[numBodies];
		for(int i = 0; i < numBodies; i++)
			bodies[i] = new Body();
		
		try {
			readBuffer = new BufferedReader(new FileReader("points.dat"));
			
			while((currLine = readBuffer.readLine()) != null && count < numBodies)
			{
				tokens = currLine.split(" ");
				bodies[count].setXPos(Double.valueOf(tokens[0]));
				bodies[count].setYPos(Double.valueOf(tokens[1]));
				bodies[count].setXVel(Double.valueOf(tokens[2]));
				bodies[count].setYVel(Double.valueOf(tokens[3]));
				bodies[count].setRadius(bodySize);
				bodies[count].setMass(MASS);
				count++;
			}
			
		} catch (FileNotFoundException e1) {
			System.out.println("points.dat couldnt be opened.");
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// This function is for the sequential instantiation of Collision.
	// This function defaults to use all of the bodies for calculating the forces.
	private void calculateForces() {
		calculateForcesHelper( 0 );
	}
	
	// This function is for the parallel instantiation of Collision.
	// This function takes the thread id and says tells the main function
	// how many bodies to go through as well as exactly which bodies are being
	// accounted for by this thread
	protected void calculateForces( int num ) {
		calculateForcesHelper( num );
	}
	
	// This function has been changed to run through a loop from a given input
	// rather than going from 0 to numBodies. This is because when going through
	// the threads, we will not be going through every body in every thread when
	// this function is called. We also did not want to just create a new function
	// because the code would be all the same, the only difference being the beginning
	// and end of the main loop within the function
	private void calculateForcesHelper( int num ) {
		double distance, magnitude;
		Point direction;
		int body;
		
		for(int i = 0; i < workerBodies[num].length; i++)
		{
			body = workerBodies[num][i];
			for(int j = body + 1; j < numBodies; j++)
			{
				distance = Math.sqrt((bodies[body].getXPos() - bodies[j].getXPos()) * 
						 (bodies[body].getXPos() - bodies[j].getXPos()) +
						 (bodies[body].getYPos() - bodies[j].getYPos()) *
						 (bodies[body].getYPos() - bodies[j].getYPos()));
				
				magnitude = G * bodies[body].getMass() * bodies[j].getMass() / (distance * distance);
				direction = new Point(bodies[j].getXPos() - bodies[body].getXPos(),
						bodies[j].getYPos() - bodies[body].getYPos());
				
				bodies[body].setXForce(bodies[body].getXForce() + magnitude * direction.x / distance);
				bodies[j].setXForce(bodies[j].getXForce() - magnitude * direction.x / distance);
				bodies[body].setYForce(bodies[body].getYForce() + magnitude * direction.y / distance);
				bodies[j].setYForce(bodies[j].getYForce() - magnitude * direction.y / distance);
			}
		}
	}

	// This function is for the sequential instantiation of Collision.
	// This function defaults to use all of the bodies for moving the bodies.
	private void moveBodies() {
		moveBodiesHelper( 0 );
	}
	
	// This function is for the parallel instantiation of Collision.
	// This function takes the thread id and says tells the main function
	// how many bodies to go through as well as exactly which bodies are being
	// accounted for by this thread
	protected void moveBodies( int num ) {
		moveBodiesHelper( num );
	}
	
	// This function has been changed to run through a loop from a given input
	// rather than going from 0 to numBodies. This is because when going through
	// the threads, we will not be going through every body in every thread when
	// this function is called. We also did not want to just create a new function
	// because the code would be all the same, the only difference being the beginning
	// and end of the main loop within the function
	private void moveBodiesHelper( int num ) {
		int body;
		
		for(int i = 0; i < workerBodies[num].length; i++)
		{
			body = workerBodies[num][i];
			
			Point deltaV;
			Point deltaP;
			
			deltaV = new Point(bodies[body].getXForce() / bodies[body].getMass() * DT,
					bodies[body].getYForce() / bodies[body].getMass() * DT);
			deltaP = new Point( (bodies[body].getXVel() + deltaV.x / 2) * DT,
					(bodies[body].getYVel() + deltaV.y / 2) * DT);
			
			bodies[body].setXVel(bodies[body].getXVel() + deltaV.x);
			bodies[body].setYVel(bodies[body].getYVel() + deltaV.y);
			
			bodies[body].setXPos(bodies[body].getXPos() + deltaP.x);
			bodies[body].setYPos(bodies[body].getYPos() + deltaP.y);
			
			// reset force vector
			bodies[body].setXForce(0);
			bodies[body].setYForce(0);
		}
	}
	
	// This function is for the sequential instantiation of Collision.
	// This function defaults to use all of the bodies for detecting collisions.
	private void detectCollisions() {
		detectCollisionsHelper( 0 );
	}
	
	// This function is for the parallel instantiation of Collision.
	// This function takes the thread id and says tells the main function
	// how many bodies to go through as well as exactly which bodies are being
	// accounted for by this thread
	protected void detectCollisions( int num ) {
		detectCollisionsHelper( num );
	}

	// This function has been changed to run through a loop from a given input
	// rather than going from 0 to numBodies. This is because when going through
	// the threads, we will not be going through every body in every thread when
	// this function is called. We also did not want to just create a new function
	// because the code would be all the same, the only difference being the beginning
	// and end of the main loop within the function
	private void detectCollisionsHelper( int num )
	{
		double distance;
		int body;
		
		for(int i = 0; i < workerBodies[num].length; i++)
		{
			body = workerBodies[num][i];
			for(int j = body + 1; j < numBodies; j++)
			{
				distance = Math.sqrt((bodies[body].getXPos() - bodies[j].getXPos()) *
						(bodies[body].getXPos() - bodies[j].getXPos()) +
						(bodies[body].getYPos() - bodies[j].getYPos()) *
						(bodies[body].getYPos() - bodies[j].getYPos()));
				
				if( distance <= (bodies[body].getRadius() + bodies[j].getRadius()) )
				{
					ResolveCollision(body, j);
					numCollisions++;
				}
			}
		}
		
	}
	
	private void ResolveCollision(int b1, int b2) {
		double distSquared;
		double v1fx, v1fy, v2fx, v2fy;
		double v1nfxNumerator, v1txNumerator, v2nfxNumerator, v2txNumerator;
		double v1nfyNumerator, v1tyNumerator, v2nfyNumerator, v2tyNumerator;
		double diffXPos, diffYPos;
		
		// dist = b1.r + b2.r
		distSquared = (bodies[b1].getRadius() + bodies[b2].getRadius()) * 
				(bodies[b1].getRadius() + bodies[b2].getRadius());
		
		// x2 - x1
		diffXPos = bodies[b2].getXPos() - bodies[b1].getXPos();
		// y2 - y1
		diffYPos = bodies[b2].getYPos() - bodies[b1].getYPos();
		
		// Find final normal and tangent vectors for Body 1's x
		v1nfxNumerator = bodies[b2].getXVel() * diffXPos * diffXPos +
				bodies[b2].getYVel() * diffXPos * diffYPos;
		v1txNumerator = bodies[b1].getXVel() * diffYPos * diffYPos -
				bodies[b1].getYVel() * diffXPos * diffYPos;
		// Find the final total x vector for Body 1
		v1fx = (v1nfxNumerator + v1txNumerator) / distSquared;
		
		// Find final normal and tangent y vectors for Body 1
		v1nfyNumerator = bodies[b2].getXVel() * diffXPos * diffYPos +
				bodies[b2].getYVel() * diffYPos * diffYPos;
		v1tyNumerator = -(bodies[b1].getXVel() * diffYPos * diffXPos) +
				bodies[b1].getYVel() * diffXPos * diffXPos;
		// Find the final total y vector for Body 1
		v1fy = (v1nfyNumerator + v1tyNumerator) / distSquared;
		
		// Find final normal and tangent x vectors for Body 2
		v2nfxNumerator = bodies[b1].getXVel() * diffXPos * diffXPos +
				bodies[b1].getYVel() * diffXPos * diffYPos;
		v2txNumerator = bodies[b2].getXVel() * diffYPos * diffYPos -
				bodies[b2].getYVel() * diffXPos * diffYPos;
		// Find the final total x vector for Body 1
		v2fx = (v2nfxNumerator + v2txNumerator) / distSquared;
		
		// Find final normal and tangent y vectors for Body 2
		v2nfyNumerator = bodies[b1].getXVel() * diffXPos * diffYPos +
				bodies[b1].getYVel() * diffYPos * diffYPos;
		v2tyNumerator = -(bodies[b2].getXVel() * diffYPos * diffXPos) +
				bodies[b2].getYVel() * diffXPos * diffXPos;
		// Find the final total y vector for Body 1
		v2fy = (v2nfyNumerator + v2tyNumerator) / distSquared;
		
		// Update the final velocities
		bodies[b1].setXVel(v1fx);
		bodies[b1].setYVel(v1fy);
		bodies[b2].setXVel(v2fx);
		bodies[b2].setYVel(v2fy);
	}
	
	// All the setters and getters below. Most of these were created for CollisionWorker to work

	public int getNumTimeSteps() {
		return numTimeSteps;
	}

	public void setNumTimeSteps(int numTimeSteps) {
		this.numTimeSteps = numTimeSteps;
	}

	public int getNumBodies() {
		return numBodies;
	}

	public void setNumBodies(int numBodies) {
		this.numBodies = numBodies;
	}

	public double getBodySize() {
		return bodySize;
	}

	public void setBodySize(int bodySize) {
		this.bodySize = bodySize;
	}

	public int getNumWorkers() {
		return numWorkers;
	}

	public void setNumWorkers(int numWorkers) {
		this.numWorkers = numWorkers;
	}

	public int[][] getWorkerBodies() {
		return workerBodies;
	}

	public void setWorkerBodies(int[][] workerBodies) {
		this.workerBodies = workerBodies;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isGuiFlag() {
		return guiFlag;
	}
	
	public void setIsGuiFlag(boolean guiFlag) {
		this.guiFlag = guiFlag;
	}
	
	public boolean isCSV() {
		return csv;
	}

	public void setCSV(boolean csv) {
		this.csv = csv;
	}
	
	public int getNumCollisions() {
		return numCollisions;
	}

	public void setNumCollisions(int numCollisions) {
		this.numCollisions = numCollisions;
	}

	public Body[] getBodies() {
		return bodies;
	}

	public void setBodies(Body[] bodies) {
		this.bodies = bodies;
	}

	public int getNumArrived() {
		return numArrived;
	}

	public void setNumArrived(int numArrived) {
		this.numArrived = numArrived;
	}

	public void aquireMutex() {
		try {
			mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void releaseMutex() {
		mutex.release();
	}

	public void acquireBarrier(int barrierIndex) {
		try {
			barrier[barrierIndex].acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void releaseAllBarrier(int barrierIndex) {
		barrier[barrierIndex].release(numWorkers - 1);
	}

	public double getG() {
		return G;
	}

	public double getDT() {
		return DT;
	}

	public double getMASS() {
		return MASS;
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