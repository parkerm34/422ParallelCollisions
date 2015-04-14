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
	
	private int numBodies;
	private int bodySize;
	private int numTimeSteps;
	private int numWorkers;
	private int[] workerBodies;
	protected CollisionGUI gui;
	
	public boolean guiFlag = false;
	public boolean debug = false;
	int numCollisions;
	private Body[] bodies;
	Semaphore threadsEnd;
			
	// Parallel constructor
	public Collision( int w, int b, int s, int t, boolean guiFlag )
	{
		if(debug)
			System.out.println("start parallel");
		
		this.guiFlag = guiFlag;
		workerBodies = new int[w + 1];
		
		numBodies = b;
		bodySize = s;
		setNumTimeSteps(t);
		numWorkers = w;
		
		parseBodies();
		
		readPoints();

		numCollisions = 0;
	}
	
	// Sequential constructor
	public Collision( int b, int s, int t, boolean guiFlag )
	{
		if(debug)
			System.out.println("start sequential");
		
		this.guiFlag = guiFlag;
		numBodies = b;
		bodySize = s;
		setNumTimeSteps(t);
		
		readPoints();

		numCollisions = 0;
	}
	
	public void parallelStart( CollisionGUI gui ) {
		long startTime, endTime;
		if(guiFlag)
			this.gui = gui;
		
		endTime = 0;

		CollisionWorker[] threads = new CollisionWorker[numWorkers];
		
		startTime = System.currentTimeMillis();
		
		for(int i = 0; i < numWorkers; i++)
		{
			threads[i] = new CollisionWorker(i, this);
			threads[i].start();
		}
		
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
		
		System.out.println("computation time: " + (endTime - startTime) / 1000 + " seconds " +
				(endTime - startTime) % 1000 + " milliseconds");
		System.out.println("number of collisions detected = " + numCollisions);
		
//		System.exit(0);
	}
	
	public void sequentialStart( CollisionGUI gui ) {
		long startTime, endTime;
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
		
		endCollision();
//		gui.updateCircles();
		System.out.println("computation time: " + (endTime - startTime) / 1000 + " seconds " +
				(endTime - startTime) % 1000 + " milliseconds");
		System.out.println("number of collisions detected = " + numCollisions);
		
//		System.exit(0);
	}
	
	
	
	// This function is used to separate the number of bodies by number of workers
	// as well as putting them into a usable array for other functions to know their
	// bounds by each thread id
	private void parseBodies()
	{
		int end = 0;
		workerBodies[0] = 0;
		for(int i = 1; i < numWorkers + 1; i++)
		{
			end += numBodies/numWorkers;
			if( i <= (numBodies%numWorkers) && i != 0)
				end++;
			workerBodies[i] = end;
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
	
	private void barrier( int worker ) {
		
	}
	
/*	private void worker( int worker ) {
		for(int i = 0; i < getNumTimeSteps(); i++)
		{
			if(debug)
			{
				System.out.println("Before TR " + i + ": Number of collisions: " + numCollisions);
				for(int j = workerBodies[worker]; j < workerBodies[worker + 1]; j++)
				{
					System.out.println("Body: " + j);
					System.out.println(" - Before move: xPos: " + bodies[j].getXPos() + " yPos: " + bodies[j].getYPos());
					System.out.println(" - Before move: xVel: " + bodies[j].getXVel() + " yVel: " + bodies[j].getYVel());
				}
			}
			
			calculateForces( worker );
			barrier( worker );
			moveBodies( worker );
			barrier( worker );
			detectCollisions( worker );
			barrier( worker );
			
			if(debug)
			{
				for(int j = workerBodies[worker]; j < workerBodies[worker + 1]; j++)
				{
					System.out.println("Body: " + j);
					System.out.println(" - After move: xPos: " + bodies[j].getXPos() + " yPos: " + bodies[j].getYPos());
					System.out.println(" - After move: xVel: " + bodies[j].getXVel() + " yVel: " + bodies[j].getYVel());
				}
				System.out.println();
			}
		}

	}*/
	
	// This function is for the sequential instantiation of Collision.
	// This function defaults to use all of the bodies for calculating the forces.
	private void calculateForces() {
		calculateForcesHelper( 0, numBodies );
	}
	
	// This function is for the parallel instantiation of Collision.
	// This function takes the thread id and says tells the main function
	// how many bodies to go through as well as exactly which bodies are being
	// accounted for by this thread
	protected void calculateForces( int num ) {
		calculateForcesHelper( workerBodies[num], workerBodies[num + 1] );
	}
	
	// This function has been changed to run through a loop from a given input
	// rather than going from 0 to numBodies. This is because when going through
	// the threads, we will not be going through every body in every thread when
	// this function is called. We also did not want to just create a new function
	// because the code would be all the same, the only difference being the beginning
	// and end of the main loop within the function
	private void calculateForcesHelper( int start, int num ) {
		double distance, magnitude;
		Point direction;
		
		for(int i = start; i < num - 1; i++)
		{
			for(int j = i + 1; j < num; j++)
			{
				distance = Math.sqrt((bodies[i].getXPos() - bodies[j].getXPos()) * 
						 (bodies[i].getXPos() - bodies[j].getXPos()) +
						 (bodies[i].getYPos() - bodies[j].getYPos()) *
						 (bodies[i].getYPos() - bodies[j].getYPos()));
				
				magnitude = G * bodies[i].getMass() * bodies[j].getMass() / (distance * distance);
				direction = new Point(bodies[j].getXPos() - bodies[i].getXPos(),
						bodies[j].getYPos() - bodies[i].getYPos());
				
				bodies[i].setXForce(bodies[i].getXForce() + magnitude * direction.x / distance);
				bodies[j].setXForce(bodies[j].getXForce() - magnitude * direction.x / distance);
				bodies[i].setYForce(bodies[i].getYForce() + magnitude * direction.y / distance);
				bodies[j].setYForce(bodies[j].getYForce() - magnitude * direction.y / distance);
			}
		}
	}

	// This function is for the sequential instantiation of Collision.
	// This function defaults to use all of the bodies for moving the bodies.
	private void moveBodies() {
		moveBodiesHelper( 0, numBodies );
	}
	
	// This function is for the parallel instantiation of Collision.
	// This function takes the thread id and says tells the main function
	// how many bodies to go through as well as exactly which bodies are being
	// accounted for by this thread
	protected void moveBodies( int num ) {
		moveBodiesHelper( workerBodies[num], workerBodies[num + 1] );
	}
	
	// This function has been changed to run through a loop from a given input
	// rather than going from 0 to numBodies. This is because when going through
	// the threads, we will not be going through every body in every thread when
	// this function is called. We also did not want to just create a new function
	// because the code would be all the same, the only difference being the beginning
	// and end of the main loop within the function
	private void moveBodiesHelper( int start, int num ) {
		
		for(int i = start; i < num; i++)
		{
			Point deltaV;
			Point deltaP;
			
			deltaV = new Point(bodies[i].getXForce() / bodies[i].getMass() * DT,
					bodies[i].getYForce() / bodies[i].getMass() * DT);
			deltaP = new Point( (bodies[i].getXVel() + deltaV.x / 2) * DT,
					(bodies[i].getYVel() + deltaV.y / 2) * DT);
			
			bodies[i].setXVel(bodies[i].getXVel() + deltaV.x);
			bodies[i].setYVel(bodies[i].getYVel() + deltaV.y);
			
			bodies[i].setXPos(bodies[i].getXPos() + deltaP.x);
			bodies[i].setYPos(bodies[i].getYPos() + deltaP.y);
			
			// reset force vector
			bodies[i].setXForce(0);
			bodies[i].setYForce(0);
		}
	}
	
	// This function is for the sequential instantiation of Collision.
	// This function defaults to use all of the bodies for detecting collisions.
	private void detectCollisions() {
		detectCollisionsHelper( 0, numBodies );
	}
	
	// This function is for the parallel instantiation of Collision.
	// This function takes the thread id and says tells the main function
	// how many bodies to go through as well as exactly which bodies are being
	// accounted for by this thread
	protected void detectCollisions( int num ) {
		detectCollisionsHelper( workerBodies[num], workerBodies[num + 1] );
	}

	// This function has been changed to run through a loop from a given input
	// rather than going from 0 to numBodies. This is because when going through
	// the threads, we will not be going through every body in every thread when
	// this function is called. We also did not want to just create a new function
	// because the code would be all the same, the only difference being the beginning
	// and end of the main loop within the function
	private void detectCollisionsHelper( int start, int num )
	{
		double distance;
		
		for(int i = start; i < num - 1; i++)
		{
			for(int j = i + 1; j < num; j++)
			{
				distance = Math.sqrt((bodies[i].getXPos() - bodies[j].getXPos()) *
						(bodies[i].getXPos() - bodies[j].getXPos()) +
						(bodies[i].getYPos() - bodies[j].getYPos()) *
						(bodies[i].getYPos() - bodies[j].getYPos()));
				
				if( distance <= (bodies[i].getRadius() + bodies[j].getRadius()) )
				{
					ResolveCollision(i, j);
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

	public int getBodySize() {
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

	public int[] getWorkerBodies() {
		return workerBodies;
	}

	public void setWorkerBodies(int[] workerBodies) {
		this.workerBodies = workerBodies;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
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
		System.out.println("\tjava Collision w b s t [g]\n");
		System.out.println("\tw - Number of workers, 1 to 16. Ignored by sequential program.");
		System.out.println("\tb - number of bodies.");
		System.out.println("\ts - size of each body.");
		System.out.println("\tt - number of time steps.");	
		System.out.println("\tg - to use the gui, set to 1.");	
	}
	
}
