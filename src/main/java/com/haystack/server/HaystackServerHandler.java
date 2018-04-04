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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.haystack.server.web.PhotoWebHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;

public class HaystackServerHandler extends SimpleChannelInboundHandler<HttpObject> {
  private FullHttpRequest request;
  /* Buffer that stores the response content */
  private final StringBuilder buf = new StringBuilder();
  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    processRequest(ctx, msg);
  }

  private void processRequest(ChannelHandlerContext ctx, HttpObject msg) {
    request = (FullHttpRequest) msg;

    // TODO: here, we need to process this get or POST request
    final ByteArrayDataOutput respBuf = ByteStreams.newDataOutput();

    if (request.method() == HttpMethod.GET) {
      writeResponse(ctx, new PhotoWebHandler(respBuf, ctx, request).handleRetrieval());
    }
    else {
      writeResponse(ctx, new PhotoWebHandler(respBuf, ctx, request).handleCreation());
    }

    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
      .addListener(ChannelFutureListener.CLOSE);
  }

  private void writeResponse(ChannelHandlerContext ctx,
                                final FullHttpResponse response) {
    // set connection status
    setupConnectionStatus(response);

    // Write the response.
    ctx.write(response);
  }
  private void setupConnectionStatus(final FullHttpResponse response) {
    // Add 'Content-Length' header only for a keep-alive connection.
    response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH,
      response.content().readableBytes());
    /**
     * Add keep alive header as per:
     * http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#
     * Connection
     */
    response.headers().set(HttpHeaderNames.CONNECTION,
      HttpHeaderValues.KEEP_ALIVE);
  }
}
