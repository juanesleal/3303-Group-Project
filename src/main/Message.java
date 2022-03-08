package main;

//TODO huge update, this needs much more versatility, see Communicator

public class Message {

    private String[] data;
    private long time;
    private String to;

    public Message(String[] msgData, long msgTime, String msgTo) {
        data = msgData;
        time = msgTime;
        to = msgTo;
    }
    public String[] getData() {
        return data;
    }

    public long getTime() {
        return time;
    }

    public String getTo() {
        return to;
    }
}
