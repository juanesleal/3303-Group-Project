package main;

public class Output {
	
	Communicator com = new Communicator();
	
	private Communicator cCommunicator;
    private Communicator dCommunicator = new Communicator(com.OUTPUT_PORT, "output");

    static void main(String[] args) {
		while(true) {

			Output o = new Output();

			System.out.println("The output is being sent");

			Message m = o.dCommunicator.receive(0);

			System.out.println("Output message is:" + m);

			//should be a condition here to stop the communicator
		}
	}

	
	
}
