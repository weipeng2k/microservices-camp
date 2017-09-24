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
$ sudo docker images
REPOSITORY                        TAG                 IMAGE ID            CREATED             SIZE
weipeng2k/hola-springboot           1.0                 7405f14d812e        21 seconds ago      436MB
fabric8/java-jboss-openjdk8-jdk   1.0.10              1a13e31efd4b        13 months ago       421MB
```

如果环境配置正确，并且启动了CDK，就可以使用`mvn -Pf8-local-deploy`来部署到本地的Kubernetes上。Kubernetes暴露了一个REST接口，并允许外界操作集群，Kubernetes遵循最终状态一致的原则，使用者只需要描述部署的要求和模式，Kubernetes就会尽力将其完成。举个例子：如果我们需要启动一个运行了`hola-springboot`的Pod，我们可以通过HTTP请求，将一个JSON/YAML传递给Kubernetes，Kubernetes会理解请求，然后创建对应的Pod，在Pod中创建Docker容器来运行应用，并在集群中对Pod进行调度。一个Kubernetes的Pod是原子化的，能够在Kubernetes集群中进行调度，同时它是有至少一个Docker容器所组成。

通过使用`fabric8-maven-plugin`可以将项目完成构建并通过Kubernetes的REST接口创建Pod，只需要运行`mvn -Pf8-local-deploy`，然后通过CDK的`oc get pod`检查是否部署成功。**由于译者并没有使用CDK** ，所以采用的方式是通过`mvn -Df8-build`来构建镜像，然后用minikube将镜像在本地完成部署的方式来进行，下面介绍一下具体的操作方式。我们观察可以发现，通过执行`mvn -Pf8-build`后在本地的docker镜像仓库中已经生成了镜像，如果使用原生的docker是否可以启动它呢？我们尝试一下。

> 以下内容在功能上同原文一致，但是工具使用上不会使用CDK<br>docker部署属于新增内容

```sh
$ sudo docker run -d -p 8080:8080 --name hola-spring -it weipeng2k/hola-springboot:1.0
264c5d2ca402910d3c567dd4f0f19b6e04a769e503ac8d198441f35b2e8c7d58
$ curl http://localhost:8080/api/holaV1
Hola Spring Boot @ 172.17.0.2
```

可以看到我们通过`docker run`启动了一个容器，并将容器的8080端口绑定到了本机的8080上，当然启动一个容器是不够的，我们需要分布式部署应用，因此需要多个容器实例，可以通过在本机不同端口上启动多个容器实例，具体做法如下：

```sh
$ sudo docker run -d -p 8081:8080 -it --name hola-springboot2 weipeng2k/hola-springboot:1.0
492451e8b52993084246dc2b4ff78e7fff41f95d89e3f01670c2d6cec9774a47
$ curl http://localhost:8081/api/holaV1
Hola Spring Boot @ 172.17.0.3
```

通过启动一个映射在本机8081端口上的容器，这样客户端就可以通过负载均衡策略来访问其中一个容器中的应用了。由于镜像具备不变递交的特性，那么就可以在不同的主机上部署多个相同的容器实现分布式部署，这么一看，实现分布式部署在docker的帮助下其实很简单呢，但是还是会面对以下问题：

* 如果其中一个容器实例崩溃了呢？
* 整个系统的负载是否可以支持？
* 如果扩容，已经有了最终的容器实例数，但是要新增多少个呢？
* 客户端的负载均衡如何保证健壮性？

直接使用docker容器来进行部署，会面对这些问题，而接下来将使用Kubernetes进行部署，在它的帮助下来解决这些问题。接下来，采用yml的方式部署到Kubernetes，首先运行完成`mvn -Pf8-build`之后，在`classes`目录下会生成Kubernetes的部署描述符。

```sh
microservices-camp/hola-springboot/target/classes$ ls
application.properties  com  kubernetes.json  kubernetes.yml  META-INF
```

其中`kubernetes.json`和`kubernetes.yml`是`f8`插件替我们生成的，但是该文件的格式只有CDK认识，标准的Kubernetes无法识别，我们需要从中截取一部分才能够使用，生成部署描述文件主要由两部分，一个是`Service`，另一个是`ReplicationController`，我们在正式部署时，很少的面对`Pod`，因为它太细节，更多的是面向`Service`和`ReplicationController`。我们使用yml来创建，先将`kubernetes.yml`中的`ReplicationController`部分拷贝到另一个文件中，文件名可以自定义，文件内容如下：

> 译者创建了一个`rc.yml`的部署描述文件，放置在`hola-springboot`工程目录下

```yml
apiVersion: "v1" #指定api版本，此值必须在kubectl api-version中
kind: "ReplicationController" #指定创建资源的角色/类型
metadata: #资源的元数据/属性
  annotations: #自定义注解列表
    fabric8.io/git-branch: "master"
    fabric8.io/git-commit: "116ea497b4f32f48d9395827c5879955b1eca71d"
  labels:
    project: "hola-springboot"
    provider: "fabric8"
    version: "1.0"
    group: "com.murdock.examples"
  name: "hola-springboot"
