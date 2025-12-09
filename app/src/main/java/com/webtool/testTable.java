package com.webtool;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
public class testTable extends JFrame {//
    private DefaultTableModel model;//ģ
    private JTable table;//
    private JButton addButton, delButton, updButton;//ɾť
    private JTextField aTextField, bTextField;//ťԱߵ
    public testTable() {//幹췽
        setTitle("ģ");
        setBounds(100, 100, 400, 200);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        String[] columnNames = {"A","b"};//
        String[][] tableValues=new String[2][0];//
        model = new DefaultTableModel(tableValues, columnNames);//ģ
        table = new JTable(model);//ģͣtable.setModel(model);
        JScrollPane sc = new JScrollPane(table);
        getContentPane().add(sc, BorderLayout.CENTER);
        buttonInit();//ðťťʼ
        addMyListener();//ļ¼    }
    }
    private void buttonInit(){ 
        final JPanel panel = new JPanel();
        aTextField = new JTextField("A4", 5);
        panel.add(new Label("B:"));
        bTextField = new JTextField("B4", 5);
        panel.add(bTextField);
        addButton = new JButton("");
        delButton = new JButton("ɾ");
        updButton = new JButton("޸");
        panel.add(addButton);
        panel.add(delButton);
        panel.add(updButton);
    }
      
       
    
   
    private void addMyListener() {
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String rowData[] = {aTextField.getText(), bTextField.getText()};
                model.addRow(rowData);//ڱģһ(ı)
                int rowCount = table.getRowCount() + 1;//ȡǰ+1
                aTextField.setText("A" + rowCount);//ı1
                bTextField.setText("B" + rowCount);
            }
        });
        updButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();//ȡѡе
                if (selectedRow != -1) {//ڱѡ
                    model.setValueAt(aTextField.getText(), selectedRow, 0);//޸ĵ1еֵ
                    model.setValueAt(bTextField.getText(), selectedRow, 1);//޸ĵ2еֵ                }
            }
        }});
        delButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow != -1) {
                    model.removeRow(selectedRow);//ɾѡе                }
            }
        }});
        model.addTableModelListener(new TableModelListener() {//ģͼ
            public void tableChanged(TableModelEvent e) {
                int type = e.getType();//ȡ¼(ɾĵ)
                int row = e.getFirstRow();//ȡ¼
                int column = e.getColumn();//ȡ¼
                if (type == TableModelEvent.INSERT) {//""¼
                    System.out.println("¼\"\"," + row + "" + column + "");
                } else if (type == TableModelEvent.UPDATE) {
                    System.out.println("¼\"޸\"," + row + "" + column + "");
                } else if (type == TableModelEvent.DELETE) {
                    System.out.println("¼\"ɾ\"," + row + "" + column + "");
                } else {
                    System.out.println("¼ԭ򴥷");
                }
            }
        });
    }
 
    public static void main(String[] args) {
        testTable frame = new testTable();
        frame.setVisible(true);
    }
}

