/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kyselad;

import static ij.IJ.*;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.SaveDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.AutoThresholder;
import ij.process.ImageProcessor;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 *
 * @author dtk6
 */
public class Orient_Array implements PlugIn {
    double pixelSize = 2.6;
    double spotSpacing = 500;
    double pinSpacing = 4500;
    double maxOffset = 0.75; // maximum spot offset from center, ranging from 0-1
    int xPinCount = 9;
    int yPinCount = 4;
    int arrayCountX = 8;
    int arrayCountY = 8;
    double minSpotArea = 2500; // Minimum area of a spot, in square microns
    double minSpotDensity = ((double)(minSpotArea))/(pixelSize*pixelSize);
    int dilateErodeSteps = 3;

    @Override
    public void run(String arg) {
    
        ImagePlus imp = getImage();
        
        // Initial point ROI is top left reference position
        Roi initRoi = imp.getRoi();
        if (
                initRoi == null || 
                initRoi.getType() != Roi.POINT || 
                initRoi.getPolygon().npoints != 1) 
        {
            IJ.error("requires point ROI");
            return;
        }
        
        pixelSize = IJ.getNumber("pixel size", 2.6d);
        double refX = initRoi.getPolygon().xpoints[0];
        double refY = initRoi.getPolygon().ypoints[0];
        
        // Build a list of reference points, one per pin array
        float[] xRoi = new float[xPinCount * yPinCount];
        float[] yRoi = new float[xPinCount * yPinCount];
        for (int yi=0; yi<yPinCount; yi++) {
            for (int xi=0; xi<xPinCount; xi++) {
                int pinN = (yi * xPinCount) + xi;
                xRoi[pinN] = (float)(refX + (xi * pinSpacing / pixelSize));
                yRoi[pinN] = (float)(refY + (yi * pinSpacing / pixelSize));
            }
        }
        PointRoi referencePinRoi = new PointRoi(xRoi, yRoi, xPinCount * yPinCount);
        imp.setRoi(referencePinRoi);
        
        // Manual adjustment of reference points
        GenericDialog gd = new NonBlockingGenericDialog("Adjust points");
        gd.addMessage("Adjust reference pin positions, then click OK");
        gd.showDialog();
        if (gd.wasCanceled()) {return;};
        Roi adjustedRoi = imp.getRoi();
        if (
                adjustedRoi == null ||
                adjustedRoi.getType() != Roi.POINT || 
                adjustedRoi.getPolygon().npoints != xPinCount * yPinCount)
        {
            IJ.error("Failed to find point selection matching pin count");
            return;
        }
        
        // Get image-wide threshold value
        imp.setRoi((Roi)null);
        IJ.setAutoThreshold(imp, "Default dark");
        Prefs.blackBackground = true;
        IJ.run(imp, "Convert to Mask", "");
        imp.show();
        ImageProcessor ip = imp.getProcessor();
        
        // Iterate through pins to find spot positions
        int totalSpotCount = (xPinCount * yPinCount * arrayCountX * arrayCountY) + (2 * xPinCount * yPinCount);
        Coordinate[] coordArray = new Coordinate[totalSpotCount];
        int[] roiXList = new int [totalSpotCount];
        int[] roiYList = new int [totalSpotCount];
        int n = 0;
        int spotIpWidth = (int)Math.round(spotSpacing/pixelSize);
        for (int pinN=0; pinN < xPinCount * yPinCount; pinN++) {
            String pinName = String.format("%02d", pinN + 1);
            int xOffset = adjustedRoi.getPolygon().xpoints[pinN] - (int)Math.round(0.5 * spotSpacing / pixelSize);
            int yOffset = adjustedRoi.getPolygon().ypoints[pinN] - (int)Math.round(0.5 * spotSpacing / pixelSize);
            for (int yi=0; yi<arrayCountY; yi++) {
                for (int xi=-1; xi<arrayCountX; xi++) {
                    if (xi==-1 && yi>1) {continue;}
                    String posName;
                    if (xi==-1 && yi==0) {
                        posName = pinName+"Fid";
                    } else if (xi==-1 && yi==1){ 
                        posName = pinName+"Ref";
                    } else {
                        posName = pinName+getCharForNumber(yi+1)+(xi+1);
                    }
                    int x = xOffset + (int)Math.round(((xi + 1) * spotSpacing)/pixelSize);
                    int y = yOffset + (int)Math.round((yi * spotSpacing)/pixelSize);
                    Roi roi = new Roi(x, y, spotIpWidth, spotIpWidth);
                    ip.setRoi(roi);
                    ImageProcessor spotIp = ip.crop();
                    //Erode and dilate are backwards on the thresholded ImageProcessor!
                    for (int i=0; i<dilateErodeSteps; i++) {spotIp.erode();}
                    for (int i=0; i<dilateErodeSteps; i++) {spotIp.dilate();}
                    double[] centroid = getCenterUsingParticleAnalyzer(spotIp);
                    // Calculate coordinate distances from reference point in microns
                    // Note for Nikon stage, we need to negate x values
                    double coordX = -(x + centroid[0] - refX) * pixelSize;
                    double coordY = (y + centroid[1] - refY) * pixelSize;
                    if (n==0) {
                        coordX=0; coordY=0;
                        refX = x + centroid[0]; 
                        refY = y + centroid[1];
                    }
                    Coordinate coord = new Coordinate(coordX, coordY, posName);
                    coordArray[n] = coord;
                    roiXList[n] = (int)Math.round(x + centroid[0]);
                    roiYList[n] = (int)Math.round(y + centroid[1]);
                    n++;
                }
            }
        }
        PointRoi allPosRoi = new PointRoi(roiXList, roiYList, roiXList.length);
        imp.setRoi(allPosRoi);
        MultipointUtils mp = new MultipointUtils();
//        JFileChooser wjfc = new JFileChooser(System.getProperty("user.home"));
//        int returnValue = wjfc.showSaveDialog(null);
//        if (returnValue != JFileChooser.APPROVE_OPTION) {
//            return;
//        }
//        File writeFile = wjfc.getSelectedFile();
        SaveDialog sd = new SaveDialog(
                "save", 
                System.getProperty("user.home"),
                null,
                ".xml");
        File writeFile = new File(sd.getDirectory()+System.getProperty("file.separator")+sd.getFileName());
        mp.writeNDCoordinateFile(coordArray, writeFile);
    }
    
