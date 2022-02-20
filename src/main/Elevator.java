package main;


import java.util.ArrayList;
import java.util.LinkedList;

public class Elevator implements Runnable
{
    private EventHolder eventHolder;
    private ElevatorMovement eM = new ElevatorMovement();
    private ElevatorState[] states = {new InitState(this), new IdleState(this), new EmptyTState(this), new WaitPassEntryState(this), new FullTState(this), new WaitPassExitState(this)};
    private int[][] transition = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 2, 5}, {5, 1, 4}};
    private int currentState = 0;
    private LinkedList<Integer> queue = new LinkedList<Integer>();
    private boolean floorOk = false;



    public Elevator(EventHolder eH) {
        eventHolder = eH;
    }

    public void run() {
        //first things first, INIT
        states[currentState].entry();
        next(1);
        while (true) {
            //wait for message
            Message event = eventHolder.getMsgE();
            if (event.getFunc() == "timeFor") {
                states[currentState].timeFor(event.getFloor());
                next(0);
            }else if (event.getFunc() == "goTo") {
                states[currentState].goTo(event.getFloor());
                if (floorOk) {
                    //move the elevator
                    eM.move(event.getFloor());
                    next(1);
                }else{
                    next(0);
                }
            }
            //we've received a message from Scheduler, we need to parse it to find out where we should go.
            eventHolder.put("Elevator Data");
        }
    }
    private void next(int state) {
        currentState = transition[currentState][state];
    }

    public void send(String msg) {
        eventHolder.put(msg);
    }

    public LinkedList<Integer> getQueue() {
        return queue;
    }

    public void setQueue(LinkedList<Integer> q) {
        queue = q;
    }

    public void setFloorOk(boolean floorOk) {
        this.floorOk = floorOk;
    }

    public boolean isFloorOk() {
        return floorOk;
    }

    public double getFloor() {
        return eM.getFloor();
    }

    public double getVelocity() {
        return eM.getFloor();
    }
    public ElevatorMovement geteM() {
        return eM;
    }
}