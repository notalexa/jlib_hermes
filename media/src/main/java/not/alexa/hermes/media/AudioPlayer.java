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

import not.alexa.netobjects.Context;

/**
 * Major interface of this package. Roughly, a player is an (endless) audio stream with additional 
 * possibilities to <i>move</i> inside this stream.
 * 
 * @author notalexa
 */
public interface AudioPlayer extends AudioStream {
	/**
	 * 
	 * @return the state of this player (including it's current capabilities)
	 */
	public PlayerState getState();
	
	/**
	 * 
	 * @return {@code true} if this player is playing audio (and not silence)
	 */
	public boolean hasAudio();
	
	/**
	 * Switch to the provided {@code uri}. Note that this doesn't imply that the stream is played. If the underlying
	 * sink is pausing, it may be necessary to resume.
	 * 
	 * @param uri the uri to play
	 * @return {@code true} if the uri matches this player
	 */
	public boolean play(String uri);
	
	/**
	 * Test if this player supports the uri. The method <b>should</b> return a value {@code &geq;0) if the uri can be
	 * played. In this case, {@link #play(String)} <b>must</b> return {@code true} if called (it should produce {@link Silence}
	 * if some errors occurs).
	 * <br>An underlying audio player may choose the player with the biggest return value. Think of the return value as a weight for
	 * handling the requested uri. An example is a player collection where no players fit. The underlying collection can handle the uri
	 * producing silence but other players may do better.
	 * 
	 * @param uri the uri to check
	 * @return a value {@code &geq;0) if this uri can be played.
	 */
	public int supports(String uri);
	
	/**
	 * Replay the current track (this defaults to {@link #seekTo(float)} with a value of 0.
	 */
	public default void replay() {
		seekTo(0);
	}
	
	/**
	 * Play the next track.
	 */
	public void nextTrack();
	
	/**
	 * Play the first song of the next album.
	 */
	public default void nextAlbum() {
	}
	
	/**
	 * Play the previous track.
	 */
	public default void previousTrack() {
	}
	
	/**
	 * Play the (first track) of the previous album.
	 */
	public default void previousAlbum() {
	}
	
	/**
	 * Play the first track of this album.
	 */
	public default void firstTrack() {
	}
	
	/**
	 * Play a random track of this album.
	 */
	public default void randomTrack() {
	}
	
	/**
	 * Play (the first track) of a random album.
	 */
	public default void randomAlbum() {
	}
	
	/**
	 * Set the repeat flag for the current track.
	 * 
	 * @param repeat repeat forever if the value is {@code true}
	 */
	public default void repeatTrack(boolean repeat) {
	}
	
	/**
	 * Set the repeat flag for the current album.
	 * 
	 * @param repeat repeat this album forever if the value is {@code true}
	 */
	public default void repeatAlbum(boolean repeat) {
	}
	
	/**
	 * Enable/Disable shuffle on this player
	 * 
	 * @param shuffle if {@code true}, enable shuffle, otherwise disable shuffle 
	 */
	public void shuffle(boolean shuffle);
	
	/**
	 * Peek the tracks of the current album. Each track should be played for the specified seconds before switching to the next one
	 * 
	 * @param hint a hint which tracks should be peeked
	 * @param seconds the number of seconds to play each track
	 */
	public default void peekTracks(String hint,float seconds) {
	}
	
	/**
	 * Peek the albums of this player. Each first track should be played for the specified seconds before switching to the next album.
	 * 
	 * @param hint a hint which albums should be peeked
	 * @param seconds the number of seconds to play the first track of an album
	 */
	public default void peekAlbums(String hint,float seconds) {
	}
	
	/**
	 * Stop peeking. Keep the current track if the flag is set, otherwise switch to the last peeked track.
	 * @param keepCurrent if {@code true}, keep the current track, otherwise switch to the peeked track. 
	 */
	public default void stopPeeking(boolean keepCurrent) {
	}
	
	/**
	 * @param listener the listener to add
	 */
	public void addStreamListener(AudioStreamListener listener);
	
	/**
	 * 
	 * @param listener the listener to remove
	 */
	public void removeStreamListener(AudioStreamListener listener);
	
	/**
	 * A player is endless.
	 */
	@Override 
	public default boolean isStream() {
		return true;
	}
	
	/**
	 * Startup this player
	 * 
	 * @param context the context to use for startup
	 */
	public default void startup(Context context) {
	}
	
	/**
	 * Request focus for the specified uri.
	 * 
	 * @param uri the uri to play
	 */
	public default void requestAudioFocus(String uri) {
	}
}
