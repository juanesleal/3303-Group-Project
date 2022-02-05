package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Floor implements Runnable {

    private EventHolder eventHolder; // eventHolder is used for us to send and retereive data and messages

    /**
     * Generates a new floor subsystem that communicates using the specified EventHolder
     * @param eH, specifies the eventHolder for shared memory.
     */
    public Floor(EventHolder eH) {
        this.eventHolder = eH;
    }

    public void run() {
        try {
            //    Read floor data values from file
            BufferedReader br = new BufferedReader(new FileReader("FloorEventTest.txt"));
            Event fd;

            String line;
            while ((line = br.readLine()) != null) {
                //    Read line and convert to floor data
                fd = Event.parseString(line);

                //    Send data to scheduler
                System.out.println("== Floor Subsystem sending data << " + fd + " >> to schedular");
                this.eventHolder.put(fd);

                //    Sleep unessesary since get and put wait for data
                Object receivedFd = this.eventHolder.getMsgF();
                System.out.println("== Floor Subsystem receiving data << " + receivedFd + " >> from schedular");
            }

            this.eventHolder.put(null);

            br.close();

        }catch(IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("== Floor Subsystem finished");
    }
}
