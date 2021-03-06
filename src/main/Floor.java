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
    private LinkedList<String[]> messages = new LinkedList<>();
    private int nextFloorReq = 0;
    private long lastEventTime = Integer.MAX_VALUE;
    private String filename;


    /**
     * Generates a new floor subsystem that communicates using the specified EventHolder
     *  specifies the eventHolder for shared memory.
     */
    public Floor(String fn) {
        filename = fn;
        Communicator com = new Communicator();
        floorCommunicator = new Communicator(com.FLOOR_PORT, "Floor");
    }


    @Override
    public void run() {
        readAndSort();
        for (String[] s : messages) {
            System.out.println(s[0] + "::" + s[1] + "::" + s[2] + "::" + s[3]);
        }
        //edited portion
        while(true) {
            floorMessageCheck();
        }
    }

    public static void main(String[] args) {
        Floor f = new Floor("FloorEventText.txt");
        f.run();
    }


    // when floor gets a request send in the following format: "FloorRequest, time, where, Up/Down, buttonReq on arrival"

    //System.out.println("== Floor Subsystem finished");
    private void readAndSort () {
        try {
            //    Read floor data values from file
            BufferedReader br = new BufferedReader(new FileReader(filename));

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

    private boolean floorMessageCheck (){
        Clock time = Clock.systemDefaultZone();

        Message m = floorCommunicator.receive(2000);

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
            if (nextFloorReq < (messages.size() - 1)) {
                if (lastEventTime == Integer.MAX_VALUE) {
                    lastEventTime = time.millis();
                    //scheduler wants next event, we've already sorted the events...
                    floorCommunicator.send(new Message(messages.get(nextFloorReq), time.millis(), m.getToFrom()));
                    nextFloorReq++;
                } else if ((time.millis() - lastEventTime) >= 5000) {
                    lastEventTime = time.millis();
                    //it's been more then 10 seconds since we sent the last event'
                    //scheduler wants next event, we've already sorted the events...
                    floorCommunicator.send(new Message(messages.get(nextFloorReq), time.millis(), m.getToFrom()));
                    if (nextFloorReq != (messages.size() - 1)) {
                        nextFloorReq++;
                    } else {
                        floorCommunicator.send(new Message(new String[]{"NoMoreEvents"}, time.millis(), m.getToFrom()));
                    }
                } else {
                    floorCommunicator.send(new Message(new String[]{"NoEvent"}, time.millis(), m.getToFrom()));
                }
                System.out.println("Floor EventReq, Time: " + time.millis() + " last Event: " + lastEventTime + " time since last: " + (time.millis() - lastEventTime));
            } else {
                floorCommunicator.send(new Message(new String[]{"NoMoreEvents"}, time.millis(), m.getToFrom()));
                return false;
            }

        }
        return true;
    }
}
