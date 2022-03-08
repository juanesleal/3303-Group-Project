package main;

import java.time.Clock;

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
public class ElevatorMovement {
    final double height = 4.3;
    final int maxVelocity = 2;
    final double acceleration = 0.34;
    final double accelToMax = (maxVelocity/acceleration);
    //how far we travel when accelerating to the max speed
    //TODO distance for max is fundementally busted, use the distance func.
    final double distanceForMax = ((accelToMax * accelToMax) * +acceleration);



    Elevator elevatorRef;
    private int destination;
    private Clock time = Clock.systemDefaultZone();
    private long accelTime;
    private long decelTime;
    private double velForDest = 0;
    private int prevFloor = 1;



    public void move(int dest) {
        //get our velocity
        double velocity = getVelocity();
        double timeTo = arriveWhen(dest, velocity);

        if (velocity != 0) {
            //slow down
            if (timeTo != 2000) {
                //find out when we need to decelerate.
                decelTime = time.millis() + ((int) (timeTo - accelToMax) * 1000);
            }
        } else {
            //find out how long we need to accelerate and if/when to start decelerating...
            if (timeTo != 2000) {
                //we are legit.
                if (timeTo < (accelToMax * 2)) {
                    //this means we won't get to max velocity.
                    accelTime = time.millis();
                    decelTime = time.millis() + ((int) (timeTo/2) * 1000);
                    //what's the max velocity for this trip?
                    velForDest = ((decelTime - accelTime) /1000) * acceleration;
                }else {
                    //this means we will get to max velocity.
                    accelTime = time.millis();
                    decelTime = time.millis() + ((int) (timeTo - accelToMax) * 1000);
                    velForDest = maxVelocity;
                }
            }
            accelTime = time.millis();
        }
    }

    public double getVelocity() {
        double velocity = velForDest;
        double curTime = time.millis();
        if (velForDest != 0) {
            if (curTime > decelTime) {
                //we should slow down
                velocity -= ((curTime - decelTime) / 1000) * acceleration;
            } else if (time.millis() < decelTime) {
                //we should either accelerate, or we're at max
                if ((accelTime + (accelToMax * 1000)) > time.millis()) {
                    //we should accelerate
                    velocity = ((curTime - accelTime) / 1000) * acceleration;
                }
            }
        }
        System.out.println("velocity: " + velocity);
        System.out.println("velocity: curTime: " + curTime);
        return velocity;
    }

    public double getFloor() {

        double distance;
        double vel = getVelocity();
        if (vel < velForDest && (time.millis() < decelTime)) {
            //we are accelerating
            distance = distance(0, acceleration, ((time.millis() - accelTime) / 1000));
        }else if (vel < velForDest && (time.millis() > decelTime)){
            //we are decelerating
            if (velForDest < maxVelocity) {
                //this means we only accel and decel, nothing else
                distance = distance(0, acceleration, ((decelTime - accelTime) / 1000));
            }else {
                //we have hit max vel for some time...
                //accel distance
                distance = distance(0, acceleration, accelToMax);
                distance += distance(velForDest, 0, (((decelTime - accelTime)/1000) - accelToMax));
            }

            distance += distance(velForDest, -acceleration, ((time.millis() - decelTime) / 1000));
        }else {
            //we are at max velocity.
            //edge case, it is exactly decelTime
            distance = distance(0, acceleration, accelToMax);
            distance += distance(velForDest, 0, (((decelTime - accelTime)/1000) - accelToMax));
        }

        //offset represents how many floor distances away from the previous floor
        double offset = distance / height;

        if (destination > prevFloor) {
            //we are going up.
            return prevFloor + offset;
        }else {
            //going down.
            return prevFloor - offset;
        }
    }

    public void arrive() {
        prevFloor = destination;
    }

    public double arriveWhen(int floor, double velocity) {
        double time = 0;
        double distance = Math.abs(floor - getFloor() * height);
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
        System.out.println("arrival at: " + destination + " in " + time + " seconds");
        return time;
    }
    private double distance (double velocity, double acceleration, double time) {
        //the real world equation for this: ^x = vi*t + 1/2(a)(t^2)
        double dist = velocity * time;
        dist += 0.5 * acceleration * (Math.pow(time, 2));

        System.out.println("Distance traveled at: " + time + "s at: " + velocity + " accelerating at: " + acceleration + " is: " + dist);
        return dist;
    }
}


