package main;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class Communicator {
    //definitions

    final int MAXELEVATORS = 9;
    //max amount of data that can me put in a message
    final int MAXDATA = 5;
    final int SCHEDULER_EPORT = 23;
    final int SCHEDULER_FPORT = 69;
    final int FLOOR_PORT = 28;
    //TODO use these constants, ELEVATORBYTE MUST be different then FLOOR and SCHEDULER[0]
    final byte[] SCHEDULER_BYTE = {0,1};
    final byte[] FLOOR_BYTE = {0,2};
    final byte ELEVATOR_BYTE = 1;

    DatagramPacket sendPacket, receivePacket;
    DatagramSocket sendReceiveSocket;
    //this is how we can tell who we are...
    String me;
    Map<String, Integer> received;

    //default constructer, only for getting default values.
    public Communicator() { }

    public Communicator(int port, String user) {
        if (port == 0) {
            try {
                sendReceiveSocket = new DatagramSocket();
            }catch(SocketException e) {
                e.printStackTrace();
            }
        }else {
            try {
                sendReceiveSocket = new DatagramSocket(port);
            }catch(SocketException e) {
                e.printStackTrace();
            }
        }
        me = user;
        received = new HashMap<>();
        received.put("Floor", 0);
        for (int i = 1; i <= MAXELEVATORS; i++) {
            //this should put the keys for received (Elevator1, Elevator2, ....)
            received.put(("Elevator" + i), 0);
        }
    }
    public int getFloorPort() {
        return received.get("Floor");
    }
    public Message rpc_send(Message m) {
        //TODO timeout
        send(m);
        //successfully sent.
        return receive();
    }
    public Message receive() {
        //string represents who we received from
        String s;
        //setup data to receive (up to 100 bytes)
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
        try {
            System.out.println(me + " waiting...");
            sendReceiveSocket.receive(receivePacket);
        } catch (IOException e) {
            System.out.println("Receive timed out\n" + e);
            e.printStackTrace();
            //"loop"
            receive();
        }

        //now we find out who it's from
        if (data[0] == FLOOR_BYTE[0] && data[1] == FLOOR_BYTE[1]) {
            //this data is from the Floor
            s = "Floor";
        }else if (data[0] == ELEVATOR_BYTE) {
            //this data is from the Elevator
            //update the port information.
            //note this assumes that the next BYTE is the elevatorid-------------------------------------------------------------------------------------------------
            received.replace(("Elevator" + data[1]), receivePacket.getPort());
            s = "Elevator" + data[1];
        }else if (data[0] == SCHEDULER_BYTE[0] && data[1] == SCHEDULER_BYTE[1]){
            //from the Scheduler, we don't have to save this port info
            s = "Scheduler";
        }else {
            s = "ERROR";
        }


        // Process the received Floor, Scheduler, or Elevator.
        System.out.println(me + " Packet received:");
        System.out.println("From " + s + ": " + receivePacket.getAddress());
        System.out.println(s + " port: " + receivePacket.getPort());
        int len = receivePacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: " );

        // Form a String from the byte array.
        String received = new String(data,0,len);
        System.out.println("String: " + received + "\n");
        System.out.println("Bytes: ");
        for (int i = 0; i < len; i++) {
            System.out.print(receivePacket.getData()[i]);
            System.out.print(",");
        }
        System.out.println("\n");
        //parse the received data into a Message
        Object[] args = new Object[]{data, null};
        parse(args);
        return (Message) args[1];
    }
    public void send(Message m) {
        Object[] args = {null, m};
        //since parse is implemented by the Scheduler,Elevator, and Floor Subclasses,
        // it will tell us who we are and where to send.
        parse(args);
        byte[] request = (byte[]) args[0];
        //who are we, who are we sending to?
        //FIXME
        int sendPort = 0;
        if (request[request.length - 1] == SCHEDULER_BYTE[1] && request[request.length - 2] == SCHEDULER_BYTE[0]) {
            //this means we are sending to the scheduler, we still need to know who we are.
            if (me == "Floor") {
                //this means WE are the Floor.
                sendPort = SCHEDULER_FPORT;
            }else if (me.substring(0, (me.length() - 1)).equals("Elevator")) {
                sendPort = SCHEDULER_EPORT;
            }
        }else if (request[request.length - 1] == FLOOR_BYTE[1] && request[request.length - 2] == FLOOR_BYTE[0]) {
            // we are not sending to Scheduler, we may be an error, unless we are the Scheduler.
            if (me.equals("Scheduler") || me.substring(0, (me.length() - 1)).equals("Elevator")) {
                //who are we sending it to
                //sending to floor
                sendPort = FLOOR_PORT;
            }
        } else if (request[request.length - 2] == ELEVATOR_BYTE && !(me.substring(0, (me.length() - 1)).equals("Elevator"))) {
            sendPort = received.get("Elevator" + request[request.length - 1]);
        }

        System.out.println(request[request.length - 2]);
        // Construct a datagram packet that is to be sent to a specified port (sendPort)
        try {
            sendPacket = new DatagramPacket(request, request.length,
                    InetAddress.getLocalHost(), sendPort);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println(me + ": Sending packet:");
        System.out.println("To "  + ": " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        int len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
        System.out.println("String: " + new String(sendPacket.getData(),0,len));
        System.out.print("Bytes: ");
        for (int i = 0; i < request.length; i++) {
            System.out.print(request[i]);
            System.out.print(",");
        }
        System.out.println("\n");
        // Send the datagram packet to the server via the send/receive socket.

        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
    public void parse(Object[] parseOne) {
        byte[] msg = (byte[]) parseOne[0];
        Message m = (Message) parseOne[1];
        ByteBuffer bb = ByteBuffer.allocate(100);
        if (msg == null && m != null) {
            //this means that we need to parse the message into byte[]
            //using byteBuffer since it's nice, recipient expects 100 bytes, lets use it.
            //check if we are the Scheduler, the Floor or the Elevator
            if (me.equals("Scheduler")) {
                bb.put(SCHEDULER_BYTE);
            }else if (me.equals("Floor")) {
                bb.put(FLOOR_BYTE);
            }else if (me.substring(0,me.length() - 1).equals("Elevator")) {
                bb.put(ELEVATOR_BYTE);
                bb.put((byte)Character.getNumericValue(me.charAt(me.length() - 1)));
            }
            bb.put((byte)0);
            //add each string data member with a 1 following
            String[] data = m.getData();
            for (int i = 0; i < data.length; i++) {
                bb.put(data[i].getBytes());
                //follow each data member with a 1 so we can tell strings apart from the next data thing
                //if this is the last string, no 1 follows
                if (i != data.length - 1) {
                    bb.put((byte)1);
                }
            }
            bb.put((byte)0);
            bb.putLong(m.getTime());
            bb.put((byte)0);
            //throw in netascii
            bb.put("netascii".getBytes());
            bb.put((byte)0);
            //now we parse the String "to"
            String s = m.getToFrom();
            if (s.substring(0, s.length() - 1).equals("Elevator")) {
                bb.put(ELEVATOR_BYTE);
                //make damn sure we are putting the actual desired value...----------------------------------------------------------------------------------------------
                char num = s.charAt(s.length() - 1);
                bb.put((byte)Character.getNumericValue(num));
            }else if (m.getToFrom().equals("Floor")){
                bb.put(FLOOR_BYTE);
            }else if (m.getToFrom().equals("Scheduler")){
                bb.put(SCHEDULER_BYTE);
            }
            //write to length so it isn't 100 bytes long.
            byte[] bytes = new byte[bb.position()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = bb.get(i);
            }
            parseOne[0] = bytes;
            //now the output is written to the proper length
        }else if (msg != null && m == null){
            //convert msg to m
            //Now we need to process the data.
            //use bb since it's getters are nice.
            bb.put(msg);
            if (bb.get(0) == FLOOR_BYTE[0] && bb.get(1) == FLOOR_BYTE[1]) {
                bb.rewind();
                //from the Floor
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                data[0] = "";
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this is a FloorBYTE---------------------------------------------------------------------------------------------------
                bb.get(); //this is a Floorbyte
                bb.get(); //this should be a 0
                byte character = bb.get();
                while (character != 0) {
                    if (character == 1) {
                        //next String
                        count++;
                        data[count] = "";
                    }else {
                        //put each char from the string into the string
                        data[count] += (char)character;
                    }
                    character = bb.get();
                }

                //bb should be at the long for time
                parseOne[1] = new Message(data, bb.getLong(), "Floor");
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }

                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get(bb.position()) != SCHEDULER_BYTE[0] && me.equals("Scheduler")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                if (bb.get(bb.position() + 1) != SCHEDULER_BYTE[1] && me.equals("Scheduler")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                if (bb.get(bb.position()) != FLOOR_BYTE[0] && me.equals("Floor")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                if (bb.get(bb.position() + 1) != FLOOR_BYTE[1] && me.equals("Floor")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }

                //all set
            }else if (bb.get(0) == ELEVATOR_BYTE){
                bb.rewind();
                //from the Elevator
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                data[0] = "";
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this is a ELEVATORBYTE---------------------------------------------------------------------------------------------------
                byte id = bb.get(); //this is a ELEVATORidBYTE
                bb.get(); //this should be a 0
                byte character = bb.get();
                while (character != 0) {
                    if (character == 1) {
                        //next String
                        count++;
                        data[count] = "";
                    }else {
                        //put each char from the string into the string
                        data[count] += (char)character;
                    }
                    character = bb.get();
                }

                //bb should be at the long for time
                parseOne[1] = new Message(data, bb.getLong(), "Elevator" + id);
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }

                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get(bb.position()) != SCHEDULER_BYTE[0] && me.equals("Scheduler")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Elevator" + id);
                }
                if (bb.get(bb.position() + 1) != SCHEDULER_BYTE[1] && me.equals("Scheduler")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Elevator" + id);
                }
                if (bb.get(bb.position()) != FLOOR_BYTE[0] && me.equals("Floor")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Elevator" + id);
                }
                if (bb.get(bb.position() + 1) != FLOOR_BYTE[1] && me.equals("Floor")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Elevator" + id);
                }
            }else if (bb.get(0) == SCHEDULER_BYTE[0] && bb.get(1) == SCHEDULER_BYTE[1]){
                bb.rewind();
                //from the Scheduler
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                data[0] = "";
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this is a SCHEDULERBYTE---------------------------------------------------------------------------------------------------
                bb.get(); //this is a SCHEDULERBYTE
                bb.get(); //this should be a 0
                byte character = bb.get();
                while (character != 0) {
                    if (character == 1) {
                        //next String
                        count++;
                        data[count] = "";
                    }else {
                        //put each char from the string into the string
                        data[count] += (char)character;
                    }
                    character = bb.get();
                }

                //bb should be at the long for time
                parseOne[1] = new Message(data, bb.getLong(), "Scheduler");
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }

                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get(bb.position()) != FLOOR_BYTE[0] && me.equals("Floor")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Scheduler");
                }
                if (bb.get(bb.position() + 1) != FLOOR_BYTE[1] && me.equals("Floor")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Scheduler");
                }
                if (bb.get(bb.position()) != ELEVATOR_BYTE && me.substring(0,me.length()-1).equals("Elevator")) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Scheduler");
                }
            }
        }
    }
}
/*
class ElevatorCommunicator extends Communicator {
    int id;

    public ElevatorCommunicator(int port, String user) {
        super(port, user);
        //IMPORTANT Assumes that the last Character of me is a number---------------------------------------------------------------------------------------------------------------------------
        id = Character.getNumericValue(me.charAt(me.length() - 1));
    }


    //TODO stop using parse seperately, we probably don't even need seperate classes...------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    public void parse(Object[] parseOne) {
        System.out.println("Elevator Parsing...");
        ByteBuffer bb = ByteBuffer.allocate(100);
        if (parseOne[0] == null && parseOne[1] != null) {
            Message m = (Message) parseOne[1];
            //this means that we need to parse the message into byte[]
            //using byteBuffer since it's nice, recipient expects 100 bytes, lets use it.
            //We are an Elevator
            bb.put(ELEVATOR_BYTE);
            //see constructor for where we get id
            bb.put((byte)id);
            //seperate id byte from data byte
            bb.put((byte)0);
            //add each string data member with a 1 following
            String[] data = m.getData();
            for (int i = 0; i < data.length; i++) {
                bb.put(data[i].getBytes());
                //follow each data member with a 1 so we can tell strings apart from the next data thing
                //if this is the last string, no 1 follows
                if (i != data.length - 1) {
                    bb.put((byte)1);
                }
            }
            bb.put((byte)0);
            //when we are sending something and we don't have an actual time value, we set time = 0-----------------------------------------------------------------------------------------
            if (m.getTime() == 0) {
                //put in the current time, since the message doesn't specify a time.
                bb.putLong(time.millis());
            }else {
                System.out.println(m.getTime());
                bb.putLong(m.getTime());
            }
            bb.put((byte)0);
            //throw in netascii for mode
            bb.put("netascii".getBytes());
            bb.put((byte)0);
            //now we parse the String "to"
            if (m.getToFrom() == "Scheduler") {
                bb.put(SCHEDULER_BYTE);
            }else if (m.getToFrom() == "Floor"){
                bb.put(FLOOR_BYTE);
            }else {
                //FIXME
                System.out.println("parse error, message had unexpected 'to': " + m.getToFrom());
            }
            //done...

            //write to length so it isn't 100 bytes long.
            byte[] bytes = new byte[bb.position()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = bb.get(i);
            }
            parseOne[0] = bytes;
            //now the output is written to the proper length
        }else if (parseOne[0] != null && parseOne[1] == null){
            byte[] msg = (byte[]) parseOne[0];
            //convert msg to m
            // Now we need to process the data.
            //we are Elevator, so packet should be of the form: 0(1 or 2)0sometext1sometext0somelong0netascii00(1 or 2)
            //use bb since it's getters are nice.
            bb.put(msg);
            if (bb.get(0) == SCHEDULER_BYTE[0] && bb.get(1) == SCHEDULER_BYTE[1]) {
                //from the Scheduler
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this is a 0---------------------------------------------------------------------------------------------------
                bb.get(); //this is a 1
                bb.get(); //this should be a 0
                while (bb.get() != 0) {
                    if (bb.get() == 1) {
                        //FIXME see SchedulerCommunicator
                        //next String
                        count++;
                    }else {
                        //put each char from the string into the string
                        data[count] += bb.getChar();
                    }
                }
                System.out.println("Tester in parse: bb.position " + bb.position());
                parseOne[1] = new Message(data, bb.getLong(), "Scheduler");
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }
                bb.get(); //should be 0
                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get() != ELEVATOR_BYTE) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Scheduler");
                }
                if (bb.get() != id) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Scheduler");
                }
                //all set
            }else if (bb.get(0) == FLOOR_BYTE[0] && bb.get(1) == FLOOR_BYTE[1]){
                //from the Floor
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this should be a 0---------------------------------------------------------------------------------------------------
                while (bb.get() != 0) {
                    if (bb.get() == 1) {
                        //next String
                        count++;
                    }else {
                        //put each char from the string into the string
                        data[count] += bb.getChar();
                    }
                }
                //increment the bb so that getLong "works"
                bb.get();
                System.out.println("Tester in parse: bb.position " + bb.position());
                parseOne[1] = new Message(data, bb.getLong(), "Floor");
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }
                bb.get(); //should be 0
                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get() != ELEVATOR_BYTE) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                if (bb.get() != id) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                //all set
            }
        }
        System.out.println(parseOne[0].getClass().getName());
    }
}

//TODO Scheduler needs two Communicators or smth, it will also have two threads...

class SchedulerCommunicator extends Communicator {

    public SchedulerCommunicator(int port, String user) {
        super(port, user);
    }

    public void parse(Object[] parseOne) {
        byte[] msg = (byte[]) parseOne[0];
        Message m = (Message) parseOne[1];
        ByteBuffer bb = ByteBuffer.allocate(100);
        if (msg == null && m != null) {
            //this means that we need to parse the message into byte[]
            //using byteBuffer since it's nice, recipient expects 100 bytes, lets use it.

            //our id as the Scheduler is 01
            bb.put(SCHEDULER_BYTE);
            //add each string data member with a 1 following
            for (String str : m.getData()) {
                bb.put(str.getBytes());
                //follow each data member with a 1 so we can tell strings apart from the next data thing
                bb.put((byte)1);
            }
            bb.put((byte)0);
            bb.putLong(m.getTime());
            bb.put((byte)0);
            //throw in netascii
            bb.put("netascii".getBytes());
            bb.put((byte)0);
            //now we parse the String "to"
            String s = m.getToFrom();
            if (s.substring(0, s.length() - 1).equals("Elevator")) {
                bb.put(ELEVATOR_BYTE);
                //make damn sure we are putting the actual desired value...----------------------------------------------------------------------------------------------
                char num = s.charAt(s.length() - 1);
                bb.put((byte)Character.getNumericValue(num));
            }else if (m.getToFrom() == "Floor"){
                bb.put(FLOOR_BYTE);
            }else {
                //FIXME this is an error message
            }
            //write to length so it isn't 100 bytes long.
            byte[] bytes = new byte[bb.position()];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = bb.get(i);
            }
            parseOne[0] = bytes;
            //now the output is written to the proper length
        }else if (msg != null && m == null){
            //convert msg to m
            //Now we need to process the data.
            //we are Scheduler, so packet should be of the form: (02 or 1x)0sometext1sometext0somelong0netascii001
            //use bb since it's getters are nice.
            bb.put(msg);
            if (bb.get(0) == FLOOR_BYTE[0] && bb.get(1) == FLOOR_BYTE[1]) {
                //from the Floor
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this is a 0---------------------------------------------------------------------------------------------------
                bb.get(); //this is a 2 or whatever bloorbytes are
                bb.get(); //this should be a 0
                while (bb.get() != 0) {
                    if (bb.get() == 1) {
                        //next String
                        count++;
                    }else {
                        //put each char from the string into the string
                        data[count] += bb.getChar();
                    }
                }
                //bb should be at the long for time
                parseOne[1] = new Message(data, bb.getLong(), "Floor");
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }
                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get() != SCHEDULER_BYTE[0]) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                if (bb.get() != SCHEDULER_BYTE[1]) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Floor");
                }
                //all set
            }else if (bb.get(0) == ELEVATOR_BYTE){
                bb.rewind();
                //from the Elevator
                //make a string array and a counter
                String[] data = new String[MAXDATA];
                data[0] = "";
                int count = 0;
                //basically loop from after the first three bytes till we hit the first 0 (which indicates we are done with data)
                bb.get(); //this is a ELEVATORBYTE---------------------------------------------------------------------------------------------------
                byte id = bb.get(); //this is a ELEVATORidBYTE
                bb.get(); //this should be a 0
                byte character = bb.get();
                while (character != 0) {
                    if (character == 1) {
                        //next String
                        count++;
                    }else {
                        //put each char from the string into the string
                        data[count] += (char)character;
                    }
                    character = bb.get();
                }

                //bb should be at the long for time
                parseOne[1] = new Message(data, bb.getLong(), "Elevator" + id);
                //now check if the message is supposed to be To me:
                bb.get(); //now it should be at the mode bytes;
                while (bb.get() != 0) {
                    //nothing, lol these should be netascii bytes
                }

                //finally, bb.position should be past the netascii bytes and at the To indecator bytes
                if (bb.get() != SCHEDULER_BYTE[0]) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Elevator" + id);
                }
                if (bb.get() != SCHEDULER_BYTE[1]) {
                    parseOne[1] = new Message(new String[]{"PacketNotForMe"}, 0, "Elevator" + id);
                }
            }
        }
    }
}
*/