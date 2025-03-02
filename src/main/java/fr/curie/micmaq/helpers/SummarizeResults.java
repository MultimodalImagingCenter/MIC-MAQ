package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import java.util.HashMap;
import java.util.Map;

public class SummarizeResults implements PlugIn {
    @Override
    public void run(String arg) {
        GenericDialog gd = new GenericDialog("Summarize Results");
        //String[] windowTitles = ResultsTable.getAllResultsWindowTitles();
        //gd.addChoice("Select Results Window", windowTitles, windowTitles[0]);
        gd.addStringField("Results Window Title", "");
        gd.addStringField("Experiment Name Column", "Name experiment");
        gd.showDialog();

        if (gd.wasCanceled()) {
            return;
        }

        // Get the selected Results window and column name
        //String selectedWindow = gd.getNextChoice();
        String selectedWindow = gd.getNextString();
        String experimentNameColumnName = gd.getNextString();

        // Get the selected ResultsTable
        ResultsTable rt = ResultsTable.getResultsTable(selectedWindow);

        if (rt == null) {
            IJ.error("No Results Table", "There is no active Results Table.");
            return;
        }

        // Check if the "experiment name" column exists
        int experimentNameColumn = rt.getColumnIndex(experimentNameColumnName);
        if (experimentNameColumn == -1) {
            IJ.error("Column Not Found", "The 'experiment name' column was not found in the Results Table.");
            return;
        }

        ResultsTable summaryTable = new ResultsTable();
        summarize(summaryTable,rt,experimentNameColumn);
        summaryTable.show("Summary Results");
    }

    public static void summarize(ResultsTable summaryTable, ResultsTable rt, int experimentNameColumn){

        // Create a map to store the summary
        Map<String, double[]> summaryMap = new HashMap<>();
        Map<String, double[]> summaryPoints = new HashMap<>();
        int columnCount = rt.getLastColumn() + 1;

        // Iterate over the rows in the ResultsTable
        for (int i = 0; i < rt.getCounter(); i++) {
            String experimentName = rt.getStringValue(experimentNameColumn, i);
            double[] values = new double[columnCount];

            // Retrieve values for each column in the row
            for (int j = 0; j < columnCount; j++) {
                if(rt.getColumnHeading(j).equals("Cell ID")||rt.getColumnHeading(j).equals("Nucleus ID")){
                    values[j]=1;
                }else {
                    values[j] = rt.getValue(j, i);
                }

            }

            // Summarize the values
            if (summaryMap.containsKey(experimentName)) {
                double[] existingValues = summaryMap.get(experimentName);
                for (int j = 0; j < values.length; j++) {
                    existingValues[j] += values[j];
                }
            } else {
                summaryMap.put(experimentName, values.clone());
            }
        }

        // Display the summary
        summaryMap.remove("0");
        for (Map.Entry<String, double[]> entry : summaryMap.entrySet()) {
            summaryTable.incrementCounter();
            summaryTable.addValue(rt.getColumnHeading(experimentNameColumn), entry.getKey());
            for (int i = 0; i < entry.getValue().length; i++) {
                int index=rt.getColumnIndex("Cell ID");
                if(index<0) index= rt.getColumnIndex("Nucleus ID");
                double nbCells=entry.getValue()[index];
                if(i!=experimentNameColumn) {
                    if(rt.getColumnHeading(i).equals("Cell ID")){
                        summaryTable.addValue("Cell nr.", nbCells);
                    }else if(rt.getColumnHeading(i).equals("Nucleus ID")) {
                        summaryTable.addValue("Nuclei nr.", nbCells);
                    }else{
                        summaryTable.addValue(rt.getColumnHeading(i), entry.getValue()[i]/nbCells);
                    }
                }
            }
        }
    }




