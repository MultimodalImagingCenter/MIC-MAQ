package fr.curie.micmaq.config;

import fr.curie.micmaq.detectors.*;
import fr.curie.micmaq.helpers.MeasureCalibration;
import fr.curie.micmaq.segment.Segmentation;
import fr.curie.micmaq.segment.SegmentationParameters;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.LutLoader;

import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.ArrayList;

public class ExperimentSettings {
    FieldOfView imgs;
    int nucleiSegmentationChannel=-1;
    SegmentationParameters nucleiSegmentationParams;
    NucleiDetector nucleiDetector;
    Roi[] nucleiRois;
    int  cellSegmentationChannel=-1;
    SegmentationParameters cellSegmentationParams;
    CellDetector cellDetector;
    CytoDetector cytoDetector;
    ArrayList<MeasureValue> quantificationChannels;
    MeasureCalibration calibration;
    String resultsDir;


    public ExperimentSettings(FieldOfView images){
        this.imgs=images;
        quantificationChannels=new ArrayList<>(images.getNbAvailableChannels());
        for(int i=0;i<imgs.getNbAvailableChannels();i++){
            quantificationChannels.add(null);
        }
    }

    public void setSegmentationNuclei(int channel, SegmentationParameters params){
        this.nucleiSegmentationChannel=channel;
        this.nucleiSegmentationParams=params;
        //params.getMeasurements().setName("Nuclei");
        //quantificationChannels.set(channel-1,params.getMeasurements());
    }

    public void removeSegmentationNuclei(){
        nucleiSegmentationChannel=-1;
        nucleiDetector=null;
        nucleiSegmentationParams=null;
        cellDetector=null;
        cytoDetector=null;
    }
    public void setSegmentationCell(int channel, SegmentationParameters params){
        this.cellSegmentationChannel=channel;
        this.cellSegmentationParams=params;
        //params.getMeasurements().setName("Cell");
        //quantificationChannels.set(channel-1,params.getMeasurements());
    }
    public void removeSegmentationCell(){
        cellSegmentationChannel=-1;
        cellSegmentationParams=null;
        cellDetector=null;
        cytoDetector=null;
    }



    public void setQuantification(int channel, MeasureValue params){
        quantificationChannels.set(channel-1,params);
    }

    public void setCalibration(MeasureCalibration calibration) {
        this.calibration = calibration;
    }

    public Experiment createExperiment(String directory, FieldOfView imgs, ResultsTable finalResultsCellspot, ResultsTable finalResultsNuclei,boolean preview){
        this.imgs=imgs;
        resultsDir= directory;
        File tmp=new File(resultsDir);
        if(!tmp.exists()) tmp.mkdirs();
        System.out.println("outputs will be saved in "+resultsDir);
        IJ.log("working directory is "+resultsDir);

        NucleiDetector nucl=getNucleiDetector(preview);
        CellDetector cell=getCellDetector(preview);
        ArrayList<SpotDetector> spots=getSpotDetectors(preview);
        System.out.println("quantification channels: "+spots.size());
        if(nucl!=null) nucl.cleanNameExperiment();
        if(cell!=null) cell.cleanNameExperiment();
        /*if(nucleiDetector!=null) IJ.log("nuclei name:"+nucleiDetector.getNameExperiment()+"("+nucleiDetector.getImageTitle()+")");
        if(cellDetector!=null) IJ.log("cell name:"+cellDetector.getNameExperiment()+"("+cellDetector.getImageTitle()+")");
        if(spots!=null) {
            IJ.log("spots available" );
            ArrayList<SpotDetector> spotsArray=getSpotDetectors(preview);
            for(SpotDetector sp:spotsArray){
                if(sp!=null) IJ.log("spot name: "+sp.getNameExperiment()+"("+sp.getImageTitle()+")");
                else IJ.log("null");
            }
        }*/


        return new Experiment(nucl, cell, spots,finalResultsCellspot,finalResultsNuclei,calibration);
    }

