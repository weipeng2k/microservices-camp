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

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-2.png" width="70%" height="70%" />
</center>


你可以打开浏览器，访问默认的REST端点：`http://localhost:8080`，这会不会返回很多内容，你可能看到：

```json
{
code: 404,
message: "HTTP 404 Not Found"
}
```

如果你访问管理端口：`http://localhost:8081`，你会看到一个简单页面以及链接，这里面是当前应用的运行时信息。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-3.png" width="50%" height="50%" />
</center>

## 你好，世界

现在使用Dropwizard构建的工程已经准备好了，让我们添加一个REST端点。我们会在`/api/holaV1`暴露一个HTTP/REST端点，访问它，将会返回 **Hola Dropwizard @ X**，而 **X** 指的是运行应用的机器IP。如果想做到这个，首先需要在`resources`包下面新建类型，比如：`HolaRestResourceV1`（记住，类型放置的包，必须符合Dropwizard的约定）。添加一个方法`hola()`，然后在其中返回所需的内容。

```java
public class HolaRestResourceV1 {

    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "Hola Dropwizard @ " + hostname;
    }
}
```

可以针对这个`hola()`方法做测试。

## 添加HTTP端点

这看起来很像Spring Boot，我们想创建REST端点以及服务，也是在POJO上增加一些JAX-RS的注解。

```java
@Path("/api")
public class HolaRestResourceV1 {

    @Path("/holaV1")
    @GET
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return "Hola Dropwizard @ " + hostname;
    }
}
```

现在，打开之间生成的`HolaDropwizardApplication`，在`run()`方法中添加新创建的`HolaRestResourceV1`（REST资源）。

```java
@Override
public void run(final HolaDropwizardConfiguration configuration,
                final Environment environment) {
    environment.jersey().register(new HolaRestResourceV1());
}
```

接着就可以在`hola-dropwizard`目录下执行`mvn clean package exec:java`，随着应用的启动，我们打开浏览器访问`http://localhost:8080/api/holaV1`，可以看到如下内容。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-4.png" width="70%" height="70%" />
</center>

## 外部配置

Dropwizard提供了针对内置组件的诸多配置（比如：servlet引擎或者数据源）方式，你可以使用配置文件来完成配置。可以使用系统环境变量或者Java System properties来进行配置，这样可以使应用运行在不同的环境上。如同之前介绍的Spring Boot，Dropwizard也可以将配置绑定到指定的实例上。在接下来的例子中，我们就将`helloapp.*`下面的配置，绑定到`HolaRestResourceV2`上。不像Spring Boot通过`application.properties`完成配置，Dropwizard只支持`YAML`。

在工程的根目录（`hola-dropwizard`）下（注意：不是类路径下），创建一个`conf/application.yml`（如果conf目录不存在，你需要创建它），我们将配置文件放置在该目录中，先给`conf/application.yml`添加一些内容：

```yml
helloapp:
  saying: Hola Dropwizard @
```

这样我们为属性指定了值，如果我们需要为某些环境更改这个值，该如何做呢？可以通过Java System properties来做到，可以通过定义`-Ddw.helloapp.saying=Guten Tag`。注意`dw.*`代表着Dropwizard可以覆盖该配置的值，但是如果需要使用操作系统变量来进行覆盖呢，如何做到？

```yml
helloapp:
  saying: ${HELLOAPP_SAYING:-Guten Tag aus}
```

可以看到对`saying`的配置首先回去查看环境变量`HELLOAPP_SAYING`，如果该环境变量不存在，那么就会使用默认的`Guten Tag aus`，默认Dropwizard不会从环境变量中获取配置，如果需要让Dropwizard使用环境变量，需要做一些额外改动。打开`HolaDropwizardApplication`，编辑`initialize()`方法。

```java
@Override
public void initialize(final Bootstrap<HolaDropwizardConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                    new EnvironmentVariableSubstitutor(false)));
}
```

接下来创建配置，我们定义了一个专门的配置类型，这个配置类型用于接受来自`helloapp`下的配置，接下来看如何将配置类型和`application.yml`绑定。在`resources`包下，创建一个类型。

```java
public class HelloSayingFactory {
    @NotEmpty
    private String saying;

    @JsonProperty
    public String getSaying() {
        return saying;
    }

    @JsonProperty
    public void setSaying(String saying) {
        this.saying = saying;
    }

}
```

这个简单的Java Bean上增加了一些注解，比如：`Jackson`和`Bean Validator`，这个配置对象将会包装在`application.yml`中，处于`helloapp`之下的配置。这一刻，只有一个配置属性`saying`，下面需要将`HolaDropwizardConfiguration`与它关联起来。

```java
public class HolaDropwizardConfiguration extends Configuration {

    private HelloSayingFactory sayingFactory;

    @JsonProperty("helloapp")
    public HelloSayingFactory getSayingFactory() {
        return sayingFactory;
    }

    @JsonProperty("helloapp")
    public void setSayingFactory(
            HelloSayingFactory sayingFactory) {
        this.sayingFactory = sayingFactory;
    }
}
```

