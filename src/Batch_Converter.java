/*
 * @(#)Batch_Converter.java        1.0 19.2.2013
 *
 * I found this file in my archives and I assume that I have written most of it
 * myself. If you find that this is not the case, please contact me!
 *
 * This is made mostly for processing lots of .dm3 files to tiffs. 
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

import ij.plugin.*;
import java.awt.*;
import java.io.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.ScaleBar;
import ij.measure.Calibration;

/*  Converts a folder of images in any format supported by ImageJ's 
File>Open command into TIFF, 8-bit TIFF, JPEG, GIF, PNG, PGM,
BMP, FITS, Text Image, ZIP or Raw. The plugin displays three dialogs. 
In the first, select the source folder. In the second, select the format
you want to convert to. In the third, select the destination folder.
*/
public class Batch_Converter implements PlugIn {

    private static String[] choices = {"TIFF", "8-bit Color TIFF", "JPEG", "GIF", "PNG", "PGM", "BMP", "FITS", "Text Image", "ZIP", "Raw"};
    private static String format = "TIFF";
    private static boolean convertToGrayscale;
    private static boolean addScaleBar;
	
	public void run(String arg) {
		String dir1 = IJ.getDirectory("Select source folder...");
		if (dir1==null) return;
		if (!showDialog()) return;
		String dir2 = IJ.getDirectory("Select destination folder...");
		if (dir2==null) return;
		convert(dir1, dir2, format);
	}

	boolean showDialog() {
		GenericDialog gd = new GenericDialog("Batch Converter");
		gd.addChoice("Convert to: ", choices, format);
		gd.addCheckbox("Make Grayscale", convertToGrayscale);
		gd.addCheckbox("Add Scale Bar", addScaleBar);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		format = gd.getNextChoice();
		convertToGrayscale = gd.getNextBoolean();
		addScaleBar = gd.getNextBoolean();
		return true;
	}

	public void convert(String dir1, String dir2, String format) {
		IJ.log("\\Clear");
		IJ.log("Converting to "+format);
		IJ.log("dir1: "+dir1);
		IJ.log("dir2: "+dir2);
		String[] list = new File(dir1).list();
		if (list==null) return;
		for (int i=0; i<list.length; i++) {
			IJ.showProgress(i, list.length);
			IJ.log((i+1)+": "+list[i]+"  "+WindowManager.getImageCount());
			IJ.showStatus(i+"/"+list.length);
			boolean isDir = (new File(dir1+list[i])).isDirectory();
			if (!isDir && !list[i].startsWith(".")) {
				ImagePlus img = IJ.openImage(dir1+list[i]);
				if (img==null) continue;
				img = process(img);
				if (img==null) continue;
				if (img.isComposite())
					img = convertToRGB(img);
				if (img.getStackSize()>1)
					img = getFirstSlice(img);
				if (convertToGrayscale)
					img = convertToGrayscale(img);
				if (addScaleBar)
				    img = addScaleBar(img);
				if (format.equals("8-bit Color TIFF")||format.equals("GIF"))
					img = convertTo8Bits(img);
				WindowManager.setTempCurrentImage(img);
				IJ.saveAs(format, dir2+list[i]);
				img.close();
			}
		}
		IJ.showProgress(1.0);
		IJ.showStatus("");
	}

	/** This is the place to add code to process each image. The image 
		is not written if this method returns null. */
	public ImagePlus process(ImagePlus img) {
		double scale = 0.5;
		int width = img.getWidth();
		int height = img.getHeight();
		//ImageProcessor ip = img.getProcessor();
		//ip.setInterpolate(true);
		//ip = ip.resize((int)(width*scale), (int)(height*scale));
		//img.setProcessor(null, ip);
		ScaleBar sb = new ScaleBar();
		return img;
	}

	ImagePlus convertTo8Bits(ImagePlus img) {
		ImageProcessor ip = img.getProcessor();
		if (ip instanceof ColorProcessor) {
			MedianCut mc = new MedianCut((int[])ip.getPixels(), ip.getWidth(), ip.getHeight());
			img.setProcessor(null, mc.convertToByte(256));
		} else {
			ip = ip.convertToByte(true);
			img.setProcessor(null, ip);
		}
		return img;
	}

	ImagePlus convertToRGB(ImagePlus img) {
		ImagePlus img2 = img.createImagePlus();
		img.updateImage();
		img2.setProcessor(img.getTitle(), new ColorProcessor(img.getImage()));
		return img2;
	}

	ImagePlus getFirstSlice(ImagePlus img) {
		ImagePlus img2 = img.createImagePlus();
		img2.setProcessor(img.getTitle(), img.getProcessor());
		return img2;
	}

	ImagePlus convertToGrayscale(ImagePlus img) {
		ImagePlus img2 = img.createImagePlus();
		img2.setProcessor(img.getTitle(), img.getProcessor().convertToByte(true));
		return img2;
	}

    ImagePlus addScaleBar(ImagePlus img) {
	Calibration cal = img.getCalibration();
	//if (!cal.calibrated())
	//    return img; // Don't do anything if not calibrated

	// Get some calibration values
	double pixelHeight = cal.pixelHeight;
	double pixelWidth = cal.pixelWidth;
	String pixelUnit = cal.getUnit();
	int imageDimensions[] = img.getDimensions();
	int imageWidth = imageDimensions[0];
	double imageWidthInUnits = cal.getX((double)imageWidth);
	
	double width = imageWidthInUnits / 4.0;
	double decade;
	if (width > 1000.0) 
	    decade = 1000.0;
	else if (width > 100.0)
	    decade = 100.0;
	else if (width > 10.0)
	    decade = 10.0;
	else if (width > 1.0)
	    decade = 1.0;
	else if (width > 0.1)
	    decade = 0.1;
	else if (width > 0.01)
	    decade = 0.01;
	else 
	    decade = 0.001;

	width = Math.floor((width/decade)+0.5)*decade;
	IJ.log("Scalebar width: "+width+pixelUnit);

	double height = 16.0;
	double font = 48.0;
	String color = "Black";
	String bgcolor = "White";
	String location = "[Lower Left]";
	String options = "bold overlay";
	IJ.run(img, "Scale Bar...", "width="+width+" height="+height+" unit="+pixelUnit+" font="+font+" color="+color+" background="+bgcolor+" location="+location+" "+options);
	
	// Make semitransparent bg
	Overlay ol = img.getOverlay();
	if (ol.size() == 3) {
	    Roi r = ol.get(0); // This should be the bg
	    r.setFillColor(new Color(255,255,255,128));
	    r.setStrokeColor(new Color(0,0,0,255));
	}
	ImagePlus flatImg = img.flatten();
	flatImg.copyScale(img);
	img = flatImg;
	
	return img;
    }

	/**	Run Batch_Converter using a command something like
			"java -cp ij.jar;. Batch_Converter c:\dir1\ c:\dir2\"
		or (Unix)
			"java -cp ij.jar:. Batch_Converter /users/wayne/dir1 /users/wayne/dir2/"
	*/
	public static void main(String args[]) {
		if (args.length<2)
			IJ.log("usage: java Batch_Converter srcdir dstdir");
		else {
			new Batch_Converter().convert(args[0], args[1], "Jpeg");
			System.exit(0);
		}
	}

}


