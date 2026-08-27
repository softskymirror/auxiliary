package com.sqltool;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;

import static com.commontool.XMLUtils.loadFromFile;
import static com.commontool.XMLUtils.saveToFile;

public class XMLForSqlConvertor {
    /**
     * 从 XML 文件加载并解析表结构信息。
     * @param filePath XML 文件路径
     * @return TableInfo 列表
     * @throws Exception 如果文件不存在或解析失败
     */
    public static List<TableInfo> parseXMLToTables(String filePath) {
        try {
            return xmlParseToTables(loadFromFile(filePath));
        } catch (Exception e) {
            throw new RuntimeException("解析XML文件失败: " + filePath, e);
        }
    }

    public static void saveTablesToXml(String filePath, List<TableInfo> tables) {
        try {
            Document doc = tablesParseToXml(tables);
            saveToFile(doc, filePath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("保存文件失败");
        }
    }

    /**
     Parse the list of TableInfo TO the XML Document
     */
    public static Document tablesParseToXml(List<TableInfo> tables) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element databaseElem = doc.createElement("database");
        doc.appendChild(databaseElem);

        for (TableInfo table : tables) {
            Element tableElem = doc.createElement("table");
            tableElem.setAttribute("name", table.getTableName());
            if (table.getCharacterSet() != null)
                tableElem.setAttribute("characterSet", table.getCharacterSet());
            if (table.getCollate() != null)
                tableElem.setAttribute("collate", table.getCollate());
            if (table.getEngine() != null)
                tableElem.setAttribute("engine", table.getEngine());

            // 添加字段
            for (FieldInfo field : table.getFields()) {
                Element fieldElem = doc.createElement("field");
                fieldElem.setAttribute("name", field.getColumnName());
                fieldElem.setAttribute("dataType", String.valueOf(field.getDataType())); // 存常量值

                if (field.getLength() != null)
                    fieldElem.setAttribute("length", String.valueOf(field.getLength()));
                if (field.getPrecision() != null)
                    fieldElem.setAttribute("precision", String.valueOf(field.getPrecision()));
                if (field.getScale() != null)
                    fieldElem.setAttribute("scale", String.valueOf(field.getScale()));

                fieldElem.setAttribute("unsigned", String.valueOf(field.isUnsigned()));
                fieldElem.setAttribute("notNull", String.valueOf(field.isNotNull()));

                if (field.getDefaultValue() != null)
                    fieldElem.setAttribute("defaultValue", field.getDefaultValue());

                fieldElem.setAttribute("autoIncrement", String.valueOf(field.isAutoIncrement()));

                tableElem.appendChild(fieldElem);
            }

            // 添加主键列表
            if (table.getPrimaryKeys() != null && !table.getPrimaryKeys().isEmpty()) {
                Element pkElem = doc.createElement("primaryKeys");
                for (String pk : table.getPrimaryKeys()) {
                    Element keyElem = doc.createElement("key");
                    keyElem.setTextContent(pk);
                    pkElem.appendChild(keyElem);
                }
                tableElem.appendChild(pkElem);
            }

            databaseElem.appendChild(tableElem);
        }
        return doc;
    }

    /** Parse the list of TableInfo from the XML Document
     * @param doc
     * @throws Exception
     */
    public static List<TableInfo> xmlParseToTables(Document doc) {
        List<TableInfo> tables = new ArrayList<>();
        Element databaseElem = doc.getDocumentElement();
        NodeList tableNodes = databaseElem.getElementsByTagName("table");

        for (int i = 0; i < tableNodes.getLength(); i++) {
            Element tableElem = (Element) tableNodes.item(i);
            TableInfo table = new TableInfo();
            table.setTableName(tableElem.getAttribute("name"));

            if (tableElem.hasAttribute("characterSet"))
                table.setCharacterSet(tableElem.getAttribute("characterSet"));
            if (tableElem.hasAttribute("collate"))
                table.setCollate(tableElem.getAttribute("collate"));
            if (tableElem.hasAttribute("engine"))
                table.setEngine(tableElem.getAttribute("engine"));

            // 解析字段
            List<FieldInfo> fields = new ArrayList<>();
            NodeList fieldNodes = tableElem.getElementsByTagName("field");
            for (int j = 0; j < fieldNodes.getLength(); j++) {
                Element fieldElem = (Element) fieldNodes.item(j);
                FieldInfo field = new FieldInfo();
                field.setColumnName(fieldElem.getAttribute("name"));

                int dataType = Integer.parseInt(fieldElem.getAttribute("dataType"));
                field.setDataType(dataType); // 假设 setDataType(int) 存在

                if (fieldElem.hasAttribute("length"))
                    field.setLength(Integer.parseInt(fieldElem.getAttribute("length")));
                if (fieldElem.hasAttribute("precision"))
                    field.setPrecision(Integer.parseInt(fieldElem.getAttribute("precision")));
                if (fieldElem.hasAttribute("scale"))
                    field.setScale(Integer.parseInt(fieldElem.getAttribute("scale")));

                field.setUnsigned(Boolean.parseBoolean(fieldElem.getAttribute("unsigned")));
                field.setNotNull(Boolean.parseBoolean(fieldElem.getAttribute("notNull")));

                if (fieldElem.hasAttribute("defaultValue"))
                    field.setDefaultValue(fieldElem.getAttribute("defaultValue"));

                field.setAutoIncrement(Boolean.parseBoolean(fieldElem.getAttribute("autoIncrement")));

                fields.add(field);
            }
            table.setFields(new ArrayList<>(fields)); // 假设 setFields 接受 ArrayList

            // 解析主键
            NodeList pkNodes = tableElem.getElementsByTagName("primaryKeys");
            if (pkNodes.getLength() > 0) {
                Element pkElem = (Element) pkNodes.item(0);
                NodeList keyNodes = pkElem.getElementsByTagName("key");
                List<String> primaryKeys = new ArrayList<>();
                for (int k = 0; k < keyNodes.getLength(); k++) {
                    primaryKeys.add(keyNodes.item(k).getTextContent());
                }
                table.setPrimaryKeys(primaryKeys);
            }

            tables.add(table);
        }
        return tables;
    }

}
