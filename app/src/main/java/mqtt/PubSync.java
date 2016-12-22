package mqtt;

import com.ibm.micro.client.mqttv3.MqttClient;
import com.ibm.micro.client.mqttv3.MqttDeliveryToken;
import com.ibm.micro.client.mqttv3.MqttMessage;
import com.ibm.micro.client.mqttv3.MqttTopic;

public class PubSync {
	public static String doTest() {
		try {
			MqttClient client = new MqttClient("tcp://120.197.98.61:1883",
					"java_client", null);
			MqttTopic topic = client.getTopic("MQTT Example");
			MqttMessage message = new MqttMessage(
					"Hello World. Hello IBM".getBytes());
			message.setQos(1);
			client.connect();
			MqttDeliveryToken token = topic.publish(message);
			while (!token.isComplete()) {
				token.waitForCompletion(1000);
			}
			client.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			return "failed";
		}
		return "success";
	}
}
