package fr.curie.micmaq.helpers;

import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.gui.GenericDialog;

import java.util.ArrayList;
import java.util.Arrays;

public class SummaryResultsWithThreshold implements PlugIn {

    private static final String PREF_IMAGE_NAME = "positivecellcounter.imageName";
    private static final String PREF_COLUMN_TO_THRESHOLD = "positivecellcounter.columnToThreshold";
    private static final String PREF_THRESHOLD = "positivecellcounter.threshold";


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
            GenericDialog gd = new GenericDialog("Count Positive Cells");
            gd.addChoice("Select Table", tableTitles, titleOri);
            gd.showDialog();
            if (gd.wasCanceled()) return;

            // Get the user's selected table
            titleOri = gd.getNextChoice();

        }

        ResultsTable resultsTable = ResultsTable.getResultsTable(titleOri);


        // Get column headings
        String[] headings = resultsTable.getHeadings();
        if (headings.length == 0) {
            IJ.error("No headings found in the Results Table.");
            return;
        }

        // Load preferences (or use defaults if not set)
        String defaultImageName = Prefs.get(PREF_IMAGE_NAME, headings[0]); // Default to 1rst column
        String defaultColToThreshold = Prefs.get(PREF_COLUMN_TO_THRESHOLD, headings[2]); // Default to 3rd column
        double defaultThreshold = Prefs.get(PREF_THRESHOLD, 5000); // Default threshold


        // Show the dialog to configure inputs
        GenericDialog gd = new GenericDialog("Count Positive Cells");
        gd.addChoice("Name of images", headings, defaultImageName);
        gd.addChoice("Column to threshold", headings, defaultColToThreshold);
        gd.addNumericField("Threshold", defaultThreshold, 0);
        gd.showDialog();
        if (gd.wasCanceled()) return;

        // Extract user input from dialog
        String name = gd.getNextChoice();
        String colT = gd.getNextChoice();
        double threshold = gd.getNextNumber();

        // Save preferences for future use
        Prefs.set(PREF_IMAGE_NAME, name);
        Prefs.set(PREF_COLUMN_TO_THRESHOLD, colT);
        Prefs.set(PREF_THRESHOLD, threshold);


        // Match choices with index
        int indexName = -1;
        int indexToThreshold = -1;
        for (int col = 0; col < headings.length; col++) {
            if (headings[col].equals(name)) indexName = col;
            if (headings[col].equals(colT)) indexToThreshold = col;
        }


        int nrows = resultsTable.size();
        int count = 0;
        int positive = 0;
        int rowIndex = 0;
        String title = resultsTable.getStringValue(indexName, 0);

        // Create a new ResultsTable for counting results
        ResultsTable countingTable = new ResultsTable();

        for (int i = 0; i < nrows; i++) {
            String v = resultsTable.getStringValue(indexName, i);

            if (v.startsWith(title) && v.endsWith(title)) {
                count++;
                if (resultsTable.getValue(indexToThreshold, i) >= threshold) positive++;
            } else {
                // Save the current block's results
                countingTable.setValue(headings[indexName], rowIndex, title);
                countingTable.setValue("Number of cells", rowIndex, count);
                countingTable.setValue(
                        "Number of positive (>= " + threshold + ") for " + headings[indexToThreshold],
                        rowIndex,
                        positive
                );

                // Reset counters
                count = 1;
                positive = 0;
                title = v;
                if (resultsTable.getValue(indexToThreshold, i) > threshold) positive++;
                rowIndex++;
            }

            // Handle last row
            if (i == nrows - 1) {
                countingTable.setValue(headings[indexName], rowIndex, title);
                countingTable.setValue("Number of cells", rowIndex, count);
                countingTable.setValue(
                        "Number of positive (>= " + threshold + ") for " + headings[indexToThreshold],
                        rowIndex,
                        positive
                );
            }
        }

        // Show the new ResultsTable
        countingTable.show("Counting positives of " + titleOri);
    }
}

