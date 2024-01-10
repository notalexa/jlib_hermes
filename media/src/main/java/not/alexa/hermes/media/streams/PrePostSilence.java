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

import javax.sound.sampled.AudioFormat;

import not.alexa.hermes.media.AudioStream;

/**
 * Add pre and post silence to the given audio stream.
 */
public class PrePostSilence implements AudioStream {
	private int preSilence;
	private int postSilence;
	private AudioStream stream;
	private int countDown;
	
	public PrePostSilence(float preSilence,float postSilence,AudioStream stream) {
		this.preSilence=(int)(preSilence*stream.getFormat().getSampleRate()*stream.getFormat().getChannels());
		this.postSilence=-(int)(postSilence*stream.getFormat().getSampleRate()*stream.getFormat().getChannels());
		this.countDown=this.preSilence;
		this.stream=stream;
	}

	@Override
	public AudioFormat getFormat() {
		return stream.getFormat();
	}

	@Override
	public int next() throws IOException {
		if(countDown>0) {
			countDown--;
			return 0;
		} else if(countDown<0) {
			if(countDown<=postSilence) {
				return Integer.MAX_VALUE;
			} else {
				countDown--;
				return 0;
			}
		} else {
			int n=stream.next();
			if(n==Integer.MAX_VALUE) {
				countDown--;
				if(postSilence==0) {
					return Integer.MAX_VALUE;
				} else {
					return 0;
				}
			}
			return n;
		}
	}

	@Override
	public boolean isStream() {
		return stream.isStream();
	}
}
