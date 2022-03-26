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

public class Elevator {
    private Communicator communicator;
    private ElevatorMovement eM = new ElevatorMovement(this);
    private ElevatorState[] states = {new InitState(this), new IdleState(this), new EmptyTState(this), new WaitPassEntryState(this), new FullTState(this), new WaitPassExitState(this)};
    private int[][] transition = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 2, 5}, {5, 1, 4}};
    private int currentState = 0;
    private LinkedList<Integer> queue = new LinkedList<Integer>();
    private boolean floorOk = false;
    private boolean doorsOpen = false;
    private String requestTime;

    Clock time = Clock.systemDefaultZone();



    public Elevator(int i) {
        communicator = new Communicator(0, "Elevator" + i);
    }


    public static void main(String[] args) {
        System.out.println(args[0]);
        Elevator e = new Elevator(Integer.parseInt(args[0]));
        if (Integer.parseInt(args[0]) == 2) {
            //start on 2nd floor
            e.eM.prevFloor++;
        }
        //testing

        while (true) {
            //TODO check if arrived

            e.entry();
            //receive a message
            Message m = e.communicator.receive();
            System.out.println("state: "+ e.currentState);
            System.out.println(m.getData()[0]);
            if (m.getData()[0].equals("timeFor")) {
                e.timeFor(Integer.parseInt(m.getData()[1]));
            }else if (m.getData()[0].equals("goTo")) {
                //check whether the goTo is being ignored
                if (e.goTo(Integer.parseInt(m.getData()[1]))) {
                    //set the request time since it uniquely identifies the request, we need this on arrival
                    e.requestTime = m.getData()[2];
                }
            }
            System.out.println("state: "+ e.currentState);
        }
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
        Message m = communicator.rpc_send(new Message(msg, time.millis(), to));
        return m.getData();
    }

    public void reply(String[] msg, String to) {
        //we are replying, so we don't need an rpc_send
        communicator.send(new Message(msg, time.millis(), to));
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