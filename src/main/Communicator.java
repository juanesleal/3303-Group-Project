package main;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

public interface Communicator {
    public Message rpc_send(Message m);
    public void parse(Object[] parceOne);
    public void receive_send();
}

class ElevatorCommunicator implements Communicator {
    DatagramPacket sendPacket, receivePacket;
    DatagramSocket sendReceiveSocket;
    Elevator elevator;

    public ElevatorCommunicator() {
        try {
            sendReceiveSocket = new DatagramSocket();
        }catch(SocketException e) {
            e.printStackTrace();
        }
    }

    public void setElevator(Elevator elevator) {
        this.elevator = elevator;
    }

    public Message rpc_send(Message m) {
        //note since we are the Scheduler we only send requests


        Object[] args = {null, m};
        parse(args);
        byte[] request = (byte[]) args[0];
        //TODO send it
        // add logic to determine where it should go, specifying port number

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host (in this case we know to send to port 23)
        try {
            sendPacket = new DatagramPacket(request, request.length,
                    InetAddress.getLocalHost(), 23);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Elevator: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
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
        //now receive
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
        try {
            System.out.println("Elevator waiting...");
            sendReceiveSocket.receive(receivePacket);
        } catch (IOException e) {
            System.out.println("Receive timed out\n" + e);
            e.printStackTrace();
        }
        System.out.println("Elevator received");
        args = new Object[]{data, null};
        parse(args);
        return (Message) args[1];
    }

    public void parse(Object[] parseOne) {
        byte[] msg = (byte[]) parseOne[0];
        Message m = (Message) parseOne[1];
        ByteBuffer bb = ByteBuffer.allocate(100);
        if (msg == null && m != null) {
            //this means that we need to parse the message into byte[]
            //using byteBuffer since it's nice, recipient expects 100 bytes, lets use it.

            //TODO Elevator id's
            //our id as the Elevator is 11
            bb.put((byte)1);
            bb.put((byte)1);
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
            bb.putLong(m.getTime());
            bb.put((byte)0);
            //now we parse the String "to"
            if (m.getTo() == "Scheduler") {
                bb.put((byte)0);
                bb.put((byte)1);
            }else {
                //TODO add shit
                System.out.println("parse error, message had unexpected 'to': " + m.getTo());
            }
            //everything added... throw in netascii
            bb.put((byte)0);
            bb.put("netascii".getBytes());
            bb.put((byte)0);
            //done...
            parseOne[0] = bb.array();
        }else if (msg != null && m == null){
            //convert msg to m
            //TODO throw this code into the Elveator parser
            //Now we need to process the data.
            //packet should be of the form: 0(1 or 2)sometext0somelong0sometext0netascii0
            //use bb since it's getters are nice.
            bb.put(msg);
            if (bb.get(0) == 0 && bb.get(1) == 1) {
                //from the Scheduler
                //make a string array and a counter
                String[] data = {};
                int count = 0;
                //basically loop from after the first two bytes till we hit the first 0 (which indicates we are done with data)
                int i = 2;
                while (bb.get(i) != 0) {
                    if (bb.get(i) == 1) {
                        //next String
                        count++;
                    }else {
                        //put each char from the string into the string
                        data[count] += bb.getChar(i);
                    }
                    i++;
                }
                //TODO remove me
                /*
                //get to the long for the time value
                i++;
                // a long is 8 bytes
                byte[] time = new byte[8];
                while (bb.get(i) != 0) {
                    //put each byte from the time long into the byte array
                    time[i] += bb.get(i);
                }
                bb.put(0, time);
                */
                parseOne[1] = new Message(data, bb.getLong(), "Scheduler" + bb.getInt(i + 4));

            }else {
                //TODO add shit
                System.out.println("parse error, message had unexpected from: " + msg[0] + msg[1]);
            }
        }
    }


    public void receive_send() {
        /*
        //string represents who we received from
        String s;
        //setup data to receive (up to 100 bytes)
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
        try {
            System.out.println("Elevator waiting...");
            sendReceiveSocket.receive(receivePacket);
            port = receivePacket.getPort();
        } catch (IOException e) {
            System.out.println("Receive timed out\n" + e);
            e.printStackTrace();
            //"loop"
            receive_send();
        }

        if (data[0] == 0 && data[1] == 1) {
            //From the Scheduler
            s = "Scheduler";
        }else {
            s = "Error";
            System.out.println(s + "Unexpected packet sender");
        }

        // Process the received Floor or Scheduler datagram.
        System.out.println("Elevator Packet received:");
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
        }
        System.out.println("\n");

        //TODO process all that to get a proper response
        // rn we'll echo
        // this might look like a series of checks: (is this for me? is it from someone that makes sense? what is going on?)
        try {
            sendPacket = new DatagramPacket(data, data.length,
                    InetAddress.getLocalHost(), receivePacket.getPort());
        }catch (UnknownHostException e){
            e.printStackTrace();
        }
        System.out.println("Elevator sent: " + new String(data, 0,data.length) + " to " + s);
        */
    }
}

//TODO Scheduler needs two Communicators or smth, it will also have two threads...

class SchedulerCommunicator implements Communicator {
    DatagramPacket sendPacket, receivePacket;
    DatagramSocket sendReceiveSocket;
    Scheduler scheduler;
    private int[] ports;

