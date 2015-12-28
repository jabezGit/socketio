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
package io.scalecube.socketio.packets;

import io.scalecube.socketio.TransportType;


/**
 * socekt 包是指连接客户端和服务器端之间的协议
 * Socket.io packet is a message protocol unit that wired between socket.io client to socket.io server
 *
 * @author Ronen Hamias
 */
public interface IPacket {

  /**
   * the type of the packet
   * 包的类型是指，是不是说，我可以从这里开始更改成我自己定义的协议呢
   * packet type is the prefix transfered on the wire
   * @return {@link PacketType}
   */
  PacketType getType();

  String getOrigin();

  void setOrigin(final String origin);

  String getSessionId();

  void setSessionId(final String sessionId);

  TransportType getTransportType();

  void setTransportType(TransportType transportType);

  String getJsonpIndexParam();

  void setJsonpIndexParam(String jsonpIndexParam);

}
