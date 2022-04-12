package main;

public class Initializer implements Runnable {
    private int elevCount = 1;
    String filename = "";

    public Initializer(int eC, String fn) {
        elevCount = eC;
        filename = fn;
    }

    public static void main(String[] args) {
        //IMPORTANT: ARGS[0] is the amount of elevators we have... if null, 1 elevator is the default

        String fn = "";
        int eC = 1;

        if (args.length > 1) {
            System.out.println("INITIALIZER: " + args[1]);
            fn += args[1];
            eC = Integer.parseInt(args[0]);
        }
        Initializer i = new Initializer(eC, fn);
        i.run();
    }

    @Override
    public void run() {
        //IMPORTANT: ARGS[0] is the amount of elevators we have... if null, 1 elevator is the default
        Thread scheduler, floor;



        if (!filename.equals("")) {
            floor = new Thread(new Floor(filename), "Floor");
        }else {
            floor = new Thread(new Floor("FloorEventTest.txt"), "Floor");
        }

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
