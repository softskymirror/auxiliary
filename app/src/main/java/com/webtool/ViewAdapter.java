package com.webtool;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.*;
import org.w3c.dom.Text;

import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
public class ViewAdapter {
    ButtonGroup bg;
    public void generateJFrame(JFrame jframe,int location_x,int location_y,int width,int height,String name,JPanel [] panels,JScrollPane [] scrollpanels,int close_operation,boolean isVisible){
        jframe.setBounds(location_x, location_y,width,height);
        jframe.setName(name);
        for(int i=0;i<panels.length;i++)
        jframe.add(panels[i]);
        for(int i=0;i<scrollpanels.length;i++)
        jframe.add(scrollpanels[i]);
        jframe.setLocationRelativeTo(null);
        jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jframe.setLayout(null);
        jframe.setVisible(isVisible);
    }
    public void generateJPanel(JPanel jpanel,int location_x,int location_y,int width,int height,String name,JComponent [] jcomponents,LayoutManager ls){
        jpanel.setBounds(location_x, location_y,width,height);
        
        jpanel.setName(name);
        jpanel.setLayout(ls);
        jpanel.setPreferredSize(new Dimension(width,height));
        for(int i=0;i<jcomponents.length;i++)
        jpanel.add(jcomponents[i]);
        
    }
    public void generateJScrollpanes(JScrollPane jScrollPane,int location_x,int location_y,int width,int height,String name,FocusListener fl){
    jScrollPane.setBounds(location_x, location_y,width,height);
    jScrollPane.setName(name);
    jScrollPane.setAutoscrolls(true);
    jScrollPane.setFocusable(false);   
    jScrollPane.addFocusListener(fl);
    }
    public void generateJTable(JTable jtables,int location_x,int location_y,int width,int height,String name,MouseAdapter ma,TableModelListener tm,FocusListener fl){
        jtables.setBounds(location_x, location_y,width,height);
        jtables.setName(name);
        jtables.setForeground(Color.BLACK);
        jtables.addMouseListener(ma);
        jtables.getModel().addTableModelListener(tm);
        jtables.addFocusListener(fl);
        
       
    }
    public void generateJButton(JButton jbutton,int location_x,int location_y,int width,int height,String name,ActionListener al){
        jbutton.setBounds(location_x, location_y,width,height);
        jbutton.setText(name);
        jbutton.addActionListener(al);
    }
    public void generateJTextArea(JTextArea jtextarea,int location_x,int location_y,int width,int height,String name,boolean isEitable,FocusListener fl){
        jtextarea.setBounds(location_x, location_y,width,height);
        jtextarea.setEditable(isEitable);
        jtextarea.setName(name);
        jtextarea.setLineWrap(true);
        jtextarea.setWrapStyleWord(true);
        jtextarea.addFocusListener(fl);

        
    }
    public void generateJTextField(JTextField jtextfield,int location_x,int location_y,int width,int height,String name,boolean isEditable,FocusListener fl){
        jtextfield.setBounds(location_x, location_y,width,height);
        jtextfield.setAutoscrolls(true);
        jtextfield.setEditable(isEditable);
        jtextfield.addFocusListener(fl);
        
    }
    public void generateJLabel(JLabel warnnings,int location_x,int location_y,int width,int height,String text,int size){
        
        warnnings.setBounds(location_x, location_y,width,height);
        warnnings.setText(text);
        warnnings.setFont(new Font("font", Font.BOLD, size));
    }
    public void generateJTree(JTree jtree,int location_x,int location_y,int width,int height,String [][] NodeNTree,TreeSelectionListener tsl){
        
        DefaultMutableTreeNode[][]defaultMutableTreeNodes=new DefaultMutableTreeNode[NodeNTree.length][NodeNTree[NodeNTree.length-1].length];
        for(int i=0;i<NodeNTree.length;i++){
        for(int j=0;j<NodeNTree[i].length;j++){
        defaultMutableTreeNodes[i][j]=new DefaultMutableTreeNode(NodeNTree[i][j]);
        System.out.println("i="+i+"j="+j+NodeNTree[i][j]);
        if(j!=0)
        defaultMutableTreeNodes[i][0].add(defaultMutableTreeNodes[i][j]);
       }
       }
       jtree = new JTree(defaultMutableTreeNodes[0][0]);
       jtree.setShowsRootHandles(false);
       // 设置树节点可编辑
       jtree.setEditable(false);
     
       jtree.setBounds(location_x, location_y,width,height);
       }

       public void generateJRadioButton(JRadioButton jr,int location_x,int location_y,int width,int height,String text,int size,ActionListener ac){
        jr.setBounds(location_x, location_y,width,height);
        jr.setText(text);
        jr.setFont(new Font("font", Font.BOLD, size));
        jr.addActionListener(ac);
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
