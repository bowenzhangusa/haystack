package com.haystack.server.web;

import com.haystack.storage.Service;
import com.haystack.server.model.Photo;

import java.io.IOException;
import java.util.UUID;
import io.netty.buffer.Unpooled;

import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.io.ByteArrayDataOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;

public class PhotoWebHandler {
  private ByteArrayDataOutput respBuf;
  private ChannelHandlerContext ctx;
  private FullHttpRequest req;

  public PhotoWebHandler(ByteArrayDataOutput respBuf, ChannelHandlerContext ctx,
                         FullHttpRequest req) {
    this.respBuf = respBuf;
    this.ctx = ctx;
    this.req = req;
  }


  public FullHttpResponse handleCreation() throws IOException {
    Photo photo = newPhotoFromRequest();
    photo.setId(UUID.randomUUID());

    Service service = Service.getService();
    service.saveFile(photo.getId(), photo.getImage());
    System.out.println("saving photo " + photo.getId());
    appendln(String.format("{\"%s\":\"%s\"}", "id", photo.getId()), this.respBuf);
    return newResponse(HttpResponseStatus.CREATED, respBuf);
  }

  public FullHttpResponse handleRetrieval() {
    String[] tokens = req.uri().toString().split("/");
    if (tokens.length < 2) {
      System.out.println("No UUID in request");
      return newResponse(HttpResponseStatus.BAD_REQUEST, respBuf);
    }

    UUID id = null;

    try {
      id = UUID.fromString(tokens[tokens.length - 1]);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      return newResponse(HttpResponseStatus.BAD_REQUEST, respBuf);
    }

    Service service = Service.getService();
    byte[] image = service.getFile(id);
    try {
      return newResponseForInstance(id.toString(), new Photo(id, image));
    }
    catch (JsonProcessingException ex) {
      return newResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, respBuf);
    }
  }

  private Photo newPhotoFromRequest()
    throws JsonParseException, JsonMappingException, IOException {
    final ByteBuf content = this.req.content();
    if (content.isReadable()) {
      try {
        final byte[] json = ByteBufUtil.getBytes(content);
        return Photo.newInstance(json);
      } catch (JsonParseException ex) {
        // if a request contained binary data instead of json
        return new Photo(null, ByteBufUtil.getBytes(content));
      }
    }
    return null;
  }
  protected static FullHttpResponse newResponse(
    final HttpResponseStatus status,
    final ByteArrayDataOutput respBuf) {
    FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
      Unpooled.copiedBuffer(respBuf.toByteArray()));

    response.headers().set(HttpHeaderNames.CONTENT_TYPE,
      "application/json; charset=UTF-8");
    return response;
  }

  protected static void appendln(
    final String str,
    final ByteArrayDataOutput respBuf) {
    final String line = String.format("%s%n", str);
    respBuf.write(line.getBytes(CharsetUtil.UTF_8));
  }

  protected void appendByteArray(final byte[] byteArray) {
    this.respBuf.write(byteArray);
  }

  private FullHttpResponse newResponseForInstance(final String id,
                                                  final Photo instance) throws JsonProcessingException {
    if (instance != null) {
      appendByteArray(instance.toJsonByteArray());
      return newResponse(HttpResponseStatus.OK, respBuf);
    } else {
      appendln(
        String.format("Nonexistent resource with URI: /photo/%s", id), this.respBuf);
      return newResponse(HttpResponseStatus.NOT_FOUND, respBuf);
    }
  }
}
