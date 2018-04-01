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

// A simple client to test our implementation. We may not even need the Client package at all
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URI;

public class HaystackClient {

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("host, port and REST URI must be provided");
      return;
    }

    String HOST = args[0];
    int PORT = Integer.parseInt(args[1]);
    String REST_URI = args[2];
    final String url = String.format(
      "%s://%s:%d/%s",
      "http",
      HOST,
      PORT,
      REST_URI);
    final URI uri = URI.create(url);
    // Configure the client.
    EventLoopGroup group = new NioEventLoopGroup();
    try {
      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class)
        .handler(new HaystackClientInitializer());

      // Make the connection attempt.
      Channel ch = b.connect(HOST, PORT).sync().channel();

      // Send the HTTP request.
      // TODO: The method is not necessarily GET, can be POST as well
      FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
        HttpMethod.GET, uri.getRawPath());
      ch.writeAndFlush(request);

      // Wait for the server to close the connection.
      ch.closeFuture().sync();
    } finally {
      // Shut down executor threads to exit.
      group.shutdownGracefully();
    }
  }

}
