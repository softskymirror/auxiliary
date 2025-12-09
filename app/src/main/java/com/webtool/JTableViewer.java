package com.webtool;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
public class JTableViewer extends JTable{
    String []tablesnames;
    Object [][]tabledatas;
      public JTableViewer(String []names,Object [][]datas){
       this.tablesnames=names;
       this.tabledatas=datas;
       System.out.println("start init");
       init();
      }
      private void init() {
        DefaultTableModel dtm = new DefaultTableModel(tabledatas, tablesnames) {

            @Override
            public boolean isCellEditable(int row, int column) {
                
        
                addKeyListener(new KeyListener() {
                    public void keyReleased(KeyEvent e) {
                       
                    }

                    @Override
                    public void keyTyped(KeyEvent e) {
                        // TODO Auto-generated method stub
                        
                    }

                    @Override
                    public void keyPressed(KeyEvent e) {
                        // TODO Auto-generated method stub
                        
                    }

                 
              
                });
                        getModel().addTableModelListener((TableModelListener) new TableModelListener() {//?
                            @Override
                            public void tableChanged(TableModelEvent e) {
                                // TODO Auto-generated method stub
                                int type = e.getType();//()
                                int row = e.getFirstRow();//
                                int column = e.getColumn();//
                                if (type == TableModelEvent.INSERT) {//?""?
                                    System.out.println("?\"\"," + row + "" + column + "");
                                } else if (type == TableModelEvent.UPDATE) {
                                    System.out.println("?\"?\"," + row + "" + column + "");
                                } else if (type == TableModelEvent.DELETE) {
                                    System.out.println("?\"?\"," + row + "" + column + "");
                                } else {
                                    System.out.println("?");
                                }
                            }
                        });
                return true;
            }
        };
        JScrollPane js=new JScrollPane(this);
        setModel(dtm);
           addKeyListener(new KeyListener() {
           
            public void keyReleased(KeyEvent e) {
                event();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
    }
       
        private void event() {
            int row = getSelectedRow();
            int column = getSelectedColumn();
            DefaultCellEditor obj = (DefaultCellEditor) (getColumnModel().getColumn(column).getCellEditor());
            if (obj != null) {
                JComponent com = (JComponent) obj.getComponent();
                Object value = null;
                if (com instanceof JTextField) {
                    value = ((JTextField) com).getText();
                } else if (com instanceof JToggleButton) {
                    value = ((JToggleButton) com).isSelected();
                }
                System.out.println("row:" + row + " ,column:" + column + " ,value:"
                        + value);
            }
    
        }
}