    public NucleiDetector getNucleiDetector(boolean preview){
        if(nucleiSegmentationChannel<0) {
            nucleiDetector=null;
            return null;
        }
        if(nucleiDetector!=null) return nucleiDetector;
        nucleiDetector=new NucleiDetector(imgs.getImagePlus(nucleiSegmentationChannel),imgs.getFieldname(), resultsDir, preview);

        nucleiDetector.setMeasurements(nucleiSegmentationParams.getMeasurements().measure);
        nucleiDetector.setNameChannel(nucleiSegmentationParams.getMeasurements().name);
        //IJ.log("nuclei detector : "+nucleiDetector.getNameChannel()+" should be "+nucleiSegmentationParams.getMeasurements().name);
        if(nucleiSegmentationParams.isZproject()){
            if(nucleiSegmentationParams.getProjectionSliceMin()>=0){
                nucleiDetector.setzStackParameters(nucleiSegmentationParams.getProjectionMethodAsString(),nucleiSegmentationParams.getProjectionSliceMin(), nucleiSegmentationParams.getProjectionSliceMax());
            }else {
                nucleiDetector.setzStackParameters(nucleiSegmentationParams.getProjectionMethodAsString());
            }
        }
        if(nucleiSegmentationParams.getPreprocessMacro()!=null && !nucleiSegmentationParams.getPreprocessMacro().equals("")){
            nucleiDetector.setPreprocessingMacro(nucleiSegmentationParams.getPreprocessMacro());
        }
        if(nucleiSegmentationParams.getPreprocessMacroQuantif()!=null && !nucleiSegmentationParams.getPreprocessMacroQuantif().equals("")){
            nucleiDetector.setPreprocessingMacroQuantif(nucleiSegmentationParams.getPreprocessMacroQuantif());
        }
        if(nucleiSegmentationParams.getMethod()==SegmentationParameters.THRESHOLDING){
            nucleiDetector.setThresholdMethod(nucleiSegmentationParams.getThresholdMethod(),nucleiSegmentationParams.getMinSize(),nucleiSegmentationParams.isThresholdingWatershed(),nucleiSegmentationParams.isExcludeOnEdge());
        }else if(nucleiSegmentationParams.getMethod()==SegmentationParameters.CELLPOSE){
            String cellposemodel=(nucleiSegmentationParams.getCellposeModel().equalsIgnoreCase("own model"))?nucleiSegmentationParams.getPathToModel().getAbsolutePath():nucleiSegmentationParams.getCellposeModel();
            nucleiDetector.setCellposeMethod(nucleiSegmentationParams.getCellposeDiameter(),
                    nucleiSegmentationParams.getCellposeCellproba_trheshold(),
                    cellposemodel,
                    nucleiSegmentationParams.isExcludeOnEdge());
        }else if(nucleiSegmentationParams.getMethod()==SegmentationParameters.STARDIST){
            nucleiDetector.setStarDistMethod(nucleiSegmentationParams.getStardistModel(),
                    nucleiSegmentationParams.getStardistPercentileBottom(),
                    nucleiSegmentationParams.getStardistPercentileTop(),
                    nucleiSegmentationParams.getStardistProbThresh(),
                    nucleiSegmentationParams.getStardistNmsThresh(),
                    nucleiSegmentationParams.getStardistModelFile(),
                    nucleiSegmentationParams.getStardistScale(),
                    nucleiSegmentationParams.isExcludeOnEdge());
        }
        nucleiDetector.setSavings(nucleiSegmentationParams.isSaveMasks(),nucleiSegmentationParams.isSaveROIs());
        nucleiDetector.setSegmentation(nucleiSegmentationParams.isUserValidation(),preview);
        if(nucleiSegmentationParams.getExpansionRadius()>0) nucleiDetector.setExpandRadius(nucleiSegmentationParams.getExpansionRadius());

        return nucleiDetector;

    }

    public CellDetector getCellDetector(boolean preview){
        if(cellSegmentationChannel<0) {
            cellDetector=null;
            return null;
        }
        if(cellDetector!=null) {
            return cellDetector;
        }
        cellDetector = new CellDetector(imgs.getImagePlus(cellSegmentationChannel), imgs.getFieldname(), resultsDir, preview, preview, preview);
        cellDetector.setMeasurements(cellSegmentationParams.getMeasurements().measure);
        cellDetector.setNameChannel(cellSegmentationParams.getMeasurements().name);


//        Projection ?
        if (cellSegmentationParams.isZproject()) {
            if (cellSegmentationParams.getProjectionSliceMin()>=0) {
                cellDetector.setZStackParameters(cellSegmentationParams.getProjectionMethodAsString(), cellSegmentationParams.getProjectionSliceMin(), cellSegmentationParams.getProjectionSliceMax());
            } else {
                cellDetector.setZStackParameters(cellSegmentationParams.getProjectionMethodAsString());
            }
        }
//        Macro pretreatment?
        if (cellSegmentationParams.getPreprocessMacro()!=null && !cellSegmentationParams.getPreprocessMacro().equals("")) {
            IJ.log("cell detector initiation: add macro");
            cellDetector.setPreprocessingMacro(cellSegmentationParams.getPreprocessMacro());
        }
//        Macro pretreatment for quantification?
        if (cellSegmentationParams.getPreprocessMacroQuantif()!=null && !cellSegmentationParams.getPreprocessMacroQuantif().equals("")) {
            IJ.log("cell detector initiation: add macro");
            cellDetector.setPreprocessingMacroQuantif(cellSegmentationParams.getPreprocessMacroQuantif());
        }



        String cellposemodel=(cellSegmentationParams.getCellposeModel().equalsIgnoreCase("own model"))?cellSegmentationParams.getPathToModel().getAbsolutePath():cellSegmentationParams.getCellposeModel();
        System.out.println("cellpose model (experimentsettings):"+cellposemodel);
        cellDetector.setDeepLearning(cellSegmentationParams.getCellposeDiameter(),
                cellSegmentationParams.getCellposeCellproba_trheshold(),
                cellposemodel,
                cellSegmentationParams.isExcludeOnEdge(),cellSegmentationParams.isUserValidation(), preview);

//        Cytoplasm ?
        NucleiDetector nucleiDetector=getNucleiDetector(preview);
        cellDetector.setNucleiDetector(nucleiDetector);
        if(nucleiDetector!=null){
            cellDetector.setCytoplasmParameters(cellSegmentationParams.getMinOverlap(), cellSegmentationParams.getMinCytoSize());
            IJ.log("segmentation of cells is associated to the nuclei image: cytoplasm mask will be generated");
            IJ.log("nuclei image: "+nucleiDetector.getImageTitle());
            IJ.log("cell image: "+cellDetector.getImageTitle());

        }
        cellDetector.setSavings(cellSegmentationParams.isSaveMasks(),cellSegmentationParams.isSaveROIs());
        return cellDetector;
    }