spec:
  replicas: 1 #副本数量
  selector: #RC通过spec.selector来筛选要控制的Pod
    project: "hola-springboot"
    provider: "fabric8"
    version: "1.0"
    group: "com.murdock.examples"
  template: #这里写Pod的定义
    metadata:
      annotations: {}
      labels:
        project: "hola-springboot"
        provider: "fabric8"
        version: "1.0"
        group: "com.murdock.examples"
    spec: #这里是资源内容
      containers:
      - args: []
        command: []
        env: #指定容器中的环境变量
        - name: "KUBERNETES_NAMESPACE"
          valueFrom:
            fieldRef:
              fieldPath: "metadata.namespace"
        image: "weipeng2k/hola-springboot:1.0"
        name: "hola-springboot"
        ports:
        - containerPort: 8080
          name: "http"
        - containerPort: 8778
          name: "jolokia"
        readinessProbe: #pod内容器健康检查的设置
          httpGet: #通过httpget检查健康，返回200-399之间，则认为容器正常
            path: "/health"
            port: 8080
          initialDelaySeconds: 5
          timeoutSeconds: 30
        securityContext: {}
        volumeMounts: []
      imagePullSecrets: []
      nodeSelector: {}
      volumes: []
```

运行`kubectl create`命令可以将镜像进行部署，创建对应的Pod。

```sh
$ kubectl create -f rc.yml
replicationcontroller "hola-springboot" created
```

运行`kubectl get pods`可以查看创建的Pod，可以看到`hola-springboot-fmfc7`就是我们进行部署的Pod

```sh
$ kubectl get pod
NAME                              READY     STATUS    RESTARTS   AGE
hello-minikube-2210257545-ndc6s   1/1       Running   4          20d
hola-springboot-fmfc7             1/1       Running   0          1m
```

> 一个Java的微服务启动时间到自检完成，可能在30s以上

接下来，可以通过`kubectl delete pod $pod_name`来停止一个Pod。

```sh
$ kubectl delete pod hola-springboot-fmfc7
pod "hola-springboot-fmfc7" deleted
$ kubectl get pods
NAME                              READY     STATUS    RESTARTS   AGE
hello-minikube-2210257545-ndc6s   1/1       Running   4          20d
hola-springboot-k843c             0/1       Running   0          4s
```

可以看到，虽然通过`kubectl delete`删除了一个Pod，但是Kubernetes迅速的又启动了一个，简直就是一个打不死的小强，更准确的说另一个Pod在我们删除上一个Pod后被创建了。Kubernetes能够为你的微服务做到启动、停止和自动重启，你能想象如果你要检查一个服务的容量有多困难吗？让我们接下来探索Kubernetes带来的更有价值的集群管理技术，并用这些技术来管理我们的微服务。

## 伸缩性

微服务架构的一个巨大优点就是可伸缩性，我们能够在集群中复制很多服务，但不用担心端口冲突、JVM或者依赖缺失。在Kubernetes中，可以通过 **ReplicationController** 完成伸缩工作，我们先查看一下本机环境中部署的 **ReplicationController**。

```sh
$ kubectl get rc
NAME              DESIRED   CURRENT   READY     AGE
hola-springboot   1         1         0         1m
```

可以看到我们创建了一个 `ReplicationController`，在`rc.yml`中，我们定义`replicas`为`1`，这代表在任何时候都要有一个Pod实例运行我们的微服务应用，如果一个Pod挂掉，Kubernetes会进行判断该Pod的最终状态是需要几个，然后为我们创建等额的Pod，如果我们需要改变Pod（复制体）的数量，做到服务的伸缩该怎么办呢？

```sh
$ kubectl scale rc hola-springboot --replicas=3
replicationcontroller "hola-springboot" scaled
```

运行`kubectl scale rc $rc_name --replicas=$count`可以调整复制控制器的Pod目标数目，上述命令将hola-springboot调整为3个。我们使用`kubectl get pods`观察一下是否生效。

```sh
$ kubectl get pods
NAME                              READY     STATUS    RESTARTS   AGE
hola-springboot-6fj5k             1/1       Running   0          1h
hola-springboot-k843c             1/1       Running   0          1h
hola-springboot-ltzs5             1/1       Running   0          1h
```

如果这个过程中这些Pod有挂掉，Kubernetes将会尽可能的确定复制体的数量是 **3**。注意，我们不需要改变这些服务监听的端口或者任何不自然的端口映射，每一个实例都是在监听8080端口，而且相互之间不会冲突。接下来将复制体的数量设置为0，只需要运行以下命令：

```sh
$ kubectl scale rc hola-springboot --replicas=0
replicationcontroller "hola-springboot" scaled
$ kubectl get pods
No resources found.
```

Kubernetes也具备根据各种指标，诸如：CPU、内存使用率或者用户自定义触发器来进行 [自行伸缩](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) 的能力，能控制复制体（Pod）的增加或者减少。自行伸缩已经超过了本书的范畴，但是它仍是我们需要关注的集群管理技术。

> 一般一个Pod会占一个CPU，如果是4核，那么就可以运行4个，超过4个后，你会发现各个Pod在交替启动运行，本机使用的A8是4核处理器，在默认情况下无法保证4个以上的Pod都处于Running状态

## 服务发现（Service discovery）

我们需要理解Kubernetes中最后一个概念是 **Sevice** ， 一个Service是一组Pods之间间接关系的简单抽象，它是应用用来描述一组Pods的表现形式。我们在之前的例子中看到Kubernetes是如何管理Pods的创建和消亡，我们也了解到Kubernetes能够方便的进行一个服务的伸缩，在接下来的例子中，我们将尝试启动`hola-backend`服务，然后使用`hola-springboot`与`hola-backend`之间进行通信。

> `hola-backend`服务之前作为服务提供方，提供了Book的创建和查询服务，**这里的backend比原书中的例子更贴近现实**

接下来将`hola-backend`镜像化，然后分别在Docker和Kubernetes中尝试部署， **这部分内容在原书中没有涉及**。对于`hola-backend`的镜像构建，我们需要将WildFly构建在其中，所以先进入`hola-backend`工程目录，执行mvn构建，然后在target目录下建立Dockerfile并进行镜像构建。

```sh
microservices-camp/hola-backend$ mvn clean package
microservices-camp/hola-backend$ cd target/
weipeng2k@weipeng2k-workstation:~/Documents/workspace/microservices-camp/hola-backend/target$ ls
classes  generated-sources  hola-backend  hola-backend-swarm.jar  hola-backend.war  hola-backend.war.original  maven-archiver  maven-status
microservices-camp/hola-backend/target$ vi Dockerfile
```

Dockerfile的内容如下：

```sh
FROM jboss/wildfly
MAINTAINER weipeng2k "weipeng2k@126.com"
ADD hola-backend.war /opt/jboss/wildfly/standalone/deployments/
```

可以看到父镜像来自于`jboss/wildfly`，构建时将war包拷贝到jboss WildFly默认的部署目录，执行`sudo docker build -t="weipeng2k/hola-backend:1.0" .`进行镜像构建，随后在启动这个镜像时，将会完成`hola-backend`的部署。

> 如果不想本地构建进项，可以执行`sudo docker pull weipeng2k/hola-backend:1.0`从docker hub上获取

启动镜像，通过执行`sudo docker run --name hola-backend -itd -p 8080:8080 weipeng2k/hola-backend:1.0`（如果自己构建的镜像，注意镜像的名称），启动容器后将本机的8080端口与容器中的8080进行映射，可以使用Postman进行测试。

接下来需要创建Kubernetes对应的ReplicationController和Service，需要编写Kubernetes描述文件，先看一下ReplicationController描述文件。

> rc.yml和svc.yml可以在`hola-backend`工程目录下找到

```yml
apiVersion: "v1" #指定api版本，此值必须在kubectl api-version中
kind: "ReplicationController" #指定创建资源的角色/类型
metadata: #资源的元数据/属性
  labels:
    project: "hola-backend"
    provider: "fabric8"
    version: "1.0"
    group: "com.murdock.examples"
  name: "hola-backend"