> 笔者注：HolaDropwizardConfiguration代表着指定的`application.yml`，而`helloapp`是配置中的一个节点，而该节点以下的结构，将会设置到类型`HelloSayingFactory`实例中

接下来需要将配置引入到REST资源中，在`resources`包下，创建类型`HolaRestResourceV2`。

```java
@Path("/api")
public class HolaRestResourceV2 {

    private String saying;

    public HolaRestResourceV2(String saying) {
        this.saying = saying;
    }

    @Path("/holaV2")
    @GET
    public String hola() throws UnknownHostException {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost()
                    .getHostAddress();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        return saying + " " + hostname;
    }
}
```

由于Dropwizard没有依赖注入框架的帮助，所以你需要依靠自己将配置注入到REST资源中。编辑`HolaDropwizardApplication`：

```java
@Override
public void run(final HolaDropwizardConfiguration configuration,
                final Environment environment) {
    environment.jersey().register(new HolaRestResourceV1());
    environment.jersey().register(new HolaRestResourceV2(configuration.getSayingFactory().getSaying()));
}
```

到目前为止，一个具备配置注入的项目已经基本搭建完成，这个例子中对于配置，我们故意设计的复杂一些，目的是还原一个真实场景。虽然开起来零碎的步骤不少，但是可以看到最关键的几个步骤：定义`application.yml`，并且将`HolaDropwizardConfiguration`与之绑定，而后续配置的添加就变得很简单了。

如果想在maven中运行，还需要将配置文件的位置传递给Dropwizard，所以还需要编辑一下`pom.xml`

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
            <argument>conf/application.yml</argument>
        </arguments>
    </configuration>
</plugin>
```

在项目目录`hola-dropwizard`下，执行`mvn clean package exec:java`，然后打开浏览器访问`http://localhost:8080/api/holaV2`，可以看到：

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-5.png" width="70%" height="70%" />
</center>

接下来停止应用，然后导出一个环境变量，再启动它，随后访问原来的页面：

```sh
$ export HELLOAPP_SAYING="Hello Dropwizard @ "
$ echo $HELLOAPP_SAYING
Hello Dropwizard @
$ mvn clean package exec:java
```

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-6.png" width="70%" height="70%" />
</center>

## 暴露应用Metrics和信息

Dropwizard中做的最好的是将Metrics作为了一等公民，Dropwizard从一开始就考虑了Metrics，而非像其他框架一样事后考虑，所以当一个Dropwizard应用启动的同时，`8081`管理端口就暴露了Metrics信息。我们只需要在对应的REST资源上增加一些注解就可以做到。

在`HolaRestResourceV2`中的`hola()`方法，增加注解`@Timed`，它将跟踪该服务的调用耗时与次数等信息，当然还有其他的Metrics组件可供选择。

* @Metered<br>服务调用频率
* @ExceptionMetered<br>异常抛出频率

> 不能都添加上，只能选择一种

重新启动Dropwizard应用，然后访问几次`http://localhost:8080/api/holaV2`，然后用浏览器打开`http://localhost:8081/metrics?pretty=true`，然后搜索`hola`，你可以看到类似如下内容：

```json
com.murdock.examples.dropwizard.resources.HolaRestResourceV2.hola:{
  count: 3,
  max: 5.0143579240000005,
  mean: 5.006734606111542,
  min: 5.002627207000001,
  p50: 5.004089423,
  p75: 5.0143579240000005,
  p95: 5.0143579240000005,
  p98: 5.0143579240000005,
  p99: 5.0143579240000005,
  p999: 5.0143579240000005,
  stddev: 0.0051299569217563845,
  m15_rate: 0.003305709235676515,
  m1_rate: 0.04423984338571901,
  m5_rate: 0.009754115099857198,
  mean_rate: 0.11808407104043579,
  duration_units: "seconds",
  rate_units: "calls/second"
}
```

## 如果在maven之外运行

Dropwizard通过`maven-shade-plugin`打包成了一个jar，所以只需要通过`java -jar`就可以运行，我们唯一需要知道的就是传递配置给Dropwizard，比如这样：`java -jar target/hola-dropwizard-1.0.jar server conf/application.yml`。

## 调用其他服务

在微服务环境下，服务之间需要相互调用，和之前的Spring Boot应用一样，Dropwizard提供了自己的REST客户端供我们使用。类似之前在Spring Boot的章节，我们使用Dropwizard完成这项工作。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-7.png" width="50%" height="50%" />
</center>

在开始之前，先在`hola-dropwizard`项目中添加依赖。

```xml
<dependency>
  <groupId>io.dropwizard</groupId>
  <artifactId>dropwizard-client</artifactId>
</dependency>
```

