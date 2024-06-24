package com.spms.common.dbTool;

import com.spms.dbhsm.encryptcolumns.domain.dto.DbhsmEncryptColumnsAdd;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * <p> description: 组合Hbase.xml文件 </p>
 *
 * <p> Powered by wzh On 2024-06-17 15:16 </p>
 * <p> @author wzh [zhwang2012@yeah.net] </p>
 * <p> @version 1.0 </p>
 */

@Slf4j
public class HbaseConfigXmlUtil {

    private static final String xmlPath = "/home/hbconf/extconfig.xml";

    public static void initHbaseXmlFile(DbhsmEncryptColumnsAdd encryptColumns) {
        File file = new File(xmlPath);
        if (!file.exists()) {
            //如果不存在创建第一个节点
            createHbaseXmlFile(encryptColumns);
        } else {
            //存在追加节点
            addToHbaseXmlNode(encryptColumns);
        }
    }

    /*
     * @description 初始化第一个加密列
     * @author wzh [zhwang2012@yeah.net]
     * @date 15:19 2024/6/17
     * @param encryptColumns 加密列对象
     */
    public static void createHbaseXmlFile(DbhsmEncryptColumnsAdd encryptColumns) {
        try {
            // 创建 DocumentBuilderFactory 和 DocumentBuilder
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();

            // 创建一个新的 Document
            Document document = documentBuilder.newDocument();

            // 创建根元素 <configuration>
            Element root = document.createElement("configuration");
            document.appendChild(root);

            // 添加第一个 <table> 元素
            Element table = document.createElement("table");
            root.appendChild(table);

            // 添加 <name>student</name>
            Element name = document.createElement("name");
            name.appendChild(document.createTextNode(encryptColumns.getDbTable()));
            table.appendChild(name);

            // 添加第一个 <family>
            Element family = document.createElement("family");
            table.appendChild(family);

            // 添加 <name>info</name>
            Element familyName = document.createElement("name");
            //加密列  列族:列名
            String[] split = encryptColumns.getEncryptColumns().split(":");
            familyName.appendChild(document.createTextNode(split[0]));
            family.appendChild(familyName);

            // 添加 <Qualifier> 元素
            addQualifier(document, family, encryptColumns);

            // 创建 TransformerFactory 和 Transformer
            transformXml(document);
            log.info("xml文件生成成功！：{}", xmlPath);
        } catch (ParserConfigurationException | TransformerException e) {
            log.error("创建xml文件失败：{}", e.getMessage());
        }
    }

    public static void addToHbaseXmlNode(DbhsmEncryptColumnsAdd encryptColumns) {
        try {
            // 创建DocumentBuilderFactory实例
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 创建DocumentBuilder实例
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 解析XML文件并加载到Document对象中
            Document document = builder.parse(xmlPath);

            // 规范化XML文档
            document.getDocumentElement().normalize();
            // 获取根元素
            Element root = document.getDocumentElement();

            Element tableElement = getElementByNodeValue(document, "table", "name", encryptColumns.getDbTable());
            if (null == tableElement) {
                //如果xml中的没有该表的信息
                addTable(document, encryptColumns, root);
            } else {
                //如果节点存在，查看表中的列族是否存在XML文件中
                String[] split = encryptColumns.getEncryptColumns().split(":");
                //查询列族是否存在
                String familyName = split[0];
                Element familyElement = getElementByNodeValue(document, "family", "name", familyName);
                if (null == familyElement) {
                    //列族不存在新增列族
                    addFamily(document, encryptColumns, tableElement, familyName);
                } else {
                    Element qualifierElement = getElementByNodeValue(document, "Qualifier", "name", split[1]);
                    //防止重新添加
                    if (null == qualifierElement) {
                        //列族存在，新增加密列节点
                        addQualifier(document, familyElement, encryptColumns);
                    }
                }
            }

            //创建文件
            transformXml(document);
            log.info("xml文件更新成功！");
        } catch (Exception e) {
            log.error("追加xml文件失败：{}", e.getMessage());
        }
    }


    /*
     * @description 创建Qualifier节点
     * @author wzh [zhwang2012@yeah.net]
     * @date 15:17 2024/6/17
     * @param document 文件
     * @param family 列族节点
     * @param name 列名称
     * @param url 策略下载地址
     * @param pid 列唯一标识
     */
    private static void addQualifier(Document document, Element familyElement, DbhsmEncryptColumnsAdd encryptColumns) {
        Element qualifier = document.createElement("Qualifier");

        String[] split = encryptColumns.getEncryptColumns().split(":");
        Element nameElement = document.createElement("name");
        nameElement.appendChild(document.createTextNode(split[1]));
        qualifier.appendChild(nameElement);

        Element urlElement = document.createElement("url");
        urlElement.appendChild(document.createTextNode("http://" + encryptColumns.getIpAndPort() + "/prod-api/dbhsm/api/datahsm/v1/strategy/get"));
        qualifier.appendChild(urlElement);

        Element pidElement = document.createElement("pid");
        pidElement.appendChild(document.createTextNode(encryptColumns.getId()));
        qualifier.appendChild(pidElement);

        familyElement.appendChild(qualifier);
    }

    private static void addTable(Document document, DbhsmEncryptColumnsAdd encryptColumns, Element root) {
        Element table = document.createElement("table");
        root.appendChild(table);
        // 添加table下的name节点
        Element tableNodeName = document.createElement("name");
        tableNodeName.appendChild(document.createTextNode(encryptColumns.getDbTable()));
        table.appendChild(tableNodeName);
        //加密列  列族:列名
        String[] split = encryptColumns.getEncryptColumns().split(":");

        //创建加密列列族
        addFamily(document, encryptColumns, table, split[0]);
    }

    private static void addFamily(Document document, DbhsmEncryptColumnsAdd encryptColumns, Element familyElement, String familyNameContext) {
        // 追加 <family>节点
        Element family = document.createElement("family");
        familyElement.appendChild(family);
        Element familyName = document.createElement("name");
        //加密列  列族:列名
        familyName.appendChild(document.createTextNode(familyNameContext));
        family.appendChild(familyName);
        //创建加密列节点
        addQualifier(document, family, encryptColumns);
    }

    public static Element getElementByNodeValue(Document document, String tagName, String childTagName, String childTagValue) {
        NodeList nodeList = document.getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            NodeList childNodes = element.getElementsByTagName(childTagName);
            for (int j = 0; j < childNodes.getLength(); j++) {
                if (childNodes.item(j).getTextContent().equals(childTagValue)) {
                    return element;
                }
            }
        }
        return null;
    }

    private static void transformXml(Document document) throws TransformerException {
        // 创建 TransformerFactory 和 Transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        // 移除 XML 声明
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        // 创建 DOMSource 和 StreamResult
        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(xmlPath));

        // 将 DOM 写入文件
        transformer.transform(domSource, streamResult);
    }
}
