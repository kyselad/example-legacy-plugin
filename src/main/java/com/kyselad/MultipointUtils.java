/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kyselad;

import ij.IJ;
import ij.io.OpenDialog;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author dtk6
 */
public class MultipointUtils {

    public MultipointUtils() {

    }

    public Coordinate[] readNDCoordinateFile(File f) {
        ArrayList<Coordinate> coordList = new ArrayList<Coordinate>();
        try {
            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(f);
            Node node = doc.getDocumentElement();
            while (node.getNodeName() != "no_name") {
                node = node.getFirstChild();
            }
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                if (!nodeList.item(i).getNodeName().contains("point")) {
                    continue;
                }
                NodeList pointNodeList = nodeList.item(i).getChildNodes();
                boolean foundX = false, foundY = false, foundName = false;
                double x = 0., y = 0.;
                String name = null;
                for (int j = 0; j < pointNodeList.getLength(); j++) {

                    if (pointNodeList.item(j).getNodeName().contains("strName")) {
                        NamedNodeMap attributes = pointNodeList.item(j).getAttributes();
                        name = attributes.getNamedItem("value").getNodeValue();
                        foundName = true;
                    }
                    if (pointNodeList.item(j).getNodeName().contains("dXPosition")) {
                        NamedNodeMap attributes = pointNodeList.item(j).getAttributes();
                        String xPosStr = attributes.getNamedItem("value").getNodeValue();
                        x = Double.parseDouble(xPosStr);
                        foundX = true;
                    }
                    if (pointNodeList.item(j).getNodeName().contains("dYPosition")) {
                        NamedNodeMap attributes = pointNodeList.item(j).getAttributes();
                        String yPosStr = attributes.getNamedItem("value").getNodeValue();
                        y = Double.parseDouble(yPosStr);
                        foundY = true;
                    }
                }
                if (foundX && foundY && foundName) {
                    coordList.add(new Coordinate(x, y, name));
                };
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return coordList.toArray(new Coordinate[coordList.size()]);

    }

    public void writeNDCoordinateFile(Coordinate[] coordArray, File f) {

        try {

            DocumentBuilderFactory dbFactory
                    = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.newDocument();

            // root element
            Element variantElement = doc.createElement("variant");
            doc.appendChild(variantElement);
            Attr versionAttr = doc.createAttribute("version");
            versionAttr.setValue("1.0");
            variantElement.setAttributeNode(versionAttr);

            Element noNameElement = doc.createElement("no_name");
            variantElement.appendChild(noNameElement);
            Attr runtypeAttr = doc.createAttribute("runtype");
            runtypeAttr.setValue("CLxListVariant");
            noNameElement.setAttributeNode(runtypeAttr);

            Element includezElt = doc.createElement("bIncludeZ");
            noNameElement.appendChild(includezElt);
            Attr includezRuntypeAttr = doc.createAttribute("runtype");
            includezRuntypeAttr.setValue("bool");
            includezElt.setAttributeNode(includezRuntypeAttr);
            Attr includezValueAttr = doc.createAttribute("value");
            includezValueAttr.setValue("false");
            includezElt.setAttributeNode(includezValueAttr);

            Element pfsenabledElt = doc.createElement("bPFSEnabled");
            noNameElement.appendChild(pfsenabledElt);
            Attr pfsenabledRuntypeAttr = doc.createAttribute("runtype");
            pfsenabledRuntypeAttr.setValue("bool");
            pfsenabledElt.setAttributeNode(pfsenabledRuntypeAttr);
            Attr pfsenabledValueAttr = doc.createAttribute("value");
            pfsenabledValueAttr.setValue("true");
            pfsenabledElt.setAttributeNode(pfsenabledValueAttr);

            double pfsOffset = IJ.getNumber("PFS offset", 4715.d);

            for (int i = 0; i < coordArray.length; i++) {
                Coordinate c = coordArray[i];
                //double newPfs = ((c.y/18000) * 100) + pfsOffset;
                addXmlPoint(
                        c.x,
                        c.y,
                        pfsOffset,
                        i,
                        c.name,
                        doc,
                        noNameElement
                );
            }

            // write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);

            StreamResult result = new StreamResult(f);
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addXmlPoint(
            double x,
            double y,
            double offset,
            int index,
            String name,
            Document doc,
            Element parentElement) {
        Element elt = doc.createElement("point" + String.format("%05d", index));
        parentElement.appendChild(elt);
        Attr baseAttr = doc.createAttribute("runtype");
        baseAttr.setValue("NDSetupMultipointListItem");
        elt.setAttributeNode(baseAttr);

        Element eltChecked = doc.createElement("bChecked");
        elt.appendChild(eltChecked);
        Attr checkedRuntypeAttr = doc.createAttribute("runtype");
        checkedRuntypeAttr.setValue("bool");
        eltChecked.setAttributeNode(checkedRuntypeAttr);
        Attr checkedValueAttr = doc.createAttribute("value");
        checkedValueAttr.setValue("true");
        eltChecked.setAttributeNode(checkedValueAttr);

        Element eltName = doc.createElement("strName");
        elt.appendChild(eltName);
        Attr nameRuntypeAttr = doc.createAttribute("runtype");
        nameRuntypeAttr.setValue("CLxStringW");
        eltName.setAttributeNode(nameRuntypeAttr);
        Attr nameValueAttr = doc.createAttribute("value");
        nameValueAttr.setValue(name);
        eltName.setAttributeNode(nameValueAttr);

        Element eltXPos = doc.createElement("dXPosition");
        elt.appendChild(eltXPos);
        Attr xPosRuntypeAttr = doc.createAttribute("runtype");
        xPosRuntypeAttr.setValue("double");
        eltXPos.setAttributeNode(xPosRuntypeAttr);
        Attr xPosValueAttr = doc.createAttribute("value");
        xPosValueAttr.setValue("" + x);
        eltXPos.setAttributeNode(xPosValueAttr);

        Element eltYPos = doc.createElement("dYPosition");
        elt.appendChild(eltYPos);
        Attr yPosRuntypeAttr = doc.createAttribute("runtype");
        yPosRuntypeAttr.setValue("double");
        eltYPos.setAttributeNode(yPosRuntypeAttr);
        Attr yPosValueAttr = doc.createAttribute("value");
        yPosValueAttr.setValue("" + y);
        eltYPos.setAttributeNode(yPosValueAttr);

        Element eltZPos = doc.createElement("dZPosition");
        elt.appendChild(eltZPos);
        Attr zPosRuntypeAttr = doc.createAttribute("runtype");
        zPosRuntypeAttr.setValue("double");
        eltZPos.setAttributeNode(zPosRuntypeAttr);
        Attr zPosValueAttr = doc.createAttribute("value");
        zPosValueAttr.setValue("0.0");
        eltZPos.setAttributeNode(zPosValueAttr);

        Element eltOffset = doc.createElement("dPFSOffset");
        elt.appendChild(eltOffset);
        Attr offsetRuntypeAttr = doc.createAttribute("runtype");
        offsetRuntypeAttr.setValue("double");
        eltOffset.setAttributeNode(offsetRuntypeAttr);
        Attr offsetValueAttr = doc.createAttribute("value");
        offsetValueAttr.setValue("" + offset);
        eltOffset.setAttributeNode(offsetValueAttr);

        Element eltUserdata = doc.createElement("baUserData");
        elt.appendChild(eltUserdata);
        Attr userdataRuntypeAttr = doc.createAttribute("runtype");
        userdataRuntypeAttr.setValue("CLxByteArray");
        eltUserdata.setAttributeNode(userdataRuntypeAttr);
        Attr userdataAttr = doc.createAttribute("value");
        userdataAttr.setValue("" + offset);
        eltUserdata.setAttributeNode(userdataAttr);
    }

    public static double[] rotateXY(double x, double y, double angle) {
        // Make sure angle is in radians!
        // Remember that, im ImgaeJ, image x axis increases to right, but y axis increases downward
        // Since y is inverted compared to usual Cartesian coordinates, positive angle is *clockwise* rotation
        // However, in standard Cartesian plane, positive angle is counter-clockwise rotation
        // If you calculate angles outside ImageJ using standard vector math,
        // you should get the "normal" Cartesian behavior
        double xRotated = (x * Math.cos(angle)) - (y * Math.sin(angle));
        double yRotated = (x * Math.sin(angle)) + (y * Math.cos(angle));
        return new double[]{xRotated, yRotated};
    }
}
