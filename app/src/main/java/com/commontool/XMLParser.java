package com.commontool;
import com.sqltool.FieldInfo;
import com.sqltool.TableInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


public class XMLParser {

public String rootElement;
public String nodeElement;
public String []metadata;
public String [][]values;




            /** Auxiliary: Save the Document to a file
              * @param doc
              * @throws Exception
              */
    public static void saveToFile(Document doc, String filePath) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // 使用 try-with-resources 确保流关闭
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fos);
            transformer.transform(source, result);
        }catch (FileNotFoundException e) {
            System.err.println("文件无法创建或打开: " + filePath);
            e.printStackTrace();
            // 根据业务逻辑决定如何处理（如提示用户、返回错误码等）
        } catch (IOException e) {
            // 处理其他 IO 异常（如写入时出错）
            e.printStackTrace();
        }
        System.out.println("XML文件保存成功: " + filePath);
    }


    /**
     * Auxiliary: Load Document from File
     * @param filePath
     * @return
     * @throws Exception
     */
    public static Document loadFromFile(String filePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(filePath));
    }



}





