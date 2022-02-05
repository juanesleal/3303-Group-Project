
/**
 * EventHolder, holds messages and data from the floor elevator and scheduler.
 * the put and get methods need internal logic to put data and messages into the right variable.
 */
public class EventHolder
{
    private Object floorData = null; // event from the floor
    private Object elevatorData = null; // data from the elevator
    private Object floorMessage = null; // message for the Floor
    private Object elevatorMessage = null; //message for the Elevator
    private boolean empty = true; // empty?
    /**
     * put executes for both the Floor and Elevator, internal logic is needed to know where to put the data
     */
    public synchronized void put(Object item) {
        System.out.println(Thread.currentThread().getName() + " putting Data");
        while (!empty) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (Thread.currentThread().getName() == "Floor") {
            floorData = item;
        }
        else if (Thread.currentThread().getName() == "Elevator") {
            elevatorData = item;
        }else {
            System.out.println("Unexpected Put operation used from: " + Thread.currentThread().getName());
        }
        empty = false;
        System.out.println(Thread.currentThread().getName() + " sent " + item);
        notifyAll();
    }
    /**
     * putMsg is specifically designed to send messages to floor and elevator, only implemented in the scheduler class
     */
    public synchronized void putMsg(Object item, String to) {
        System.out.println(Thread.currentThread().getName() + " putting a message.");
        while (!empty) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (to == "Floor") {
            floorMessage = item;
        }
        else if (to == "Elevator") {
            elevatorMessage = item;
        }else {
            System.out.println("Unexpected Putmssg operation used to send to: " + to);
        }
        empty = false;
        System.out.println(Thread.currentThread().getName() + " sent " + item + " to " + to);
        notifyAll();
    }
    /**
     * Gets data from floor or elevator, depending which method is called
     * empty the appropriate data variable, returning the data it used to have.
     * notifyAll() interupts all threads in wait().
     */
    public synchronized Object getFloor() {
        System.out.println(Thread.currentThread().getName() + " getting data");
        while (empty || (floorData == null)) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        Object item = floorData;
        floorData = 0;
        empty = true;
        System.out.println(Thread.currentThread().getName() + " got " + item);
        notifyAll();
        return item;
    }
    public synchronized Object getElevator() {
        System.out.println(Thread.currentThread().getName() + " getting data");
        while (empty || (elevatorData == null)) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        Object item = elevatorData;
        elevatorData = 0;
        empty = true;
        System.out.println(Thread.currentThread().getName() + " got " + item);
        notifyAll();
        return item;
    }
    /**
     * Gets message from scheduler, depending on what thread is asking.
     * returns message to caller
     * notifyAll() interupts all threads in wait().
     */
    public synchronized Object getMsgF() {
        System.out.println(Thread.currentThread().getName() + " getting a message.");
        while (empty || (floorMessage == null)) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        Object item = floorMessage;
        floorMessage = null;
        empty = true;
        System.out.println(Thread.currentThread().getName() + " got " + item);
        notifyAll();
        return item;
    }
    public synchronized Object getMsgE() {
        System.out.println(Thread.currentThread().getName() + " getting a message.");
        while (empty || (elevatorMessage == null)) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        Object item = elevatorMessage;
        elevatorMessage = null;
        empty = true;
        System.out.println(Thread.currentThread().getName() + " got " + item);
        notifyAll();
        return item;
    }
}
