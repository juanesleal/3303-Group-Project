The Project has 4 key classes: Elevator.java, ElevatorSystem.java, Event.java, EventHolder.java, Scheduler.java and Floor.java

Event.java
-The instance creator for any button presses, includes the time, floor, fbutton, cbutton 

EventHolder.java
-Holds messages and data from the floor elevator and scheduler, put and get methods need internal logic to put data and messages into the right variable.

Elevator.java
-Generates a new Elevator subsystem that communicates using the given EventHolder, gets the values of floor data from a file and sends new data to Scheduler.

ElevatoMovement.java
- System of threads that each give and take data, Scheduler is used to communicate between the Floor and the Elevator, where the Floor represents requests made by button presses.
Elevator is told where to go by the scheduler.

Scheduler.java
-Gets the floordata from floor.java, checks for any events using EventHolder.java, sends message to Elevator and sends any messages from the Elevator to the floor.
-Now implements a state machine that implements each state in a communication process between the elevator and the scheduler, and the floor and the scheduler

Floor.java
-Generates a new floor subsystem that communicates using the given EventHolder, gets the values of floor data from a file and sends new data to Scheduler.

The Project also includes FloorEventTest.txt Which is the test values for the classes.

Set-Up and Running Instructions:
-To set up make sure that the files are all together in the same location open them in a java IDE and then run ElevatorSystem.java

