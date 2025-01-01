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
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
 * The link between {@link AudioStream} and {@link AudioSink}. The master is created using the {@link AudioStream#createControls(AudioSink)} method and
 * provides the basic {@link AudioControls}.
 * 
 * @author notalexa
 */
public class MasterStream implements AudioControls, LineListener {
	private AudioStream stream;
	private AudioFormat format;
	private AudioSink sink;
	private SecondaryLines secondaryLines;
	private int volume=256;
	private float normalizedVolume=1f;
	private boolean pausing;
	private Object pauseLock=new Object();
	private Object streamLock=new Object();
	private AudioControls controls=new AudioControls() {
		
		@Override
		public Object getStreamLock() {
			return MasterStream.this.getStreamLock();
		}

		@Override
		public void setVolume(float vol) {
			MasterStream.this.setVolume(false,vol);
		}
		
		@Override
		public void resume() {
			MasterStream.this.resume(false);
		}
		
		@Override
		public void requestFocus(String uri) {
		}
		
		@Override
		public void pause() {
			MasterStream.this.pause(false);
		}
		
		@Override
		public boolean isPlaying() {
			return MasterStream.this.isPlaying();
		}
		
		@Override
		public float getVolume() {
			return MasterStream.this.getVolume();
		}
		
		@Override
		public void flush() {
			MasterStream.this.flush();
		}

		@Override
		public void onAudioFormatChanged(AudioFormat format) {
			MasterStream.this.onAudioFormatChanged(format);
		}

		@Override
		public void close() {
			// We do not allow closing from inside
		}

		@Override
		public void addStream(AudioStream stream) {
			MasterStream.this.addStream(stream);
		}
	};
	
	protected MasterStream(AudioStream stream) {
		this.stream=stream;
		pausing=true;
		stream.setAudioControls(controls);
		this.format=stream.getFormat();
	}
	
	@Override
	public void update(LineEvent e) {
		if(stream!=null) {
			stream.update(e);
		}
	}
	
	public MasterStream attach(AudioSink sink) {
		this.sink=sink;
		normalizedVolume=sink.getVolume();
		secondaryLines=new SecondaryLines(stream.getFormat());
		stream.onAudioStateChanged();
		return this;
	}
	
	public void detach() {
		stream.close();
		sink=null;
		secondaryLines=null;
	}
	
	@Override
	public Object getStreamLock() {
		return streamLock;
	}

	@Override
	public void onAudioFormatChanged(AudioFormat format) {
		synchronized(streamLock) {
			this.format=format;
			secondaryLines.resample(format);
		}
	}

	public AudioFormat getFormat() {
		return stream.getFormat();
	}
	
	@Override
	public void resume() {
		resume(true);
	}
	
	private void resume(boolean generateEvents) {
		if(sink!=null) {
			boolean p=pausing;
			synchronized(pauseLock) {
				pausing=false;
				sink.resume();
			}
			if(p!=pausing&&generateEvents) {
				stream.onAudioStateChanged();
			}
		}
	}

	@Override
	public void pause() {
		pause(true);
	}
	
	private void pause(boolean generateEvents) {
		if(sink!=null) {
			boolean p=pausing;
			synchronized(pauseLock) {
				pausing=true;
				sink.pause();
			}
			if(p!=pausing&&generateEvents) {
				stream.onAudioStateChanged();
			}
		}
	}
	
	public boolean hasSecondaries() {
		return secondaryLines.hasStreams();
	}
	
	@Override
	public void setVolume(float volume) {
		setVolume(true,volume);
		
	}
	public void setVolume(boolean generateEvents,float volume) {
		if(sink!=null) {
			volume=Math.max(0,Math.min(volume, 1f));
			if(volume!=normalizedVolume) {
				normalizedVolume=volume;
				if(sink.setVolume(volume)) {
					this.volume=256;
				} else {
					this.volume=(int)(256*normalizedVolume);
				}
				if(generateEvents) {
					stream.onAudioStateChanged();
				}
			}
		}
	}
	
	@Override
	public float getVolume() {
		return normalizedVolume;
	}

	@Override
	public void addStream(AudioStream secondaryStream) {
		if(sink!=null) {
			secondaryLines.add(secondaryStream.resample(stream.getFormat()));
			sink.resume();
		}
	}

	public int read(AudioFormat format,byte[] buffer, int offset, int length) throws IOException {
		synchronized(streamLock) {
			if(this.format==format) {
				return update(volume==256,volume,volume,buffer,offset,length);
			} else if(this.format.matches(format)) {
				this.format=format;
				return read(format, buffer, offset, length);
			} else {
				return -2;
			}
		}
	}

	protected int update(boolean primary,int totalVolume,int volume, byte[] buffer, int offset, int length) throws IOException {
		int n;
		if(pausing) {
			if(secondaryLines.hasStreams()) {
				Arrays.fill(buffer, offset, offset+length, (byte)0);
				n=length;
			} else {
				return -1;
			}
		} else {
			n=stream.update(primary,totalVolume,totalVolume, buffer, offset, length);
		}
		if(secondaryLines.hasStreams()) {
			int vol=(7*totalVolume)>>3;
			secondaryLines.update(false,vol, (3*vol)>>2, buffer, offset, n);
			secondaryLines.update();
		}
		if(n<0&&sink!=null) {
			sink.pause();
		}
		return n;
	}

	@Override
	public boolean isPlaying() {
		return sink!=null&&!sink.paused;
	}
	@Override
	public void flush() {
		if(sink!=null) {
			sink.flush();
		}
	}
	
	@Override
	public void close() {
		sink.close();
		stream.close();
	}

	@Override
	public void requestFocus(String uri) {
		if(stream instanceof AudioPlayer) {
			((AudioPlayer)stream).requestAudioFocus(uri);
		}
	}
}