首先创建`GreeterSayingFactory`配置，在这个配置中将描述调用`hola-backend`的具体URL和端口等信息。

```java
public class GreeterSayingFactory {
    @NotEmpty
    private String saying;
    @NotEmpty
    private String host;
    @NotEmpty
    private int port;
    private JerseyClientConfiguration jerseyClientConfig =
            new JerseyClientConfiguration();

    @JsonProperty("jerseyClient")
    public JerseyClientConfiguration getJerseyClientConfig() {
        return jerseyClientConfig;
    }

    public String getSaying() {
        return saying;
    }

    public void setSaying(String saying) {
        this.saying = saying;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;

    }

    public void setPort(int port) {
        this.port = port;
    }
}
```

然后需要将`application.yml`中的配置设置到`GreeterSayingFactory`中，所以需要将`greeter`下面的配置完成设置，这时，需要继续编辑`HolaDropwizardConfiguration`，增加以下两个方法。

```java
@JsonProperty("greeter")
public GreeterSayingFactory getGreeterSayingFactory() {
    return greeterSayingFactory;
}

@JsonProperty("greeter")
public void setGreeterSayingFactory(
        GreeterSayingFactory greeterSayingFactory) {
    this.greeterSayingFactory = greeterSayingFactory;
}
```

对于`GreeterSayingFactory`中属性的具体配置，需要在`conf/application.yml`中编辑，增加以下内容：

```yml
greeter:
  saying: ${GREETER_SAYING:-Guten Tag Dropwizard}
  host: ${GREETER_BACKEND_HOST:-localhost}
  port: ${GREETER_BACKEND_PORT:-8080}
```

通过这样就可以使用系统环境变量对应用做不同的配置了，到此配置基本结束，我们编写一个REST端点用于提供HTTP服务。

```java
@Path("/api")
public class GreeterRestResource {
    private String saying;
    private String backendServiceHost;
    private int backendServicePort;
    private Client client;

    public GreeterRestResource(final String saying, String host, int port, Client client) {
        this.saying = saying;
        this.backendServiceHost = host;
        this.backendServicePort = port;
        this.client = client;
    }

    @Path("/greeting/{bookId}")
    @GET
    @Timed
    public String greeting(@PathParam("bookId") Long bookId) {
        String backendServiceUrl =
                String.format("http://%s:%d",
                        backendServiceHost, backendServicePort);

        Map map = client.target(backendServiceUrl).path("hola-backend").path("rest").path("books").path(
                bookId.toString()).request().accept("application/json").get(Map.class);

        return map.toString();
    }
}
```

可以看到，通过访问：`http://localhost:8080/api/greeting/1`，能够进行Book资源的查询工作，但是可以看到`GreeterRestResource`的构造函数，需要一个`javax.ws.rs.client.Client`。前面提到，Dropwizard没有依赖注入的帮助，一切都要靠自己来完成组装，所以最后还需要编辑`HolaDropwizardApplication`，在`run()`方法中，增加以下内容：

```java
// greeter service
GreeterSayingFactory greeterSayingFactory = configuration.getGreeterSayingFactory();
Client greeterClient =
        new JerseyClientBuilder(environment)
                .using(greeterSayingFactory.getJerseyClientConfig()).build("greeterClient");
environment.jersey().register(new GreeterRestResource(
        greeterSayingFactory.getSaying(),
        greeterSayingFactory.getHost(),
        greeterSayingFactory.getPort(), greeterClient));
```

Dropwizard提供了两种REST调用方式：HttpComponents和Jersey/JAX-RS，默认使用的后者，我们就使用它来完成调用。接下来我们在开发机上部署了`hola-backend`，它的ip是`11.239.175.192`，你的环境也许是其他。我们需要将ip设置到环境变量`GREETER_BACKEND_HOST`上。在`hola-dropwizard`工程目录下执行：

```sh
$ export GREETER_BACKEND_HOST="11.239.175.192"
$ echo $GREETER_BACKEND_HOST
11.239.175.192
$ mvn clean package exec:java
```

访问`http://localhost:8080/api/greeting/1`，可以看到如下内容：

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter3-8.png" width="70%" height="70%" />
</center>

## 小结

本章介绍了Dropwizard的基本知识，可以看到一个不同于Spring Boot的方式去暴露REST端点、以及不同的方式进行应用配置，如果你想深入了解Dropwizard可以访问如下内容。

* [Dropwizard Core](http://www.dropwizard.io/1.1.0/docs/manual/core.html)
* [Dropwizard Getting Started](http://www.dropwizard.io/1.1.0/docs/getting-started.html)
* [Client API](http://www.oracle.com/splash/java.net/maintenance/index.html)
* [Dropwizard on GitHub](https://github.com/dropwizard/dropwizard)
* [Dropwizard examples on GitHub](https://github.com/dropwizard/dropwizard/tree/master/dropwizard-example)
