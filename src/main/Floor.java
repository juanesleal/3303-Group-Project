package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Clock;
import java.util.LinkedList;

public class Floor {

    private Communicator floorCommunicator;
    private LinkedList<String[]> messages = new LinkedList<>();

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
        f.readAndSort();
        for (String[] s : f.messages) {
            System.out.println(s[0]);
            Message m = f.floorCommunicator.rpc_send(new Message(s, time.millis(), "Scheduler"));
            while (m.getData()[0].equals("BadRequest")) {
                //wait 0.5 seconds, send again
                try {
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
                m = f.floorCommunicator.rpc_send(new Message(s, time.millis(), "Scheduler"));
            }
        }
        //all events sent to Scheduler, now we wait for button requests
        Message m = f.floorCommunicator.receive();
        if (m.getData()[0].equals("ButtonReq")) {
            boolean ok = false;
            //figure out which floor it's for...
            for (String[] s : f.messages) {
                if (m.getData()[1].equals(s[0])) {
                    f.floorCommunicator.send(new Message(new String[]{"OK", s[3]}, time.millis(), m.getToFrom()));
                    ok = true;
                }
            }
            if (!ok) {
                f.floorCommunicator.send(new Message(new String[]{"Request Not Availible"}, time.millis(), m.getToFrom()));
            }
        }
        // when floor gets a request send in the following format: "FloorRequest, time, where, Up/Down, buttonReq on arrival"

        //System.out.println("== Floor Subsystem finished");
    }
    private void readAndSort() {
        try {
            //    Read floor data values from file
            BufferedReader br = new BufferedReader(new FileReader("FloorEventTest.txt"));

            String line;

            while ((line = br.readLine()) != null) {

                //    Read line and convert to floor data
                //fd = Event.parseString(line);

                //    Send data to scheduler
                //FIXME
                String[] data = (line.replace("\t", "_")).split("_", 7);
                String[] trimmedData = new String[data.length];

                for (int i = 0; i < data.length; i++) {
                    if (!data[i].equals("")) {
                        trimmedData[i] = data[i];
                    }
                }
                String[] messageData;
                messageData = trimmedData;
                //now we sort the messageData into the messageData LinkedList
                if (!messages.isEmpty()) {
                    for (int i =0; i < messages.size(); i++) {
                        if (messages.get(i)[0].compareTo(messageData[0]) > 0) {
                            //messageData is sooner then what already exists
                            messages.add(i, messageData);
                            break;
                        }
                    }
                    //did we already add?
                    if (!(messages.contains(messageData))) {
                        //it is the largest so far
                        messages.addLast(messageData);
                    }
                }else {
                    messages.addFirst(messageData);
                }
            }
            br.close();
        }catch(IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
