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

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.channel.Channel;
import io.scalecube.socketio.TransportType;
import io.scalecube.socketio.packets.Packet;
import io.scalecube.socketio.packets.PacketType;
import io.scalecube.socketio.packets.PacketsFrame;

// 虚拟轮询的方法
public abstract class AbstractPollingSession extends AbstractSession {

  // 声明一个ACK包 
  private final Packet ackPacket = new Packet(PacketType.ACK);
  // 声明一个轮询队列
  private final PollingQueue messagesQueue = new PollingQueue();
  // 就是对对象进行原子操作，保证了线程之间channel是可见的
  private final AtomicReference<Channel> outChannelHolder = new AtomicReference<Channel>();

  public AbstractPollingSession(final Channel channel, final String sessionId, final String origin,
                                final SessionDisconnectHandler disconnectHandler, final TransportType upgradedFromTransportType, final int localPort,
                                final SocketAddress remoteAddress) {
    super(channel, sessionId, origin, disconnectHandler, upgradedFromTransportType, localPort, remoteAddress);
  }

  // 又一次的重写父类
  @Override
  public boolean connect(Channel channel) {
    // 连接一个通道，返回连接的结果
    boolean initialConnect = super.connect(channel);
    // 如果连接失败
    if (!initialConnect) {
      // 进行问题处理，再一次做断开，刷新通道的操作
      bindChannel(channel);
    }
    // 方法连接的结果
    return initialConnect;
  }

  private void bindChannel(final Channel channel) {
    if (getState() == State.DISCONNECTING) {
      disconnect(channel);
    } else {
      flush(channel);
    }
  }

  // 刷新操作
  private void flush(final Channel channel) {
    // 进行同步操作
    synchronized (messagesQueue) {
      if (messagesQueue.isEmpty()) {
        outChannelHolder.set(channel);
      } else {
        PacketsFrame packetsFrame = messagesQueue.takeAll();
        sendPacketToChannel(channel, packetsFrame);
      }
    }
  }

  // 重写发送包命令
  @Override
  public void sendPacket(final Packet packet) {
    // 如果包是空的，那么抛出一个参数异常
    if (packet == null) {
      throw new IllegalArgumentException("Packet is null");
    }

    Channel channel = outChannelHolder.getAndSet(null);
    // 判断此时通道不为空，而且通道是可用的
    if (channel != null && channel.isActive()) {
      sendPacketToChannel(channel, packet);
    } else {
      synchronized (messagesQueue) {
        messagesQueue.add(packet);
      }
    }
  }


  // 重写断开连接操作
  @Override
  public void disconnect() {
    if (getState() == State.DISCONNECTED) {
      return;
    }
    if (getState() != State.DISCONNECTING) {
      setState(State.DISCONNECTING);

      // Check if there is active polling channel and disconnect
      // otherwise schedule forced disconnect
      // 这句话是啥
      Channel channel = outChannelHolder.getAndSet(null);
      if (channel != null && channel.isActive()) {
        disconnect(channel);
      } else {
        heartbeatScheduler.scheduleDisconnect();
      }
    } else {
      //forced disconnect
      disconnect(null);
    }
  }

  @Override
  public void acceptPacket(final Channel channel, final Packet packet) {
    if (packet.getSequenceNumber() == 0) {
      sendPacketToChannel(channel, ackPacket);
    }
  }

}
