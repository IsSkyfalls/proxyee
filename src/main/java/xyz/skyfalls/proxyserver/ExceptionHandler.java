package xyz.skyfalls.proxyserver;

import com.github.monkeywie.proxyee.exception.HttpProxyExceptionHandle;
import io.netty.channel.Channel;

public class ExceptionHandler extends HttpProxyExceptionHandle {
    @Override
    public void beforeCatch(Channel clientChannel, Throwable cause) throws Exception{
        System.out.println("Exception: " + cause.toString() + " Caused by: " + clientChannel.remoteAddress().toString());
    }

    @Override
    public void afterCatch(Channel clientChannel, Channel proxyChannel, Throwable cause) throws Exception{
        clientChannel.close();
    }
}
