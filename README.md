# websocket广播功能
CF MES WebSocket

### 功能说明

该项目最主要的功能在于，能够替换之前[SocketCluster](https://www.socketcluster.io/)的功能，但因为SocketCluster是基于Nodejs开发的，与SpringBoot的兼容不是很好，所以现在使用[Netty](https://netty.io/)开发WebSocket功能，方便前端与后端进行Socket集成。

项目包含两部分功能
* 与前端React`建立Socket连接`，前端能够通过[PubSubJS](https://github.com/mroderick/PubSubJS)`订阅Socket的消息`。
* 预先建立接口，方便Backend调用该接口，通过该项目以广播的形式`将消息发送到各个客户端`。



接口标准

`POST`发送单个[http://localhost:8081/send/single?uid=SampleChannel&data=SampleData](http://localhost:8081/send/single?uid=SampleChannel&data=SampleData)
其中SampleChannel代表监控的频道，SampleData代表发送的数据，`后期会将数据存放在Body中`，方便集成。


调用关系如下：

UI   <========>    WebSocket服务器   <=========>   Backend调用方法


---


### Docker
编译镜像
```shell script
docker build -t weschen/cf-mes-websocket:20210624.1 .
```
保存镜像
```shell script
docker save -o cf-mes-websocket-new-1 weschen/cf-mes-websocket:20210624.1
```
运行镜像
```shell script
docker run
```
