package com.webtool;


import com.webtool.captureutils.EasyHttpProxyServer;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;


public class HtmlWatcher extends JFrame {
   Object [][] tabledatas;
   static String [] tablesnames;
   static String [] resources;

java.net.URLConnection Uc;
   boolean isChuncked=false;
   Object downurl;
   String filepath,downloadfilename;
   String htmlfilepath="C:\\Program Files\\HtmlWatcher\\temp\\html\\";
   String mediafilepath="C:\\Program Files\\HtmlWatcher\\temp\\media\\";
   HtmlReader htmlreader;
   GlobalSettings globalSettings=new GlobalSettings();//??????????????????
   StringBuffer htmlResoucesInfo=new StringBuffer();
   String [] key={"Accept","Accept-Encoding","Accept-Encoding","Accept-Language","User-Agent"};
   byte []  mediaResouces;
   JButton requestUrl=new JButton();
   JButton advancedSettings=new JButton();
   JButton autoRequest=new JButton();
   JButton autoCaptures=new JButton();
   JLabel forwriteurl=new JLabel();
   JLabel forshowurlinfo=new JLabel();
   JLabel forshowtextinfo=new JLabel();
   JLabel forshowrunstate=new JLabel();
   JLabel title=new JLabel();
   JTextArea forwriteaddress=new JTextArea();
   JTextArea htmlResourceField=new JTextArea();
   JTextArea applicationLogOutput=new JTextArea();
   JTable jtable=new JTable();
   JPanel headStatePanel=new JPanel();
   JScrollPane fortableshow=new JScrollPane(jtable);
   JScrollPane forraddressshow=new JScrollPane(forwriteaddress);
   JScrollPane forresourcesshow=new JScrollPane(htmlResourceField);
   JScrollPane forlogoutput=new JScrollPane(applicationLogOutput);

