/*
 *
 * MIT License
 *
 * Copyright (c) 2017 朱辉 https://blog.yeetor.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.adbtool.util;

import com.adbtool.androidcontrol.DeviceInfo;
import com.adbtool.androidcontrol.GroupInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成XML数据结构的数据封装类
 */
public class XMLRange {
    //创建节点分级数组
     int type;
    private ArrayList<List<String>> level_nodes;
    private ArrayList<List<String>> node_attributes;
    private ArrayList<List<String>> attribute_values;
    private ArrayList<List<DeviceInfo>> devices_node_texts;
    private ArrayList<List<GroupInfo>> groups_node_texts;
    private List<String> nodes;
    private List<String> attributes;
    private List<String> values;
    private List<String> texts;


    public XMLRange(ArrayList<List<String>> level_nodes,ArrayList<List<String>> node_attributes,ArrayList<List<String>> attribute_values,ArrayList node_texts){
        this.level_nodes=level_nodes;
        this.node_attributes=node_attributes;
        this.attribute_values=attribute_values;
        if(type==XMLUtil.Device_Xml) this.devices_node_texts=node_texts;
        if(type==XMLUtil.Device_Xml) this.groups_node_texts=node_texts;
    }



    public ArrayList<List<String>> getLevelNodes(){ return level_nodes; }

    public ArrayList<List<String>> getNodeAttributes(){
        return node_attributes;
    }
    public ArrayList<List<String>> getAttributeValues(){
        return attribute_values;
    }
    public ArrayList<List<DeviceInfo>> getDevicesNodeTexts(){
        return devices_node_texts;
    }
    public ArrayList<List<GroupInfo>> getGroupsNodeTexts(){return groups_node_texts;}
    public int getNodesArraySize(){return getLevelNodes().size();}
    public int getAttributesArraySize(){return getNodeAttributes().size();}
    public int getValuesArraySize(){return getAttributeValues().size();}
    public int getDeviceTextsArraySize(){return getDevicesNodeTexts().size();}
    public int getGroupTextsArraySize(){return getGroupsNodeTexts().size();}
    public int getNodesSize(int index){return getLevelNodes().get(index).size();}
    public int getAttributesSize(int index){return getNodeAttributes().get(index).size();}
    public int getValuesSize(int index){return getAttributeValues().get(index).size();}
    public int getDeviceTextsSize(int index){return getDevicesNodeTexts().get(index).size();}
    public int getGroupTextsSize(int index){return getGroupsNodeTexts().get(index).size();}
    public List<String> getLevelNode(int index){return getLevelNodes().get(index); }
    public List<String> getNodeAttribute(int index){
        return getNodeAttributes().get(index);
    }
    public List<String> getAttributeValue(int index){
        return getAttributeValues().get(index);
    }
    public List<DeviceInfo> getDeviceNodeText(int index){
        return getDevicesNodeTexts().get(index);
    }
    public List<GroupInfo> getGroupNodeText(int index){
        return getGroupsNodeTexts().get(index);
    }
    public String getNode(int list_index,int index){
        return getLevelNode(list_index).get(index);
    }

    public String getAttribute(int list_index,int index){
        return getNodeAttribute(list_index).get(index);
    }

    public String getValue(int list_index,int index){
        return getAttributeValue(list_index).get(index);
    }

    public DeviceInfo getDeviceInfo(int list_index,int index){ return getDeviceNodeText(list_index).get(index);
    }

    public GroupInfo getGroupInfo(int list_index,int index){
        return getGroupNodeText(list_index).get(index);
    }

    /**
     * 添加节点分级组
     * @param nodes
     */
    public void setLevelNodes(ArrayList<List<String>> nodes){
        this.level_nodes=nodes;
    }

    /**
     * 添加节点属性组
     * @param attributes
     */
    public void setNodeAttributes(ArrayList<List<String>> attributes){
        this.node_attributes=attributes;
    }

    /**
     * 添加节点数值组
     * @param values
     */
    public void setAttributeValues( ArrayList<List<String>> values){
        this.attribute_values=values;
    }

    /**
     * 添加设备信息类数组节点内容组
     * @param texts
     */
    public void setDevicesNodeTexts(ArrayList<List<DeviceInfo>> texts){
        this.devices_node_texts=texts;
    }


    /**
     * 添加Group信息类数组节点内容组
     * @param texts
     */
    public void setGroupsNodeTexts(ArrayList<List<GroupInfo>> texts){
        this.groups_node_texts=texts;
    }

}
