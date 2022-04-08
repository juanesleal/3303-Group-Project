package main;

public class Output {
	Communicator dCommunicator;


	public Output() {
		Communicator com = new Communicator();
		Communicator dCommunicator = new Communicator(com.OUTPUT_PORT, "Output");
	}

    public static void main(String[] args) {
		Output o = new Output();
		String[] floorReqs = new String[20];
		int next = 0;
		while(true) {
			System.out.println("The output is being sent");
			Message m = o.dCommunicator.receive(0);
			System.out.println("Output message is:" + m);
			
			if (m.getData()[0].length() > 22 && m.getData()[0].substring(0, 23).equals ("Scheduler sent Elevator") || m.getData()[0].substring(10, 20).equals ("Arrived at") ) {
				floorReqs[next++] = m.getData()[0] + ":::::" + m.getTime();
			}



			//should be a condition here to stop the communicator
			System.out.println("==========================FloorReqs and arrivals=====================");
			for (int i = 0; i < next; i++) {
				System.out.println(floorReqs[i]);
			}
			System.out.println("==========================FloorReqs and arrivals=====================");
		}

	}


}

