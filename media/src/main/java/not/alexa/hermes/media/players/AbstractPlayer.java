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
package not.alexa.hermes.media.players;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import not.alexa.hermes.media.AudioControls;
import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.PlayerState;
import not.alexa.hermes.media.streams.Silence;

/**
 * Class implementing most features of an {@link AudioPlayer} based on a "current stream". The class handles events if the stream changes and
 * provides basic functionality for history.
 * 
 * @author notalexa
 * @param <T> the type of the history entry
 */
public abstract class AbstractPlayer<T> implements AudioPlayer {
	protected static Logger LOGGER=LoggerFactory.getLogger(AudioPlayer.class);
	private AudioStream currentStream=new Silence();
	protected List<AudioStreamListener> listeners=new ArrayList<>();
	protected PlayHistory<T> history=new PlayHistory<>(100);
	protected AudioControls controls;
	protected boolean shuffle;
	protected PlayerState state=new PlayerState();

	public AbstractPlayer() {
		state.capabilities=PlayerState.TURN_ON_OFF|PlayerState.PAUSE_RESUME|PlayerState.VOLUME;
	}

	public void setAudioControls(AudioControls controls) {
		this.controls=controls;
		state.active=true;
	}
	
	@Override
	public AudioFormat getFormat() {
		synchronized(controls.getStreamLock()) {
			return currentStream.getFormat();
		}
	}
	
	protected void play(AudioStream stream) {
		boolean fireEvent=false;
		synchronized(controls.getStreamLock()) {
			if(stream!=null) {
				if(stream==currentStream) {
					return;
				} else {
					currentStream.close();
				}
				this.currentStream=stream;
				fireEvent=true;
				if(controls!=null) {
					controls.onAudioFormatChanged(stream.getFormat());
				}
			}
		}
		if(fireEvent) {
			fireStateChanged();
		}
	}
	
	protected AudioStream getCurrent() {
		synchronized(controls.getStreamLock()) {
			return currentStream;
		}
	}

	@Override
	public int next() throws IOException {
		try {
			synchronized(controls.getStreamLock()) {
				int val=currentStream.next();
				if(val<Integer.MAX_VALUE) {
					return val;
				}
			}
		} catch(Throwable t) {
			LOGGER.error("Failed to get next sample",t);
		}
		try {
			currentStream.close();
		} catch(Throwable t) {
		}
		currentStream=new Silence(currentStream.getFormat());
		nextTrack();
		return Integer.MAX_VALUE-1;
	}

	@Override
	public int update(boolean primary,int totalVolume, int volume, byte[] buffer, int offset, int length) throws IOException {
		try {
			synchronized(controls.getStreamLock()) {
				int ret=currentStream.update(primary,totalVolume, volume, buffer, offset, length);
				if(ret>=0) {
					return ret;
				}
			}
		} catch(Throwable t) {
			LOGGER.error("Failed to get next sample",t);
		}
		try {
			currentStream.close();
		} catch(Throwable t) {
		}
		currentStream=new Silence(currentStream.getFormat());
		nextTrack();
		return 0;
	}

	@Override
	public AudioInfo getCurrentInfo() {
		synchronized(controls.getStreamLock()) {
			return currentStream.getCurrentInfo();
		}
	}

	@Override
	public boolean hasAudio() {
		synchronized(controls.getStreamLock()) {
			return !(currentStream instanceof Silence);
		}
	}

	@Override
	public void nextTrack() {
	}

	@Override
	public void shuffle(boolean shuffle) {
		if(this.shuffle!=shuffle) {
			this.shuffle=shuffle;
			fireStateChanged();
		}
	}

	@Override
	public boolean seekTo(float seconds) {
		synchronized(controls.getStreamLock()) {
			return currentStream.seekTo(seconds);
		}
	}

	@Override
	public void onAudioStateChanged() {
		fireStateChanged();
	}
	
	@Override
	public void addStreamListener(AudioStreamListener listener) {
		if(!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	@Override
	public void removeStreamListener(AudioStreamListener listener) {
		listeners.remove(listener);
	}
	
	@Override
	public PlayerState getState() {
		state.playing=controls.isPlaying();
		state.volume=controls.getVolume();
		state.audioInfo=getStateInfo();
		return state.copy(isStream()?~(PlayerState.SEEK):~0);
	}
	
	protected AudioInfo getStateInfo() {
		return getCurrentInfo();
	}

	protected void fireStateChanged() {
		if(controls!=null) {
			controls.onStateChanged();
			for(AudioStreamListener listener:listeners) {
				listener.onStateChanged();
			}
		}
	}
}
