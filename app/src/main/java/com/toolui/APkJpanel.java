package com.toolui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class APkJpanel extends JPanel {

    JPanel apk_source, dex4j_tool_source, jdgui_tool_source,output_path;
    JLabel source, dex4j, jdgui,output;
    JTextArea sourcepath, dex4jtool, jdguitool,outputpath;
    JButton findapksource, finddex4jtool, findjdguitoolsource,findoutputpath;
    JButton decompilation;//
    private int signal;
    String file_dir_path=null;
    JProgressBar jprogressbar=null;
    JTreeDir jTreeDir = new JTreeDir();
    ViewAdapter v = new ViewAdapter();
    JFileChooser fc=new JFileChooser();
    /**
     * ��װAPKҳ��������
     * @param frame
     */
    public APkJpanel(JFrame frame) {
        apk_source = new JPanel();
        dex4j_tool_source = new JPanel();
        jdgui_tool_source = new JPanel();
        output_path=new JPanel();
        String[] jpanel_input_text = new String[]{"apk��·��", "dex4j���߰�װ·��", "jd-gui��װ·��","apk�ѿǻ�������ļ���"};
        source= new JLabel();
        dex4j= new JLabel();
        jdgui= new JLabel();
        output=new JLabel();
        sourcepath = new JTextArea();
        dex4jtool = new JTextArea();
        jdguitool = new JTextArea();
        outputpath=new JTextArea();
        findapksource = new JButton();
        finddex4jtool = new JButton();
        findjdguitoolsource = new JButton();
        findoutputpath=new JButton();
        decompilation= new JButton("������");
        jprogressbar=new JProgressBar();
        JLabel[] jlabels = new JLabel[]{source, dex4j, jdgui,output};
        JTextArea[] jtextareas = new JTextArea[]{sourcepath, dex4jtool,jdguitool,outputpath};
        JButton[] jbuttons = new JButton[]{findapksource,finddex4jtool,findjdguitoolsource,findoutputpath};
        int i = 0;
        for(JLabel label:jlabels){
            //��panel�������λ����������ڸ�panel���ֶ���
            v.generateJLabel(label,frame.getWidth()/18,0, frame.getWidth()/8, frame.getHeight()/25, jpanel_input_text[i], 12, false);
            i++;
        }
        i=0;
        for(JTextArea jtextarea:jtextareas){
            v.generateJTextArea(jtextarea,frame.getWidth()/4,0, frame.getWidth()/4, frame.getHeight()/25, "�����밲װ�ļ���Ŀ�������ļ�·��", 12, false, true, null, null);
            i++;
        }
         i=0;
        for(JButton jbutton:jbuttons){
            int finalI = i;
            v.generateJButton(jbutton, frame.getWidth() / 2, 0, frame.getWidth() / 30, frame.getHeight() / 25, "...", 12, true, new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                  chooseFile(jtextareas[finalI]);
              }
          });
          i++;
         }
        v.generateJPanel(apk_source, frame.getWidth()/3, (frame.getHeight()/25)*1, frame.getWidth(), frame.getHeight()/15, "apk_source", new JComponent[]{source, sourcepath, findapksource}, null);
        v.generateJPanel(dex4j_tool_source, frame.getWidth()/3, (frame.getHeight()/25)*3, frame.getWidth(), frame.getHeight()/15, "dex4j_tool_source", new JComponent[]{dex4j, dex4jtool, finddex4jtool}, null);
        v.generateJPanel(jdgui_tool_source, frame.getWidth()/3, (frame.getHeight()/25)*5, frame.getWidth(), frame.getHeight()/15, "jdgui_tool_source", new JComponent[]{jdgui, jdguitool, findjdguitoolsource}, null);
        v.generateJPanel(output_path, frame.getWidth()/3, (frame.getHeight()/25)*7,frame.getWidth(), frame.getHeight()/15, "output_path_source", new JComponent[]{output,outputpath, findoutputpath}, null);
        v.generateJButton(decompilation, frame.getWidth() / 3, (frame.getHeight() / 25) * 9, frame.getWidth() / 3, frame.getHeight() / 15, "������", 12, true, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jprogressbar.setIndeterminate(true);
                jprogressbar.setString("���ڷ�������.....");
            }
        });
        v.generateJProgressBar(jprogressbar,(int)(frame.getWidth()*0.9D/3D),(frame.getHeight()/25)*11,(int)(frame.getWidth()*(2D/3D)), frame.getHeight()/15,"׼��������",true,false);
        v.generateJPanel(this,this.getWidth()/5,0, frame.getWidth() , frame.getHeight(), "tool_source", new JComponent[]{apk_source, dex4j_tool_source, jdgui_tool_source,output_path, decompilation,jprogressbar}, null);
    }
    /**
     * ѡ���ļ�
     */
    public void chooseFile(JTextArea output_component) {
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        try {
            signal=fc.showOpenDialog(null);
        }catch(HeadlessException e) {

        }
        if(signal==JFileChooser.APPROVE_OPTION) {
            File f=fc.getSelectedFile();
            file_dir_path=f.getPath();
            try {
                output_component.setText(file_dir_path);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
//            choosefile.setText(filetext+filedirectorypath);
        }
    }

}
