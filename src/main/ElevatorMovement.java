package main;

import java.time.Clock;
import java.util.Timer;
import java.util.TimerTask;

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

    public ElevatorMovement (Elevator ref) {
        elevatorRef = ref;
    }

    Elevator elevatorRef;
    public int destination;
    private Clock time = Clock.systemDefaultZone();
    private long accelTime;
    private long decelTime;
    private double velForDest = 0;
    public int prevFloor = 1;
    private Timer timer = new Timer();
    private TimerTask checkArrive;



    public void move(int dest) {
        if (dest == prevFloor) {
            System.out.println("already here");
            arrive();
            return;
        }
        //check if we are already going there
        if (destination != dest) {
            //get our velocity
            double velocity = getVelocity();
            double timeTo = arriveWhen(dest, velocity);

            long current = time.millis();

            if (velocity != 0) {
                //FIXME
                //slow down
                if (timeTo != 2000) {
                    //find out when we need to decelerate.
                    decelTime = current + ((int) (timeTo - accelToMax) * 1000);
                }
            } else {
                //find out how long we need to accelerate and if/when to start decelerating...
                if (timeTo != 2000) {
                    //we are legit.
                    if (timeTo < (accelToMax * 2)) {
                        //this means we won't get to max velocity.
                        accelTime = current;
                        decelTime = current + ((int) ((timeTo / 2.0) * 1000));
                        //what's the max velocity for this trip?
                        velForDest = (decelTime - accelTime) * (acceleration * 0.001);
                    } else {
                        //this means we will get to max velocity.
                        accelTime = current;
                        decelTime = current + ((int) ((timeTo - accelToMax) * 1000));
                        System.out.println(timeTo - accelToMax);
                        velForDest = maxVelocity;
                    }
                }
            }
            destination = dest;
            checkArrive = new TimerTask() {
                @Override
                public void run() {
                    System.out.println("Timer going: ");
                    if (getFloor() == destination) {
                        //we have arrived
                        arrive();
                    }else {
                        //sleep a little, then check again?
                        try {
                            Thread.sleep(2000);
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        }
                        run();
                    }
                }
            };
            timer.schedule(checkArrive, (int) Math.floor(1000 * timeTo));
            System.out.println("move complete: cur: " + current + "  a: " + (current - accelTime) + " d: " + (decelTime) + " vel: " + velForDest + " dest: " + destination  + "timeTO: "+ timeTo);
        }
    }

    public double getVelocity() {
        double velocity = velForDest;
        double curTime = time.millis();
        if (velForDest != 0) {
            if (curTime > decelTime) {
                //we should slow down
                velocity -= ((curTime - decelTime) * 0.001) * acceleration;
            } else if (time.millis() < decelTime) {
                //we should either accelerate, or we're at max
                if ((accelTime + (accelToMax * 1000)) > time.millis()) {
                    //we should accelerate
                    velocity = ((curTime - accelTime) * 0.001) * acceleration;
                }
            }
        }
        System.out.println("velocity: " + velocity);
        System.out.println("velocity: curTime: " + curTime);
        return velocity;
    }

    public double getFloor() {
        double vel = getVelocity();
        double distance;
        double current = time.millis();

        if (vel != 0) {
            if (vel < velForDest && (time.millis() < decelTime)) {
                //we are accelerating
                distance = timeDistance(0, acceleration, ((time.millis() - accelTime) * 0.001), 2000);
            } else if (vel < velForDest && (time.millis() > decelTime)) {
                //we are decelerating
                if (velForDest < maxVelocity) {
                    //this means we only accel and decel, nothing else
                    distance = timeDistance(0, acceleration, ((decelTime - accelTime) * 0.001), 2000);
                } else {
                    //we have hit max vel for some time...
                    //accel distance
                    distance = timeDistance(0, acceleration, accelToMax, 2000);
                    distance += timeDistance(velForDest, 0, (((decelTime - accelTime) * 0.001) - accelToMax), 2000);
                }
                distance += timeDistance(velForDest, -acceleration, ((time.millis() - decelTime) * 0.001), 2000);

                if (current > decelTime + (velForDest/acceleration) * 1000) {
                    //we've arrived, we are at the destination.
                    velForDest = 0;
                    return destination;
                }
            } else {
                //we are at max velocity.
                //edge case, it is exactly decelTime
                distance = distanceForMax;
                distance += timeDistance(velForDest, 0, (((decelTime - accelTime) * 0.001) - accelToMax), 2000);
            }
        }else {
            distance = 0;
        }

        //offset represents how many floor distances away from the previous floor
        double offset = distance / height;
        System.out.println("computing floor, offset: " + offset + " prev:" + prevFloor + "Dest:" + destination);
        if (destination > prevFloor) {
            //we are going up.
            elevatorRef.reply(new String[] {"Elevator " + elevatorRef.elevNum + " is currently " + distance + "m above floor " + prevFloor}, "Output");
            return prevFloor + offset;
        }else {
            //going down.
            elevatorRef.reply(new String[] {"Elevator " + elevatorRef.elevNum + " is currently " + distance + "m below floor " + prevFloor}, "Output");
            return prevFloor - offset;
        }
    }

    public void arrive() {
        //if we are already on the floor that we are going to, destination is never set.
        if (destination != 0) {
            prevFloor = destination;
        }

        elevatorRef.arrive();
    }

    public double arriveWhen (int floor, double velocity) {
        //how far from the destination?
        double distance = Math.abs((floor - getFloor()) * height);
        System.out.println("dist: " + distance);
        //whether we are going up or down is irrelevant...
        velocity = Math.abs(velocity);
        double ti = 2000;
        if (velocity == 0) {
            if (distance > (distanceForMax * 2)) {
                //this means we have enough distance to fully accelerate and decellerate
                ti = (accelToMax) * 2;
                //this is how long we are travelling at max velocity
                ti += (distance - (distanceForMax * 2)) / maxVelocity;
                System.out.println("arriveWhen: accelerating to max, currently stationary");
            }else {
                //we won't be able to go to max velocity
                //accelerate for half the distance, decellerate for the other half.
                ti = 2 * timeDistance(velocity, acceleration, 2000, distance/2);
                System.out.println("arriveWhen: not accelerating to max, currently stationary");
            }
        }else if (velocity == maxVelocity || velocity == -maxVelocity) {
            //we are travelling somewhere at maxVelocity
            //can we slow down in time?
            //since we are at max velocity, we know exactly our breaking distance (distanceForMax)
            if (distance < distanceForMax) {
                //not enough space to break, set time to an error value (2000)
                ti = 2000;
                System.out.println("arriveWhen: not enough breaking distance, currently maxVel");
            }else {
                //we have space to break, but we might not want to break immediately: this calculates how long to travel distance - distanceForMax at maxVelocity
                ti = timeDistance(maxVelocity, 0, 2000, (distance - distanceForMax));
                ti += accelToMax;
                System.out.println("arriveWhen: enough breaking distance, currently maxVel");
            }
        }else {
            //we are either accelerating or decellerating
            if (time.millis() > decelTime) {
                //we are slowing down rn...
                ti = timeDistance(velocity, -acceleration, 2000, distance);
                System.out.println("arriveWhen: currently decel");
            }else {
                //accelerating...
                if (floor == destination) {
                    //do we have breaking distance??
                    if (velocity > Math.abs(velForDest)) {
                        System.out.println("===============================Weird behaviour in ElevatorMovement.arriveWhen");
                    }
                    double breakingDist = timeDistance(velForDest, -acceleration, ((decelTime - accelTime) * 0.001), 2000);
                    if (distance > breakingDist) {
                        //how long till we start to decellerate?
                        ti = ((decelTime - time.millis()) * 0.001);
                        //how long it takes to decellerate
                        ti += timeDistance(velForDest, -acceleration, 2000, breakingDist);
                        System.out.println("arriveWhen: enough breaking distance, decel soon or already...");
                    }
                }else {
                    if (floor < destination && floor >= getFloor()) {
                        //did some fairly complex math for this...
                        //basically, the amount of braking distance needed is the distance it took to get to the existing velocity
                        //then you need to add the distance it would take to get to the maximum velocity for this destination
                        //the maximum velocity is the velocity at which you have the breakingdistance amount to slow down...
                        //anyways:
                        double timeForVel1 = (velocity / acceleration);
                        //this is the weird part of the soln
                        double distForVel2 = (0.5 * (distance - timeDistance(0, acceleration, timeForVel1, 2000)));
                        //now time for vel2
                        double timeForVel2 = timeDistance(velocity, acceleration, 2000, distForVel2);
                        //now, finally
                        ti = (2 * timeForVel2) + timeForVel1;
                    }
                }
            }
        }
        System.out.println("arrival at: " + floor + " in " + ti + " seconds");
        return ti;
    }

    private double timeDistance (double velocity, double acceleration, double time, double distance) {
        //function to solve distance or time with all other givens
        if (time == 2000) {
            //they want us to solve for time.
            //this is a quadratic equation...
            double determ = Math.sqrt(Math.pow(velocity, 2) - ((4 * (acceleration) / 2) * (0 - distance)));
            double soln = (0 - velocity) + determ;
            if (soln < 0 && acceleration > 0) {
                //wrong soln, get the other
                soln = (0 - velocity) - determ;
            }
            System.out.println("time traveling at: " + velocity  + " accelerating at: " + acceleration + " for " + distance + "m is: " + (soln/Math.abs(acceleration)));
            return (soln / (acceleration));
        }else if (distance == 2000) {
            //the real world equation for this: ^x = vi*t + 1/2(a)(t^2)
            double dist = velocity * time;
            dist += 0.5 * acceleration * (Math.pow(time, 2));
            System.out.println("Distance traveled at: " + time + "s at: " + velocity + " accelerating at: " + acceleration + " is: " + dist);
            return dist;
        }
        return 2000;
    }
}


