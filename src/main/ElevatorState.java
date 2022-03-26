package main;

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
        System.out.println("Init entry");
        String[] s = super.elevatorRef.send(new String[]{"Availible"}, "Scheduler");
        //check that we got a reply
        while (!s[0].equals("OK")) {
            //sending availible till we get a proper response
            s = super.elevatorRef.send(new String[]{"Availible"}, "Scheduler");
        }
        //great, go to idle
        super.elevatorRef.next("Idle");
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
        System.out.println("Idle entry");
    }

    void timeFor(int floor) {
        //this func is the most complex... normally we would "get" our velocity, but where idle
        //convert double to msg, i just use concatination here...
        String msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, 0);
        super.elevatorRef.reply(new String[]{msg}, "Scheduler");
    }

    void goTo(int floor) {
        //add to queue, send OK, change states, and start moving
        LinkedList<Integer> addDest = new LinkedList<Integer>();
        //need to add at front
        addDest.addFirst(floor);
        super.elevatorRef.setQueue(addDest);
        super.elevatorRef.setFloorOk(true);
        super.elevatorRef.reply(new String[]{"OK"}, "Scheduler");
        super.elevatorRef.next("Empty");
    }

    void arrive() {

    }
}

class EmptyTState extends ElevatorState {
    public EmptyTState(Elevator elev) {
        super(elev);
    }
    public void entry() {
        //move to dest at top of Q
        super.elevatorRef.geteM().move(super.elevatorRef.getQueue().getFirst());
    }

    void timeFor(int floor) {
        if (super.elevatorRef.getQueue().getFirst() == floor) {
            //they are asking us when we will arrive at the current destination
            String msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity());
            super.elevatorRef.reply(new String[]{msg}, "Scheduler");
        }else {
            super.elevatorRef.reply(new String[]{"NotAvailible"}, "Scheduler");
        }

    }

    void goTo(int floor) {
        if (super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity()) == 2000) {
            //2000 is an error it means we can't get to the given floor
            super.elevatorRef.reply(new String[]{"NO"}, "Scheduler");
        }else {
            //this probably means we can stop at this destination on the way to our current...
            LinkedList<Integer> q = super.elevatorRef.getQueue();
            q.addFirst(floor);
            super.elevatorRef.setQueue(q);
            super.elevatorRef.setFloorOk(true);
            super.elevatorRef.geteM().move(floor);
            super.elevatorRef.reply(new String[]{"OK"}, "Scheduler");
        }
    }

    void arrive() {
        //remove shit from queue
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always arrive at the top of the q
        q.removeFirst();
        super.elevatorRef.setQueue(q);
        super.elevatorRef.reply(new String[]{"Arrived"}, "Scheduler");
        //Change states.
        super.elevatorRef.next("WaitEntry");
    }
}

class WaitPassEntryState extends ElevatorState {
    public WaitPassEntryState(Elevator elev) {
        super(elev);
    }

    public void entry() {
        System.out.println("Wait Entry Entry");
        //we've arrived, open doors
        super.elevatorRef.setDoorsOpen(true);
        //wait for pass to enter
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        //close doors
        super.elevatorRef.setDoorsOpen(false);
        String[] s = super.elevatorRef.send(new String[]{"ButtonReq", "" + super.elevatorRef.geteM().getFloor()}, "Floor");
        //check that we got a reply
        while (!s[0].equals("OK")) {
            //sending availible till we get a proper response
            s = super.elevatorRef.send(new String[]{"ButtonReq", "" + super.elevatorRef.geteM().getFloor()}, "Floor");
        }
        //we've got a botton request, pass to Scheduler
        System.out.println("button press: " + s[1]);
        s = super.elevatorRef.send(new String[]{"ButtonPress", "" + super.elevatorRef.geteM().getFloor(), "" + s[1]}, "Scheduler");

        //check OK

        while (!s[0].equals("OK")) {
            //sending availible till we get a proper response
            s = super.elevatorRef.send(new String[]{"ButtonPress", "" + super.elevatorRef.geteM().getFloor(),"" + s[1].charAt(4)}, "Scheduler");
        }
        //awesome, we've received the passenger...
        //add their destination to the top of the Q??

        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always add passengers destination to the bottom of the queue idk if this works
        q.addFirst(Character.getNumericValue(s[1].charAt(4)));
        super.elevatorRef.setQueue(q);
        super.elevatorRef.setFloorOk(true);
        super.elevatorRef.next("Full");
    }

    void timeFor(int floor) {
        super.elevatorRef.reply(new String[]{"NotAvailible"}, "Scheduler");
    }

    void goTo(int floor) {
        super.elevatorRef.reply(new String[]{"NO"}, "Scheduler");
        /*
        //in this state, goTo is ignored...?
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always add passengers destination to the bottom of the queue
        q.addLast(floor);
        super.elevatorRef.setQueue(q);
        super.elevatorRef.setFloorOk(true);
        super.elevatorRef.reply(new String[]{"OK"}, "Scheduler");
        super.elevatorRef.next("Full");
        */
    }

    void arrive() {

    }
}

class FullTState extends ElevatorState {
    public FullTState(Elevator elev) {
        super(elev);
    }

    public void entry() {
        //move to dest at top of Q
        super.elevatorRef.geteM().move(super.elevatorRef.getQueue().getFirst());
    }

    void timeFor(int floor) {
        //check if floor is "on the way", if not, we can't give an accurate time.
        /*
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
        super.elevatorRef.send(new String[]{msg}, "Scheduler");

         */
       //just tell them when i'll arrive, shouldn't complicate it any more...
       String msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity());
       super.elevatorRef.reply(new String[]{msg}, "Scheduler");
    }

    void goTo(int floor) {
        if (super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity()) == 2000) {
            //2000 is an error it means we can't get to the given floor
            super.elevatorRef.reply(new String[]{"NO"}, "Scheduler");
        }else {
            //this probably means we can stop at this destination on the way to our current...
            LinkedList<Integer> q = super.elevatorRef.getQueue();
            q.addFirst(floor);
            super.elevatorRef.setQueue(q);
            super.elevatorRef.setFloorOk(true);
            super.elevatorRef.geteM().move(floor);
            super.elevatorRef.reply(new String[]{"OK"}, "Scheduler");
        }
        /*
        if (super.elevatorRef.isFloorOk()) {
            //we know we can get there
            //now we just need to go there
            //don't actually move, just add things to queue, send messages...
            LinkedList<Integer> addDest = super.elevatorRef.getQueue();
            //need to add at front
            addDest.addFirst(floor);
            super.elevatorRef.setQueue(addDest);
            super.elevatorRef.send(new String[]{"OK"}, "Scheduler");
        }else {
            //bad floor, send NO and stay in this state.
            super.elevatorRef.send(new String[]{"NO"}, "Scheduler");
        }

         */
    }

    void arrive() {
        //remove shit from queue
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always arrive at the top of the q
        q.removeFirst();
        super.elevatorRef.setQueue(q);
        super.elevatorRef.reply(new String[]{"Arrived"}, "Scheduler");
        //wait for pass to exit
        super.elevatorRef.next("WaitExit");
    }
}

class WaitPassExitState extends ElevatorState {
    public WaitPassExitState(Elevator elev) {
        super(elev);
    }

    public void entry() {
        super.elevatorRef.setDoorsOpen(true);
        //wait for pass to enter
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        //close doors
        super.elevatorRef.setDoorsOpen(false);
        //all good, set Idle;
        super.elevatorRef.next("Idle");
    }

    void timeFor(int floor) {

    }

    void goTo(int floor) {

    }

    void arrive() {

    }
}