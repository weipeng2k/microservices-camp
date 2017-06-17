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

<script type="text/javascript" src="https://asciinema.org/a/6kauk8aosiy3g05yt9k6ivunj.js" id="asciicast-14" async></script>

##
