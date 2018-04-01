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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

public class HaystackClientHandler extends SimpleChannelInboundHandler<HttpObject> {
  private HttpResponse httpResponse;
  private HttpContent httpContent;

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
    if (msg instanceof HttpResponse) {
      httpResponse = (HttpResponse) msg;
      System.err.println("-------HTTP/HTTPS RESPONSE-------");
      System.err.println("STATUS: " + httpResponse.getStatus());
      System.err.println("VERSION: " + httpResponse.getProtocolVersion());
      System.err.println();

      System.err.println("PRINTING HEADER");
      if (!httpResponse.headers().isEmpty()) {
        for (CharSequence name : httpResponse.headers().names()) {
          for (CharSequence value : httpResponse.headers().getAll(name)) {
            System.err.println("HEADER: " + name + " = " + value);
          }
        }
        System.err.println();
      }
    }

    if (msg instanceof HttpContent) {
      final HttpContent content = (HttpContent) msg;
      /* deep copy content in case it's deallocated while going out of scope */
      httpContent = content.copy();

      System.err.print(content.content().toString(CharsetUtil.UTF_8));
      System.err.flush();

      if (content instanceof LastHttpContent) {
        System.err.println("} END OF CONTENT");
        ctx.close();
      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
