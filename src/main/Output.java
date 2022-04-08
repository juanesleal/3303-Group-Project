package main;

public class Output {
	//declaring this here but should be in communicator class
	final int OUTPUT_PORT = 35;
	
	
	Communicator com = new Communicator();
	
	private Communicator cCommunicator;
    private Communicator dCommunicator = new Communicator(com.OUTPUT_PORT, "output");
	
	while(true) {
		
		System.out.println("The output is being sent");
		
		Message m = dCommunicator.receive(0);
		
		System.out.println("Output message is:" + m);
		
		 //should be a condition here to stop the communicator
	}
	
	
}
