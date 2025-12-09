package com.webtool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/**
 * Jsoup????html??????????JQuery????????
 * 
 * @author chixh
 *
 */
public class HtmlReader {
	String filename=null;
	ArrayList<String> textlist=new ArrayList<String>() ;
	ArrayList<String> urllist=new ArrayList<String>();
	protected List<List<String>> data = new LinkedList<List<String>>();
 
	/**
	 * ???value?
	 *
	 * @return
	 */

	 public HtmlReader(String resources_location){
		this.filename=resources_location;
	 }
	public static String getValue(Element e) {
		return e.attr("value");
	}
 
	/**
	 * ???
	 * <tr>
	 * ??
	 * </tr>
	 * ???????
	 * 
	 * @param e
	 * @return
	 */
	public static String getText(Element e) {
		return e.text();
	}
 
	/**
	 * ???????id????,??????html???id??
	 * 
	 * @param body
	 * @param id
	 * @return
	 */
	public static Element getID(String body, String id) {
		Document doc = Jsoup.parse(body);
		// ????#id????
		Elements elements = doc.select("#" + id);
		// ????????
		return elements.first();
	}
 
	/**
	 * ???????class????
	 * 
	 * @param body
	 * @return
	 */
	public static Elements getClassTag(String body, String classTag) {
		Document doc = Jsoup.parse(body);
		// ????#id????
		return doc.select("." + classTag);
	}
 
	/**
	 * ???tr????????
	 * 
	 * @param e
	 * @return
	 */
	public static Elements getTR(Element e) {
		return e.getElementsByTag("tr");
	}
 
	/**
	 * ???td????????
	 * 
	 * @param e
	 * @return
	 */
	public static Elements getTD(Element e) {
		return e.getElementsByTag("td");
	}
	/**
	 * ????????
	 * @param table
	 * @return
	 */
	public static List<List<String>> getTables(Element table){
		List<List<String>> data = new ArrayList<>();
		
		for (Element etr : table.select("tr")) {
			List<String> list = new ArrayList<>();
			for (Element etd : etr.select("td")) {
				String temp = etd.text();
				//?????????????
				list.add(temp);
			}
			//???????
			data.add(list);
		}
		return data;
	}
	/**
	 * ??html???
	 * @return
	 */
	public String readHtml() throws Exception{
		FileInputStream fis = null;
		StringBuffer sb = new StringBuffer();
		String str=new String((ByteUtils.filetoBytes(filename)));
//     	System.out.println(str);
//		try {
//			fis = new FileInputStream(file);
//			byte[] bytes = new byte[1024];
//			while (-1 != fis.read(bytes)) {
//				sb.append(new String(bytes));
//
//			}
//			System.out.println("non-message");
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} finally {
//			try {
//				fis.close();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//		}
	
		return str;
	}
	public String getElement(String selectType,String []indexStr) throws Exception {
		Document doc = Jsoup.parse(readHtml());
		String returnStr=null;
		// ???html?????
		if(selectType.equals("singleselect")){
			System.out.println("finish step three");
		    returnStr=doc.select(indexStr[0]).text();	
			System.out.println( returnStr);
	}else if(selectType.equals("selectattr")){
		// ???????????
		returnStr=doc.select(indexStr[0]).select(indexStr[1]).attr(indexStr[2]);
	}
		// ????????????
         return returnStr;
}

   public void getText(String selectType,String [] indexStr) throws Exception {
	Document doc = Jsoup.parse(readHtml());
	if(selectType.equals("multiselect")){
		for(int i=0;i<indexStr.length;i++){
		int count=0;
		Elements element = doc.select(indexStr[i]);
		for (Element e : element) {
			count=count+1;
			String elestr=e.text();
			if(!elestr.equals(""))
			textlist.add(elestr);
		}
	}
}
   }
	public  void  getAddress(String selectType,String []indexStr) throws Exception {
		Document doc = Jsoup.parse(readHtml());
		if(selectType.equals("multiselect")){
			for(int i=0;i<indexStr.length;i++){
			int count=0;
			Elements element = doc.select(indexStr[i]);
			System.out.println("elements"+element);
			for (Element e : element) {
				count=count+1;
				String elesrc=e.attr("src");
				String elehref=e.attr("href");
				if(!elesrc.equals(""))
				urllist.add(elesrc);
				if(!elehref.equals(""))
				urllist.add(elehref);
			}

		}
		}
}

}