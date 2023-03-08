package com.cj.codec;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cj.codec.proto.Message;
import com.cj.codec.proto.MessageHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if(byteBuf.readableBytes()<28){
            return;
        }
        int command = byteBuf.readInt();
        int version = byteBuf.readInt();
        int clientType = byteBuf.readInt();
        int messageType = byteBuf.readInt();
        int appId = byteBuf.readInt();
        int imeiLength = byteBuf.readInt();
        int bodyLen = byteBuf.readInt();



        if( bodyLen +imeiLength > byteBuf.readableBytes()){
            byteBuf.resetReaderIndex();
            return;
        }
        byte imeiBuffer[] = new byte[imeiLength];
        byteBuf.readBytes(imeiBuffer);
        String imei = imeiBuffer.toString();

        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setCommand(command);
        messageHeader.setVersion(version);
        messageHeader.setClientType(clientType);
        messageHeader.setAppId(appId);
        messageHeader.setMessageType(messageType);
        messageHeader.setImeiLength(imeiLength);
        messageHeader.setImei(imei);
        messageHeader.setBodyLen(bodyLen);

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        byte bodyBuffer[] = new byte[bodyLen];
        byteBuf.readBytes(bodyBuffer);

        if(messageType == 0x0){
            String body = new String(bodyBuffer);
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }
        byteBuf.markReaderIndex();
        list.add(message);

    }
}
