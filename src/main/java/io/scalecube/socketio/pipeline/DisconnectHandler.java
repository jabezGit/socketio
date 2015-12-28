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
package io.scalecube.socketio.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.ReferenceCountUtil;
import io.scalecube.socketio.packets.Packet;
import io.scalecube.socketio.packets.PacketType;

/**
 * Class which provides handler for supporting forced socket disconnection
 * according to specification below.
 *
 * <h1>Forced socket disconnection</h1>
 * <p/>
 * A Socket.IO server must provide an endpoint to force the disconnection of the
 * socket.
 * <p/>
 * While closing the transport connection is enough to trigger a disconnection,
 * it sometimes is desirable to make sure no timeouts are activated and the
 * disconnection events fire immediately.
 * <p/>
 * {@code http://example.com/socket.io/1/xhr-polling/812738127387123?disconnect}
 * <p/>
 * The server must respond with 200 OK, or 500 if a problem is detected.
 */
@ChannelHandler.Sharable
public class DisconnectHandler extends ChannelInboundHandlerAdapter {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String DISCONNECT = "disconnect";


  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      final HttpRequest req = (HttpRequest) msg;
      final HttpMethod requestMethod = req.getMethod();
      final QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
      final String requestPath = queryDecoder.path();

      boolean disconnect = queryDecoder.parameters().containsKey(DISCONNECT);
      if (disconnect) {
        if (log.isDebugEnabled())
          log.debug("Received HTTP disconnect request: {} {} from channel: {}", requestMethod, requestPath, ctx.channel());

        final String sessionId = PipelineUtils.getSessionId(requestPath);
        final Packet disconnectPacket = new Packet(PacketType.DISCONNECT, sessionId);
        disconnectPacket.setOrigin(PipelineUtils.getOrigin(req));
        ctx.fireChannelRead(disconnectPacket);
        ReferenceCountUtil.release(msg);
        return;
      }
    }
    ctx.fireChannelRead(msg);
  }
}
