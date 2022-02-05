
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
        floor = new Thread(new Floor(),"Floor");
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
        for(int i = 0; i < 10; i++) {
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
class Floor implements Runnable
{
    //your code here
    public void run() {

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
            eventHolder.put("Elevator sending data");
        }
    }
}