    double[] getCenterUsingPixels (ImageProcessor ip) {
        double[] center = new double[2];
        double sum=0;
        ArrayList<Integer> xList = new ArrayList<Integer>();
        ArrayList<Integer> yList = new ArrayList<Integer>();
        for (int xi=0; xi<ip.getWidth(); xi++) {
            for (int yi=0; yi<ip.getHeight(); yi++) {
                if (ip.getPixel(xi, yi) > 0){
                    xList.add(xi);
                    yList.add(yi);
                    sum++;
                }
            }
        }
        double xAvg=0;
        double yAvg=0;
        for (int x : xList) {
            xAvg += (double)x / (double)xList.size();
        }
        for (int y : yList) {
            yAvg += (double)y / (double)yList.size();
        }
        center[0] = xAvg;
        center[1] = yAvg;
        if (sum < minSpotDensity) {
            return getDefaultCenter(ip);
        } else {
            return center;
        }
    }
    
    double[] getCenterUsingParticleAnalyzer (ImageProcessor ip) {
        double[] center = new double[2];
        ResultsTable rt = new ResultsTable();
        ParticleAnalyzer pa = new ParticleAnalyzer(
                ParticleAnalyzer.SHOW_NONE, 
                ParticleAnalyzer.AREA | ParticleAnalyzer.CENTROID, 
                rt,
                minSpotDensity,
                Double.POSITIVE_INFINITY);
        ImagePlus spotImp = new ImagePlus("spot", ip);
        pa.analyze(spotImp);
        if (rt.size() < 1) {
            return getDefaultCenter(ip);
        }
        int areaIndex = rt.getColumnIndex("Area");
        float[] area = rt.getColumn(areaIndex);
        float areaMax=0f;
        int areaMaxIndex=-1;
        for (int i=0; i<area.length; i++) {
            if (area[i] > areaMax) {
                areaMaxIndex=i;
                areaMax = area[i];
            }
        }
        double[] defaultCenter = getDefaultCenter(ip);
        if (areaMaxIndex > -1) {
            center[0]=rt.getValue("X", areaMaxIndex);
            center[1]=rt.getValue("Y", areaMaxIndex);
        } else {
            center = defaultCenter;
        }
        double xDev = 2.*Math.abs(center[0] - defaultCenter[0])/((double)ip.getWidth());
        double yDev = 2.*Math.abs(center[1] - defaultCenter[1])/((double)ip.getWidth());
        if (xDev > maxOffset || yDev > maxOffset) {center = defaultCenter;}
        return center;
    }
    
    double[] getDefaultCenter (ImageProcessor ip) {
        double[] center = new double[2];
        center[0] = (double)ip.getWidth() / 2.;
        center[1] = (double)ip.getHeight() / 2.;
        return center;
    }
    
    String getCharForNumber(int i) {
        return i > 0 && i < 27 ? String.valueOf((char)(i + 64)) : null;
    }
}
