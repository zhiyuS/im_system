package com.cj.im.tcp.consumer;

import com.alibaba.fastjson.JSON;
import com.cj.codec.proto.MessagePack;
import com.cj.im.common.ClientType;
import com.cj.im.common.constant.Constants;
import com.cj.im.common.enums.DeviceMultiLoginEnum;
import com.cj.im.common.enums.command.SystemCommand;
import com.cj.im.common.model.UserClientDto;
import com.cj.im.tcp.redis.RedisManage;
import com.cj.im.tcp.util.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;

import java.util.List;

/**
 * 监听用户登录
 * * @description:
 *多端同步：1单端登录：一端在线：踢掉除了本clinetType + imel 的设备
 *         2双端登录：允许pc/mobile 其中一端登录 + web端 踢掉除了本clinetType + imel 以外的web端设备
 *       3 三端登录：允许手机+pc+web，踢掉同端的其他imei 除了web
 *       4 不做任何处理
 */
public class UserLoginMessageLister {
    private Integer LoginModel;

    public UserLoginMessageLister(Integer loginModel) {
        LoginModel = loginModel;
    }

    /**
     * 监听用户上线的消息
     */
    public void ListerUserLogin(){
        RedissonClient redissonClient = RedisManage.getRedissonClient();
        RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
        topic.addListener(String.class, new MessageListener<String>() {
            @Override
            public void onMessage(CharSequence charSequence, String s) {
                //1.获取消息
                UserClientDto userClientDto = JSON.parseObject(s,UserClientDto.class);
                //2.通知所有服务获取channel
                List<NioSocketChannel> nioSocketChannels = SessionSocketHolder.get(userClientDto.getUserId(), userClientDto.getAppId().toString());

                for (NioSocketChannel nioSocketChannel : nioSocketChannels) {

                    if(LoginModel == DeviceMultiLoginEnum.ONE.getLoginMode()){//单端登录
                        String clientImei = userClientDto.getClientType() + ":" + userClientDto.getImei();

                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

                        if(!clientImei.equals(clientType + ":" + imei)){
                            MessagePack<Object> pack = new MessagePack<>();

                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());//设置下线命令

                            pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            nioSocketChannel.writeAndFlush(pack);

                        }

                    }else if(LoginModel == DeviceMultiLoginEnum.TWO.getLoginMode()){//双端登录
                        if(userClientDto.getClientType() == ClientType.WEB.getCode()){
                            continue;
                        }
                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();

                        if(clientType == ClientType.WEB.getCode()){
                            continue;
                        }

                        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        String clientImei = userClientDto.getClientType() + ":" + userClientDto.getImei();

                        if(!clientImei.equals(clientType + ":" + imei)){
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());//设置下线命令

                            pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            nioSocketChannel.writeAndFlush(pack);
                        }

                    }else if(LoginModel == DeviceMultiLoginEnum.THREE.getLoginMode()){//多端登录
                        if(userClientDto.getClientType() == ClientType.WEB.getCode()){
                            continue;
                        }
                        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                        //旧的端是手机端并且新的端也是手机端才是true
                        boolean isSameClientType = false;
                        if((userClientDto.getClientType() == ClientType.IOS.getCode() ||
                                userClientDto.getClientType() == ClientType.ANDROID.getCode()) &&
                                (clientType == ClientType.IOS.getCode() || clientType == ClientType.ANDROID.getCode())
                        ){
                            isSameClientType = true;
                        }
                        if((userClientDto.getClientType() == ClientType.WINDOWS.getCode() ||
                                userClientDto.getClientType() == ClientType.MAC.getCode()) &&
                                (clientType == ClientType.WINDOWS.getCode() || clientType == ClientType.MAC.getCode())
                        ){
                            isSameClientType = true;
                        }

                        String clientImei = userClientDto.getClientType() + ":" + userClientDto.getImei();

                        if(isSameClientType && !clientImei.equals(clientType + ":" + imei)){
                            MessagePack<Object> pack = new MessagePack<>();
                            pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());//设置下线命令

                            pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
                            nioSocketChannel.writeAndFlush(pack);
                        }

                    }

                }

                System.out.println("收到用户上线");
            }
        });
    }
}
