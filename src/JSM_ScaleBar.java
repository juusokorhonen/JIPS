/*
 * @(#)JSM_ScaleBar.java        1.0.1 29.4.2014
 *
 * This software is provided under the MIT Licence. Details below.
 * In addition, the author would appreciate it if any improvements to the code
 * would be sent to him.
 *
 * The MIT License (MIT)
 * Copyright (c) 2011-2014 Juuso Korhonen (jk.lic@turqoosi.net)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software and to permit persons to whom 
 * the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
 * SOFTWARE.
 *
 */
package JIPS;

import java.io.*;
import ij.*;
import ij.io.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import ij.measure.*;
import ij.text.*;
import java.util.*;
import java.awt.*;
import java.io.*;

/**
 * This class is a plugin for the program ImageJ. It is meant for users of a 
 * JEOL scanning electron microscope (specifically the JSM-7500F), which creates
 * an informative .txt -file along with the .tif/.jpg/.bmp image file. This
 * plugin reads the information from the .txt -file and enables automatically
 * set the scale in the micrograph.
 * v1.0.1 : Try to update the plugin to work also with Zeiss Sigma micrographs,
 * which are TIFF-files that contain the pixel size information in the headers.
 * Not all functionality might be implemented.
 *
 * @version 	%I% %U%
 * @author 	Juuso Korhonen
 */
public class JSM_ScaleBar implements PlugIn {

    private String fileName;
    private String fileDir;
    private String baseName;
    private String extension;
    private String txtFileName;
    private ImagePlus img;
    private ImagePlus origImg;
    private Calibration imgCal;
    private double imgWidth, imgHeight;
    private double fullWidth, fullHeight;
    private double pixelSize;
    private HashMap infoFromFile;

    private boolean pp_crop = true;
    private boolean pp_smooth = true;
    private boolean pp_manbc = true;
    private boolean pp_resizesmall = false;
    private boolean pp_convertrgb = true;
    private boolean pp_scalebar = true;
    private boolean pp_autosave = false;
    private boolean pp_showimage = true;
    private boolean pp_showinfo = false;

    /**
     * This function is executed, when the plugin is run from the menu.
     * It contains the functionality of the plugin.
     *
     * @param arg   arguments defined in IJ_Props.txt. Not used.
     */
    public void run(String arg) {
			// Open a file using last used directory
			// String def_path = OpenDialog.getLastDirectory();
			OpenDialog od = new OpenDialog("Open image ...", null);

			// From the selected file, get dir and filename
			this.fileDir = od.getDirectory();
			this.fileName = od.getFileName();

			// Check if cancel was pressed
			if (this.fileName == null) return; // Operation cancelled

			// Show a dialog for post-processing
			GenericDialog gd = new GenericDialog("Post-process options");
			gd.setOKLabel("Execute");
			gd.addCheckbox("Crop infobar?", pp_crop);
			gd.addCheckbox("Smoothen?", pp_smooth);
			gd.addCheckbox("Auto enhance contrast?", pp_manbc);
			gd.addCheckbox("Resize to 800x600?", pp_resizesmall);
			gd.addCheckbox("Convert to RGB mode?", pp_convertrgb);
			gd.addCheckbox("Make scalebar at bottom left?", pp_scalebar);
			gd.addCheckbox("Autosave?", pp_autosave);
			gd.addCheckbox("Show image?", pp_showimage);
			gd.addCheckbox("Show info?", pp_showinfo);
			gd.showDialog();

			// Check if process was cancelled
			if (gd.wasCanceled()) return;
			this.pp_crop = gd.getNextBoolean();
			this.pp_smooth = gd.getNextBoolean();
			this.pp_manbc = gd.getNextBoolean();
			this.pp_resizesmall = gd.getNextBoolean();
			this.pp_convertrgb = gd.getNextBoolean();
			this.pp_scalebar = gd.getNextBoolean();
			this.pp_autosave = gd.getNextBoolean();
			this.pp_showimage = gd.getNextBoolean();
			this.pp_showinfo = gd.getNextBoolean();

			// Execute the script (the functionality)
			this.exec();

			// Post-process
			this.origImg = this.img.duplicate();
			if (pp_crop) {
					int w = (int)(((Double)this.infoFromFile.get("image width")).doubleValue()  * ((Double)this.infoFromFile.get("scaling factor")).doubleValue());
					int h = (int)(((Double)this.infoFromFile.get("image height")).doubleValue() * ((Double)this.infoFromFile.get("scaling factor")).doubleValue());
					this.img.setRoi(0,0, w, h);
					IJ.run(this.img, "Crop", "");
			}
			if (pp_manbc) {
					IJ.run(this.img, "Enhance Contrast", "saturated=0.4 equalize");	    
			}
			if (pp_smooth) {
					IJ.run(this.img, "Smooth", "");
			}
			if (pp_resizesmall) {
					IJ.run(this.img, "Size...", "width=800 height=600 constrain average interpolation=Bilinear");
			}
			if (pp_convertrgb) {
					IJ.run(this.img, "RGB Color", "");
			}
			if (pp_scalebar) {
					//	    String scaleunit = (String)this.infoFromFile.get("micron bar unit");
					double width = ((Double)this.infoFromFile.get("micron bar")).doubleValue();
					double height = 4.0;
					double font = 14.0;
					String color = "Black";
					String bgcolor = "White";
					String location = "[Lower Left]";
					String options = "bold overlay";
					IJ.run(this.img, "Scale Bar...", "width="+width+" height="+height+" font="+font+" color="+color+" background="+bgcolor+" location="+location+" "+options);

					// Make semitransparent bg
					Overlay ol = this.img.getOverlay();
					if (ol.size() == 3) {
							Roi r = ol.get(0); // This should be the bg
							r.setFillColor(new Color(255,255,255,128));
							r.setStrokeColor(new Color(0,0,0,255));
					}
					ImagePlus flatImg = this.img.flatten();
					flatImg.copyScale(this.img);
					this.img = flatImg;
			}

			if (pp_autosave) {
					//IJ.showMessage("Not implemented", "Autosave is not yet implemented");
					String newName = new String(this.baseName);
					String newExt = ".tif";
					String format = "TIFF";
					String prefix = "e-";
					if (pp_resizesmall) {
							prefix = "er-";
					}
					newName = prefix + newName;
					IJ.saveAs(this.img, format, this.fileDir + newName);
			}

			if (pp_showimage) {
					this.img.show();
			}

			if (pp_showinfo) {
					String infoWindowTitle = "Information read from the .txt file";
					TextWindow infoWindow = new TextWindow(infoWindowTitle, this.infoFromFile.toString(), 400, 450);
			}
    }   

