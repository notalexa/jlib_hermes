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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioControls;
import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.PlayerState;
import not.alexa.hermes.media.streams.Silence;
import not.alexa.netobjects.Context;

/**
 * A player which delegates to one of the configured players using the "best match" from {@link AudioPlayer#supports(String)}. A player returning 
 * a value {@code &lt;Integer.MAX_VALUE} is expected to play something for this URL (silence by default) returning {@code true} if no error occured,
 * {@code false} otherwise.
 * <p><b>Configuration:</b> The only configuration possible is a list of {@code players}.
 * 
 * @author notalexa
 */
public class AudioPlayers extends AbstractPlayer<Void> {
	private static final Logger LOGGER=LoggerFactory.getLogger(AudioPlayers.class);
	@JsonProperty(defaultValue="satellite:.*") private Pattern ignore;
	@JsonProperty private List<AudioPlayer> players;
	private AudioPlayer currentPlayer;

	@Override
	public void startup(Context context) {
		if(players!=null) for(AudioPlayer player:players) {
			player.setAudioControls(new AudioControls() {
				
				@Override
				public Object getStreamLock() {
					return controls.getStreamLock();
				}

				@Override
				public void setVolume(float vol) {
					if(currentPlayer==player&&controls!=null) {
						controls.setVolume(vol);
					}
				}
				
				@Override
				public void resume() {
					if(currentPlayer==player&&controls!=null) {
						controls.resume();
					}
				}
				
				@Override
				public void requestFocus(String uri) {
					if(player!=currentPlayer&&controls!=null) {
						play(uri);
					}
				}
				
				@Override
				public void pause() {
					if(currentPlayer==player&&controls!=null) {
						controls.pause();
					}
				}

				@Override
				public void flush() {
					if(currentPlayer==player&&controls!=null) {
						controls.flush();
					}
				}

				@Override
				public boolean isPlaying() {
					return controls!=null&&controls.isPlaying()&&currentPlayer==player;
				}
				
				@Override
				public float getVolume() {
					return controls==null?1f:controls.getVolume();
				}

				@Override
				public void onAudioFormatChanged(AudioFormat format) {
					if(currentPlayer==player&&controls!=null) {
						controls.onAudioFormatChanged(format);
					}
				}

				@Override
				public void onStateChanged() {
					if(currentPlayer==player&&controls!=null) {
						controls.onStateChanged();
					}
				}

				@Override
				public boolean isActive() {
					return player==currentPlayer;
				}

				@Override
				public void close() {
					if(currentPlayer==player&&controls!=null) {
						controls.close();
					}
				}

				@Override
				public void addStream(AudioStream stream) {
					if(currentPlayer==player&&controls!=null) {
						controls.addStream(stream);
					}
				}
			});
			player.addStreamListener(new AudioStreamListener() {
				@Override
				public void onStateChanged() {
					if(currentPlayer==player) {
						fireStateChanged();
					}
				}
			});
			player.startup(context);
		}
	}

	@Override
	public void setAudioControls(AudioControls controls) {
		this.controls=controls;
	}
	
	@Override
	public boolean play(String uri) {
		AudioPlayer selected=null;
		int score=Integer.MAX_VALUE;
		if(players!=null) for(AudioPlayer player:players) {
			int s=player.supports(uri);
			if(s<score) {
				score=s;
				selected=player;
			}
		}
		if(selected==null&&ignore!=null&&ignore.asPredicate().test(uri)) {
			LOGGER.info("Uri {} ignored.",uri);
			return currentPlayer!=null;
		}
		if(selected!=currentPlayer) {
			if(currentPlayer!=null) {
				AudioPlayer player=currentPlayer;
				currentPlayer=null;
				player.onAudioStateChanged();
			}
			if(selected!=null) {
				currentPlayer=selected;
				if(!selected.play(uri)) {
					selected=null;
					play(new Silence());
				} else {
					play(selected);
				}
			}
			currentPlayer=selected;
		} else if(selected!=null) {
			if(!selected.play(uri)) {
				selected=null;
				play(new Silence());
			} else {
				play(selected);
			}
		}
		return currentPlayer!=null;
	}

	@Override
	public int supports(String uri) {
		int score=Integer.MAX_VALUE;
		if(players!=null) for(AudioPlayer player:players) {
			int s=player.supports(uri);
			if(s<score) {
				score=s;
			}
		}
		return score;
	}

	@Override
	public boolean hasAudio() {
		return currentPlayer!=null&&currentPlayer.hasAudio();
	}

	@Override
	public void replay() {
		if(currentPlayer!=null) {
			currentPlayer.replay();
		}
	}

	@Override
	public void nextTrack() {
		if(currentPlayer!=null) {
			currentPlayer.nextTrack();
		}
	}

	@Override
	public void nextAlbum() {
		if(currentPlayer!=null) {
			currentPlayer.nextAlbum();
		}
	}

	@Override
	public void previousTrack() {
		if(currentPlayer!=null) {
			currentPlayer.previousTrack();
		}
	}

	@Override
	public void previousAlbum() {
		if(currentPlayer!=null) {
			currentPlayer.previousAlbum();
		}
	}

	@Override
	public void firstTrack() {
		if(currentPlayer!=null) {
			currentPlayer.firstTrack();
		}
	}

	@Override
	public void randomTrack() {
		if(currentPlayer!=null) {
			currentPlayer.randomTrack();
		}
	}

	@Override
	public void repeatTrack(boolean repeat) {
		if(currentPlayer!=null) {
			currentPlayer.repeatTrack(repeat);
		}
	}

	@Override
	public void repeatAlbum(boolean repeat) {
		if(currentPlayer!=null) {
			currentPlayer.repeatAlbum/*
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
			 */(repeat);
		}
	}

	@Override
	public void randomAlbum() {
		if(currentPlayer!=null) {
			currentPlayer.randomAlbum();
		}
	}

	@Override
	public void shuffle(boolean enable) {
		if(currentPlayer!=null) {
			currentPlayer.shuffle(enable);
		}
	}

	@Override
	public void peekTracks(String hint, float seconds) {
		if(currentPlayer!=null) {
			currentPlayer.peekTracks(hint, seconds);
		}
	}

	@Override
	public void peekAlbums(String hint, float seconds) {
		if(currentPlayer!=null) {
			currentPlayer.peekAlbums(hint, seconds);
		}
	}

	@Override
	public boolean seekTo(float seconds) {
		if(currentPlayer!=null) {
			return currentPlayer.seekTo(seconds);
		} else {
			return false;
		}
		
	}

	@Override
	public void close() {
		if(currentPlayer!=null) {
			currentPlayer.close();
		}
	}

	@Override
	public void onAudioStateChanged() {
		if(currentPlayer!=null) {
			currentPlayer.onAudioStateChanged();
		} else {
			super.onAudioStateChanged();
		}
	}
	
	@Override
	public PlayerState getState() {
		if(currentPlayer==null) {
			state.active=false;
			return super.getState();
		} else {
			return currentPlayer.getState();
		}
	}
}
