# 番外篇：使用MicroK8s

&nbsp;&nbsp;&nbsp;&nbsp;在单节点的`Kubernetes`搭建过程中，一般会采用`MiniKube`，可是`MiniKube`会要求宿主机安装虚拟机，在其上完成部署，这个过程会比较麻烦，有没有一种方式，能够提供一种不依赖虚拟机，但是能和宿主机相对隔离的方案来搭建单节点的`Kubernetes`呢？答案是有的，**Canonical** 提供了`MicroK8s`，可以用它来完成不依赖虚拟机的搭建，同时它提供了非常好的安装体验。

&nbsp;&nbsp;&nbsp;&nbsp;`MicroK8s`的描述是 *“Zero-ops Kubernetes for workstations and edge / IoT
A single package of k8s for 42 flavours of Linux. Made for developers, and great for edge, IoT and appliances.”* ，可以看到这是一个目标在单节点的`Kubernetes`部署方案，同时它也通过了`Kubernetes`认证。 

> `MicroK8s`也可以通过`add node`组成多节点模式，但是它主要是用来完成单节点原型搭建，而非生产环境的构建。
> 
> 网上也有一些安装教程，但大部分都是摘抄，很多都跑不通。

## 工具安装

&nbsp;&nbsp;&nbsp;&nbsp;工具安装的环境是`Ubuntu 18.04 LTS`，接下来开始安装`MicorK8s`。

&nbsp;&nbsp;&nbsp;&nbsp;先安装`MicroK8s`本体。

```sh
// 安装`MicroK8s`
sudo snap install microk8s --classic

// 查看一下版本，当前1.18已经发布，目前笔者使用的是1.17.3
snap info microk8s

// 用户组
sudo usermod -a -G microk8s $USER

// 防火墙设置
sudo ufw allow in on cni0 && sudo ufw allow out on cni0
sudo ufw default allow routed

// 启动相关的add-on
microk8s.enable dashboard dns
```

&nbsp;&nbsp;&nbsp;&nbsp;运行完这些命令，如果在国内，不出意外，是无法启动的，如果运行成功是如下输出：

```sh
$  microk8s.status
microk8s is running
addons:
cilium: disabled
dashboard: enabled
dns: enabled
fluentd: disabled
gpu: disabled
helm3: disabled
helm: disabled
ingress: disabled
istio: disabled
jaeger: disabled
juju: disabled
knative: disabled
kubeflow: disabled
linkerd: disabled
metallb: disabled
metrics-server: disabled
prometheus: disabled
rbac: disabled
registry: disabled
storage: disabled
```

&nbsp;&nbsp;&nbsp;&nbsp;原因还是gcr镜像无法下载，这里需要先调整一下docker的配置。

```sh
sudo vi /etc/docker/daemon.json
```

> 前提是安装了`Docker`

&nbsp;&nbsp;&nbsp;&nbsp;对应的配置下，增加`insecure-registries`。

```json
{
  "registry-mirrors": ["https://heli2ujx.mirror.aliyuncs.com"],
  "insecure-registries" : ["localhost:32000"]
}
```

&nbsp;&nbsp;&nbsp;&nbsp;无法启动的原因是一些关键的镜像无法下载，在墙那边，所以需要下载后，方可启动。但是其中的`POD`无法正常启动的原因需要能够查清楚，`MicroK8s`提供的命令可以帮助查看对应的`POD`处于的状态。

> 其实也就是describe命令。
>
> 可以使用`microk8s.kubectl get all --all-namespaces`来查看所有的`POD`和`SERVICE`等资源运行情况。

&nbsp;&nbsp;&nbsp;&nbsp;使用如下命令，可以查看对应的`POD`相关情况。

```sh
microk8s.kubectl describe pod ${pod name}  -n kube-system
```

&nbsp;&nbsp;&nbsp;&nbsp;例如：`microk8s.kubectl describe pod monitoring-influxdb-grafana-v4-6d599df6bf-28m97  -n kube-system`，如果这个`POD`运行存在问题，会将问题显示出来，可以跟进排查。

## 镜像下载

&nbsp;&nbsp;&nbsp;&nbsp;对于 *1.17.3* 版本的`MicroK8s`，可以使用如下脚本将镜像从aliyun搬到本地，然后打上对应的tag，这样镜像就在本地存在了。

```sh
$ more fetch-image.sh 
#!/bin/bash
images=(
k8s.gcr.io/pause:3.1=gcr.azk8s.cn/google-containers/pause:3.1
gcr.io/google_containers/defaultbackend-amd64:1.4=gcr.azk8s.cn/google-containers/defaultbackend-amd64:1.4
k8s.gcr.io/kubernetes-dashboard-amd64:v1.10.1=registry.cn-hangzhou.aliyuncs.com/google_containers/kubernetes-dashboard-amd64:v1.10.1
k8s.gcr.io/heapster-influxdb-amd64:v1.3.3=registry.cn-hangzhou.aliyuncs.com/google_containers/heapster-influxdb-amd64:v1.3.3
k8s.gcr.io/heapster-amd64:v1.5.2=registry.cn-hangzhou.aliyuncs.com/google_containers/heapster-amd64:v1.5.2
k8s.gcr.io/heapster-grafana-amd64:v4.4.3=registry.cn-hangzhou.aliyuncs.com/google_containers/heapster-grafana-amd64:v4.4.3
)

OIFS=$IFS; # 保存旧值

for image in ${images[@]};do
    IFS='='
    set $image
    docker pull $2
    docker tag  $2 $1
    docker rmi  $2
    docker save $1 > 1.tar && microk8s.ctr --namespace k8s.io image import 1.tar && rm 1.tar
    IFS=$OIFS; # 还原旧值
done
```

