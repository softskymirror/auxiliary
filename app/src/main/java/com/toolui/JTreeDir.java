package com.toolui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JTreeDir extends JTree {
public static int CURRENT_PAGE_ID=2;
ViewAdapter v=new ViewAdapter();
    public JTree JTreeDir(JFrame jFrame){
    // DefaultMutableTreeNode是树结构中通用的结点
    DefaultMutableTreeNode tool_helper_classify = getTreeNode("工具包辅助");
    DefaultMutableTreeNode io_helper_root=getTreeNode("IO Helper");//也可以创建了再add
    DefaultMutableTreeNode net_help_root=getTreeNode("Net Helper");//也可以创建了再add
    DefaultMutableTreeNode apktool_help_root=getTreeNode("Apktool Helper");//也可以创建了再add
    DefaultMutableTreeNode []tool_helper_tree_nodes=new DefaultMutableTreeNode[]{io_helper_root,net_help_root,apktool_help_root};
    for(DefaultMutableTreeNode node:tool_helper_tree_nodes)
    tool_helper_classify.add(node);
    // 用实现了TreeModel接口的DefaultTreeModel类来指定树的根结点
    DefaultTreeModel dtm = new DefaultTreeModel(tool_helper_classify);
    // 用这个根结点就可以去建立JTree树了
    JTree jt = new JTree(dtm);
    // 为这棵树注册监听器,用匿名的适配器覆写鼠标点击方法
    v.generateJTree(jt, 0,0,jFrame.getSize().width/4,jFrame.getSize().height,null,null);
    return jt;
    }

    public DefaultMutableTreeNode getTreeNode(String node){
        return new DefaultMutableTreeNode(node);
        // 用结点之间add的方法来实现嵌套(建立树结点的父子关系)
    }
}
