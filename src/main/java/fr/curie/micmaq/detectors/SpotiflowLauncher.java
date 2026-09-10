package fr.curie.micmaq.detectors;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ch.epfl.biop.wrappers.spotiflow.DefaultSpotiflowTask;
import ch.epfl.biop.wrappers.spotiflow.SpotiflowTaskSettings;
import ij.gui.PointRoi;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.List;


public class SpotiflowLauncher {
    public static final String PROP_INTENSITY = "intensity";
    public static final String PROP_PROBABILITY = "probability";
    private static final Set<String> PRETRAINED_MODELS = new HashSet<>(Arrays.asList("general", "hybiss", "synth_complex", "fluo_live", "synth_3d", "smfish_3d"));
    private static final Set<String> PEAK_FINDERS = new HashSet<>(Arrays.asList("fast", "skimage"));

    String pretrainedModel = "general";
    File modelDir = null;
    boolean is3D = false;
    Double probabilityThreshold = null;
    Integer nTilesY = null;
    Integer nTilesX = null;
    Integer minDistance = 0;
    Double excludeBorder = null;
    Double scale = 1.0;
    String subpix = "True";
    Integer subpixRadius = 0;
    String peakFinder = null;
    String normalizer = null;
    String zarrComponent = null;
    ImagePlus workingImage;
    String envdir;
    private File tempDir;
    private File inputFile;
    private File outputFile;
    private double minProbability = 0;

    public SpotiflowLauncher(ImagePlus imagePlus) {
        this.workingImage = imagePlus;
        this.envdir = Prefs.get("micmaq.spotiflow.envdir", "C:/Users/miniconda3/envs/spotiflow");
    }

    public void setPretrainedModel(String model) {
        if (model == null || model.trim().isEmpty()) return;
        model = model.trim();
        if (!PRETRAINED_MODELS.contains(model)) throw new IllegalArgumentException("Unknown Spotiflow pretrained model: " + model);
        this.pretrainedModel = model;
        this.modelDir = null;
    }

    public void setModelPath(String modelPath) {
        this.modelDir = new File(modelPath);
        if (!modelDir.exists()){
            modelDir = null;
            return;
        }
        this.pretrainedModel = null;
    }

    public void setProbabilityThreshold(double threshold) {
        this.probabilityThreshold = threshold;
        this.minProbability = threshold;
    }

    public void setNTiles(int nTilesY, int nTilesX) {
        this.nTilesY = nTilesY;
        this.nTilesX = nTilesX;
    }

    public void setMinDistance(int minDistance) {
        this.minDistance = minDistance;
    }

    public void setExcludeBorder(double excludeBorder) {
        this.excludeBorder = excludeBorder;
    }

    public void setScale(double scale) {
        this.scale = scale;
        if(workingImage.getImageStackSize()>1) {
            this.scale=1.0;
            System.err.println("Image stack size > 1, scale is not compatible with 3D, scale set to 1");
            IJ.log("Image stack size > 1, scale is not compatible with 3D, scale set to 1");
        }
        if(this.scale!=1) subpix="False";
    }

    public void setSubpix(String subpix) {
        this.subpix = subpix;
    }

    public void setSubpixRadius(int subpixRadius) {
        if(this.scale==1) this.subpix="True";
        this.subpixRadius = subpixRadius;
    }

    public void setPeakFinder(String peakFinder) {
        if (peakFinder == null || peakFinder.trim().isEmpty()) return;
        peakFinder = peakFinder.trim();
        if (!PEAK_FINDERS.contains(peakFinder)) throw new IllegalArgumentException("Unknown Spotiflow peak finder: " + peakFinder);
        this.peakFinder = peakFinder;
    }

    public void setNormalizer(String normalizer) {
        this.normalizer = normalizer;
    }

    public void setZarrComponent(String zarrComponent) {
        this.zarrComponent = zarrComponent;
    }

    public void setMinProbability(double minProbability) {
        setProbabilityThreshold(minProbability);
    }

