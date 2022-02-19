package main;

import java.lang.Math;
import java.util.ArrayList;

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
        ArrayList<Integer> addDest = new ArrayList<Integer>();
        addDest.add(floor);
        super.elevatorRef.setQueue(addDest);
        super.elevatorRef.send("OK");
    }
}

    is target floor "on the way"
