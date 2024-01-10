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

import not.alexa.hermes.media.AudioStream.AudioStreamListener;

/**
 * Interface defining operations on the audio stream.
 * 
 * @author notalexa
 */
public interface AudioControls extends AudioStreamListener, AutoCloseable {

	/**
	 * Global stream synchronization lock. Inside a synchronized block, the format will not change.
	 * 
	 * @return the global stream lock
	 */
	public Object getStreamLock();
	
	/**
	 * Set the volume (between 0&leq;vol&leq;1).
	 * 
	 * @param vol the new volume
	 */
	public void setVolume(float vol);
	
	/**
	 * 
	 * @return the current volume
	 */
	public float getVolume();
	
	/**
	 * 
	 * @return {@code true} if the player is active
	 */
	default boolean isActive() {
		return true;
	}
	
	/**
	 * 
	 * @return {@code true} if the player is playing
	 */
	boolean isPlaying();
	
	/**
	 * Request (audio) focus for the specified uri.
	 * 
	 * @param uri the uri to request focus for
	 */
	void requestFocus(String uri);
	
	/**
	 * Pause the current playing.
	 */
	void pause();
	
	/**
	 * Resume the current playing.
	 */
	void resume();
	
	/**
	 * Flush the current playing. This results in emptying the audio buffer if possible.
	 */
	void flush();

	/**
	 * Close the underlying resources
	 */
	public void close();

	/**
	 * Add a secondary stream
	 * @param stream the stream to add
	 */
	public void addStream(AudioStream stream);	
}
