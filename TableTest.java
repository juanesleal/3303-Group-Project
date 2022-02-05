import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/** BoxTest.java
 *
 * Testing Box by creating a producer and consumer.
 * 
 * @author Lynn Marshall
 * @version 1.00
 * 
 */

public class TableTest
{
    public static void main(String[] args)
    {
        Thread scheduler, floor, elevator;
        Table eventHolder;

        eventHolder = new Table();

        // Create the Scheduler, Floor, and Elevator threads, passing each thread
        // a reference to the shared BoundedBuffer object.
        scheduler = new Thread(new Scheduler(eventHolder),"Scheduler");
        floor = new Thread(new Floor(eventHolder),"Floor");
        elevator = new Thread(new Elevator(), "Elevator");
        scheduler.start();
        floor.start();
        elevator.start();
    }
}


class Scheduler implements Runnable
{
    private Table eventHolder;

    public Scheduler(Table table)
    {
        this.eventHolder = table;
    }

    public void run()
    {
        for(int i = 0; i < 10; i++) {
            //use the get logic to wait for and get an event to process.
            Object event = eventHolder.get();
        }
    }
}
class Floor implements Runnable
{

    private Table eventHolder;
    public Floor(Table table)
    {
        this.eventHolder = table;
    }

    public void run()
    {
        //As the Floor, we read in events from a file:
        try {
            File inputEvents = new File("FloorEventTest.txt");
            Scanner eventScanner = new Scanner (inputEvents);
            while (eventScanner.hasNextLine()) {
                String event = eventScanner.nextLine();
                System.out.println(event);
                eventHolder.put(event);
            }
        } catch (FileNotFoundException e) {
            System.out.println("file error: Floor input file not found");
        }
    }
}
class Elevator implements Runnable
{
    public void run() {
        System.out.println("hiya");
    }
}

