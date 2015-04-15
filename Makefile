CC=gcc

LDFLAGS=-lpthread -lm 

CFLAGS=-Wall -g -std=gnu99 -o

Collision.o: Collision.c
	$(CC) Collision.c $(LDFLAGS) $(CFLAGS) -c

Collision: Collision.c
	$(CC) $(CFLAGS) Collision Collision.c $(LDFLAGS)

CollisionController: src/controller/*.java src/model/*.java src/view/*.java
	javac -d bin src/controller/*.java src/model/*.java src/view/*.java -classpath bin

testsuite: testsuite.c
	$(CC) $(CFLAGS) testsuite testsuite.c

clean: 
	rm -rf Collision.o Collision testsuite testsuite.dSYM

all:
	$(CC) $(CFLAGS) Collision Collision.c $(LDFLAGS)
	$(CC) $(CFLAGS) testsuite testsuite.c
	javac -d bin src/controller/*.java src/model/*.java src/view/*.java -classpath bin
