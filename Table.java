
/**
 * EventHolder, events could be from floor
 * This means the put and get methods need to check the current thread name to decipher what is being requested or sent
 */
public class EventHolder
{
    private Object floorData = null; // event from the floor
    private Object elevatorData = null; // data from the elevator
    private Object floorMessage = null; // message for the Floor
    private Object elevatorMessage = null; //message for the Elevator
    private boolean empty = true; // empty?
    /**
     * put executes for both the Floor Elevator, and Scheduler subsystems
     */
    public synchronized void put(Object item) {
        //if (item instanceof Event) {

        //}
        while (!empty) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
        if (Thread.currentThread().getName() == "Floor") {
            event = item;
        }
        else if (Thread.currentThread().getName() == "Elevator") {
            message = item;
        }else {
            System.out.println("Unexpected Put operation used from: " + Thread.currentThread().getName());
        }
        empty = false;
        System.out.println(Thread.currentThread().getName() + " sent " + item);
        notifyAll();
    }
    
    /**
     * Gets String from EventHolder.
     * returns string, removes it from contents, and sets empty.
     * notifyAll() interupts all threads in wait().
     */
    public synchronized Object[] get(Object ingredient) {
        System.out.println(Thread.currentThread().getName() + " ready to consume.");
        while (empty || (contents[1] == ingredient || contents[0] == ingredient)) {
            try {
                wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        Object[] items = contents;
        contents = null;
        empty = true;
        System.out.println(Thread.currentThread().getName() + " consumed " + items[0] + items[1]);
        notifyAll();
        return items;
    }
}
