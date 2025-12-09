package com.webtool;

import javax.swing.ButtonGroup;
import javax.swing.ComponentInputMap;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.TabExpander;
import javax.swing.text.View;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.*;
import java.net.ConnectException;
import java.util.ArrayList;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import org.apache.http.conn.params.ConnConnectionParamBean;


import java.awt.BorderLayout;
import java.awt.Container;
public class GlobalSettings extends JFrame{
      ArrayList <String>ConnectionSettings=new ArrayList<>();
     ArrayList <String>DownloadSettings=new ArrayList<>();
      ArrayList <String>ConnectionValues=new ArrayList<>();
     ArrayList <String>DownloadValues=new ArrayList<>();
      static ArrayList <RequestPool> RequestLists=new ArrayList<RequestPool>();
    public int globallastindex=0;
    DefaultMutableTreeNode [][] defaultMutableTreeNodes;
    boolean manunalModel,autoModel;

    public int datacount=0;
    static String pathstr="[, ]";
    static  String[] RequestType=new String[]{"HTTP","HTTPS","GET","POST"};
    
    static int DownLoadRequestSignal,ConnectionRequestSignal,DownLoadMethodSignal,ConnectionMethodSignal;
    
    public static final String []tablenames={"Header","Value"};
    public static final String []requestinfos={"Number","Url","Method","Headers"};
    public static final String []autolModelTypes={"??","Cookieμ?"};
    Object [][] headersdatas;
    
    static JTableViewer jTableViewer=new JTableViewer(tablenames,null);
    static JTableViewer autorequestTable=new JTableViewer(requestinfos,null);
    static JTree jtree;
    static JLabel requestUrl=new JLabel();
    static JLabel requestUrl1=new JLabel();
    static JLabel requestType=new JLabel(); 
    static JTextField foreasyshow=new JTextField();
    static JTextField forwriteautoinfos=new JTextField();
    static JTextArea [] jtextareas;
    static JLabel [] settingslabels;
    static JRadioButton [] jRadioButtons;
    static JRadioButton [] jreqmethodsbuttons;
    static JRadioButton [] jreqbuttons;
   
    JLabel warnnings=new JLabel();
    JRadioButton httpreq=new JRadioButton();
    JRadioButton httpsreq=new JRadioButton();
    JRadioButton getreq=new JRadioButton();
    JRadioButton postreq=new JRadioButton();
    JRadioButton jb=new JRadioButton();
    JRadioButton jb1=new JRadioButton();
    JRadioButton jb2=new JRadioButton();
    ButtonGroup bgroup=new ButtonGroup();
    ButtonGroup reqtypes=new ButtonGroup();
    ButtonGroup reqmethodstypes=new ButtonGroup();
    boolean isClicked=false;
    JButton addHeader=new JButton();
    JButton deleteHeader=new JButton();
    JButton addRequest=new JButton();
    JButton deleteRequest=new JButton();
    JButton jbutton=new JButton("Ч");
    JButton jbutton2=new JButton("");
    JComponent []manuaulJc=new JComponent[]{httpreq,httpsreq,postreq,getreq,jTableViewer,addHeader,deleteHeader,foreasyshow};
    JComponent []autoJc=new JComponent[]{autorequestTable,addRequest,deleteRequest};
    JPanel jpanel=new JPanel();
    JPanel jpanel2=new JPanel();
    static String []ConnectionLabel={"Connection Settings",""};
    static String []DownloadLabel={"Download Settings",""};
    
