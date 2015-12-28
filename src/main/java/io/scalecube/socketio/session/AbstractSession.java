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
    heartbeatScheduler.reschedule();
    State previousState = setState(State.CONNECTED);
    boolean initialConnect = previousState == State.CONNECTING;
    if (initialConnect) {
      sendPacketToChannel(channel, connectPacket);
    }
    return initialConnect;
  }

  @Override
  public void disconnect(final Channel channel) {
    if (getState() == State.DISCONNECTED) {
      return;
    }

    setState(State.DISCONNECTING);
    heartbeatScheduler.disableHeartbeat();
    if (!isUpgraded()) {
      sendPacket(disconnectPacket);
      disconnectHandler.onSessionDisconnect(this);
    }
    setState(State.DISCONNECTED);
  }

  @Override
  public void sendHeartbeat() {
    sendPacket(heartbeatPacket);
  }

  @Override
  public void send(final ByteBuf message) {
    Packet messagePacket = new Packet(PacketType.MESSAGE);
    messagePacket.setData(message);
    sendPacket(messagePacket);
  }

  protected void sendPacketToChannel(final Channel channel, IPacket packet) {
    fillPacketHeaders(packet);
    channel.writeAndFlush(packet);
  }

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

  protected void fillPacketHeaders(IPacket packet) {
    packet.setOrigin(getOrigin());
    packet.setSessionId(getSessionId());
    packet.setTransportType(getTransportType());
  }

  protected State setState(final State state) {
    State previousState = stateHolder.getAndSet(state);
    if (previousState != state && log.isDebugEnabled()) {
      log.debug("Session {} state changed from {} to {}", getSessionId(), previousState, state);
    }
    return previousState;
  }

}
