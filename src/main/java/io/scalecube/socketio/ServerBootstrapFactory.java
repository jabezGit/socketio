package io.scalecube.socketio;

import io.netty.bootstrap.ServerBootstrap;

/**
 * 这个工厂类就是用来创建ServerBootstrap服务器启动脚本
 * @author Anton Kharenko
 */
public interface ServerBootstrapFactory {

  ServerBootstrap createServerBootstrap();

}
