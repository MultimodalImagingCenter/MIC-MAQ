package fr.curie.micmaq.detectors;

import de.csbdresden.stardist.StarDist2D;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class StarDistLauncher {
    private final ImagePlus imagePlus;
    String model;
    boolean normalizeInput=true;
    double percentileBottom=0.0;
    double percentileTop=100.0;
    double probThresh=0.5;
    double nmsThresh=0.0;
    String outputType="Both";
    int nTiles=1;
    int excludeBoundary=2;
    String roiPosition="Automatic";
    boolean verbose=false;
    boolean showCsbdeepProgress=false;
    boolean showProbAndDist=false;
    boolean process=false;
    String modelFile=null;
    double scale=1.0;
    boolean excludeOnEdges=false;

    RoiManager stardistRoiManager;

    private ImagePlus stardistMask;


    public StarDistLauncher(ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
    }


    public void analysis(){

        ImagePlus scaledImg=imagePlus;
        if(scale!=1.0){
            scaledImg=imagePlus.resize((int)(imagePlus.getWidth()*scale), (int)(imagePlus.getHeight()*scale),"bicubic" );
            System.out.println("new size ("+scaledImg.getWidth()+ ", "+ scaledImg.getHeight()+")");
        }

        ImageJ ij=new ImageJ();
        //ImgPlus ip=new ImgPlus(ImageJFunctions.wrap(scaledImg));
       //ij.ui().show(ip);
        //System.out.println("stardist launcher ip:"+ip);
        //Dataset input = ij.imageDisplay().getActiveDataset();
        //Dataset input = ij.convert().convert(ip, Dataset.class);
        System.out.println("stardist exclude on edges: " + excludeOnEdges);
        Dataset input = null;
        try {
            String tempDir = IJ.getDirectory("Temp");
            File t_imp_path = new File(tempDir, imagePlus.getShortTitle() + ".tif");
            FileSaver fs = new FileSaver(scaledImg);
            fs.saveAsTiff(t_imp_path.getAbsolutePath());
            input = ij.scifio().datasetIO().open(t_imp_path.getAbsolutePath());

            System.out.println("stardist launcher input:"+input);
            System.out.println("stardist launcher scale:"+scale);
            System.out.flush();
            //ij.ui().show(input);
            HashMap<String, Object> params = new HashMap();
            params.put("input", input);
            params.put("modelChoice",model);
            params.put("normalizeInput",normalizeInput);
            params.put("percentileBottom",percentileBottom);
            params.put("percentileTop",percentileTop);
            params.put("probThresh",probThresh);
            params.put("nmsThresh",nmsThresh);
            params.put("outputType",outputType);
            if(modelFile!=null) params.put("modelFile",modelFile);
            params.put("nTiles",nTiles);
            params.put("excludeBoundary",excludeBoundary);
            params.put("roiPosition",roiPosition);
            params.put("verbose",verbose);
            params.put("showCsbdeepProgress",showCsbdeepProgress);
            params.put("showProbAndDist",showProbAndDist);

            stardistRoiManager = RoiManager.getRoiManager();
            stardistRoiManager.reset();

            ij.command().run(StarDist2D.class, true, params).get();

            stardistRoiManager = RoiManager.getRoiManager();
            System.out.println("nb rois: "+stardistRoiManager.getCount());
            if(scale!=1.0){
                double iscale=1/scale;
                stardistRoiManager.scale(iscale,iscale,false);
            }
            validateRoi();
            System.out.println("nb rois after validation: "+stardistRoiManager.getCount());


            stardistMask = Detector.labeledImage(imagePlus.getWidth(), imagePlus.getHeight(), stardistRoiManager.getRoisAsArray());
            stardistMask.setTitle(imagePlus.getShortTitle() + "-stardist");

            t_imp_path.delete();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void validateRoi(){
        if(!excludeOnEdges) return;
        ArrayList<Roi> validated=new ArrayList<>();
        for (int i=0;i<stardistRoiManager.getCount();i++){
            Roi roi=stardistRoiManager.getRoi(i);
            if (excludeOnEdges) {
                Rectangle r = roi.getBounds();
                if (r.x <= 1 || r.y <= 1 || r.x + r.width >= imagePlus.getWidth() - 1 || r.y + r.height >= imagePlus.getHeight() - 1) {
                    continue;
                }
            }
            validated.add(roi);
        }
        stardistRoiManager.reset();
        for (Roi r:validated){
            stardistRoiManager.addRoi(r);
        }
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isNormalizeInput() {
        return normalizeInput;
    }

    public void setNormalizeInput(boolean normalizeInput) {
        this.normalizeInput = normalizeInput;
    }

    public double getPercentileBottom() {
        return percentileBottom;
    }

    public void setPercentileBottom(double percentileBottom) {
        this.percentileBottom = percentileBottom;
    }

    public double getPercentileTop() {
        return percentileTop;
    }

    public void setPercentileTop(double percentileTop) {
        this.percentileTop = percentileTop;
    }

    public double getProbThresh() {
        return probThresh;
    }

    public void setProbThresh(double probThresh) {
        this.probThresh = probThresh;
    }

    public double getNmsThresh() {
        return nmsThresh;
    }

    public void setNmsThresh(double nmsThresh) {
        this.nmsThresh = nmsThresh;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public int getnTiles() {
        return nTiles;
    }

    public void setnTiles(int nTiles) {
        this.nTiles = nTiles;
    }

    public int getExcludeBoundary() {
        return excludeBoundary;
    }

    public void setExcludeBoundary(int excludeBoundary) {
        this.excludeBoundary = excludeBoundary;
    }

    public String getRoiPosition() {
        return roiPosition;
    }

    public void setRoiPosition(String roiPosition) {
        this.roiPosition = roiPosition;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isShowCsbdeepProgress() {
        return showCsbdeepProgress;
    }

    public void setShowCsbdeepProgress(boolean showCsbdeepProgress) {
        this.showCsbdeepProgress = showCsbdeepProgress;
    }

    public boolean isShowProbAndDist() {
        return showProbAndDist;
    }

    public void setShowProbAndDist(boolean showProbAndDist) {
        this.showProbAndDist = showProbAndDist;
    }

    public boolean isProcess() {
        return process;
    }

    public void setProcess(boolean process) {
        this.process = process;
    }

    public String getModelFile() {
        return modelFile;
    }

    public void setModelFile(String modelFile) {
        this.modelFile = modelFile;
    }

    public RoiManager getStardistRoiManager() {
        return stardistRoiManager;
    }

    public ImagePlus getInstanceMask() {
        return stardistMask;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public boolean isExcludeOnEdges() {
        return excludeOnEdges;
    }

    public void setExcludeOnEdges(boolean excludeOnEdges) {
        this.excludeOnEdges = excludeOnEdges;
    }
}