        public static void summarize(ResultsTable summaryTable, ResultsTable nucleiResultTable, int experimentNameColumn1, ResultsTable cellResultsTable, int experimentNameColumn2,boolean onlyPositiveSpot) {
            // Create a map to store the summary
            Map<String, double[]> summaryMap1 = new HashMap<>();
            Map<String, double[]> summaryMap2 = new HashMap<>();
            int columnCount1 = nucleiResultTable.getLastColumn() + 1;
            int columnCount2 = cellResultsTable.getLastColumn() + 1;

            // Iterate over the rows in the first ResultsTable
            for (int i = 0; i < nucleiResultTable.getCounter(); i++) {
                String experimentName = nucleiResultTable.getStringValue(experimentNameColumn1, i);
                double[] values = new double[columnCount1];

                // Retrieve values for each column in the row
                for (int j = 0; j < columnCount1; j++) {
                    if(nucleiResultTable.getColumnHeading(j).equals("Nucleus ID")){
                        values[j]=1;
                    }else {
                        values[j] = nucleiResultTable.getValue(j, i);
                    }

                }

                // Summarize the values
                if (summaryMap1.containsKey(experimentName)) {
                    double[] existingValues = summaryMap1.get(experimentName);
                    for (int j = 0; j < values.length; j++) {
                        existingValues[j] += values[j];
                    }
                } else {
                    summaryMap1.put(experimentName, values.clone());
                }
            }
            // Iterate over the rows in the second ResultsTable
            for (int i = 0; i < cellResultsTable.getCounter(); i++) {
                String experimentName = cellResultsTable.getStringValue(experimentNameColumn2, i);
                double[] values = new double[columnCount2];

                // Retrieve values for each column in the row
                for (int j = 0; j < columnCount2; j++) {
                    if(cellResultsTable.getColumnHeading(j).equals("Cell ID")){
                        values[j]=1;
                    }else {
                        values[j] = cellResultsTable.getValue(j, i);
                    }

                }

                // Summarize the values
                if (summaryMap2.containsKey(experimentName)) {
                    double[] existingValues = summaryMap2.get(experimentName);
                    for (int j = 0; j < values.length; j++) {
                        existingValues[j] += values[j];
                    }
                } else {
                    summaryMap2.put(experimentName, values.clone());
                }
            }

            // Display the summary
            summaryMap1.remove("0");
            summaryMap2.remove("0");
            for (Map.Entry<String, double[]> entry : summaryMap1.entrySet()) {
                summaryTable.incrementCounter();
                summaryTable.addValue(nucleiResultTable.getColumnHeading(experimentNameColumn1), entry.getKey());
                double[] entry2=summaryMap2.get(entry.getKey());
                for (int i = 0; i < entry.getValue().length; i++) {
                    int index=nucleiResultTable.getColumnIndex("Cell ID");
                    if(index<0) index= nucleiResultTable.getColumnIndex("Nucleus ID");
                    double nbCells=entry.getValue()[index];
                    if(i!=experimentNameColumn1) {
                        if(nucleiResultTable.getColumnHeading(i).equals("Cell ID")||nucleiResultTable.getColumnHeading(i).equals("Nucleus ID")) {
                            summaryTable.addValue(nucleiResultTable.getColumnHeading(i), nbCells);
                        }else{
                            summaryTable.addValue(nucleiResultTable.getColumnHeading(i), entry.getValue()[i]/nbCells);
                        }
                    }
                }
                for (int i = 0; i < entry2.length; i++) {
                    int index=cellResultsTable.getColumnIndex("Cell ID");
                    if(index<0) index= cellResultsTable.getColumnIndex("Nucleus ID");
                    double nbCells=entry2[index];
                    if(i!=experimentNameColumn2) {
                        if(cellResultsTable.getColumnHeading(i).equals("Cell ID")){
                            summaryTable.addValue("Cell nr.", nbCells);
                        }else if(cellResultsTable.getColumnHeading(i).equals("Nucleus ID")) {
                            summaryTable.addValue("Nuclei nr.", nbCells);
                        }else{
                            summaryTable.addValue(cellResultsTable.getColumnHeading(i), entry2[i]/nbCells);
                        }
                    }
                }
            }
        }
    }
