# 使用Dropwizard构建微服务

Dropwizard的历史要早于Spring Boot和WildFly Swarm，它最早是在`2011.12`发布的`v0.1.0`版本，在本文编写的过程中，它已经发布了`v0.9.2`版本，而`v1.0.0`版本也在准备中了。Dropwizard是`Coda Hale`在`Yammer`公司时创立的，它旨在提升公司分布式系统的架构（现在叫：微服务）。虽然它最早被用来构建REST Web 服务，而现在它具备了越来越多的功能，但是它的目标始终是作为轻量化、为生产环境准备且容易使用的web框架。

> 目前Dropwizard已经发布了`v1.1.0`版本

Dropwizard与Spring Boot类似，也是构建微服务可选的工具，但是它显得比Spring Boot更加规范一些。它使用的组件一般不会做可选替换，而好处是可以不需要那么多的修饰，比如写基于REST的web服务。比方说，Dropwizard选择使用`Jetty`作为Servlet容器，REST库使用`Jersey`，序列化和反序列化使用了`Jackson`，而想将其中的`Jetty`替换成`Undertow`就没有那么容易。

Dropwizard默认也不具备依赖注入的容器（像Spring或者CDI），你当然可以自行添加，但是Dropwizard推荐你把微服务弄的简单一些，不需要这些额外的组件。Spring Boot 隐藏的非常多的底层实现，而这些内容十分的复杂，就像Spring隐藏了通过注解可以完成Bean注入这个复杂的场景一样。虽然注解很好用，也解决了某些领域比较琐碎的代码，但是当你想在生产环境DEBUG或者排查问题时，这些东西往往会把简单的问题搞得很复杂。Dropwizard推荐所有的内容都显示的使用，你得到的输出也就更加的肯定和明确。

> 原文涉及到生产环境的DEBUG，笔者认为更多的是问题排查

就像Spring Boot一样，Dropwizard推荐将整个工程打包成一个可执行的jar，通过这种方式开发人员不用在担心程序运行的应用服务器是什么，需要什么额外的配置，应用再也不需要被构建成war包了，而且也不会有那么多复杂层级的类加载器了。Dropwizard中的类加载也是扁平结构的，它和我们常用的应用服务器不一样，应用服务器往往具备多层级如同图一般的类加载器，这会涉及到类加载器的优先级，而这些在不同的应用服务器的实现上都是大相径庭的。运行在独立进程中的Dropwizard实例也便于进行各自的JVM调优和监控，因为运行在应用服务器上的多个应用，很可能由于一个应用导致的GC或者内存溢出，进而导致整个应用服务器的崩溃，毕竟它们是在同一个进程中。

## Dropwizard技术栈

Dropwizard在优秀的三方库协助下，提供了不错的抽象层，使之更有效率，更简单的编写生产用途的微服务。

* Servlet容器使用`Jetty`
* REST/JAX-RS实现使用`Jersey`
* JSON序列化使用`Jackson`
* 集成`Hibernate Validator`
* Guava
* Metrics
* SLF4J + Logback
* 数据访问层上使用`JDBI`

Dropwizard偏执的认为框架就是用来写代码的，因此对于框架的底层技术栈的调整，原则上Dropwizard是拒绝的。正因为它这么做，使得Dropwizard开发起代码来更快，而且配置更加容易。`Jetty`、`Jersey`和`Jackson`都是广为人知的项目，使用它们来构造用于生产环境的web服务，看起来没什么毛病，而google的`Guava`作为提供了工具类的包显然经得住考验，而Dropwizard Metrics更是一个强大工具，它能够暴露出应用相当多的运行细节，而正因为此，Dropwizard Metrics被广泛的使用于Spring Boot和WildFly Swarm。

Dropwizard暴露了如下抽象，如果你能掌握这些简单的抽象，你就能很快的使用Dropwizard进行开发了。

* Application<br>包含了`public void main()`方法
* Environment<br>放置`servlet`、`resources`、`filters`、`health checks`、`task`的地方
* Configuration<br>用于改变环境或者系统配置的地方
* Commands<br>当我们启动微服务后，使用它来与微服务交互
* Resources<br>REST/JAX-RS资源
* Tasks<br>对于应用的管理，比如改变日志级别或者暂停数据库连接等

当你启动一个Dropwizard应用，一个`Jetty`服务就会启动，同时它会创建两个Handler：一个在8080，为你的应用提供服务，另一个在8081，这个提供管理功能。Dropwizard之所以这么做，是因为不想将管理功能通过8080进行暴露，这样你可以把端口隐藏在防火墙后面。诸如Metrics和健康检查，这些也是暴露在管理端口上的，区分的很大原因是考虑安全问题。

## 开始

Dropwizard没有那些美轮美奂用于创建工程的工具，它只有一个最简单的方式：maven archetype，或者在已经搭建好的项目中，增加一些maven配置。当然你可以使用`jboss-forge`来完成工程的创建，用它来添加对应的依赖等等，但是本章，我们还是使用maven archetype。

选择一个目录，然后输入一段maven命令，可以完成项目的创建。

```sh
$ mvn -B archetype:generate -DarchetypeGroupId=io.dropwizard.archetypes -DarchetypeArtifactId=java-simple -DarchetypeVersion=1.1.0 -DgroupId=com.murdock.examples.dropwizard -DartifactId=hola-dropwizard -Dversion=1.0 -Dname=HolaDropwizard
```

