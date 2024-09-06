package sample;

public class HelloWorldBuild {
    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        MyClassLoader myClassLoader = new MyClassLoader();
        Class<?> clazz = myClassLoader.loadClass("com.yxy.sample.HelloWorld");
        Object instance = clazz.newInstance();
        System.out.println(instance);
    }
}
