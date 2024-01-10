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
package not.alexa.hermes.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioFormat;

import not.alexa.hermes.media.streams.Silence;

/**
 * Class representing an audio stream consisting of several (typically short) streams. This class
 * is used to mix in additional streams into the main stream by {@link MasterStream}. Streams in this
 * class are for example generated using TTS software.
 * 
 * @see MasterStream
 */
public class SecondaryLines implements AudioStream {
	private AudioStream first;
	private List<AudioStream> streams=new ArrayList<>();
	private int sampleSize;
	private Set<Integer> remove=new HashSet<>();
	private AudioFormat format;
	
	public SecondaryLines(AudioFormat format) {
		this.format=format;
	}
	
	public int next() throws IOException {
		int accu=first==null?0:first.next();
		int audioAccu=0;
		if(accu==Integer.MAX_VALUE) {
			remove.add(-1);
			first=new Silence(first.getFormat());
		} else {
			audioAccu=accu;
		}
		switch(sampleSize) {
			case 0:return Integer.MAX_VALUE;
			case 1:return accu;
			default:for(int i=0;i<sampleSize-1;i++) {
					int a=streams.get(i).next();
					if(a==Integer.MAX_VALUE) {
						streams.set(i,new Silence(streams.get(i).getFormat()));
						remove.add(i);
					} else {
						audioAccu+=a;
					}
				}
				return audioAccu/sampleSize;
		}
	}
	
	public boolean hasStreams() {
		return first!=null;
	}
	
	public boolean update() {
		if(first!=null) {
			if(!remove.isEmpty()) for(Integer i:remove) {
				if(i<0) {
					if(streams.size()>0) {
						first=streams.remove(0);
					} else {
						first=null;
					}
				} else {
					streams.remove(i.intValue());
				}
			}
			remove.clear();
			if(first==null) {
				sampleSize=0;
			} else {
				sampleSize=streams.size()+1;
			}
		} else {
			sampleSize=0;
		}
		return !hasStreams();
	}
	
	public void add(AudioStream stream) {
		if(first==null) {
			first=stream;
			sampleSize=1;
		} else {
			streams.add(stream);
		}
	}
	
	public SecondaryLines resample(AudioFormat format) {
		if(first!=null) {
			first=first.resample(format);
			for(int i=0;i<streams.size();i++) {
				streams.set(i,streams.get(i).resample(format));
			}
		}
		return this;
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}

	@Override
	public boolean isStream() {
		return false;
	}
}
