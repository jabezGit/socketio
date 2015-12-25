/**
 * Copyright 2012 Ronen Hamias, Anton Kharenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.scalecube.socketio;

//
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.HashedWheelTimer;
import io.scalecube.socketio.pipeline.SocketIOChannelInitializer;
import io.scalecube.socketio.session.SocketIOHeartbeatScheduler;

/**
 * A Socket.IO server launcher class.
 *
 * @author Anton Kharenko
 */
public class SocketIOServer {

  // 这个jar包几乎成了开发服务器的标准了
  private final Logger log = LoggerFactory.getLogger(getClass());

  private enum State {
    STARTED, STOPPED
  }
  
  // 设置配置文件
  private ServerConfiguration configuration;
  
  private SocketIOListener listener;

  private HashedWheelTimer timer;

  private volatile State state = State.STOPPED;

  private ServerBootstrapFactory serverBootstrapFactory;

  private ServerBootstrap bootstrap;

  private SocketIOServer(ServerConfiguration configuration) {
    // 只需要传入一个配置文件即可
    this.configuration = configuration;
  }

  /**
   * 使用默认的配置创建socket.IO 实例
   * Creates instance of Socket.IO server with default settings.
   */
  public static SocketIOServer newInstance() {
    return new SocketIOServer(ServerConfiguration.DEFAULT);
  }

  /**
   * 通过传入的端口值来创建Socket.io实例
   * Creates instance of Socket.IO server with the given port.
   */
  public static SocketIOServer newInstance(int port) {
    return new SocketIOServer(ServerConfiguration.builder().port(port).build());
  }

  /**
   * 通过安全端口创建实例，这也就是. ssl 的作用
   * Creates instance of Socket.IO server with the given secure port.
   */
  public static SocketIOServer newInstance(int port, SSLContext sslContext) {
    return new SocketIOServer(ServerConfiguration.builder()
        .port(port)
        .sslContext(sslContext)
        .build());
  }

  /**
   * 通过传入的配置文件来创建Socket.io 实例
   * Creates instance of Socket.IO server with the given configuration.
   */
  public static SocketIOServer newInstance(ServerConfiguration config) {
    return new SocketIOServer(config);
  }

  /**
   * 通过目前的配置文件来启动 socket 服务器
   * Starts Socket.IO server with current configuration settings.
   *
   *  抛出一个异常如果服务器已经启动了
   * @throws IllegalStateException
   *             if server already started
   */
  public synchronized void start() {
    if (isStarted()) {
      throw new IllegalStateException("Failed to start Socket.IO server: server already started");
    }
    // 使用log4j插件来显示现在服务器已经启动了
    log.info("Socket.IO server starting");

    // Configure heartbeat scheduler
    // 配置心跳调度器
    timer = new HashedWheelTimer();
    // 启动时间定时器
    timer.start();
    SocketIOHeartbeatScheduler.setHashedWheelTimer(timer);
    SocketIOHeartbeatScheduler.setHeartbeatInterval(configuration.getHeartbeatInterval());
    SocketIOHeartbeatScheduler.setHeartbeatTimeout(configuration.getHeartbeatTimeout());

    // Configure server
    // 配置服务器
    SocketIOChannelInitializer channelInitializer = new SocketIOChannelInitializer(configuration, listener);
    // 判断现在的bootsrap 是否就绪状态
    bootstrap = serverBootstrapFactory != null ? serverBootstrapFactory.createServerBootstrap() :
        createDefaultServerBootstrap();
    // 启动服务器
    bootstrap.childHandler(channelInitializer);
    
    // 绑定端口
    int port = configuration.getPort();
    bootstrap.bind(new InetSocketAddress(port));
    // 将状态机的状态设成启动状态
    state = State.STARTED;
    // 写入日志文件。服务器目前的状态为就绪态
    log.info("Started {}", this);
  }

  private ServerBootstrap createDefaultServerBootstrap() {
    return new ServerBootstrap()
        .group(new NioEventLoopGroup(), new NioEventLoopGroup())
        .channel(NioServerSocketChannel.class)
        .childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
  }

  /**
   * 停止Socket服务
   * Stops Socket.IO server.
   * 如果服务器已经关闭了，那么将抛出一个服务器已经关闭了的异常
   * @throws IllegalStateException
   *             if server already stopped
   */
  public synchronized void stop() {
    if (isStopped()) {
      throw new IllegalStateException("Failed to stop Socket.IO server: server already stopped");
    }
    // 将这条信息写入到日志系统中
    log.info("Socket.IO server stopping");
    // 定时器终止
    timer.stop();
    // 将组关闭，这个过程中不允许中断
    bootstrap.group().shutdownGracefully().syncUninterruptibly();
    // 改变状态机的状态
    state = State.STOPPED;
    
    log.info("Socket.IO server stopped");
  }

  /**
   * Restarts Socket.IO server. If server already started it stops server;
   * otherwise it just starts server.
   */
  public synchronized void restart() {
    if (isStarted()) {
      stop();
    }
    start();
  }

  /**
   * Returns if server is in started state or not.
   */
  public boolean isStarted() {
    // 通过查询状态机来显示目前服务器运行的状态
    return state == State.STARTED;
  }

  /**
   * Returns if server is in stopped state or not.
   */
  public boolean isStopped() {
    return state == State.STOPPED;
  }

  /** 
   * 获得这个IO时间的监听者
   * Socket.IO events listener.
   */
  public SocketIOListener getListener() {
    return listener;
  }

  /**
   * Sets Socket.IO events listener. If server already started new listener will be applied only after
   * server restart.
   */
  public void setListener(SocketIOListener listener) {
    this.listener = listener;
  }

  /**
   * Returns server configuration settings.
   */
  public ServerConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Sets server configuration settings. If server already started new settings will be applied only after
   * server restart.
   */
  public void setConfiguration(ServerConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * 需要了解这个工厂类的作用
   * Returns ServerBootstrap factory.
   */
  public ServerBootstrapFactory getServerBootstrapFactory() {
    return serverBootstrapFactory;
  }

  /**
   * Sets ServerBootstrap factory. If server already started new boostrap factory will be applied only after
   * server restart.
   */
  public void setServerBootstrapFactory(ServerBootstrapFactory serverBootstrapFactory) {
    this.serverBootstrapFactory = serverBootstrapFactory;
  }

  // 重构toString 方法
  @Override
  public String toString() {
    return "SocketIOServer{" +
        "configuration=" + configuration +
        ", state=" + state +
        '}';
  }
}
