package main;


//TODO
// apperently this system is responsible for handling faults like an elevator's doors not opening,
// an elevator getting stuck between floors, and packet loss...
// I'm not sure how you would solve these issues without just designing them out ie: get reply messages for everything,
// and make sure an elevator doesn't move until it gets a reply.
// The elevator should just be designed to never move if the doors are open... why should the scheduler have to do this?
//TODO apperently roughly half of the people will come in at the GROUND Floor

import java.time.Clock;

public class Scheduler {
    private Communicator schedulerCommunicator;

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
            
            public void StateMachine(Message m) {
        		State currentState;
				Object currentDirection;
				int currentFloor;
				Event event;
				
				if (currentState == State.IDLE) {
					currentState = State.AVAILABLE;
        			currentDirection = Elevator.getDirection();
        			
        		} else if (currentState == State.UNAVAILABLE) {

        			if (Event.getButtonEvent().getDirectionToGo() != currentDirection) {
        				
        				addtoPendingJobs(event);
        				
        			} else if (event.getButtonevent().getDirectionToGo() == currentDirection) {

						if (currentDirection == Direction.UP && event.getInternalevent().getDestinationFloor() < currentFloor) {
        					addtoPendingJobs(event);
        				} else if (currentDirection == Direction.DOWN && event.getInternalevent().getDestinationFloor() > currentFloor) {
        					addtoPendingJobs(event);
        				} else {
        					//CurrentJobs is just a placeholder might just change it to a list or something that holds the list of current jobs for up and down
							currentJobs.add(event);
        				}

        			}

        		}

        	}
            
            //will change this to get all the events and store them according to a message so they can be sent as soon as the scheduler is free
        	public void addtoPendingJobs(Event event) {
        		if (event.getEvent().getDirectionToGo() == Direction.UP) {
        			System.out.println("Add to pending up jobs");
        			upPendingJobs.add(event);
        		} else {
        			System.out.println("Add to pending down jobs");
        			downPendingJobs.add(event);
        		}
        	}
        	//Selects the best elevator depending on the floor where the request was made
        	public void selectElevator(Elevator elevator1, Elevator elevator2) {
        		Elevator selectedElevator;
        		if (currentFloor - elevator2.getFloor() < currentFloor - elevator1.getFloor()) {
        		selectedElevator = elevator2;
        		
        		
        		}
        		
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