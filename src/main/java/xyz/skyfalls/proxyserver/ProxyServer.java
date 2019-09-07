package xyz.skyfalls.proxyserver;

import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptInitializer;
import com.github.monkeywie.proxyee.intercept.HttpProxyInterceptPipeline;
import com.github.monkeywie.proxyee.proxy.ProxyConfig;
import com.github.monkeywie.proxyee.proxy.ProxyType;
import com.github.monkeywie.proxyee.server.HttpProxyServer;
import com.github.monkeywie.proxyee.server.HttpProxyServerConfig;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProxyServer {
    public static void main(String[] args) throws IOException{
        File f;
        if(args.length != 1){
            f = new File("config.json");
        } else {
            f = new File(args[0]);
        }
        JSONObject json = new JSONObject(new String(Files.readAllBytes(Paths.get(f.toURI()))));
        System.out.println("Starting server...");
        int port = json.getInt("port");
        ProxyConfig proxyConfig = null;
        if(json.has("proxyIP")){
            String proxyIP = json.getString("proxyIP");
            int proxyPort = json.getInt("proxyPort");
            proxyConfig = new ProxyConfig(ProxyType.SOCKS5, proxyIP, proxyPort);
            System.out.println("Using proxy: " + proxyIP + ":" + proxyPort);
        }
        if(json.has("sessionLength")){
            long sessionLength = json.getLong("sessionLength");
            String secretUrl = json.getString("secretUrl");
            System.out.println("Using advanced mode: secretUrl=" + secretUrl + ",sessionLength=" + sessionLength);
            startServer(port, proxyConfig, secretUrl, sessionLength);
        } else {
            System.out.println("Using simple mode!");
            startServer(port, proxyConfig);
        }
    }

    private static void startServer(int port, ProxyConfig proxyConfig){
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        new HttpProxyServer()
                .serverConfig(config)
                .proxyConfig(proxyConfig)
                .httpProxyExceptionHandle(new ExceptionHandler())
                .start(port);
    }

    private static void startServer(int port, ProxyConfig proxyConfig, String secretUrl, long sessionLength){
        HttpProxyServerConfig config = new HttpProxyServerConfig();
        new HttpProxyServer()
                .serverConfig(config)
                .proxyConfig(proxyConfig)
                .proxyInterceptInitializer(new HttpProxyInterceptInitializer() {
                    @Override
                    public void init(HttpProxyInterceptPipeline pipeline){
                        pipeline.addFirst(new IdentityCheckInterceptor(secretUrl, sessionLength));
                    }
                })
                .httpProxyExceptionHandle(new ExceptionHandler())
                .start(port);
    }
}