    public ArrayList<SpotDetector> getSpotDetectors(boolean preview){
        ArrayList<SpotDetector> spots=new ArrayList<SpotDetector>();
        if(quantificationChannels!=null){
            //for(int i=0;i<quantificationChannels.size();i++) spots.add(null);
            System.out.println("quantificationChannels size: "+quantificationChannels.size());
            for(int i=0;i<quantificationChannels.size();i++){
                //System.out.println("quantif "+i+" "+quantificationChannels.get(i));
                if(quantificationChannels.get(i)!=null){
                    MeasureValue measureValue=quantificationChannels.get(i);
                    //IJ.log("exp settings getSpotDetector: macro: "+measureValue.getPreprocessMacro());
                    //IJ.log("exp settings getSpotDetector: macro quantif: "+measureValue.getPreprocessMacroQuantif());
                    ImagePlus tmpchan=imgs.getImagePlus(i+1);
                    //System.out.println(tmpchan.getTitle());
                    //System.out.println(measureValue.getMeasure());
                    SpotDetector tmp=new SpotDetector(tmpchan, measureValue.getName(), imgs.getFieldname(), resultsDir, preview);
                    //        Projection ?
                    if (measureValue.isZproject()) {
                        if (measureValue.getProjectionSliceMin()>=0) {
                            tmp.setzStackParameters(measureValue.getProjectionMethodAsString(), measureValue.getProjectionSliceMin(), measureValue.getProjectionSliceMax());
                        } else {
                            tmp.setzStackParameters(measureValue.getProjectionMethodAsString());
                        }
                    }
//        Macro pretreatment?
                    if (measureValue.getPreprocessMacro()!=null && !measureValue.getPreprocessMacro().equals("")) {
                        //IJ.log("exp setting get detector set macro");
                        tmp.setPreprocessingMacro(measureValue.getPreprocessMacro());
                    }
                    if (measureValue.getSubtractBGRadius()>0) tmp.setRollingBallSize(measureValue.getSubtractBGRadius());

                    if(measureValue.isSpotFindMaxima()) {
                        tmp.setSpotByFindMaxima(measureValue.getMaximaProminence(), preview);
                        tmp.setSaving(true,true);
                    }

                    if(measureValue.isSpotThreshold()) {
                        tmp.setSpotByThreshold(measureValue.getThresholdMethod(),
                                measureValue.getMinThreshold(), measureValue.getMaxThreshold(),
                                measureValue.getMinSizeSpot(), measureValue.isUseWatershed(), preview);
                        tmp.setSaving(true,true);
                    }

                    tmp.setMeasurements(measureValue.getMeasure());
                    tmp.setPreprocessingMacroQuantif(measureValue.getPreprocessMacroQuantif());


                    //spots.set(i,tmp);
                    spots.add(tmp);
                }else{
                    spots.add(null);
                }
            }
        }
        IJ.log("spots size: "+ spots.size());
        return spots;
    }

    public ResultsTable run(){
        if(this.nucleiSegmentationChannel>0){
            IndexColorModel cm= LutLoader.getLut("3-3-2 RGB");
            ImagePlus nucImp=imgs.getImagePlus(nucleiSegmentationChannel);
            Object[] result=Segmentation.segment(nucImp,nucleiSegmentationParams);
            ImagePlus mask2=(ImagePlus) result[0];
            mask2.getProcessor().resetMinAndMax();
            mask2.getProcessor().setColorModel(cm);
            mask2.show();
            nucleiRois=(Roi[])result[1];

        }
        //ResultsTable rt=measureAll();
        //return rt;
        return null;
    }




    /*
    run experiment
    segmentation of nuclei
    segmentation of cell
    cytoplasm mask creation if available
    nuclei measure
    cell measure
    cytoplasm measure
    Quantification channels measures in nuclei/cell/cytoplasm

     */


}
