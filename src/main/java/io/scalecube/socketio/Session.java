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

import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;

/**
 * 当一个新的客户端要连接进来的时候，创建一个session，当成功连接之后，需要返回一个
 * session ID 
 * When client handshake and connects to the socket.io server a
 * session is created this session handles the communication with a
 * specific client connection each client is represented with specific
 * session id that is correlated in the communication protocol.
 *
 * @author Ronen Hamias 
 */
public interface Session {

  // 设置状态集合：创建，连接中，连接完成，断开中，已断开
  public enum State {
    CREATED, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
  }

  /**
   * Returns session identifier.
   * 返回Seeion的ID
   */
  String getSessionId();

  /**
   * 返回本次回话的来源
   * Returns origin header of this session.
   */
  String getOrigin();

  /**
   * 返回的值：如果这个标志位为ture的话，这就意味着回话已被创建成功（返回数据交换的结果）
   * @return if this flag is true it means that session was created in result
   *         of switching transport protocol (e.g. from WebSocket to
   *         XHR-Polling).
   */
  boolean isUpgradedSession();

  /**
   * 返回：协议转换类型
   * @return if that session was created in result of switching transport
   *         protocol (e.g. from WebSocket to XHR-Polling) it will return
   *         previous transport type. If this session isn't upgraded session
   *         it will return null.
   */
  TransportType getUpgradedFromTransportType();

  /**
   * Returns transport type for this session (e.g. WebSocket or XHR-Polling).
   */
  TransportType getTransportType();

  /**
   * 返回远程的地址
   * Returns remote address of the client.
   */
  SocketAddress getRemoteAddress();

  /**
   * 返回会话的状态
   * Returns session state (see {@link Session.State}).
   */
  State getState();

  /**
   * 返回到客户端建立连接的本地端口
   * Returns local port to which client connection is established.
   */
  int getLocalPort();

  /**
   * Sends provided message's payload to client. Passed ByteBuf will be released
   * during sending operation.
   *
   * @param message
   *            message's payload to be sent to client
   */
  void send(final ByteBuf message);

  /**
   * Disconnects this session
   */
  void disconnect();

}
