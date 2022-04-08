package main;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.time.Clock;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class Floor implements Runnable{

    private Communicator floorCommunicator;
    private Communicator outputCommunicator;
    private LinkedList<String[]> messages = new LinkedList<>();
    private int nextFloorReq = 0;
    
    private DatagramPacket sendPacket;
	private DatagramSocket socket;
	private ByteArrayOutputStream buildPacket;
	private String mode = "netASCII";
	private String filename = "test.txt";
	private InetAddress localHostVar;


    /**
     * Generates a new floor subsystem that communicates using the specified EventHolder
     *  specifies the eventHolder for shared memory.
     */
    public Floor() {
        Communicator com = new Communicator();
        floorCommunicator = new Communicator(com.FLOOR_PORT, "Floor");
        outputCommunicator = new Communicator(com.OUTPUT_PORT, "Output");
    }


    @Override
    public void run() {
        readAndSort();
        //edited portion
        while(true) {
            floorMessageCheck();
        }
    }

    public static void main(String[] args) {
        Floor f = new Floor();
        f.run();
    }


    // when floor gets a request send in the following format: "FloorRequest, time, where, Up/Down, buttonReq on arrival"

    //System.out.println("== Floor Subsystem finished");
    private void readAndSort () {
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
                String[] trimmedData = new String[data.length + 1];
                trimmedData[0] = "FloorRequest";
                for (int i = 0; i < data.length; i++) {
                    if (!data[i].equals("")) {
                        trimmedData[i + 1] = data[i];
                    }
                }
                String[] messageData;
                messageData = trimmedData;
                //now we sort the messageData into the messageData LinkedList
                if (!messages.isEmpty()) {
                    for (int i = 0; i < messages.size(); i++) {
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
                } else {
                    messages.addFirst(messageData);
                }
            }
            br.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void floorMessageCheck (){
        Clock time = Clock.systemDefaultZone();

        Message m = floorCommunicator.receive(0);

        if (m.getData()[0].equals("ButtonReq")) {
            boolean ok = false;
            //figure out which floor it's for...
            for (String[] s : messages) {
                if (m.getData()[1].equals(s[1])) {
                    floorCommunicator.send(new Message(new String[]{"OK", s[4]}, time.millis(), m.getToFrom()));
                    ok = true;
                }
            }
            if (!ok) {
                floorCommunicator.send(new Message(new String[]{"Request Not Availible"}, time.millis(), m.getToFrom()));
            }
        }

        if (m.getData()[0].equals("EventReq")) {
            //scheduler wants next event, we've already sorted the events...
            floorCommunicator.send(new Message(messages.get(nextFloorReq), time.millis(), m.getToFrom()));
            nextFloorReq++;
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            floorMessageCheck();
        }


    }
}
