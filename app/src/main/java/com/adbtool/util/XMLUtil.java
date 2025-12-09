package com.adbtool.util;

import com.yeetor.adb.AdbDevice;
import com.yeetor.androidcontrol.DeviceInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class XMLUtil{
public String rootElement;
public String nodeElement;
public String []metadata;
public String [][]values;
public static final int Device_Xml=1;
public static final int Group_Xml=2;


    /**
     *自定义封装XML文件
     * @param file
     */
    public static void generateXml(File file,int XML_Type,String root_element, XMLRange range) {
        try {
            Document doc=null;
            switch (XML_Type) {
                case Device_Xml: doc = generateDevicesXml(root_element, range);// 生成XML文件
                case Group_Xml: doc = generateGroupXml(root_element, range);// 生成XML文件
                outputXml(doc, file);//将文件输出到指定的路径
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("出现异常");
        }
    }


    /**
     *将XML数据转化为ArrayList<DeviceInfo>对象
     */
    public static ArrayList<DeviceInfo> executeDeviceInfoXML(File file,String root_element){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        ArrayList<DeviceInfo> infos=new ArrayList();
        try{
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file.getPath());
            NodeList node = document.getElementsByTagName(root_element);
            for(int i = 0; i < node.getLength(); i++){
                String sn=null;int width=0;int height=0;String quanlity = null;
                System.out.println("--------第" + (i+1) + "个设备----------");
                Element ele = (Element)node.item(i);
                DeviceInfo info;
                NodeList childNodes= ele.getChildNodes();
                for(int j = 0; j < childNodes.getLength(); j++){
                    Node n = childNodes.item(j);
                    switch (n.getNodeName()){
                        case "sn": sn=n.getTextContent();
                        case "width":width=Integer.parseInt(n.getTextContent());
                        case "height":height=Integer.parseInt(n.getTextContent());
                        case "quanlity":quanlity=n.getTextContent();
                    }
                }
                info=new DeviceInfo(sn,width,height,quanlity,"");
                infos.add(info);
            }
        }catch (ParserConfigurationException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (SAXException e){
            e.printStackTrace();
        }
        return infos;
    }

    /**
     *将XML数据转化为ArrayList<APPInfo>对象
     */
    public static ArrayList<DeviceInfo> executeAPPInfoXML(File file,String root_element){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        ArrayList<DeviceInfo> infos=new ArrayList();
        try{
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(file.getPath());
            NodeList node = document.getElementsByTagName(root_element);
            for(int i = 0; i < node.getLength(); i++){
                String sn=null;int width=0;int height=0;String quanlity = null;
                System.out.println("--------第" + (i+1) + "本书----------");
                Element ele = (Element)node.item(i);
                DeviceInfo info;
                NodeList childNodes= ele.getChildNodes();
                for(int j = 0; j < childNodes.getLength(); j++){
                    Node n = childNodes.item(j);
                    switch (n.getNodeName()){
                        case "sn": sn=n.getTextContent();
                        case "width":width=Integer.parseInt(n.getTextContent());
                        case "height":height=Integer.parseInt(n.getTextContent());
                        case "quanlity":quanlity=n.getTextContent();
                    }
                }
                info=new DeviceInfo(sn,width,height,quanlity,null);
                infos.add(info);
            }
        }catch (ParserConfigurationException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (SAXException e){
            e.printStackTrace();
        }
        return infos;
    }

    /**
              * 将XML文件输出到指定的路径
              * @param doc
              * @param file
              * @throws Exception
              */
    private static void outputXml(Document doc, File file)
            throws Exception {
        PrintWriter pw;
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        DOMSource source = new DOMSource(doc);
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");// 设置文档的换行与缩进
        pw = new PrintWriter(new FileOutputStream(file));
        if (pw.checkError()) {
           pw = new PrintWriter(new FileOutputStream(file));
        }
        StreamResult result = new StreamResult(pw);
        transformer.transform(source, result);
        System.out.println("生成XML文件成功!文件位置:"+file.getPath());
        pw.flush();

    }

      /**
        * 生成Devices XML文件
        * @return
        */
    public static Document generateDevicesXml(String root_element,XMLRange range) {
        Document doc = null;
        Element docroot = null;
        Element lowLevelNode = null, highLevelNode = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            docroot = doc.createElement(root_element);
            //docroot.setAttribute("xmlns:android","http://schemas.android.com/apk/res/android");
            doc.appendChild(docroot);
            for (int i = 0; i <= range.getNodesArraySize(); i++) {
                for (int j = 0; j <= range.getNodesSize(i); j++) {
                    //按级别构建节点
                    if (i == 0)
                        docroot.appendChild(lowLevelNode = doc.createElement(range.getNode(i, j)));
                    else if (i == range.getNodesArraySize()) {
                        for (int x = 0; x <= range.getDeviceTextsArraySize(); x++) {
                            for (int y = 0; y <= range.getDeviceTextsSize(x); y++) {
                                DeviceInfo info = range.getDeviceInfo(x, y);
                                String node = range.getNode(i, j);
                                Element childnode = null;
                                switch (node) {
                                    case AdbDevice.SERIAL_NUMBER:
                                        highLevelNode.appendChild(childnode = doc.createElement(node));
                                        childnode.setTextContent(info.getSn());
                                    case AdbDevice.SCREEN_SIZE:
                                        highLevelNode.appendChild(childnode = doc.createElement(node));
                                        childnode.setTextContent(info.getWidth() + "x" + info.getHeight());
                                    case AdbDevice.DEVICE_CODE:
                                        highLevelNode.appendChild(childnode = doc.createElement(node));
                                        childnode.setTextContent(info.getModel());
                                }
                            }
                        }
                    }
                }
                highLevelNode = lowLevelNode;
                //为节点分别设置属性内容
                for (int z = 0; z <= range.getAttributesArraySize(); z++) {
                    for (int v = 0; v <= range.getAttributesSize(z); v++) {
                        highLevelNode.setAttribute(range.getAttribute(z, v), range.getValue(z, v));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

    /**
     * 生成Devices XML文件
     * @return
     */
    public static Document generateGroupXml(String root_element,XMLRange range) {
        Document doc = null;
        Element docroot = null;
        Element lowLevelNode = null, highLevelNode = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
            docroot = doc.createElement(root_element);
            //docroot.setAttribute("xmlns:android","http://schemas.android.com/apk/res/android");
            doc.appendChild(docroot);
            for (int i = 0; i <= range.getNodesArraySize(); i++) {
                for (int j = 0; j <= range.getNodesSize(i); j++) {
                    //按级别构建节点
                    if (i == 0)
                        docroot.appendChild(lowLevelNode = doc.createElement(range.getNode(i, j)));
                    else if (i == range.getNodesArraySize()) {
                        for (int x = 0; x <= range.getDeviceTextsArraySize(); x++) {
                            for (int y = 0; y <= range.getDeviceTextsSize(x); y++) {
                                DeviceInfo info = range.getDeviceInfo(x, y);
                                String node = range.getNode(i, j);
                                Element childnode = null;
                                switch (node) {
                                    case AdbDevice.SERIAL_NUMBER:
                                        highLevelNode.appendChild(childnode = doc.createElement(node));
                                        childnode.setTextContent(info.getSn());
                                    case AdbDevice.SCREEN_SIZE:
                                        highLevelNode.appendChild(childnode = doc.createElement(node));
                                        childnode.setTextContent(info.getWidth() + "x" + info.getHeight());
                                    case AdbDevice.DEVICE_CODE:
                                        highLevelNode.appendChild(childnode = doc.createElement(node));
                                        childnode.setTextContent(info.getModel());
                                }
                            }
                        }
                    }
                }
                highLevelNode = lowLevelNode;
                //为节点分别设置属性内容
                for (int z = 0; z <= range.getAttributesArraySize(); z++) {
                    for (int v = 0; v <= range.getAttributesSize(z); v++) {
                        highLevelNode.setAttribute(range.getAttribute(z, v), range.getValue(z, v));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }

}


    




