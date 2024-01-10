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
 * Audio stream based on data in an array
 * 
 * @author notalexa
 */
public class ByteArrayAudioStream implements AudioStream {
	private AudioFormat format;
	private int offset;
	private byte[] data;
	

	public ByteArrayAudioStream(AudioFormat format,int headerOffset,byte[] data) {
		offset=headerOffset;
		this.data=data;
		this.format=format;
	}


	@Override
	public AudioFormat getFormat() {
		return format;
	}


	@Override
	public int next() throws IOException {
		if(offset>=data.length) {
			return Integer.MAX_VALUE;
		}
		offset+=2;
		return (data[offset-2]&0xff)+(data[offset-1]<<8);
	}


	@Override
	public int update(boolean primary,int totalVolume,int volume, byte[] buffer, int offset, int length) throws IOException {
		if(primary) {
			// No transformation needed
			length=Math.min(length, data.length-this.offset);
			if(length>0) {
				System.arraycopy(data, this.offset, buffer, offset, length);
				this.offset+=length;
				return length;
			} else {
				return -1;
			}
		}
		return AudioStream.super.update(primary,totalVolume,volume, buffer, offset, length);
	}


	@Override
	public boolean isStream() {
		return false;
	}
}
