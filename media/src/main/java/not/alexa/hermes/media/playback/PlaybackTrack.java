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
package not.alexa.hermes.media.playback;

import java.io.IOException;

import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.AudioStream.MediaType;
import not.alexa.hermes.media.io.CacheManager;

/**
 * Interface defining one entry in a playback stream.
 * 
 * @author notalexa
 */
public interface PlaybackTrack {
	
	/**
	 * @return a unique id for the stream
	 */
	public String getId();
	
	/**
	 * Load the stream.
	 * 
	 * @param callback an (optional) callback for download notifications.
	 * @return the audio stream corresponding to the given id
	 * @throws IOException if an error occurs
	 */
	public AudioStream load(Callback callback) throws IOException;

	/**
	 * 
	 * @return the media type for the stream
	 */
	public MediaType getMediaType();

	/**
	 * 
	 * @return the start time (in milliseconds) where to start playing 
	 */
	public int startPos();
	
	/**
	 * Try casting this track to the given class. Can be used to retrieve the original track in facades (like caching facades created by {@link CacheManager}.
	 * 
	 * @param <T> the type of the class
	 * @param clazz the class we want to cast to
	 * @return the casted object (or {@code null} if casting is not possible
	 */
	@SuppressWarnings("unchecked")
	public default <T extends PlaybackTrack> T as(Class<T> clazz) {
		if(clazz.isInstance(this)) {
			return (T)this;
		} else {
			return null;
		}
	}
}
