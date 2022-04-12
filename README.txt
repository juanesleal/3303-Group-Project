The Project has 10 classes:

Communicator.Java:
-mediator for the communications of all classes, translates Message objects to UDP using a custom protocol, and vice versa.

DataTable.java:
-class specifically for JUNIT and setting up the data display on the GUI

Elevator.java
-Handles various requests from the Scheduler using the ElevatorState machine.

ElevatorMovement.java
-All the code that handles the location and velocity of elevators, breaks down to high school physics but is fairly daughting because of exception handling

ElevatorState.java
-Mediator between the ElevatorMovement and the Elevator class, elevator state machine determines what various commands from the Elevator should do by aquiring necessary data from the ElevatorMovement.

Floor.java
-reasonably simple, reads the input file and keeps it in a data structure for on -demand access by the Scheduler.
    Note: in floorEventTest the trailing 0's represent the faults.
    the first 0 is a boolean for if there is a travel fault.
    the second 0 is a boolean for if the doors are stuck in the closed position on arrival.

Initializer
-makes it much easier to run and track the 4 elevators, floor and scheduler, runs using a program argument for the amount of elevators and the .txt input file.

Message
-simple data structure to pass messages between classes

Output
-output listens on a port and is sent many messages while the Initializer runs.

Scheduler
-The main class for all the marbles. the Scheduler uses a state machine to get events and sequentially handle them by sorting through each elevator's "TimeFor" responses. uses a Timer which starts a new thread just befor a Elevator is expected to arrive.


The Project also includes FloorEventTest2.txt Which is the test calues for the classes.

Set-Up and Running Instructions:
-Run Output, then initializer with the program arguments 4, "FloorEventTest2.txt"
for easiest comprehension, focus on the first few evens or search the output for the keywords "Already Here", "Already Going", "SHUTDOWN" and "On the way"