   class tablelists extends AbstractTableModel{
        String [] tablesnames;
        Object [][] tabledatas;
        public tablelists(String []names,Object [][]datas){
         this.tablesnames=names;
         this.tabledatas=datas;
        }
        public int getColumnCount() { return tablesnames.length; }
        public int getRowCount() { return tabledatas.length;}
        public String getColumnName(int column)  {return tablesnames[column]; }
        public Object getValueAt(int row, int col) { return tabledatas[row][col];}
   }
public HtmlWatcher(){
    System.setProperty("http.proxyHost", "localhost");
System.setProperty("http.proxyPort", "8888");
System.setProperty("https.proxyHost", "localhost");
System.setProperty("https.proxyPort", "8888");
try {
    new SslUtils().ignoreSsl();
} catch (Exception e2) {
    // TODO Auto-generated catch block
    e2.printStackTrace();
}
 ViewAdapter viewadapter=new ViewAdapter();
 try{
 searchDirectory();
}catch(Exception e){
    e.printStackTrace();
}
viewadapter.generateJButton(advancedSettings,800,60,150,25,"settings",new ActionListener(){
    public void actionPerformed(ActionEvent e){
        if(globalSettings!=null)
       globalSettings.open();
       else
       globalSettings.setVisible(true);
    }
});

viewadapter.generateJButton(autoRequest,980,60,150,25,"AutoRequest",new ActionListener(){
    public void actionPerformed(ActionEvent e){
if(globalSettings.RequestLists.size()==0){
    JOptionPane.showMessageDialog(null,"Cookie???????");
}


 for(int i=0;i<globalSettings.RequestLists.size();i++){

    System.out.println("http"+globalSettings.RequestLists.get(i).getUrl().startsWith("http:"));
    System.out.println("https"+globalSettings.RequestLists.get(i).getUrl().startsWith("https:"));
if(globalSettings.RequestLists.get(i).getUrl().startsWith("http:")){
       Uc=autoRequestHttp(globalSettings.RequestLists.get(i));
       applicationLogOutput.append("??????HTTP?????"+i+"??,???:"+globalSettings.RequestLists.get(i).getUrl().trim()+"\r\n");
    } else if(globalSettings.RequestLists.get(i).getUrl().startsWith("https:")){
       Uc= autoRequestHttps(globalSettings.RequestLists.get(i));
       applicationLogOutput.append("??????HTTPS?????"+i+"??,???:"+globalSettings.RequestLists.get(i).getUrl().trim()+"\r\n");
    }
    System.out.println(globalSettings.RequestLists.size());
    if(i==globalSettings.RequestLists.size()-1){
        System.out.println("getHeaders");
        saveFile(Uc);
    }
    applicationLogOutput.append("??"+i+"????????"+"\r\n");
    }

}
});
    viewadapter.generateJButton(autoCaptures, 1080, 60, 150, 25, "AutoCapture", new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            EasyHttpProxyServer.startServer(new String[]{"3666"});
        }
    });

 viewadapter.generateJButton(requestUrl,600,60,150,25,"connect",new ActionListener(){
    public void actionPerformed(ActionEvent e){
        if(forwriteaddress.getText().equals("")){
            applicationLogOutput.append("????????????"+"\r\n");
    }else if(forwriteaddress.getText().startsWith("http")) {
            applicationLogOutput.append("??????????????" + forwriteaddress.getText() + "\r\n");
            Uc=getHttpConnection(forwriteaddress.getText(),globalSettings.ConnectionSettings,globalSettings.ConnectionValues);
          try {
              getTableData(saveFile(Uc));
          }catch (Exception e1){
              e1.printStackTrace();
          }
            jtable.setModel(new tablelists(tablesnames, tabledatas));
        }else if(forwriteaddress.getText().startsWith("https")){
            Uc=getHttpsConnection(forwriteaddress.getText(),globalSettings.ConnectionSettings,globalSettings.ConnectionValues);
            try {
                getTableData(saveFile(Uc));
            }catch (Exception e1){
                e1.printStackTrace();
            }
            jtable.setModel(new tablelists(tablesnames, tabledatas));
    }else if(forwriteaddress.getText().startsWith("C:")){
            try {
                getTableData(forwriteaddress.getText());
            } catch (Exception exception) {
                applicationLogOutput.append("??д·????????????????????.\r\n");
                exception.printStackTrace();
            }
        }else{
            applicationLogOutput.append("??д·????????淶.\r\n");
        }
    }
 });
 viewadapter.generateJLabel(forwriteurl,50,50,250,50,"???????????????????",15);
 viewadapter.generateJLabel(forshowurlinfo,50,100,250,50,"?????????????",15);
 viewadapter.generateJLabel(title, 550, 10, 150, 50, "?????????",25);
 viewadapter.generateJLabel(forshowtextinfo, 800,100, 250, 50, "??????????:", 15);
 viewadapter.generateJLabel(forshowrunstate, 5,790, 1920, 50, "", 12);
 viewadapter.generateJTextArea(forwriteaddress,0,0,350,25,"address",true,null);
 viewadapter.generateJTextArea(htmlResourceField, 0, 0, 550, 450, "resourceHTML",false,null);
 viewadapter.generateJTextArea(applicationLogOutput, 0, 0, 1445, 150, "logOutput",false,null);
 viewadapter.generateJTable(jtable,0,0, 650,450,"resources",new MouseAdapter(){
    public void mouseClicked(MouseEvent e) {//?????????????
       //?????е????е??????
      int r= jtable.getSelectedRow();
      int c= jtable.getSelectedColumn();
      downurl= jtable.getValueAt(r, 1);
      Object resourcestype=jtable.getValueAt(r,2);
      String urlinfo="??????:"+downurl.toString();
      //?????е?????????????ж????????
      if(c==1){
        forshowrunstate.setText(urlinfo);
      }
      if(c==3){
       setClipboardString(downurl.toString());
       forshowrunstate.setText("??????");
      }
      if(c==4){
       String []srcs=new String(new String(downurl.toString())).split("/");
       String []headprotocal=new String(new String(downurl.toString())).split("//");
       String []filetype=new String(new String(resourcestype.toString())).split("/");
       String []unvalidstr=new String[]{"=","?"};

       for(String str:unvalidstr)
       downloadfilename=(srcs[srcs.length-1]+"."+filetype[0]).replace(str, "");
       String downurlstr=downurl.toString();
       if(downurlstr.endsWith("com"))
       downurlstr+="/";
       if(headprotocal[0].equals("")){
       downurlstr=globalSettings.RequestType[globalSettings.ConnectionRequestSignal].equals("HTTP")?"http:"+downurlstr:"https:"+downurlstr;
       applicationLogOutput.append(downurlstr+"\r\n");
    }else{
        applicationLogOutput.append(downurlstr+"\r\n");
    }
    applicationLogOutput.append("?????????"+"\r\n");
    getMediaResources(downurlstr,globalSettings.DownloadSettings,globalSettings.DownloadValues);
    }}},null,null);
 viewadapter.generateJScrollpanes(fortableshow,50,150, 650,450, "tableshow",null);
 viewadapter.generateJScrollpanes(forraddressshow, 220,60, 350,25, "logShow",null);
 viewadapter.generateJScrollpanes(forlogoutput, 0, 650, 1445, 150, "logShow",null);
 viewadapter.generateJScrollpanes(forresourcesshow, 800, 150, 550, 450, "logShow",null);
 JComponent []jcomponents=new JComponent[]{title,forwriteurl,forshowurlinfo,forshowtextinfo,forshowrunstate,fortableshow,forraddressshow,forlogoutput,forresourcesshow,requestUrl,autoRequest,autoCaptures,advancedSettings};
 viewadapter.generateJPanel(headStatePanel,0,0,1920,1080,"requestUrl",jcomponents,null);
 viewadapter.generateJFrame(this,0,0,1920,1080,"myframe",new JPanel[]{headStatePanel},new JScrollPane[]{},JFrame.EXIT_ON_CLOSE,true);
}
public void searchDirectory() throws Exception{
    applicationLogOutput.append("???????????????......"+"\r\n");
File htmlfiledirectory=new File(htmlfilepath);
File mediafiledirectory=new File(mediafilepath);
File[] files=new File[]{htmlfiledirectory,mediafiledirectory};
for(int i=0;i<files.length;i++){
if(files[i].exists()){
    applicationLogOutput.append("?????????????"+"\r\n");
}else{
    applicationLogOutput.append("???????????????"+"\r\n");
    files[i].mkdirs();
    applicationLogOutput.append("?????????"+"\r\n");
}
}
}

