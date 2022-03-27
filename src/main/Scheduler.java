package main;


//TODO
// apperently this system is responsible for handling faults like an elevator's doors not opening,
// an elevator getting stuck between floors, and packet loss...
// I'm not sure how you would solve these issues without just designing them out ie: get reply messages for everything,
// and make sure an elevator doesn't move until it gets a reply.
// The elevator should just be designed to never move if the doors are open... why should the scheduler have to do this?
//TODO apperently roughly half of the people will come in at the GROUND Floor

/**
 * NOTE, you may want to use threads so we can receive communications from Floor and from Elevator.
 *
 *
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
import java.util.LinkedList;

public class Scheduler {
    private Communicator eCommunicator;
    private Communicator fCommunicator;
    private LinkedList<Message> queue =  new LinkedList<>();
    //todo maybe have this sorted, if it is sorted, then it needs to be a list or smth
    //time TillRequest[0] is the floor for which the times are relevant
    private String[] timeTillRequest;
    private int elevCount;
    //we use timtillrequests as a checker to see how many elevators sent availible, since timetillReq[0] == the floor number, we'll skip it.
    private int next = 1;
    private State currentState = State.INIT;

    public Scheduler(int i) {
        elevCount = i;
        Communicator com = new Communicator();
        eCommunicator = new Communicator(com.SCHEDULER_EPORT, "Scheduler");
        fCommunicator = new Communicator(com.SCHEDULER_FPORT, "Scheduler");
        timeTillRequest = new String[elevCount + 1];
    }

    public static void main(String[] args) {
        Clock time = Clock.systemDefaultZone();
        //IMPORTANT: ARGS[0] is the amount of elevators we have... if null, 1 elevator is the default
        Scheduler s;
        if (args.length != 0) {
            s = new Scheduler(Integer.parseInt(args[0]));
        }else {
            s = new Scheduler(1);
        }


        while(true) {
            //state machine by Cameron
            switch (s.currentState) {
                case INIT:
                    System.out.println("Scheduler " + s.currentState);
                    //just go to the next state
                    s.currentState = s.next();
                    //don't break, we've received a message so we may as well hear it out.
                case EVENTWAIT:
                    System.out.println("Scheduler " + s.currentState);
                    Message m = s.fCommunicator.receive();
                    System.out.println(m.getData()[0]);
                    //waiting for an event
                    if (m.getData()[0].equals("FloorRequest")) {
                        //send OK back to wherever we got it
                        s.fCommunicator.send(new Message(new String[]{"OK"}, time.millis(), m.getToFrom()));
                        //add this to the queue
                        //TODO  sort these by chronological order
                        s.queue.add(m);
                        //set timeTillRequest
                        System.out.println(m.getData()[2]);
                        s.timeTillRequest[0] = m.getData()[2];
                        //go to the next state
                        s.currentState = s.next();
                    }else {
                        //we received something else, probably an Elevator saying availible...
                        //want to send something back so the sender can resend later.
                        s.fCommunicator.send(new Message(new String[]{"BadRequest"}, time.millis(), m.getToFrom()));
                    }
                    break;
                case ELEVWAIT:
                    System.out.println("Scheduler " + s.currentState);
                    m = s.eCommunicator.receive();
                    System.out.println(m.getData()[0]);
                    //waiting for elevator
                    if (m.getData()[0].equals("Availible")) {
                        if (s.next == (s.timeTillRequest.length - 1)){
                            s.timeTillRequest[s.next] = "avail";
                            //Scheduler needs this at 1
                            s.next = 1;
                            //great, send ok
                            s.eCommunicator.send(new Message(new String[]{"OK"}, time.millis(), m.getToFrom()));
                            //go to the next state
                            s.currentState = s.next();
                        }else {
                            s.timeTillRequest[s.next++] = "avail";
                            s.eCommunicator.send(new Message(new String[]{"OK"}, time.millis(), m.getToFrom()));
                            break;
                        }
                    }else {
                        //we received something else, probably a floor request
                        //want to send something back so the sender can resend later.
                        s.eCommunicator.send(new Message(new String[]{"BadRequest"}, time.millis(), m.getToFrom()));
                        break;
                    }
                case SCHEDULING:
                    System.out.println("Scheduler " + s.currentState);
                    //TODO consider adding threads so that we are always adding floor events to the queue...
                    //have we already scheduled the given floor request
                    if (s.timeTillRequest[0].length() > 1) {
                        m = s.eCommunicator.receive();
                        if (m.getData()[0].equals("Arrived")) {
                            //FIXME
                            m = s.eCommunicator.rpc_send(new Message(new String[] {"timeFor", "5"}, time.millis(), m.getToFrom()));
                        }
                    }else {
                        //start processing a request, maybe seperate this into a function....
                        //now loop and ask everyone for their floor times.
                        while (s.next != s.timeTillRequest.length) {
                            //ask for the time for desired request sent to the elevators in order
                            m = s.eCommunicator.rpc_send(new Message(new String[] {"timeFor", s.timeTillRequest[0]}, time.millis(),  "Elevator" + s.next));
                            //message received
                            if (m.getData()[0].equals("0")) {
                                //Elevator sends time = 0 whenever they have the fastest possible time, immediately send them.
                                s.timeTillRequest[s.next] = m.getData()[0];
                                m = s.eCommunicator.rpc_send(new Message(new String[]{"goTo", s.timeTillRequest[0]}, time.millis(), "Elevator" + s.next));
                                //FIXME set a timer, change states, do something while we wait for this elevator to arrive
                                break;
                            }else {
                                //FIXME check if we received from the correct person...
                                s.timeTillRequest[s.next] = m.getData()[0];
                            }
                            s.next++;
                        }
                        //got all the Elevator times, nobody was min time. s.next must be > the last elevator id
                        int min = 1;
                        for (int i = 1; i < s.next; i++) {
                            if (s.timeTillRequest[i].compareTo(s.timeTillRequest[min]) < 0) {
                                //current time is less then the existing minimum time
                                min = i;
                            }
                        }
                        //send a goTo to the minimum elevator
                        m = s.eCommunicator.rpc_send(new Message(new String[]{"goTo", s.timeTillRequest[0]}, time.millis(), "Elevator" + min));
                        if (m.getData()[0].equals("OK")) {
                            //FIXME
                            try {
                                Thread.sleep(7000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //ping to check their time and shit...
                            m = s.eCommunicator.rpc_send(new Message(new String[]{"timeFor", s.timeTillRequest[0]}, time.millis(), m.getToFrom()));

                            //m = s.eCommunicator.rpc_send(new Message(new String[] {"timeFor", "5"}, time.millis(), "Elevator1"));
                        }else {
                            //FIXME loop till they send OK or pick the next best elevator
                        	while(!m.getData()[0].equals("OK")) {
                        		

                        		}
                        	}
                        }
                        //elevator sent, update timeTillRequest
                        s.timeTillRequest[0] += " Ordered";
                        //FIXME maybe add a state here...
                        
                        break;
                        }
                    break;
            }

            /*
            if (m.getData()[0].equals("Arrived")) {
                m = s.eCommunicator.rpc_send(new Message(new String[] {"timeFor", "5"}, time.millis(), m.getToFrom()));
            }
            if (m.getData()[0].equals("Availible")) {
                //send OK back to wherever we got it
                s.eCommunicator.send(new Message(new String[]{"OK"}, time.millis(), m.getToFrom()));
                m = s.eCommunicator.rpc_send(new Message(new String[] {"timeFor", "5"}, time.millis(),  m.getToFrom()));


                s.timeTillRequest[Character.getNumericValue(m.getToFrom().charAt(8))] = m.getData()[0];
                s.next++;
            }
            if (s.next == 2) {
                if (s.timeTillRequest[1].compareTo(s.timeTillRequest[2]) < 0) {
                    m = s.eCommunicator.rpc_send(new Message(new String[]{"goTo", "5"}, time.millis(), "Elevator" + 1));
                } else {
                    m = s.eCommunicator.rpc_send(new Message(new String[]{"goTo", "5"}, time.millis(), "Elevator" + 2));
                }
                if (m.getData()[0].equals("OK")) {
                    try {
                        Thread.sleep(7000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //ping to check their time and shit...
                    m = s.eCommunicator.rpc_send(new Message(new String[]{"timeFor", "5"}, time.millis(), m.getToFrom()));

                    //m = s.eCommunicator.rpc_send(new Message(new String[] {"timeFor", "5"}, time.millis(), "Elevator1"));
                }
            }
            if (m.getData()[0].equals("ButtonPress")) {
                //normally would process... but i don't care.
                s.eCommunicator.send(new Message(new String[] {"OK"}, time.millis(), m.getToFrom()));
                try {
                    Thread.sleep(1000);
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
                m = s.eCommunicator.rpc_send(new Message(new String[] {"goTo", "" + m.getData()[2].charAt(4)}, time.millis(), m.getToFrom()));
            }

             */
        }
    }
    enum State {
        INIT, EVENTWAIT, ELEVWAIT, SCHEDULING;
    }
    private State next() {
        State[] states = State.values();
        int current = 0;
        //just cycle till we hit current state, return it.
        while (states[current] != currentState) {
            current++;
        }
        return states[current + 1];
    }
    private State prev() {
        State[] states = State.values();
        int current = 0;
        //just cycle till we hit current state, return it.
        for (int i = 0; states[i] != currentState; i++) {
            current = i;
        }
        return states[current - 1];
    }
}




