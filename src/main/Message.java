package main;

public class Message {
    private String func;
    private int floor;
    public Message(String s, int i) {
        func = s;
        floor = i;
    }
    public String getFunc() {
        return func;
    }

    public int getFloor() {
        return floor;
    }
}
