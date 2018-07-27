package com.renj.rqottlibrary;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * 邮箱：renjunhua@anlovek.com
 * <p>
 * 创建时间：2018-07-26   19:41
 * <p>
 * 描述：Mqtt 相关监听适配器
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public abstract class RMqttServerAdapter {
    /**
     * 接收到消息，返回消息为 {@link MqttMessage} 对象
     *
     * @param topic   主题
     * @param message 消息内容 {@link MqttMessage}
     * @throws Exception 异常
     */
    abstract void messageArrived(String topic, MqttMessage message) throws Exception;


    /**
     * 传送完成
     *
     * @param token
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    /**
     * 连接断开
     *
     * @param cause 抛出的异常信息
     */
    public void connectionLost(Throwable cause) {
    }

    /**
     * 连接成功
     *
     * @param asyncActionToken
     */
    public void onSuccess(IMqttToken asyncActionToken) {

    }

    /**
     * 连接失败
     *
     * @param asyncActionToken
     * @param exception        抛出的异常信息
     */
    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

    }
}
