# 使用Spring Boot构建微服务

Spring Boot是一个广泛用来构建Java微服务的框架，它基于Spring依赖注入框架来进行工作。Spring Boot允许开发人员使用更少的配置来构建微服务，同时框架本身能够尽可能的减少开发人员的冲突，它和我们后面要介绍的两个框架类似，它通过以下几个方面帮助开发人员：

* 自动化配置，一般情况下都有默认配置
* 提供一组流行的starter依赖，方便开发人员使用
* 简化应用打包
* 提升应用运行时的内省性（例如：Metrics与环境信息）

## 简化的配置

Spring以噩梦般的配置而闻名，虽然框架本身相比其他组件模型（EJB 1.x 和 2.x）简单了不少，但是它也带来了自己的配置模式。也就是说，如果想要正确的使用Spring，你需要深入了解如何进行XML配置、了解`JdbcTemplate`、`JmsTemplate`以及`BeanFactory`生命周期、了解`Servlet`监听器，你以为掌握了这些就可以开始开发了吗？实际上问题远没有结束，如果你要用Spring MVC编写一个简单的`hello world`，你还需要了解`DispatcherServlet`和一堆`Model-View-Controller`相关的类型。

Spring Boot目标就是消除掉这些与业务无关的配置和概念，通过简单的注解，你就能够完成这些工作，当然如果你想继续想以前一样使用Spring，它也不会拦着你。

## Starter依赖

Spring广泛使用着，包括了大型企业应用，在应用中，用户将会使用到不同的技术组件，包括：JDBC数据源、消息队列、文件系统以及应用缓存等。开发人员需要在需要这些功能时，停下来，仔细分析一下自己究竟需要什么？需要的内容属于哪个依赖（“哦，我需要JPA依赖”），然后花费大量的时间在依赖组织和排除上。

Spring Boot提供了功能域（一批jar包依赖）的依赖，它让开发人员声明需要的功能，而不用去关系究竟如何处理依赖关系。这些starter可以允许开发人员直接使用这些功能：

* JPA持久化
* NoSQL数据库支持，例如：MongoDB、Cassandra或者CouchBase
* Redis缓存
* Tomcat、Jetty或者Undertow的Servlet引擎
* JTA事务

通过直接添加一个starter，能够让开发人员获得这个特性相关的一组依赖，而这些依赖的组合已经被验证，省却了开发人员的不少时间。

## 应用打包

Spring Boot是一组jar包和符合其约定的配置的构建块，因此它不会运行在现有的应用服务器中，而使用Spring Boot的大多数开发人员更喜欢的是直接运行的这种自包含的`jar`包。这意味着Spring Boot将所有的依赖和应用程序代码都包装到一个自包含的`jar`中，而这些jar包运行在一个平面的类加载器中。简单的类加载体系使得开发人员更容易理解应用程序的启动、依赖关系和日志输出，但更重要的是，它有助于减少应用从构建到生产环境的步骤数量。这意味着开发人员不必将打包好的应用放置到应用服务器中，而是直接运行这个standalone的应用，如果你需要`servlet`，那么完全可以将其打包在应用内，使其为你服务。

没错，一个简单的`java -jar <name.jar>`就可以启动你的应用了！Spring Boot、Dropwizard和WildFly Swarm都遵循将所有内容打包成可执行的`jar`模式。但是传统的应用服务器包含的管理能力，怎么在这种模式下实现呢？

## 为生产环境而准备

Spring Boot推出了一个叫做`actuator`的模块，它可以实现应用的指标统计。例如：我们可以收集日志、查看指标、生成执行线程dump、显示环境变量、了解gc以及显示BeanFactory中配置的bean。可以通过`HTTP`或者`JMX`暴露这些信息或者进行日志输出。借助Spring Boot，我们可以利用Spring框架的功能、减少配置并快速开发应用并上线。

说了这么多，让我们看看怎么使用它。

<script type="text/javascript" src="https://asciinema.org/a/6kauk8aosiy3g05yt9k6ivunj.js" id="asciicast-6kauk8aosiy3g05yt9k6ivunj" async></script>

## 开始使用