    JComboBox <String>autoRequests=new JComboBox<String>(autolModelTypes);
   JScrollPane fortableshow=new JScrollPane(jTableViewer);
   JScrollPane foreaseshow=new JScrollPane(foreasyshow);
   JScrollPane forautotableshow=new JScrollPane(autorequestTable);
   JScrollPane forautorequestinfosshows=new JScrollPane(forwriteautoinfos);
   JScrollPane forpanel2show=new JScrollPane(jpanel2,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    public GlobalSettings(){
   Container c=getContentPane();
ViewAdapter viewAdapter=new ViewAdapter();
viewAdapter.generateJLabel(requestUrl, 0, 0, 300, 150, ConnectionLabel[0], 15);
viewAdapter.generateJLabel(requestUrl1, 0, 20, 550, 150, ConnectionLabel[1], 15);
viewAdapter.generateJLabel(requestType, 150, 110, 100, 25, "?", 15);
viewAdapter.generateJTextField(foreasyshow,0,0,150,30, "requesttext", true,new FocusListener(){

    @Override
    public void focusGained(FocusEvent e) {
        // TODO Auto-generated method stub
        foreasyshow.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped(KeyEvent e) {
              
                // TODO Auto-generated method stub
                
            }

            @Override
            public void keyPressed(KeyEvent e) {
                
                // TODO Auto-generated method stub
                /*KeyStroke ak = KeyStroke.getKeyStrokeForEvent(e);
                if(ak.equals(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT,InputEvent.ALT_DOWN_MASK))){
                    System.out.println("select All");
                    foreasyshow.selectAll();
                }*/
            }

            @Override
            public void keyReleased(KeyEvent e) {
                System.out.println("arg0.getModifiers()=" + e.getModifiers());
				System.out.println("arg0.getKeyCode()=" + e.getKeyCode());
                // TODO Auto-generated method stub
                if (e.getModifiers() == 2 && e.getKeyCode() == 39) {
					foreasyshow.selectAll();//
				}
            }
            
        });
        foreasyshow.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                // TODO Auto-generated method stub
                for(int i=0;i<jTableViewer.getRowCount();i++)
                ((DefaultTableModel) jTableViewer.getModel()).removeRow(i);
                String[] parts=foreasyshow.getText().toString().split(": ");  
              
                for(int j=0;j<parts.length;j+=2){
             ((DefaultTableModel) jTableViewer.getModel()).addRow(new String[]{parts[j].trim()," "+parts[j+1]});
              
        }   
        }
            @Override
            public void removeUpdate(DocumentEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // TODO Auto-generated method stub
                
            }
            
        });
    }

    @Override
    public void focusLost(FocusEvent e) {
        // TODO Auto-generated method stub
        
    }
    
});
viewAdapter.generateJButton(addHeader,20,140,90,25,"Add",new ActionListener(){
    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
      
        String rowData[]=new String[tablenames.length];
        ((DefaultTableModel) jTableViewer.getModel()).addRow(rowData);
        //rebuildTable(tablenames, headersdatas);
    }
});
viewAdapter.generateCombobox(autoRequests,0,380,75,17,new ItemListener(){

    @Override
    public void itemStateChanged(ItemEvent e) {
        // TODO Auto-generated method stub
        if (e.getStateChange() == ItemEvent.SELECTED) {
            globallastindex=globallastindex+Integer.parseInt(forwriteautoinfos.getText().toString());
            String bytesRange="Range: bytes="+globallastindex+"-"+forwriteautoinfos.getText().toString();

            System.out.println(autoRequests.getSelectedIndex());
            if(autoRequests.getSelectedIndex()==0){
            RequestPool pool=new RequestPool();
            String orignalWeb=(String)autorequestTable.getValueAt(0, 1);
            String orignalHeaders=(String)autorequestTable.getValueAt(0, 3)+": "+bytesRange;

            }else if(autoRequests.getSelectedIndex()==1){
               
            }
        }
    }
});
viewAdapter.generateJButton(deleteHeader,150,140,90,25,"Delete",new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        int selectedRow = jTableViewer.getSelectedRow();
                if (selectedRow != -1) {
                    ((DefaultTableModel) jTableViewer.getModel()).removeRow(selectedRow);//??е                }
            }
}
});
viewAdapter.generateJButton(addRequest,20,410,90,25,"Add",new ActionListener(){
    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
      
        String rowData[]=new String[tablenames.length];
        rowData[0]=String.valueOf(autorequestTable.getRowCount()+1);
        ((DefaultTableModel)autorequestTable.getModel()).addRow(rowData);
        //rebuildTable(tablenames, headersdatas);
    }
});
viewAdapter.generateJButton(deleteRequest,150,410,90,25,"Delete",new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        int selectedRow = autorequestTable.getSelectedRow();
                if (selectedRow != -1) {
                    ((DefaultTableModel)autorequestTable.getModel()).removeRow(selectedRow);//??е                }
            }
}
});
viewAdapter.generateJTable(jTableViewer,0,0,450,200,pathstr, null,null,null);
viewAdapter.generateJTable(autorequestTable,0,0,450,200,pathstr, null,null,null);
viewAdapter.generateJScrollpanes(fortableshow,20,170,450,200,"headers",null);
viewAdapter.generateJScrollpanes(foreaseshow,250,135,210,30,"responsetext",new FocusListener(){

    @Override
    public void focusGained(FocusEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void focusLost(FocusEvent e) {
        // TODO Auto-generated method stub
        
    }
    
});
viewAdapter.generateJScrollpanes(forautotableshow,20,440,450,200,"headers",null);
viewAdapter.generateJScrollpanes(forpanel2show,150, 0, 535,550, "headers",null);
viewAdapter.generateJLabel(warnnings, 0, 400, 250, 150, "", 12);
FocusListener fl=new FocusListener(){
    @Override
    public void focusGained(FocusEvent arg0) {
        // TODO Auto-generated method stub
      
    }
    @Override
    public void focusLost(FocusEvent arg0) {
        // TODO Auto-generated method stub
        //System.out.println(jtextarea.getText().toString().equals("GET"));
        
}
};
//jtextareas=new JTextArea[]{jtextarea,jtextarea2,jtextarea3,jtextarea4,jtextarea5,jtextarea6,jtextarea7,jtextarea8};
viewAdapter.generateJRadioButton(jb, 0, 110, 100, 25, "?", 15,new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
       jTableViewer.setEnabled(false);
       for(JComponent unit:manuaulJc){
        unit.setEnabled(true);
       }
       for(JComponent unit:autoJc){
        unit.setEnabled(false);
       }
    }

});
viewAdapter.generateJRadioButton(jb1, 0, 380,250, 25, "cookie?", 15,new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        jTableViewer.setEnabled(false);
        for(JComponent unit:manuaulJc){
         unit.setEnabled(false);
        }
        for(JComponent unit:autoJc){
         unit.setEnabled(true);
        
     }
    }
    
});