    private String buildAdditionalFlags() {
        ArrayList<String> flags = new ArrayList<>();
        flags.add("-v");

        if (modelDir != null) {
            flags.add("-md");
            flags.add(modelDir.getAbsolutePath());
        } else if (pretrainedModel != null) {
            flags.add("-pm");
            flags.add(pretrainedModel);
        }

        if (probabilityThreshold != null) {
            flags.add("-t");
            flags.add(Double.toString(probabilityThreshold));
        }

        if (nTilesY != null && nTilesX != null) {
            flags.add("-n");
            flags.add(Integer.toString(nTilesY));
            flags.add(Integer.toString(nTilesX));
        }

        if (minDistance != null) {
            flags.add("-min");
            flags.add(Integer.toString(minDistance));
        }

        if (excludeBorder != null) {
            flags.add("-eb");
            flags.add(Double.toString(excludeBorder));
        }

        if (scale != null) {
            flags.add("-s");
            flags.add(Double.toString(scale));
        }

        if (subpix != null && !subpix.trim().isEmpty()) {
            flags.add("-sp");
            flags.add(subpix.trim());
        }

        if (subpixRadius != null) {
            flags.add("-spr");
            flags.add(Integer.toString(subpixRadius));
        }

        if (peakFinder != null) {
            flags.add("-p");
            flags.add(peakFinder);
        }

        if (normalizer != null && !normalizer.trim().isEmpty()) {
            flags.add("-norm");
            flags.add(normalizer.trim());
        }

        if (zarrComponent != null && !zarrComponent.trim().isEmpty()) {
            flags.add("-zc");
            flags.add(zarrComponent.trim());
        }

        return String.join(",", flags);
    }



    public File runSpotiflow() throws Exception {
        createTemporaryFiles();
        IJ.log("spotiflow Env path = " + envdir);
        System.out.println("Dataset dir = " + tempDir);
        System.out.println("Input file = " + inputFile);
        System.out.println("Output file = " + outputFile);
        System.out.println("Pretrained model = " + pretrainedModel);
        System.out.println("Model dir = " + modelDir);
        System.out.println("Probability threshold = " + probabilityThreshold);

        SpotiflowTaskSettings settings = new SpotiflowTaskSettings();
        settings.setEnvPath(envdir);
        settings.setEnvType("conda");
        settings.setDatasetDir(tempDir.getAbsolutePath());

        String flags = buildAdditionalFlags();
        IJ.log("Spotiflow flags = " + flags);
        settings.setAdditionalFlags(flags);

        DefaultSpotiflowTask task = new DefaultSpotiflowTask();
        task.setSettings(settings);
        task.run();
        //IJ.log("Spotiflow finished.");
        System.out.println("CSV trouvé = " + outputFile.exists());
        return outputFile;
    }

    public String getOptions() throws Exception{
        createTemporaryFiles();
        SpotiflowTaskSettings settings = new SpotiflowTaskSettings();
        settings.setEnvPath(envdir);
        settings.setEnvType("conda");
        settings.setDatasetDir(tempDir.getAbsolutePath());

        String flags = "--options";
        settings.setAdditionalFlags(flags);
        DefaultSpotiflowTask task = new DefaultSpotiflowTask();
        task.setSettings(settings);
        task.run();
        return "";
    }

    private void createTemporaryFiles() throws Exception {
        tempDir = Files.createTempDirectory("spotiflowTemp").toFile();
        File[] contents = tempDir.listFiles();
        if (contents != null) {
            for (File f : contents) {
                f.delete();
            }
        }
        inputFile = new File(tempDir,workingImage.getShortTitle() + "-t1.tif");
        outputFile = new File(tempDir,workingImage.getShortTitle() + "-t1.csv");

        new FileSaver(workingImage).saveAsTiff(inputFile.getAbsolutePath());
    }

    public void cleanup() {
        if (inputFile != null && inputFile.exists())
            inputFile.delete();

        if (outputFile != null && outputFile.exists())
            outputFile.delete();

        if (tempDir != null && tempDir.exists())
            tempDir.delete();
    }

