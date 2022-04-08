package main;

import java.util.LinkedList;




abstract class ElevatorState {

    Elevator elevatorRef;

    public ElevatorState(Elevator elev) {
        elevatorRef = elev;
    }

    abstract void entry();
    abstract void timeFor(int floor);
    abstract boolean goTo(int floor);
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

    boolean goTo(int floor) {
        return false;
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

    boolean goTo(int floor) {
        //Travel fault is a float from 0-1 that represents what proportion of the goTo request should be filled
        if (super.elevatorRef.getTravelFault() > 0) {
            //we travel only to the floor that's somewhere between here and the desired destination.
            floor = (int) Math.floor(floor * super.elevatorRef.getTravelFault());
        }
        //add to queue, send OK, change states, and start moving
        LinkedList<Integer> addDest = new LinkedList<>();
        //need to add at front
        addDest.addFirst(floor);
        super.elevatorRef.setQueue(addDest);
        super.elevatorRef.setFloorOk(true);
        if (floor == super.elevatorRef.geteM().getFloor()) {
            super.elevatorRef.reply(new String[]{"OK", "Already Here"}, "Scheduler");
        }else {
            super.elevatorRef.reply(new String[]{"OK"}, "Scheduler");
        }
        super.elevatorRef.next("Empty");
        return true;
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

    boolean goTo(int floor) {
        if (super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity()) == 2000) {
            //2000 is an error it means we can't get to the given floor
            super.elevatorRef.reply(new String[]{"NO"}, "Scheduler");
            return false;
        }else {
            //Travel fault is a float from 0-1 that represents what proportion of the goTo request should be filled
            if (super.elevatorRef.getTravelFault() > 0) {
                //we travel only to the floor that's somewhere between here and the desired destination.
                floor = (int) Math.floor(floor * super.elevatorRef.getTravelFault());
            }
            //this probably means we can stop at this destination on the way to our current...
            LinkedList<Integer> q = super.elevatorRef.getQueue();
            q.addFirst(floor);
            super.elevatorRef.setQueue(q);
            super.elevatorRef.setFloorOk(true);
            super.elevatorRef.geteM().move(floor);
            super.elevatorRef.reply(new String[]{"OK"}, "Scheduler");
            return true;
        }
    }

    void arrive() {
        //remove shit from queue
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always arrive at the top of the q
        q.removeFirst();
        super.elevatorRef.setQueue(q);
        String[] s = super.elevatorRef.send(new String[]{"Arrived", "" + super.elevatorRef.geteM().getFloor(), super.elevatorRef.getRequestTime()}, "Scheduler");
        while (!s[0].equals("OK")) {
            s = super.elevatorRef.send(new String[]{"Arrived", "" + super.elevatorRef.geteM().getFloor(), super.elevatorRef.getRequestTime()}, "Scheduler");
        }
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
        if (super.elevatorRef.getDoorFault() == 0) {
            //we've arrived, open doors
            super.elevatorRef.setDoorsOpen(true);
            //wait for pass to enter
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //close doors
            super.elevatorRef.setDoorsOpen(false);
        }else if (super.elevatorRef.getDoorFault() == 1) {
            //we've arrived, open doors
            super.elevatorRef.setDoorsOpen(true);
            //door stuck open
        }else if (super.elevatorRef.getDoorFault() == 2) {
            //close doors
            super.elevatorRef.setDoorsOpen(false);
            //doors stuck shut, tell Scheduler we have no button press
            String[] s = super.elevatorRef.send(new String[]{"NoButtonPress", super.elevatorRef.getRequestTime()}, "Scheduler");
            //check OK
            if (s[0].equals("SHUTDOWN")) {
                super.elevatorRef.setShutdown(true);
                return;
            }
            while (!s[0].equals("OpenDoors")) {
                //sending availible till we get a proper response
                s = super.elevatorRef.send(new String[]{"NoButtonPress", super.elevatorRef.getRequestTime()}, "Scheduler");
            }
            super.elevatorRef.setDoorsOpen(true);
        }
        String[] s = super.elevatorRef.send(new String[]{"ButtonReq", super.elevatorRef.getRequestTime()}, "Floor");
        //check that we got a reply
        while (!s[0].equals("OK")) {
            //sending availible till we get a proper response
            s = super.elevatorRef.send(new String[]{"ButtonReq", super.elevatorRef.getRequestTime()}, "Floor");
        }
        int button = Integer.parseInt(s[1]);
        //we've got a botton request, pass to Scheduler
        System.out.println("button press: " + s[1]);
        s = super.elevatorRef.send(new String[]{"ButtonPress", super.elevatorRef.getRequestTime(), "" + button}, "Scheduler");

        //check OK

        while (!s[0].equals("OK")) {
            //sending availible till we get a proper response
            s = super.elevatorRef.send(new String[]{"ButtonPress", super.elevatorRef.getRequestTime(),"" + button}, "Scheduler");
        }


        //awesome, we've received the passenger...
        //add their destination to the top of the Q??
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        //always add passengers destination to the bottom of the queue idk if this works
        q.addFirst(button);
        super.elevatorRef.setQueue(q);
        super.elevatorRef.setFloorOk(true);
        super.elevatorRef.next("Full");
    }

    void timeFor(int floor) {
        super.elevatorRef.reply(new String[]{"NotAvailible"}, "Scheduler");
    }

    boolean goTo(int floor) {
        super.elevatorRef.reply(new String[]{"NO"}, "Scheduler");
        return false;
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
        //just tell them when i'll arrive, shouldn't complicate it any more...
        String msg = ""  + super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity());
        if (super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity()) == 2000) {
            //2000 is an error it means we can't get to the given floor
            super.elevatorRef.reply(new String[]{"NotAvailible"}, "Scheduler");
        }else {
            super.elevatorRef.reply(new String[]{msg}, "Scheduler");
        }
    }

    boolean goTo(int floor) {
        LinkedList<Integer> q = super.elevatorRef.getQueue();
        if (q.getFirst().equals(floor)) {
            //we are already going where we were told to go...
            super.elevatorRef.setFloorOk(true);
            super.elevatorRef.reply(new String[]{"OK", "Already Going"}, "Scheduler");
            return true;
        }
        if (super.elevatorRef.geteM().arriveWhen(floor, super.elevatorRef.geteM().getVelocity()) == 2000) {
            //2000 is an error it means we can't get to the given floor
            super.elevatorRef.reply(new String[]{"NO"}, "Scheduler");
            return false;
        }else {
            //Travel fault is a float from 0-1 that represents what proportion of the goTo request should be filled
            if (super.elevatorRef.getTravelFault() > 0) {
                //we travel only to the floor that's somewhere between here and the desired destination.
                floor = (int) Math.floor(floor * super.elevatorRef.getTravelFault());
            }
            //this probably means we can stop at this destination on the way to our current...
            q.addFirst(floor);
            super.elevatorRef.setQueue(q);
            super.elevatorRef.setFloorOk(true);
            super.elevatorRef.geteM().move(floor);
            super.elevatorRef.reply(new String[]{"OK", "On the way"}, "Scheduler");
            return true;
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
        System.out.println("sending Arrived");
        String[] s = super.elevatorRef.send(new String[]{"Arrived", "" + super.elevatorRef.geteM().getFloor(), super.elevatorRef.getRequestTime()}, "Scheduler");
        //after attempts, we give up...
        int attempts = 0;
        while (!(s[0].equals("OK")) && attempts < 3) {
            attempts++;
            System.out.println("arrive received: " + s[0]);
            s = super.elevatorRef.send(new String[]{"Arrived", "" + super.elevatorRef.geteM().getFloor(), super.elevatorRef.getRequestTime()}, "Scheduler");
        }
        if (attempts == 3) {
            super.elevatorRef.setShutdown(true);
        }
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
        //send availible again...
        String[] s = super.elevatorRef.send(new String[]{"Availible"}, "Scheduler");
        //check that we got a reply
        while (!s[0].equals("OK")) {
            //sending availible till we get a proper response
            s = super.elevatorRef.send(new String[]{"Availible"}, "Scheduler");
        }
        //great, go to idle
        super.elevatorRef.next("Idle");
        //all good, set Idle;
        super.elevatorRef.next("Idle");
    }

    void timeFor(int floor) {

    }

    boolean goTo(int floor) {
        return false;
    }

    void arrive() {

    }
}