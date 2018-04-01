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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class HaystackClientInitializer extends ChannelInitializer<SocketChannel> {

  private HaystackClientHandler httpClientHandler;

  @Override
  public void initChannel(SocketChannel ch) {
    ChannelPipeline p = ch.pipeline();

    p.addLast(new HttpClientCodec());

    /*
     * Remove the following line if you don't want automatic content
     * decompression.
     */
    p.addLast(new HttpContentDecompressor());

    // Uncomment the following line if you don't want to handle HttpContents.
    p.addLast(new HttpObjectAggregator(1048576));

    httpClientHandler = new HaystackClientHandler();
    p.addLast(httpClientHandler);
  }
}
