package main;

import java.lang.Math;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Calculations for floor timing...
 *
 *
 * avg bottom to top time: 20s
 * avg floor-floor time: 5s
 * max speed: 2 meters per second (faster end of a fairly sized building)
 * assuming we won't reach the max speed when traveling 1 floor, since the height of an avg floor is 4.3m:
 * acceleration = 2.15/(2.5^2) = 0.34 m/s^2 (calculated with half the distance to floor and half the time target)
 * this means we will reach our max speed in ~5.88s
 *
 */


abstract class ElevatorState {
    final double height = 4.3;
    final int maxVelocity = 2;
    final double acceleration = 0.34;
    final double accelToMax = (maxVelocity/acceleration);
    //how far we travel when accelerating to the max speed
    final double distanceForMax = ((accelToMax * accelToMax) * acceleration);

    Elevator elevatorRef;

    public ElevatorState(Elevator elev) {
        elevatorRef = elev;
    }
    public double arriveWhen(int floor, double velocity) {
        double time = 0;
        double distance = Math.abs(floor - elevatorRef.getFloor() * height);
        //if the distance is far enough, we can reach our max speed
        if (velocity == 0) {
            if (distance > (distanceForMax * 2)) {
                //we need to accelerate to the max speed, then decellerate from it
                time += (accelToMax) * 2;
                time += (distance - (distanceForMax * 2)) / maxVelocity;
            } else {
                time += Math.sqrt(distance/acceleration);
            }
        }else if (velocity == maxVelocity) {
            //are we trying to go somewhere above floor?
            //can we slow down in time?
            //since we are at max velocity, we know exactly our breaking distance (distanceForMax)
            if (distance < distanceForMax) {
                //not enough space to break, set time to an error value (2000)
                time = 2000;
            }else {
                //we can break...
                time += accelToMax;
            }

        }else if (velocity == -maxVelocity) {
            //can we slow down in time?
            //since we are at max velocity, we know exactly our breaking distance (distanceForMax)
            if (distance < distanceForMax) {
                //not enough space to break, set time to an error value (2000)
                time = 2000;
            }else {
                //we can break...
                time += accelToMax;
            }

        }else if (velocity > 0) {
            //going up, still accellerating
            double timeToDecel = Math.abs(velocity/acceleration);
            if (distance < (acceleration * Math.pow(timeToDecel, 2))) {
                //can't break
                time = 2000;
            }else {
                time += timeToDecel;
            }
        }else if (velocity < 0) {
            //going down, still accellerating
            double timeToDecel = Math.abs(velocity/acceleration);
            if (distance < (acceleration * Math.pow(timeToDecel, 2))) {
                //can't break
                time = 2000;
            }else {
                time += timeToDecel;
            }
        }else {
            System.out.println("Critical error, unexpected velocity" + velocity);
        }

        return time;
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
        super.elevatorRef.send("Availible");
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
        String msg = ""  + arriveWhen(floor, 0);
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
        LinkedList<Integer> q = elevatorRef.getQueue();
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
        super.elevatorRef.acceptPass();
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
                msg = ""  + arriveWhen(floor, velocity);
            }else if (destination < floor && velocity < 0) {
                //floor is on the way, we can now send a time
                msg = ""  + arriveWhen(floor, velocity);
            } else {
                //floor isn't on the way, out time is fairly unknown, so we'll make that clear.
                msg = "Inaccurate"  + (arriveWhen(destination, velocity) + 10.0);
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
this isn't working, this below isn't right at all----------------------------------------------------------------
    void goTo(int floor) {
        //in this state, goTo is when the passenger sets a destination.
        LinkedList<Integer> q = elevatorRef.getQueue();
        //always add passengers destination to the bottom of the queue
        q.addLast(floor);
        super.elevatorRef.setQueue(q);
    }

    void arrive() {

    }
}