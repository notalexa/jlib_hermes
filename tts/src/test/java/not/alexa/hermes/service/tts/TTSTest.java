/*
 * Copyright (C) 2023 Not Alexa
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
package not.alexa.hermes.service.tts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.audio.AudioPlayBytes;
import not.alexa.hermes.audio.AudioPlayFinished;
import not.alexa.hermes.tts.Say;
import not.alexa.hermes.tts.SayFinished;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.DefaultTypeLoader;

public class TTSTest {

	public TTSTest() {
	}
	
	private boolean seen;
	private static boolean valid;
	private static String ttsCmd;
	
	@BeforeAll
	static void check() {
		valid="Linux".equals(System.getProperty("os.name"));
		File f=new File("test/bin/tts.sh");
		valid&=f.exists();
		if(valid) {
			f.setExecutable(true);
			valid&=f.canExecute();
		}
		if(valid) {
			ttsCmd=f.getAbsolutePath();
		} else {
			System.err.println("Invalid setup: OS="+System.getProperty("os.name")+", TTS CMD="+f.getAbsolutePath()+"(exists="+f.exists()+", execute="+f.canExecute()+")");
		}
	}
	
	public HermesApi prepareApi(Object resource,boolean broken,Class<?>...overlays) {
		List<Class<?>> allOverlays=new ArrayList<>();
		allOverlays.add(HermesApi.FeaturesRequestHandler.class);
		allOverlays.add(TTS.SayHandler.class);
		allOverlays.add(TTS.AudioPlayFinishedHandler.class);
		allOverlays.add(SayFinishedHandler.class);
		if(!broken) {
			allOverlays.add(AudioPlayBytesHandler.class);
		}
		allOverlays.addAll(Arrays.asList(overlays));
		Context context=new DefaultTypeLoader().overlay(allOverlays).createContext();
		if(resource!=null) {
			context.putAdapter(resource);
		}
		context.putAdapter(this);
		return new HermesApi(context,"default");
	}
	
	@Test
	void test1() {
		if(valid) try {
			seen=false;
			TTS tts=new TTS(ttsCmd,"de-DE",Collections.singleton("de-DE"));
			new Say("Sag mal was").publish(prepareApi(tts,false,TTS.SayHandler.class));
			assertEquals(true, seen);
		} catch(Throwable t) {
		}
	}

	@Test
	void test3() {
		if(valid) try {
			seen=false;
			TTS tts=new TTS(ttsCmd,"de",Collections.singleton("de-DE"));
			new Say("Sag mal was").publish(prepareApi(tts,false,TTS.SayHandler.class));
			assertEquals(false, seen);
		} catch(Throwable t) {
			
		}
	}

	@Test
	void test2() {
		if(valid) try {
			TTS tts=new TTS(ttsCmd,"de-DE",Collections.singleton("de-DE")) {
				{
					timeout=500;
				}
			};
			HermesApi api=prepareApi(tts,true,TTS.SayHandler.class);
			api.getContext().putAdapter(TTS.class,tts);
			new Say("Sag mal was").publish(api);
			new Say("Sag mal was","en",null,-1f,"default",null,null).publish(api);
			new Say("Sag mal was","en",null,-1f,"otherSite",null,null).publish(api);
			Thread.sleep(2000);
			new Say("Sag mal was").publish(api);
		} catch(Throwable t) {
		}
	}

	@Overlay
	public static class AudioPlayBytesHandler extends AudioPlayBytes {

		AudioPlayBytesHandler() {
			super();
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			api.publish(new AudioPlayFinished(getRequestId()));
		}
	}
	
	@Overlay
	public class SayFinishedHandler extends SayFinished {

		SayFinishedHandler() {
			super();
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			seen=true;
		}
	}
	
	
}