spec:
  replicas: 1 #副本数量
  selector: #RC通过spec.selector来筛选要控制的Pod
    project: "hola-backend"
    provider: "fabric8"
    version: "1.0"
    group: "com.murdock.examples"
  template: #这里写Pod的定义
    metadata:
      annotations: {}
      labels:
        project: "hola-backend"
        provider: "fabric8"
        version: "1.0"
        group: "com.murdock.examples"
    spec: #这里是资源内容
      containers:
      - args: []
        command: []
        env: #指定容器中的环境变量
        - name: "KUBERNETES_NAMESPACE"
          valueFrom:
            fieldRef:
              fieldPath: "metadata.namespace"
        image: "weipeng2k/hola-backend:1.0"
        name: "hola-backend"
        ports:
        - containerPort: 8080
          name: "http"
        readinessProbe: #pod内容器健康检查的设置
          httpGet: #通过httpget检查健康，返回200-399之间，则认为容器正常
            path: "/"
            port: 8080
          initialDelaySeconds: 5
          timeoutSeconds: 30
        securityContext: {}
        volumeMounts: []
      imagePullSecrets: []
      nodeSelector: {}
      volumes: []
```

在`rc.yml`中定义了Pods，以及服务对应的镜像，通过`kubectl create -f rc.yml`可以让Kubernetes创建对应的ReplicationController，然后按照replicas中对复制体的数量约定，开始创建一个Pod。

> 启动的过程比较慢，原因在于下载weipeng2k/hola-backend:1.0这个镜像，后续读者可以使用aliyun等镜像，如果要确定当前Kubernetes的工作状态，可以使用`kubectl describe pods`来观察当前的部署情况

> 可以通过`minikube ssh`登录，然后执行`docker pull weipeng2k/hola-backend:1.0`后，可以观察到下载进度，该过程比较缓慢

当`hola-backend`的ReplicationController启动后，可以通过`kubectl get pods`检查一下当前Pod的状态。

```sh
$ kubectl get pods
NAME                 READY     STATUS    RESTARTS   AGE
hola-backend-t06sg   1/1       Running   1          2d
$ kubectl get rc
NAME           DESIRED   CURRENT   READY     AGE
hola-backend   1         1         1         2d
```

可以看到我们检查了刚创建的ReplicationController和由ReplicationController创建出来的Pod，接着我们开始创建Service，通过`kubectl create -f svc.yml`可以让Kubernetes创建对应的服务。

```yml
apiVersion: "v1"
kind: "Service"
metadata:
  annotations: {}
  labels:
    project: "hola-backend"
    provider: "fabric8"
    version: "1.0"
    group: "com.murdock.examples"
  name: "hola-backend"