    public ArrayList<PointRoi> loadPoints(File csvFile) throws Exception {

        ArrayList<PointRoi> rois = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {

            boolean firstLine = true;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (firstLine) {
                    firstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 4)
                    continue;
                int offset = (parts.length>4) ? 1: 0;
                is3D = (parts.length>4);

                double z = (is3D) ? Double.parseDouble(parts[0].trim()) : 0;
                double y = Double.parseDouble(parts[0+offset].trim());
                double x = Double.parseDouble(parts[1+offset].trim());
                double intensity = Double.parseDouble(parts[2+offset].trim());
                double probability = Double.parseDouble(parts[3+offset].trim());
                if (probability < minProbability) continue;

                PointRoi roi = new PointRoi(x, y);
                if(is3D) roi.setPosition((int)Math.round(z)+1);
                roi.setProperty(PROP_INTENSITY,Double.toString(intensity));
                roi.setProperty(PROP_PROBABILITY,Double.toString(probability));
                roi.setName("Spot_x"+x+"_y"+y+"_z"+z+"_prob"+probability+"_int"+intensity);

                rois.add(roi);
            }
        }
        //IJ.log("spotiflow nb rois found :"+rois.size());
        return rois;
    }

    public static double getIntensity(PointRoi roi) {
        return Double.parseDouble(
                roi.getProperty(PROP_INTENSITY));
    }

    public static double getProbability(PointRoi roi) {
        return Double.parseDouble(
                roi.getProperty(PROP_PROBABILITY));
    }

    public static PointRoi getGlobalPointRoi( ArrayList<PointRoi> rois, double probabilityThreshold) {
        PointRoi roi = null;
        for (PointRoi r : rois) {
            if(getProbability(r)>=probabilityThreshold) {
                if (roi == null) {
                    roi = new PointRoi(r.getFloatPolygon().xpoints[0], r.getFloatPolygon().ypoints[0]);

                } else {
                    roi.addPoint(r.getFloatPolygon().xpoints[0], r.getFloatPolygon().ypoints[0]);
                }
            }
        }
        return roi;
    }





    private void printCsv(File csv) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String line;

            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
    }



    public static void main(String[] args) {
        try {
            Class.forName(
                    "ch.epfl.biop.wrappers.spotiflow.SpotiflowTaskSettings");
            System.out.println("OK");
        } catch (Throwable t) {
            t.printStackTrace();
        }

        System.out.println(
                SpotiflowTaskSettings.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
        );

        boolean is3D = true;
        String imageName = is3D ? "ForSpotiflow3D.tif":"ForSpotiflow.tif";
        String model = is3D ? "smfish_3d":"general";
        //String modelPath = "C:/Users/cedric/.spotiflow/models/smfish_3d";
        String imgPath = (true)? "C:/Users/cedric/Desktop/tests/DL/"+imageName :  "C:/Users/cedric/Desktop/tests/test MIC-MAQ/dir_tif/mock+HU_020_w2FITC.TIF";
        ImagePlus imp = IJ.openImage(imgPath);
        imp.setHideOverlay(true);
        imp.deleteRoi();

        if (imp == null) {
            System.err.println("Impossible d'ouvrir l'image");
            return;
        }

        imp.show();

        try {
            SpotiflowLauncher launcher = new SpotiflowLauncher(imp);
            launcher.envdir="C:/Users/cedric/miniconda3/envs/spotiflow";
            launcher.setPretrainedModel(model);
            //launcher.setModelPath(modelPath);
            launcher.setMinProbability(0.5);
            //launcher.setScale(0.5);
            //launcher.setSubpixRadius(10);
            //launcher.setSubpix("False");
            //launcher.setMinDistance(10);
            //launcher.getOptions();

            File csv = launcher.runSpotiflow();

            System.out.println("CSV attendu :");
            System.out.println(csv.getAbsolutePath());

            System.out.println("Existe : " + csv.exists());

            if (csv.exists()) {
                System.out.println("Taille : " + csv.length() + " bytes");
                //launcher.printCsv(csv);
                ArrayList<PointRoi> rois = launcher.loadPoints(csv);
                System.out.println("Nombre de ROIs : " + rois.size());
                RoiManager roiManager = RoiManager.getRoiManager();
                for (PointRoi roi : rois) {
                    //System.out.println(roi.getBounds());
                    //System.out.println(getIntensity(roi));
                    //System.out.println(getProbability(roi));
                    roiManager.addRoi(roi);
                    roiManager.selectAndMakeVisible(imp,roiManager.getCount()-1);
                }
                //IJ.showMessage("wait for you to check");
            }
            launcher.cleanup();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}


