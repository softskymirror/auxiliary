package com.toolui;

import javax.swing.*;
import javax.swing.text.View;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainUI extends JFrame{
    ViewAdapter v=new ViewAdapter();
    Container container=new Container();
    public static int IO_PANEL_ID=0;
    public static int NET_PANEL_ID=1;
    public static int APK_PANEL_ID=2;
    public static int PAGE_ID=2;
    JPanel io_jpanel=new JPanel();
    JPanel net_jpanel=new JPanel();
    APkJpanel apk_jpanel;
    JTree root_tree;
    JPanel[] helper_jpanel;
    public MainUI(){
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        root_tree=new JTreeDir().JTreeDir(this);
        setBounds(screenSize.width/12,screenSize.height/12,(int)(screenSize.width*(2D/3D)),(int)(screenSize.height*(2D/3D)));
        v.generateJFrame(this,screenSize.width/12,screenSize.height/12,(int)(screenSize.width*(2D/3D)),(int)(screenSize.height*(2D/3D)),"开发辅助工具包",new JComponent[]{root_tree=new JTreeDir().JTreeDir(this),(helper_jpanel=new JPanel[]{io_jpanel,net_jpanel,apk_jpanel=new APkJpanel(this)})[PAGE_ID]},EXIT_ON_CLOSE);	//设置窗体大小
        getContentPane().setBounds(screenSize.width/12,screenSize.height/12,(int)(screenSize.width*(2D/3D)),(int)(screenSize.height*(2D/3D)));
        root_tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 如果在这棵树上点击了2次,即双击
                if (e.getSource() == root_tree && e.getClickCount() == 2) {
                    // 按照鼠标点击的坐标点获取路径
                    TreePath selPath = root_tree.getPathForLocation(e.getX(), e.getY());
                    if (selPath != null)// 谨防空指针异常!双击空白处是会这样
                    {
                        System.out.println(selPath);// 输出路径看一下
                        // 获取这个路径上的最后一个组件,也就是双击的地方
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                        if(node.toString().equals("Apktool Helper")){
                            helper_jpanel[PAGE_ID].setVisible(false);
                            helper_jpanel[PAGE_ID=MainUI.APK_PANEL_ID].setVisible(true);
                        }else if(node.toString().equals("IO Helper")){
                            helper_jpanel[PAGE_ID].setVisible(false);
                            helper_jpanel[PAGE_ID=MainUI.IO_PANEL_ID].setVisible(true);
                        }else if(node.toString().equals("Net Helper")){
                            helper_jpanel[PAGE_ID].setVisible(false);
                            helper_jpanel[PAGE_ID=MainUI.NET_PANEL_ID].setVisible(true);
                        }
                        System.out.println(PAGE_ID);
                    }
                }
            }
        });
           setResizable(false);
         setVisible(true);			//可视化窗体
    }


}
