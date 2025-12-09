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
    // DefaultMutableTreeNode
    DefaultMutableTreeNode tool_helper_classify = getTreeNode("");
    DefaultMutableTreeNode io_helper_root=getTreeNode("IO Helper");//
    DefaultMutableTreeNode net_help_root=getTreeNode("Net Helper");//
    DefaultMutableTreeNode apktool_help_root=getTreeNode("Apktool Helper");//
    DefaultMutableTreeNode []tool_helper_tree_nodes=new DefaultMutableTreeNode[]{io_helper_root,net_help_root,apktool_help_root};
    for(DefaultMutableTreeNode node:tool_helper_tree_nodes)
    tool_helper_classify.add(node);
    // ��ʵ����TreeModel�ӿڵ�DefaultTreeModel����ָ�����ĸ����
    DefaultTreeModel dtm = new DefaultTreeModel(tool_helper_classify);
    // ����������Ϳ���ȥ����JTree����
    JTree jt = new JTree(dtm);
    // Ϊ�����ע�������,����������������д���������
    v.generateJTree(jt, 0,0,jFrame.getSize().width/4,jFrame.getSize().height,null,null);
    return jt;
    }

    public DefaultMutableTreeNode getTreeNode(String node){
        return new DefaultMutableTreeNode(node);
        // �ý��֮��add�ķ�����ʵ��Ƕ��(���������ĸ��ӹ�ϵ)
    }
}