    public void exec() {
	// Process the filename to get the corresponding .txt file
	this.baseName = this.fileName.substring(0, this.fileName.lastIndexOf('.'));
	this.extension = this.fileName.substring(this.fileName.lastIndexOf('.')+1);
        this.txtFileName = this.baseName + ".txt";

	// Get calibration (from the txtfile)
	getCalibration();
    }

    private static String readFileAsString(String filePath)
	throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            fileData.append(buf, 0, numRead);
        }
        reader.close();
        return fileData.toString();
    }

    /**
     * This function reads image calibration from .txt file. If reading of the
     * file fails, a black calibration is set.
     */
    private void getCalibration() {
			// Get the image size (from the image)
			if (this.img == null) {
					this.img = IJ.openImage(this.fileDir + this.fileName);
			}
			this.imgWidth = this.img.getWidth();
			this.imgHeight = this.img.getHeight();

			// (Try to) read the .txt -file as a string. If this fails, set empty calibration
			String lines = new String();
			try {
					lines = readFileAsString(this.fileDir + this.txtFileName);
			}
			catch (Exception e) {
					IJ.error("JSM ScaleBar...", e.getMessage());
					this.imgCal = new Calibration();
					this.img.setCalibration(this.imgCal);
			}	

			// Process the file to an array
			String linesarray[] = lines.split("\\r?\\n|\\r");

			this.infoFromFile = new MyHashMap();
			// Now read the file and put information into temporary variables
			for (int i=0; i<linesarray.length; i++) {
					String line = linesarray[i];
					if (line.startsWith("$$SM_MICRON_BAR ")) {
							String micron_bar_string = line.substring(16);
							this.infoFromFile.put("micron bar px", new Double(micron_bar_string));
					}
					else if (line.startsWith("$$SM_MICRON_MARKER ")) {
							String micron_marker_string = line.substring(19, line.length()-2);
							String micron_marker_unit_string = line.substring(line.length()-2);
							this.infoFromFile.put("micron bar", new Double(micron_marker_string));
							this.infoFromFile.put("micron bar unit", micron_marker_unit_string);
					}
					else if (line.startsWith("$CM_FULL_SIZE ")) {
							String fullsize_string = line.substring(14);
							String fullsize_strings[] = fullsize_string.split(" ");
							this.infoFromFile.put("image width",  new Double(fullsize_strings[0]));
							this.infoFromFile.put("image height",  new Double(fullsize_strings[1]));
					}
					else if (line.startsWith("$CM_TITLE ")) {
							String title_string = line.substring(10);
							this.infoFromFile.put("title", title_string);
					}
					else if (line.startsWith("$CM_INSTRUMENT ")) {
							String instrument_string = line.substring(15);
							this.infoFromFile.put("instrument", instrument_string);
					}
					else if (line.startsWith("$CM_DATE ")) {
							String date_string = line.substring(9);
							this.infoFromFile.put("date", date_string);
					}
					else if (line.startsWith("$CM_TIME ")) {
							String time_string = line.substring(9);
							this.infoFromFile.put("time", time_string);
					}
					else if (line.startsWith("$SM_PENNING_VAC ")) {
							String vac_string = line.substring(16);
							this.infoFromFile.put("vacuum", vac_string);
					}
					else if (line.startsWith("$CM_SIGNAL ")) {
							String signal_string = line.substring(11);
							this.infoFromFile.put("signal", signal_string);
					}
					else if (line.startsWith("$CM_SIGNAL_NAME ")) {
							String signalname_string = line.substring(16);
							this.infoFromFile.put("signal name", signalname_string);
					}
					else if (line.startsWith("$CM_ACCEL_VOLT ")) {
							String accv_string = line.substring(15);
							this.infoFromFile.put("acceleration voltage", accv_string);
					}
					else if (line.startsWith("$SM_GB_BIAS_VOLT ")) {
							String biasv_string = line.substring(17);
							this.infoFromFile.put("bias voltage", biasv_string);
					}
					else if (line.startsWith("$$SM_WD ")) {
							String wd_string = line.substring(8);
							this.infoFromFile.put("working distance", new Double(wd_string));
					}
					else if (line.startsWith("$CM_MAG ")) {
							String mag_string = line.substring(8);
							this.infoFromFile.put("magnification", new Double(mag_string));
					}
					else if (line.startsWith("$$SM_COLUMN_MODE ")) {
							String colmode_string = line.substring(17);
							this.infoFromFile.put("column mode", colmode_string);
					}
					else if (line.startsWith("$CM_STAGE_POS ")) {
							String stagepos_string = line.substring(14);
							String stagepos_strings[] = stagepos_string.split(" ");
							Double x = new Double(stagepos_strings[0]);
							Double y = new Double(stagepos_strings[1]);
							Double z = new Double(stagepos_strings[2]);
							Double r = new Double(stagepos_strings[3]);
							Double t = new Double(stagepos_strings[4]);
							//Double extra = new Double(stagepos_strings[0]);
							this.infoFromFile.put("stage x", x);
							this.infoFromFile.put("stage y", y);
							this.infoFromFile.put("stage z", z);
							this.infoFromFile.put("stage r", r);
							this.infoFromFile.put("stage t", t);
					}
			}

			try { // Check for nullpointer exceptions	
					// The width of the image does not contain the information bar and can thus be used to calculcate the scaling factor
					double sizeScaleFactor = this.imgWidth / ((Double)this.infoFromFile.get("image width")).doubleValue();

					// Calculate the pixelsize. The scaling factor has to be taken into account, because of a bug in the PC_SEM software
					// which occurs, when a file is saved in a lower resolution than 1024 px wide.
					this.pixelSize = ((Double)this.infoFromFile.get("micron bar")).doubleValue() / ( ((Double)this.infoFromFile.get("micron bar px")).doubleValue() * sizeScaleFactor );
					this.infoFromFile.put("scaling factor", new Double(sizeScaleFactor));

					// Make the calibration
					this.imgCal = new Calibration();
					this.imgCal.setUnit((String)this.infoFromFile.get("micron bar unit"));
					this.imgCal.pixelWidth = this.pixelSize;
					this.imgCal.pixelHeight = this.pixelSize;

					this.img.setCalibration(this.imgCal);
			}
			catch (NullPointerException e) {
					// Do nothing here
			}
	}

    /** 
     * This is a class which inherits the HashMap. It's only functionality so far
     * is to make a nicer toString() method.
     */
    private class MyHashMap extends HashMap {
	public String toString() {
	    String message = new String();
	    Iterator i = this.entrySet().iterator();
	    while(i.hasNext()){
		Map.Entry me = (Map.Entry)i.next();
		message += me.getKey().toString() + " : " + me.getValue().toString() + "\n";
	    }
	    return message;
	}
    }

}

