package not.alexa.hermes.intent.handling;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.hermes.nlu.Query;
import not.alexa.hermes.nlu.Reply;
import not.alexa.hermes.tts.Say;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.DefaultTypeLoader;

public class IntentHandlingTest {

	public IntentHandlingTest() {
	}

	public HermesApi prepareApi(Object resource,Class<?>...overlays) {
		List<Class<?>> allOverlays=new ArrayList<>();
		allOverlays.add(ReplyHandler.class);
		allOverlays.add(SayHandler.class);
		Context context=new DefaultTypeLoader().overlay(allOverlays).createContext();
		if(resource!=null) {
			context.putAdapter(resource);
		}
		Stack stack=new Stack(new LogHandler(),new DateTimeHandler());
		return new HermesApi(context,"default",stack);
	}
	
	private String say;
	private String reply;
	
	@Test
	void baseTest() {
		HermesApi api=prepareApi(null);
		NLUIntent intent=Query.createBuilder("input").build().createAnswer().setIntent("unsupportedIntent").build();
		try {
			api.publish(intent);
			assertNull(say);
			assertNull(reply);
		} catch(Throwable t) {
			t.printStackTrace();
		}

	}

	@Test
	void sayTimeTest() {
		HermesApi api=prepareApi(this);
		NLUIntent intent=Query.createBuilder("input").build().createAnswer().setIntent("currentTime").build();
		try {
			api.publish(intent);
			assertNull(reply);
			assertNotNull(say);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	void replyTimeTest() {
		HermesApi api=prepareApi(this);
		NLUIntent intent=Query.createBuilder("input").addInitialSlot(new Slot("reply-to","xxx")).build().createAnswer().setIntent("currentTime").build();
		try {
			api.publish(intent);
			assertNull(say);
			assertNotNull(reply);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	void sayDateTest() {
		HermesApi api=prepareApi(this);
		NLUIntent intent=Query.createBuilder("input").build().createAnswer().setIntent("currentDate").build();
		try {
			api.publish(intent);
			assertNull(reply);
			assertNotNull(say);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	void replyDateTest() {
		HermesApi api=prepareApi(this);
		NLUIntent intent=Query.createBuilder("input").addInitialSlot(new Slot("reply-to","xxx")).build().createAnswer().setIntent("currentDate").build();
		try {
			api.publish(intent);
			assertNull(say);
			assertNotNull(reply);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	void toggleOfTest() {
		HermesApi api=prepareApi(this);
		NLUIntent intent=Query.createBuilder("input").addInitialSlot(new Slot("reply-to","xxx")).build().createAnswer().setIntent("currentDate").build();
		try {
			new ToggleOff().publish(api);
			api.publish(intent);
			assertNull(say);
			assertNull(reply);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	void toggleOnTest() {
		HermesApi api=prepareApi(this);
		NLUIntent intent=Query.createBuilder("input").addInitialSlot(new Slot("reply-to","xxx")).build().createAnswer().setIntent("currentDate").build();
		try {
			new ToggleOff().publish(api);
			new ToggleOn().publish(api);
			api.publish(intent);
			assertNull(say);
			assertNotNull(reply);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	@Overlay
	public class ReplyHandler extends Reply  {

		@Override
		public void received(HermesApi api) throws BaseException {
			if(getCallerId().equals("xxx")) {
				reply=getText();
			}
		}
	}
	
	@Overlay
	public class SayHandler extends Say  {

		@Override
		public void received(HermesApi api) throws BaseException {
			say=getText();
		}
	}

}
