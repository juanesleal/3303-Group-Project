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
import java.util.Timer;
import java.util.TimerTask;

public class Scheduler implements Runnable {
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
    Clock time = Clock.systemDefaultZone();
    private boolean scheduling = false;
    private boolean inMethod = false;
    private Timer timer = new Timer();
    private TimerTask checkArrive;

    //also for some reason the scheduler sends elevator 1 to go to floor 4 instead of elevator 2 even though e2 is faster...
    //everything is a bit of a mess since elev 2 keeps taking requests it can't do...

    public Scheduler(int i) {
        elevCount = i;
        Communicator com = new Communicator();
        eCommunicator = new Communicator(com.SCHEDULER_EPORT, "Scheduler");
        fCommunicator = new Communicator(com.SCHEDULER_FPORT, "Scheduler");
        timeTillRequest = new String[elevCount + 1];
    }

    @Override
    public void run() {
        while(currentState != null) {
            //state machine by Cameron
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!scheduling) {
                System.out.println("Scheduler " + currentState);
                switch (currentState) {
                    case INIT:
                        System.out.println("Scheduler " + currentState);
                        //just go to the next state
                        currentState = next();
                        //don't break, we've received a message so we may as well hear it out.
                    case ELEVWAIT:
                        inMethod = true;
                        if (timeTillRequest[1] == null) {
                            //basically means we are a first time run.
                            Message m = eCommunicator.receive(0);
                            eCommunicator.send(ElevWait(m));
                        } else if (timeTillRequest[1].equals("avail")) {
                            //we have a timeout since we don't want to wait forever on the other elevator
                            Message m = eCommunicator.receive(3000);
                            if (!m.getData()[0].equals("TimeOut")) {
                                eCommunicator.send(ElevWait(m));
                            }
                        } else {
                            //not the first request, Elevators are smart enough to send appropriate responses to our requests, so all good.
                            currentState = next();
                            System.out.println("test: " + timeTillRequest[1]);
                        }
                        break;
                    case EVENTWAIT:
                        inMethod = true;
                        Message m = fCommunicator.rpc_send(new Message(new String[]{"EventReq"}, time.millis(), "Floor"));
                        EventWait(m);
                        break;
                    //TODO for testing you may want to have JUNIt open a UDP socket same thing with GUI,
                    //  when ERROR messages exist, same with SUCCESSES, when someone arrives, send to that socket.
                    //  make an integrations JUNIT, test that basically prints the input file and whether it worked.. with
                    case GETDATA:
                        inMethod = true;
                        //getData does all the work for us...
                        GetData();
                        break;
                    case SCHEDULING:
                        System.err.println("ERROR how did i get here");
                }
            }
        }

    }

    public static void main(String[] args) {
        //IMPORTANT: ARGS[0] is the amount of elevators we have... if null, 1 elevator is the default
        Scheduler s;
        if (args.length != 0) {
            s = new Scheduler(Integer.parseInt(args[0]));
        }else {
            s = new Scheduler(1);
        }
        s.run();
    }



    //this note causes an error, it's important... the next thing i need to do is get the elevators to not accept requests to go weird floors while they are travelling while full...
    enum State {
        INIT,  ELEVWAIT, EVENTWAIT, GETDATA, SCHEDULING;
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

    public synchronized void EventWait(Message m) {
        inMethod = false;
        notifyAll();
        while(scheduling) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inMethod = true;
        System.out.println("Event received for floor: " + m.getData()[0]);
        //waiting for an event
        if (m.getData()[0].equals("FloorRequest")) {
            //add this to the queue
            queue.add(m);
            //set timeTillRequest
            System.out.println("floor request on floor: " + m.getData()[2]);
            timeTillRequest[0] = m.getData()[2]  + m.getData()[5] + m.getData()[6] + m.getData()[1];
            //go to the next state
            currentState = next();
        }
        inMethod = false;
        notifyAll();
    }

    public synchronized Message ElevWait(Message m) {
        inMethod = false;
        notifyAll();
        while(scheduling) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inMethod = true;
        System.out.println(m.getData()[0]);
        System.out.println("ElevWait: " + next);
        //waiting for elevator
        if (m.getData()[0].equals("Availible")) {
            //this may be confusing, but basically check if we have set all the timeTillRequests, and that the current one isn't the same sender as the previous
            System.out.println("Received Availible");
            timeTillRequest[Character.getNumericValue(m.getToFrom().charAt(8))] = "avail";
            if (next == (timeTillRequest.length - 1)) {
                //Scheduler needs this at 1
                next = 1;
                //great, send ok
                //go to the next state
                currentState = next();
            }else {
                next++;
            }
            System.out.println("Sending OK");
            inMethod = false;
            notifyAll();
            return new Message(new String[]{"OK"}, time.millis(), m.getToFrom());
        }else {
            //we received something else, probably a floor request
            //want to send something back so the sender can resend later.
            notifyAll();
            inMethod = false;
            return new Message(new String[]{"BadRequest"}, time.millis(), m.getToFrom());
        }
    }
    public synchronized void GetData() {
        inMethod = false;
        notifyAll();
        while(scheduling) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inMethod = true;
        for (Message m : queue) {
            if ((!m.getData()[2].equals("Arrived")) && queue.size() > 1) {
                //this means this request has not yet reached the request floor
                System.out.println("handling previous or next: " + m.getData()[2]);
                //this is basically resetting the request so that we now handle the previous one.
                timeTillRequest[0] = m.getData()[2]  + m.getData()[5] + m.getData()[6] + m.getData()[1];
            }
        }
        Message m;
        //start processing a request, maybe seperate this into a function....
        //now loop and ask everyone for their floor times.
        while (next != timeTillRequest.length) {

            //ask for the time for desired request sent to the elevators in order
            m = eCommunicator.rpc_send(new Message(new String[] {"timeFor", timeTillRequest[0].charAt(0) + ""}, time.millis(),  "Elevator" + next));
            System.out.println("Received a TimeFor response: " + m.getData()[0]);
            //message received
            if (m.getData()[0].equals("0.0")) {
                //Elevator sends time = 0 whenever they have the fastest possible time, immediately send them.
                timeTillRequest[next] = m.getData()[0];
                m = eCommunicator.rpc_send(new Message(new String[]{"goTo", timeTillRequest[0].substring(0, 3), timeTillRequest[0].substring(3)}, time.millis(), "Elevator" + next));
                if (m.getData()[0].equals("OK")) {
                    System.out.println("got OKr");
                    //awesome, set timer, do stuff..
                    //elevator sent, update timeTillRequest
                    timeTillRequest[0] += " Ordered";
                    int elevNo = next;
                    checkArrive = new TimerTask() {
                        @Override
                        public void run() {
                            if (currentState != null) {
                                System.out.println("==========Elevator scheduled for first arrival=============");
                                scheduling = true;
                                //reset the timeTillRequest, not sure what that is rn...
                                Message m = Scheduling(elevNo);
                                if (m.getData()[0].equals("SHUTDOWN")) {
                                    System.out.println("System Shutdown");
                                    currentState = null;
                                    return;
                                }
                                //they'll send a reply, we should wait
                                eCommunicator.rpc_send(m);
                                scheduling = false;
                                inMethod = false;
                                taskDone();
                            }
                        }
                    };
                    if (m.getData().length > 1) {
                        if (m.getData()[1].equals("Already Here")) {
                            //schedule a timer with avery short delay, since the elevator is likely already there...
                            timer.schedule(checkArrive, 100);
                        }else {
                            //weird
                            System.out.println("Elevator at 0 seconds sent something weird============ " + m.getData()[1]);
                            //it may be going to the next floor, wait a default 5 seconds
                            timer.schedule(checkArrive, 5000);
                        }
                    }else {
                        //it may be going to the next floor, wait a default 5 seconds
                        timer.schedule(checkArrive, 5000);
                    }

                    //check for a new Event...
                    currentState = State.EVENTWAIT;
                    //reset next
                    next = 1;
                    inMethod = false;
                    notifyAll();
                    return;
                }
            }else if (!m.getData()[0].equals("NotAvailible")){
                boolean isDouble = false;
                while (!isDouble) {
                    try {
                        Double.parseDouble(m.getData()[0]);
                        isDouble = true;
                    } catch (NumberFormatException e) {
                        //this means we received something completely unexpected
                        m = eCommunicator.receive(100);
                    }
                }
                while (Character.getNumericValue(m.getToFrom().charAt(8)) != next) {
                    m = eCommunicator.rpc_send(new Message(new String[] {"timeFor", timeTillRequest[0].charAt(0) + ""}, time.millis(),  "Elevator" + next));
                    System.out.println("Received a TimeFor response: " + m.getData()[0]);
                }
                timeTillRequest[next] = m.getData()[0];

            }else {
                //make timetill huge so it doesn't get picked
                timeTillRequest[next] = "2000";
            }
            next++;
        }


        //got all the Elevator times, nobody was min time. s.next must be > the last elevator id
        int min = 1;
        int countChecked = 0;
        for (int i = 1; i < next; i++) {
            if (Double.parseDouble(timeTillRequest[i]) < Double.parseDouble(timeTillRequest[min])) {
                //current time is less then the existing minimum time
                min = i;
            }
            if (timeTillRequest[i].equals("2000")) {
                countChecked ++;
            }
        }
        System.out.println("Elevators that aren't availible: " + countChecked);
        if (countChecked == timeTillRequest.length - 1) {
            //every Elevator has been asked
            //check for a new Event (previous will stay in the queue)
            currentState = State.EVENTWAIT;
            //reset next
            next = 1;
            inMethod = false;
            notifyAll();
            return;
        }

        //send a goTo to the minimum elevator
        m = eCommunicator.rpc_send(new Message(new String[]{"goTo", timeTillRequest[0].substring(0, 3), timeTillRequest[0].substring(3)}, time.millis(), "Elevator" + min));
        System.out.println("Received a GoTo response: " + m.getData()[0]);
        while(!m.getData()[0].equals("OK")) {
            //note this is destructive, we wish to look for a new Minimum
            timeTillRequest[min] = "2000";
            int oldMin = min;
            //got all the Elevator times, nobody was min time. s.next must be > the last elevator id
            min = 1;
            for (int i = 1; i < next; i++) {
                if (timeTillRequest[i].compareTo(timeTillRequest[min]) < 0) {
                    //current time is less then the existing minimum time
                    min = i;
                }

            }
            if (oldMin == min && m.getData()[0].equals("NO")) {
                //nobody is availible to GOTO:
                //check for a new Event (previous will stay in the queue)
                currentState = State.EVENTWAIT;
                //reset next
                next = 1;
                inMethod = false;
                notifyAll();
                return;
            }
            //min recalculated...
            //send a goTo to the minimum elevator
            m = eCommunicator.rpc_send(new Message(new String[]{"goTo", timeTillRequest[0].substring(0, 3), timeTillRequest[0].substring(3)}, time.millis(), "Elevator" + min));
            System.out.println("Received a GoTo response: " + m.getData()[0]);
        }
        //elevator sent, update timeTillRequest
        timeTillRequest[0] += " Ordered";
        if (m.getData()[1] != null) {
            //do not set a new timer if the elevator is already set to go there.
            if (m.getData()[1].equals("Already Going")) {
                //don't set a timer, they have one
            }else {
                System.out.println("ERROR==============================GETDATA");
            }
        }else {
            System.out.println("Scheduling timer for " + ((1000) * Math.floor(Double.parseDouble(timeTillRequest[min]))) + " milliseconds===============================");
            int finalMin = min;
            checkArrive = new TimerTask() {
                @Override
                public void run() {
                    if (currentState != null) {
                        System.out.println("==========Elevator scheduled for first arrival non 0=============");
                        scheduling = true;
                        //reset the timeTillRequest, not sure what that is rn...
                        Message m = Scheduling(finalMin);
                        if (m.getData()[0].equals("SHUTDOWN")) {
                            System.out.println("System Shutdown");
                            currentState = null;
                            return;
                        }
                        //they'll send a reply, we should wait
                        eCommunicator.rpc_send(m);
                        scheduling = false;
                        inMethod = false;
                        taskDone();
                    }
                }
            };
            //schedule a timer for the amount of time it will take them to get there.
            int time = (int) ((1000) * Math.floor(Double.parseDouble(timeTillRequest[min])));
            System.out.println("seting timer to go off in time: " + time);
            timer.schedule(checkArrive, time);
        }
        //check for a new Event...
        currentState = State.EVENTWAIT;
        //reset next
        next = 1;
        inMethod = false;
        notifyAll();
    }
    public synchronized Message Scheduling(int elevator) {
        //check so we aren't disrupting another method...
        while (inMethod) {
            try {
                System.out.println("Scheduling waiting");
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        inMethod = true;
        System.out.println("Scheduling Elevator: " + elevator + "  " + currentState);
        //wait only 1 second longer
        Message m = eCommunicator.receive(2000);
        System.out.println("Received a Maybe Arrived: " + m.getData()[0]);

        while (!m.getData()[0].equals("Arrived") || !m.getToFrom().equals("Elevator" + elevator)) {
            if (m.getData()[0] .equals("TimeOut")) {
                //shutdown
                System.out.println("Scheduling Timed out");
                //send a new elevator
                currentState = State.GETDATA;
                return new Message(new String[] {"SHUTDOWN"}, time.millis(), ("Elevator" + elevator));
            }
            //setting a shorter timeout...
            m = eCommunicator.receive(100);
        }

        Message msg = null;
        for (Message mes : queue) {
            //check which event the message is referring to
            if (m.getData()[2].equals(mes.getData()[1])) {
                //this means msg is the event that we should check against.
                msg = mes;
                System.out.println("Scheduling, Message: " + msg.getToFrom() + " " + msg.getData()[1]);
                break;
            }
        }

        if (msg != null && m.getToFrom().equals("Elevator" + elevator)) {
            System.out.println("Scheduling arrived: " + (int) Double.parseDouble(m.getData()[1]));
            System.out.println("Scheduling arrived: " + msg.getData()[2]);
            //if the elevator already went to the first location, we want to check that they arrived at the second...
            //since we use this checker more then once, i'll store it...
            boolean secArrive = msg.getData()[2].equals("Arrived") && ((int) Double.parseDouble(m.getData()[1])) == Integer.parseInt(msg.getData()[4]);
            if (secArrive || ((int) Double.parseDouble(m.getData()[1]) == Integer.parseInt(msg.getData()[2]))) {
                //they've arrived alright...
                eCommunicator.send(new Message(new String[] {"OK"}, time.millis(), m.getToFrom()));
                //check for buttonPress and handle...
                //only wait 5 seconds
                m = eCommunicator.receive(5000);
                while (!m.getData()[0].equals("ButtonPress") || !m.getToFrom().equals("Elevator" + elevator)) {
                    if (m.getData()[0] .equals("TimeOut")) {
                        //gracefully handling, tell them to open the door lol
                        m = eCommunicator.rpc_send(new Message(new String[] {"OpenDoor"}, time.millis(),"Elevator" + elevator));
                        while (!m.getData()[0].equals("OK") || !m.getToFrom().equals("Elevator" + elevator)) {
                            if (m.getData()[0] .equals("TimeOut")) {
                                //give up on them...
                                System.out.println("Elevator TimeOut");
                                //send a new elevator
                                currentState = State.GETDATA;
                                return new Message(new String[] {"SHUTDOWN"}, time.millis(), ("Elevator" + elevator));
                            }
                            m = eCommunicator.rpc_send(new Message(new String[] {"OpenDoor"}, time.millis(),"Elevator" + elevator));
                        }
                    }
                    //setting a shorter timeout...
                    m = eCommunicator.receive(100);
                }

                String request = m.getData()[2];
                //check that their doors have closed...
                eCommunicator.send(new Message(new String[]{"OK"}, time.millis(), m.getToFrom()));
                m = eCommunicator.rpc_send(new Message(new String[]{"DoorStatus"}, time.millis(), m.getToFrom()));

                while (!m.getData()[0].equals("Status") || (m.getData().length > 1 && !m.getData()[1].equals("Closed")) || !m.getToFrom().equals("Elevator" + elevator)) {
                    if (m.getData()[0] .equals("TimeOut")) {
                        //give up on them...
                        System.out.println("Elevator TimeOut");
                        //send a new elevator
                        currentState = State.GETDATA;
                        return new Message(new String[] {"SHUTDOWN"}, time.millis(), ("Elevator" + elevator));
                    }
                    if (m.getData().length > 1 && !m.getData()[1].equals("Open")) {
                        m = eCommunicator.rpc_send(new Message(new String[] {"CloseDoor"}, time.millis(),"Elevator" + elevator));
                        while (!m.getData()[0].equals("OK") || !m.getToFrom().equals("Elevator" + elevator))  {
                            if (m.getData()[0].equals("TimeOut")) {
                                //give up on them...
                                System.out.println("Elevator TimeOut");
                                //send a new elevator
                                currentState = State.GETDATA;
                                return new Message(new String[] {"SHUTDOWN"}, time.millis(), ("Elevator" + elevator));
                            }
                            m = eCommunicator.rpc_send(new Message(new String[] {"CloseDoor"}, time.millis(),"Elevator" + elevator));
                        }
                    }
                    //setting a shorter timeout...
                    m = eCommunicator.receive(100);
                }

                if (secArrive) {
                    //doors are closed, good...
                    //this request has been fulfilled, remove from the queue
                    queue.remove(msg);
                    //really don't need to send anything to the elevator
                    return new Message(new String[]{"nothing"}, time.millis(), m.getToFrom());
                }else {
                    //doors are closed, good...
                    //tell the elevator to go to the given floor...
                    //we won't remove the item from the queue, but we will change the data for the floor it arrived at.
                    msg.getData()[2] = "Arrived";
                    checkArrive = new TimerTask() {
                        @Override
                        public void run() {
                            if (currentState != null) {
                                System.out.println("===========Elevator scheduled for second arrival==========");
                                scheduling = true;
                                //reset the timeTillRequest, not sure what that is rn...
                                Message m = Scheduling(elevator);
                                if (m.getData()[0].equals("SHUTDOWN")) {
                                    System.out.println("System Shutdown");
                                    currentState = null;
                                    return;
                                }
                                //they'll send a reply, we should wait
                                eCommunicator.rpc_send(m);
                                scheduling = false;
                                inMethod = false;
                                taskDone();
                            }
                        }
                    };
                    //set a timer to execute when this is scheduled to arrive at the next location...
                    boolean tryAgain = true;
                    while (tryAgain) {
                        m = eCommunicator.rpc_send(new Message(new String[]{"timeFor", msg.getData()[4]}, time.millis(), m.getToFrom()));
                        try {
                            System.out.println("Scheduling timer for " + ((1000) * Math.floor(Double.parseDouble(m.getData()[0]))) + " milliseconds===============================");
                            timer.schedule(checkArrive, (int) ((1000) * Math.floor(Double.parseDouble(m.getData()[0]))));
                            tryAgain = false;
                        } catch (NumberFormatException e) {
                            System.out.println("didn't get a good response for timeFor");
                        }
                    }

                    //notNecessary since the elevator already goes to the button press floor on arrival.
                    return new Message(new String[]{"goTo", request, "notNecessary"}, time.millis(), m.getToFrom());
                }
            }
        }
        //send a new elevator
        currentState = State.GETDATA;
        return new Message(new String[]{"SHUTDOWN"}, time.millis(), m.getToFrom());
    }

    private synchronized void  taskDone() {
        //helper func for the timerTasks so the output isn't super gross
        notifyAll();
    }
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
