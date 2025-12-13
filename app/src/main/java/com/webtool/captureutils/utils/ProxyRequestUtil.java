package com.webtool.captureutils.utils;


import com.webtool.captureutils.bean.ClientRequest;
import com.webtool.captureutils.bean.Constans;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import static com.webtool.captureutils.bean.Constans.CLIENTREQUEST_ATTRIBUTE_KEY;

/**s
 *
 * @author puhaiyang
 * created on 2019/10/25 23:12
 */
public class ProxyRequestUtil {
    private ProxyRequestUtil() {
    }

    /**
     * ��ȡ��������
     *
     * @param httpRequest http����
     */
    public static ClientRequest getClientReuqest(HttpRequest httpRequest) {

        String host = httpRequest.headers().get("host");
        //��host�л�ȡ���˿�
        String[] hostStrArr = host.split(":");
        int port = 80;
        if (hostStrArr.length > 1){
            port = Integer.parseInt(hostStrArr[1]);
        } else if (httpRequest.getUri().startsWith(Constans.HTTPS_PROTOCOL_NAME)) {
            port = 443;
        }
        return new ClientRequest(hostStrArr[0], port);
    }

    /**
     * ��channel�л�ȡclientRequest
     */
    public static ClientRequest getClientRequest(Channel channel) {
        //��clientRequesst���浽channel��
        Attribute<ClientRequest> clientRequestAttribute = channel.attr(CLIENTREQUEST_ATTRIBUTE_KEY);
        return clientRequestAttribute.get();
    }
}
