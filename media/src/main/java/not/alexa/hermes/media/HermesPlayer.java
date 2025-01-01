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

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.HermesApi.Slot;
import not.alexa.hermes.HermesComponent;
import not.alexa.hermes.HermesMessage;
import not.alexa.hermes.audio.AudioPlayBytes;
import not.alexa.hermes.audio.AudioToggleOff;
import not.alexa.hermes.audio.AudioToggleOn;
import not.alexa.hermes.intent.handling.IntentHandler;
import not.alexa.hermes.media.AudioSink.Listener;
import not.alexa.hermes.media.AudioStream.AudioInfo;
import not.alexa.hermes.media.AudioStream.AudioStreamListener;
import not.alexa.hermes.media.streams.PrePostSilence;
import not.alexa.hermes.media.streams.SourceDataLineDecorator;
import not.alexa.hermes.media.streams.WavAudioStream;
import not.alexa.hermes.mqtt.HermesServer;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.hermes.tts.Say;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;

/**
 * Wrapper around an {@link AudioPlayer} to control the player using the hermes protocol. A description of the
 * intents can be found in the project's readme file.
 * <p>Typically, this wrapper is configured using a YAML file which describes a {@link HermesServer}.
 * 
 * @author notalexa
 */
public class HermesPlayer implements IntentHandler, HermesComponent, Listener {
	private static Logger LOGGER=LoggerFactory.getLogger(HermesPlayer.class);
	
	@JsonProperty(defaultValue = "Weiß nicht") String noInfo="Weiß nicht";
	@JsonProperty(defaultValue = "Du hörst gerade {0}") String shortInfo="Du hörst gerade {0}";
	@JsonProperty(defaultValue = "Du hörst gerade {1} von {0}") String longInfo="Du hörst gerade {1} von {0}";
	@JsonProperty(defaultValue="false") boolean publishState;
	@JsonProperty(required = true) AudioPlayer player;
	@JsonProperty(defaultValue="0.5f") float initialVolume=0.5f;
	@JsonProperty SourceDataLineDecorator profiler;
	
	private AudioControls controls;
	private boolean audioPlay=true;
	
