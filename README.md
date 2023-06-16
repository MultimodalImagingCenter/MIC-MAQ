# MIC-MAQ <img src="resources/MIC-MAQ_logo.png" width="100" >

## About 

MIC-MAQ for Microscopy Images of Cells - Multi Analyses and Quantifications is an ImageJ/Fiji Plugin for automatic segmentation of nuclei and/or cells for quantifications in other channels including foci detection 

- MIC-MAQ automatically segments cells and/or nuclei on 2D microscopy images 
- The plugin takes Z-stack but works only on 2D projection
- It provides intensity based and/or morphological measurements in nuclei and/or cells in multi channels experiment
- The plugin can propose the foci/spots detection

## Install

First install Cellpose 

installation instruction can be found at 
https://github.com/MouseLand/cellpose#installation

Then install and configure Cellpose wrapper for Fiji 

installation instruction can be found at 
https://github.com/BIOP/ijl-utilities-wrappers#ib-fiji---cellpose-wrapper

Finally install MIC-MAQ plugin
The best is to use the updater in Fiji:

menu >Help>Update...

click the button "Manage update sites"

select MIC-MAQ

if it is not available directly you can add it (button add update site) with the folowing URL https://sites.imagej.net/MIC-MAQ/


## Usage

run >Plugin>MIC-MAQ>MIC-MAQ

## Licensing

 MIC-MAQ plugin is licensed under the GNU GPL v2 License