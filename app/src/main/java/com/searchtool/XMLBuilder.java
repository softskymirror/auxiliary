package com.searchtool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class XMLBuilder {

public String rootElement;
public String nodeElement;
public String []metadata;
public String [][]values;

    public static void generateXml(File file, String root_element,String node_element,String [] attributename,String [][] infos) {
    
        try {

                Document doc = generateXml(root_element,node_element,attributename,infos);//
                outputXml(doc, file);// ���ļ������ָ����·��

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("�����쳣");
        }  
    }  
  
    /** 
              * ��XML�ļ������ָ����·�� 
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
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");// �����ĵ��Ļ���������
        pw = new PrintWriter(new FileOutputStream(file));
        if (pw.checkError()) {
           pw = new PrintWriter(new FileOutputStream(file));
        }
        StreamResult result = new StreamResult(pw);
        transformer.transform(source, result);  
        System.out.println("����XML�ļ��ɹ�!�ļ�λ��:"+file.getPath());
        pw.flush();

    }  
    /** 
              * ����XML�ļ� 

              * @return 
              */  
    public static Document generateXml(String root_element,String node_element,String []attributename,String [][] infos) {
        Document doc = null;  
        Element docroot = null;
        try {  
            DocumentBuilderFactory factory=DocumentBuilderFactory
                    .newInstance();  
            DocumentBuilder builder=factory.newDocumentBuilder();
            doc=builder.newDocument();  
            docroot=doc.createElement(root_element);
            docroot.setAttribute("xmlns:android","http://schemas.android.com/apk/res/android");
            doc.appendChild(docroot);
        } catch (Exception e) {  
            e.printStackTrace();  
            return null;// ��������쳣����������ִ��  
        }
        if(infos.length!=0) {
            for (int l = 0; l < infos.length; l++) {
                Element docnode = doc.createElement(node_element);
                for(int i=0;i<attributename.length;i++){
                docnode.setAttribute(attributename[i], infos[l][i]);
            }
                docroot.appendChild(docnode);
            }   
        }
        return doc;  
        }

    }
    




