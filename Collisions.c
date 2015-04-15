#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct Point
{
	double x;
	double y;
};

struct Body
{
	struct Point pos;
	struct Point vel;
	struct Point force;
	double mass;
	double radius;
};

double G = 6.67e-11;
double DT = 0.01f;
double MASS = 1.0f;

int numCollisions;
int numBodies;
int bodySize;
int numTimeSteps;
int numWorkers;
int workerBodies[][];
struct Body bodies[];
int numArrived;
Semaphore mutex;
Semaphore barrier[];

FILE output;
int debug = 1;
int csv = 0;

void readPoints()
{
	int count = 0;
	FILE* input = fopen("points.dat", "r");
	char x[50], y[50], vx[50], vy[50];

	bodies = malloc(sizeof(Body) * numBodies);

	while( count < numBodies )
	{
		fscanf(input, "%s %s %s %s", x, y, vx, vy);
		bodies[count]->pos->x = x;
		bodies[count]->pos->y = y;
		bodies[count]->vel->x = vx;
		bodies[count]->vel->x = vy;
		bodies[count]->radius = bodySize;
		bodies[count]->mass = MASS;
		count++;
	}
	for( int i = 0; i < numBodies; i++)
	{
		printf("%d %d\n", i, bodies[i]->pos->x);
	}
}

void collisionSeq( int bods, double size, int timeSteps )
{
	numBodies = bods;
	bodySize = size;
	numTimeSteps = timeSteps;
	
	if(debug == 1)
		print("Start Sequential\n");

	workerBodies = malloc(sizeof(int) * numBodies);

	for(int i = 0; i < numBodies; i++)
		workerBodies[0][i] = i;

	readPoints();
}



void collisionPar( int workers, int bods, double size, int timeSteps )
{
	numWorkers = workers;
	numBodies = bods;
	bodySize = size;
	numTimeSteps = timeSteps;
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

	if( atoi(argv[0]) == 1 )
	{
		collisionSeq( atoi(argv[1]), atof(argv[2]), atoi(argv[3]));
	}
	else
	{
		collisionPar( atoi(argv[0]), atoi(argv[1]), atof(argv[2]), atoi(argv[3]));

	}
}
