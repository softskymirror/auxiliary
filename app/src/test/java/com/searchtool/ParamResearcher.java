package com.searchtool;

import com.toolui.ViewAdapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


// import com.google.common.primitives.Bytes;

import java.awt.Dimension;
public class ParamResearcher extends JFrame{
    boolean localModel=true,onlineModel=false,noCrypt=false,noEdit=false,noGzip=false;
    String filepath="C:\\Program Files\\ParamResearcher\\exports\\";
    static int totalcount;
    public static int pref_height=1200;

    String [] crypttypes={"Base64","MD5"};
    String [] gziptypes={"GZIP"};
    String [] mixDirection={"开头","结尾"};
    com.toolui.ViewAdapter va=new ViewAdapter();
    ByteUtils bU=new ByteUtils();
    JLabel title=new JLabel();//字节解析器
    JLabel introduce=new JLabel();//选择获取数据源的类型：
    JLabel cryptInfos=new JLabel();//加解密信息表述
    JLabel cryptTool=new JLabel();//加密解密选项
    JLabel zipTool=new JLabel();//解压加压工具选项
    JLabel zipInfos=new JLabel();//加解压信息表述
    JLabel editTool=new JLabel();//字节修改选项
    JLabel cutHexInfos=new JLabel();//截取字节表述
    JLabel inputHexInfos=new JLabel();//插入字节表述
    JLabel mixHexInfos=new JLabel();//合成字节表述
    JLabel copyright=new JLabel();//版权表述
    JLabel fileTotalSize=new JLabel();//文件总大小
    JLabel fileTypes=new JLabel();//文件类型

    JRadioButton jt1=new JRadioButton();//本地路径
    JRadioButton jt2=new JRadioButton();//网络路径
    JButton analysis=new JButton();//解析数据按钮
    JFileChooser fileChooser=new JFileChooser();
    JRadioButton Encrypt=new JRadioButton();//base64加密
    JRadioButton Decrypt=new JRadioButton();//base64解密
    JRadioButton noCryptAction=new JRadioButton();//不进行任何操作
    JRadioButton gzip=new JRadioButton();//GZIP压缩
    JRadioButton unGzip=new JRadioButton();//GZIP解压
    JRadioButton noGzipAction=new JRadioButton();//不进行任何操作
    JRadioButton cutHex=new JRadioButton();
    JRadioButton inputHex=new JRadioButton();
    JRadioButton mixHex=new JRadioButton();
    JRadioButton noEditAction=new JRadioButton();
    JComboBox <String> crypttyesbox = new JComboBox<String>(crypttypes);
    JComboBox <String> gziptypesbox = new JComboBox<String>(gziptypes);
    JComboBox <String> mixLocationsbox = new JComboBox<String>(mixDirection);
    JButton run=new JButton();//执行字节操作
    JButton export=new JButton();//导出文件操作
    JButton turnArround=new JButton();//上移位
    JTextArea hexAddress=new JTextArea();//十六进制内存地址
    JTextArea afterhexAddress=new JTextArea();//十六进制内存地址
    JTextArea hexArea=new JTextArea();//十六进制
    JTextArea strArea=new JTextArea();//UTF-8文字编码
    JTextArea afterhexArea=new JTextArea();//经处理后十六进制
    JTextArea afterstrArea=new JTextArea();//经处理后的UTF-8文字编码
    JTextArea cryptpwd=new JTextArea();//加密密钥
    JTextArea cutStart=new JTextArea();//裁剪起始数
    JTextArea cutLength=new JTextArea();//裁剪高度
    JTextArea inputStart=new JTextArea();//插入起始数
    JTextArea inputhexarea=new JTextArea();//插入字节区域
    JTextArea mixhexarea=new JTextArea();//混合字节区域
    ButtonGroup chooseGroup=new ButtonGroup();//单按钮组
    ButtonGroup cryptGroup=new ButtonGroup();//加解密单选项
    ButtonGroup codeGroup=new ButtonGroup();//加解压单选项
    ButtonGroup editGroup=new ButtonGroup();//字节修改选项
    JPanel byteViewPanel=new JPanel();//byte视图面板
    JPanel afterByteViewPanel=new JPanel();//经处理后byte视图面板
    JPanel layoutPanel=new JPanel();
    JTextArea sourcepath=new JTextArea();//输入路径地址
    JScrollPane pathpanel=new JScrollPane(sourcepath);
    JLabel state=new JLabel(); 
    
