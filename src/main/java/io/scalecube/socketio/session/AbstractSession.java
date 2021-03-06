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
package io.scalecube.socketio.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.scalecube.socketio.TransportType;
import io.scalecube.socketio.packets.IPacket;
import io.scalecube.socketio.packets.Packet;
import io.scalecube.socketio.packets.PacketType;

public abstract class AbstractSession implements ManagedSession {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private final String sessionId;
  private final String origin;
  private final SocketAddress remoteAddress;
  private final TransportType upgradedFromTransportType;
  private final int localPort;

  private final Packet connectPacket = new Packet(PacketType.CONNECT);
  private final Packet disconnectPacket = new Packet(PacketType.DISCONNECT);
  private final Packet heartbeatPacket = new Packet(PacketType.HEARTBEAT);

  protected final SessionDisconnectHandler disconnectHandler;
  protected final SocketIOHeartbeatScheduler heartbeatScheduler;

  private final AtomicReference<State> stateHolder = new AtomicReference<State>(State.CREATED);
  private volatile boolean upgraded = false;

  public AbstractSession(
      final Channel channel,  // 1,连接的通道，也就是我们之前用的socket是一样的道理，我们就是向这个通道发送消息
      final String sessionId, // 2，我们规定每一个会话，有他的名称，这样方便我们进行管理
      final String origin,    // 3，源
      final SessionDisconnectHandler disconnectHandler,  //4，。。。
      final TransportType upgradedFromTransportType, // 5，
      final int localPort,    // 6，本地端口号
      final SocketAddress remoteAddress) {  // 7，远程地址
    this.sessionId = sessionId;
    this.remoteAddress = remoteAddress == null ? channel.remoteAddress() : remoteAddress;
    this.origin = origin;
    this.localPort = localPort;
    this.disconnectHandler = disconnectHandler;
    this.upgradedFromTransportType = upgradedFromTransportType;
    heartbeatScheduler = new SocketIOHeartbeatScheduler(this);
    setState(State.CONNECTING); // 设置状态为连接
  }

  // 这是重写了，最顶层的那个内的方法
  @Override
  public final String getSessionId() {
    return sessionId;
  }
  // 这个标志我们后面再了解
  @Override
  public final boolean isUpgradedSession() {
    return upgradedFromTransportType != null;
  }

  @Override
  public TransportType getUpgradedFromTransportType() {
    return upgradedFromTransportType;
  }

  @Override
  public final SocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  @Override
  public final String getOrigin() {
    return origin;
  }

  @Override
  public int getLocalPort() {
    return localPort;
  }

  protected boolean isUpgraded() {
    return upgraded;
  }

  @Override
  public State getState() {
    return stateHolder.get();
  }

  // 实现连接这个方法
  @Override
  public boolean connect(final Channel channel) {
    // 启动心跳包调度器，重新？调度
    heartbeatScheduler.reschedule();
    // 将过去的状态设置为连接
    State previousState = setState(State.CONNECTED);
    boolean initialConnect = previousState == State.CONNECTING;
    // 如果连接成功，那么就就发送连接包到这个通道中
    if (initialConnect) {
      sendPacketToChannel(channel, connectPacket);
    }
    // 返回连接的状态
    return initialConnect;
  }

  // 关闭连接的具体方法
  @Override
  public void disconnect(final Channel channel) {
    // 判断如果状态是已经断开了的，那么就直接退出
    if (getState() == State.DISCONNECTED) {
      return;
    }
    // 1，设置状态为断开连接状态
    setState(State.DISCONNECTING);
    // 2，关闭心跳包发送
    heartbeatScheduler.disableHeartbeat();
    // 如果没有。。。
    if (!isUpgraded()) {
      // 发送一个断开连接包
      sendPacket(disconnectPacket);
      // 调用断开连接引擎，断开这个通道连接
      disconnectHandler.onSessionDisconnect(this);
    }
    // 再一次，设置状态为断开连接
    setState(State.DISCONNECTED);
  }

  // 实现发送心跳包方法
  @Override
  public void sendHeartbeat() {
    sendPacket(heartbeatPacket);
  }

  // 实现发送字节流消息
  @Override
  public void send(final ByteBuf message) {
    // 新建一个包类型
    Packet messagePacket = new Packet(PacketType.MESSAGE);
    // 将消息加包
    messagePacket.setData(message);
    // 把消息给发送出去
    sendPacket(messagePacket);
  }

  // 这里有一个内部方法：向通道里发送包
  protected void sendPacketToChannel(final Channel channel, IPacket packet) {
    // 填包
    fillPacketHeaders(packet);
    // 这个方法和熟悉，写入通道并且刷新缓冲区
    channel.writeAndFlush(packet);
  }

  // 这个是空方法
  @Override
  public void acceptPacket(final Channel channel, final Packet packet) {
  }

  public void markAsUpgraded() {
    upgraded = true;
  }

  @Override
  public void acceptHeartbeat() {
    heartbeatScheduler.reschedule();
  }

  // 填包操作
  protected void fillPacketHeaders(IPacket packet) {
    packet.setOrigin(getOrigin());
    packet.setSessionId(getSessionId());
    packet.setTransportType(getTransportType());
  }

  protected State setState(final State state) {
    State previousState = stateHolder.getAndSet(state);
    if (previousState != state && log.isDebugEnabled()) {
      // 每一次的状态改变，在都会在日志中显示出来
      log.debug("Session {} state changed from {} to {}", getSessionId(), previousState, state);
    }
    return previousState;
  }

}
