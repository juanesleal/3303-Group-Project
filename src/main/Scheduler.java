package main;


//TODO
// apperently this system is responsible for handling faults like an elevator's doors not opening,
// an elevator getting stuck between floors, and packet loss...
// I'm not sure how you would solve these issues without just designing them out ie: get reply messages for everything,
// and make sure an elevator doesn't move until it gets a reply.
// The elevator should just be designed to never move if the doors are open... why should the scheduler have to do this?
//TODO apperently roughly half of the people will come in at the GROUND Floor

/**
 * receive from elevator (look for "Availible")
 *          check if valid, send response
 * receive from Floor (add data to queue (chronologically, according to the time in the data.))
 *          check if valid, send response
 * loop(while queue has something)
 *      handle event
 *          ask elevators how long till they get to the floor (if no reply, all elevators are busy)
 *              wait for reply, store in timeTillRequest[1] = time for elevator 1, [2] is for elevator 2...
 *              NOTE might be worthwhile to store a minimum possible time for an elevator to get to a floor. (Elevator responds to time request with a flag for minimum possible time.)
 *          process that
 *          ask 1 elevator to go to the floor (set a timer for the amount that they said it would take them to get there)
 *              wait for reply (if they say NO, we need to ask the next best Elevator)
 *      loop
 */

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

public class Scheduler {
    private Communicator eCommunicator;
    private Communicator fCommunicator;
    private LinkedList<Message> queue =  new LinkedList<>();
    //todo maybe have this sorted, if it is sorted, then it needs to be a list or smth
    //time TillRequest[0] is the floor for which the times are relevant
    private int[] timeTillRequest;

    public Scheduler() {
        Communicator com = new Communicator();
        eCommunicator = new Communicator(com.SCHEDULER_EPORT, "Scheduler");
        fCommunicator = new Communicator(com.SCHEDULER_FPORT, "Scheduler");
        timeTillRequest = new int[com.MAXELEVATORS + 1];
    }

    public static void main(String[] args) {
        Clock time = Clock.systemDefaultZone();
        Scheduler s = new Scheduler();
        while(true) {
            Message m = s.eCommunicator.receive();
            System.out.println(m.getData()[0]);
            if (m.getData()[0].equals("Availible")) {
                //send OK back to wherever we got it
                s.eCommunicator.send(new Message(new String[] {"OK"}, time.millis(), m.getToFrom()));
            }
            /*
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
            eventHolder.putMsg(new Message(msg, 3), "Elevator");
            //wait for Elevator to send something back
            event = eventHolder.getElevator();
            //now we can send a msg to Floor
            eventHolder.putMsg(new Message((String)event, 0), "Floor");
            */
        }
    }
}
