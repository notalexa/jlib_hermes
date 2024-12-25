package not.alexa.hermes.mqtt;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesComponent;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;

/**
 * Attach the hermes API to an MQTT instance (the typical case).
 * <br>Use {@link #subscribeTo(MqttClient)} to subscribe to an MQTT instance using the given client.
 * 
 * @author notalexa
 *
 */
public class HermesMqtt extends HermesApi implements IMqttMessageListener {
	private static Logger LOGGER=LoggerFactory.getLogger(HermesMqtt.class);
	private IMqttClient client;
	
	public HermesMqtt(Context context,HermesComponent...components) {
		super(context,components);
	}
	
	public HermesMqtt(Context context,String siteId,HermesComponent...components) {
		super(context,siteId,components);
	}
	
	/**
	 * Subscribe to an MQTT instance
	 * 
	 * @param client the client to use
	 * @return this instance
	 * @throws BaseException if an error occurs
	 */
	public HermesMqtt subscribeTo(IMqttClient client) throws BaseException {
		if(this.client!=null) {
			throw new BaseException(BaseException.FORBIDDEN,"Api already subscribed to "+client.getServerURI());
		}
		if(client==null) {
			throw new NullPointerException("MqttClient");
		}
		this.client=client;
		if(topics.length>0) try {
			IMqttMessageListener[] callbacks=new IMqttMessageListener[topics.length];
			for(int i=0;i<callbacks.length;i++) {
				callbacks[i]=createListener(topics[i]);
			}
			client.subscribe(topics,callbacks);
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
		client.setCallback(new MqttCallbackExtended() {
			@Override
			public void connectionLost(Throwable cause) {
				LOGGER.warn("Connection to {} lost ({})",client.getServerURI(),cause==null?'?':cause.getMessage()==null?cause.getClass().getSimpleName():cause.getMessage());
			}
			
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				LOGGER.info("Connection to {} established (reconnect={})",serverURI,reconnect);
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
		});
		return this;
	}

	@Override
	public void publish(String topic,byte[] msg) throws BaseException {
		if(client!=null) try {
			client.publish(topic, new MqttMessage(msg));
		} catch(Throwable t) {
			BaseException.throwException(t);
		}
	}

	public static MqttClient createClient(String uri) throws BaseException {
		try {
			return new MqttClient(uri, UUID.randomUUID().toString().substring(0,23),new MemoryPersistence());
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			received(topic,message.getPayload());
		} catch(Throwable t) {
		}
	}

	@Override
	protected void addTopic(String topic) {
		super.addTopic(topic);
		try {
			client.subscribe(topic,createListener(topic));
		} catch(Throwable t) {
		}
	}
	
	protected IMqttMessageListener createListener(String topic) {
		Subscriber subscriber=subscribers.get(topic);
		if(subscriber==null) {
			return this;
		} else {
			return new IMqttMessageListener() {
				@Override
				public void messageArrived(String topic, MqttMessage message) throws Exception {
					subscriber.received(HermesMqtt.this, topic, message.getPayload());
				}
			};
		}
	}
}
