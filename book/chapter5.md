# 使用Docker和Kubernetes构建可伸缩的微服务

&nbsp;&nbsp;&nbsp;&nbsp;从现在开始，我们将从更高的维度讨论微服务，涵盖了组织敏捷性、设计和依赖的思考、领域驱动设计以及Promise理论。当我们深入使用之前介绍的三个流行的微服务框架：Spring Boot、Dropwizard和WildFly Swarm，我们能够使用它们开箱即用的能力去构建一个暴露或者消费REST服务的应用，能够使用外部环境对应用进行配置，可以打包成一个可执行的jar，同时提供Metrics信息，但这些都是围绕着一个微服务实例。当我们需要管理微服务之间的依赖、集群的启动和关闭、健康检查以及负载均衡的时候，我们使用微服务架构会面临什么问题呢？本章，我们将讨论这些高阶话题用来理解部署微服务时面对的挑战。

&nbsp;&nbsp;&nbsp;&nbsp;当我们开始将我们的应用和服务按照微服务的思路进行拆分后，我们将面临这样的场景：我们有了更多的服务、更多的二进制内容、更多的配置，更多的交互点等等。传统方式是将这些构建成一个二进制单元，比如：WARs或者EARs，然后将其打包后等待运维人员将它部署到我们指定的应用服务器上。如果对于高可用有要求，也会将应用服务器进行分布式部署，形成集群，依靠负载均衡、共享磁盘（数据库）等方式提升可用性。传统运维体系下也开发了一些自动化部署的工具，比如：`Chef`和`Ansible`，工具虽然简化了部署，但是开发人员还是需要面对部署时容易出现的问题，比如：配置、环境等不可预知的问题。

