package com.sqltool;

public class FiledInfo {
    public String column_name;

    public String data_type;

    public boolean isNull=false;

    public String defaultValue;

    public static final int  INT_TYPE=0;

    public static final int CHAR_TYPE=1;

    public static final int DATE_TYPE=2;

    public void setColumnName(String column_name) {
        this.column_name = column_name;
    }

    public void setDefaultValue(String value){
        this.defaultValue=value;
    }

    public void setDataType(int type){
        switch (type){
            case INT_TYPE:this.data_type="INT";break;
            case CHAR_TYPE:this.data_type="VARCHAR";break;
            case DATE_TYPE:this.data_type="DATE";break;
        }
    }
    public String getColumnName(){
      return column_name;
    }

    public String isNullautoText(){
        String s = (isNull)?"":"NOT NULL";
        return s;
    }



    public String getDataType(){
        return data_type;
    }
}