    public SchedulerCommunicator(int port) {
        try {
            sendReceiveSocket = new DatagramSocket(port);
            //note, ports are the maximum amount of elevators and 1 floor
            ports = new int[10];
        }catch(SocketException e) {
            e.printStackTrace();
        }
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Message rpc_send(Message m) {
        //note since we are the Scheduler we only send requests
        Object[] args = {null, m};
        parse(args);
        byte[] request = (byte[]) args[0];
        //TODO send it
        // add logic to determine where it should go, specifying port number

        // Construct a datagram packet that is to be sent to a specified port
        // on a specified host (in this case we know to send to port 23)
        try {
            sendPacket = new DatagramPacket(request, request.length,
                    InetAddress.getLocalHost(), ports[0]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Scheduler: Sending packet:");
        System.out.println("To host: " + sendPacket.getAddress());
        System.out.println("Destination host port: " + sendPacket.getPort());
        int len = sendPacket.getLength();
        System.out.println("Length: " + len);
        System.out.print("Containing: ");
        System.out.println("String: " + new String(sendPacket.getData(),0,len));
        System.out.print("Bytes: ");
        for (int i = 0; i < request.length; i++) {
            System.out.print(request[i]);
        }
        System.out.println("\n");
        // Send the datagram packet to the server via the send/receive socket.

        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        //now receive
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
        try {
            System.out.println("Scheduler waiting...");
            sendReceiveSocket.receive(receivePacket);
        } catch (IOException e) {
            System.out.println("Receive timed out\n" + e);
            e.printStackTrace();
        }
        args = new Object[]{data, null};
        parse(args);
        return (Message) args[1];
    }


    public void parse(Object[] parseOne) {
        byte[] msg = (byte[]) parseOne[0];
        Message m = (Message) parseOne[1];
        ByteBuffer bb = ByteBuffer.allocate(100);
        if (msg == null && m != null) {
            //this means that we need to parse the message into byte[]
            //using byteBuffer since it's nice, recipient expects 100 bytes, lets use it.

            //our id as the Scheduler is 01
            bb.putInt(0);
            bb.putInt(1);
            //add each string data member with a 1 following
            for (String str : m.getData()) {
                bb.put(str.getBytes());
                //follow each data member with a 1 so we can tell strings apart from the next data thing
                bb.putInt(1);
            }
            bb.putInt(0);
            bb.putLong(m.getTime());
            bb.putInt(0);
            //now we parse the String "to"
            String s = m.getTo();
            if (s.substring(0, s.length()-1) == "Elevator") {
                bb.putInt(1);
                //make damn sure we are putting the actual desired value...
                char num = s.charAt(s.length() - 1);
                bb.putInt(Character.getNumericValue(num));
            }else {
                //TODO add shit
                System.out.println("parse error, message had unexpected 'to': " + s);
            }
            //everything added... throw in netascii
            bb.putInt(0);
            bb.put("netascii".getBytes());
            bb.putInt(0);
            //done...
            parseOne[0] = bb.array();
        }else if (msg != null && m == null){
            //convert msg to m
            //TODO throw this code into the Elveator parser
            //Now we need to process the data.
            //packet should be of the form: 0(1 or 2)sometext0somelong0sometext0netascii0
            //use bb since it's getters are nice.
            bb.put(msg);
            if (bb.get(0) == 0 && bb.get(1) == 1) {
                //from the Scheduler
                //make a string array and a counter
                String[] data = {};
                int count = 0;
                //basically loop from after the first two bytes till we hit the first 0 (which indicates we are done with data)
                int i = 2;
                while (bb.get(i) != 0) {
                    if (bb.get(i) == 1) {
                        //next String
                        count++;
                    }else {
                        //put each char from the string into the string
                        data[count] += bb.getChar(i);
                    }
                    i++;
                }
                //TODO remove me
                /*
                //get to the long for the time value
                i++;
                // a long is 8 bytes
                byte[] time = new byte[8];
                while (bb.get(i) != 0) {
                    //put each byte from the time long into the byte array
                    time[i] += bb.get(i);
                }
                bb.put(0, time);
                */
                parseOne[1] = new Message(data, bb.getLong(), "Elevator" + bb.getInt(i + 4));

            }else {
                //TODO add shit
                System.out.println("parse error, message had unexpected from: " + msg[0] + msg[1]);
            }
        }
    }


    public void receive_send() {
        //string represents who we received from
        String s;
        //setup data to receive (up to 100 bytes)
        byte data[] = new byte[100];
        receivePacket = new DatagramPacket(data, data.length);
        try {
            System.out.println("Scheduler waiting...");
            sendReceiveSocket.receive(receivePacket);
            ports[0] = receivePacket.getPort();
        } catch (IOException e) {
            System.out.println("Receive timed out\n" + e);
            e.printStackTrace();
            //"loop"
            receive_send();
        }

        //TODO this doesn't account for many elevId's

        if (data[0] == 1 && data[1] == 1) {
            //From the Scheduler
            s = "Elevator";
        }else {
            s = "Error";
            System.out.println(s + "Unexpected packet sender");
        }

        // Process the received Floor or Scheduler datagram.
        System.out.println("Scheduler Packet received:");
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
        }
        System.out.println("\n");

        //TODO process all that to get a proper response
        // rn it's going to be OK
        byte[] msg = new byte[2];
        msg[0] = 79; //O
        msg[1] = 75; //K


        try {
            sendPacket = new DatagramPacket(msg, msg.length,
                    InetAddress.getLocalHost(), ports[0]);
        }catch (UnknownHostException e){
            e.printStackTrace();
        }
        try {
            sendReceiveSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Scheduler sent: " + new String(msg, 0, msg.length) + " to " + s + " on " + ports[0]);
    }
}
