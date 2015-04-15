#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <semaphore.h>
#include <math.h>
#include <sys/times.h>
#include <time.h>
#include <limits.h>

#define MAX 160

struct Body
{
	double xpos;
	double ypos;
	double xvel;
	double yvel;
	double xforce;
	double yforce;
	double mass;
	double radius;
};

double G = 6.67e-11;
double DT = 0.1f;
double MASS = 1.0f;

clock_t start, stop;
double time_spent;

struct timeval startTime, endTime;
long seconds, microseconds;

int numCollisions;
int numBodies;
double bodySize;
int numTimeSteps;
int numWorkers;
int workerBodies[16][MAX];
struct Body bodies[MAX];
int numArrived;
sem_t mutex;
sem_t barrier[3];

FILE *output, *end;
int debug = 1;
int csv = 0;

void readPoints()
{
	int count = 0;
	FILE* input = fopen("points.dat", "r");
	char x[50], y[50], vx[50], vy[50];

	if(input == NULL)
	{
		printf("could not open input file\n");
		exit(1);
	}

	while( count < numBodies )
	{
		fscanf(input, "%s %s %s %s", x, y, vx, vy);
		bodies[count].xpos = atof(x);
		bodies[count].ypos = atof(y);
		bodies[count].xvel = atof(vx);
		bodies[count].yvel = atof(vy);
		bodies[count].radius = bodySize;
		bodies[count].mass = MASS;
		count++;
	}
}

int length( int workerbods[16][MAX], int num )
{
	int count = 0;
	int i = workerbods[num][count];
	
	while( i != -1)
	{
		count++;
		i = workerbods[num][count];
	}
	return count;
}

void calculateForces( int num )
{
	double distance, magnitude;
	double xdirection, ydirection;
	int body;
		
	for(int i = 0; i < length(workerBodies, num); i++)
	{
		body = workerBodies[num][i];
		for(int j = body + 1; j < numBodies; j++)
		{
			distance = sqrt((bodies[body].xpos - bodies[j].xpos) * 
						(bodies[body].xpos - bodies[j].xpos) +
						(bodies[body].ypos - bodies[j].ypos) *
						(bodies[body].ypos - bodies[j].ypos));
				
			magnitude = G * bodies[body].mass * bodies[j].mass / (distance * distance);
			xdirection = bodies[j].xpos - bodies[body].xpos;
			ydirection = bodies[j].ypos - bodies[body].ypos;
				
			bodies[body].xforce = bodies[body].xforce + magnitude * xdirection / distance;
			bodies[j].xforce = bodies[j].xforce - magnitude * xdirection / distance;
			bodies[body].yforce = bodies[body].yforce + magnitude * ydirection / distance;
			bodies[j].yforce = bodies[j].yforce - magnitude * ydirection / distance;
		}
	}
}

void moveBodies( int num )
{
	int body;
		
	for(int i = 0; i < length(workerBodies, num); i++)
	{
		body = workerBodies[num][i];
			
		double deltaVx, deltaVy;
		double deltaPx, deltaPy;
			
		deltaVx = bodies[body].xforce / bodies[body].mass * DT;
		deltaVy = bodies[body].yforce / bodies[body].mass * DT;
		deltaPx = (bodies[body].xvel + deltaVx / 2) * DT;
		deltaPy = (bodies[body].yvel + deltaVy / 2) * DT;
			
		bodies[body].xvel = bodies[body].xvel + deltaVx;
		bodies[body].yvel = bodies[body].yvel + deltaVy;
			
		bodies[body].xpos = bodies[body].xpos + deltaPx;
		bodies[body].ypos = bodies[body].ypos + deltaPy;
			
		// reset force vector
		bodies[body].xforce = 0;
		bodies[body].yforce = 0;
	}
}

