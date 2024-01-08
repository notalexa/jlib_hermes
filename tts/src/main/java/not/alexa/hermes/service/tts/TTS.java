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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.AudioFormat;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesComponent;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.audio.AudioPlayBytes;
import not.alexa.hermes.audio.AudioPlayFinished;
import not.alexa.hermes.tts.Say;
import not.alexa.hermes.tts.SayFinished;
import not.alexa.hermes.tts.TTSError;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.api.Overlay;

/**
 * Implementation of the text to speech feature in the hermes API.
 * The implementation expects the following setup:
 * <ul>
 * <li>{@code script} denotes the script which generates the WAV output (defaults to {@code tts.sh}).
 * <li>{@code defaultLanguage} denotes the language if the provided language is {@code null} or unknown.
 * <li>{@code languages} denotes a set of accepted languages
 * </ul>
 * Tested with {@code nanotts} in which case the script should look like
 * <pre>
 * #!/bin/bash
 * echo "$2" | nanotts -l ~/lib/nanotts/pico/lang -w -o /tmp/$$.wav -v $1 && \
 * cat /tmp/$$.wav && rm /tmp/$$.wav
 * </pre>
 * and the (YAML) setup is
 * <pre>
 * script: tts.sh # optional
 * defaultLanguage: de-DE
 * languages:
 * - de-DE
 * - en-US
 * - en-GB
 * - es-ES
 * - fr-FR
 * - it-IT
 * </pre>
 * 
 * @author notalexa
 *
 */
public class TTS implements HermesComponent {
	@JsonProperty(defaultValue = "tts.sh") String script;
	@JsonProperty(required = true) String defaultLanguage;
	@JsonProperty(required = true) Set<String> languages;
	
	long timeout=30000;
	
	private Map<String,Long> pending=new LinkedHashMap<>();
	
	TTS() {
	}
	
	public TTS(String script,String defaultLanguage,Set<String> languages) {
		this.script=script;
		this.defaultLanguage=defaultLanguage;
		this.languages=languages;
	}
	

	@Override
	public void configure(Map<String,Class<? extends HermesMessage<?>>> extensions,Map<Class<?>, Object> resources, List<Class<? extends HermesMessage<?>>> overlays) {
		resources.put(TTS.class,this);
		overlays.add(SayHandler.class);
		overlays.add(AudioPlayFinishedHandler.class);
	}

	/**
	 * Create a voice representation of the given text.
	 * 
	 * @param lang the language to use
	 * @param text the text to spec
	 * @return a wav representation of the text based on the given configuration
	 * @throws BaseException if an error occurs
	 */
	public byte[] say(String lang,String text) throws BaseException {
		try {
			if(!languages.contains(lang)) {
				lang=defaultLanguage;
			}
			Process process=new ProcessBuilder().command(script,lang,text).start();
			ScriptOutput stdErr=new ScriptOutput(process.getErrorStream());
			ScriptOutput stdOut=new ScriptOutput(process.getInputStream());
 			process.waitFor();
 			if(process.exitValue()==0) {
 				return stdOut.getContent();
 			} else {
 				throw new BaseException(BaseException.BAD_REQUEST, new String(stdErr.getContent()));
 			}
		} catch(Throwable t) {
			return BaseException.throwException(t);
		}
	}
	
	/**
	 * For the given (WAV-) data, calculate the expected time.
	 * 
	 * @param data the wav data
	 * @return the time this data needs to be spoken
	 */
	public float getTime(byte[] data) {
		AudioFormat format=WavHeader.getFormat(data);
		if(format!=null) {
			return (8*data.length)/(format.getSampleRate()*format.getChannels()*format.getSampleSizeInBits());
		}
		return -1f;
	}
	
	@Overlay
	public class SayHandler extends Say {
		@Override
		public void received(HermesApi api) throws BaseException {
			if(!pending.isEmpty()) for(Iterator<Map.Entry<String,Long>> entries=pending.entrySet().iterator();entries.hasNext();) {
				if(entries.next().getValue()<System.currentTimeMillis()-timeout) {
					entries.remove();
				} else {
					break;
				}
			}
			if(api.matches(getSiteId())) try {
				byte[] wavData=say(getLang()==null?defaultLanguage:getLang(),getText());
				if(getId()!=null) {
					float time=getTime(wavData);
					pending.put(getId(), System.currentTimeMillis()+(long)(1000*Math.max(time,0f)));
				}
				api.publish(new AudioPlayBytes(getSiteId(), getId()==null?HermesApi.createId():getId(), wavData));
			} catch(Throwable t) {
				api.getContext().getLogger().error("Say failed",t);
				api.publish(new TTSError(t.getMessage()));
			}
		}
	}
	
	@Overlay
	public class AudioPlayFinishedHandler extends AudioPlayFinished {

		public AudioPlayFinishedHandler(@JsonProperty("requestId") String requestId) {
			super(requestId);
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			if(getRequestId()!=null&&pending.remove(getRequestId())!=null) {
				api.publish(new SayFinished(api.getSiteId(),getRequestId()));
			}
		}	
	}
	
	private class ScriptOutput extends Thread {
		InputStream in;
		ByteArrayOutputStream out=new ByteArrayOutputStream();
		ScriptOutput(InputStream in) {
			this.in=in;
			setDaemon(true);
			start();
		}
		
		@Override
		public void run() {
			byte[] buffer=new byte[1024];
			int n;
			try {
				while((n=in.read(buffer))>=0) {
					out.write(buffer,0,n);
				}
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
		
		byte[] getContent() {
			return out.toByteArray();
		}
	}
}
