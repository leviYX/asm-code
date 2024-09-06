package sample;

public class MyClassLoader extends ClassLoader {
    @Override
    public Class<?> findClass(String className) {
        if("com.yxy.sample.HelloWorld".equals(className)){
            byte[] classByte = HelloWorldDump.dump();
            Class<?> defineClass = defineClass(className, classByte, 0, classByte.length);
            return defineClass;
        }else {
            throw new RuntimeException(className + ":class not found");
        }
    }
}
