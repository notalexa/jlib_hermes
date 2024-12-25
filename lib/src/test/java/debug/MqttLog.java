/*
 * Copyright (C) 2024 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package debug;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import not.alexa.hermes.mqtt.HermesMqtt;
import not.alexa.hermes.mqtt.HermesServer;

/**
 * Simple class for outputing mqtt content.
 * <p>Up to 2 arguments can be defined:
 * <ul>
 * <li>Argument 1 is the host to connect to (defaults to {@code localhost}).
 * <li>Argument 2 is the port to connect to (defaults to {@code 1883}).
 * </ul>
 * 
 * @author notalexa
 * 
 */
public class MqttLog {
	private String url;
	public MqttLog(String url) {
		this.url=url;
	}
	
	public void log() throws Throwable {
		IMqttClient client=HermesMqtt.createClient(url);
		client.setCallback(new MqttCallbackExtended() {
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				if(!topic.contains("audioServer")) {
					System.out.println(topic+"\n"+new String(message.getPayload()));
				} else {
					System.out.println(topic);
				}
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
				System.out.println("Connection lost: "+cause.getMessage());
			}
			
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				System.out.println("Connection completed: "+reconnect+", "+serverURI);
			}
		});
		client.connect(HermesServer.createConnectOptions(true));
		client.subscribe("#");
	}
	
	public static void main(String[] args) throws Throwable {
		String url=args.length==0?"tcp://localhost:1883":args.length==1?"tcp://"+args[0]+":1883":("tcp://"+args[0]+":"+args[1]);
		System.out.println("Connecting to "+url);
		MqttLog log=new MqttLog(url);
		log.log();
		Thread.sleep(Long.MAX_VALUE);
	}

}
