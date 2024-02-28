package fr.curie.micmaq.helpers;

import fr.curie.micmaq.detectors.CellposeLauncher;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

public class TiledCellposeLauncher implements PlugInFilter {
    ImagePlus imp;
    int sizexy=1024;
    int overlap=100;
    int cellposeDiameter=50;
    double cellproba = 0.0;

    String model="cyto2";

    @Override
    public int setup(String arg, ImagePlus imp) {
        this.imp=imp;
        if(!askParameters()) return DONE;
        return DOES_ALL;
    }

    public void setParameters(int sizexy,  int overlap){
        this.sizexy=sizexy;
        this.overlap=overlap;
    }


    public boolean askParameters(){
        GenericDialog gd= new GenericDialog("Tiled Cellpose");
        gd.addNumericField("tile_size (square)", sizexy);
        gd.addNumericField("overlap between tiles", overlap);
        gd.addMessage("----------Cellpose-------------");
        gd.addStringField("Cellpose_model",model);
        gd.addNumericField("Cellpose_diameter", cellposeDiameter);
        gd.addNumericField("Cellpose_cell_proba", cellproba);


        gd.showDialog();
        if(gd.wasCanceled())return false;

        sizexy=(int)gd.getNextNumber();
        overlap=(int)gd.getNextNumber();
        model=gd.getNextString();
        cellposeDiameter=(int)gd.getNextNumber();
        cellproba=gd.getNextNumber();
        return true;
    }

    @Override
    public void run(ImageProcessor ip) {
        IJ.showStatus("create tiles");
        MakeTiles tilesMaker=new MakeTiles();
        tilesMaker.setParameters(sizexy,1,overlap);
        ImagePlus tiles=null;
        if(imp.getNChannels()==1) {
            tiles=new ImagePlus("tiles",tilesMaker.tileImage(ip));
        }else{
            tiles=tilesMaker.tileChannels(imp);
        }
        ImageStack masks=new ImageStack(sizexy, sizexy);
        tiles.show();

        CombineTiles combiner=new CombineTiles();
        combiner.setParameters(imp.getWidth(),imp.getHeight(),1, overlap, true);
        for(int slice=1;slice<=tiles.getNSlices();slice++){
            IJ.showStatus("run cellpose on tile #"+slice+" / "+tiles.getNSlices());
            if(imp.getNChannels()==1){
                tiles.setSlice(slice);
                ImageProcessor tmp= tiles.getProcessor();
                CellposeLauncher cpl=new CellposeLauncher(new ImagePlus("tile"+slice,tmp),cellposeDiameter,cellproba,model,1,0,false);
                cpl.analysisWithoutRois();
                masks.addSlice(cpl.getCellposeMask().getProcessor());
            }else{
                tiles.setPosition(1,slice,1);
                ImageProcessor c1=tiles.getProcessor().duplicate();
                tiles.setPosition(2,slice,1);
                ImageProcessor c2=tiles.getProcessor().duplicate();
                ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{new ImagePlus("c1",c1), new ImagePlus("c2",c2)}, true);
                composite.show();
                CellposeLauncher cpl=new CellposeLauncher(composite,cellposeDiameter,cellproba,model,1,2,false);
                cpl.analysisWithoutRois();
                masks.addSlice(cpl.getCellposeMask().getProcessor());
            }
            IJ.showProgress(slice,tiles.getNSlices());
        }
        IJ.showStatus("combine masks");

        ImagePlus result= new ImagePlus(imp.getTitle()+"TiledCellpose_mask",combiner.combineTileImage(masks));
        result.show();

    }
}
