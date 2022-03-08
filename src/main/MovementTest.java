package main;

import java.time.Clock;

import static java.lang.Thread.sleep;

public class MovementTest {
    private ElevatorMovement em = new ElevatorMovement();
    private Clock time = Clock.systemDefaultZone();
    public static void main(String args[]) {
        MovementTest m = new MovementTest();
        System.out.println("movin");
        m.em.move(5);
        for (int i = 0; i < 5; i++) {
            double velocity = m.em.getVelocity();
            System.out.println("time of arrival: " + m.em.arriveWhen(5, velocity));
            try {
                System.out.println("sleeping");
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