    JScrollPane byteViewScrollPane=new JScrollPane(byteViewPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    //byte视图面板处理前
    JScrollPane afterbyteViewScrollPane=new JScrollPane(afterByteViewPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    public ParamResearcher(){
    va.generateJLabel(title, 450,20,150,25, "字节解析器", 20,false);
    va.generateJLabel(introduce, 20,80,250,20, "选择获取数据源的方式", 15,false);
    va.generateJLabel(state, 20,780, 1920,50,"testing area",12,false);
    va.generateJLabel(cryptTool, 680, 200, 150, 25, "加解密选项", 15, false);
    va.generateJLabel(zipTool, 680, 300, 150, 25, "解压选项", 15, false);
    va.generateJLabel(cryptInfos, 680,240, 300,20,"加密类型                             加密密钥", 15, false);
    va.generateJLabel(zipInfos, 680,340, 300,20,"压缩类型", 15, false);
    va.generateJLabel(editTool, 680, 380, 450, 25, "字节编辑选项", 15, false);
    va.generateJLabel(cutHexInfos, 780, 420, 450, 25, "起始数                           截取长度", 15, false);
    va.generateJLabel(inputHexInfos, 780, 450, 450, 25, "起始数                          插入片段", 15, false);
    va.generateJLabel(mixHexInfos, 780, 480, 450, 25, "合并位置                                合并片段", 15, false);
    va.generateJLabel(copyright, 1100, 760, 550, 25,"Copyright ?2022 HuangRuiNan, All Rights Reserved.",12,false);
    va.generateJLabel(fileTotalSize,1100, 760, 550, 25, "filesize",12,false);
    va.generateJLabel(fileTypes,1100, 760, 550, 25, "fileTypes",12,false);
    va.generateJRadioButton(jt1, 200, 80, 150, 25, "本地路径", 15, true,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            localModel=true;
            onlineModel=false;
        }
    });
    va.generateJRadioButton(jt2, 400, 80, 150, 25, "网络路径", 15,false,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            localModel=false;
            onlineModel=true;
        }    
    });
    va.generateJRadioButton(Encrypt, 1100, 240, 100, 25, "加密", 15, false,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            crypttyesbox.setEnabled(true);
        
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }
    });
    va.generateJRadioButton(Decrypt, 1200, 240, 100, 25, "解密", 15,false,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            crypttyesbox.setEnabled(true);
          
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(gzip, 950, 340, 100, 25, "压缩", 15,false,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            gziptypesbox.setEnabled(true);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(unGzip, 1050, 340, 100, 25, "解压", 15,false,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            gziptypesbox.setEnabled(true);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(noCryptAction, 1300, 240, 200, 25, "无需加/解密", 15,true,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            crypttyesbox.setEnabled(false);
            cryptpwd.setEnabled(false);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(noGzipAction, 1150, 340, 200, 25, "无需压缩/解压", 15,true,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            gziptypesbox.setEnabled(false);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(noEditAction, 680, 510, 200, 25, "无需字节编辑", 15,true,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            cutStart.setEnabled(false);
            cutLength.setEnabled(false);
            inputStart.setEnabled(false);
            inputhexarea.setEnabled(false);
            mixLocationsbox.setEnabled(false);
            mixhexarea.setEnabled(false);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(cutHex, 680,420,100,25, "裁剪", 15,false,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            cutStart.setEnabled(true);
            cutLength.setEnabled(true);
            mixLocationsbox.setEnabled(false);
            mixhexarea.setEnabled(false);
            inputStart.setEnabled(false);
            inputhexarea.setEnabled(false);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(inputHex, 680,450, 100, 25, "插入",15, false,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            cutStart.setEnabled(false);
            cutLength.setEnabled(false);
            mixLocationsbox.setEnabled(false);
            mixhexarea.setEnabled(false);
            inputStart.setEnabled(true);
            inputhexarea.setEnabled(true);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
    va.generateJRadioButton(mixHex, 680,480, 100, 25,"合并", 15,  false,new ActionListener(){

        @Override
        
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            cutStart.setEnabled(false);
            cutLength.setEnabled(false);
            mixLocationsbox.setEnabled(true);
            mixhexarea.setEnabled(true);
            inputStart.setEnabled(false);
            inputhexarea.setEnabled(false);
            if(noCryptAction.isSelected()&&noEditAction.isSelected()&&noGzipAction.isSelected())
            run.setEnabled(false);
            else
            run.setEnabled(true);
        }    
    });
   
    va.generateJRadioButton(jt1, 200, 80, 150, 25, "本地路径", 15, false, new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            localModel=true;
            onlineModel=false;
        }
        
    });
    va.generateJRadioButton(jt2, 400, 80, 150, 25, "网络路径", 15, false,new ActionListener(){

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            localModel=false;
            onlineModel=true;
        }    
    });
    va.generateCombobox(crypttyesbox, 750, 240, 75,17, new ItemListener(){

        @Override
        public void itemStateChanged(ItemEvent e) {
            // TODO Auto-generated method stub
            if (e.getStateChange() == ItemEvent.SELECTED) {
                System.out.println(crypttyesbox.getSelectedIndex());
                if(crypttyesbox.getSelectedIndex()==0){
                    cryptpwd.setEnabled(false);
                }else if(crypttyesbox.getSelectedIndex()==1){
                    cryptpwd.setEnabled(true);
                }else if(crypttyesbox.getSelectedIndex()==2){
                    
                }
            }
        }
    });


    va.generateCombobox(gziptypesbox, 750, 340, 150,17, new ItemListener(){

        @Override
        public void itemStateChanged(ItemEvent e) {
            // TODO Auto-generated method stub
            if (e.getStateChange() == ItemEvent.SELECTED) {
                System.out.println("选中: " + crypttyesbox.getSelectedIndex() + " = " + crypttyesbox.getSelectedItem());
            }
        
        }
    });
    va.generateCombobox(mixLocationsbox, 850, 480, 75,18, new ItemListener(){

        @Override
        public void itemStateChanged(ItemEvent e) {
            // TODO Auto-generated method stub
            if (e.getStateChange() == ItemEvent.SELECTED) {
                System.out.println("选中: " + crypttyesbox.getSelectedIndex() + " = " + crypttyesbox.getSelectedItem());
            }
        
        }
    });
    va.generateButtonGroup(chooseGroup, new JRadioButton[]{jt1,jt2});
    va.generateButtonGroup(cryptGroup, new JRadioButton[]{Encrypt,Decrypt,noCryptAction});
    va.generateButtonGroup(codeGroup, new JRadioButton[]{gzip,unGzip,noGzipAction});
    va.generateButtonGroup(editGroup, new JRadioButton[]{cutHex,inputHex,mixHex,noEditAction});
    va.generateJTextArea(sourcepath, 0, 0, 300, 25, "sourcepath", 12,true, true,null,null);
    va.generateJScrollpanes(pathpanel, 20, 120, 300, 25, "pathpanel");
    va.generateJButton(analysis, 400, 120, 100, 25,"解析数据", 12,true,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            String path=sourcepath.getText();
            if(path.equals("")){
            JOptionPane.showMessageDialog(null, "文件地址不能为空");
            }else{   
          
            System.out.println(path);          
            try{
                byte [] filesbytes=bU.filetoBytes(path);
                hexArea.setText(bU.getByteHex(filesbytes));
                strArea.setText(bU.getByteString(filesbytes));
                setPreferredSizeForComponent(filesbytes,new JTextArea[]{hexAddress,hexArea,strArea});

                Dimension dim=new Dimension(byteViewPanel.getPreferredSize().width+byteViewScrollPane.getVerticalScrollBar().getSize().width,pref_height);
                byteViewPanel.setPreferredSize(dim);
                hexAddress.setText(bU.getHexColumns(totalcount));
                state.setText("解析成功,总文件大小"+filesbytes.length+"byte");

            }catch(Exception ec){
                JOptionPane.showMessageDialog(null, "未找到指定文件");
                 ec.printStackTrace();
            }    
        }
      
        }
    });
    va.generateJButton(run, 1240,570,120,50,"执行操作",12,false,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            try {
                StartEdit();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                JOptionPane.showMessageDialog(null,"中途发生错误");
                e1.printStackTrace();
            }
        }
    });
    va.generateJButton(export, 1060,570,120,50,"导出文件",12,true,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
          if(bU.bytes!=null)
            getOutputPath();
            else
            JOptionPane.showMessageDialog(null,"结果域为空，无法导出文件");
        
        }
    });
    va.generateJButton(turnArround, 600,400,50,150,"上"+"\r"+"移"+"\r"+"位",15,true,new ActionListener(){
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO Auto-generated method stub
            moveToHexArea();
        }
    });
    va.generateJTextArea(hexAddress,0, 0, 50,0 , "", 12,false,true,null,null);
    va.generateJTextArea(afterhexAddress, 0, 0, 50, 0,"", 12,false,true,null,null);
    va.generateJTextArea(hexArea, 60, 0, 320, 0, "", 12,false, true,null,new MouseListener(){

        @Override
        public void mouseClicked(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // TODO Auto-generated method stub
            //strArea.select(hexArea.getSelectionStart(), hexArea.getSelectionEnd());
      
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // TODO Auto-generated method stub
            int selectbegin=hexArea.getSelectionStart()/3;
            String text=hexArea.getSelectedText().replaceAll(" ", "");
            String []byteslen=hexArea.getSelectedText().split(" ");
            byte [] bytes=bU.hexStringToBytes(text);
            String strBytes=new String(bytes);
            System.out.println(byteslen.length);
            int intBytes=(byteslen.length>3)?bU.byte2Int(bytes):0;
            String infos=String.valueOf(byteslen.length)+(byteslen.length>1?"bytes":"byte")+"|"+"String:"+strBytes+"|"+"int:"+intBytes+"|"+"length:"+bytes.length+"|"+"beginBytes:"+selectbegin;
            state.setText(infos);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // TODO Auto-generated method stub
          
        }
        
    });
    va.generateJTextArea(strArea, 390, 0, 130, 0, "", 12,false, true,null,null);
    va.generateJTextArea(afterhexArea, 60, 0, 320, 0, "", 12,true, true,null,new MouseListener(){

        @Override
        public void mouseClicked(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // TODO Auto-generated method stub
           
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // TODO Auto-generated method stub
           
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // TODO Auto-generated method stub  
        }   
    });
    va.generateJTextArea(afterstrArea, 390, 0, 130, 0, "",12, true, true,null,null);
    va.generateJTextArea(cryptpwd, 940, 240, 150, 17, "",12, true, false,null,null);
    va.generateJTextArea(cutStart, 840,420,50, 17,"", 12, true, false,null,null);
    va.generateJTextArea(cutLength, 1010, 420, 50, 17, "", 12, true, false,null,null);
    va.generateJTextArea(inputStart, 840, 450, 50, 17, "", 12,  true, false,null,null);
    va.generateJTextArea(inputhexarea, 1020, 450, 250, 17,"", 12, true, false,null,null);
    va.generateJTextArea(mixhexarea, 1050, 480, 250, 17,"", 12, true, false,null,null);
    va.generateJPanel(byteViewPanel, 20, 200, 550, 300, "byteViewPanel",new JComponent[]{hexAddress,hexArea,strArea},null);
    va.generateJScrollpanes(byteViewScrollPane,20, 160,550, 300, "byteViewScrollPane");
    va.generateJPanel(afterByteViewPanel, 20, 200, 550, 300, "afterByteViewPanel", new JComponent[]{afterhexAddress,afterhexArea,afterstrArea},null); 
    va.generateJScrollpanes(afterbyteViewScrollPane, 20, 480,550, 300, "afterByteViewPanel"); 
    JComponent []jcomponents=new JComponent[]{title,introduce,jt1,jt2,pathpanel,analysis,turnArround,byteViewScrollPane,afterbyteViewScrollPane,state,cryptTool,zipTool,cryptInfos,zipInfos,editTool,Encrypt,Decrypt,gzip,unGzip,noCryptAction,noGzipAction,noEditAction,cutHex,cutHexInfos,inputHex,inputHexInfos,mixHex,mixHexInfos,crypttyesbox,gziptypesbox,mixLocationsbox,cryptpwd,cutStart,cutLength,inputStart,inputhexarea,mixhexarea,copyright,run,export};
    va.generateJPanel(layoutPanel, 0, 0, 1440, 900, "panel", jcomponents,null);    
    va.generateJFrame(this, 0, 0, 1440, 900,"panel", new JPanel[]{layoutPanel}, JFrame.EXIT_ON_CLOSE);
    initDirectory();
}
public void initDirectory(){
    File file=new File(filepath);
    if(!file.exists()){
        file.mkdirs();
    }
}
public void moveToHexArea(){
    if(!afterhexArea.getText().equals("")){
    hexAddress.setText(afterhexAddress.getText());
    hexArea.setText(afterhexArea.getText());
    strArea.setText(afterstrArea.getText());
    Dimension dim=new Dimension(afterByteViewPanel.getPreferredSize().width+afterbyteViewScrollPane.getVerticalScrollBar().getSize().width,pref_height);
    byteViewPanel.setPreferredSize(dim);
    afterhexAddress.setText("");
    afterhexArea.setText("");
    afterstrArea.setText("");
    afterByteViewPanel.setPreferredSize(new Dimension(0,0));
}else{
    JOptionPane.showMessageDialog(null, "结果域为空");
}
}


