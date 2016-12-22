package mqtt;

import android.os.Handler;

import com.ibm.micro.client.mqttv3.MqttClient;
import com.ibm.micro.client.mqttv3.MqttConnectOptions;
import com.ibm.micro.client.mqttv3.MqttException;

public class Subscribe {
	private static MqttClient client = null;
	private static String clientID = "";
	
	public static String doTest(Handler handler, String deviceId) {
		try {
			clientID = deviceId;
			client = new MqttClient("tcp://120.197.98.61:1883",
					clientID, null);
			CallBack callback = new CallBack(clientID, handler);
			client.setCallback(callback);
			MqttConnectOptions conOptions = new MqttConnectOptions();
			conOptions.setCleanSession(false);
			client.connect(conOptions);
			client.subscribe("MQTT Example", 1);
			client.subscribe("MQTT Example Demo", 1);

		} catch (Exception e) {
			e.printStackTrace();
			return "failed";
		}
		return "success";
	}
	public static void doClose(){
		try {
			client.disconnect();
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
}