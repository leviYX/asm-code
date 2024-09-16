# 简介

[java虚拟机规范][docs.oracle.com/javase/specs/index.html]

本文主要是针对java字节码进行学习的阐述。其中主体为jdk8的内容，而java8中的字节码规范文档位置位于第四章节。

[][]

[java8字节码规范文档][https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html]

我们的工具都是使用的idea的插件，Binary/hex editor和jclasslib。分别查看二进制内容和字节码内容。

# 一、字节码简述

实际上java源码通过javac的前端编译器的编译之后，就会生成字节码。而这个字节码不仅仅是java的专属，实际上你只要按照这个字节码规范实现了你的字节码生成结果，就可以运行在jvm上，所以jvm虚拟机上可以跑很多语言，比如java，scala等等。

而字节码的格式也很简单，**就是一个字节长的操作码**+多个操作参数，其中操作参数有时候可以没有。这个我们后面会见到很多这类。

而字节码文件没有任何分隔符，不像什么xml有标记符号，他就是根据空格来划分的。所以就要严格按照字节码规范顺序来生成，他按照这个顺序来解析。这种没有分隔符可以极限的节省空间。

其次我们的代码编译之后的class文件其实就是字节码文件，jvm解析的字节码文件可以是我们编译之后的磁盘上的class文件，也可以是网络上的文件流。只要你是字节码格式的就没问题，他的解析来源是没硬性要求的。

# 二、class文件规范

# 1、字节码格式

我们在java字节码规范文档第四章可以看到，一个class文件的字节码内容有如下内容：

其中可以看到一些u2 u4 之类的描述，他们表示的是占用多少个字节长度。比如第一项u4             magic;表示的就是我们编译出来的字节码文件第一部分是一个占四字节长度的内容，也就是magic，就是魔数。其他的也是一样的含义。

而还有一些不是这种的，比如cp_info，field_info ，method_info，attribute_info在字节码规范中表示一个数组，这个数组里面有很多元素，每个元素其实也还是一个一个的u2 u4 u8这种。他是个组合结构体，也有人叫表。

之所以存在表这个结构是因为有些部分是不确定的，比如cp_info，这个结构表示常量池的内容，而一个类里面的常量是不固定的，所以他不能固定用某一个u2 u8这种表示，他是个数组。但是数组里面有多个，那怎么知道到哪个结束呢，毕竟也没有个开始符号之类的。所以每一个这种表的前一项就是他的长度，比如cp_info的前面是一个u2两字节长度的constant_pool_count，表示的是这个cp_info里面有多少项内容。类似tcp那种头体分离的机制。

~~~markdown
ClassFile {
    u4             magic;
    u2             minor_version;
    u2             major_version;
    u2             constant_pool_count;
    cp_info        constant_pool[constant_pool_count-1];
    u2             access_flags;
    u2             this_class;
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[interfaces_count];
    u2             fields_count;
    field_info     fields[fields_count];
    u2             methods_count;
    method_info    methods[methods_count];
    u2             attributes_count;
    attribute_info attributes[attributes_count];
}
~~~

具体解释如下表展示：

| 类型           | 名称                | 说明                                                         | 长度  | 数量                  |
| -------------- | ------------------- | ------------------------------------------------------------ | ----- | --------------------- |
| u4             | magic               | 魔数，用来标识这个文件是一个字节码文件，如果不是这个内容就不是 | 4字节 | 1                     |
| u2             | minor_version       | 小版本号                                                     | 2字节 | 1                     |
| u2             | major_version       | 大版本号                                                     | 2字节 | 1                     |
| u2             | constant_pool_count | 常量池计数器，表示常量池里面有多少常量                       | 2字节 | 1                     |
| cp_info        | constant_pool       | 常量池表                                                     | n字节 | constant_pool_count-1 |
| u2             | access_flags        | 访问标识，比如你是一个类还是接口，有没有final或者抽象类修饰，public还是啥。都在这里存储。 | 2字节 | 1                     |
| u2             | this_class          | 就表示当前这个类的名字                                       | 2字节 | 1                     |
| u2             | super_class         | 当前类的父类名字                                             | 2字节 | 1                     |
| u2             | interfaces_count    | 当前类实现的接口个数                                         | 2字节 | 1                     |
| u2             | interfaces          | 当前类实现了哪些接口，通过符号存储的，所以2字节能存很多很多，就是接口的索引集合 | 2字节 | interfaces_count      |
| u2             | fields_count        | 当前类的字段个数                                             | 2字节 | 1                     |
| field_info     | fields              | 当前类的字段内容表                                           | n字节 | fields_count          |
| u2             | methods_count       | 当前类方法的个数，最少也有个构造的init                       | 2字节 | 1                     |
| method_info    | methods             | 当前类方法信息表                                             | n字节 | methods_count         |
| u2             | attributes_count    | 当前类的一些属性，属性个数                                   | 2字节 | 1                     |
| attribute_info | attributes          | 当前类的属性信息表                                           | n字节 | attributes_count      |

# 2、准备材料

下面我们就开始阅读一下编译出来的字节码看是不是和这个规范一致。

我们先用最简单的一段代码开始。

> 源码

~~~java
/**
 * 字节码学习
 */
public class Demo {
    private int num;

    public int add() {
        num = num + 2;
        return num;
    }
}
~~~

> 编译出来的二进制字节码

我们用Binary/hex editor插件打开class文件如下：显示为十六进制的表达，其中每一个位置表示1个字节。比如CA FE其实就是4字节。

十六进制的A代表十进制的10，然后往下推。

![image-20240916213428220](bytecode.assets/image-20240916213428220.png)

> 二进制字节码编译可视化结果

我们用jclasslib插件看一下这个二进制对应的class结构，然后和我们这个二进制的做对比。看看是不是能对上。查看方法很简单，鼠标选中那个类，然后idea中在view选中jclasslib打开就好。结构如下。

![image-20240916213829026](bytecode.assets/image-20240916213829026.png)

具体里面有很多表项，我们后面挨个打开看看。

