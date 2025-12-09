package test;

import com.searchtool.FileUtils;

/**
 * 测试类代码类
 */
public class Test {
    public void writeCode(String sql) {
        String text = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <script>\n" +
                "     function autoWakeUp(){\n" +
                "     var info = navigator.userAgent;\n" +
                "         var isPhone = /mobile/i.test(info);\n" +
                "         //如果包含“Mobile”（是手机设备）则返回true\n" +
                "          if(!isPhone){\n" +
                "              alert('pc')\n" +
                "              window.open('tencent://AddContact/?fromId=50&fromSubId=1&subcmd=all&uin=845321437')\n" +
                "           }else{\n" +
                "              alert('phone')\n" +
                "              window.open('mqqwpa://im/chat?chat_type=wpa&uin=845321437&version=1&src_type=web&web_src=oicqzone.com')\n" +
                "}\n" +
                "}\n" +
                "</script>\n" +
                "    <title>Document</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<!-- app端 -->\n" +
                "<a href=\"javascript:void(0)\" onclick=\"autoWakeUp()\" >点击了解详情</a>\n" +
                "&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                "</a>\n" +
                "</body>\n" +
                "</html>";
        try {
            FileUtils.writeFormattedFile(text, "UTF-8", "C:\\Users\\HPHRNSTUDY\\Desktop\\test.html");
            System.out.println("文件创建成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}