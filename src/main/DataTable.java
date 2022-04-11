package main;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.Dimension;
import java.awt.GridLayout;

public class DataTable extends JPanel {

    private int next;
    private JTable table;
    private JScrollPane scrollPane;


    public DataTable (Object[][] data) {
        super(new GridLayout(1,0));

        String[] columnNames = {"Elevator 1",
                "Elevator 2",
                "Elevator 3",
                "Elevator 4",
                "Time"};
        table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(500, 700));
        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        add(scrollPane);
    }
}
