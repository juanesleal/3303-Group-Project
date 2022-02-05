package main;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** ElevatorSystem.java
 *
 *  system of threads that each produce and consume data
 * Scheduler is a medium between Floor and Elevator, Floor represents requests made by button presses.
 * Elevator is told where to go by the scheduler.
 *
 */

public class ElevatorSystem
{
    public static void main(String[] args)
    {
        Thread scheduler, floor, elevator;
        EventHolder eventHolder;

        eventHolder = new EventHolder();

        // Create the Scheduler, Floor, and Elevator threads, passing each thread
        // a reference to the shared BoundedBuffer object.
        scheduler = new Thread(new Scheduler(eventHolder),"Scheduler");
        floor = new Thread(new Floor(eventHolder),"Floor");
        elevator = new Thread(new Elevator(eventHolder), "Elevator");
        scheduler.start();
        floor.start();
        elevator.start();
    }
}






