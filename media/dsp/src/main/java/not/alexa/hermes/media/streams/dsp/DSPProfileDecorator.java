/*
 * Copyright (C) 2025 Not Alexa
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
package not.alexa.hermes.media.streams.dsp;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioSink;
import not.alexa.hermes.media.MasterStream;
import not.alexa.hermes.media.streams.SourceDataLineDecorator;
import not.alexa.hermes.media.streams.dsp.DSP.Profile;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;

/**
 * Source data line decorator which enables DSP profiles <b>on the local machine</b>.
 * <br>If the default profile is configured, any format without a profile property set activates this default profile. Otherwise, the dsp is reset if no profile property is set.
 * 
 * @author notalexa
 * 
 */
public class DSPProfileDecorator implements SourceDataLineDecorator {
	@JsonProperty(value="default") String defaultProfile;
	public static final LineEvent.Type LOCKED=new LineEvent.Type("LOCKED") {};
	public static final LineEvent.Type UNLOCKED=new LineEvent.Type("UNLOCKED") {};
	private Map<String, Profile> profiles=new HashMap<>();
	private DSP dsp;

	public DSPProfileDecorator() {
	}
	
	public Profile getProfile(String profile) {
		return dsp==null?null:profiles.computeIfAbsent(profile,p -> {
			try(InputStream stream=DSPSourceDataLine.class.getClassLoader().getResourceAsStream("profiles/"+profile+".xml")) {
				return stream==null?null:dsp.load(Context.createRootContext(), stream);
			} catch(IOException|BaseException e) {
				e.printStackTrace();
				return null;
			}
		});
	}
	
	private void resetInternal(boolean force) throws IOException {
		Profile profile=defaultProfile==null?null:getProfile(defaultProfile);
		if(profile!=null) try {
			dsp.apply(profile);
			return;
		} catch(Throwable t) {
		}
		dsp.reset(force);
	}
	
	@Override
	public void init(AudioSink sink,MasterStream stream) {
		dsp=new DSP("localhost",8086);
		try {
			dsp.connect();
			resetInternal(true);
		} catch(Throwable t) {
		}
	}
	
	DSP getDSP() {
		return dsp;
	}
	
	@Override
	public void close() {
		try {
			dsp.reset();
		} catch(Throwable t) {
		}
	}

	@Override
	public SourceDataLine decorate(SourceDataLine line) {
		Object profile=line.getFormat().getProperty("profile");
		if(profile!=null) {
			Profile resolvedProfile=getProfile(profile.toString());
			if(resolvedProfile!=null) {
				return new DSPSourceDataLine(this,line, resolvedProfile);
			}
		}
		try {
			resetInternal(false);
		} catch(Throwable t) {
		}
		return line;
	}

	public void close(DSPSourceDataLine profiledSourceDataLine) throws IOException {
		resetInternal(false);
	}

	public boolean apply(Profile profile) throws IOException {
		dsp.apply(profile);
		return true;
	}

	@Override
	public void dispose(SourceDataLine line) {
		if(line instanceof DSPSourceDataLine) {
			((DSPSourceDataLine)line).dispose();
		}
	}
}
