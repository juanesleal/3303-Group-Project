package main;


//TODO
// apperently this system is responsible for handling faults like an elevator's doors not opening,
// an elevator getting stuck between floors, and packet loss...
// I'm not sure how you would solve these issues without just designing them out ie: get reply messages for everything,
// and make sure an elevator doesn't move until it gets a reply.
// The elevator should just be designed to never move if the doors are open... why should the scheduler have to do this?
//TODO apperently roughly half of the people will come in at the GROUND Floor

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

public class Scheduler {
    private Communicator schedulerCommunicator;
	int currentFloor;
	State currentState;
	Object currentDirection;
	Event event;
    List<Event> currentJobs = new ArrayList<Event>();   
    List<Event> upPendingJobs = new ArrayList<Event>();   
    List<Event> downPendingJobs = new ArrayList<Event>();   
	
    public Scheduler() {
        Communicator com = new Communicator();
        schedulerCommunicator = new Communicator(com.SCHEDULER_EPORT, "Scheduler");
    }

    public static void main(String[] args) {
        Clock time = Clock.systemDefaultZone();
        Scheduler s = new Scheduler();
        while(true) {
            Message m = s.schedulerCommunicator.receive();
            System.out.println(m.getData()[0]);
            if (m.getData()[0].equals("Availible")) {
                //send OK back to wherever we got it
                s.schedulerCommunicator.send(new Message(new String[] {"OK"}, time.millis(), m.getToFrom()));
            }
        
            //this should be looping I am not sure if that is what the while loop is for or not but if it isnt then i will place it in its own loop
            public void StateMachine(Message m) {

				if (currentState == State.IDLE && m.getData()[0].equals("Availible")) {
					currentState = State.AVAILABLE;
					
        		} else if (currentState == State.UNAVAILABLE && m.getData()[0].equals("Availible")) {
        			//gets the direction of the chosen elevator depending on the button input then decides which way it will go
        			
					Elevator givenElevator = selectElevator(elevator1,elevator2);
					currentDirection= givenElevator.getDirection();
					
        			if (Event.getButtonEvent().getDirection() != currentDirection) {
        				
        				addtoPendingJobs(event);
        				
        			} else if (event.getButtonevent().getDirection() == currentDirection) {

						if (currentDirection == Direction.UP && givenElevator.getDestinationFloor() < currentFloor) {
        					//should be code here to send a message to the elevator to move just wasnt sure how to implement it
							
							s.schedulerCommunicator.send(new Message(new String[] {"OK"}, time.millis(), m.getToFrom()));
							addtoPendingJobs(event);
							
							
        				} else if (currentDirection == Direction.DOWN && givenElevator.getDestinationFloor() > currentFloor) {
        					//should be code here to send a message to the elevator to move just wasnt sure how to implement it
        					
        					
        					addtoPendingJobs(event);
        					
        					
        					
        				} else {
        					//CurrentJobs is just a placeholder might just change it to a list or something that holds the list of current jobs for up and down
							currentJobs.add(event);
        				}

        			}

        		}
        		else if(currentState == State.UNAVAILABLE && m.getData()[0].equals("Unavailible")) {
        			//wait for message to be sent then set State to available
        			currentState = State.AVAILABLE;
        			
        			
        		}

        	}
        }
            
            //Stores the tasks at hand if elevators or scheduler are busy
        	public void addtoPendingJobs(Event event) {
        		
        		if (event.getEvent().getDirectionToGo() == Direction.UP) {
        			System.out.println("Add to pending up jobs");
        			upPendingJobs.add(event);
        		} 
        		
        		else {
        			System.out.println("Add to pending down jobs");

					downPendingJobs.add(event);
        		}
        	}
        	
        	
        	//Selects the best elevator depending on the floor where the request was made
        	public Elevator selectElevator(Elevator elevator1, Elevator elevator2) {
        		Elevator selectedElevator;
        		if (currentFloor - elevator2.getFloor() < currentFloor - elevator1.getFloor()) {
        		selectedElevator = elevator2;
        		
        		
        		}
        		else if (currentFloor - elevator1.getFloor() < currentFloor - elevator2.getFloor()) {
        		selectedElevator = elevator1;
        		
        		
        		}
        		return selectedElevator;
        	}
        	
        	
        	
        }
        
        
        enum State {

        	UNAVAILABLE, AVAILABLE, IDLE

        }

        enum Direction {

        	UP, DOWN

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