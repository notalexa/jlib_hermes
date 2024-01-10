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

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioStream.AudioInfo;

/**
 * Class representing the state of a player. This is for informational use only and can be used to build up an UI for the player.
 * 
 */
public class PlayerState implements Cloneable {
	/**
	 * Capability flag: Can we turn on/off the player?
	 */
	public static final int TURN_ON_OFF=0x1;

	/**
	 * Capability flag: Can we pause/resume the player?
	 */
	public static final int PAUSE_RESUME=0x2;
	
	/**
	 * Capability flag: Can we set the volume?
	 */
	public static final int VOLUME=0x4;
	
	/**
	 * Capability flag: Can we seek to a position?
	 */
	public static final int SEEK=0x8;
	
	/**
	 * Capability flag: Can we seek to the firt track in the album?
	 */
	public static final int FIRST=0x10;
	
	/**
	 * Capability flag: Can we replay the current track?
	 */
	public static final int REPLAY=0x20;
	
	/**
	 * Capability flag: Can we turn on/off shuffling?
	 */
	public static final int SHUFFLE=0x40;
	
	/**
	 * Capability flag: Can we skip to the next track?
	 */
	public static final int NEXT_TRACK=0x80;
	
	/**
	 * Capability flag: Can we skip to the next album?
	 */
	public static final int NEXT_ALBUM=0x100;
	
	/**
	 * Capability flag: Can we skip to the previous track?
	 */
	public static final int PREVIOUS_TRACK=0x200;
	
	/**
	 * Capability flag: Can we skip to the previous album?
	 */
	public static final int PREVIOUS_ALBUM=0x400;
	
	/**
	 * Capability flag: Can we skip to a random track?
	 */
	public static final int RANDOM_TRACK=0x800;
	
	/**
	 * Capability flag: Can we skip to a random album?
	 */
	public static final int RANDOM_ALBUM=0x1000;
	
	/**
	 * Capability flag: Can we repeat the current track?
	 */
	public static final int REPEAT_TRACK=0x2000;
	
	/**
	 * Capability flag: Can we repeat the current album?
	 */
	public static final int REPEAT_ALBUM=0x4000;
	
	/**
	 * Capability flag: Can we peek tracks in the current album?
	 */
	public static final int PEEK_TRACK=0x8000;
	
	/**
	 * Capability flag: Can we peek albums?
	 */
	public static final int PEEK_ALBUM=0x10000;
	
	/**
	 * Is the player active?
	 */
	@JsonProperty(defaultValue = "false") public boolean active;
	
	/**
	 * Is the player playing?
	 */
	@JsonProperty(defaultValue = "false") public boolean playing;
	
	/**
	 * Is shuffling active?
	 */
	@JsonProperty(defaultValue = "false") public boolean shuffle;
	
	/**
	 * Do we repeat the track?
	 */
	@JsonProperty(defaultValue = "false") public boolean repeatTrack;
	
	/**
	 * Do we repeat the album?
	 */
	@JsonProperty(defaultValue = "false") public boolean repeatAlbum;
	
	/**
	 * The current volume
	 */
	@JsonProperty(defaultValue = "0")  public float volume;
	
	/**
	 * A list of the (current) capabilities
	 */
	@JsonProperty(defaultValue = "0")  public int capabilities;
	
	/**
	 * Audio information of the current track
	 */
	@JsonProperty public AudioInfo audioInfo;
	
	public PlayerState() {
	}
	
	/**
	 * Copy this state for the given capability mask
	 * @param capabilityMask the capability mask
	 * @return the state with the masked capabilities
	 */
	public PlayerState copy(int capabilityMask) {
		try {
			PlayerState copy=(PlayerState)clone();
			copy.capabilities&=capabilityMask;
			return copy;
		} catch(Throwable t) {
			return this;
		}
	}
	
	public String toString() {
		return "PlayerState[capabilities="+Integer.toBinaryString(capabilities)+", active="+active+", playing="+playing+", shuffle="+shuffle+", repeat="+repeatAlbum+":"+repeatTrack+", volume="+volume+", audio="+audioInfo+"]";
	}
}
