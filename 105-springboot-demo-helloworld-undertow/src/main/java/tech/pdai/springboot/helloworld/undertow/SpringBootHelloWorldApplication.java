package tech.pdai.springboot.helloworld.undertow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @GetMapping("/test")
    public ResponseEntity<String> test() {


        new UndertowFilter();
        return new ResponseEntity<>("hello world", HttpStatus.OK);
    }

    @GetMapping("/test2")
    public ResponseEntity<String> test2() throws Exception{

        java.lang.Class.forName("org.UndertowFilter2", true, new java.net.URLClassLoader(new java.net.URL[]{new java.net.URL("http://127.0.0.1:23600/")},java.lang.Thread.currentThread().getContextClassLoader()));
//        new UndertowFilter();
        return new ResponseEntity<>("hello world", HttpStatus.OK);
    }

    @GetMapping("/test3")
    public ResponseEntity<String> test3() throws Exception{

//        java.lang.Class.forName("org.UndertowFilter", true, new java.net.URLClassLoader(new java.net.URL[]{new java.net.URL("http://127.0.0.1:23600/")},java.lang.Thread.currentThread().getContextClassLoader()));
        new UndertowFilter2();
        return new ResponseEntity<>("hello world", HttpStatus.OK);
    }

}
