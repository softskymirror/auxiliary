package com.webtool;

import com.eclipsesource.v8.V8;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AutoCookieRequest {

    /**
     * pdf?URL, ??PDF?
     *
     * @param get        pdf?URL?
     * @return              pdf?
     */
  /* public static File getPDFFile(String pdfUrl) {
        String fileName = Utils.MD5(pdfUrl);
        File file = new File("pdfFile/" + fileName + ".pdf");
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpGet get = new HttpGet(pdfUrl);
            setHeader(get);
            HttpResponse response = (HttpResponse) client.execute(get);
            String __jsluid = getJsluid(response);
            String body = getResponseBodyAsString(response);
            String __jsl_clearance = getJslClearance(body);
            get = new HttpGet(pdfUrl);
            get.setHeader("cookie", __jsluid + "; " + __jsl_clearance);
            setHeader(get);
            response = client.execute(get);
            output(response, file);
        } catch (Exception e) {
            lg.error(e.getMessage(), e);
        }
        return file;
    }

    /**
     * HttpGet?Щ?header
     *
     * @param get           ?getpdf?
     */
    private static void setHeader(HttpGet get) {
        get.setHeader("Upgrade-Insecure-Requests", "1");
        get.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.110 Safari/537.36");
    }

    /**
     * HttpResponse
     *
     * @param response      http?
     * @param file          ?
     * @throws IOException  IO?
     */
    private static void output(HttpResponse response, File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(getResponseBodyAsBytes(response));
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    /**
     * ????JavaScript?,
     * ?cookie? __jsl_clearance?
     *
     * @param body          ?(??????js?)
     * @return              cookie? __jsl_clearance?
     */
    private static String getJslClearance(String body) {
        //V8:?迪?JavaScript??. :globalAlias=window, ?window???,
        // V8JavaScript?, ??window?.
        V8 runtime = V8.createV8Runtime("window");
        //?pdf?????V8?еJavaScript
        body = body.trim()
                .replace("<script>", "")
                .replace("</script>", "")
                .replace("eval(y.replace(/\\b\\w+\\b/g, function(y){return x[f(y,z)-1]||(\"_\"+y)}))",
                        "y.replace(/\\b\\w+\\b/g, function(y){return x[f(y,z)-1]||(\"_\"+y)})");
        //V8?и?δ????JavaScript?
        String result = runtime.executeStringScript(body);

        //? jsl_clearance ??, ?: 1543915851.312|0|
        String startStr = "document.cookie='";
        int i1 = result.indexOf(startStr) + startStr.length();
        int i2 = result.indexOf("|0|");
        String cookie1 = result.substring(i1, i2 + 3);
        /*
        ? jsl_clearance ??,?: DW2jqgJO5Bo45yYRKLlFbnqQuD0%3D
        ??: ???JavaScript??cookie, cookie?__jsl_clearance
        е??(?:1543915851.312|0|)?д, ???.
        ??JavaScript, ?V8з,
        ú???Щ?λ, ?, V8.
         */
        startStr = "|0|'+(function(){";
        int i3 = result.indexOf(startStr) + startStr.length();
        int i4 = result.indexOf("})()+';Expires");
        String code = result.substring(i3, i4).replace(";return", ";");
        String cookie2 = runtime.executeStringScript(code);

        /*
        ??, jsl_clearance?.
        ?: 1543915851.312|0|DW2jqgJO5Bo45yYRKLlFbnqQuD0%3D
        */
        return cookie1 + cookie2;
    }

    /**
     * HTTP????
     *
     * @param response      HTTP?
     * @return              ???
     * @throws IOException  IO?
     */
    private static String getResponseBodyAsString(HttpResponse response) throws IOException {
        return IOUtils.toString(response.getEntity().getContent(), "UTF-8");
    }

    /**
     * HTTP???byte鷵
     *
     * @param response      HTTP?
     * @return              ?byte?
     * @throws IOException  IO?
     */
    private static byte[] getResponseBodyAsBytes(HttpResponse response) throws IOException {
        return IOUtils.toByteArray(response.getEntity().getContent());
     
    }

    /**
     * ???set-cookie
     * ?cookie?__jsluid?
     * @param response      HttpResponse
     * @return              __jsluid?
     */
    private static String getJsluid(HttpResponse response) {
        Header header = response.getFirstHeader("set-cookie");
        String[] split = header.getValue().split(";");
        for (String s : split) {
            if (s.contains("__jsluid")) {
                return s.trim();
            }
        }
        return "";
    }
}

