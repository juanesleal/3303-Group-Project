package main;

public class Output {

	private Communicator dCommunicator;

	public Output() {
		Communicator com = new Communicator();
		dCommunicator = new Communicator(com.OUTPUT_PORT, "Output");
	}

    public static void main(String[] args) {
		Output o = new Output();
		String[] floorReqs = new String[20];
		int next = 0;
		Message m = o.dCommunicator.receive(0);
		while(!m.getData()[0].equals("TimeOut")) {


			if (m.getData()[0].length() > 22 && m.getData()[0].substring(0, 23).equals ("Scheduler sent Elevator") || m.getData()[0].substring(10, 20).equals ("Arrived at") ) {
				floorReqs[next++] = m.getData()[0] + "	" + m.getTime();
			}



			//should be a condition here to stop the communicator
			System.out.println("==========================FloorReqs and arrivals=====================");
			for (int i = 0; i < next; i++) {
				System.out.println(floorReqs[i]);
			}
			System.out.println("==========================FloorReqs and arrivals=====================");
			System.out.println("The output is being sent");
			m = o.dCommunicator.receive(20000);
			System.out.println("Output message is:" + m.getData()[0]);
		}
		//all done, all requests have been handled... analysis:
		long[] data = new long[next + 1];
		long first = Long.parseLong((floorReqs[0].split("\t", 3))[1]);

		for (int i = 0; i < next; i++) {
			String[] s = floorReqs[i].split("\t", 3);
			//this way each time in data corresponds to the amount of time since the last message...
			data[i] = Long.parseLong(s[1]) - first;
			System.out.println("Data: " + i + " is " + data[i] + "ms after the first");
		}
	}


}

