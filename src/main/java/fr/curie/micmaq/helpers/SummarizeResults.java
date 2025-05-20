package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SummarizeResults implements PlugIn {
    private static final String PREF_IMAGE_NAME = "summarize.imageName";
    @Override
    public void run(String arg) {

        // Fetch open table titles
        String[] openTables = WindowManager.getNonImageTitles(); // Gets all open non-image windows
        ArrayList<String> resultsTableTitles = new ArrayList<>();

        // Collect results table names
        for (String title : openTables) {
            if (ResultsTable.getResultsTable(title) != null) {
                resultsTableTitles.add(title);
            }
        }

        // If no tables are open, exit
        if (resultsTableTitles.isEmpty()) {
            IJ.error("No Results Tables are currently open!");
            return;
        }

        // Convert list of table titles to array
        String[] tableTitles = resultsTableTitles.toArray(new String[0]);


        String titleOri =  tableTitles[0];
        if(tableTitles.length>1){
            // Show a dialog to select the table and configure inputs
            GenericDialog gd = new GenericDialog("choose result window");
            gd.addChoice("Select Table", tableTitles, titleOri);
            gd.showDialog();
            if (gd.wasCanceled()) return;

            // Get the user's selected table
            titleOri = gd.getNextChoice();

        }

        ResultsTable rt = ResultsTable.getResultsTable(titleOri);
        // Get column headings
        String[] headings = rt.getHeadings();
        if (headings.length == 0) {
            IJ.error("No headings found in the Results Table.");
            return;
        }

        // Load preferences (or use defaults if not set)
        String defaultImageName = Prefs.get(PREF_IMAGE_NAME, headings[0]); // Default to 1rst column

        // Show the dialog to configure inputs
        GenericDialog gd = new GenericDialog("Summarize Results");
        gd.addChoice("Name of images", headings, defaultImageName);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        // Extract user input from dialog
        String experimentNameColumnName = gd.getNextChoice();

        // Save preferences for future use
        Prefs.set(PREF_IMAGE_NAME, experimentNameColumnName);



        // Check if the "experiment name" column exists
        int experimentNameColumn = rt.getColumnIndex(experimentNameColumnName);
        if (experimentNameColumn == -1) {
            IJ.error("Column Not Found", "The 'experiment name' column was not found in the Results Table.");
            return;
        }

        ResultsTable summaryTable = new ResultsTable();
        summarize(summaryTable,rt,experimentNameColumn);
        summaryTable.show("Summary of "+titleOri);
    }

    public static void summarize(ResultsTable summaryTable, ResultsTable rt, int experimentNameColumn){

        // Create a map to store the summary
        Map<String, double[]> summaryMap = new HashMap<>();
        Map<String, double[]> summaryPoints = new HashMap<>();
        int columnCount = rt.getLastColumn() + 1;
        ArrayList<String> names=new ArrayList<>();

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
                if(!experimentName.equals("0")) names.add(experimentName);
            }
        }

        // Display the summary
        summaryMap.remove("0");
        //for (Map.Entry<String, double[]> entry : summaryMap.entrySet()) {
        for(String name : names){
            double[] entry=summaryMap.get(name);
            summaryTable.incrementCounter();
            summaryTable.addValue(rt.getColumnHeading(experimentNameColumn), name);
            for (int i = 0; i < entry.length; i++) {
                int index=rt.getColumnIndex("Cell ID");
                if(index<0) index= rt.getColumnIndex("Nucleus ID");
                double nbCells=entry[index];
                if(i!=experimentNameColumn) {
                    if(rt.getColumnHeading(i).equals("Cell ID")){
                        summaryTable.addValue("Cell nr.", nbCells);
                    }else if(rt.getColumnHeading(i).equals("Nucleus ID")) {
                        summaryTable.addValue("Nuclei nr.", nbCells);
                    }else{
                        summaryTable.addValue(rt.getColumnHeading(i), entry[i]/nbCells);
                    }
                }
            }
        }
    }




        public static void summarize(ResultsTable summaryTable, ResultsTable nucleiResultTable, int experimentNameColumn1, ResultsTable cellResultsTable, int experimentNameColumn2,boolean onlyPositiveSpot) {
            // Create a map to store the summary
            IJ.log("summary only positives spot:"+onlyPositiveSpot);
            Map<String, double[]> summaryMap1 = new HashMap<>();
            Map<String, double[]> summaryMap2 = new HashMap<>();
            int columnCount1 = nucleiResultTable.getLastColumn() + 1;
            int columnCount2 = cellResultsTable.getLastColumn() + 1;
            boolean maxima=false;
            boolean thresh=false;
            ArrayList<String> names=new ArrayList<>();

            // Iterate over the rows in the first ResultsTable
            for (int i = 0; i < nucleiResultTable.getCounter(); i++) {
                String experimentName = nucleiResultTable.getStringValue(experimentNameColumn1, i);
                double[] values = new double[columnCount1+2];

                // Retrieve values for each column in the row
                for (int j = 0; j < columnCount1; j++) {
                    if(nucleiResultTable.getColumnHeading(j).equals("Nucleus ID")){
                        values[j]=1;
                    }else if(nucleiResultTable.getColumnHeading(j).endsWith("maxima nr. spots")){
                        double val=nucleiResultTable.getValue(j, i);
                        if(val>0) values[columnCount1]++;
                        values[j] = val;
                        maxima=true;
                    }else if(nucleiResultTable.getColumnHeading(j).endsWith("threshold nr. spots")){
                        double val=nucleiResultTable.getValue(j, i);
                        if(val>0) values[columnCount1+1]++;
                        values[j] = val;
                        thresh=true;
                    }else {
                        double val=nucleiResultTable.getValue(j, i);
                        if(Double.isNaN(val)) {
                            values[j] = 0;
                        }else{
                            values[j] = val;
                        }
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
                    if(! experimentName.equals("0")) names.add(experimentName);
                }
            }
            // Iterate over the rows in the second ResultsTable
            for (int i = 0; i < cellResultsTable.getCounter(); i++) {
                String experimentName = cellResultsTable.getStringValue(experimentNameColumn2, i);
                double[] values = new double[columnCount2+2];

                // Retrieve values for each column in the row
                for (int j = 0; j < columnCount2; j++) {
                    if(cellResultsTable.getColumnHeading(j).equals("Cell ID")){
                        values[j]=1;
                    }else if(cellResultsTable.getColumnHeading(j).startsWith("Cell")&& cellResultsTable.getColumnHeading(j).endsWith("maxima nr. spots")){
                        double val=cellResultsTable.getValue(j, i);
                        if(val>0) values[columnCount2]++;
                        values[j] = val;
                    }else if(cellResultsTable.getColumnHeading(j).startsWith("Cell")&&cellResultsTable.getColumnHeading(j).endsWith("threshold nr. spots")){
                        double val=cellResultsTable.getValue(j, i);
                        if(val>0) values[columnCount2+1]++;
                        values[j] = val;
                    }else {
                        double val=cellResultsTable.getValue(j, i);
                        if(Double.isNaN(val)) {
                            values[j] = 0;
                        }else{
                            values[j] = val;
                        }
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
            //for (Map.Entry<String, double[]> entry : summaryMap1.entrySet()) {
            for(String name : names){
                double[] entry=summaryMap1.get(name);
                summaryTable.incrementCounter();
                summaryTable.addValue(nucleiResultTable.getColumnHeading(experimentNameColumn1), name);
                System.out.println(name);
                System.out.println("positive Nuclei count maxima: "+entry[columnCount1]);
                System.out.println("positive Nuclei count threshold: "+entry[columnCount1+1]);
                double[] entry2=summaryMap2.get(name);
                for (int i = 0; i < entry.length-2; i++) {
                    int index=nucleiResultTable.getColumnIndex("Nucleus ID");
                    if(index<0) index= nucleiResultTable.getColumnIndex("Cell ID");
                    double nbCells=entry[index];
                    if(i!=experimentNameColumn1) {
                        if(nucleiResultTable.getColumnHeading(i).equals("Nucleus ID")) {
                            summaryTable.addValue("Nuclei nr.", nbCells);
                            if(onlyPositiveSpot&&maxima) summaryTable.addValue("Nuclei positive maxima spots nr.", entry[columnCount1]);
                            if(onlyPositiveSpot&&thresh) summaryTable.addValue("Nuclei positive threshold spots nr.", entry[columnCount1+1]);
                        }else{
                            if(nucleiResultTable.getColumnHeading(i).contains("Cell ID")){

                            }else {
                                double nbCellstmp = nbCells;

                                summaryTable.addValue(nucleiResultTable.getColumnHeading(i), entry[i] / nbCellstmp);
                                if (nucleiResultTable.getColumnHeading(i).contains("threshold") && entry[columnCount1+1] > 0) {
                                    nbCellstmp = entry[columnCount1+1];
                                    summaryTable.addValue(nucleiResultTable.getColumnHeading(i) +"(only positive)", entry[i] / nbCellstmp);
                                } else if (nucleiResultTable.getColumnHeading(i).contains("max")&& !nucleiResultTable.getColumnHeading(i).contains("prominence")  && entry[columnCount1] > 0) {
                                    nbCellstmp = entry[columnCount1];
                                    summaryTable.addValue(nucleiResultTable.getColumnHeading(i)+"(only positive)", entry[i] / nbCellstmp);
                                }
                            }
                        }
                    }
                }


                System.out.println(name);
                System.out.println("positive Cell count maxima: "+entry2[columnCount2]);
                System.out.println("positive Cell count threshold: "+entry2[columnCount2+1]);
                for (int i = 0; i < entry2.length-2; i++) {
                    int index=cellResultsTable.getColumnIndex("Cell ID");
                    if(index<0) index= cellResultsTable.getColumnIndex("Nucleus ID");
                    double nbCells=entry2[index];
                    if(i!=experimentNameColumn2) {
                        if(cellResultsTable.getColumnHeading(i).equals("Cell ID")) {
                            summaryTable.addValue("Cell nr.", nbCells);
                            if(onlyPositiveSpot&&maxima) summaryTable.addValue("Cell positive maxima spots nr.", entry2[columnCount2]);
                            if(onlyPositiveSpot&&thresh) summaryTable.addValue("Cell positive threshold spots nr.", entry2[columnCount2+1]);
                        }else{
                            if(cellResultsTable.getColumnHeading(i).contains("Nuclei ID")){

                            }else {
                                double nbCellstmp = nbCells;
                                summaryTable.addValue(cellResultsTable.getColumnHeading(i), entry2[i] / nbCellstmp);
                                if (cellResultsTable.getColumnHeading(i).contains("threshold") && entry2[columnCount2+1] > 0) {
                                    nbCellstmp = entry2[columnCount2+1];
                                    summaryTable.addValue(cellResultsTable.getColumnHeading(i) +"(only positive)", entry2[i] / nbCellstmp);
                                } else if (cellResultsTable.getColumnHeading(i).contains("max")&& !cellResultsTable.getColumnHeading(i).contains("prominence")  && entry2[columnCount2] > 0) {
                                    nbCellstmp = entry2[columnCount2];
                                    summaryTable.addValue(cellResultsTable.getColumnHeading(i)+"(only positive)", entry2[i] / nbCellstmp);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