public  void setClipboardString(String text) {

    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    Transferable trans = new StringSelection(text);

    clipboard.setContents(trans, null);

}
public void updateLogText(String nextLog){

    String text=forlogoutput.getToolTipText()+nextLog;
    text+="\r\n";
    System.out.println(text);

}

    /**
     * ???????HTML?????????
     * @param filetype
     * @return
     */
    public String getFilePath(String filetype){
    String filepath=null;
    if(filetype.equals("html")){
    Date date = new Date();
    SimpleDateFormat dateFormat= new SimpleDateFormat("yyyyMMddhhmmss");
    filepath=htmlfilepath+dateFormat.format(date)+"html.temp";
}else if(filetype.equals("media")){
    filepath=mediafilepath+downloadfilename;
}
return filepath;
}

    /**
     * ????????????б????????HttpsConnection
     * @param pools
     * @return
     */
    public HttpsURLConnection autoRequestHttps(RequestPool pools){
    String header;
    int responseCode=0;
    htmlResoucesInfo.setLength(0);
    htmlResourceField.setText("");
    HttpsURLConnection httpConnection=null;
    try{
        URL url = new URL(pools.getUrl());
        System.out.println("searchFile:"+filepath);
        String[] parts=pools.getHeaders().split(": ");
        httpConnection = (HttpsURLConnection) url.openConnection();
        responseCode=httpConnection.getResponseCode();
        if(responseCode == HttpsURLConnection.HTTP_OK){
        httpConnection.setRequestMethod(pools.getMethod());
        for(int j=0;j<parts.length;j+=2){
        System.out.println(parts[j]+":"+parts[j+1]);
        httpConnection.setRequestProperty(parts[j],parts[j+1]);
        httpConnection.connect();

}}else
    applicationLogOutput.append("??????????????????????????????"+responseCode+"\r\n");

}catch(Exception e){
}
return httpConnection;
}

    /**
     * ????????????б????????HttpConnection
     * @param pools
     * @return
     */
    public HttpURLConnection autoRequestHttp(RequestPool pools){
    String header;
    int responseCode=0;
    htmlResoucesInfo.setLength(0);
    htmlResourceField.setText("");
    HttpURLConnection httpConnection=null;
    try{
        URL url = new URL(pools.getUrl());
        System.out.println("searchFile:"+filepath);
        String[] parts=pools.getHeaders().split(": ");
        httpConnection = (HttpURLConnection) url.openConnection();
        responseCode=httpConnection.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK){
        httpConnection.setRequestMethod(pools.getMethod());
        for(int j=0;j<parts.length;j+=2){
        System.out.println(parts[j]+":"+parts[j+1]);
        httpConnection.setRequestProperty(parts[j],parts[j+1]);
        httpConnection.connect();
}}else
    applicationLogOutput.append("??????????????????????????????"+responseCode+"\r\n");
}catch(Exception e){
}
return httpConnection;
}

    /**
     * ?????????byte???
     * @param sources
     * @return
     * @throws Exception
     */
    public static byte[] getResponseBytes(InputStream sources) throws Exception{
    byte [] bytes=new byte[1024];
    byte [] total_bytes=new byte[0];
    while (sources.read(bytes, 0, 1024) != -1) {
        total_bytes=ByteUtils.byteMerger(total_bytes,bytes);
    }
    return total_bytes;
}

    /**
     * ????????????
     * ????????????
     * @param hul
     * @return
     */
    public String saveFile(URLConnection hul){
    String filepath=getFilePath("html");
    String line;
    byte [] bytes=new byte[1024];
    try{
    String unitData;
    File file=new File(filepath);
    InputStream sources=hul.getInputStream();
    InputStream filterInputStream=null;
    FileWriter fileWriter=new FileWriter(file);
    String encoding=getHeaders(hul).get("Transfer-Encoding").toString();
    if(encoding!=null){
    //????????Headers????ж?Content-Encoding????????gzip,?????????gzip???????????
    if(encoding.equals("[chunked]")){//System.out.println("gzip-encoding");
    System.out.println("chunck-encoding");
//    filterInputStream=new GZIPInputStream(sources);
        if(sources!=null) {
            System.out.println("InputStream is not null");
             fileWriter.write(readBody(readGzipBody(sources)).toString());
        }

    //??gzip???????б??????????????
    }else{
        byte[] total_bytes=getResponseBytes(sources);
        System.out.println("unchunk-encoding");
        filterInputStream=new ByteArrayInputStream(total_bytes);
        while (filterInputStream.read(bytes)!=-1 ) {
            htmlResoucesInfo.append(new String(bytes,"UTF-8"));
        }
        System.out.println(htmlResoucesInfo.toString());
        fileWriter.write(htmlResoucesInfo.toString());
}
}else{
        byte[] total_bytes=getResponseBytes(sources);
     System.out.println("html-encoding");
     filterInputStream=new ByteArrayInputStream(total_bytes);
     while (filterInputStream.read(bytes)!=-1 ) {
         htmlResoucesInfo.append(new String(bytes,"UTF-8"));
     }
     fileWriter.write(htmlResoucesInfo.toString());
}
    applicationLogOutput.append("??????С"+htmlResoucesInfo.length()+"??????????·????"+filepath+"\r\n");
    sources.close();
    filterInputStream.close();
    fileWriter.close();
    }catch(Exception e){
e.printStackTrace();
    }
    return filepath;
}

    /**
     * ???????????????
     * @param urlConnection
     * @return
     */
    public  Map<String, List<String>> getHeaders(URLConnection urlConnection){
    String header;
    Map<String, List<String>> headers=null;
    String filepath=getFilePath("html");
    try{
        headers = urlConnection.getHeaderFields();;
        Iterator<String> headerSet=headers.keySet().iterator();
        while(headerSet.hasNext()){
        header=headerSet.next();
        applicationLogOutput.append(header+":"+headers.get(header)+"\r\n");
        /*if(header.toString().equals("Transfer-Encoding")){
        if(headers.get(header).toString().equals("[chunked]"))
        isChuncked=true;
        else
        isChuncked=false;
        }*/
        }
    }catch(Exception e){

    }
    return headers;
}