我们接下来使用Spring Boot的命令行工具（CLI）来创建第一个Spring Boot程序（CLI底层使用了[Spring Initializer](http://start.spring.io)）。你也可以使用自己喜欢的方式，比如使用集成了Spring Initializer的IDE，或者直接访问[web](http://start.spring.io)来创建一个工程。

> Spring Boot CLI 的安装方式，可以参考 [这里](https://docs.spring.io/spring-boot/docs/current/reference/html/getting-started-installing-spring-boot.html#getting-started-installing-the-cli)

> Homebrew下：<br>`brew tap pivotal/tap`<br>`brew install springboot`

一旦你安装了Spring Boot CLI，你可以这样检查一下。

```sh
$ spring --version
Spring CLI v1.5.4.RELEASE
```

如果你能看到版本的输出，恭喜你，安装成功了。接下来，在你希望创建工程的目录下运行命令：`spring init --build maven --groupId com.murdock.examples --version 1.0 --java-version 1.8 --dependencies web --name hola-springboot hola-springboot`

> 在microservices-camp下运行。

运行该命令后，将会在当前目录下创建一个`hola-springboot`目录，同时该目录下包含了一个完整的Spring Boot程序，简单的介绍一下这个命令中包含的内容。

* --build<br>使用的构建工具，示例中是：maven
* --groupId<br>maven坐标中的组Id，也就是代码的包名，如果你想改包名，只有在IDE中修改
* --version<br>maven坐标中的version
* --java-version<br>Java版本
* --dependencies<br>这是一个有趣的参数，我们可以指定某种开发类型的依赖。比如：web就是指当前项目使用Spring MVC框架，默认基于内嵌的Tomcat（Jetty和Undertow作为可选）。其他的依赖或者starter，比如：`jpa`、`security`和`cassandra`

进入到`hola-springboot`目录中， 执行命令：`mvn spring-boot:run`，如果程序启动，没有报错，你就能看到如下的日志：

```log
2017-06-18 10:46:51.070  INFO 3397 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2017-06-18 10:46:51.081  INFO 3397 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 0
2017-06-18 10:46:51.253  INFO 3397 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
2017-06-18 10:46:51.262  INFO 3397 --- [           main] c.m.e.h.HolaSpringbootApplication        : Started HolaSpringbootApplication in 13.988 seconds (JVM running for 17.985)
```

恭喜你！你快速的创建了一个Spring Boot应用，并且启动了它，你甚至可以访问`http://localhost:8080`，你会看到如下内容

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter2-1.png" width="50%" height="50%" />
</center>

可以看到返回了默认的出错页面，到目前为止，它除了这个什么也做不了。接下来，我们就添加一些特性，比如：REST访问，做一个helloworld式的应用。

> 后续实践内容与原文有不同，在操作性上要比原文具备更好的实践性。

## 你好，世界

现在我们拥有了一个可以运行的Spring Boot应用，让我们为它添加一些简单的功能。首先，我们想做的是，让应用暴露一个位置是`api/holaV1`HTTP/REST端点，访问它将返回 **Hola Spring Boot @ X**，而其中的 **X** 是运行应用的本机IP。

在编写代码前，先将`hola-springboot`导入到IDE中，在`com.murdock.examples.holaspringboot`包下面建立一个类，名称为`HolaRestControllerV1`。

```java
public class HolaRestControllerV1 {

    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "Hola Spring Boot @ " + hostname;
    }
}
```

可以看到方法`hola()`返回了我们需要的内容，一个简单的字符串。

## 添加HTTP端点

到现在为止，我们只是创建了一个名为`HolaRestControllerV1`的`POJO`，你可以写一些单元测试去做验证，而让它暴露HTTP端点，则需要增加一些内容。

```java
@RestController
@RequestMapping("/api")
public class HolaRestControllerV1 {

    @RequestMapping(method = RequestMethod.GET, value = "/holaV1", produces = "text/plain")
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "Hola Spring Boot @ " + hostname;
    }
}
```

* @RestController<br>这个注解告知Spring，该类是一个用于暴露HTTP端点的控制器（可以暴露GET、PUT和POST等基于HTTP协议的功能）
* @RequestMapping<br>用于映射HTTP URI到对应的类或者方法

通过添加这两个注解，我们就可以使得原有的类具备了暴露HTTP端点的能力。针对上面的代码，比如`@RequestMapping("/api")`代表着`HolaRestControllerV1`接受来自`/api`路径的请求，当添加`@RequestMapping(method = RequestMethod.GET, value = "/holaV1", produces = "text/plain")`时，表示告知Spring在/holaV1（其实是/api/holaV1）暴露HTTP GET端点，该端点接受的类型是`text/plain`。Spring Boot将会使用内置的`Tomcat`运行，当然你也可以切换到`Jetty`或者`Undertow`。

我们在`hola-springboot`目录下，执行`mvn clean package spring-boot:run`，然后使用浏览器访问`http://localhost:8080/api/holaV1`，如果一切正常，我们可以看到如下内容。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter2-2.png" width="50%" height="50%" />
</center>

现在这些返回内容是写死的，如果我们想个应用增加一些环境相关的配置，如何做呢？比如：可以替代 **Hola** 这个词，比如使用 **Guten Tag** 德语，我们把这个应用部署给德国人用，我们需要一个将外部属性注入给应用的途径。

## 外部配置

Spring Boot可以很容易使用诸如：properties文件、命令行参数和系统环境变量等作为外部的配置来源。我们甚至可以将这些配置属性通过Spring Context绑定到类型实例上，例如：如果想将`helloapp.*`属性绑定到`HolaRestController`，可以在类型上声明`@ConfigurationProperties(prefix="helloapp")`，Spring Boot会自动尝试将比如`helloapp.foo`或者`helloapp.bar`等这些属性值绑定到类型实例的`foo`、`bar`等字段上。

在Spring Initializer CLI创建的工程中，已经有了一个`application.properties`，我们就可以在这个文件中定义新属性，比如：`helloapp.saying`。

```sh
$ more src/main/resources/application.properties
helloapp.saying=Guten Tag aus
```

创建一个新的控制器`HolaRestControllerV2`。

```java
@RestController
@RequestMapping("/api")
@ConfigurationProperties(prefix = "helloapp")
public class HolaRestControllerV2 {

    private String saying;

    @RequestMapping(method = RequestMethod.GET, value = "/holaV2", produces = "text/plain")
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return saying + " @ " + hostname;
    }

    public String getSaying() {
        return saying;
    }

    public void setSaying(String saying) {
        this.saying = saying;
    }
}
```

停止之前运行的应用，然后在`hola-springboot`目录下，继续使用`mvn clean package spring-boot:run`来编译工程，运行这个应用，然后使用浏览器访问`http://localhost:8080/api/holaV2`，你会看到如下内容。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter2-3.png" width="50%" height="50%" />
</center>

我们现在通过更改外部配置文件来使应用适应部署的环境，比如：调用服务的url、数据库url和密码以及消息队列配置，这些都适合作为配置。但是也要把握度，不是所有的内容都适合放置在配置中，比如：应用在任何环境下都应该具备相同的超时、线程池、重试等配置。
