package main;

import java.lang.Math;
import java.util.ArrayList;
import java.util.LinkedList;




abstract class ElevatorState {

    Elevator elevatorRef;

    public ElevatorState(Elevator elev) {
        elevatorRef = elev;
    }

    abstract void entry();
    abstract void timeFor(int floor);
    abstract void goTo(int floor);
    abstract void arrive();
}


class InitState extends ElevatorState {
    public InitState(Elevator elev) {
        super(elev);
    }
    public void entry() {
        super.elevatorRef.send("Available");
    }

    void timeFor(int floor) {

    }

    void goTo(int floor) {

    }

    void arrive() {

    }
}


class IdleState extends ElevatorState {
    public IdleState(Elevator elev) {
        super(elev);
    }

    public void entry() {

    }

    void timeFor(int floor) {
        //this func is the most complex... normally we would "get" our velocity, but where idle
        //convert double to msg, i just use concatination here...
        String msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, 0);
        super.elevatorRef.send(msg);
    }

    void goTo(int floor) {
        //don't actually move, just add things to queue, send messages...
        LinkedList<Integer> addDest = new LinkedList<Integer>();
        //need to add at front
        addDest.addFirst(floor);
        super.elevatorRef.setQueue(addDest);
        super.elevatorRef.setFloorOk(true);
        super.elevatorRef.send("OK");
    }

    void arrive() {

    }
}

class EmptyTState extends ElevatorState {
    public EmptyTState(Elevator elev) {
        super(elev);
    }
    public void entry() {

    }

    void timeFor(int floor) {
        //we can't give an answer for that,
        //we need to get the passenger destination first.
    }

    void goTo(int floor) {
        //this should also never happen...
    }

    void arrive() {
        //remove shit from queue
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always arrive at the top of the q
        q.removeFirst();
        super.elevatorRef.setQueue(q);
        super.elevatorRef.send("arrived");
    }
}

class WaitPassEntryState extends ElevatorState {
    public WaitPassEntryState(Elevator elev) {
        super(elev);
    }

    public void entry() {
        // this is BROKEN--------------------------------------------------------------------------------------------- super.elevatorRef.acceptPass();
    }

    void timeFor(int floor) {

    }

    void goTo(int floor) {
        //in this state, goTo is when the passenger sets a destination.
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always add passengers destination to the bottom of the queue
        q.addLast(floor);
        super.elevatorRef.setQueue(q);
    }

    void arrive() {

    }
}

class FullTState extends ElevatorState {
    public FullTState(Elevator elev) {
        super(elev);
    }

    public void entry() {

    }

    void timeFor(int floor) {
        //check if floor is "on the way", if not, we can't give an accurate time.
        double velocity = super.elevatorRef.getVelocity();
        int destination = super.elevatorRef.getQueue().getFirst();
        String msg = "";
        if (velocity != 0) {
            if (destination > floor && velocity > 0) {
                //floor is on the way, we can now send a time
                msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, velocity);
            }else if (destination < floor && velocity < 0) {
                //floor is on the way, we can now send a time
                msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, velocity);
            } else {
                //floor isn't on the way, out time is fairly unknown, so we'll make that clear.
                msg = "Inaccurate"  + (super.elevatorRef.geteM().arriveWhen(destination, velocity) + 10.0);
                super.elevatorRef.setFloorOk(false);
            }
        }else {
            //hyper edge case
            System.out.println("We should be moving... what's going on??");
        }
        if (msg.substring(0, 10) == "Inaccurate" || msg == "2000") {
            //errorCode
            super.elevatorRef.setFloorOk(false);
        }else {
            super.elevatorRef.setFloorOk(true);
        }
        super.elevatorRef.send(msg);
    }

    void goTo(int floor) {
        if (super.elevatorRef.isFloorOk()) {
            //we know we can get there
            //now we just need to go there
            //don't actually move, just add things to queue, send messages...
            LinkedList<Integer> addDest = super.elevatorRef.getQueue();
            //need to add at front
            addDest.addFirst(floor);
            super.elevatorRef.setQueue(addDest);
            super.elevatorRef.send("OK");
        }else {
            //bad floor, send NO and stay in this state.
            super.elevatorRef.send("NO");
        }
    }

    void arrive() {
        //remove shit from queue
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always arrive at the top of the q
        q.removeFirst();
        super.elevatorRef.setQueue(q);
        super.elevatorRef.send("arrived");
    }
}

class WaitPassExitState extends ElevatorState {
    public WaitPassExitState(Elevator elev) {
        super(elev);
    }

    public void entry() {
        //whether we have something else in the q or not, we need to signal availible, then we can handle whether or not we get a bad floor.
        super.elevatorRef.send("Available");
        //also gotta let people out of the elevator too lol.
        //BROKEN ------------------------------------------------------------------------------------------------------------------------super.elevatorRef.acceptPass();
    }

    void timeFor(int floor) {

    }

    void goTo(int floor) {

    }

    void arrive() {

    }
}