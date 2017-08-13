# 集群管理、失败转移和负载均衡的实践

&nbsp;&nbsp;&nbsp;&nbsp;在前一章节中，我们快速的介绍了集群管理、Linux容器，接下来让我们使用这些技术来解决微服务的伸缩性问题。作为参考，我们使用的微服务工程来自于第二、第三和第四章节（Spring Boot、Dropwizard和WildFly Swarm）中的内容，接下来的步骤都适合上述三款框架。

## 开始

我们需要将微服务打包成为Docker镜像，最终将其部署到Kubernetes，首先进入到项目工程`hola-springboot`，然后启动`jboss-forge`，然后安装`fabric8`插件，这个插件使我们安装maven插件变得非常容易。

```sh
$ cd ~/Documents/workspace/microservices-camp/
weipengktekiMBP:microservices-camp weipeng2k$ ls
README.md         SUMMARY.md        book              book.json         hola-backend      hola-dropwizard   hola-springboot   hola-wildflyswarm hola-x            resource
$ forge
Using Forge at /usr/local/Cellar/jboss-forge/3.7.1.Final/libexec

    _____                    
   |  ___|__  _ __ __ _  ___
   | |_ / _ \| `__/ _` |/ _ \  \\
   |  _| (_) | | | (_| |  __/  //
   |_|  \___/|_|  \__, |\___|
                   |__/      

JBoss Forge, version [ 3.7.1.Final ] - JBoss, by Red Hat, Inc. [ http://forge.jboss.org ]
Hit '<TAB>' for a list of available commands and 'man [cmd]' for help on a specific command.

To quit the shell, type 'exit'.

[microservices-camp]$ addon-install --coordinate io.fabric8.forge:devops,2.2.148
***SUCCESS*** Addon io.fabric8.forge:devops,2.2.148 was installed successfully.
[microservices-camp]$ cd hola-springboot/
[hola-springboot]$ ls
hola-springboot.iml  mvnw  mvnw.cmd  pom.xml  src  target

[hola-springboot]$ fabric8-setup
***SUCCESS*** Added Fabric8 Maven support with base Docker image: fabric8/java-jboss-openjdk8-jdk:1.0.10. Added the following Maven profiles [f8-build, f8-deploy, f8-local-deploy] to make building the project easier, e.g. mvn -Pf8-local-deploy
```

运行完`fabric8-setup`之后，然后在IDE中打开`pom.xml`，可以发现增加了一些属性。

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <docker.assemblyDescriptorRef>artifact</docker.assemblyDescriptorRef>
    <docker.from>docker.io/fabric8/java-jboss-openjdk8-jdk:1.0.10</docker.from>
    <docker.image>weipeng2k/${project.artifactId}:${project.version}</docker.image>
    <docker.port.container.http>8080</docker.port.container.http>
    <docker.port.container.jolokia>8778</docker.port.container.jolokia>
    <fabric8.iconRef>icons/spring-boot</fabric8.iconRef>
    <fabric8.readinessProbe.httpGet.path>/health</fabric8.readinessProbe.httpGet.path>
    <fabric8.readinessProbe.httpGet.port>8080</fabric8.readinessProbe.httpGet.port>
    <fabric8.readinessProbe.initialDelaySeconds>5</fabric8.readinessProbe.initialDelaySeconds>
    <fabric8.readinessProbe.timeoutSeconds>30</fabric8.readinessProbe.timeoutSeconds>
    <fabric8.service.containerPort>8080</fabric8.service.containerPort>
    <fabric8.service.name>hola-springboot</fabric8.service.name>
    <fabric8.service.port>80</fabric8.service.port>
    <fabric8.service.type>LoadBalancer</fabric8.service.type>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    <java.version>1.8</java.version>
</properties>
```

> 注意这里docker.image改为了$username，使用者需要结合自己的情况进行修改

并且添加了两个maven插件：`docker-maven-plugin`和`fabric8-maven-plugin`。

```xml
<plugin>
  <groupId>io.fabric8</groupId>
  <artifactId>docker-maven-plugin</artifactId>
  <version>0.14.2</version>
  <configuration>
      <images>
          <image>
              <name>${docker.image}</name>
              <build>
                  <from>${docker.from}</from>
                  <assembly>
                      <basedir>/app</basedir>
                      <descriptorRef>${docker.assemblyDescriptorRef}</descriptorRef>
                  </assembly>
                  <env>
                      <JAR>${project.artifactId}-${project.version}.war</JAR>
                      <JAVA_OPTIONS>-Djava.security.egd=/dev/./urandom</JAVA_OPTIONS>
                  </env>
              </build>
          </image>
      </images>
  </configuration>
</plugin>
<plugin>
  <groupId>io.fabric8</groupId>
  <artifactId>fabric8-maven-plugin</artifactId>
  <version>2.2.100</version>
  <executions>
      <execution>
          <id>json</id>
          <phase>generate-resources</phase>
          <goals>
              <goal>json</goal>
          </goals>
      </execution>
      <execution>
          <id>attach</id>
          <phase>package</phase>
          <goals>
              <goal>attach</goal>
          </goals>
      </execution>
  </executions>
