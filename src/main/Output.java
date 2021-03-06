package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class Output implements Runnable{

	private Communicator dCommunicator;
	private JFrame frame;
	private Object[][] tableData;
	private int dataCount;
	private long firstTime;
	private Object[] nextData = {"0", "0", "0", "0", "0"};




	public Output() {
		Communicator com = new Communicator();
		dCommunicator = new Communicator(com.OUTPUT_PORT, "Output");
	}

	public Object[][] getTableData(){
		return tableData;
	}

    public static void main(String[] args) {
		Output o = new Output();
		o.frame = new JFrame("My First GUI");
		//frame.setLayout(new BorderLayout());
		//JScrollBar vbar=new JScrollBar(JScrollBar.VERTICAL, 30, 40, 0, 500);
		//vbar.addAdjustmentListener(new AdjustmentListener() {
		//	@Override
		//	public void adjustmentValueChanged(AdjustmentEvent e) {
		//		frame.repaint();
		//	}
		//});
		//frame.getContentPane().add(vbar, BorderLayout.EAST);
		o.tableData = new Object[500][5];
		o.tableData[0] = new Object[] {"1", "2", "3", "4", "0"};

		o.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		o.frame.setSize(700,1000);
		DataTable table = new DataTable(o.tableData);
		table.setOpaque(true);
		o.frame.setContentPane(table);
		o.frame.pack();
		o.frame.setVisible(true);


		String[] floorReqs = new String[1000];
		int next = 0;
		o.dataCount = 1;
		Message m = o.dCommunicator.receive(0);
		while(!m.getData()[0].equals("TimeOut")) {

			o.parseReceived(m);



			//if (m.getData()[0].length() > 22 && m.getData()[0].substring(0, 23).equals ("Scheduler sent Elevator") || m.getData()[0].length() > 19 &&  m.getData()[0].substring(10, 20).equals ("Arrived at") ) {
				floorReqs[next++] = m.getData()[0] + "	" + m.getTime();
			//}



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

	private void parseReceived(Message m) {

		String[] received = m.getData()[0].split(" ", 9);

		int elevNo;
		try {
			elevNo = Integer.parseInt(received[1] + "");
		} catch (NumberFormatException e) {
			return;
		}

		System.out.println("=========================================================" + received[1] + "=============================================" + received[2] + "============" + elevNo);
		if (received[2].equals("is") && received[3].equals("currently")) {
			//these come in as the distance above a given floor, so we'll parse the distance
			double floorNum;
			if (received[5].equals("at")) {
				floorNum = Integer.parseInt(received[6]);
			}else {
				floorNum = Double.parseDouble(received[4].substring(0, received[4].length() - 1));
				//then devide by 4.3 (height of 1 floor) to get a offset above/below a given floor...
				floorNum = floorNum / (4.3);
				if (received[5].equals("above")) {
					floorNum += Integer.parseInt(received[7]);
				}
				if (received[5].equals("below")) {
					floorNum = Integer.parseInt(received[7]) - floorNum;
				}
				System.out.println("=============" + floorNum);
			}


			switch (elevNo) {
				case 1:
					nextData[0] = floorNum;
					break;
				case 2:
					nextData[1] = floorNum;
					break;
				case 3:
					nextData[2] = floorNum;
					break;
				case 4:
					nextData[3] = floorNum;
					break;
			}
		}
		int zeroCount = 0;
		for (int i =0; i < 4; i++) {
			if (nextData[i] == "0") {
				zeroCount++;
			}
		}
		if (zeroCount <= 1) {
			if (dataCount == 1) {
				firstTime = m.getTime();
				nextData[4] = 0;
			}else {
				nextData[4] = m.getTime() - firstTime;
			}
			tableData[dataCount] = nextData;
			DataTable table = new DataTable(tableData);
			table.setOpaque(true);
			frame.setContentPane(table);
			frame.pack();
			frame.setVisible(true);
			nextData = new Object[] {"0", "0", "0", "0", "0"};
			dataCount++;
		}
	}

	@Override
	public void run() {

		frame = new JFrame("My First GUI");
		//frame.setLayout(new BorderLayout());
		//JScrollBar vbar=new JScrollBar(JScrollBar.VERTICAL, 30, 40, 0, 500);
		//vbar.addAdjustmentListener(new AdjustmentListener() {
		//	@Override
		//	public void adjustmentValueChanged(AdjustmentEvent e) {
		//		frame.repaint();
		//	}
		//});
		//frame.getContentPane().add(vbar, BorderLayout.EAST);
		tableData = new Object[500][5];
		tableData[0] = new Object[] {"1", "2", "3", "4", "0"};

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700,1000);
		DataTable table = new DataTable(tableData);
		table.setOpaque(true);
		frame.setContentPane(table);
		frame.pack();
		frame.setVisible(true);


		String[] floorReqs = new String[1000];
		int next = 0;
		dataCount = 1;
		Message m = dCommunicator.receive(0);
		while(!m.getData()[0].equals("TimeOut")) {

			parseReceived(m);


			//if (m.getData()[0].length() > 22 && m.getData()[0].substring(0, 23).equals ("Scheduler sent Elevator") || m.getData()[0].length() > 19 &&  m.getData()[0].substring(10, 20).equals ("Arrived at") ) {
			floorReqs[next++] = m.getData()[0] + "	" + m.getTime();
			//}


			//should be a condition here to stop the communicator
			System.out.println("==========================FloorReqs and arrivals=====================");
			for (int i = 0; i < next; i++) {
				System.out.println(floorReqs[i]);
			}
			System.out.println("==========================FloorReqs and arrivals=====================");
			System.out.println("The output is being sent");
			m = dCommunicator.receive(20000);
			System.out.println("Output message is:" + m.getData()[0]);
		}
	}
}

