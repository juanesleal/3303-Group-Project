package main;


import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.time.Clock;
import java.util.LinkedList;

//TODO apperently roughly half of the people will come in at the GROUND Floor
// need to add lamps, arrival sensors, and other things...???


public class Elevator {
    private ElevatorCommunicator communicator;
    private ElevatorMovement eM = new ElevatorMovement();
    private ElevatorState[] states = {new InitState(this), new IdleState(this), new EmptyTState(this), new WaitPassEntryState(this), new FullTState(this), new WaitPassExitState(this)};
    private int[][] transition = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 2, 5}, {5, 1, 4}};
    private int currentState = 0;
    private LinkedList<Integer> queue = new LinkedList<Integer>();
    private boolean floorOk = false;



    public Elevator() {
        communicator = new ElevatorCommunicator(0, "Elevator1");
    }

    public static void main(String[] args) {
        Clock time = Clock.systemDefaultZone();
        Elevator e = new Elevator();
        //testing
        Message m = e.communicator.rpc_send(new Message(new String[] {"Availible"}, time.millis(), "Scheduler"));
        System.out.println(m.getData()[0]);


        //TODO do some states... idk
        /*
        //first things first, INIT
        states[currentState].entry();
        next(1);
        while (true) {
            //wait for message
            long x = 1000;
            Message event = send;
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
        */
    }
    private void next(int state) {
        currentState = transition[currentState][state];
    }

    public void send(String msg) {
        //TODO fixme
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