spec:
  deprecatedPublicIPs: []
  externalIPs: []
  ports:
  - port: 80
    protocol: "TCP"
    targetPort: 8080
  selector: # Service使用该selector来对应到Pods
    project: "hola-backend"
    provider: "fabric8"
    group: "com.murdock.examples"
  type: "LoadBalancer"
```

通过`kubectl get service`可以查看在Kubernetes中部署的服务。

```sh
$ kubectl get svc
NAME           CLUSTER-IP   EXTERNAL-IP   PORT(S)        AGE
hola-backend   10.0.0.131   <pending>     80:30699/TCP   1h
kubernetes     10.0.0.1     <none>        443/TCP        33d
```

注意 **Service** 创建出来后，有两个有趣的属性需要我们关注一下。一个是 `CLUSTER-IP`，这个虚拟的IP被分配给了一个Service实例，分配之后不会变化，这是一个固定的IP，在Kubernetes集群中运行的实例都能够通过这个IP和后面的 Pods 进行通信。而 Service后面的Pods是被 **SELECTOR** 选择出来的。可以看到当前`svc.yml`中定义的`selector`对于属性的指定。Pods在Kubernetes中可以被标定上任意的属性，你可以使用（类似：version、component或者team等）各种KV表示。在这个例子中，一个名为`hola-backend`服务，它会选择标有`project=hola-backend and provider=fabric8`的Pods，这代表着只要能够通过该选择器选择到的Pods，都能够用这个服务的`CLUSTER-IP`所访问到，而这个过程不需要复杂的分布式注册中心（例如：Zookeeper、Consule或者Eureka）支持，这个机制是Kubernetes内置的特性，集群级别（Cluster-level）的 **DNS** 也被内置在Kubernetes中。使用DNS作为微服务的服务发现机制是非常有挑战和困难的，在Kubernetes中集群的DNS指向`CLUSTER-IP`，而这个`CLUSTER-IP`对于各个服务是固定不变的，这样就不会出现使用传统的DNS解决方案中面对的DNS缓存或者其他诡异的问题。

> 原书中是`component=backend`，应该是错误，不应该是component而是project

我们可以给`hola-springboot`添加一组环境变量，让其`books.backendHost`和`books.backendPort`能够指定到`hola-backend`服务上，这样我们就可以在Kubernetes中运行`hola-springboot`并使其调用后端的`hola-backend`服务，在`hola-springboot`中添加配置。

```xml
<fabric8.env.GREETING_BACKENDSERVICEHOST >hola-backend</fabric8.env.GREETING_BACKENDSERVICEHOST>
<fabric8.env.GREETING_BACKENDSERVICEPORT>80</fabric8.env.GREETING_BACKENDSERVICEPORT>
```

Spring Boot会解析我们在应用中配置的`application.properties`，但是它会被系统环境变量所覆盖，通过执行`mvn fabric8:json`可以生成新的Kubernetes描述文件。**这里原书中的Spring Boot示例存在问题** ，因为它没有定义和环境变量之间的映射关系，这样做是无法工作的，因此译者采取的方式是修改`application.properties`中的内容，重新构建镜像（1.1版本），然后在Kubernetes中部署，把这个例子跑起来。

在没有部署到容器之前，我们对于`hola-springboot`中`application.properties`的定义是基于ip的，因此我们需要将其修改为基于域名，如下所示：

```sh
helloapp.saying=Guten Tag aus
management.security.enabled=false
books.backendHost=hola-backend
books.backendPort=80
```

> 其中`hola-backend`服务支持在Kubernetes中对`hola-backend`这个域名解析，同时将请求派发到后端的Pods上

修改`pom.xml`，将版本号改为`1.1`，保证每次镜像的构建都是独立的。

在`hola-springboot`工程目录下执行`mvn -Pf8-build`，构建镜像到本地，然后通过`docker push $image`将其推到docker hub，这样Kubernetes才能够下载对应的镜像。新的镜像是`weipeng2k/hola-springboot:1.1`，这样也需要新的`rc.yml`，我们只需要修改原来`spring-boot`中的镜像版本即可（将其从1.0改为1.1）。

> 在`hola-springboot`工程根目录下，`rc2.yml`和`svc2.yml`对应的是修改后的Kubernetes配置

运行`kubectl create -f rc2.yml`和`kubectl create -f svc2.yml`，创建了`hola-springboot`对应的ReplicationController和Service，通过`kubectl get pods`可以观察已经创建的Pods。

```sh
$ kubectl get pods
NAME                    READY     STATUS    RESTARTS   AGE
hola-backend-t06sg      1/1       Running   1          2d
hola-springboot-mdx1v   1/1       Running   0          1m
```

现在我们想象有一种Debug技术，能够有端口转发的能力，它能够建立起`hola-springboot-mdx1v`和本机之间的管理，这样我们就可以验证服务是否正常工作，这个功能在Kubernetes中通过`kubectl port-forward`来完成。由于`kubectl port-forward`会启动一个进程来完成本机和Pods之间的通信，所以我们将其设定成为后台进程，防止它退出。

```sh
$ kubectl port-forward hola-springboot-mdx1v 9000:8080 &
[1] 7478
$ kubectl port-forward hola-backend-t06sg 9001:8080 &
[2] 7490
```

我们建立两个`port-forward`，分别指向`hola-springboot`和`hola-backend`，端口分别是本机的`9000`和`9001`，现在我们尝试访问一下`hola-springboot`。

```sh
$ curl http://localhost:9000/api/holaV1
Handling connection for 9000
Hola Spring Boot @ 172.17.0.5
```

返回的`172.17.0.5`就是Kubernetes为当前Pod分配的IP，接着我们需要访问一下Book接口，在此之前我们先添加一本书。

```sh
$ curl -H "Content-Type: application/json" -X POST -d '{"name":"Java编程的艺术","authorName":"魏鹏","publishDate":"2015-07-01"}' http://localhost:9001/hola-backend/rest/books/
Handling connection for 9001
```

在访问`hola-backend`添加了一个数据之后，我们访问`hola-springboot`。

```sh
$ curl http://localhost:9000/api/books/1
Handling connection for 9000
{id=1, version=0, name=Java编程的艺术, authorName=魏鹏, publishDate=2015-07-01}
```

我们可以看到`hola-backend`返回给了`hola-springboot`正确的查询数据，因此我们的服务被正确的发现了，而这仅仅是使用到了Kubernetes服务发现的一部分特性。需要特别指出的是，这个过程我们没有使用任何额外的客户端组件来完成服务的注册和发现，我们虽然使用了Java来完成这个场景的构建，但是Kubernetes集群的DNS能够提供技术无关的支持，一个基础通用的服务发现机制。

## 容忍失败（Fault Tolerance）

构建类似微服务架构这样的复杂分布式系统，需要在心中有一个重要的假设：没有什么不会坏的（things will fail）。我们能花大量的精力来防止失败，但是就算这样，我们也不能预防所有的案例，因此面对这个必然的假设唯一的解法就是：我们面向失败进行设计，换一句话说就是，如何做到在一个充满变数的不稳定环境中幸存。

> figure out how to survive in an environment where there are failures

## 集群自愈

如果一个服务开始变得有问题，我们怎样发现它呢？理想情况下我们的集群管理系统能够探测到它，并且通过一切可能的方式通知到我们，这种方式是传统环境惯用的手段。当把环境切换到一定规模的微服务环境下，我们有一大堆类似的微服务应用，我们真的需要停下来逐个分析到底是哪里导致了一个服务的不正常吗？长时间运行的服务可能处于不健康的状态。一个简单的途径就是把我们的微服务设计成为一种能够在任意时刻被杀死，特别是能够当它们出现问题时被杀死的架构体系。

Kubernetes提供一组开箱即用的健康探测仪（Probe），我们能用它们来实现集群对实例的管理，使之能够做到自愈。第一个需要介绍的是readiness探测仪，Kubernetes通过它来判断当前的Pod是否能够被服务发现或者挂载到负载均衡上，需要readiness探测仪的原因是应用在容器中启动之后，仍需要一段时间启动应用的进程，这时需要等待应用已经完全启动完成之后才能够让流量进入。

如果我们在刚启动的阶段放入流量，该Pod并不一定在那一刻处于正常状态，用户就会获得失败的结果或者不一致的状态。使用readiness探测仪，我们能够让Kubernetes查询一个HTTP端点，当且仅当该端点返回200或者其他结果时才会放入流量。如果Kubernetes探测到一个Pod在一段时间内readiness探测仪一直返回失败，这个Pod将会被杀死并重启。

另一个健康探测仪被称为liveness探测仪，它和readiness探测仪很像，它被检测的时间段是在Pod到达ready状态后，当时在流量放入前（可以理解为早于readiness探测仪）。如果liveness探测仪（可能也是一个简单的HTTP端点）返回了一个不健康的状态（比如：HTTP 500），Kubernetes就会自动的杀死该Pod并重启。

当我们使用`jboss-forge`工具以及`fabric8-setup`命令进行工程创建时，一个readiness探测仪就被默认创建了，可以观察一下`hola-springboot`的`pom.xml`，如果没有创建，可以使用`fabric8-readiness-probe`或者`fabric8-liveness-probe`两个maven命令进行创建。

```xml
<fabric8.readinessProbe.httpGet.path>/health</fabric8.readinessProbe.httpGet.path>
<fabric8.readinessProbe.httpGet.port>8080</fabric8.readinessProbe.httpGet.port>
<fabric8.readinessProbe.initialDelaySeconds>5</fabric8.readinessProbe.initialDelaySeconds>
<fabric8.readinessProbe.timeoutSeconds>30</fabric8.readinessProbe.timeoutSeconds>
```

可以看到在`pom.xml`中对于readiness探测仪的配置，而生成的Kubernetes描述文件中的内容如下：

```yml
readinessProbe: #pod内容器健康检查的设置
  httpGet: #通过httpget检查健康，返回200-399之间，则认为容器正常
    path: "/health"
    port: 8080
  initialDelaySeconds: 5
  timeoutSeconds: 30
