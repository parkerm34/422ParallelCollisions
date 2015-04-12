import java.util.concurrent.Semaphore;


public class CollisionWorker extends Thread {

	private int id;
	private Collision parent;
	private int numWorkers;
	private int numArrived = 0;
	private Semaphore e;
	private Semaphore barrier;
	
	public CollisionWorker( int id, Collision parent )
	{
		this.id = id;
		this.parent = parent;
		this.numWorkers = parent.getNumWorkers();
		barrier = new Semaphore(1);
		e = new Semaphore(numWorkers);
	}
	
	public void run()
	{
		for(int i = 0; i < parent.getNumTimeSteps(); i++)
		{
			if(parent.debug)
			{
				System.out.println("Before TR " + i + ": Number of collisions: " + parent.numCollisions);
				for(int j = parent.getWorkerBodies()[id]; j < parent.getWorkerBodies()[id + 1]; j++)
				{
					System.out.println("Body: " + j);
					System.out.println(" - Before move: xPos: " + parent.getBodies()[j].getXPos() + " yPos: " + parent.getBodies()[j].getYPos());
					System.out.println(" - Before move: xVel: " + parent.getBodies()[j].getXVel() + " yVel: " + parent.getBodies()[j].getYVel());
				}
			}
			
			parent.calculateForces( id );
			barrier( id );
			parent.moveBodies( id );
			barrier( id );
			parent.detectCollisions( id );
			barrier( id );
			
			if(parent.debug)
			{
				for(int j = parent.getWorkerBodies()[id]; j < parent.getWorkerBodies()[id + 1]; j++)
				{
					System.out.println("Body: " + j);
					System.out.println(" - After move: xPos: " + parent.getBodies()[j].getXPos() + " yPos: " + parent.getBodies()[j].getYPos());
					System.out.println(" - After move: xVel: " + parent.getBodies()[j].getXVel() + " yVel: " + parent.getBodies()[j].getYVel());
				}
				System.out.println();
			}
		}
	}
	
	private void barrier( int worker ) {
		//lock
		try {
			e.acquire();
			barrier.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		numArrived++;
		if(numArrived == numWorkers)
		{
			numArrived = 0;
			//start
			e.release(numWorkers);
		} else
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}//wait
		barrier.release();
		//unlock
	}
	
}
