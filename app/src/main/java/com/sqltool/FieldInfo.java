package com.sqltool;


public class FieldInfo {
        // 列名
        private String columnName;
        // 数据类型常量
        public static final int TYPE_INT = 0;
        public static final int TYPE_VARCHAR = 1;
        public static final int TYPE_DATE = 2;
        public static final int TYPE_DATETIME = 3;
        public static final int TYPE_DECIMAL = 4;
        public static final int TYPE_TEXT = 5;
        // 更多类型可按需添加

        private int dataType;               // 数据类型常量
        private Integer length;              // 长度（如 VARCHAR(255)）
        private Integer precision;           // 精度（DECIMAL 总位数）
        private Integer scale;                // 小数位数（DECIMAL 小数点后位数）
        private boolean isUnsigned;           // 是否无符号（数值类型）
        private boolean isNotNull;            // 是否 NOT NULL
        private String defaultValue;          // 默认值（字符串形式）
        private boolean isAutoIncrement;      // 是否自增
        private boolean isPrimaryKey;         // 是否主键（简单单列主键）

        // 构造器、setter/getter 省略，请自行生成（可使用 IDE 自动生成）
        // 以下列出关键方法

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public void setUnsigned(boolean unsigned) {
        isUnsigned = unsigned;
    }

    public void setNotNull(boolean notNull) {
        isNotNull = notNull;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        isAutoIncrement = autoIncrement;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    /**
         * 生成该列的 SQL 定义片段，例如：`id` INT(11) NOT NULL AUTO_INCREMENT
         */
        public String toDefinition() {
            StringBuilder sb = new StringBuilder();
            sb.append("`").append(columnName).append("` ");
            sb.append(getDataTypeString());

            if (isUnsigned) {
                sb.append(" UNSIGNED");
            }
            if (isNotNull) {
                sb.append(" NOT NULL");
            }
            if (defaultValue != null) {
                sb.append(" DEFAULT ").append(formatDefaultValue(defaultValue));
            }
            if (isAutoIncrement) {
                sb.append(" AUTO_INCREMENT");
            }
            // 主键约束通常在表级别定义，此处不在列级别添加 PRIMARY KEY，避免复合主键问题
            return sb.toString();
        }

        /** 根据 dataType 和长度/精度生成类型字符串 */
        private String getDataTypeString() {
            switch (dataType) {
                case TYPE_INT:
                    return length != null ? "INT(" + length + ")" : "INT";
                case TYPE_VARCHAR:
                    if (length == null) length = 255; // 默认长度
                    return "VARCHAR(" + length + ")";
                case TYPE_DATE:
                    return "DATE";
                case TYPE_DATETIME:
                    return "DATETIME";
                case TYPE_DECIMAL:
                    if (precision != null && scale != null) {
                        return "DECIMAL(" + precision + "," + scale + ")";
                    } else if (precision != null) {
                        return "DECIMAL(" + precision + ")";
                    } else {
                        return "DECIMAL";
                    }
                case TYPE_TEXT:
                    return "TEXT";
                default:
                    return "VARCHAR(255)";
            }
        }

        /** 格式化默认值：字符串加引号，数字/函数不加 */
        private String formatDefaultValue(String value) {
            // 简单判断：如果 value 看起来是数字或函数（如 NOW()），不加引号
            if (value.matches("^\\d+(\\.\\d+)?$") || value.matches("^[A-Z_]+\\(.*\\)$")) {
                return value;
            }
            // 否则加单引号，并转义内部单引号
            return "'" + value.replace("'", "''") + "'";
        }

        // 其他 getter/setter ...
    }

//    public void setColumnName(String column_name) {
//        this.column_name = column_name;
//    }
//
//    public void setDefaultValue(String value){
//        this.defaultValue=value;
//    }
//
//    public void setDataType(int type){
//        switch (type){
//            case INT_TYPE:this.data_type="INT";break;
//            case CHAR_TYPE:this.data_type="VARCHAR";break;
//            case DATE_TYPE:this.data_type="DATE";break;
//        }
//    }
//    public String getColumnName(){
//      return column_name;
//    }
//
//    public String isNullautoText(){
//        String s = (isNull)?"":"NOT NULL";
//        return s;
//    }
//
//
//
//    public String getDataType(){
//        return data_type;
//    }