```

这意味着readiness探测仪在`hola-springboot`中已经进行了配置，而Kubernetes会周期性检查Pod的`/heath`端点。当我们给`hola-springboot`添加了actuator时，就默认添加了一个`/heath`端点，这点在Dropwizard和WildFly Swarm也可以按照一样模式处理！

## 断路器（Circuit Breaker）

作为服务提供者，你的职责是提供给消费者稳定的服务，根据《Java环境下的微服务》章节提到的承诺设计，服务提供者也会依赖其他的服务或者下游系统，但是这种依赖不能是强依赖。一个服务提供者对其消费者服务承诺有完全的责任，因为分布式系统中总会存在失败，而这些失败将会导致承诺无法被兑现。在我们之前的例子中，`hola-spring`会调用到`hola-backend`，如果`hola-backend`不可用，将会发生什么？在这个场景下我们如何能够信守服务承诺？

我们需要处理这些分布式系统中常见的错误，一个服务可能会不可用，底层网络可能会间歇性的不稳定，后端服务可能因为负载升高而导致它的响应变慢，一个后端服务的bug可能会造成服务调用时收到异常。如果我们不显式的处理这些场景，我们就会面临自己提供的服务降级的风险，可能会使线程处于阻塞状态，不仅如此可能会造成获取的资源（如：数据库锁或者其他资源）没有被释放，进而导致整个分布式系统出现雪崩。为了解决这个问题，我们将使用`NetflixOSS`工具栈下的`Hystrix`。

`Hystrix`是一个支持容错的Java类库，它能够支持微服务兑现服务的承诺，主要在以下几个方面：

* 当依赖不可用时提供保护
* 支持监控并提供调用依赖时的超时机制，用于调用依赖服务时的延迟问题
* 负载隔离和自愈
* 优雅降级
* 实时的监控失败状态
* 支持处理失败时的业务逻辑

使用`Hystrix`的方式，就是使用命令模式，将调用外部依赖的逻辑放置在`HystrixCommand`的实现中，也就是将调用外部可能失败的代码放置在`run()`方法中。为了帮助我们开始了解这个过程，我们从`hola-wildflyswarm`项目入手，我们使用`Netflix`的最佳实践，显式的进行调用，为什么这样做？因为调试分布式系统是一件非常困难的工作，调用栈分布在不同的机器上，而越少的干预使用者，使调试过程变得简单就显得很重要了。

> 虽然`Hystrix`提供了注解的使用方式，但是我们仍然使用最基本的方式去用

在`hola-wildflyswarm`项目中，增加依赖：

```xml
<dependency>
    <groupId>com.netflix.hystrix</groupId>
    <artifactId>hystrix-core</artifactId>
    <version>${hystrix.version}</version>
