package main;


import java.time.Clock;
import java.util.LinkedList;
/*
public class ElevatorInit {
    //Initialize Elevators
    public static void main(String[] args) {
        Elevator e1 = new Elevator(1);
        Elevator e2 = new Elevator(2);
        e1.main(new String[]{});
        e2.main();
    }
}
*/

public class Elevator implements Runnable{
    private Communicator communicator;
    private ElevatorMovement eM = new ElevatorMovement(this);
    private ElevatorState[] states = {new InitState(this), new IdleState(this), new EmptyTState(this), new WaitPassEntryState(this), new FullTState(this), new WaitPassExitState(this)};
    private int[][] transition = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 2, 5}, {5, 1, 4}};
    private int currentState = 0;
    private LinkedList<Integer> queue = new LinkedList<Integer>();
    private boolean floorOk = false;
    private boolean doorsOpen = false;
    private float travelFault;
    private int doorFault;
    private String requestTime;
    private boolean shutdown = false;
    private boolean arrived = false;
    public int elevNum;

    Clock time = Clock.systemDefaultZone();



    public Elevator(int i) {
        elevNum = i;
        eM.prevFloor = i;
        communicator = new Communicator(0, "Elevator" + i);
    }

    @Override
    public void run() {

        //testing

        while (!shutdown) {
            entry();
            if (!arrived) {
                //receive a message
                Message m = communicator.receive(0);
                System.out.println("state: " + currentState);
                System.out.println(m.getData()[0]);
                if (m.getData()[0].equals("timeFor")) {
                    timeFor(Integer.parseInt(m.getData()[1]));
                } else if (m.getData()[0].equals("goTo") && !(m.getData()[2].equals("notNecessary"))) {
                    travelFault = Float.parseFloat(m.getData()[1].charAt(1) + "");
                    travelFault = Integer.parseInt(m.getData()[1].charAt(2) + "");
                    //check whether the goTo is being ignored
                    if (goTo(Integer.parseInt(m.getData()[1].charAt(0) + ""))) {
                        //set the request time since it uniquely identifies the request, we need this on arrival
                        requestTime = m.getData()[2];
                    }
                } else if (m.getData()[0].equals("DoorStatus")) {
                    if (doorsOpen) {
                        reply(new String[]{"Status", "Open"}, "Scheduler");
                    } else {
                        reply(new String[]{"Status", "Closed"}, "Scheduler");
                    }
                } else if (m.getData()[0].equals("SHUTDOWN")) {
                    shutdown = true;
                    System.out.println("Elevator SHUTDOWN");
                }
            } else {
                //we already did all the work when the arrive func was called.
                arrived = false;
            }
            System.out.println("state: "+ currentState);
        }
    }


    public static void main(String[] args) {
        int elevNum = 1;
        if (args.length != 0) {
            elevNum = Integer.parseInt(args[0]);
        }
        Elevator e = new Elevator(elevNum);
        e.run();
    }

    public void next(String n) {

        switch(n) {
            case "Init":
                currentState = 0;
                break;
            case "Idle":
                currentState = 1;
                break;
            case "Empty":
                currentState = 2;
                break;
            case "WaitEntry":
                currentState = 3;
                break;
            case "Full":
                currentState = 4;
                break;
            case "WaitExit":
                currentState = 5;
        }
        System.out.println("next: " + n + "  " + currentState);
    }

    public String[] send(String[] msg, String to) {
        System.out.println("Sending from Elevator");
        Message m = communicator.rpc_send(new Message(msg, time.millis(), to));
        System.out.println("Receiving from "  + m.getToFrom());
        return m.getData();
    }

    public String[] send(String[] msg, String to, int timeout) {
        System.out.println("Sending from Elevator");
        Message m = communicator.rpc_send(new Message(msg, time.millis(), to), timeout);
        System.out.println("Receiving from "  + m.getToFrom());
        return m.getData();
    }

    public void reply(String[] msg, String to) {
        //we are replying, so we don't need an rpc_send
        communicator.send(new Message(msg, time.millis(), to));
    }

    public float getTravelFault() {
        return travelFault;
    }

    public int getDoorFault() {
        return doorFault;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public void entry() {
        states[currentState].entry();
    }

    public void timeFor(int floor) {
        states[currentState].timeFor(floor);
    }

    public boolean goTo(int floor) {

        return states[currentState].goTo(floor);
    }

    public void arrive() {
        arrived = true;
        states[currentState].arrive();
    }

    public LinkedList<Integer> getQueue() {
        return queue;
    }

    public String getRequestTime() {
        return requestTime;
    }

    public void setQueue(LinkedList<Integer> q) {
        queue = q;
    }

    public void setFloorOk(boolean floorOk) {
        this.floorOk = floorOk;
    }

    public void setDoorsOpen(boolean doorsOpen) {
        this.doorsOpen = doorsOpen;
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