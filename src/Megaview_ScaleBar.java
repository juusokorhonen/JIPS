/*
 * @(#)Megaview_ScaleBar.java        1.0 19.2.2013
 *
 * This software is provided under the MIT Licence. Details below.
 * In addition, the author would appreciate it if any improvements to the code
 * would be sent to him.
 *
 * The MIT License (MIT)
 * Copyright (c) 2011 Juuso Korhonen (jk.lic@turqoosi.net)
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
import java.util.*;
import java.awt.*;
import java.io.*;

public class Megaview_ScaleBar implements PlugIn {

    public void run(String arg) {
	OpenDialog od = new OpenDialog("Open image ...", arg);
	String directory = od.getDirectory();
	String fileName = od.getFileName();
	if (fileName == null) return;
	String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
	String extension = fileName.substring(fileName.lastIndexOf('.')+1);

	Calibration scal = getCalibrationFromFile(directory + fileName);

	ImagePlus img = IJ.openImage(directory + fileName);
	img.setCalibration(scal);
	img.show();
	//	img.setRoi(0,0,1280,960);
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

    private static Calibration getCalibrationFromFile(String textfile) {
	// Read every line of the file
	String lines = new String();
	try {
	    lines = readFileAsString(textfile);
	}
	catch (Exception e) {
	    IJ.error("JSM ScaleBar...", e.getMessage());
	    return new Calibration();
	}

	// Split the file into lines (it's binary, but who cares?)
	String linesarray[] = lines.split("\\r?\\n|\\r");

       
	String mag_string = new String();

	// Find the correct line containing the magnification info and put the magnification in mag_string (e.g. 98000x)
	for (int i=0; i<linesarray.length; i++) {
	    String line = linesarray[i];
	    if (line.startsWith("Mode ")) {
		String[] result = line.split("\\s");
		mag_string = result[result.length-1];	 
	    }
	}

	// Initialize magnification variables and read the magnification value
	double mag = Double.parseDouble(mag_string.substring(0,mag_string.length()-1));
	String micron_marker_unit = "nm";
	double pixelsize = 0;
	// Magnification calibration is set below, pixelsize is the size of pixel in nanometers
	if (mag == 340000) {
	    pixelsize = 0.2354;
	}
	else if (mag == 150000) {
	    pixelsize = 0.5419;
	}
	else if (mag == 120000) {
	    pixelsize = 0.6821;
	}
	else if (mag == 68000) {
	    pixelsize = 1.237;
	}
	else if (mag == 23000) {
	    pixelsize = 3.735;
	}
	else if (mag == 6800) {
	    pixelsize = 12.41;
	}
	else if (mag == 30000) {
	    pixelsize = 2.868;
	}
	else if (mag == 250000) {
	    pixelsize = 0.3216;
	}
	else if (mag == 18500) {
	    pixelsize = 4.635;
	}
	else {
	    pixelsize = 1.0;
	}

	Calibration cal = new Calibration();
	cal.setUnit(micron_marker_unit);
	cal.pixelWidth = pixelsize;
	cal.pixelHeight = pixelsize;
	
	return cal;
    }
}