</dependency>
```

但实际上只需要增加`<hystrix.version>1.5.12</hystrix.version>`到pom属性即可。 ** 原书中的示例在新的docker ce下理论已经无法跑起来，支离破碎的实例代码让读者无法串联起整个过程，因此译者认为首先需要将`hola-wildflyswarm`跑起来，然后添加断路器的相关功能。 **

> 这个过程中，译者会介绍一下如何将镜像推送到aliyun，避免中美交互的网速问题。

首先在`hola-wildflyswarm`工程下，启动`jboss-forge`，然后运行`fabric8-setup`。然后修改POM，以下针对关键点做说明，具体的内容可以GitHub上对应的pom。对`docker-maven-plugin`需要增加一些配置，用来禁用fabric8提供的jolokia，这个组件目前和`WildFly`有兼容性问题。

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
                        <inline>
                            <id>${project.artifactId}</id>
                            <files>
                                <file>
                                    <source>
                                        ${project.build.directory}/${project.build.finalName}-swarm.jar
                                    </source>
                                    <outputDirectory>/</outputDirectory>
                                </file>
                            </files>
                        </inline>
                    </assembly>
                    <env>
                        <JAVA_APP_JAR>${project.build.finalName}-swarm.jar</JAVA_APP_JAR>
                        <AB_JOLOKIA_OFF>true</AB_JOLOKIA_OFF>
                        <AB_OFF>true</AB_OFF>
                        <JOLOKIA_OFF>true</JOLOKIA_OFF>
                    </env>
                </build>
            </image>
        </images>
    </configuration>
</plugin>
```

