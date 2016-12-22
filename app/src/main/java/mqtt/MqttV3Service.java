package mqtt;

import android.os.Handler;

import com.ibm.micro.client.mqttv3.MqttClient;
import com.ibm.micro.client.mqttv3.MqttConnectOptions;
import com.ibm.micro.client.mqttv3.MqttDeliveryToken;
import com.ibm.micro.client.mqttv3.MqttException;
import com.ibm.micro.client.mqttv3.MqttMessage;
import com.ibm.micro.client.mqttv3.MqttPersistenceException;
import com.ibm.micro.client.mqttv3.MqttTopic;

import java.util.ArrayList;

public class MqttV3Service {
    String addr = "";
    String port = "";
    private static MqttClient client = null;
    private static MqttTopic topic = null;
    static ArrayList<MqttTopic> topicList = new ArrayList<MqttTopic>();

    public static boolean connectionMqttServer(Handler handler, String ServAddress, String ServPort, String userID, ArrayList<String> Topics) {
        String connUrl = "tcp://" + ServAddress + ":" + ServPort;
        try {
            client = new MqttClient(connUrl, userID, null);
            for (int i = 0; i < Topics.size(); i++) {
                topic = client.getTopic(Topics.get(i));
                topicList.add((MqttTopic) topic);
            }
            CallBack callback = new CallBack(userID, handler);
            client.setCallback(callback);
            MqttConnectOptions conOptions = new MqttConnectOptions();
//			conOptions.setUserName(MyApplication.gUserName);
//			conOptions.setPassword(Pssword.toCharArray());
            conOptions.setCleanSession(false);
//			char[] ddd = conOptions.getPassword();
//			System.out.println(ddd);
            client.connect(conOptions);
            for (int i = 0; i < Topics.size(); i++) {
                client.subscribe(Topics.get(i), 1);

            }
        } catch (MqttException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean closeMqtt() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean publishMsg(String msg, int Qos, int position) {
        MqttMessage message = new MqttMessage(msg.getBytes());
        message.setQos(Qos);
        MqttDeliveryToken token;
        try {
            token = topicList.get(position).publish(message);
            while (!token.isComplete()) {
                token.waitForCompletion(1000);
            }
        } catch (MqttPersistenceException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return false;
        } catch (MqttException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
