package main;

import java.time.Clock;

public class Floor {

    private Communicator floorCommunicator;

    /**
     * Generates a new floor subsystem that communicates using the specified EventHolder
     *  specifies the eventHolder for shared memory.
     */
    public Floor() {
        Communicator com = new Communicator();
        floorCommunicator = new Communicator(com.FLOOR_PORT, "Floor");
    }

    public static void main(String[] args) {
        Clock time = Clock.systemDefaultZone();
        Floor f = new Floor();
        // when floor gets a request send in the following format: "FloorRequest, time, where, Up/Down, buttonReq on arrival"
        while (true) {
            Message m = f.floorCommunicator.rpc_send(new Message(new String[] {"FloorRequest", "14:05:15.0", "4", "Up", "6"}, time.millis(), "Scheduler"));
            while (m.getData()[0].equals("BadRequest")) {
                //wait 0.5 seconds, send again
                try {
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                m = f.floorCommunicator.rpc_send(new Message(new String[] {"FloorRequest", "14:05:15.0", "4", "Up", "6"}, time.millis(), "Scheduler"));
            }
            m = f.floorCommunicator.receive();
            if (m.getData()[0].equals("ButtonReq")) {
                //send a default value:
                double rand = Math.random();

                f.floorCommunicator.send(new Message(new String[]{"OK", "" + (((int) rand * 8) + 1)}, time.millis(), m.getToFrom()));
            }
        }
        //try {
            //testing

            //    Read floor data values from file
            //BufferedReader br = new BufferedReader(new FileReader("FloorEventTest.txt"));
            //Event fd;

            //String line;
            /*
            while ((line = br.readLine()) != null) {
                //    Read line and convert to floor data
                //fd = Event.parseString(line);

                //    Send data to scheduler
                System.out.println("== Floor Subsystem sending data << " + line + " >> to schedular");
                //FIXME
                String[] data = line.split(" ", 3);
                System.out.println(data[0]);
                for (int i = 0; i<4; i++) {
                    data[i] = data[i].trim();
                }

                m = f.floorCommunicator.rpc_send(new Message(data, time.millis(), "Scheduler"));
                while (!(m.getData()[0].equals("OK"))) {
                    m = f.floorCommunicator.rpc_send(new Message(data, time.millis(), "Scheduler"));
                }

                //    Sleep unessesary since get and put wait for data
                //Object receivedFd = this.eventHolder.getMsgF();
               // System.out.println("== Floor Subsystem receiving data << " + receivedFd + " >> from schedular");
            }

            //this.eventHolder.put(null);

            br.close();

             */

        //}catch(IOException e) {
            //System.err.println(e.getMessage());
        //}
        //System.out.println("== Floor Subsystem finished");
    }
}