maven配置属性需要调整，镜像from的位置需要调整，不能使用fabric8的tomcat，否则跑不起来，因为`hola-wildflyswarm`到底还是一个基于JavaEE的项目。Kubernetes的readiness地址需要调整一下，这里放置了一个rest接口。

```xml
<properties>
    <failOnMissingWebXml>false</failOnMissingWebXml>
    <docker.from>docker.io/fabric8/java-jboss-openjdk8-jdk:1.0.10</docker.from>
    <docker.image>weipeng2k/${project.artifactId}:${project.version}</docker.image>
    <fabric8.readinessProbe.httpGet.path>/api/holaV1</fabric8.readinessProbe.httpGet.path>
    <fabric8.readinessProbe.httpGet.port>8080</fabric8.readinessProbe.httpGet.port>
    <fabric8.readinessProbe.initialDelaySeconds>5</fabric8.readinessProbe.initialDelaySeconds>
    <fabric8.readinessProbe.timeoutSeconds>30</fabric8.readinessProbe.timeoutSeconds>
    <fabric8.env.GREETING_BACKEND_SERVICE_HOST>hola-backend</fabric8.env.GREETING_BACKEND_SERVICE_HOST>
    <fabric8.env.GREETING_BACKEND_SERVICE_PORT>80</fabric8.env.GREETING_BACKEND_SERVICE_PORT>
    <fabric8.env.AB_JOLOKIA_OFF>true</fabric8.env.AB_JOLOKIA_OFF>
    <version.wildfly-swarm>2017.6.1</version.wildfly-swarm>
    <hystrix.version>1.5.12</hystrix.version>
</properties>
```

