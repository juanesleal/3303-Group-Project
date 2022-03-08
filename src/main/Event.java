package main;
public class Event {
    public String time;
    public int floor;
    public String fButton;
    public int cButton;
    public Event(String tm, int flr, String fBut, int cBut) {
        this.time = tm;
        this.floor = flr;
        this.fButton = fBut;
        this.cButton = cBut;
    }
    public static Event parseString(String s) {
        //this should split the string that we expect from input into an array, then trim all the extra whitespace.
        Object[] vals = s.split("\\s+");
        //use the now splitted data to instantiate and return an event.
        Event ev = new Event((String)vals[0], Integer.parseInt((String)vals[1]), (String)vals[2], Integer.parseInt((String)vals[3]));
        return ev;
    }
}