> 本示例演示，在`microservices-camp`目录下运行

该命令会在`microservices-camp`目录下创建一个名为`hola-dropwizard`的工程，你可以选择将其导入到自己的IDE中，或者你可以在`hola-dropwizard`目录下运行`mvn clean install`完成构建。

导入`hola-dropwizard`工程后，你可以看到如下结构：

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-1.png" width="50%" height="50%" />
</center>

Dropwizard已经创建了你需要开发放置的包，它推荐你按照约定进行开发。

* api<br>放置REST资源需要使用的POJOs，你可以理解为domain objects或者DTOs
* cli<br>放置你需要添加给应用的Dropwizard命令
* client<br>客户端工具类放在这里
* db<br>和数据库相关的代码
* health<br>运行时刻暴露在管理端口的微服务健康检查逻辑
* resources<br>REST资源

同样我们还可以看到一家创建好的类型`HolaDropwizardApplication`和`HolaDropwizardConfiguration`，它们使用来启动和配置应用的。先看一下`HolaDropwizardApplication`长得样子。

```java
public class HolaDropwizardApplication extends Application<HolaDropwizardConfiguration> {

    public static void main(final String[] args) throws Exception {
        new HolaDropwizardApplication().run(args);
    }

    @Override
    public String getName() {
        return "HolaDropwizard";
    }

    @Override
    public void initialize(final Bootstrap<HolaDropwizardConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final HolaDropwizardConfiguration configuration,
                    final Environment environment) {
        // TODO: implement application
    }

}
```

这个类包含了一个`public static void main()`方法，可以想象，它是入口，而`getName()`方法将会在应用启动时展示。`initialize()`和`run()`方法是用来启动应用的地方。

配置类型已经创建了，但是目前是空的。

```java
public class HolaDropwizardConfiguration extends Configuration {
    // TODO: implement service configuration
}
```

虽然Dropwizard没有定义自己的maven plugin，但是它为我们生成了`pom.xml`。打开`pom.xml`，可以看到Dropwizard使用`maven-shade-plugin`将依赖打包成一个jar，这意味着工程依赖的jar包和代码将全部解压，然后重新组合成一个jar。而针对这个构建好的jar，我们可以使用`maven-jar-plugin`运行它。

我们唯一需要的一个插件就是`exec-maven-plugin`，这样我们就可以像使用`mvn spring-boot:run`一样运行它。

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <configuration>
        <mainClass>
            com.murdock.examples.dropwizard.HolaDropwizardApplication
        </mainClass>
        <arguments>
            <argument>server</argument>
        </arguments>
    </configuration>
</plugin>
```

接下来，可以在`hola-dropwizard`使用`mvn exec:java`来启动它。看到如下内容，代表启动成功了。

```log
================================================================================

                              HolaDropwizard

================================================================================


INFO  [2017-06-19 07:18:59,624] org.eclipse.jetty.setuid.SetUIDListener: Opened application@8032e08{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
INFO  [2017-06-19 07:18:59,624] org.eclipse.jetty.setuid.SetUIDListener: Opened admin@23339641{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
INFO  [2017-06-19 07:18:59,626] org.eclipse.jetty.server.Server: jetty-9.4.2.v20170220
INFO  [2017-06-19 07:19:00,122] io.dropwizard.jersey.DropwizardResourceConfig: The following paths were found for the configured resources:

    NONE

INFO  [2017-06-19 07:19:00,124] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@5a8a334a{/,null,AVAILABLE}
INFO  [2017-06-19 07:19:00,131] io.dropwizard.setup.AdminEnvironment: tasks =

    POST    /tasks/log-level (io.dropwizard.servlets.tasks.LogConfigurationTask)
    POST    /tasks/gc (io.dropwizard.servlets.tasks.GarbageCollectionTask)

WARN  [2017-06-19 07:19:00,131] io.dropwizard.setup.AdminEnvironment:
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!    THIS APPLICATION HAS NO HEALTHCHECKS. THIS MEANS YOU WILL NEVER KNOW      !
!     IF IT DIES IN PRODUCTION, WHICH MEANS YOU WILL NEVER KNOW IF YOU'RE      !
!    LETTING YOUR USERS DOWN. YOU SHOULD ADD A HEALTHCHECK FOR EACH OF YOUR    !
!         APPLICATION'S DEPENDENCIES WHICH FULLY (BUT LIGHTLY) TESTS IT.       !
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
INFO  [2017-06-19 07:19:00,137] org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@6555f9b5{/,null,AVAILABLE}
INFO  [2017-06-19 07:19:00,155] org.eclipse.jetty.server.AbstractConnector: Started application@8032e08{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
INFO  [2017-06-19 07:19:00,156] org.eclipse.jetty.server.AbstractConnector: Started admin@23339641{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
INFO  [2017-06-19 07:19:00,156] org.eclipse.jetty.server.Server: Started @11245ms
```

你可以打开浏览器，访问默认的REST端点：`http://localhost:8080`，这会不会返回很多内容，你可能看到：

```json
{
code: 404,
message: "HTTP 404 Not Found"
}
```

如果你访问管理端口：`http://localhost:8081`，你会看到一个简单页面以及链接，这里面是当前应用的运行时信息。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-1.png" width="50%" height="50%" />
</center>
