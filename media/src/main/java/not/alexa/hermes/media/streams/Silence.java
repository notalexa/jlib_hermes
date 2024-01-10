
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
package not.alexa.hermes.media.streams;

import java.io.IOException;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import not.alexa.hermes.media.AudioStream;

/**
 * Silence as an audio stream
 * 
 * @author notalexa
 */
public class Silence implements AudioStream {
	private AudioFormat format;
	
	public Silence() {
		this(new AudioFormat(44100f, 16, 2, true, false));
	}
	
	public Silence(AudioFormat format) {
		this.format=format;
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}

	@Override
	public int next() throws IOException {
		return 0;
	}

	@Override
	public boolean isStream() {
		return true;
	}

	@Override
	public int update(boolean primary,int totalVolume,int volume, byte[] buffer, int offset, int length) throws IOException {
		if(primary||totalVolume==volume) {
			Arrays.fill(buffer, offset, offset+length, (byte)0);
			return length;
		} else {
			// This silence reduces the volume of the underlying audio stream
			return AudioStream.super.update(primary,totalVolume, volume, buffer, offset, length);
		}
	}

	@Override
	public AudioStream resample(AudioFormat format) {
		if(format.equals(this.format)) {
			return this;
		} else {
			return new Silence(format);
		}
	}
}