void ResolveCollision( int b1, int b2)
{
	double distSquared;
	double v1fx, v1fy, v2fx, v2fy;
	double v1nfxNumerator, v1txNumerator, v2nfxNumerator, v2txNumerator;
	double v1nfyNumerator, v1tyNumerator, v2nfyNumerator, v2tyNumerator;
	double diffXPos, diffYPos;
		
	// dist = b1.r + b2.r
	distSquared = (bodies[b1].radius + bodies[b2].radius) * 
			(bodies[b1].radius + bodies[b2].radius);
		
	// x2 - x1
	diffXPos = bodies[b2].xpos - bodies[b1].xpos;
	// y2 - y1
	diffYPos = bodies[b2].ypos - bodies[b1].ypos;
		
	// Find final normal and tangent vectors for Body 1's x
	v1nfxNumerator = bodies[b2].xvel * diffXPos * diffXPos +
			bodies[b2].yvel * diffXPos * diffYPos;
	v1txNumerator = bodies[b1].xvel * diffYPos * diffYPos -
			bodies[b1].yvel * diffXPos * diffYPos;
	// Find the final total x vector for Body 1
	v1fx = (v1nfxNumerator + v1txNumerator) / distSquared;
		
	// Find final normal and tangent y vectors for Body 1
	v1nfyNumerator = bodies[b2].xvel * diffXPos * diffYPos +
			bodies[b2].yvel * diffYPos * diffYPos;
	v1tyNumerator = -(bodies[b1].xvel * diffYPos * diffXPos) +
			bodies[b1].yvel * diffXPos * diffXPos;
	// Find the final total y vector for Body 1
	v1fy = (v1nfyNumerator + v1tyNumerator) / distSquared;
		
	// Find final normal and tangent x vectors for Body 2
	v2nfxNumerator = bodies[b1].xvel * diffXPos * diffXPos +
			bodies[b1].yvel * diffXPos * diffYPos;
	v2txNumerator = bodies[b2].xvel * diffYPos * diffYPos -
			bodies[b2].yvel * diffXPos * diffYPos;
	// Find the final total x vector for Body 1
	v2fx = (v2nfxNumerator + v2txNumerator) / distSquared;
		
	// Find final normal and tangent y vectors for Body 2
	v2nfyNumerator = bodies[b1].xvel * diffXPos * diffYPos +
			bodies[b1].yvel * diffYPos * diffYPos;
	v2tyNumerator = -(bodies[b2].xvel * diffYPos * diffXPos) +
			bodies[b2].yvel * diffXPos * diffXPos;
	// Find the final total y vector for Body 1
	v2fy = (v2nfyNumerator + v2tyNumerator) / distSquared;
		
	// Update the final velocities
	bodies[b1].xvel = v1fx;
	bodies[b1].yvel = v1fy;
	bodies[b2].xvel = v2fx;
	bodies[b2].yvel = v2fy;
}

void detectCollision( int num )
{
	double distance;
	int body;
		
	for(int i = 0; i < length(workerBodies, num); i++)
	{
		body = workerBodies[num][i];
		for(int j = body + 1; j < numBodies; j++)
		{
			distance = sqrt((bodies[body].xpos - bodies[j].xpos) *
					(bodies[body].xpos - bodies[j].xpos) +
					(bodies[body].ypos - bodies[j].ypos) *
					(bodies[body].ypos - bodies[j].ypos));
				
			if( distance <= (bodies[body].radius + bodies[j].radius) )
			{
				ResolveCollision(body, j);
				numCollisions++;
			}
		}
	}
}

