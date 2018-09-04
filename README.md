# 在Android中使用Mqtt建立长连接
基于   
* `implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0'`   
* `implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'`  

库，进行封装；链式调用，使用简单。

# 使用

### 1.定义变量
    private String serverUrl = "tcp://156.23.4.8:1883";
    private String imei = "test_id";
    private String topic = "r_test";
    private int qo = 1;
    private String username = "your username";
    private String password = "your admin";  
    
    private RMqttServer rMqttServer;  

### 2.初始化Mqtt
    private void initMqtt() {
        rMqttServer = new RMqttServer.Builder()
            // mqtt服务器地址 格式例如：tcp://10.0.261.159:1883
            .serverUrl(serverUrl)
            // 主题和策略
            .topic(topic, qo)
            // 设置自动重连
            .autoReconnect(true)
            // 设置不清除回话session 可收到服务器之前发出的推送消息
            .cleanSession(false)
            // 唯一标识
            .clientId(imei)
            // 用户名、密码
            .userName(username)
            .passWord(password)
            // 心跳包默认的发送间隔
            .keepAliveInterval(20)
            // 构建出RMqttServer 建议用application的context
            .build(this.getApplicationContext());

        // 设置监听器，接收消息
        rMqttServer.setRMqttServerAdapter(new RMqttServerAdapter() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // 推送消息到达
                String result = new String(message.getPayload());
                Log.i("MainActivity", "推送消息到达 => " + result);
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
            }
        });
    }
 
 ### 3.发送消息
    rMqttServer.publish(messageContent);
    
  ### 4.取消已订阅的主题
    rMqttServer.unsubscribe(topic);
     
  ### 5.订阅主题
    rMqttServer.subscribe(topic, 1);
    
   ### 6.手动建立连接(在初始化方法中必须指定策略和主题)
     rMqttServer.connect();
     
  ### 7.断开连接、关闭客户端
    rMqttServer.disconnect(); // 断开连接
    rMqttServer.close();      // 关闭客户端
    
### 8.相关监听(可选择行重写)
    public abstract class RMqttServerAdapter {
        /**
         * 接收到消息，返回消息为 {@link MqttMessage} 对象
         *
         * @param topic   主题
         * @param message 消息内容 {@link MqttMessage}
         * @throws Exception 异常
         */
        public abstract void messageArrived(String topic, MqttMessage message) throws Exception;
    
    
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
    
   