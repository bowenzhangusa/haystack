package com.haystack.server.web;

import com.haystack.storage.Service;
import com.haystack.server.model.Photo;

import java.io.IOException;
import java.util.UUID;

import io.netty.buffer.Unpooled;

import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.io.ByteArrayDataOutput;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
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

        if (photo.getContent() == null || photo.getContent().length < 1) {
            return newResponse(HttpResponseStatus.BAD_REQUEST, respBuf, null);
        }

        Service service = Service.getService();
        service.saveFile(photo);
        System.out.println("saving photo " + photo.getId());
        appendln(String.format("{\"%s\":\"%s\"}", "id", photo.getId()), this.respBuf);
        return newResponse(HttpResponseStatus.CREATED, respBuf, null);
    }

    public FullHttpResponse handleRetrieval() {
        String[] tokens = req.uri().toString().split("/");
        if (tokens.length < 2) {
            System.out.println("No UUID in request");
            return newResponse(HttpResponseStatus.BAD_REQUEST, respBuf, null);
        }

        UUID id = null;

        try {
            id = UUID.fromString(tokens[tokens.length - 1]);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return newResponse(HttpResponseStatus.BAD_REQUEST, respBuf, null);
        }

        Service service = Service.getService();
        Photo photo = service.getFile(id);
        this.respBuf.write(photo.getContent());
        return newResponse(HttpResponseStatus.OK, respBuf, photo.getContentType());
    }

    private Photo newPhotoFromRequest() throws IOException {
        Photo photo = new Photo();

        HttpPostRequestDecoder httpDecoder = new HttpPostRequestDecoder(this.req);
        httpDecoder.setDiscardThreshold(0);
        InterfaceHttpData data = httpDecoder.getBodyHttpData("file");

        if (data == null) {
            return photo;
        }

        FileUpload fileUpload = (FileUpload) data;
        photo.setContent(fileUpload.get());
        photo.setContentType(fileUpload.getContentType());

        return photo;
    }

    protected static FullHttpResponse newResponse(
            final HttpResponseStatus status,
            final ByteArrayDataOutput respBuf,
            String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status,
                Unpooled.copiedBuffer(respBuf.toByteArray()));

        if (contentType == null) {
            contentType = "application/json; charset=UTF-8";
        }

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        return response;
    }

    protected static void appendln(
            final String str,
            final ByteArrayDataOutput respBuf) {
        final String line = String.format("%s%n", str);
        respBuf.write(line.getBytes(CharsetUtil.UTF_8));
    }
}