</plugin>
```

&nbsp;&nbsp;&nbsp;&nbsp;除了maven插件，还增加了一些maven profiles：

* f8-build<br>构建docker镜像和Kubernetes的`manifest.yml`
* f8-deploy<br>构建docker镜像、部署到远端的docker注册中心，并部署应用到Kubernetes
* f8-local-deploy<br>构建docker镜像，生成Kubernetes的`manifest.yml`并部署到本地运行的Kubernetes

`jboss-forge`插件是[Fabric8开源项目](https://fabric8.io)的一部分，Fabric8构建工具可以同Docker、Kubernetes、OpenShift进行交互，同时基于maven插件可以提供给应用诸如：依赖注入Spring/CDI、访问Kubernetes和OpenShift的API。Fabric8同时也集成了API管理，chaos monkey以及NetflixOSS等功能。

### 将微服务构建为Docker镜像

当`hola-springboot`项目添加了maven插件后，就可以使用maven命令来构建docker镜像了，在构建镜像之前需要确定系统以及安装了CDK。 **由于我们并没有安装CDK，所以需要进行一些配置和操作** ，首先需要将Docker服务暴露出管理API的端口，以及添加环境变量。

> 笔者并没有在mac进行构建，而是将代码push到git后，在ubuntu上执行构建的。

更改Docker服务，暴露管理的API端口的操作首先需要更改docker的配置，以ubuntu为例：`sudo vi /lib/systemd/system/docker.service`，将其中的 **ExecStart** 的值做修改：

```sh
ExecStart=/usr/bin/dockerd -H fd:// -H unix:///var/run/docker.sock -H tcp://0.0.0.0:4243
```

然后重启docker daemon，通过以下命令完成重启：

```sh
sudo systemctl daemon-reload
sudo systemctl restart docker
```

> 可以通过`telnet localhost 4243` 来检测一下，docker的API是否开始暴露了。

接下来需要设置环境变量，通过修改`~/.profile`，修改内容如下：

```sh
export DOCKER_HOST="tcp://127.0.0.1:4243"
PATH="$HOME/bin:$HOME/.local/bin:$PATH"
```

这个`DOCKER_HOST`环境变量就是fabric8构建时获取的Docker API，然后完成镜像的构建。在做完这些操作之后，在`hola-springboot`工程下，运行fabric8构建命令。

```sh
$:~/Documents/workspace/microservices-camp/hola-springboot$ mvn -Pf8-build
[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building hola-springboot 1.0
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] Added environment annotations:
[INFO]     Service hola-springboot selector: {project=hola-springboot, provider=fabric8, group=com.murdock.examples} ports: 80
[INFO]     ReplicationController hola-springboot replicas: 1, image: fabric8/hola-springboot:1.0
[INFO] Template is now:
[INFO]     Service hola-springboot selector: {project=hola-springboot, provider=fabric8, group=com.murdock.examples} ports: 80
[INFO]     ReplicationController hola-springboot replicas: 1, image: fabric8/hola-springboot:1.0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 36.954 s
[INFO] Finished at: 2017-08-12T21:24:56+08:00
[INFO] Final Memory: 52M/762M
[INFO] ------------------------------------------------------------------------
```

## 部署到Kubernetes

如果我们安装了Docker的客户端，我们可以在构建完成后查看是否生成了对应的镜像，可以看到通过`mvn -Pf8-build`之后，在本地安装了镜像。

```sh
$:~/Documents/workspace/microservices-camp/hola-springboot$ sudo docker images
REPOSITORY                        TAG                 IMAGE ID            CREATED             SIZE
weipeng2k/hola-springboot           1.0                 7405f14d812e        21 seconds ago      436MB
fabric8/java-jboss-openjdk8-jdk   1.0.10              1a13e31efd4b        13 months ago       421MB
```

如果环境配置正确，并且启动了CDK，就可以使用`mvn -Pf8-local-deploy`来部署到本地的Kubernetes上。Kubernetes暴露了一个REST接口，并允许外界操作集群，Kubernetes遵循最终状态一致的原则，使用者只需要描述部署的要求和模式，Kubernetes就会尽力将其完成。举个例子：如果我们需要启动一个运行了`hola-springboot`的Pod，我们可以通过HTTP请求，将一个JSON/YAML传递给Kubernetes，Kubernetes会理解请求，然后创建对应的Pod，在Pod中创建Docker容器来运行应用，并在集群中对Pod进行调度。一个Kubernetes的Pod是原子化的，能够在Kubernetes集群中进行调度，同时它是有至少一个Docker容器所组成。

通过使用`fabric8-maven-plugin`可以将项目完成构建并通过Kubernetes的REST接口创建Pod，只需要运行`mvn -Pf8-local-deploy`，然后通过CDK的`oc get pod`检查是否部署成功。 **由于译者并没有使用CDK，所以采用的方式是通过`mvn -Df8-build`来构建镜像，然后用minikube将镜像在本地完成部署的方式来进行，下面介绍一下具体的操作方式。**


$ minikube ssh
$ docker pull weipeng2k/hola-springboot:1.0
1.0: Pulling from weipeng2k/hola-springboot
a3ed95caeb02: Pull complete
da71393503ec: Pull complete
eb78add5bf3f: Pull complete
046239789b53: Pull complete
364eb6df56ec: Downloading [====================>                              ] 28.33 MB/68.74 MB
553371e68377: Download complete
63abc7cc2643: Download complete
92cddbb532d5: Download complete
d07d5053fa71: Download complete
a20e09c1ad23: Download complete
f9958ce057fe: Download complete
886c417e0275: Download complete
c0327652d3b6: Waiting




docker的url暴露，重启