# 使用WildFly Swarm构建微服务

我们最后介绍一个新的微服务框架，它构建在支持分层且可靠的`JavaEE`技术栈上（使用JBoss WildFly 应用服务器），WildFly Swarm是一个完全兼容WildFly应用服务器，它基于可重用的组件，这里称为元件（fractions）来组成微服务应用。组装这些元件和你使用maven或者gradle去添加依赖一样简单，你只需要声明元件，WildFly Swarm将会帮助你完成后续的工作。

应用服务器和`JavaEE`在企业级Java应用的领域耕耘了快20年了，WildFly（以前叫JBoss Application Server）作为一个开源的Java应用服务器在市场上出现，企业在`JavaEE`技术栈上的投入非常巨大（不论是开源还是专有的供应商），包括培训、工具开发、管理等方面。`JavaEE`通常都是能够帮助开发人员构建分层的应用，因为它提供了诸如：`servlets/JSP`、`transactions`、组件模型、消息以及持久化等技术。`JavaEE`应用在部署时，将会打包成为EARs（包括了WARs或者JARs以及配置），一旦你完成了打包，你就需要找到一个应用服务器，然后安装。你可以利用应用服务器的高阶特性，比如：动态部署或者重复部署（虽然这些在生产环境上用的比较少，但是在开发态比较有用），同时你的包也利用了应用服务器的特性，只会包含必要的jar包，变得非常轻，一般只会包含所需要的业务逻辑。虽然应用的包变小了，但是应用服务器却变得很臃肿，因为它不得不包含应用所有可能需要的依赖。应用服务器中的每个`JavaEE`组件都会极力优化自己的依赖，同时它们之间也会相互隔离。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-1.png" width="70%" height="70%" />
</center>

应用服务器在一个实例中提供了管理、部署以及配置多个应用的唯一入口，一般情况下为了提高可用性，会在不同的节点上部署多个实例。当越来越多的应用部署到一个应用服务器实例上后，问题开始出现，一个进程，一个JVM，很容易出问题。如果遇到多个团队开发的不同应用，都部署在一个应用服务器上，不同的应用类型、应用开发周期的不同、不同应用对于性能和SLA的要求，将会让情况变得更糟。相比微服务架构所提供的快速响应变化、创新以及自治，`JavaEE`应用这种将所有应用部署在单点、共享的方式就显得过于笨拙，不仅仅如此，考虑在一个单点去管理和监控多个应用就显得非常复杂。单个JVM好管理，可是在一个JVM里面放置多个应用就不那么好管理了，当我们在这个单点上使用昂贵的工具进行检测和调试找问题的时候，你就会更加感受这种痛苦了。一个解决方式就是在一个应用服务器中只部署一个应用。

虽然微服务不屑于`JavaEE`环境下的开发，但是不代表组件模型、APIs以及`JavaEE`技术没有价值。我们仍旧能够使用持久化、事务、安全以及依赖注入等特性，我们如何将`JavaEE`这些技术带入到微服务中呢？WildFly Swarm就是用来做这个的。

WildFly Swarm会根据你的`pom.xml`或者`gradle file`计算决定你需要何种`JavaEE`依赖，比如：`CDI、新消息或者servlet`，然后会将应用打包为一个统一的jar（就像Spring Boot和Dropwizard一样），这样打包的应用就包含了最小化的`JavaEE`环境。这个过程叫拼装刚好够用的`JavaEE`环境，它能让你继续使用`JavaEE`的API，而自由的选择使用微服务的方式运行，或者部署到传统的应用服务器中。你甚至可以将一个已经存在的WAR工程，通过WildFly Swarm将其构建成为包含了元件的微服务，而这种强大的能力使得你可以迅速的将一个已有的项目转化成为一个微服务部署模式的应用。

## 开始

