package main;

//TODO huge update, this needs much more versatility, see Communicator

public class Message {

    private String[] data;
    private long time;
    private String toFrom;

    public Message(String[] msgData, long msgTime, String msgToFrom) {
        data = msgData;
        time = msgTime;
        toFrom = msgToFrom;
    }
    public String[] getData() {
        return data;
    }

    public long getTime() {
        return time;
    }

    public String getToFrom() {
        return toFrom;
    }
}
