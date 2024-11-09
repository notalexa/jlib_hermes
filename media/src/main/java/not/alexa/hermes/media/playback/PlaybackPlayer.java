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

import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.AudioStream;

/**
 * Interface defining a playback player.
 * 
 * @author notalexa
 */
public interface PlaybackPlayer extends AudioPlayer {
	/**
	 * Returns the (presumably) next track in the players queue. Note that calling this method twice may result in the same track (since the current track is still playing).
	 * 
	 * @param stream the stream this track is intended for.
	 * @return the next track or {@code null} if no track is available.
	 */
	public PlaybackTrack nextTrack(PlaybackStream stream);
	
	/**
	 * Load the content of the given track.
	 * 
	 * @param track the track
	 * @param callback a callback for loading feedback
	 * @return the audio stream corresponding to the given track
	 * @throws IOException if an error occurs
	 */
	public AudioStream load(PlaybackTrack track, Callback callback) throws IOException;
}
