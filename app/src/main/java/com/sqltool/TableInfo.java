package com.sqltool;

import java.util.ArrayList;

/**
 * Package all the data in the table, according to the table structure and field types.
 */
public class TableInfo {

    private String table_name;

    private ArrayList<FiledInfo> fields=new ArrayList<>();

    private String character_set_type;

    private String collate_type;

    public void setTableName(String name){
     this.table_name=name;
    }

    public void setFields(ArrayList<FiledInfo> fields){
     this.fields=fields;
    }

    public void setCharsetType(String charset){
       this.character_set_type=charset;
    }

    public void setCollation(String collate){
      this.collate_type=collate;
    }

    public String getTableName(){
        return table_name;
    }
    public int getFieldsLength(){
     return fields.size();
    }

    public FiledInfo getFlied(int index){
        FiledInfo info=new FiledInfo();
        if(getFieldsLength()!=0) info=fields.get(index);
        return info;
    }
}
