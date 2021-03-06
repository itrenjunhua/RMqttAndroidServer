package com.renj.rqottlibrary;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * ======================================================================
 * <p>
 * 作者：Renj
 * 邮箱：renjunhua@anlovek.com
 * <p>
 * 创建时间：2018-07-27   14:02
 * <p>
 * 描述：
 * <p>
 * 修订历史：
 * <p>
 * ======================================================================
 */
public class RMqttServer {
    private final String TAG = RMqttServer.class.getName();
    private Handler handler = new Handler(Looper.getMainLooper());
    private int reConnectCount = 0;
    private MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    private Builder builder;

    private RMqttServerAdapter rMqttServiceAdapter;

    /**
     * 设置监听
     *
     * @param rMqttServiceAdapter {@link RMqttServerAdapter} 对象
     */
    public void setRMqttServerAdapter(RMqttServerAdapter rMqttServiceAdapter) {
        this.rMqttServiceAdapter = rMqttServiceAdapter;
    }

    /**
     * 监听消息回调
     */
    private MqttCallback callback = new MqttCallback() {
        @Override
        public void connectionLost(Throwable cause) {
            if (isDebug())
                Log.w(TAG, "connectionLost() => 连接断开 " + cause);
            if (rMqttServiceAdapter != null)
                rMqttServiceAdapter.connectionLost(cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            if (isDebug())
                Log.i(TAG, "messageArrived() => 主题 " + topic + " 接收到消息");
            if (rMqttServiceAdapter != null)
                rMqttServiceAdapter.messageArrived(topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            if (isDebug())
                Log.v(TAG, "deliveryComplete() => 客户端 " + token.getClient().getClientId() + " 传送完成");
            if (rMqttServiceAdapter != null)
                rMqttServiceAdapter.deliveryComplete(token);
        }
    };

    /**
     * MQTT是否连接监听
     */
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            if (isDebug())
                Log.v(TAG, "onSuccess() => 客户端 " + asyncActionToken.getClient().getClientId() + " 连接成功");

            // 如果在 Builder 中配置了 主题和策略 那么这里将自动订阅
            if (builder.topics != null && builder.topics.length > 0
                    && builder.qos != null && builder.qos.length > 0) {

                if (isDebug()) {
                    String topics = "[ ";
                    for (String topic : builder.topics) {
                        topics += topic + " ";
                    }
                    topics += "]";
                    Log.v(TAG, "subscribe() => 自动订阅主题 " + topics);
                }

                subscribe(builder.topics, builder.qos);
            } else if (!TextUtils.isEmpty(builder.topic) && builder.qo > -1) {
                if (isDebug())
                    Log.v(TAG, "subscribe() => 自动订阅主题 " + builder.topic);

                subscribe(builder.topic, builder.qo);
            }

            if (rMqttServiceAdapter != null)
                rMqttServiceAdapter.onSuccess(asyncActionToken);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            if (isDebug())
                Log.e(TAG, "onFailure() => 客户端 " + asyncActionToken.getClient().getClientId() + " 连接失败 : " + exception);

            if (rMqttServiceAdapter != null)
                rMqttServiceAdapter.onFailure(asyncActionToken, exception);

            if (builder.autoReconnect) {

                // 如果 autoReConnectCount 的值为 -1 表示无限重连，那么每次重连时都将 reConnectCount 的值设置为 -5 实现无限重连
                if (builder.autoReConnectCount == -1) reConnectCount = -5;

                if (reConnectCount < builder.autoReConnectCount) {
                    // 判断重连时间间隔
                    if (builder.autoReConnectionInterval == 0) {
                        reConnectCount += 1;
                        connect();
                    } else {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                reConnectCount += 1;
                                connect();
                            }
                        }, builder.autoReConnectionInterval * 1000);
                    }
                }
            }
        }
    };

    /**
     * 构造私有
     */
    private RMqttServer(Builder builder) {
        this.builder = builder;

        init(builder);
    }

    /**
     * 初始化 MqttAndroidClient
     */
    private void init(Builder builder) {
        // 服务器地址（协议+地址+端口号）
        client = new MqttAndroidClient(builder.context, builder.serverUrl, builder.clientId);
        // 设置MQTT监听并且接受消息
        client.setCallback(callback);
        // 连接参数
        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(builder.cleanSession);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(builder.timeOut);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(builder.keepAliveInterval);
        // 用户名
        conOpt.setUserName(builder.userName);
        // 密码
        conOpt.setPassword(builder.passWord.toCharArray());
        // 是否自动重新连接
        conOpt.setAutomaticReconnect(builder.autoReconnect);

        if (builder.autoConnect)
            connect();
    }

    /**
     * 使用新的主题和策略订阅主题
     *
     * @param topics 主题
     * @param qos    策略
     */
    public void subscribe(String[] topics, int[] qos) {
        try {
            // 订阅topic话题
            client.subscribe(topics, qos);

            if (isDebug()) {
                String temp = "[ ";
                for (String topic : topics) {
                    temp += topic + " ";
                }
                temp += "]";
                Log.v(TAG, "subscribe() => 订阅主题 " + temp);
            }
        } catch (Exception e) {
            if (isDebug()) {
                String temp = "[ ";
                for (String topic : topics) {
                    temp += topic + " ";
                }
                temp += "]";
                Log.e(TAG, "subscribe() => 订阅主题 " + temp + "异常 : " + e);
            }
        }
    }

    /**
     * 使用新的主题和策略订阅主题
     *
     * @param topic 主题
     * @param qos   策略
     */
    public void subscribe(String topic, int qos) {
        try {
            // 订阅topic话题
            client.subscribe(topic, qos);

            if (isDebug())
                Log.v(TAG, "subscribe() => 订阅主题 " + topic);
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "subscribe() => 订阅主题 " + topic + " 异常 : " + e);
        }
    }

    /**
     * 取消订阅
     *
     * @param topics 取消订阅的主题
     */
    public void unsubscribe(String[] topics) {
        try {
            client.unsubscribe(topics);
            if (isDebug()) {
                String temp = "[ ";
                for (String topic : topics) {
                    temp += topic + " ";
                }
                temp += "]";
                Log.e(TAG, "unsubscribe() => 取消订阅 " + temp);
            }
        } catch (MqttException e) {
            if (isDebug()) {
                String temp = "[ ";
                for (String topic : topics) {
                    temp += topic + " ";
                }
                temp += "]";
                Log.e(TAG, "unsubscribe() => 取消订阅 " + temp + " 异常 : " + e);
            }
        }
    }

    /**
     * 取消订阅
     *
     * @param topic 取消订阅的主题
     */
    public void unsubscribe(String topic) {
        try {
            client.unsubscribe(topic);
            if (isDebug())
                Log.e(TAG, "unsubscribe() => 取消订阅 " + topic);
        } catch (MqttException e) {
            if (isDebug())
                Log.e(TAG, "unsubscribe() => 取消订阅 " + topic + " 异常 : " + e);
        }
    }

    /**
     * 使用{@link Builder}中配置的 {@link Builder#topic} 、{@link Builder#qo} 、{@link Builder#retained} 发布消息.<br/>
     * 直接使用 {@link String#getBytes()} 转换成 byte[]，如果转换方式不是如此，请使用 {@link #publish(byte[], String, int, boolean)} 或 {@link #publish(MqttMessage, String)}
     *
     * @param msg 消息内容
     */
    public void publish(String msg) {
        try {
            client.publish(builder.topic, msg.getBytes(), builder.qo, builder.retained);
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "publish() => 发布消息异常 : " + e);
        }
    }

    /**
     * 发布消息.<br/>
     * 直接使用 {@link String#getBytes()} 转换成 byte[]，如果转换方式不是如此，请使用 {@link #publish(byte[], String, int, boolean)} 或 {@link #publish(MqttMessage, String)}
     *
     * @param msg      消息内容
     * @param topic    主题
     * @param qos      策略
     * @param retained 是否保存
     */
    public void publish(String msg, String topic, int qos, boolean retained) {
        try {
            client.publish(topic, msg.getBytes(), qos, retained);
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "publish() => 发布消息异常 : " + e);
        }
    }

    /**
     * 发布消息
     *
     * @param msg      消息内容
     * @param topic    主题
     * @param qos      策略
     * @param retained 是否保存
     */
    public void publish(byte[] msg, String topic, int qos, boolean retained) {
        try {
            client.publish(topic, msg, qos, retained);
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "publish() => 发布消息异常 : " + e);
        }
    }

    /**
     * 发布消息
     *
     * @param msg   消息 {@link MqttMessage} 对象
     * @param topic 主题
     */
    public void publish(MqttMessage msg, String topic) {
        try {
            client.publish(topic, msg);
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "publish() => 发布消息异常 : " + e);
        }
    }

    /**
     * 连接MQTT服务器
     */
    public void connect() {
        if (!isConnected()) {
            try {
                if (isDebug())
                    Log.v(TAG, "connect() => 开始连接服务器...");
                client.connect(conOpt, null, iMqttActionListener);
            } catch (Exception e) {
                if (isDebug())
                    Log.e(TAG, "connect() => 连接服务器异常 : " + e);
            }
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            client.disconnect();
            if (isDebug())
                Log.v(TAG, "disconnect() => 断开服务器连接");
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "disconnect() => 断开服务器连接异常 : " + e);
        }
    }

    /**
     * 关闭客户端
     */
    public void close() {
        try {
            client.close();
            if (isDebug())
                Log.v(TAG, "close() => 关闭客户端");
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "close() => 关闭客户端异常 : " + e);
        }
    }

    /**
     * 判断连接是否断开
     */
    public boolean isConnected() {
        try {
            return client.isConnected();
        } catch (Exception e) {
            if (isDebug())
                Log.e(TAG, "isConnected() => 判断连接是否断开异常 : " + e);
        }
        return false;
    }

    /**
     * 判断当前应用是否是debug状态
     */
    private boolean isDebug() {
        try {
            if (builder.context != null) {
                ApplicationInfo info = builder.context.getApplicationInfo();
                return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 配置类
     */
    public static final class Builder {
        private Context context;
        private String serverUrl;
        private String userName = "";
        private String passWord = "";
        private String clientId;
        private int timeOut = 10;
        private int keepAliveInterval = 20; // 心跳包发送时间间隔，单位 秒
        private boolean cleanSession = false;
        private boolean retained = false;
        private boolean autoConnect = true;
        private boolean autoReconnect = true;
        private String[] topics;
        private int[] qos;
        private String topic;
        private int qo = -1;
        private int autoReConnectionInterval = 20; // 自动重连间隔时间，单位 秒
        private int autoReConnectCount = 5; // 连接失败自动重连次数，-1 表示无限重连

        /**
         * 配置服务器地址 格式：tcp://122.12.168.8:1883
         *
         * @param serverUrl 服务器地址
         * @return
         */
        public Builder serverUrl(@NonNull String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        /**
         * 配置登录用户名，如果需要
         *
         * @param userName 登录用户名
         * @return
         */
        public Builder userName(@NonNull String userName) {
            this.userName = userName;
            return this;
        }

        /**
         * 配置登录密码，如果需要
         *
         * @param passWord 登录密码
         * @return
         */
        public Builder passWord(@NonNull String passWord) {
            this.passWord = passWord;
            return this;
        }

        /**
         * 配置<b>唯一</b>标识，建议设备序列号等
         *
         * @param clientId 唯一标识
         * @return
         */
        public Builder clientId(@NonNull String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * 配置超时时间，单位：秒  默认 10 秒
         *
         * @param timeOut 超时时间，单位：秒
         * @return
         */
        public Builder timeOut(@IntRange(from = 1) int timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        /**
         * 配置心跳包发送间隔，单位：秒  默认 20 秒
         *
         * @param keepAliveInterval 心跳包发送间隔，单位：秒
         * @return
         */
        public Builder keepAliveInterval(@IntRange(from = 1) int keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        /**
         * 是否自动连接到服务器，默认自动连接
         *
         * @param autoConnect true：自动连接 false：不自动连接
         * @return
         */
        public Builder autoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        /**
         * 配置是否设置自动重连，默认自动重连 5 次，可以通过 {@link #autoReConnectCount(int)} 方法指定次数
         *
         * @param autoReconnect true：自动重连 false：不自动重连
         * @return
         */
        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        /**
         * 连接失败时，自动重新连接的次数。默认 5次，<b>-1 表示无限重连</b>
         *
         * @param autoReConnectCount 重新连接次数
         * @return
         */
        public Builder autoReConnectCount(@IntRange(from = -1) int autoReConnectCount) {
            this.autoReConnectCount = autoReConnectCount;
            return this;
        }

        /**
         * 自动重连间隔时间，单位：秒  默认 20 秒；前提：能自动重连 可以通过 {@link #autoReConnectCount(int)} 方法设置重连次数
         *
         * @param autoReConnectionInterval 自动重连间隔时间 ，单位：秒
         * @return
         */
        public Builder autoReConnectionInterval(@IntRange(from = 0) int autoReConnectionInterval) {
            this.autoReConnectionInterval = autoReConnectionInterval;
            return this;
        }

        /**
         * 配置服务器是否保存最后一条消息，默认 false
         *
         * @param retained true：是 false：否
         * @return
         */
        public Builder retained(boolean retained) {
            this.retained = retained;
            return this;
        }

        /**
         * 配置是否清除缓存，默认 false
         *
         * @param cleanSession true：是 false：否
         * @return
         */
        public Builder cleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        /**
         * 配置多个主题和策略。<b>如果配置正常，会自动订阅，且优先级高于 {@link #topic(String, int)}</b><br/><br/>
         * 主题对应的推送策略 分别是0, 1, 2；建议服务端和客户端配置的主题一致<br/>
         * 0 表示只会发送一次推送消息 收到不收到都不关心<br/>
         * 1 保证能收到消息，但不一定只收到一条<br/>
         * 2 保证收到切只能收到一条消息<br/>
         *
         * @param topics 主题数组
         * @param qos    策略数组
         * @return
         */
        public Builder topics(@NonNull String[] topics, @NonNull int[] qos) {
            this.topics = topics;
            this.qos = qos;
            return this;
        }

        /**
         * 配置单个主题和策略。<b>如果配置正常，会自动订阅，优先级低于 {@link #topics(String[], int[])}</b><br/><br/>
         * 主题对应的推送策略 分别是0, 1, 2；建议服务端和客户端配置的主题一致<br/>
         * 0 表示只会发送一次推送消息 收到不收到都不关心<br/>
         * 1 保证能收到消息，但不一定只收到一条<br/>
         * 2 保证收到切只能收到一条消息<br/>
         *
         * @param topic 主题
         * @param qo    策略
         * @return
         */
        public Builder topic(@NonNull String topic, @IntRange(from = 0, to = 2) int qo) {
            this.topic = topic;
            this.qo = qo;
            return this;
        }

        /**
         * 构建 {@link RMqttServer} 对象
         *
         * @param context 上下文，建议使用 {@link android.app.Application} 类
         * @return
         */
        @NonNull
        public RMqttServer build(@NonNull Context context) {
            this.context = context;
            return new RMqttServer(this);
        }
    }
}
