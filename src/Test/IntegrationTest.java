package Test;

import main.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

@Test
public class IntegrationTest implements Runnable {
    Thread initializeElevator = new Thread(new Initializer(4, ""), "Initializer");
    Output o = new Output();
    Thread outputData = new Thread(o, "Output");


    void testMainInit() {


        String fileName = "";
        if(fileName == "") {
            assertEquals(fileName, "");
            initializeElevator.run();
            System.out.println("Generated file used!");
        }
        else if(fileName == "FloorEventTest.txt"){
            assertEquals(fileName, "FloorEventTest.txt");
            System.out.println("Given file used!");
        }
    }

    void testMainOutput(){
        outputData.run();
        Object[][] tableData = o.getTableData();


        int[] arrivals = new int[20];
        int nextArrival = 0;

        for (int i =0; i < tableData.length; i++) {
            for (int i2 =0; i2 < tableData[i].length; i++) {
                if ((Math.floor(Double.parseDouble((String) tableData[i][i2])) == Double.parseDouble((String) tableData[i][i2])) {
                    arrivals[nextArrival] = (int) Double.parseDouble((String) tableData[i][i2]);
                    nextArrival++;
                }
            }
        }

    }



    @Override
    public void run() {
        testMainInit();
        testMainOutput();
    }
}


