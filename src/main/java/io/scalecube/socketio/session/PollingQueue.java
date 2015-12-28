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

import java.util.concurrent.ConcurrentLinkedQueue;

import io.scalecube.socketio.packets.Packet;
import io.scalecube.socketio.packets.PacketsFrame;

// 这是一个轮询队列对象
public class PollingQueue {

  private final ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<Packet>();

  public PollingQueue() {
  }

  // 这里有一个takeall方法，具体是干啥的？采取一切？
  public PacketsFrame takeAll() {
    PacketsFrame frame = new PacketsFrame();
    Packet packet;
    // 这里为什么用一个死循环来实现这个方法呢
    while ((packet = packetQueue.poll()) != null) {
      frame.getPackets().add(packet);
    }
    return frame;
  }

  // 在队列里添加一个包
  public void add(final Packet packet) {
    if (packet != null) {
      packetQueue.add(packet);
    }
  }

  // 判断包时候为空
  public boolean isEmpty() {
    return packetQueue.isEmpty();
  }

}