一般有三种方式使用WildFly Swarm，第一种是创建一个空的maven或者gradle项目，然后手动的添加依赖和maven插件。另一种就是使用类似Spring Initializer的控制台 -- [WildFly Swarm Generator web console](http://wildfly-swarm.io/generator/)，最后一种是使用`jboss-forge`。我们强烈的建议使用`jboss-forge`来进行工程的创建，从完整性上考虑，我们会创建一个简单的工程，同时`jboss-forge`有插件支持流行的Java IDE，比如：Eclipse、Netbeans和IDEA。

## 香草Java工程

如果你有一个已经存在的Java工程，你可以通过修改一下`pom.xml`来让其变为一个WildFly Swarm，首先添加一个WildFly Swarm插件。

```xml
<plugin>
  <groupId>org.wildfly.swarm</groupId>
  <artifactId>wildfly-swarm-plugin</artifactId>
  <executions>
    <execution>
      <goals>
        <goal>package</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

如果项目想使用`JavaEE`的依赖，可以选择依赖WildFly DOM（bill of materials），在`<dependencyManagement>`元素下增加以下内容：

```xml
<dependencyManagement>
    <dependencies>
        <!-- JBoss distributes a complete set of Java EE 7 APIs
            including a Bill of Materials (BOM). A BOM specifies
            the versions of a "stack" (or a collection) of
            artifacts. We use this here so that we always get
            the correct versions of artifacts. -->
            <dependency>
              <groupId>org.wildfly.swarm</groupId>
              <artifactId>bom</artifactId>
              <version>${version.wildfly.swarm}</version>
              <type>pom</type>
              <scope>import</scope>
            </dependency>
    </dependencies>
</dependencyManagement>
```

现在你可以添加其他的`JavaEE`元件（或者你可以进行使用自己的依赖，让WildFly Swarm自己侦测依赖），接下来让我们看看`jboss-forge`如何构建工程。

> 笔者尝试将`hola-backend`按照如上方式改成WildFly Swarm，但是结果并不如意<br>尝试改造已有的web工程，结果也不好，可以看到这种方式只是一种点缀

## 使用`JBoss Forge`

`jboss-forge`是一个构建java项目的IDE插件和基于命令行的工具集，能够在Netbeans、Eclipse和IDEA中提供这些功能，帮助开发人员创建java项目、添加CDI beans，或者配置servlets。首先运行环境需要使用JDK/Java 1.8，在这个基础上进行安装 [JBoss Forge](http://forge.jboss.org/document/jboss-forge-2,-java-ee-easily,-so-easily)。

一旦你安装了`jboss-forge`，你可以在命令行下通过键入：`forge`，启动它。

> 在mac下使用Homebrew可以安装它，运行：brew install jboss-forge就可以了

你可以使用`Tab`键来获得提示并让命令自动完成，`jboss-forge`是一个基于模块化插件体系构建的工具，它支持其他用户对插件体系将进行扩展，你可以在这里找到扩展的插件[addons contributed by the community](http://forge.jboss.org/addons)，有很多插件可以选择，比如：`AsciiDoctor`、`Twitter`、`Arquillian`以及`AssertJ`，我们先用`jboss-forge`安装WildFly Swarm。

```sh
[temp]$ addon-install --coordinate org.jboss.forge.addon:wildfly-swarm,1.0.0.Beta2
***SUCCESS*** Addon org.jboss.forge.addon:wildfly-swarm,1.0.0.Beta2 was installed successfully.
```

> 笔者查看了WildFly Swarm文档，推荐这么安装最新版：addon-install-from-git --url https://github.com/forge/wildfly-swarm-addon.git --coordinate org.jboss.forge.addon:wildfly-swarm

进入`jboss-forge`后，可以使用`project-new`来进行项目的创建。接下来我们创建一个WildFly Swarm项目：`hola-wildflyswarm`，并且为这个应用添加REST以及WildFly Swarm的支持。

<script type="text/javascript" src="https://asciinema.org/a/nAKs21cW32xtwwcM3ZbR8NMH2.js" id="asciicast-nAKs21cW32xtwwcM3ZbR8NMH2" async></script>

> 可以通过`wildfly-swarm-run`来启动项目，但是退出项目遇到点小问题

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-2.png" />
</center>

我们将工程导入到IDE中，可以看到一个应用的骨架，如果我们打开`pom.xml`，会看到相关的`JavaEE`APIs以及WildFly Swarm插件。

```xml
<build>
  <finalName>hola-wildflyswarm</finalName>
  <plugins>
    <plugin>
      <groupId>org.wildfly.swarm</groupId>
      <artifactId>wildfly-swarm-plugin</artifactId>
      <version>${version.wildfly-swarm}</version>
      <executions>
        <execution>
          <goals>
            <goal>package</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
<properties>
  <failOnMissingWebXml>false</failOnMissingWebXml>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <version.wildfly-swarm>2017.6.1</version.wildfly-swarm>
</properties>
<dependencies>
  <dependency>
    <groupId>org.jboss.spec.javax.servlet</groupId>
    <artifactId>jboss-servlet-api_3.0_spec</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>org.jboss.spec.javax.ws.rs</groupId>
    <artifactId>jboss-jaxrs-api_1.1_spec</artifactId>
    <scope>provided</scope>
  </dependency>
  <dependency>
    <groupId>javax</groupId>
    <artifactId>javaee-api</artifactId>
    <scope>provided</scope>
  </dependency>
</dependencies>
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.jboss.spec</groupId>
      <artifactId>jboss-javaee-6.0</artifactId>
      <version>3.0.3.Final</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
    <dependency>
      <groupId>javax</groupId>
      <artifactId>javaee-api</artifactId>
      <version>7.0</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.wildfly.swarm</groupId>
      <artifactId>bom-all</artifactId>
      <version>${version.wildfly-swarm}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

记住，WildFly Swarm只会打包你需要的`JavaEE`框架，在这个例子中，我们首先添加了JAX-RS（`rest-setup`），所以WildFly Swarm会自动包含JAX-RS以及servlet元件，并将它们打包到你的应用中。

> 在WildFly Swarm中，有三个类型的类加载器，首先是启动它的SystemClassLoader，第二个是应用的类加载器，也就是用来执行用户代码的地方，最后一个是WildFly，有理由相信，WildFly Swarm利用了JBoss模块化技术，而这个技术的运用，使得应用代码只需要依赖标准的API，而具体的`JavaEE`实现和自己已经隔离开来

## 添加HTTP端点

现在在`jboss-forge`下，使用`rest-new-endpoint`添加一个JAX-RS端点。

> 进入到`hola-wildflyswarm`目录下，执行forge

<script type="text/javascript" src="https://asciinema.org/a/q1rVPiGceuO5AYpMqhnvzyOEL.js" id="asciicast-q1rVPiGceuO5AYpMqhnvzyOEL" async></script>

在IDE中，可以看到已经创建了一个类`HolaResource`：

```java
@Path("/api/holaV1")
public class HolaResource {

	@GET
	@Produces("text/plain")
	public Response doGet() {
		return Response.ok("method doGet invoked").build();
	}
}
```

我们可以通过在`jboss-forge`中运行`wildfly-swarm-run`将应用启动起来，打开浏览器访问：`http://localhost:8080/api/holaV1`，会看到如下页面。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-4.png" width="50%" height="50%" />
</center>

是不是很快，我们做了些什么？我们使用`jboss-forge`构建了一个JAX-RS的web应用，它使用原生的`JavaEE`技术，能够以微服务的形式运行。

##  外部配置

本文编写的时候，WildFly Swarm还没有一种方式进行外部配置，也没有一套配置框架，例如：Apache Commons Configuration或者Apache DeltaSpike Configuration组件。如果想要使用WildFly Swarm自己提供的配置功能，可以关注[this JIRA thread](https://issues.jboss.org/browse/SWARM-1273?jql=project%20%3D%20SWARM%20AND%20component%20%3D%20config-api)。在本章中，将使用Apache DeltaSpike Configuration来完成配置。

> 翻译本文时，WildFly Swarm配置已经可以使用，访问这里：[Swarm Config](https://reference.wildfly-swarm.io/v/2017.6.1/configuration.html)
> Apache DeltaSpike Configuration是一个2014年的Duke选择奖获得者，也非常不错

Apache DeltaSpike Configuration是一个CDI扩展的集合，用来简化诸如：配置、数据获取以及安全等方面，接下来就需要使用CDI扩展来将配置注入到代码中，而配置的来源能够是命令行、属性文件、JNDI或者环境变量。首先添加CDI的依赖。

```xml
<dependency>
   <groupId>org.wildfly.swarm</groupId>
   <artifactId>jaxrs-cdi</artifactId>
   <scope>provided</scope>
</dependency>
<dependency>
   <groupId>org.apache.deltaspike.core</groupId>
   <artifactId>deltaspike-core-api</artifactId>
   <version>1.5.3</version>
</dependency>
<dependency>
   <groupId>org.apache.deltaspike.core</groupId>
   <artifactId>deltaspike-core-impl</artifactId>
   <version>1.5.3</version>
</dependency>
```

由于使用了Apache DeltaSpike Configuration，使用它需要创建`META-INF/apache-deltaspike.properties`，在这个配置中放置我们需要的配置。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-3.png" width="80%" height="80%" />
</center>

这样针对变量`WF_SWARM_SAYING`，就可以注入这个值了，下面创建`HolaResource2`。

```java
@Path("/api/holaV2")
public class HolaResource2 {

    @Inject
    @ConfigProperty(name = "WF_SWARM_SAYING", defaultValue = "Hola")
    private String saying;

    @GET
    @Produces("text/plain")
    public Response doGet() {
        return Response.ok(saying + " from WF Swarm").build();
    }
}
```

可以看到如果没有`WF_SWARM_SAYING`配置，将会默认使用Hola，接着我们打包运行。

```sh
$ mvn clean package
$ java -jar target/hola-wildflyswarm-swarm.jar
```

打开浏览器，访问：`http://localhost:8080/api/holaV2`，可以看到以下内容。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-5.png" width="50%" height="50%" />
</center>

我们停止应用，然后导出一个环境变量，然后再启动应用。

```sh
$ export WF_SWARM_SAYING=Yo
$ java -jar target/hola-wildflyswarm-swarm.jar
```

打开浏览器，再次访问：`http://localhost:8080/api/holaV2`，可以看到内容已经变化。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-6.png" width="50%" height="50%" />
</center>

## 暴露应用Metrics和信息

暴露应用的Metrics十分简单，只需要添加一个依赖一个maven坐标即可，添加坐标：

```xml
<dependency>
    <groupId>org.wildfly.swarm</groupId>
    <artifactId>monitor</artifactId>
</dependency>
```

它将启动WildFly管理以及监控功能，从监控的角度看，WildFly Swarm暴露了一些基本的Metrics：

* /node<br>当前部署节点的信息
* /heap<br>堆信息
* /threads<br>Java的线程信息

当然也可以自定义添加一些端点用于检查微服务是否运作正常，你可以检测集群中哪台机器上的微服务是否工作正常，如果想了解详细的信息可以查看[WildFly Swarm documentation](https://wildfly-swarm.gitbooks.io/wildfly-swarm-users-guide/content/advanced/monitoring.html)。

## 调用其他服务

在微服务环境下，服务之间是会进行相互调用的，如果我们想使用之前的服务，就需要使用JAX-RS客户端，就像之前在Spring Boot微服务下调用books接口一样。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-7.png" width="50%" height="50%" />
</center>

接下来创建一个类型`GreeterResource`，使用JAX-RS客户端访问`hola-backend`，由于使用了JAX-RS，所以与`hola-dropwizard`的代码有些类似。

```java
@Path("/api")
public class GreeterResource {

    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_HOST",
            defaultValue = "localhost")
    private String backendServiceHost;
    @Inject
    @ConfigProperty(name = "GREETING_BACKEND_SERVICE_PORT",
            defaultValue = "8080")
    private int backendServicePort;

    @Path("/greeting/{bookId}")
    @GET
    public String greeting(@PathParam("bookId") Long bookId) {
        String backendServiceUrl = String.format("http://%s:%d",
                backendServiceHost, backendServicePort);
        System.out.println("Sending to: " + backendServiceUrl);
        Client client = ClientBuilder.newClient();
        Map map = client.target(backendServiceUrl).path("hola-backend").path("rest").path("books").path(
                bookId.toString()).request().accept("application/json").get(Map.class);

        return map.toString();
    }
}
```

在`hola-wildflyswarm`目录下，执行`mvn clean package`，完成应用的打包。在运行应用前，需要指定`hola-backend`服务端的位置，我们可以通过环境变量指定。

```sh
$ export GREETING_BACKEND_SERVICE_HOST="11.239.175.192"
$ java -jar target/hola-wildflyswarm-swarm.jar
```

打开浏览器访问：`http://localhost:8080/api/greeting/1`，可以看到如下展示。

<center>
<img src="https://github.com/weipeng2k/microservices-camp/raw/master/resource/chapter4-8.png" width="50%" height="50%" />
</center>

## 小结

通过本章内容，介绍了WildFly Swarm的基本使用方式，以及同Dropwizard和Spring Boot的对比，了解到如何暴露REST端点、配置、Metrics以及调用外部服务。快速的介绍WildFly Swarm不能面面俱到，下面是深入了解它的一些资源。

> 由于使用了jboss-module模块化系统启动应用，WildFly Swarm启动的速度慢于Spring Boot和Dropwizard，但是其隔离的体现，使得应用的jar包非常简单，旧有的`JavaEE`迁移会有优势，官方更新速度很快，但是小问题也很多

* [WildFly Swarm](http://wildfly-swarm.io)
* [WildFly Swarm documentation](http://wildfly-swarm.io/documentation)
* [WildFly Swarm examples on GitHub](https://github.com/wildfly-swarm/wildfly-swarm-examples)
* [WildFly Swarm Core examples on GitHub](https://github.com/wildfly-swarm/wildfly-swarm)
* [WildFly Swarm blog](http://wildfly-swarm.io/posts/)
* [WildFly Swarm Community](http://wildfly-swarm.io/community/)
