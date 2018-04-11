/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.haystack.server;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import com.haystack.Config;
import com.haystack.storage.Service;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class HaystackServer implements Runnable {

  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  private int port;
  private InetSocketAddress localAddr;
  private AtomicBoolean channelActive = new AtomicBoolean(false);
  private AtomicBoolean channelClosed = new AtomicBoolean(false);

  public HaystackServer(final int port) {
    this.port = port;
  }

  public static void main(String[] args) throws Exception {
    int port = 8080;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }

    Service s = Service.getService();
    s.getDb().ensureKeyspaceExists();
    s.getDb().ensureTableExists();

    final HaystackServer server = new HaystackServer(port);
    try {
      server.start();
    } finally {
      server.shutDown();
    }
  }

  private void start() throws InterruptedException {

    // Configure the server.
    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new HaystackServerInitializer());

      /* wait for channel being active */
      Channel ch = b.bind(port).sync().addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            channelActive.set(true);

          } else {
            channelActive.set(false);
            throw new Exception("failed to activate netty server channel.", future.cause());
          }
        }
      }).channel();
      localAddr = (InetSocketAddress) ch.localAddress();

      /* wait for channel being closed */
      ch.closeFuture().sync().addListener(new ChannelFutureListener() {
        public void operationComplete(ChannelFuture future) throws Exception {
          if (future.isSuccess()) {
            channelClosed.set(true);
          } else {
            channelClosed.set(false);
            throw new Exception("failed to close netty server channel.", future.cause());
          }
        }
      });
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }

  public void shutDown() {
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
    }
    if (bossGroup != null) {
      bossGroup.shutdownGracefully();
    }
  }

  @Override
  public void run() {
    try {
      start();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