&nbsp;&nbsp;&nbsp;&nbsp;`sudo ./fetch-image.sh`执行后，这些镜像被下载到本地。然后运行` microk8s.stop && microk8s.start`，等待成功。

## 运行服务

&nbsp;&nbsp;&nbsp;&nbsp;运行命令创建一个echoserver服务，运行如下命令：

```sh
microk8s.kubectl run hello-minikube --image=registry.cn-hangzhou.aliyuncs.com/google-containers/echoserver:1.4 --port=8080
microk8s.kubectl expose deployment hello-minikube
```

&nbsp;&nbsp;&nbsp;&nbsp;下载完镜像，启动了服务，可以看一下当前的部署情况：

```sh
$ microk8s.kubectl get all --all-namespaces
NAMESPACE     NAME                                                  READY   STATUS    RESTARTS   AGE
default       pod/hello-minikube-7dfbb66787-j54tw                   1/1     Running   0          4m59s
kube-system   pod/coredns-7b67f9f8c-4bm8b                           1/1     Running   2          6d4h
kube-system   pod/dashboard-metrics-scraper-687667bb6c-lvf85        1/1     Running   2          6d4h
kube-system   pod/heapster-v1.5.2-5c58f64f8b-wzm6g                  4/4     Running   8          6d4h
kube-system   pod/kubernetes-dashboard-5c848cc544-kq8c7             1/1     Running   2          6d4h
kube-system   pod/monitoring-influxdb-grafana-v4-6d599df6bf-28m97   2/2     Running   4          6d4h

NAMESPACE     NAME                                TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                  AGE
default       service/hello-minikube              ClusterIP   10.152.183.119   <none>        8080/TCP                 4m8s
default       service/kubernetes                  ClusterIP   10.152.183.1     <none>        443/TCP                  6d5h
kube-system   service/dashboard-metrics-scraper   ClusterIP   10.152.183.120   <none>        8000/TCP                 6d4h
kube-system   service/heapster                    ClusterIP   10.152.183.135   <none>        80/TCP                   6d4h
kube-system   service/kube-dns                    ClusterIP   10.152.183.10    <none>        53/UDP,53/TCP,9153/TCP   6d4h
kube-system   service/kubernetes-dashboard        NodePort    10.152.183.251   <none>        443:32100/TCP            6d4h
kube-system   service/monitoring-grafana          ClusterIP   10.152.183.84    <none>        80/TCP                   6d4h
kube-system   service/monitoring-influxdb         ClusterIP   10.152.183.183   <none>        8083/TCP,8086/TCP        6d4h

NAMESPACE     NAME                                             READY   UP-TO-DATE   AVAILABLE   AGE
default       deployment.apps/hello-minikube                   1/1     1            1           4m59s
kube-system   deployment.apps/coredns                          1/1     1            1           6d4h
kube-system   deployment.apps/dashboard-metrics-scraper        1/1     1            1           6d4h
kube-system   deployment.apps/heapster-v1.5.2                  1/1     1            1           6d4h
kube-system   deployment.apps/kubernetes-dashboard             1/1     1            1           6d4h
kube-system   deployment.apps/monitoring-influxdb-grafana-v4   1/1     1            1           6d4h

NAMESPACE     NAME                                                        DESIRED   CURRENT   READY   AGE
default       replicaset.apps/hello-minikube-7dfbb66787                   1         1         1       4m59s
kube-system   replicaset.apps/coredns-7b67f9f8c                           1         1         1       6d4h
kube-system   replicaset.apps/dashboard-metrics-scraper-687667bb6c        1         1         1       6d4h
kube-system   replicaset.apps/heapster-v1.5.2-5c58f64f8b                  1         1         1       6d4h
kube-system   replicaset.apps/kubernetes-dashboard-5c848cc544             1         1         1       6d4h
kube-system   replicaset.apps/monitoring-influxdb-grafana-v4-6d599df6bf   1         1         1       6d4h

```

&nbsp;&nbsp;&nbsp;&nbsp;接下来请求本地，然后看一下输出：

```sh
$ curl http://10.152.183.119:8080/123
CLIENT VALUES:
client_address=10.1.17.1
command=GET
real path=/123
query=nil
request_version=1.1
request_uri=http://10.152.183.119:8080/123

SERVER VALUES:
server_version=nginx: 1.10.0 - lua: 10001

HEADERS RECEIVED:
accept=*/*
host=10.152.183.119:8080
user-agent=curl/7.58.0
BODY:
-no body in request-
```