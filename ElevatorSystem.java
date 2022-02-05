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


class Scheduler implements Runnable
{
    private EventHolder eventHolder;

    public Scheduler(EventHolder eH)
    {
        this.eventHolder = eH;
    }

    public void run()
    {
        while(true) {
            //we first need to get the Floordata
            Object event = eventHolder.getFloor();
            String msg;
            if (event instanceof Event) {
                //this nicely prints the event that is being read
                msg = "At " + ((Event) event).time + " Someone requested to go " + ((Event) event).fButton + " to floor " + ((Event) event).cButton + " from " + ((Event) event).floor;
            }else {
                System.out.println("error: event should be of type Event");
                msg = "";
            }
            //now we send the event msg to Elevator
            eventHolder.putMsg(msg, "Elevator");
            //wait for Elevator to send something back
            event = eventHolder.getElevator();
            //now we can send a msg to Floor
            eventHolder.putMsg(event, "Floor");
        }
    }
}

class Floor implements Runnable {

    private EventHolder eventHolder; // eventHolder is used for us to send and retereive data and messages

    /**
     * Generates a new floor subsystem that communicates using the specified EventHolder
     * @param eH, specifies the eventHolder for shared memory.
     */
    public Floor(EventHolder eH) {
        this.eventHolder = eH;
    }

    public void run() {
        try {
            //    Read floor data values from file
            BufferedReader br = new BufferedReader(new FileReader("FloorEventTest.txt"));
            Event fd;

            String line;
            while ((line = br.readLine()) != null) {
                //    Read line and convert to floor data
                fd = Event.parseString(line);

                //    Send data to scheduler
                System.out.println("== Floor Subsystem sending data << " + fd + " >> to schedular");
                this.eventHolder.put(fd);

                //    Sleep unessesary since get and put ait for data
                Object receivedFd = this.eventHolder.getMsgF();
                System.out.println("== Floor Subsystem receiving data << " + receivedFd + " >> from schedular");
            }

            this.eventHolder.put(null);

            br.close();

        }catch(IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("== Floor Subsystem finished");
    }
}
class Elevator implements Runnable
{
    private EventHolder eventHolder;

    public Elevator(EventHolder eH) {
        eventHolder = eH;
    }

    public void run() {
        while (true) {
            Object event = eventHolder.getMsgE();
            //we've received a message from Scheduler, now send something back, ill make it an event, but it doesn't need to be
            eventHolder.put("Elevator Data");
        }
    }
}