viewAdapter.generateJRadioButton(httpreq, 220, 110, 60, 25, "HTTP", 12,new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        if(pathstr.equals("[, ]"))
      ConnectionRequestSignal=0;
      else
      DownLoadRequestSignal=0;
    }
    
});

viewAdapter.generateJRadioButton(httpsreq, 280, 110, 70, 25, "HTTPS", 12,new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        if(pathstr.equals("[, ]"))
        ConnectionRequestSignal=1;
        else
        DownLoadRequestSignal=1;
    }   
});

viewAdapter.generateJRadioButton(getreq, 350, 110, 55, 25, "GET", 12,new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        if(pathstr.equals("[, ]"))
        ConnectionMethodSignal=2;
        else
        DownLoadMethodSignal=2;
    }   
});
viewAdapter.generateJRadioButton(postreq, 410, 110, 60, 25, "POST", 12,new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        // TODO Auto-generated method stub
        if(pathstr.equals("[, ]"))
        ConnectionMethodSignal=3;
        else
        DownLoadMethodSignal=3;
    }   
    
});
jRadioButtons=new JRadioButton[]{jb,jb1};
jreqbuttons=new JRadioButton[]{httpreq,httpsreq};
jreqmethodsbuttons=new JRadioButton[]{getreq,postreq};
viewAdapter.generateButtonGroup(bgroup,jRadioButtons);
viewAdapter.generateButtonGroup(reqtypes,jreqbuttons);
viewAdapter.generateButtonGroup(reqmethodstypes,jreqmethodsbuttons);
createJTree(0,0,250,650,"",new String[]{"",""},new String[][]{},jpanel);
settingslabels=new JLabel[]{requestUrl,requestUrl1};
//viewAdapter.generateJScrollpanes(jscrollpane5, 0, 0, 0, 0, "jtree");
viewAdapter.generateJButton(jbutton, 250, 650, 100, 25, "Ч", new ActionListener(){
    @Override
    public void actionPerformed(ActionEvent arg0) {
       
        // TODO Auto-generated method stub
        if(jb.isSelected()){
        if((httpreq.isSelected()||httpsreq.isSelected()&&getreq.isSelected()||postreq.isSelected())){
        if(pathstr.equals("[, ]")){
            if(jTableViewer.getRowCount()!=0){
             ConnectionSettings.clear();
             ConnectionValues.clear();
             if(jTableViewer.getRowCount()!=0){
            for(int s=0;s<tablenames.length;s++){
                for(int i=0;i<jTableViewer.getRowCount();i++){
                String value=(String)jTableViewer.getValueAt(i, s);
                System.out.println(""+s+""+i+":"+value);
                if(value!=null&&!value.equals("")){
                    switch(s){
                    case 0:
                    ConnectionSettings.add((String)jTableViewer.getValueAt(i, 0));
                    case 1:
                    ConnectionValues.add((String)jTableViewer.getValueAt(i, 1));
                }
                
            }else{
                JOptionPane.showMessageDialog(null,"HEADERS?null");
            }
          
        }
    }
}
    
        }
    }else if(pathstr.equals("[, ]")){
        if(jTableViewer.getRowCount()!=0){
             DownloadSettings.clear();
             DownloadValues.clear();
           
            for(int s=0;s<tablenames.length;s++){
                for(int i=0;i<jTableViewer.getRowCount();i++){
                    String value=(String)jTableViewer.getValueAt(i, s);
                    if(value!=null&&!value.equals("")){
                    switch(s){
                    case 0:
                    DownloadSettings.add((String)jTableViewer.getValueAt(i, s));
                    
                    case 1:
                    DownloadValues.add((String)jTableViewer.getValueAt(i, s));
                    }
                    
                }else{
                    JOptionPane.showMessageDialog(null,"HEADERS?null");
                }
                }
            }
            }
        }
        JOptionPane.showMessageDialog(null, "Ч?");
    }else{
        JOptionPane.showMessageDialog(null,"??");
    }
}
        if(jb1.isSelected()){
            boolean isValid=false;
            if(autorequestTable.getRowCount()!=0){
            if(pathstr.equals("[, ]")){
                RequestLists.clear();
                for(int i=0;i<autorequestTable.getRowCount();i++){
                    RequestPool pool=new RequestPool();
                    for(int s=0;s<requestinfos.length;s++){  
                         
                   String value=(String)autorequestTable.getValueAt(i, s);
                   System.out.println(""+s+""+i+":"+value);
                   if(value!=null&&!value.equals("")){
                       switch(s){
                       case 1:
                       pool.setUrl(value);
                       System.out.println("URL:"+value);
                       break;
                       case 2:
                       pool.setMethod(value);
                       break;
                       case 3:
                       pool.setHeaders(value);
                       break;
                   }
               
                   isValid=true;
                }else{
                    isValid=false;
                }
            }
            RequestLists.add(i,pool);
           }
           for(int i=0;i<RequestLists.size();i++)
           System.out.println("List"+i+":"+RequestLists.get(i).getUrl());
           }else if(pathstr.equals("[, ]")){
      
        RequestLists.clear();
       
        for(int i=0;i<autorequestTable.getRowCount();i++){
            RequestPool pool=new RequestPool();
        for(int s=0;s<requestinfos.length;s++){
            String value=(String)autorequestTable.getValueAt(i, s);
            System.out.println(""+s+""+i+":"+value);
            if(value!=null&&!value.equals("")){
               
                switch(s){
                case 1:
                pool.setUrl(value);
                break;
                case 2:
                pool.setMethod(value);
                break;
                case 3:
                pool.setHeaders(value);
                
                break;
            }
       
            isValid=true;
        }else{
            isValid=false;
        }
    }
    RequestLists.add(i,pool);
    }
  
    } 
     if(isValid)
    JOptionPane.showMessageDialog(null,"Ч?");
    else
    JOptionPane.showMessageDialog(null,"URL,METHOD,HEADERS?null");
}else{
    JOptionPane.showMessageDialog(null,"?");
    }
    } 
}
}
);
viewAdapter.generateJButton(jbutton2, 400, 650, 100, 25, "", new ActionListener(){

    @Override
    public void actionPerformed(ActionEvent arg0) {
        jTableViewer.removeAll();
        if(pathstr.equals("[, ]")){
        ConnectionSettings.clear();
        ConnectionSettings.clear();
    }else if(pathstr.equals("[, ]")){
        DownloadSettings.clear();
        DownloadSettings.clear();
    }
        // TODO Auto-generated method stub
}});
JComponent [] jcL=new JComponent[]{jtree};
JComponent [] jcR=new JComponent[]{requestUrl,requestUrl1,requestType,jb,jb1,jbutton,jbutton2,httpreq,httpsreq,getreq,postreq,addRequest,deleteRequest,fortableshow,foreaseshow,forautotableshow,addHeader,deleteHeader,warnnings};
viewAdapter.generateJPanel(jpanel,0, 0, 150, 550, "leftpanel", jcL,null);
viewAdapter.generateJPanel(jpanel2, 0, 0, 650, 1550, "rightpanel", jcR,null);
viewAdapter.generateJFrame(this, 350, 350, 700, 550, "settings",new JPanel[]{jpanel},new JScrollPane[]{forpanel2show},JFrame.DISPOSE_ON_CLOSE,false);
}
/*public void rebuildTable(String [] tablenames,Object [][] headersdatas){
    headersTable=new HeadersTable(tablenames, headersdatas);
        jtable.setModel(headersTable);
        jtable.addFocusListener(new FocusListener(){
 
            @Override
            public void focusGained(FocusEvent e) {
                // TODO Auto-generated method stub
                
            }
            @Override
            public void focusLost(FocusEvent e) {
                // TODO Auto-generated method stub
                if(jtable.isEditing()) {
                    jtable.getEditorComponent().addKeyListener(new KeyListener() {
                        @Override
                        public void keyTyped(KeyEvent e) {
                            // TODO Auto-generated method stub
                        }
                        @Override
                        public void keyPressed(KeyEvent e) {
                            // TODO Auto-generated method stub
                            if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                                int nEditRow = jtable.getEditingRow();
                                int nEditColumn = jtable.getEditingColumn();
                                if(jtable.getCellEditor() == null)return;
                                jtable.getCellEditor().stopCellEditing();
                                String strSelectedTestTaskName = (String) jtable.getValueAt(nEditRow, nEditColumn);
                               System.out.println(""+nEditRow+""+nEditColumn+"value:"+strSelectedTestTaskName);
                                if(pathstr.equals("[, ]")){
                                ConnectionSettings.add(nEditColumn, strSelectedTestTaskName);
                                ConnectionValues.add(nEditColumn, strSelectedTestTaskName);
                                rebuildTable(tablenames,getTableData(ConnectionSettings, ConnectionValues));
                            }else{
                                DownloadSettings.add(nEditColumn, strSelectedTestTaskName);
                                DownloadValues.add(nEditColumn, strSelectedTestTaskName);
                                    rebuildTable(tablenames,getTableData(DownloadSettings, DownloadValues));
                            }
                              
                            }
                        }
        
                        
                        @Override
                        public void keyReleased(KeyEvent e) {
                            // TODO Auto-generated method stub
                 
                
                        }
                    });
                }
            }
            
        });
}*/
public Object[][] getTableData(ArrayList<String> Settingsdatas,ArrayList<String> Valuesdatas){

    Object [][] tabledatas=new Object[tablenames.length][Settingsdatas.size()+1];
	for(int i=0;i<2;i++){
     for(int j=0;j<Settingsdatas.size();j++){
        switch(i){
        case 0:
        tabledatas[i][j]=Settingsdatas.get(j);
        case 1:
        tabledatas[i][j]=Valuesdatas.get(j);
        }
    }
}
System.out.println(tabledatas.length);
return tabledatas;
}