/**
 * *****************************************Juan Leal's Scheduler
 * //this should be looping I am not sure if that is what the while loop is for or not but if it isnt then i will place it in its own loop
 *             public void StateMachine(Message m) {
 *
 * 				if (currentState == State.IDLE && m.getData()[0].equals("Availible")) {
 * 					currentState = State.AVAILABLE;
 *
 *                                } else if (currentState == State.UNAVAILABLE && m.getData()[0].equals("Availible")) {
 *         			//gets the direction of the chosen elevator depending on the button input then decides which way it will go
 *
 * 					Elevator givenElevator = selectElevator(elevator1,elevator2);
 * 					currentDirection= givenElevator.getDirection();
 *
 *         			if (Event.getButtonEvent().getDirection() != currentDirection) {
 *
 *         				addtoPendingJobs(event);
 *
 *                    } else if (event.getButtonevent().getDirection() == currentDirection) {
 *
 * 						if (currentDirection == Direction.UP && givenElevator.getDestinationFloor() < currentFloor) {
 *         					//should be code here to send a message to the elevator to move just wasnt sure how to implement it
 *
 * 							s.schedulerCommunicator.send(new Message(new String[] {"OK"}, time.millis(), m.getToFrom()));
 * 							addtoPendingJobs(event);
 *
 *
 *                        } else if (currentDirection == Direction.DOWN && givenElevator.getDestinationFloor() > currentFloor) {
 *         					//should be code here to send a message to the elevator to move just wasnt sure how to implement it
 *
 *
 *         					addtoPendingJobs(event);
 *
 *
 *
 *                        } else {
 *         					//CurrentJobs is just a placeholder might just change it to a list or something that holds the list of current jobs for up and down
 * 							currentJobs.add(event);
 *                        }
 *
 *                    }
 *
 *                }
 *         		else if(currentState == State.UNAVAILABLE && m.getData()[0].equals("Unavailible")) {
 *         			//wait for message to be sent then set State to available
 *         			currentState = State.AVAILABLE;
 *
 *
 *                }
 **         	}
 *         }
 *
 *             //Stores the tasks at hand if elevators or scheduler are busy
 *         	public void addtoPendingJobs(Event event) {
 *
 *         		if (event.getEvent().getDirectionToGo() == Direction.UP) {
 *         			System.out.println("Add to pending up jobs");
 *         			upPending                ent);
 *         		}
 *
 *         		else {
 *         			System.out.println("Add to pending down jobs");
 *
 * 					downPending                ent);
 *            }
 *         	}
 *
 *
 *         	//Selects the best elevator depending on the floor where the request was made
 *         	public Elevator selectElevator(Elevator elevator1, Elevator elevator2) {
 *         		Elevator selectedElevator;
 *         		if (currentFloor - elevator2.getFloor() < currentFloor - elevator1.getFloor()) {
 *         		selectedElevator = elevator2;*
 *
 *         		}
 *         		else if (currentFloor - elevator1.getFloor() < currentFloor - elevator2.getFloor()) {
 *         		selectedEle                vator1;
 *
 *
 *         		}
 *         		            ectedElevator;
 *         	}
 *
 *
 *
 *         }
 *
 *
 *         enum State {
 *
 *         	UNAVAILABLE, AVAILABLE, IDLE
 *
 *         }
 *
 *         enum Direction {
 *
 *         	UP, DOWN
 *
 *         }
 */