private boolean getOutputPath() {
    boolean pathFlg = true;
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setDialogTitle("选择文件导出的路径");
    int ret = fileChooser.showOpenDialog(null);
    if (ret == JFileChooser.APPROVE_OPTION) {
        String outFile = fileChooser.getSelectedFile().getAbsolutePath();
       bU.bytesToFile(bU.bytes, outFile);

       // outFile可选择的路径。 
    } else {
        pathFlg = false;
    }
    return pathFlg;
}
public void StartEdit() throws Exception{
  byte [] bUBytes=bU.getHexBytes(hexArea.getText());
if(Decrypt.isSelected()){
    state.setText("正在解密字节....");
    if(crypttyesbox.getSelectedIndex()==0){
 bUBytes=bU.base64Decoder(new String(bUBytes));
}else if(crypttyesbox.getItemCount()==1){

}
}
if(Encrypt.isSelected()){
    state.setText("正在加密字节....");
    if(crypttyesbox.getSelectedIndex()==0){
    bUBytes=bU.base64Decoder(hexArea.getText().toString());
    }else if(crypttyesbox.getItemCount()==1){
}
}
if(gzip.isSelected()){
    state.setText("正在压缩字节....");
if(gziptypesbox.getSelectedIndex()==0){
    bUBytes=bU.gZip(new String(bUBytes,"UTF-8")).getBytes();
}
}
if(unGzip.isSelected()){
    state.setText("正在解压字节....");
    if(gziptypesbox.getSelectedIndex()==0){
        bUBytes=bU.unGZip(new String(bUBytes,"UTF-8")).getBytes();
    }
}
if(cutHex.isSelected()){
    state.setText("正在截取字节....");
    bUBytes=bU.subByte(bUBytes,Integer.parseInt(cutStart.getText().toString()),Integer.parseInt(cutLength.getText().toString()));
   
}

if(inputHex.isSelected()){
    state.setText("正在插入字节....");
    
}

    if(mixHex.isSelected()){
        state.setText("正在混合字节....");
        if(mixLocationsbox.getSelectedItem().equals("开头")){
            bUBytes=bU.byteMerger(mixhexarea.getText().toString().getBytes(),bU.getHexBytes(hexArea.getText().toString()));
        }else if(mixLocationsbox.getSelectedItem().equals("结尾")){
            bUBytes=bU.byteMerger(bU.getHexBytes(hexArea.getText().toString()),mixhexarea.getText().toString().getBytes());
        }
    }
    

afterhexArea.setText(bU.getByteHex(bUBytes));
afterstrArea.setText(bU.getByteString(bUBytes));
setPreferredSizeForComponent(bUBytes,new JTextArea[]{afterhexAddress,afterhexArea,afterstrArea});
Dimension dim=new Dimension(afterByteViewPanel.getPreferredSize().width+afterbyteViewScrollPane.getVerticalScrollBar().getSize().width,pref_height);
afterByteViewPanel.setPreferredSize(dim);
afterhexAddress.setText(bU.getHexColumns(totalcount));
}
public static void setPreferredSizeForComponent(byte[] bytes,JTextArea[] jTextAreas){
totalcount=(bytes.length/18);
pref_height=totalcount*18;
for(int i=0;i<jTextAreas.length;i++){
    jTextAreas[i].setSize(jTextAreas[i].getWidth(),pref_height);
}


}
    public static void main(String [] args){
     ParamResearcher paramResearcher=new ParamResearcher();
    
    }
}