void collisionSeq( int bods, double size, int timeSteps )
{
	numBodies = bods;
	bodySize = size;
	numTimeSteps = timeSteps;
	numCollisions = 0;
	
	if(debug == 1)
		printf("Start Sequential\n");

//	workerBodies = malloc(sizeof(int) * numBodies);

	for(int i = 0; i < 16; i++)
	{
		for(int j = 0; j < MAX; j++)
			workerBodies[i][j] = -1;
	}

	for(int i = 0; i < numBodies; i++)
		workerBodies[0][i] = i;

	readPoints();

	start = clock();
	gettimeofday(&startTime, NULL);

	for(int i = 0; i < numTimeSteps; i++)
	{
		if(debug == 1)
		{
			printf("Before TR %d: Number of Collisions: %d\n", i, numCollisions);
			for(int j = 0; j < numBodies; j++)
			{
				printf("Body: %d\n", j);
				printf(" - Before move: xPos: %f yPos: %f\n", bodies[j].xpos, bodies[j].ypos);
				printf(" - Before move: xVel: %f yVel: %f\n", bodies[j].xvel, bodies[j].yvel);
			}
		}

		calculateForces( 0 );
		moveBodies( 0 );
		detectCollision( 0 );

		if(debug == 1)
		{
			for(int j = 0; j < numBodies; j++)
			{
				printf("Body: %d\n", j);
				printf(" - After move: xPos: %f yPos: %f\n", bodies[j].xpos, bodies[j].ypos);
				printf(" - After move: xVel: %f yVel: %f\n", bodies[j].xvel, bodies[j].yvel);
			}
			printf("\n");
		}
	}


	gettimeofday(&endTime, NULL);
	stop = clock();
	time_spent = (double) (stop-start) / CLOCKS_PER_SEC;
	seconds = endTime.tv_sec - startTime.tv_sec;
	if(seconds > 0)
		microseconds = (startTime.tv_usec - endTime.tv_usec)/1000;
	else
		microseconds = (endTime.tv_usec - startTime.tv_usec)/1000;

	printf("computation time: %lu seconds %lu milliseconds\n", seconds, microseconds);
	printf("number of collisions detected = %d\n", numCollisions);

	if(csv == 1)
	{
		output = fopen("output2.txt", "w");
		if(output == NULL)
		{
			printf("could not open output file\n");
			exit(1);
		}
		fprintf(output, "%lu,%lu\n", seconds, microseconds);
		
	}

	end = fopen("resultsC.txt", "w");
	fprintf(end, "Final Positions:\n");

	for(int i = 0; i < numBodies; i++)
	{
		fprintf(end, "Body %d: ( %.4f, %.4f )\n", i, bodies[i].xpos, bodies[i].ypos);
	}

	fprintf(end, "\nFinal Velocities:\n");
	for(int i = 0; i < numBodies; i++)
	{
		fprintf(end, "Body %d: ( %.4f, %.4f )\n", i, bodies[i].xvel, bodies[i].yvel);
	}
}

void parseBodies()
{
	int curr, helper;
	int division = numBodies / numWorkers;
	//boolean iterateFirst = division % 2 == 0;
	int iterateFirst = division % 2;	

	for(int i = 0; i < numWorkers; i++)
	{
		if(iterateFirst && i < numBodies % numWorkers)
		{
			for(int j = 0; j < division + 1; j++)
			{
				workerBodies[i][j] = 1;
			}
		}
		else if(!iterateFirst && i >= (numWorkers - numBodies % numWorkers))
		{
			for(int j = 0; j < division + 1; j++)
			{
				workerBodies[i][j] = 1;
			}
				//workerBodies[i] = new int[division + 1];
		}
		else
		{
			for(int j = 0; j < division; j++)
			{
				workerBodies[i][j] = 1;
			}
			//workerBodies[i] = new int[division];
		}
	}
		
	for(int i = 0; i < numWorkers; i++)
	{
		curr = 0;
		for(int j = 0; j < length(workerBodies, i); j++)
		{
			if(j%2 == 0)
				helper = i;
			else
				helper = numWorkers - (i + 1);
			workerBodies[i][j] = curr + helper;
			curr += numWorkers;
		}
	}
}

void barrierF(int barrierIndex)
{
//	int result, result2;
	// All processes before the last wait
	sem_wait(&mutex);
	if(numArrived < numWorkers)
	{
		numArrived = numArrived + 1;
		sem_post(&mutex);
		sem_wait(&barrier[barrierIndex]);
	}
	else // all processes waiting, release them all
	{
		numArrived = 1;
		for( int i = 0; i < numWorkers; i++)
		{
			sem_post(&barrier[barrierIndex]);
		}
		sem_post(&mutex);
	}
}

void *Worker( void *arg )
{
	int id = (int) arg;

	for(int i = 0; i < numTimeSteps; i++)
	{
		if(debug == 1)
		{
			printf("Before TR %d: Number of collisions: %d\n", i, numCollisions);
			int body;
			for(int j = 0; j < length( workerBodies, id ); j++)
			{
				body = workerBodies[id][j];
				printf("Body: %d\n", j);
				printf(" - Before move: xPos: %f yPos: %f\n", bodies[body].xpos, bodies[body].ypos);
				printf(" - Before move: xVel: %f yVel: %f\n", bodies[body].xvel, bodies[body].yvel);
			}
		}
		calculateForces( id );
		barrierF(0);
			
		moveBodies( id );
		barrierF(1);
			
		detectCollision( id );
		barrierF(2);
			
		
			
		if(debug == 1)
		{
			int body;
			for(int j = 0; j < length(workerBodies, id); j++)
			{
				body = workerBodies[id][j];
				printf("Body: %d\n", j);
				printf(" - After move: xPos: %f yPos: %f\n", bodies[body].xpos , bodies[body].ypos);
				printf(" - After move: xVel: %f yVel: %f\n", bodies[body].xvel, bodies[body].yvel);
			}
			printf("\n");
		}
	}
}