public void chunkConnection(){

}

    /**
     * ??HttpConnection???????????
     * @param address
     * @param ConnectionSettings
     * @param ConnectionValues
     * @return
     */
    public HttpURLConnection getHttpConnection(String address,ArrayList<String> ConnectionSettings,ArrayList<String> ConnectionValues){
    String header;
    int responseCode=0;
    htmlResoucesInfo.setLength(0);
    htmlResourceField.setText("");
    HttpURLConnection httpConnection=null;
    try{
        URL url = new URL(address);
        System.out.println("searchFile:"+filepath);
        httpConnection = (HttpURLConnection) url.openConnection();
        responseCode=httpConnection.getResponseCode();
        //????ж??????????????????????????????Ч?????????
        if(responseCode == HttpURLConnection.HTTP_OK){
        httpConnection.setRequestMethod(globalSettings.RequestType[globalSettings.ConnectionMethodSignal]);
        for(int i=0;i<globalSettings.ConnectionSettings.size();i++)
        httpConnection.setRequestProperty(ConnectionSettings.get(i),ConnectionValues.get(i));
        httpConnection.connect();
        }else{
         applicationLogOutput.append("??????????????????????????????"+responseCode+"\r\n"); }
        }catch(Exception e) {
        applicationLogOutput.append("?????????????????????????????????????Ч????????????"+"\r\n");
        e.printStackTrace();
    }
    return httpConnection;
    }

    /**
     * getHttpsConnection???????????
     * @param address
     * @param ConnectionSettings
     * @param ConnectionValues
     * @return
     */
    public HttpsURLConnection getHttpsConnection(String address,ArrayList<String> ConnectionSettings,ArrayList<String> ConnectionValues){
    String header;
    int responseCode=0;
    htmlResoucesInfo.setLength(0);
    htmlResourceField.setText("");
    HttpsURLConnection httpsConnection=null;
    try{
        URL url = new URL(address);
        System.out.println("searchFile:"+filepath);
        httpsConnection = (HttpsURLConnection) url.openConnection();
        responseCode=httpsConnection.getResponseCode();
        if(responseCode == HttpsURLConnection.HTTP_OK){
        httpsConnection.setRequestMethod(globalSettings.RequestType[globalSettings.ConnectionMethodSignal]);
        //?????????????????????????????
        for(int i=0;i<globalSettings.ConnectionSettings.size();i++)
        httpsConnection.setRequestProperty(ConnectionSettings.get(i),ConnectionValues.get(i));
        httpsConnection.connect();
        }else{
         applicationLogOutput.append("??????????????????????????????"+responseCode+"\r\n"); }
        }catch(Exception e) {
        applicationLogOutput.append("?????????????????????????????????????Ч????????????"+"\r\n");
        e.printStackTrace();
    }
    return httpsConnection;
    }

    /**
     * ????????????
     * @param address
     * @param DownloadSettings
     * @param DownloadValues
     */
    public void getMediaResources(String address,ArrayList<String> DownloadSettings,ArrayList<String> DownloadValues){
    filepath=getFilePath("media");
    try{
        URL url = new URL(address);
        File file=new File(filepath);
        URLConnection URLconnection = url.openConnection();
        HttpURLConnection httpConnection = (HttpURLConnection) URLconnection;
        httpConnection.setRequestMethod(globalSettings.RequestType[globalSettings.DownLoadMethodSignal]);
        for(int i=0;i<globalSettings.ConnectionSettings.size();i++)
        httpConnection.setRequestProperty(globalSettings.DownloadSettings.get(i),globalSettings.DownloadValues.get(i));
        int responseCode = httpConnection.getResponseCode();
        if(responseCode == HttpURLConnection.HTTP_OK){
        applicationLogOutput.append("??????"+"\r\n");
        //Map<String, List<String>> headers = httpConnection.getHeaderFields();;
        InputStream in = httpConnection.getInputStream();
        InputStreamReader osw=new InputStreamReader(in, "UTF-8");
        FileOutputStream fos=new FileOutputStream(file);
        applicationLogOutput.append("??????????......"+"\r\n");
        fos.write(getResponseBytes(in));
        //for(int i=0;i<key.length;i++)
        //applicationLogOutput.append(headers+":"+headers.get(key).get(i));
        applicationLogOutput.append("????????????洢·????"+filepath+"\r\n");
        in.close();
        fos.close();
        }else{
        applicationLogOutput.append("??????????????????????????????"+responseCode+"\r\n");
        }
        }catch(Exception e) {
        applicationLogOutput.append("?????????????????????????????????????Ч??"+"\r\n");
        e.printStackTrace();
    }
}

    /**
     * @param is
     * @return
     * @throws IOException
     */
    private static int getChunkSize(InputStream is) throws IOException {
    System.out.println("getChunkSize");
    BufferedReader reader=new BufferedReader(new InputStreamReader(is,"GBK"));
    String sLength = reader.readLine().trim();
    System.out.println(sLength);
    if (sLength.startsWith("\r\n")){
        //?????????п???????????????С?
        System.out.println("??????????У?????????????С?");
        //??????
        sLength = reader.readLine().trim();
    }
    if (sLength.length() < 4) {
        System.out.println("?????????С??4");
        sLength = 0 + sLength;
    }
    reader.close();
    // ??16?????????????Int????
    int length = Integer.valueOf(sLength);
    return length;
}

    /**
     * ?????Chuncked??????С??????????????
     * @param is
     * @return
     * @throws IOException
     */
    private static List<Byte> readGzipBody(InputStream is) throws IOException {
    // ???????С??????chunked???????????????????????С??16????????????????????????????????С
    int chunk = getChunkSize(is);
    System.out.println("size:"+chunk);
    List<Byte> bodyByteList = new ArrayList<Byte>();
    byte readByte = 0;
    int count = 0;
    while (count < chunk) {  // ???????壬?????chunk??byte
        readByte=(byte)is.read();
        bodyByteList.add(Byte.valueOf(readByte));
        count ++;
    }
    if (chunk > 0){ // chunk??????????????ж??????????????????????
        List<Byte> tmpList = readGzipBody(is);
        bodyByteList.addAll(tmpList);
    }

    return bodyByteList;
}


    /**
     * ???Body????
     * @param gzipbody
     * @return
     */
    private static byte [] readBody(List<Byte> gzipbody) {
//    List<Byte> lineByteList = new ArrayList<Byte>();
//    byte readByte;
//    int total = 0;
//    try {
//        do {
//            readByte = (byte) is.read();
//            lineByteList.add(Byte.valueOf(readByte));
//            total++;
//        } while (total < contentLe);
//    } catch (IOException e) {
//        e.printStackTrace();
//    }
    byte[] tmpByteArr = new byte[gzipbody.size()];
    for (int i = 0; i < gzipbody.size(); i++) {
        tmpByteArr[i] = ((Byte) gzipbody.get(i)).byteValue();
    }
    gzipbody.clear();
    /*String line = "";
    try {
        line = new String(tmpByteArr, "UTF-8");
    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
    }
    return line;*/
    return tmpByteArr;
}

    /**
     * ??????????
     * @param filepath
     * @throws Exception
     */
    public void getTableData(String filepath) throws Exception {
    tablesnames=new String[]{"???","??????","???????","???????","????????"};
    htmlreader=new HtmlReader(filepath);
	htmlreader.getAddress("multiselect",new String[]{"img","source","a"});
	htmlreader.getText("multiselect",new String[]{"title","a","h1","p","li","ul"});
    applicationLogOutput.append("???????HTML???..."+"\r\n");
    applicationLogOutput.append("??????"+htmlreader.urllist.size()+"??????"+"\r\n");
    for(int z=0;z<htmlreader.textlist.size();z++)
    htmlResourceField.append(htmlreader.textlist.get(z).toString()+"\r\n");
    tabledatas=new Object[htmlreader.urllist.size()][tablesnames.length];
	for(int i=0;i<htmlreader.urllist.size();i++){
    for(int j=0;j<tablesnames.length;j++){
        String resourceurl=htmlreader.urllist.get(i);
        switch(j){
        case 0:
        tabledatas[i][0]=i;
        case 1:
        System.out.println("resource:"+resourceurl);
        tabledatas[i][1]=resourceurl;
        case 2:
       if(resourceurl.endsWith(".mp4")){
            tabledatas[i][2]="mp4/??????";
        }else if(resourceurl.endsWith(".mp3")){
            tabledatas[i][2]="mp3/??????";
        }else if(resourceurl.endsWith(".jpg")||resourceurl.indexOf(".jpg?")!=-1){
            tabledatas[i][2]="jpg/??????";
        }else if(resourceurl.endsWith(".png")||resourceurl.indexOf(".png?")!=-1){
            tabledatas[i][2]="png/??????";
        }else if(resourceurl.endsWith(".gif")||resourceurl.indexOf(".gif?")!=-1){
            tabledatas[i][2]="gif/??????";
        }else{
            tabledatas[i][2]="html/??????";
        }
        case 3:
        tabledatas[i][3]=new ImageIcon("???????????");
        case 4:
        tabledatas[i][4]=new ImageIcon("???????");
    }
}
    }
    applicationLogOutput.append("???????"+"\r\n");
}


public Rectangle getFrameSize(){
    return getBounds();
}



public static void main(String [] args){
    HtmlWatcher htmlWatcher=new HtmlWatcher();
}
}