可以看到`hola-wildflyswarm`通过`fabric8.env`的配置形式，将系统变量进行了设置，使得容器能够在运行时感知到底层`hola-backend`提供的服务所在域名和端口，而这个`hola-backend`域名对应的真实地址，只有在Kubernetes环境中，由Kubernetes提供给所有的Pod。

在`hola-wildflyswarm`下，可以看到存在`rc.yml`和`svc.yml`，同时我们在项目中进行REST调用后，不再使用Map来作为返回值，而是定义了`Book`这个类型用来承接返回，接下来我们将它们运行起来。

```sh
$ kubectl create -f rc.yml
replicationcontroller "hola-wildflyswarm" created
$ kubectl create -f svc.yml
service "hola-wildflyswarm" created
```

运行`kubectl port-forward hola-backend-t06sg 9001:8080 &`，我们做一下数据的添加，将`hola-backend`的一个Pod端口映射到本机的`9001`上。

```sh
$ curl -H "Content-Type: application/json" -X POST -d '{"name":"Java编程的艺术","authorName":"魏鹏","publishDate":"2015-07-01"}' http://localhost:9001/hola-backend/rest/books/
Handling connection for 9001
```

添加数据之后，我们通过`kubectl get pods`查询一下创建的Pod。

```sh
$ kubectl get pods
NAME                      READY     STATUS    RESTARTS   AGE
hola-backend-t06sg        1/1       Running   4          24d
hola-springboot-mdx1v     1/1       Running   3          21d
hola-wildflyswarm-9gv39   1/1       Running   0          1h
```

接着创建`hola-wildflyswarm-9gv39`的代理，`kubectl port-forward hola-wildflyswarm-9gv39 9002:8080 &`，然后再请求一下接口，看是否能够获得数据。

```sh
$ curl http://localhost:9002/api/books/1
Handling connection for 9002
Book{id=2, name='Java编程的艺术', version=0, authorName='魏鹏', publishDate=Wed Jul 01 00:00:00 UTC 2015}
```

从调用接口的返回可以看到，`hola-wildflyswarm`通过REST调用到了`hola-backend`，但是如果`hola-backend`应用出现问题，我们再请求`hola-wildflyswarm`会发生什么呢？

```sh
$ kubectl scale rc hola-backend --replicas=0
replicationcontroller "hola-backend" scaled
$ kubectl get pods
NAME                      READY     STATUS    RESTARTS   AGE
hola-springboot-mdx1v     1/1       Running   3          21d
hola-wildflyswarm-9gv39   1/1       Running   0          1h
$ curl http://localhost:9002/api/books/1
Handling connection for 9002
^C
```

我们先将`hola-backend`缩容到0个，然后再请求原有的`hola-wildflyswarm`接口，结果卡在那里了，也就是`hola-wildflyswarm`提供的服务无法兑现承诺。