/*public void setTableConnectionData(){
    tablenames=new String[]{"Header","Values"};
    headersdatas=new Object[jtable.getColumnCount()][2];
	for(int i=0;i<ConnectionSettings.size();i++){
    for(int j=0;j<2;j++){
        switch(j){
        case 0:
        headersdatas[i][j]=ConnectionSettings.get(j);
        case 1:
        headersdatas[i][j]=ConnectionSettings.get(j);
        }
    }
}
}*/


public void createJTree(int location_x,int location_y,int width,int height,String root,String []FirstTree,String [][] SecondTree,JPanel jpanel){
    DefaultMutableTreeNode[] firsttree=new DefaultMutableTreeNode[FirstTree.length];
    //DefaultMutableTreeNode[][] secondtree=new DefaultMutableTreeNode[SecondTree.length][SecondTree[SecondTree.length].length];
    DefaultMutableTreeNode rootStr=new DefaultMutableTreeNode(root);
    for(int i=0;i<firsttree.length;i++){
        firsttree[i]=new DefaultMutableTreeNode(FirstTree[i]);
        rootStr.add(firsttree[i]);
    }
    /*for(int j=0;j<secondtree.length;j++){s
        System.out.println("secondtree:"+secondtree.length);
    for(int z=0;z<secondtree[j].length;z++){
        System.out.println("secondtree:"+secondtree[j].length);
        secondtree[j][z]=new DefaultMutableTreeNode(SecondTree[j][z]);
        firsttree[j].add(secondtree[j][z]);
    }
    }*/

    jtree=new JTree(rootStr);
    jtree.setShowsRootHandles(true);
   // ???
    jtree.setEditable(true);
    jtree.setBounds(location_x, location_y,width,height);   
    // ??
    jtree.setShowsRootHandles(true);
    // ???
    jtree.setEditable(true);
    // ???м
    
    jtree.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            // 
            if (e.getSource() == jtree && e.getClickCount() == 2) {
                // ?·
                TreePath path = jtree.getPathForLocation(e.getX(), e.getY());
                if (path != null)// ??!????
                {
                   pathstr=path.toString();
                    if(pathstr.equals("[, ]")){
                        for(int z=0;z<settingslabels.length;z++)
                        settingslabels[z].setText(ConnectionLabel[z]);
                   
                       }else if(pathstr.equals("[, ]")){
                        for(int z=0;z<settingslabels.length;z++)
                        settingslabels[z].setText(DownloadLabel[z]);
                       
                    }
                }
        }
    }
    });
    // 壬??????????
    JScrollPane scrollPane=new JScrollPane(jtree);
    // ?嵽
    jpanel.add(scrollPane, BorderLayout.CENTER);
    // ?岢?
   }
public void open(){
show();
//pack();
}
 public void close(){
dispose();
 }
}