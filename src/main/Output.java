package main;

public class Output {
	//declaring this here but should be in communicator class
	final int OUTPUT_PORT = 35;


	private Communicator dCommunicator = new Communicator(com.OUTPUT_PORT, "output");

	public Output()

	{
		while ( true) {
			System.out.println("The output is being sent");
			Message m = dCommunicator.receive(0);
			System.out.println("Output message is:" + m);

			//should be a condition here to stop the communicator
		}
	}


}