	private ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r,"player-command");
		}
	});
	
	protected HermesPlayer() {
	}
	
	/**
	 * Construct a hermes wrapper around the given player.
	 * 
	 * @param player the player to wrap
	 */
	public HermesPlayer(AudioPlayer player) {
		this.player=player;
	}
	
	@Override
	public void configure(Map<String,Class<? extends HermesMessage<?>>> extensions,Map<Class<?>, Object> resources, List<Class<? extends HermesMessage<?>>> overlays) {
		resources.put(HermesPlayer.class,this);
		extensions.put("notalexa/mediaplayer/state",StateMessage.class);
		overlays.add(AudioPlayBytesHandler.class);
		overlays.add(ToggleOnHandler.class);
		overlays.add(ToggleOffHandler.class);
		overlays.add(StateMessageHandler.class);
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		if(controls==null) {
			return false;
		} else if(intent.getIntent().startsWith("mediaplayer/")) {
			String cmd=intent.getIntent().substring("mediaplayer/".length());
			if("play".equals(cmd)||"turnon".equals(cmd)) {
				executor.execute(Commands.Play.decorate(()->{
					Slot source=intent.getSlot("source");
					if(source!=null) {
						if(player.play(source.getValue())) {
							controls.resume();
						}
					} else if(!player.hasAudio()) try {
						LOGGER.info("No music playable");
						api.publish(new Say("Ich hab keine Musik in der Rille"));
					} catch(Throwable t) {
					} else {
						controls.resume();
					}
				}));
			} else if("pause".equals(cmd)||"turnoff".equals(cmd)) {
				executor.execute(Commands.Play.decorate(()->{
					controls.pause();
				}));
			} else if("track".equals(cmd)) {
				Slot cmdSlot=intent.getSlot("cmd");
				if(cmdSlot!=null) {
					if("next".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.nextTrack();
						}));
					} else if("prev".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.previousTrack();
						}));
					} else if("replay".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.replay();
						}));
					} else if("first".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.firstTrack();
						}));
					} else if("random".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.randomTrack();
						}));
					} else if("repeat".equals(cmd)) {
						executor.execute(Commands.Track.decorate(()->{
							player.repeatTrack(true);
						}));
					} else if("forward".equals(cmd)) {
						executor.execute(Commands.Track.decorate(()->{
							player.repeatTrack(false);
						}));
					} else if("peek".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							Slot hint=intent.getSlot("hint");
							if(hint!=null) try {
								Slot duration=intent.getSlot("duration");
								player.peekTracks(hint.getValue(), duration==null?5f:Float.parseFloat(duration.getValue()));
							} catch(Throwable t) {
							}
						}));
					}
				}
			} else if("album".equals(cmd)) {
				Slot cmdSlot=intent.getSlot("cmd");
				if(cmdSlot!=null) {
					if("next".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.nextAlbum();
						}));
					} else if("prev".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.previousAlbum();
						}));
					} else if("random".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.randomAlbum();
						}));
					} else if("replay".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							player.firstTrack();
						}));
					} else if("repeat".equals(cmd)) {
						executor.execute(Commands.Track.decorate(()->{
							player.repeatAlbum(true);
						}));
					} else if("forward".equals(cmd)) {
						executor.execute(Commands.Track.decorate(()->{
							player.repeatAlbum(false);
						}));
					} else if("peek".equals(cmdSlot.getValue())) {
						executor.execute(Commands.Track.decorate(()->{
							Slot hint=intent.getSlot("hint");
							if(hint!=null) try {
								Slot duration=intent.getSlot("duration");
								player.peekAlbums(hint.getValue(), duration==null?5f:Float.parseFloat(duration.getValue()));
							} catch(Throwable t) {
							}
						}));
					}
				}
			} else if("volume".equals(cmd)) {
				executor.execute(Commands.Volume.decorate(()->{
					Slot volume=intent.getSlot("value");
					if(volume!=null) try {
						controls.setVolume(Integer.parseInt(volume.getValue())/100f);
					} catch(Throwable t) {
					} else {
						volume=intent.getSlot("offset");
						if(volume!=null) try {
							controls.setVolume(controls.getVolume()+Integer.parseInt(volume.getValue())/100f);
						} catch(Throwable t) {
						}
					}
				}));
			} else if("shuffle".equals(cmd)) {
				executor.execute(Commands.Shuffle.decorate(()->{
					player.shuffle(true);
				}));
			} else if("noshuffle".equals(cmd)) {
				executor.execute(Commands.Shuffle.decorate(()->{
					player.shuffle(false);
				}));
			} else if("forward".equals(cmd)) {
				executor.execute(Commands.Track.decorate(()->{
					player.repeatTrack(false);
					player.repeatAlbum(false);
				}));
			} else if("select".equals(cmd)) {
				Slot slot=intent.getSlot("choose");
				boolean select=slot==null||!"false".equals(slot.getValue());
				executor.execute(Commands.Track.decorate(()->{
					player.stopPeeking(select);
				}));
			} else if("info".equals(cmd)) {
				executor.execute(Commands.Info.decorate(()->{
					try {
						AudioInfo info=player.getCurrentInfo();
						String msg;
						if(info!=null) {
							if(null!=info.getArtist()) {
								msg=MessageFormat.format(longInfo,info.getArtist(),info.getTitle());
							} else if(info.getTitle()!=null) {
								msg=MessageFormat.format(shortInfo, info.getTitle());
							} else {
								msg=noInfo;
							}
						} else {
							msg=null;
						}
						if(msg!=null) {
							intent.reply(api, msg);
						}
					} catch(Throwable t) {
					}
				}));
			} else if("state".equals(cmd)) {
				executor.execute(Commands.State.decorate(()->{
					try {
						PlayerState state=player.getState();
						if(state!=null) {
							new StateMessage(api.getSiteId(), player.getState()).publish(api);
						}
					} catch(Throwable t) {
						LOGGER.error("Failed to publish state",t);
					}
				}));
			} else {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}
	
	
	@Overlay
	public class AudioPlayBytesHandler extends AudioPlayBytes {
		public AudioPlayBytesHandler() {
		}

		@SuppressWarnings("resource")
		@Override
		public void received(HermesApi api) throws BaseException {
			if(controls!=null&&audioPlay) {
				AudioStream stream=new WavAudioStream(data);
				if(player.hasAudio()) {
					stream=new PrePostSilence(0.5f, 0.5f,stream);
				}
				controls.addStream(stream);
			}
		}
	}

	@Overlay
	public class ToggleOnHandler extends AudioToggleOn {
		public ToggleOnHandler() {
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			if(getSiteId().equals(api.getSiteId())) {
				audioPlay=true;
			}
		}
	}

	@Overlay
	public class ToggleOffHandler extends AudioToggleOff {
		public ToggleOffHandler() {
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			if(getSiteId().equals(api.getSiteId())) {
				audioPlay=false;
			}
		}
	}

	@Override
	public void sinkError(Throwable t) {
	}

	@Override
	public void startup(HermesApi api,Context context) {
		player.addStreamListener(new AudioStreamListener() {
			@Override
			public void onStateChanged() {
				if(publishState) {
					executor.schedule(Commands.State.decorate(()->{
						try {
							new StateMessage(api.getSiteId(), player.getState()).publish(api);
						} catch(Throwable t) {
							LOGGER.error("Failed to publish state",t);
						}
					}),100,TimeUnit.MILLISECONDS);
				}
			}
		});
		controls=player.createControls(new AudioSink(initialVolume,profiler,this));
		player.startup(context);
	}

	@Override
	public void shutdown(HermesApi api,Context context) {
		if(controls!=null) {
			controls.close();
		}
		player.close();
	}
	
	public enum Commands {
		Play,Track,Shuffle,Volume,Info,State;
		private AtomicInteger id=new AtomicInteger();
		
		public Runnable decorate(Runnable r) {
			return new Runnable() {
				int runId=id.incrementAndGet();
				@Override
				public void run() {
					if(id.get()==runId) {
						r.run();
					}
				}
			};
		}
	}
	
	public static class StateMessage implements HermesMessage<StateMessage> {
		@JsonProperty(defaultValue = "default") String siteId;
		@JsonProperty PlayerState state;
		protected StateMessage() {
		}
		public StateMessage(String siteId,PlayerState  state) {
			this.state=state;
			this.siteId=siteId;
		}
		@Override
		public String getTopic() {
			return "notalexa/mediaplayer/state";
		}
	}
	
	@Overlay
	public static class StateMessageHandler extends StateMessage {
		protected StateMessageHandler() {
			super();
		}

		@Override
		public void received(HermesApi api) throws BaseException {
			LOGGER.info("State changed {}",state);
		}
	}
}
