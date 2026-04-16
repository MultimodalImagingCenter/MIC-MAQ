package fr.curie.micmaq.segment;

import fr.curie.micmaq.detectors.CellposeLauncher;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * ImageJ plugin to run Cellpose on HyperStacks (Z/T and optionally multi-channel).
 * It iterates through timepoints and z-slices, runs Cellpose per 2D plane or
 * per 3D stack when Z>1, and aggregates the resulting masks into a labeled
 * hyperstack matching the input dimensions. Tiling is supported via
 * CellposeLauncher.tileSize and tileOverlap.
 */
public class CellposeLauncherHyperStack implements PlugInFilter {

    ImagePlus imp;

    // Parameters with sensible defaults and linked to existing MIC-MAQ conventions
    int diameter = 50;
    double cellprob = 0.0;
    String model = "cyto2";
    boolean useNucleiChannel = false;
    int cytoChannel = 1;   // 1-based for CellposeTaskSettings
    int nucleiChannel = 0; // 0 means none
    boolean excludeOnEdges = false;

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        if (imp == null) {
            IJ.error("No image open");
            return DONE;
        }
        if (!askParameters()) return DONE;
        return DOES_ALL;
    }

    boolean askParameters() {
        GenericDialog gd = new GenericDialog("Cellpose on HyperStack");
        gd.addStringField("model", model);
        gd.addNumericField("diameter", diameter, 0);
        gd.addNumericField("cellprob_threshold", cellprob, 2);
        gd.addCheckbox("use_nuclei_channel", useNucleiChannel);
        gd.addNumericField("cyto_channel (1-3, 0=gray)", cytoChannel, 0);
        gd.addNumericField("nuclei_channel (0=None,1-3)", nucleiChannel, 0);
        gd.addCheckbox("exclude_on_edges", excludeOnEdges);
        gd.addMessage("Tiling (set in MIC-MAQ > Preferences)\nTile size=" + CellposeLauncher.tileSize + ", overlap=" + CellposeLauncher.tileOverlap);
        gd.showDialog();
        if (gd.wasCanceled()) return false;
        model = gd.getNextString();
        diameter = (int) gd.getNextNumber();
        cellprob = gd.getNextNumber();
        useNucleiChannel = gd.getNextBoolean();
        cytoChannel = (int) gd.getNextNumber();
        nucleiChannel = (int) gd.getNextNumber();
        excludeOnEdges = gd.getNextBoolean();
        if (!useNucleiChannel) nucleiChannel = 0;
        return true;
    }

    @Override
    public void run(ImageProcessor ip) {
        // Build an output stack with same X,Y,Z,T as input, single channel labeled mask
        int w = imp.getWidth();
        int h = imp.getHeight();
        int nC = Math.max(1, imp.getNChannels());
        int nZ = Math.max(1, imp.getNSlices());
        int nT = Math.max(1, imp.getNFrames());

        ImageStack outStack = new ImageStack(w, h);

        // Iterate over T; for each T, either run in 3D over Z or per-slice when Z=1
        for (int t = 1; t <= nT; t++) {
            IJ.showStatus("Cellpose HyperStack - frame " + t + "/" + nT);
            if (nZ > 1) {
                // Build a stack for current timepoint with the channels expected by Cellpose
                ImagePlus inputForCP;
                if (nC > 1 && (cytoChannel > 0 || nucleiChannel > 0)) {
                    // Extract channels as 2 stacks and merge into composite
                    ImagePlus c1 = extractChannelAtTime(imp, cytoChannel, t);
                    ImagePlus c2 = extractChannelAtTime(imp, nucleiChannel, t);
                    inputForCP = RGBStackMerge.mergeChannels(new ImagePlus[]{c1, c2}, true);
                } else {
                    inputForCP = extractChannelAtTime(imp, cytoChannel == 0 ? 1 : cytoChannel, t);
                }
                CellposeLauncher cpl = new CellposeLauncher(inputForCP, diameter, cellprob, model,
                        cytoChannel, nucleiChannel, excludeOnEdges);
                ImagePlus mask = cpl.runCellpose();
                for (int z = 1; z <= nZ; z++) {
                    mask.setZ(z);
                    outStack.addSlice(mask.getProcessor().duplicate());
                }
            } else {
                // 2D per-timepoint
                ImagePlus inputForCP;
                if (nC > 1 && (cytoChannel > 0 || nucleiChannel > 0)) {
                    ImagePlus c1 = extractChannelSliceAtTime(imp, cytoChannel, 1, t);
                    ImagePlus c2 = extractChannelSliceAtTime(imp, nucleiChannel, 1, t);
                    inputForCP = RGBStackMerge.mergeChannels(new ImagePlus[]{c1, c2}, true);
                } else {
                    inputForCP = extractChannelSliceAtTime(imp, cytoChannel == 0 ? 1 : cytoChannel, 1, t);
                }
                CellposeLauncher cpl = new CellposeLauncher(inputForCP, diameter, cellprob, model,
                        cytoChannel, nucleiChannel, excludeOnEdges);
                ImagePlus mask = cpl.runCellpose();
                outStack.addSlice(mask.getProcessor().duplicate());
            }
            IJ.showProgress(t, nT);
        }

        ImagePlus out = new ImagePlus(imp.getShortTitle() + "-cellpose-hyperstack", outStack);
        out.setDimensions(1, nZ, nT);
        out.show();
    }

    // Helpers to extract channels for a given timepoint
    private ImagePlus extractChannelAtTime(ImagePlus hs, int ch, int t) {
        if (ch <= 0) {
            // Return a grayscale stack built from the current display channel
            return extractChannelAtTime(hs, 1, t);
        }
        ImageStack st = new ImageStack(hs.getWidth(), hs.getHeight());
        for (int z = 1; z <= hs.getNSlices(); z++) {
            int index = hs.getStackIndex(ch, z, t);
            st.addSlice(hs.getStack().getProcessor(index).duplicate());
        }
        return new ImagePlus("c" + ch + "_t" + t, st);
    }

    private ImagePlus extractChannelSliceAtTime(ImagePlus hs, int ch, int z, int t) {
        if (ch <= 0) return extractChannelSliceAtTime(hs, 1, z, t);
        int index = hs.getStackIndex(ch, z, t);
        ImageProcessor ip2 = hs.getStack().getProcessor(index).duplicate();
        return new ImagePlus("c" + ch + "_z" + z + "_t" + t, ip2);
    }
}