> [chef](https://www.chef.io/chef/)<br>Chef是由Ruby与Erlang写成的配置管理软件，它以一种纯Ruby的领域专用语言（DSL）保存系统配置“烹饪法（recipes）”或“食谱（cookbooks）”。Chef由Opscode公司开发，并在Apache协议版本2.0下开源发布。

> [ansible](https://www.ansible.com)<br>使用python构建，中文化资料比较多，Ansible的简洁界面和可用性非常迎合系统管理员的想法

&nbsp;&nbsp;&nbsp;&nbsp;在传统方式下尝试微服务的部署，将会是个糟糕的结果。如何解决应用服务器在开发、测试以及生产环境的不同配置？如果没有，如何能够捕获到这些配置的变更？而这些变更如何确认已经运行在应用服务器中了？运行时软件环境，比如：操作系统、JVM以及相关的组件在生产和开发环境下的不同问题如何解决？如果我们的应用已经针对特定的JVM做了调优，这些调优参数会不会影响到他人？如果部署微服务，你会选择使用进程隔离的方式将它们部署在一台机器上吗？如果其中一个微服务实例消耗了系统100%的资源，该如何是好？如果过度的占用了I/O或者共享存储怎么办？如果部署了多个微服务实例的宿主机崩溃了怎么办？我们的应用为此做过应对方案吗？将应用分拆为微服务是小，但面对的问题显然会更多。

## 不可变的递交

&nbsp;&nbsp;&nbsp;&nbsp;不可变的递交（Immutable delivery）原则可以帮助我们应对上述的部分问题，在这个体系下，我们将使用镜像技术来尝试减少开发到生产的步骤。例如：构建系统能够输出一个包含了操作系统、JVM、配置、相关组件的镜像，我们可以将它部署到一个环境中，测试它，如果通过测试，最终可以将它部署到生产环境中而不用担心开发流程使交付的软件缺少了什么。如果你想变更应用，那么可以回到刚才这个流程的最开始，应用你的修改，重新构建镜像，最终完成部署，如果出乎你的意料，程序有问题，你可以直接选择回滚到上一个正确的镜像而不用担心遗漏了什么。

&nbsp;&nbsp;&nbsp;&nbsp;这听起来很好，但是我们怎么做到呢？将应用打包成一个jar还不足以足够做到这些。JVM是底层实现，我们如何将它也打包进去，而JVM又使用了操作系统级别组件，这些内容我们都要打包，除此之外，我们还需要配置、环境变量、权限等等，这些都需要打包，而这些内容无法被打包到一个可执行jar中去。更加重要的是，不止java一种微服务，如果程序使用NodeJS、Golang编写，我们还要针对不同的语言环境做不同的打包。你可能想使用自动化手段完成这些软件的安装，将基础设施作为服务（IaaS），使用它们的API完成环境的搭建。事实上Netflix已经使用了自动化构建工具来完成VM的构建，并利用这项技术实现了不可变的递交，但是VM很难管理、更新和变更，而每个VM都有自己完备的虚拟化环境，对资源有些浪费。

&nbsp;&nbsp;&nbsp;&nbsp;那么有什么更加轻量化的打包和镜像化方式让我们使用吗？

## Docker，Docker，Docker

&nbsp;&nbsp;&nbsp;&nbsp;Docker是近几年出现用于解决不可变递交的优雅解决方案，它允许我们将应用以及应用的所有依赖（包括了：OS，JVM以及其他组件）打包成为一个轻量的、分层的镜像格式。然后Docker使用这些镜像，运行它们，产生实例，而这些实例都运行在Linux containers中，在Linux containers中，会带来CPU、内存、网络以及磁盘的隔离。在这种模式下，这些容器实例就是一种应用虚拟化的方式，它运行一个进程去执行，你甚至可以在实例中运行`ps`查看你的进程，而且这个容器实例具备访问CPU、内存、磁盘和网络的能力，但是它只能使用指定好的配额。例如：能够启动一个Docker容器，只为它分配一部分的CPU、内存以及I/O的访问限制。如果在Linux containers外部去看，在主机上，这个容器就是一个进程，不需要设备驱动的虚拟化、操作系统、网络栈以及特殊的中间层，它仅仅是一个进程。这意味着，我们可以在一台机器上部署尽可能多的容器，提供了比虚拟机更高的部署密度。

&nbsp;&nbsp;&nbsp;&nbsp;在这些激动人心的特性下，其实没有革命性的技术。Docker使用到的技术有：`cgroups`、`namespaces`以及`chroot`，这些都已经在Linux内核中运行了相当长的时间，而这些技术被Docker用来构造应用虚拟化技术。Linux containers已经推出了十几年，而进程虚拟化技术在`Solaris`和`FreeBSD`上出现的时间更早。以往使用这些技术的`lxc`会比较复杂，而Docker通过简单的API以及优秀的用户体验使得Linux containers的运用变得火热起来，Docker通过一个客户端命令工具能够与Linux containers进行交互，去部署Docker镜像，而Docker镜像的出现改变了我们打包和交付软件的方式。

&nbsp;&nbsp;&nbsp;&nbsp;一旦你拥有了镜像，可以迅速的转化为Linux containers，镜像是按照层进行构建的，一般会在一个基础的层（例如：RHEL、 Debian等）上进行构建，然后包含应用所需的内容，构建应用其实也就是在基础层上进行一层一层的镜像构建。镜像的出现，是的发布到各种环境变得容易，不会在面对一堆零散的内容，如果发现基础镜像中有问题，可以进行重新构建，其他镜像进行重新选择构建即可，这使得从开发环境到测试，再到生产环境减少了人工干预发布内容的环节，如果我们要新发布一版本，只需要重新构建一个镜像即可，而改动只是去修改了镜像中对应的层。

&nbsp;&nbsp;&nbsp;&nbsp;构建了镜像，但是我们怎样启动一个应用？怎样停止它？怎样做健康检查？怎样收集应用的日志、Metrics等信息，使用标准的API可以使我们自己构建工具来完成这些工作。出色的集群机制，例如服务发现、负载均衡、失败容错以及配置使得开发人员很容易获得这些特性。

> Docker相关的技术可以关注 [The Docker Book](https://www.gitbook.com/book/weipeng2k/the-docker-book/details)

## Kubernetes

&nbsp;&nbsp;&nbsp;&nbsp;外界都知晓Google使用Linux containers技术来支撑其扩展性，事实上Google的所有应用都运行在Linux containers上，并且被他们的管理系统[Brog](http://research.google.com/pubs/pub43438.html)进行着管理。前Google工程师`Joe Beda`说，公司每周要启动超过20亿次的容器，Google甚至投入资源涉及到linux底层技术来支持其容器在生产环境的运用。在2006年，Google开始了一个名叫 **进程容器** 的项目，最终演变成为了`cgroups`，而它在2008被合并到了Linux核心，同年正式发布。Google在拥有极强的运维容器的背景下，其对构建容器平台的影响力就不言而喻了，事实上，一些流行的容器管理项目都受到了Google的影响。

* Cloud Foundry<br>它的创立者`Derek Collison`和`Vadim Spivak`都在Google工作过，并且使用Borg系统很多年
* Apache Mesos<br>它的创立者`Ben Hindman`在Google实习过，与Google的诸多工程师有过容器技术的交流（围绕容器集群、调度和管理等技术）
* Kubernetes<br>开源的容器集群管理平台和社区，创建它的工程师，同时也在Google创建了Borg

&nbsp;&nbsp;&nbsp;&nbsp;在Docker震惊技术届的2013年，Google决定是时候开源他们下一代的技术--Borg，而它被命名为Kubernetes。今天，Kubernetes是一个巨大、开放和快速成长的社区，来自Google、Red Hat、CoreOS以及其他的个体在为它做出贡献。Kubernetes为在可伸缩的Linux containers下运行微服务提供了非常多有价值的功能，Google将近20年的运维经验都浓缩到了Kubernetes，这对我们使用微服务部署产生了巨大的影响。大部分高流量的互联网企业在这个领域耕耘了很长时间（Netflix、Amazon等）尝试构建的伸缩技术，在Kubernetes中都已经默认进行了集成，在正式深入例子之前，我们先介绍一些Kubernetes的概念，接下来在后面的章节将会用它来挂历一个微服务集群。

### Pods

&nbsp;&nbsp;&nbsp;&nbsp;一个Pod是一个或者多个Docker容器的组合，一般情况下一个Pod对应一个Docker容器，应用部署在其中。

&nbsp;&nbsp;&nbsp;&nbsp;Kubernetes进行编排、调度以及管理Pod，当我们谈到一个运行在Kubernetes中的应用时，指的是运行在Pod中的Docker容器。一个Pod有自己的IP地址，所有运行在这个Pod中的容器共享这个IP（这个不同于普通的Docker容器，普通的Docker容器每个实例都有一个IP），当一个卷挂载到Pod，这个卷也能够被Pod中的容器共同访问。

&nbsp;&nbsp;&nbsp;&nbsp;关于Pod需要注意的一点是：它们是短暂的，这代表着它们会在任何时候消失（不是因为服务崩溃就是集群cluster杀死了它），它们不像VM一样引起你的额外注意。Pods能够在任意时刻被销毁，而这种意外的失败就如同介绍微服务架构中任何事情都会失败一样（design for failure），我们强烈建议在编写微服务时时刻记着这个建议。和之前介绍的其他原则相比，这个建议显得更加重要。

> Kubernetes的最小部署单元是Pod而不是容器。作为First class API公民，Pods能被创建，调度和管理。简单地来说，像一个豌豆荚中的豌豆一样，一个Pod中的应用容器同享同一个上下文（比如：PID名字空间、网络等）。在实际使用时，我们一般不直接创建Pods, 我们通过replication controller来负责Pods的创建，复制，监控和销毁。一个Pod可以包括多个容器，他们直接往往相互协作完成一个应用功能。

### 标签（Label）

&nbsp;&nbsp;&nbsp;&nbsp;标签（Label）是一个能分配给Pods的简单键值对，比如：`release=stable`或者`tier=backend`，Pods（或者其他资源，但是我们当前只关注Pods）可以拥有多个标签并且可以以松耦合的方式进行分组，这在Kubernetes的使用过程中非常常见。因此一点也不奇怪，Google使用这种简单的方式用来区分不同的容器，并以此来构建大规模伸缩的集群。当我们用标签区分了Pods之后，我们可以使用 **label selector** 来按照分组来查询所有的Pods，例如：如果我们有一些Pods打上了`tier=backend`的标签，而其他的一些打上了`tier=frontend`标签，只需要使用 **label selector** 表达式 `tier != frontend`就可以完成对所有没有打上`tier=frontend`的Pods进行查询，而 **label selector** 在接下来介绍的 **replication controllers** 和 **services** 所使用。

### 复制控制器（Replication Controllers）

&nbsp;&nbsp;&nbsp;&nbsp;当我们讨论微服务的可伸缩性时，可能想的是将给定的一组微服务部署到多个实例（机器）上，用多个实例的部署来增加伸缩性。Kubernetes为伸缩性定义了一个叫做 **Replication Controllers** 的概念，它能够管理给定的一组微服务的多个复制体（replicas)，例如：我们需要管理许多打上 `tier=backend and release=stable` 的需要Pods，可以创建一个复制控制器，该控制器拥有对应的 **label selector** ，此时它就能够在集群中以replicas的形式控制和管理这些Pods。如果我们设置replica的数量为10，当Kubernetes会确定当前的复制控制器是否达到了该状态，如果此刻只有5个，那么Kubernetes就会循环创建剩余的5个，当有20个运行着，Kubernetes将会选择停止10个。Kubernetes将会尽可能的保持设定的10个replica的状态，你可以认为使用复制控制器来控制集群的数量是非常容易的事情，在接下来的章节中，我们会看到使用复制控制器的例子。

### 服务（Services）

&nbsp;&nbsp;&nbsp;&nbsp;我们最后需要理解的Kubernetes概念是服务（Service）， **Replication Controllers** 能控制一个服务下的多个复制体（replicas），我们也观察到Pods能够被停止（要么自己crash、或者被kill，也有可能被复制控制器停止），因此，当我们尝试与一组Pods进行通信时，不应该依赖于具体的IP（每个Pod都有自己的IP），我们需要的是能够以组的形式访问这些Pods的方式，以组的形式发现它们，可能的话能够以负载均衡的方式访问它们，这个就是 **服务（Service）** 需要做的。它（服务）允许我们通过一个 **label selector** 获取一组Pods，将它们抽象为一个虚拟IP，然后以这个虚拟IP来让我们对这些Pods进行发现和交互，我们将在接下来的章节中介绍具体的例子。

> Service是定义一系列Pod以及访问这些Pod的策略的一层抽象。Service通过Label找到Pod组。因为Service是抽象的，所以在图表里通常看不到它们的存在

&nbsp;&nbsp;&nbsp;&nbsp;了解这些简单的概念，Pods、Labels、Replication Controllers和services，我们能够以可伸缩的模式，用Google的实践，来管理微服务。这些实践花费了多年，经历了多次失败总结出来的经验之谈，而这个模式能够解决复杂的问题，因此强烈建议学习这些概念以及实践，使用Kubernetes来管理你的微服务。

## 开始使用Kubernetes

&nbsp;&nbsp;&nbsp;&nbsp;Docker和Kubernetes都是基于Linux本地技术的产品，因此它们需要运行在一个基于Linux的环境中，我们假设大部分的Java开发人员都是工作在Windows或者Mac下，我们推荐在Linux环境下进行相关的实践。

&nbsp;&nbsp;&nbsp;&nbsp;**接下来的内容，作者作为redhat的员工，开始介绍CDK（RedHat Container Development Kit），然后是CDK的安装，译者觉得CDK没有多大的参考性，因此将其替换成了对Kubernetes官方的MiniKube使用，并基于MiniKube在linux机器上搭建Kubernetes。**

### Kubernetes之MiniKube的安装

> 笔者准备了aliyun oss 下载，比googleapis快许多

> 该文档介绍如何运行起一个本地Kubernetes集群，需要一个支持Hyper-V虚拟化的CPU以及至少8GB CPU

> 笔者的环境是 ubuntu 16.04 / amd k8 4 core CPU / 16 gb mem

### 安装MiniKube

```sh
wget http://029145.oss-cn-hangzhou.aliyuncs.com/minikube-linux-amd64
mv minikube-linux-amd64 minikube
chmod u+x minikube
sudo mv minikube /usr/local/bin/
```

### 安装Kubectl

```sh
wget http://029145.oss-cn-hangzhou.aliyuncs.com/kubectl
chmod u+x kubectl
sudo mv kubectl /usr/local/bin/
```

### 启动MiniKube

&nbsp;&nbsp;&nbsp;&nbsp;通过以下命令启动minikube，该过程会下载一个ISO镜像，然后完成启动。

```sh
minikube start
```

### 下载依赖的镜像

&nbsp;&nbsp;&nbsp;&nbsp;这个过程最为复杂，当启动minikube时，会自动下载一些镜像，但是这些镜像都被墙了，但是我们可以从aliyun的仓库下载对应的镜像，然后将其重命名。在启动完minikube后，使用`minikube ssh`可以登录到后台，然后运行下面的命令完成镜像的下载和别名设置。

```sh
docker pull registry.cn-hangzhou.aliyuncs.com/google-containers/pause-amd64:3.0
docker tag registry.cn-hangzhou.aliyuncs.com/google-containers/pause-amd64:3.0 gcr.io/google_containers/pause-amd64:3.0

docker pull registry.cn-hangzhou.aliyuncs.com/google-containers/kube-addon-manager-amd64:v6.1
docker tag registry.cn-hangzhou.aliyuncs.com/google-containers/kube-addon-manager-amd64:v6.1 gcr.io/google-containers/kube-addon-manager:v6.1

docker pull registry.cn-hangzhou.aliyuncs.com/google-containers/kubedns-amd64:1.9
docker tag registry.cn-hangzhou.aliyuncs.com/google-containers/kubedns-amd64:1.9 gcr.io/google_containers/kubedns-amd64:1.9

docker pull registry.cn-hangzhou.aliyuncs.com/google-containers/kube-dnsmasq-amd64:1.4
docker tag registry.cn-hangzhou.aliyuncs.com/google-containers/kube-dnsmasq-amd64:1.4 gcr.io/google_containers/kube-dnsmasq-amd64:1.4

docker pull registry.cn-hangzhou.aliyuncs.com/google-containers/exechealthz-amd64:1.2
docker tag registry.cn-hangzhou.aliyuncs.com/google-containers/exechealthz-amd64:1.2 gcr.io/google_containers/exechealthz-amd64:1.2

docker pull registry.cn-hangzhou.aliyuncs.com/google-containers/kubernetes-dashboard-amd64:v1.5.0
docker tag registry.cn-hangzhou.aliyuncs.com/google-containers/kubernetes-dashboard-amd64:v1.5.0 gcr.io/google_containers/kubernetes-dashboard-amd64:v1.5.1
```

### 测试echoserver

&nbsp;&nbsp;&nbsp;&nbsp;运行命令创建一个echoserver服务，运行如下命令：

```sh
kubectl run hello-minikube --image=registry.cn-hangzhou.aliyuncs.com/google-containers/echoserver:1.4 --port=8080
kubectl expose deployment hello-minikube
```

&nbsp;&nbsp;&nbsp;&nbsp;然后运行`minikube service hello-minikube --url`，将会返回`hello-minikube`的url，然后可以基于该url做一下测试。

```sh
$ minikube service hello-minikube --url
http://192.168.99.100:31907
$ curl http://192.168.99.100:31907/123
CLIENT VALUES:
client_address=172.17.0.1
command=GET
real path=/123
query=nil
request_version=1.1
request_uri=http://192.168.99.100:8080/123

SERVER VALUES:
server_version=nginx: 1.10.0 - lua: 10001

HEADERS RECEIVED:
accept=*/*
host=192.168.99.100:31907
user-agent=curl/7.47.0
BODY:
-no body in request-
```

&nbsp;&nbsp;&nbsp;&nbsp;可以看到请求对应的url，有数据返回，当然也可以启动dashboard，比如运行`minikube dashboard`将会打开管理页面。

## 小结

&nbsp;&nbsp;&nbsp;&nbsp;在本章我们学习了微服务在部署和管理上的问题，以及如何使用Linux容器来解决这些问题，使用不可变的递交来减少部署过程中遇到的问题，使重复部署成为可能。我们能够使用Linux容器做到服务之间的隔离、快速的部署以及迁移，使用Kubernetes来进行容器的管理，并享受由Kubernetes带来的服务发现、故障转移、健康检查等内置功能，Kubernetes已经解决了许多部署相关的问题，如果想深入了解，可以参考以下链接：

* [Kubernetes Reference Documentation](https://kubernetes.io/docs/concepts/services-networking/service/)
* [“An Introduction to Immutable Infrastructure” by Josha Stella](https://www.oreilly.com/ideas/an-introduction-to-immutable-infrastructure)
* [Kubernetes](https://kubernetes.io)
* [Kubernetes Reference Documentation: Pods](https://kubernetes.io/docs/concepts/workloads/pods/pod/)
* [Kubernetes Reference Documentation: Labels and Selectors](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/)
* [Kubernetes Reference Documentation: Replication Controller](https://kubernetes.io/docs/concepts/workloads/controllers/replicationcontroller/)
* [Kubernetes Reference Documentation: Services](https://kubernetes.io/docs/concepts/services-networking/service/)
