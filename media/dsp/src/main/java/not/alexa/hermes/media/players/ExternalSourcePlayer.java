/*
 * Copyright (C) 2025 Not Alexa
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

import javax.sound.sampled.LineEvent;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.streams.Silence;
import not.alexa.hermes.media.streams.dsp.DSPProfileDecorator;

/**
 * Simple player playing silence. The player is considered to play sound in the following situation:
 * 
 *  <ul>
 *  <li>The player is turned on and the line event {@link DSPProfileDecorator#UNLOCKED} was not received.
 *  <li>The line event {@link DSPProfileDecorator#LOCKED} was received.
 *  </ul>
 *  
 * The player recognizes the following url: `player:<name>' where name needs to be configured.
 * 
 * @author notalexa
 */
public class ExternalSourcePlayer extends AbstractPlayer<Void> implements AudioPlayer {
	@JsonProperty(required = true) protected String name;
	private boolean locked=false;
	
	public ExternalSourcePlayer() {
	}

	@Override
	public void update(LineEvent event) {
		super.update(event);
		if(event.getType()==DSPProfileDecorator.LOCKED) {
			if(!locked) {
				locked=true;
				controls.resume();
			}
		} else if(event.getType()==DSPProfileDecorator.UNLOCKED) {
			if(locked) {
				locked=false;
				controls.pause();
			}
		}
	}

	@Override
	public boolean play(String uri) {
		if(supports(uri)==0) {
			play(new Silence());
			locked=true;
			return true;
		}
		return false;
	}

	@Override
	public int supports(String uri) {
		return ("player:"+name).equals(uri)?0:Integer.MAX_VALUE;
	}
	
	@Override
	public boolean hasAudio() {
		return locked;
	}
}
