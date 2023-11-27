package not.alexa.hermes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi.Device.Mode;
import not.alexa.hermes.audio.AudioDevicesRequest;
import not.alexa.hermes.features.Features;
import not.alexa.hermes.features.FeaturesRequest;
import not.alexa.hermes.g2p.PhonemesRequest;
import not.alexa.hermes.hotword.HotwordsRequest;
import not.alexa.hermes.hotword.ToggleOff;
import not.alexa.hermes.hotword.ToggleOn;
import not.alexa.hermes.hotword.Hotwords.Model;
import not.alexa.hermes.g2p.Phonemes.Result;
import not.alexa.hermes.tts.VoicesRequest;
import not.alexa.hermes.tts.Voices.Voice;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.DefaultTypeLoader;

public class PublishTest {

	public PublishTest() {
	}
	
	public HermesApi prepareApi(Object resource,Class<?>...overlays) {
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
		return new HermesApi(context,"default");
	}
	
	@Test
	void test0() {
		assertEquals("default", prepareApi(null).getSiteId());
	}
	
	@Test
	void publishTest1() {
		Resource resource=new Resource();
		try {
			new FeaturesRequest().publish(prepareApi(resource,Resource.FeaturesHandler.class));
			assertNotNull(resource.answer);
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	void publishTest3() {
		try {
			HermesApi api=prepareApi(null);
			api.query("words of input");
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	void publishTest4() {
		try {
			HermesApi api=prepareApi(null);
			assertNotNull(api.getVoices());
			api.getVoices();
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	void publishTest5() {
		try {
			HermesApi api=prepareApi(null);
			assertNotNull(api.getPhonemes("hello"));
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	void publishTest6() {
		try {
			HermesApi api=prepareApi(null);
			assertNotNull(api.getHotwords());
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
	}

	@Test
	void publishTest7() {
		try {
			HermesApi api=prepareApi(null);
			assertNotNull(api.getDevices());
		} catch(BaseException e) {
			e.printStackTrace();
			fail();
		}
	}


	@Test
	void timeoutTest() {
		try {
			new HotwordsRequest("siteId").getHotwords(new HermesApi(Context.createRootContext()));
			fail();
		} catch(BaseException e) {
			assertEquals("Timeout",e.getMessage());
		}
	}
	
	public static class Resource {
		Features answer;
		@Overlay
		public class FeaturesHandler extends Features {
			@Override
			public void received(HermesApi api) throws BaseException {
				answer=this;
			}
		}
	}
	
	@Overlay
	public static class VoicesRequestHandler extends VoicesRequest {

		public VoicesRequestHandler(@JsonProperty("siteId") String siteId) {
			super(siteId);
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			api.publish(createAnswer(new Voice[] { new Voice("id","de","voice")}));
		}
	}
	
	@Overlay
	public static class AudioDeviceRequestHandler extends AudioDevicesRequest {

		public AudioDeviceRequestHandler(@JsonProperty("modes")  Mode[] modes,@JsonProperty("siteId")  String siteId,@JsonProperty("test")  boolean test) {
			super(modes, siteId, test);
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			api.publish(createAnswer(new HermesApi.Device[] {new HermesApi.Device(Mode.input, "deviceId1"),
					new HermesApi.Device(Mode.input, "deviceId2",null,null,false),
					new HermesApi.Device(Mode.output, "deviceId3",null,null,true)}));
		}
	}

	
	@Overlay
	public static class HotwordsToggleOn extends ToggleOn {
	}

	@Overlay
	public static class HotwordsToggleOff extends ToggleOff {
	}

	@Overlay
	public static class HotwordsRequestHandler extends HotwordsRequest {

		public HotwordsRequestHandler(@JsonProperty("siteId") String siteId) {
			super(siteId);
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			api.publish(createAnswer(new Model[] { new Model("id",new String[] { "wakeup"},"version","personal")}));
		}
	}

	@Overlay
	public static class PhonemesRequestHandler extends PhonemesRequest {

		PhonemesRequestHandler() {
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			api.publish(createAnswer(Collections.singletonMap("example", new Result[] {new Result(new String[] { "e" },false)})));
		}
	}

}
