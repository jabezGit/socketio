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

import io.netty.channel.Channel;
import io.scalecube.socketio.Session;
import io.scalecube.socketio.packets.Packet;

/**
 * 这个接口丰富控制要求，内部管理会话，这个接口不支持用户作为库调用
 * This interface enrich ISession interface with control methods required to manage 
 * session internally. This interface is not supposed to be used by users of the library 
 * and represents internal details session implementation and subject to change
 * in future versions.  
 *
 * @author Anton Kharenko
 *
 */
public interface ManagedSession extends Session {

  /**
   * 连接给定的通道
   * Connects current session to given channel.
   *
   * @param channel
   *            channel to which client connected
   * @return true if this is initial connection for this session; false otherwise.
   *
   */
  boolean connect(final Channel channel);

  /**
   * 断开给定的会话
   * Disconnect this session.
   *
   * @param channel the channel to use for disconnection
   */
  void disconnect(final Channel channel);

  /**
   * 发送心跳包
   * Send heartbeat packet to client.
   */
  void sendHeartbeat();

  /**
   * 发送消息包给客户
   * Send packet message to client.
   *
   * @param messagePacket message to be sent to client
   */
  void sendPacket(final Packet messagePacket);

  /**
   * 发送ACK给客户端
   * Send acknowledgment (e.g. HTTP 200) to client that message was accepted
   *
   * @param channel channel to which client connected
   */
  void acceptPacket(final Channel channel, final Packet packet);

  /**
   * 重新安排心跳包给客户端
   * Reschedule heartbeats for this client.
   */
  void acceptHeartbeat();

  /**
   * 将这个会话标记为 upgraded 如果这个会话升级到其他的传输层
   * Marks session as upgraded when session is going to be
   * upgraded to another transport type.
   */
  void markAsUpgraded();

}
