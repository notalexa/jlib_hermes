package not.alexa.hermes.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.PublishTest.AudioDeviceRequestHandler;
import not.alexa.hermes.PublishTest.HotwordsRequestHandler;
import not.alexa.hermes.PublishTest.HotwordsToggleOff;
import not.alexa.hermes.PublishTest.HotwordsToggleOn;
import not.alexa.hermes.PublishTest.PhonemesRequestHandler;
import not.alexa.hermes.PublishTest.Resource;
import not.alexa.hermes.PublishTest.VoicesRequestHandler;
import not.alexa.hermes.features.FeaturesRequest;
import not.alexa.hermes.hotword.HotwordsRequest;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.DefaultTypeLoader;

//@ExtendWith(MockitoExtension.class)
public class HermesMqttTest {
	
	public HermesMqttTest() {
	}
	
	@BeforeEach
	public void init() {
	    MockitoAnnotations.openMocks(this);
	}
	
	@Mock
	MqttClient mqttClient;
	
	public HermesMqtt prepareApi(Object resource,Class<?>...overlays) {
		List<Class<?>> allOverlays=new ArrayList<>();
		allOverlays.add(HermesApi.FeaturesRequestHandler.class);
		allOverlays.add(VoicesRequestHandler.class);
		allOverlays.add(PhonemesRequestHandler.class);
		allOverlays.add(HotwordsToggleOn.class);
		allOverlays.add(HotwordsToggleOff.class);
		allOverlays.add(HotwordsRequestHandler.class);
		allOverlays.add(AudioDeviceRequestHandler.class);
		allOverlays.addAll(Arrays.asList(overlays));
		Context context=new DefaultTypeLoader().overlay(allOverlays).createContext();
		if(resource!=null) {
			context.putAdapter(resource);
		}
		return new HermesMqtt(context,"default");
	}

	@Test
	public void test1() {
		try {
			MqttClient client=HermesMqtt.createClient("tcp://localhost:8123");
			assertNotNull(client);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void test2() {
		Resource resource=new Resource();
		try {
			HermesMqtt api=prepareApi(resource,Resource.FeaturesHandler.class).subscribeTo(mqttClient);
			Mockito.doAnswer(new Answer<Void>() {
				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					api.messageArrived(invocation.getArgument(0),invocation.getArgument(1));
					return null;
				}
			}).when(mqttClient).publish(anyString(), any(MqttMessage.class));
			new FeaturesRequest().publish(api);
			new HotwordsRequest("siteId").getHotwords(api);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	@Test
	public void test3() {
		try {
			HermesMqtt api=prepareApi(null).subscribeTo(null);
			fail();
		} catch(NullPointerException|BaseException e) {
			
		}
		
	}
	
	@Test
	public void test4() {
		try {
			Mockito.when(mqttClient.getServerURI()).thenReturn("tcp://localhost:8123");
			HermesMqtt api=prepareApi(null).subscribeTo(mqttClient);
			api.subscribeTo(mqttClient);
			fail();
		} catch(NullPointerException|BaseException e) {
			assertEquals("Api already subscribed to tcp://localhost:8123",e.getMessage());
		}
		
	}
}
