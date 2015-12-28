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

import io.netty.channel.Channel;
import io.scalecube.socketio.TransportType;

public class FlashSocketSession extends AbstractSocketSession {

  public FlashSocketSession(final Channel channel, final String sessionId, final String origin, final SessionDisconnectHandler disconnectHandler,
                            final TransportType upgradedFromTransportType, final int localPort, final SocketAddress remoteAddress) {
    super(channel, sessionId, origin, disconnectHandler, upgradedFromTransportType, localPort, remoteAddress);
  }

  // 这里只要设置传输模式为flashsocket即可，好方便的说
  @Override
  public TransportType getTransportType() {
    return TransportType.FLASHSOCKET;
  }
}
