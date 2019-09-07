package xyz.skyfalls.proxyserver;

import com.github.monkeywie.proxyee.intercept.HttpProxyIntercept;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.util.HttpUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

public class IdentityCheckInterceptor extends HttpProxyIntercept {
    private static Map<String, Long> users = new Hashtable<>();
    private String secretRegex;
    private long sessionLength;

    public IdentityCheckInterceptor(String secretUrl, long sessionLength){
        this.secretRegex = "^" + secretUrl + "$";
        this.sessionLength = sessionLength;
    }

    @Override
    public void beforeRequest(Channel clientChannel, HttpRequest httpRequest, HttpProxyInterceptPipeline pipeline) throws Exception{
        String addr = ((InetSocketAddress) clientChannel.remoteAddress()).getHostString();
        System.out.println("request to " + httpRequest.headers().get(HttpHeaderNames.HOST));
        if(HttpUtil.checkUrl(pipeline.getHttpRequest(), secretRegex)){
            HttpResponse hookResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            hookResponse.setStatus(HttpResponseStatus.FOUND);
            hookResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
            clientChannel.write(hookResponse);
            clientChannel.write(Unpooled.copiedBuffer("成功!TimeStamp=" + System.currentTimeMillis() + ",SessionLength=" + sessionLength, CharsetUtil.UTF_8));
            HttpContent lastContent = new DefaultLastHttpContent();
            clientChannel.write(lastContent);
            clientChannel.flush();
            clientChannel.close();
            System.out.println("Session refresh: " + addr);
            users.put(addr, System.currentTimeMillis() + sessionLength);
            return;
        }
        if(users.containsKey(addr)){
            long end = users.get(addr);
            if(end < System.currentTimeMillis()){
                users.remove(addr);
                clientChannel.close();
                System.out.println("Session ended: " + addr);
            } else {
                pipeline.beforeRequest(clientChannel, httpRequest);
                return;
            }
        }
        System.out.println("Not authenticated: " + addr);
        clientChannel.close();
    }

    @Override
    public void afterResponse(Channel clientChannel, Channel proxyChannel, HttpResponse httpResponse, HttpProxyInterceptPipeline pipeline) throws Exception{
        pipeline.afterResponse(clientChannel, proxyChannel, httpResponse);
    }
}
