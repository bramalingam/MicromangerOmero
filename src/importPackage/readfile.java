package importPackage;
import ij.IJ;
import ij.gui.EllipseRoi;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.plugin.frame.RoiManager;

import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import loci.formats.MetadataTools;
import loci.formats.meta.MetadataStore;
import omero.RDouble;
import omero.RLong;
import omero.RString;
import omero.rtypes;
import omero.model.EllipseI;
import omero.model.IObject;
import omero.model.LabelI;
import omero.model.LineI;
import omero.model.PointI;
import omero.model.PolygonI;
import omero.model.PolylineI;
import omero.model.RectangleI;
import omero.model.RoiI;
import omero.model.Shape;

/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2014 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class readfile {

    /**
     * 
     * @return
     */
    public static Roi[] readFromRoiManager(){

        RoiManager manager = RoiManager.getInstance();
        Roi[] rois = manager.getRoisAsArray();
        return rois;
    }

    /**
     * 
     * @param rois (ImageJ ROI's)
     * @param imageId (Omero ImageId)
     * @return
     * @throws Exception
     */
    public List<IObject> convertToOmeroRoi(Roi[] rois, RLong imageId) throws Exception {

        //Initialize Omero ROI
        RoiI roi = null;

        if (rois == null) 
            throw new IllegalArgumentException("ROI cannot be null.");

        int cntr = 0;
        List<IObject> allRois = new ArrayList<IObject>();

        for (int i=0; i<rois.length; i++) {

            //Link the roi and the image
            Boolean nullRoi = false;
            IJ.showProgress((double)(i/rois.length));
            roi = new omero.model.RoiI();
            roi.setImage(new omero.model.ImageI(imageId, false));
            Roi ijRoi = rois[i];

            if (ijRoi.isDrawingTool()){//Checks if the given roi is a Text box/Arrow/Rounded Rectangle
                if (ijRoi.getTypeAsString().matches("Text")){
                    //add method for text support
                    storeOmeroText((TextRoi) ijRoi, roi);
                }
                else if (ijRoi.getTypeAsString().matches("Rectangle")){
                    storeOmeroRectangle((Roi) ijRoi, roi);
                }
                else {
                    nullRoi = true;
                    String type = ijRoi.getName();
                    IJ.log("ROI ID : " + type + " ROI type : " +  "Arrow (Drawing Tool) is not supported");
                }
            }
            else if (ijRoi instanceof OvalRoi) {//Check if its an oval or ellipse ROI
                storeOmeroEllipse((OvalRoi) ijRoi, roi);
            }
            else if (ijRoi instanceof Line) { //Check if its a Line or Arrow ROI
                boolean checkpoint = ijRoi.isDrawingTool();
                if (checkpoint != true){
                    storeOmeroLine((Line) ijRoi, roi);
                }
                else {
                    nullRoi = true;
                    String type = ijRoi.getName();
                    IJ.log("ROI ID : " + type + " ROI type : " +  "Arrow (Drawing Tool) is not supported");
                }
            }
            else if (ijRoi instanceof PolygonRoi || ijRoi instanceof EllipseRoi) {
                if (ijRoi.getTypeAsString().matches("Polyline") || ijRoi.getTypeAsString().matches("Freeline") || ijRoi.getTypeAsString().matches("Angle")){
                    storeOmeroPolygon((PolygonRoi) ijRoi, roi);
                }
                else if (ijRoi.getTypeAsString().matches("Point")){
                    storeOmeroPoint((PointRoi) ijRoi, roi);
                }
                else if (ijRoi.getTypeAsString().matches("Polygon") || ijRoi.getTypeAsString().matches("Freehand") || ijRoi.getTypeAsString().matches("Traced") || ijRoi.getTypeAsString().matches("Oval")){
                    storeOmeroPolygon((PolygonRoi) ijRoi, roi);
                }
            }
            else if (ijRoi instanceof ShapeRoi) {
                Roi[] subRois = ((ShapeRoi) ijRoi).getRois();
                for (int q=0; q<subRois.length; q++) {

                    Roi ijShape = subRois[q];

                    if (ijShape instanceof Line) {
                        boolean checkpoint = ijShape.isDrawingTool();
                        if (checkpoint != true){
                            storeOmeroLine((Line) ijShape, roi);
                        }
                        else {
                            nullRoi = true;
                            String type1 = ijShape.getName();
                            IJ.log("ROI ID : " + type1 + " ROI type : " + "Arrow (DrawingTool) is not supported");
                        }
                    }
                    else if (ijShape instanceof OvalRoi) {
                        storeOmeroEllipse((OvalRoi) ijShape, roi);
                    }
                    else if (ijShape instanceof PolygonRoi || ijShape instanceof EllipseRoi) {
                        if (ijShape.getTypeAsString().matches("Polyline") || ijShape.getTypeAsString().matches("Freeline") || ijShape.getTypeAsString().matches("Angle")){
                            storeOmeroPolygon((PolygonRoi) ijShape, roi);
                        }
                        else if (ijShape.getTypeAsString().matches("Point")){
                            storeOmeroPoint((PointRoi) ijShape, roi);
                        }
                        else if (ijShape.getTypeAsString().matches("Polygon") || ijShape.getTypeAsString().matches("Freehand") || ijShape.getTypeAsString().matches("Traced") || ijShape.getTypeAsString().matches("Oval")){
                            storeOmeroPolygon((PolygonRoi) ijShape, roi);
                        }
                    }
                    else if (ijShape.getTypeAsString().matches("Rectangle")){
                        storeOmeroRectangle((Roi) ijShape, roi);
                    }
                    else {
                        nullRoi = true;
                        String type = ijShape.getName();
                        IJ.log("ROI ID : " + type + " ROI type : " + ijShape.getTypeAsString() + "is not supported");
                    }
                }
            }

            else if(ijRoi.getTypeAsString().matches("Rectangle")){//Check if its a Rectangle or Rounded Rectangle ROI
                storeOmeroRectangle((Roi) ijRoi, roi);
            }
            else {
                nullRoi = true;
                String type = ijRoi.getName();
                IJ.log("ROI ID : " + type + " ROI type : " + ijRoi.getTypeAsString() + "is not supported");
            }

            if (nullRoi == false){
                allRois.add(roi);
                cntr++;
            }
        }

        return allRois;
    }

    /**
     * 
     * @param ijRoi
     * @param roi
     */
    private void storeOmeroText(TextRoi ijRoi, RoiI roi) {
        // TODO Auto-generated method stub
        LabelI text = new LabelI();
        text.setTextValue(rstring(ijRoi.getText())); 
        text.setX(rdouble(ijRoi.getPolygon().getBounds().getX()));
        text.setY(rdouble(ijRoi.getPolygon().getBounds().getY()));
        roi.addShape(text);
    }

    /**
     * 
     * @param roiType
     * @param roi
     */
    private static void storeOmeroPoint(PointRoi roiType, RoiI roi) {

        int[] x = roiType.getPolygon().xpoints;
        int[] y = roiType.getPolygon().ypoints;

        for (int cntr=0 ; cntr<x.length; cntr++){
            PointI point = new PointI();
            point.setCx(rdouble(x[cntr]));
            point.setCy(rdouble(y[cntr]));
            roi.addShape(point);
        }

    }

    /**
     * 
     * @param roiType
     * @param roi
     */
    private static void storeOmeroPolygon(PolygonRoi roiType, RoiI roi) {

        int[] x = roiType.getPolygon().xpoints;
        int[] y = roiType.getPolygon().ypoints;

        String st1 = roiType.getTypeAsString();

        String points = "1";
        for (int i=0 ; i<x.length ; i++){

            if(i==0){
                points = (x[i] + "," + y[i]);
            }else{
                points= (points + " " + x[i] + "," + y[i]);
            }

        }

        Shape shape = null;
        if(st1.matches("Polyline") || st1.matches("Freeline") || st1.matches("Angle")){
            PolylineI polyline = new PolylineI();
            polyline.setPoints(rtypes.rstring(points));
            shape = polyline;
        }else if (st1.matches("Polygon") || st1.matches("Freehand") || st1.matches("Traced") || st1.matches("Oval")){
            PolygonI polygon = new PolygonI();
            polygon.setPoints(rtypes.rstring(points));
            polygon.getPoints();
            shape=polygon;
        }
        if (shape!=null){
            roi.addShape(shape);
        }
    }

    /**
     * 
     * @param roiType
     * @param roi
     */
    private static void storeOmeroEllipse(OvalRoi roiType, RoiI roi) {

        Rectangle vnRectBounds = roiType.getPolygon().getBounds();
        int x = vnRectBounds.x;
        int y = vnRectBounds.y;
        int rx = vnRectBounds.width;
        int ry = vnRectBounds.height;

        EllipseI ellipse = new EllipseI();
        ellipse.setCx(rdouble(x + rx/2));
        ellipse.setCy(rdouble(y + ry/2));
        ellipse.setRx(rdouble(rx/2));
        ellipse.setRy(rdouble(ry/2));
        roi.addShape(ellipse);
    }

    /**
     * 
     * @param roiType
     * @param roi
     */
    private static void storeOmeroRectangle(Roi roiType, RoiI roi) {

        Rectangle vnRectBounds = roiType.getPolygon().getBounds();
        int x = vnRectBounds.x;
        int y = vnRectBounds.y;
        int w = vnRectBounds.width;
        int h = vnRectBounds.height;

        //omero rectangle params
        RectangleI rectangle = new RectangleI();
        rectangle.setX(rdouble(x));
        rectangle.setY(rdouble(y));
        rectangle.setWidth(rdouble(w));
        rectangle.setHeight(rdouble(h));
        roi.addShape(rectangle);
    }

    /**
     * 
     * @param roiType
     * @param roi
     */
    private static void storeOmeroLine(Line roiType, RoiI roi)
    {
        int[] xcoords = roiType.getPolygon().xpoints;
        int[] ycoords = roiType.getPolygon().ypoints;

        //omero straight line params
        LineI line = new LineI();
        line.setX1(rdouble(xcoords[0]));
        line.setX2(rdouble(xcoords[1]));
        line.setY1(rdouble(ycoords[0]));
        line.setY2(rdouble(ycoords[1]));
        roi.addShape(line);
    }

    private static RDouble rdouble(int i) {
        // TODO Auto-generated method stub
        return null;
    }

    private static RString rstring(String i) {
        // TODO Auto-generated method stub
        return null;
    }

    private RDouble rdouble(double x) {
        // TODO Auto-generated method stub
        return null;
    }

}

