package main;

public class Elevator implements Runnable
{
    private EventHolder eventHolder;

    public Elevator(EventHolder eH) {
        eventHolder = eH;
    }

    public void run() {
        while (true) {
            Object event = eventHolder.getMsgE();
            //we've received a message from Scheduler, now send something back, ill make it an event, but it doesn't need to be
            eventHolder.put("Elevator Data");
        }
    }
}
