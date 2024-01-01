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

import javax.sound.sampled.AudioFormat;

/**
 * Analyse a WAV header
 * 
 * @author notalexa
 *
 */
public class WavHeader {

	private WavHeader() {
	}
	
	/**
	 * Return the {@code AudioFormat} of the data (in WAV format)
	 * 
	 * @param wavData the data
	 * @return the audio format of the data
	 */
	public static AudioFormat getFormat(byte[] wavData) {
		int offset=0;
		outerloop: while(offset<wavData.length-4) {
			for(Part part:Part.values()) {
				byte[] id=part.getId();
				if(wavData[offset]==id[0]&&wavData[offset+1]==id[1]&&wavData[offset+2]==id[2]&&wavData[offset+3]==id[3]) {
					switch(part) {
					case RIFF:offset+=12; continue outerloop;
					case FRMT: //int format=length2(offset+8,wavData);
						int channels=length2(offset+10,wavData);
						int sampleRate=length4(offset+12,wavData);
						//int bytesPerSecond=length4(offset+16,wavData);
						int blockAlign=length2(offset+20,wavData);
						int bitsPerSample=length2(offset+22,wavData);
						AudioFormat audioFormat=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, bitsPerSample, channels, blockAlign, sampleRate,false);
						return audioFormat;
					default:
						break;
					}
				}
			}
			break;
		}
		return null;
	}
	
	private static int length2(int offset,byte[] wav) {
		int l=wav[offset+1]&0xff;
		l=(l<<8)+(wav[offset+0]&0xff);
		return l;
	}
	
	private static int length4(int offset,byte[] wav) {
		return (length2(offset+2,wav)<<16)+length2(offset,wav);
	}

	private enum Part {
		RIFF("RIFF",12), FRMT("fmt ",24),DATA("data",-1);
		int length;
		byte[] id;
		private Part(String name,int length) {
			this.length=length;
			id=name.getBytes();
		}
		
		@SuppressWarnings("unused")
		public int getLength() {
			return length;
		}
		public byte[] getId() {
			return id;
		}
	}
}
