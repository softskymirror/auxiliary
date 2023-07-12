package com.toolui;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.*;
import org.w3c.dom.Text;

import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.*;
import java.awt.BorderLayout;
public class ViewAdapter {
    ButtonGroup bg;
    public void generateJFrame(JFrame jframe,int location_x,int location_y,int width,int height,String name,JComponent [] jcomponents,int close_operation){

        jframe.setName(name);

        jframe.setLocationRelativeTo(null);
        jframe.setDefaultCloseOperation(close_operation);
        jframe.setLayout(null);
        jframe.setVisible(true);
        jframe.setBounds(location_x, location_y,width,height);
        for(JComponent c:jcomponents)
            jframe.getContentPane().add(c);
    
        
    }
    public void generateJPanel(JPanel jpanel,int location_x,int location_y,int width,int height,String name,JComponent [] jcomponents,LayoutManager ls){
        jpanel.setBounds(location_x, location_y,width,height);
        jpanel.setName(name);
        jpanel.setLayout(ls);
        for(JComponent c:jcomponents)
        jpanel.add(c);
    }
    public void generateJScrollpanes(JScrollPane jScrollPane,int location_x,int location_y,int width,int height,String name){
    jScrollPane.setBounds(location_x, location_y,width,height);
    jScrollPane.setName(name);
    jScrollPane.setAutoscrolls(true);
    jScrollPane.setFocusable(false); 
    jScrollPane.setVisible(true); 
    }
    public void generateJTable(JTable jtables,int location_x,int location_y,int width,int height,String name,MouseAdapter ma){
        jtables.setBounds(location_x, location_y,width,height);
        jtables.setName(name);
        jtables.addMouseListener(ma);
       
    }
    public void generateJButton(JButton jbutton,int location_x,int location_y,int width,int height,String name,int size,boolean isEnabled,ActionListener al){
        jbutton.setBounds(location_x, location_y,width,height);
        jbutton.setEnabled(isEnabled);
        jbutton.setText(name);
        jbutton.setFont(new Font("Courier", Font.PLAIN, size));
        jbutton.addActionListener(al);
    }
    public void generateJTextArea(JTextArea jtextarea,int location_x,int location_y,int width,int height,String name,int size,boolean isEitable,boolean isEnabled,FocusListener fl,MouseListener ml){
        jtextarea.setBounds(location_x, location_y,width,height);
        jtextarea.setEditable(isEitable);
        jtextarea.setEnabled(isEnabled);
        jtextarea.setName(name);
        jtextarea.setLineWrap(true);
        jtextarea.setFont(new Font("Courier", Font.PLAIN, size));
        jtextarea.setWrapStyleWord(true);
        jtextarea.addFocusListener(fl);
        jtextarea.addMouseListener(ml);
        
    }
    public void generateJTextField(JTextField jtextfield,int location_x,int location_y,int width,int height,String name){
        jtextfield.setBounds(location_x, location_y,width,height);
        jtextfield.setAutoscrolls(true);
        jtextfield.setEditable(false);
        
    }
    public void generateJLabel(JLabel warnnings,int location_x,int location_y,int width,int height,String text,int size,boolean isAutoScrolls){
        warnnings.setAutoscrolls(isAutoScrolls);
        warnnings.setBounds(location_x, location_y,width,height);
        warnnings.setText(text);
        warnnings.setFont(new Font("font", Font.BOLD, size));
    }
    public void generateJTree(JTree jtree,int location_x,int location_y,int width,int height,String [][] NodeNTree,TreeSelectionListener tsl){
        
           if(NodeNTree!=null) {
               DefaultMutableTreeNode[][]defaultMutableTreeNodes=new DefaultMutableTreeNode[NodeNTree.length][NodeNTree[NodeNTree.length-1].length];
               for (int i = 0; i < NodeNTree.length; i++) {
               for (int j = 0; j < NodeNTree[i].length; j++) {
                   defaultMutableTreeNodes[i][j] = new DefaultMutableTreeNode(NodeNTree[i][j]);
                   System.out.println("i=" + i + "j=" + j + NodeNTree[i][j]);
                   if (j != 0)
                       defaultMutableTreeNodes[i][0].add(defaultMutableTreeNodes[i][j]);
               }
           }
           jtree = new JTree(defaultMutableTreeNodes[0][0]);
       }
       jtree.setShowsRootHandles(false);
       // 设置树节点可编辑
       jtree.setEditable(false);
       jtree.setBounds(location_x, location_y,width,height);
       }

       public void generateJRadioButton(JRadioButton jr,int location_x,int location_y,int width,int height,String text,int size,boolean isSelected,ActionListener ac){
        jr.setBounds(location_x, location_y,width,height);
        jr.setText(text);
        jr.setFont(new Font("font", Font.BOLD, size));
        jr.setSelected(isSelected);
        jr.addActionListener(ac);
       }

    /**
     * JProgressBar初始化
     */
    public void generateJProgressBar( JProgressBar progressBar,int location_x,int location_y,int width,int height,String text,boolean isShowString,boolean isIndeterminate){
        progressBar.setStringPainted(isShowString);// 设置显示提示信息
        progressBar.setIndeterminate(isIndeterminate);// 设置采用不确定进度条
        progressBar.setString(text);// 设置提示信息
        progressBar.setBounds(location_x,location_y,width,height);
       }

       public void generateJTextPane(JTextPane jtextpane,int location_x,int location_y,int width,int height,String name,boolean isEitable,FocusListener fl,MouseListener ml){
        jtextpane.setBounds(location_x, location_y,width,height);
        jtextpane.setEditable(isEitable);
        jtextpane.setName(name);
        jtextpane.setAutoscrolls(true);
       
        jtextpane.addFocusListener(fl);
        jtextpane.addMouseListener(ml);
       }
       public void generateButtonGroup(ButtonGroup buttongroup,JRadioButton [] jRadioButtons){
        for(int i=0;i<jRadioButtons.length;i++){
            buttongroup.add(jRadioButtons[i]);
        }
        
       }

       public void generateCombobox(JComboBox jc,int location_x,int location_y,int width,int height,ItemListener il){
        jc.setBounds(location_x, location_y,width,height);
        jc.addItemListener(il);
       }
     
}
