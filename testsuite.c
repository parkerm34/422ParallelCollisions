/* Author: Parker Mathewson
 * Date: 2/14/15
 * SID: 23118888
 * File: sortSeq.c
 *
 * Purpose: This file is to implement quicksort in sequential programming
 *            with using strcmp() as the comparator for sorting.
 *            This file will sort an input from the command line (a file)
 *            that has at maximum of 500,000 lines and will sort each line
 *            and output a sorted file without adding anything or missing
 *            anything in the sorting algorithm.
 *
 *
 * INPUT: a file to be sorted, 'any' format
 *
 * OUTPUT: a sorted file containing the same contents as the input file
 *
 * DEPENDENCIES: stdio.h
 */

#include <string.h>
#include <stdlib.h>
#include <stdio.h>

FILE *inputFile, *outFile, *helpFile, *helpFile2;

void usage()
{
	printf("\nTESTSUITE USAGE:\n\n");
	printf("testsuite <numTestCases> <outFile> <gui>\n\n");
	printf("numTestCases is the number of test cases to run in the tests directory starting from 1.\n");
	printf("outFile is the name of the output csv file.\n");
	printf("gui is whether to test the Java's GUI version if set to 1. This does not run C at all in this case.\n");
	printf("This suite is made for the test files commands.txt and guicommands.txt\n\n");
	printf("This suite creates a file called by the 3rd argument that puts all the times into a spreadsheet.\n\n\n");
}

int main( int argc, char *argv[] )
{
	char runC[50], runJava[100], command[20];
	char threads[50], bodies[50], size[50], timesteps[50], flag[50];
	char helper[50];
	if(argv[1] == NULL || argv[2] == NULL)
        {
                usage();
                exit(1);
        }

	char* num = argv[1];
	int gui;
	int cases = atoi(num);
	int count = 0;
	
	if(argv[3] == NULL)
		gui = 0;
	else
		gui = atoi(argv[3]);

	if(gui != 1)
		inputFile = fopen("commands.txt", "r");
	else
		inputFile = fopen("guicommands.txt", "r");

	if(inputFile == NULL)
	{
		printf("input command file could not be opened. Exiting.\n\n");
		return 1;
	}

	outFile = fopen(argv[2], "w");
	if(outFile == NULL)
	{
		printf("output file could not be opened. Exiting.\n\n");
                return 1;
	}

	system("make clean");
	system("make CollisionController");
	system("make Collision");

	fprintf( outFile, " , , , ,Java,Java");
	if( gui != 1 )
		fprintf( outFile, ",C,C");
	fprintf( outFile, "\n");

	fprintf( outFile, "Threads,Bodies,Size,TimeSteps,Seconds,Milliseconds");
	if( gui != 1 )
		fprintf( outFile, ",Seconds,Milliseconds");
	fprintf( outFile, "\n");

	while(count < cases)
	{
		fscanf(inputFile, "%s %s %s %s %s", threads, bodies, size, timesteps, flag);
		sprintf(command, "%s %s %s %s %s 1", threads, bodies, size, timesteps, flag);

		printf("\n-------- JAVA TEST CASE %d --------\n", count);
		sprintf(runJava, "java -cp bin controller.CollisionController %s", command);
		system(runJava);

		fprintf( outFile, "%s,%s,%s,%s,", threads, bodies, size, timesteps);
		helpFile = fopen("output.txt", "r");
		fscanf(helpFile, "%s", helper);
		fprintf( outFile, "%s", helper);

		if( gui != 1 )
		{
			printf("\n++++++++ C TEST CASE %d ++++++++\n", count);
			sprintf(runC, "./Collision %s", command);
			system(runC);

			helpFile2 = fopen("outputC.txt", "r");
			fscanf(helpFile2, "%s", helper);
			fprintf( outFile, "%s", helper);
		}
		fprintf( outFile, "\n");

		printf("\n----**** END TEST CASE %d ****----\n", count);
		count++;
	}
	return 0;
}
