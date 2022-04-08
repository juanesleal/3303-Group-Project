package main;

public class Initializer {
    public static void main(String[] args) {
        //IMPORTANT: ARGS[0] is the amount of elevators we have... if null, 1 elevator is the default
        Thread scheduler, floor;

        int elevCount = 1;
        if (args.length != 0) {
            elevCount = Integer.parseInt(args[0]);
        }

        floor = new Thread(new Floor(), "Floor");
        floor.start();

        scheduler = new Thread(new Scheduler(elevCount), "Scheduler");
        scheduler.start();



        Thread[] elevs = new Thread[elevCount];

        for (int i = 1; i <= elevCount; i++) {
            elevs[i - 1] = new Thread(new Elevator(i), ("Elevator" + (i)));
            elevs[i - 1].start();
        }
    }
}