void collisionPar( int workers, int bods, double size, int timeSteps )
{
	if(debug == 1)
		printf("Start Parellel\n");

	pthread_t workerid[16];
	pthread_attr_t attr;

	numWorkers = workers;
	numBodies = bods;
	bodySize = size;
	numTimeSteps = timeSteps;
	numCollisions = 0;
	numArrived = 1;

//	int temp1, temp2;

	sem_init(&barrier[0], 0, 0);
	sem_init(&barrier[1], 0, 0);
	sem_init(&barrier[2], 0, 0);
	sem_init(&mutex, 0, 1);

	pthread_attr_init(&attr);
	pthread_attr_setscope(&attr, PTHREAD_SCOPE_SYSTEM);

//	pthread_mutex_init(&mutex, NULL);
//	pthread_cond_init(&barrier, NULL);

	readPoints();

	for(int i = 0; i < 16; i++)
	{
		for(int j = 0; j < MAX; j++)
			workerBodies[i][j] = -1;
	}

	parseBodies();

	start = clock();
	gettimeofday(&startTime, NULL);

	for(int i = 0; i < numWorkers; i++)
		pthread_create(&workerid[i], &attr, Worker, (void *) i);
	for(int i = 0; i < numWorkers; i++)
		pthread_join(workerid[i], NULL);


	gettimeofday(&endTime, NULL);
	stop = clock();
	time_spent = (double) (stop-start) / CLOCKS_PER_SEC;
	seconds = endTime.tv_sec - startTime.tv_sec;
	if(seconds > 0)
		microseconds = (startTime.tv_usec - endTime.tv_usec)/1000;
	else
		microseconds = (endTime.tv_usec - startTime.tv_usec)/1000;

	printf("computation time: %lu seconds %lu milliseconds\n", seconds, microseconds);
	printf("number of collisions detected = %lu\n", numCollisions);

	if(csv == 1)
	{
		output = fopen("output2.txt", "w");
		if(output == NULL)
		{
			printf("could not open output file\n");
			exit(1);
		}
		fprintf(output, "%lu,%lu\n", seconds, microseconds);
		
	}

	end = fopen("resultsC.txt", "w");
	fprintf(end, "Final Positions:\n");

	for(int i = 0; i < numBodies; i++)
	{
		fprintf(end, "Body %d: ( %.4f, %.4f )\n", i, bodies[i].xpos, bodies[i].ypos);
	}

	fprintf(end, "\nFinal Velocities:\n");
	for(int i = 0; i < numBodies; i++)
	{
		fprintf(end, "Body %d: ( %.4f, %.4f )\n", i, bodies[i].xvel, bodies[i].yvel);
	}
}

void usage()
{
	printf("Collisions Usage:\n\n");
	printf("\t./Collision w b s t [c]\n\n");
	printf("\tw - Number of workers, 1 to 16. 1 starts sequential.\n");
	printf("\tb - number of bodies.\n");
	printf("\ts - size of each body (radius).\n");
	printf("\tt - number of time steps.\n");
	printf("\tc - optional, flag for creating CSV for output testing.\n\n");
}

int main( int argc, char* argv[] )
{
	if( argc < 4 )
	{
		printf("Too few arguments: 4 are required, %d were given\n", argc);
		usage();
		exit(1);
	}
	if( argc == 6 )
		csv = 1;

	if( atoi(argv[1]) == 1 )
	{
		collisionSeq( atoi(argv[2]), atof(argv[3]), atoi(argv[4]));
	}
	else
	{
		collisionPar( atoi(argv[1]), atoi(argv[2]), atof(argv[3]), atoi(argv[4]));

	}
}
