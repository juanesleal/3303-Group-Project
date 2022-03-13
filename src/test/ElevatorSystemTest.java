package test;

import main.*;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElevatorSystemTest 
{
    /* Example of asserting true */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    /* Example of asserting false */
    @Test
    public void shouldAnswerWithFalse()
    {
        assertFalse( false );
    }
    //test whether the floor data gives us an event data structure
    @Test
    public void floorDataEvent() throws InterruptedException {
        EventHolder eventHolder = new EventHolder();
        Thread floor = new Thread(new Floor(eventHolder));
        floor.start();
        while (eventHolder.getData()[0] != null) {
        	assertTrue(eventHolder.getData()[0] instanceof Event);
        	//now we need to make the floor move on to the next data
        	eventHolder.putMsg("hello", "Floor");
        }
    }
    
    //test if the put method is ever used by anything not named Floor or Elevator
    @Test
    public void floorAndElevatorPut() throws InterruptedException {
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
        
        //run until floor has finished the test file
        while (eventHolder.getData()[0] != null) {
        	Object[] data = eventHolder.getData();
        	//when the put method is called from a thread that isn't named "Floor" or "Elevator" the floordata and elevator data
        	//will be different from putData, which is at [4] in the getData array
        	assertTrue(data[0] == data[4] || data[1] == data[4]);
        }
    }
    //test if the putmsg is ever sent to somewhere that isn't floor or elevator
    @Test
    public void putMsg() throws InterruptedException {
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
        
        //run until floor has finished the test file
        while (eventHolder.getData()[0] != null) {
        	Object[] data = eventHolder.getData();
        	//when the putmsg method is called to send to something that isn't "Floor" or "Elevator" the putMessage is different from the floormessage or the elevator message
        	//will be different from putData, which is at [4] in the getData array
        	assertTrue(data[2] == data[5] || data[3] == data[5]);
        }
    }
}