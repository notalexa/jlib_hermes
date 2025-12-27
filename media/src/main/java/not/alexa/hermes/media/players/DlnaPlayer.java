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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.PlayerState;
import not.alexa.hermes.media.players.dlna.DidlLite;
import not.alexa.hermes.media.players.dlna.DidlLite.Container;
import not.alexa.hermes.media.players.dlna.DidlLite.Content;
import not.alexa.hermes.media.players.dlna.DlnaBrowser;
import not.alexa.hermes.media.streams.MP3AudioStream;
import not.alexa.hermes.media.streams.Silence;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;

/**
 * In the DLNA universe, we have DLNA Renderer (presenting media content), DLNA Media Server (servcing media content) and
 * DLNA Controller (providing an interface to consume media content). This DLNA Player is in between renderer and controller.
 * It provides the capabilities to present audio content (as all other players) and some controlling mechanism to keep the audio stream
 * alive.
 * <p>The player accepts uris of the form <code>dlna://&lt;control-uri>/&lt;content-id></code> where
 * <ul>
 * <li>{@code <control-uri>} is the URL of the Control Channel of a Media Server (without <code>http://</code>) and 
 * <li>{@code <content-id>} is the content
 * id of the requested audio content.
 * </ul>
 * If the content is a container, an audio track is randomly choosen.
 * <br>The url must be provided from outside but the special id {@code 0} references the root container.
 *  
 * <p><b>Restrictions:</b> Currently only MP3 files are supported and the seek operations are not implemented. The control url <b>must</b>
 * be accessible via the {@code http} protocol. {@code https} is not supported (since the replacement {@code dlna} to {@code http} is hard wired). 
 * 
 * @author notalexa
 *  
 */
public class DlnaPlayer extends AbstractPlayer<DlnaPlayer.DlnaHistory> implements AudioPlayer {
	private static Logger LOGGER=LoggerFactory.getLogger(DlnaPlayer.class);
	
	@JsonProperty String name;
	private Random random=new Random();
	private boolean repeatTrack;
	private boolean repeatAlbum;
	private Track currentTrack;
	private Context context=Context.createRootContext();
	
	private Map<String,DlnaBrowser> browsers=new HashMap<>();
	
	protected DlnaPlayer() {
		state.capabilities|=PlayerState.NEXT_ALBUM|PlayerState.NEXT_TRACK
				|PlayerState.PREVIOUS_ALBUM|PlayerState.PREVIOUS_TRACK
				|PlayerState.REPEAT_ALBUM|PlayerState.REPEAT_TRACK
				|PlayerState.RANDOM_ALBUM|PlayerState.RANDOM_TRACK
				|PlayerState.REPLAY|PlayerState.FIRST
				/*|PlayerState.SEEK*/|PlayerState.SHUFFLE;
	}
	
	public DlnaPlayer(String name) {
		this();
		this.name=name;
	}
	
	private void play(Track track) {
		if(track!=null) {
			history.add(new DlnaHistory(track.getUri(),track.getAlbumId()));
			if(currentTrack==null||!track.getUri().equals(currentTrack.getUri())) {
				history.add(new DlnaHistory(track.getUri(),track.getAlbumId()));
			}
			currentTrack=track;
			play(track.open());
			LOGGER.info("Play {}",track);
		}
	}
	
	@Override
	public boolean isStream() {
		return false;
	}
	
	@Override
	public void nextTrack() {
		if(currentTrack==null) {
			randomTrack();
		} else {
			play(currentTrack.next());
		}
	}

	@Override
	public void repeatTrack(boolean repeat) {
		if(repeat!=repeatTrack) {
			repeatTrack=repeat;
			fireStateChanged();
		}
	}

	@Override
	public void repeatAlbum(boolean repeat) {
		if(repeat!=repeatAlbum) {
			repeatAlbum=repeat;
			fireStateChanged();
		}
	}

	@Override
	public void previousTrack() {
		if(currentTrack==null) {
			randomTrack();
		} else {
			DlnaHistory f=history.pop();
			if(f!=null) try {
				Track t=resolveUri(f.getUri());
				if(t!=null) {
					play(t);
				}
			} catch(Throwable t) {
				LOGGER.error(t.getMessage(),t);
			}
		}
	}
	
	@Override
	public void firstTrack() {
		if(currentTrack!=null) {
			play(currentTrack.firstTrack());
		}
	}
	
	@Override
	public void randomTrack() {
		Track track=null;
		if(currentTrack!=null) {
			track=currentTrack.getAlbum().randomTrack();
		} else if(browsers.size()>0) {
			DlnaBrowser[] b=browsers.values().toArray(new DlnaBrowser[0]);
			DlnaBrowser browser=null;
			if(b.length==1) {
				browser=b[0];
			} else if(b.length>1) {
				browser=b[random.nextInt(b.length)];
			}
			if(browser!=null) {
				not.alexa.hermes.media.players.dlna.DidlLite.Track t=browser.random();
				if(t!=null) {
					track=new Track(browser,t);
				}
			}			
		}
		if(track!=null) {
			play(track);
		}
	}

	@Override
	public void randomAlbum() {
		Track track=null;
		DlnaBrowser browser=null;
		if(currentTrack!=null) {
			browser=currentTrack.getBrowser();
		} else {
			DlnaBrowser[] b=browsers.values().toArray(new DlnaBrowser[0]);
			if(b.length==1) {
				browser=b[0];
			} else if(b.length>1) {
				browser=b[random.nextInt(b.length)];
			}
		}
		if(browser!=null) {
			Container container=browser.getRootContainer().randomTrack().getParent();
			if(container!=null) {
				track=new Track(browser,container.first());
			}
		}
		if(track!=null) {
			play(track);
		}
	}

	@Override
	public void nextAlbum() {
		Track track=null;
		if(currentTrack!=null) try {
			track=currentTrack.getAlbum().next().first(currentTrack);
		} catch(Throwable t) {
		}
		if(track!=null) {
			play(track);
		}
	}
	
	/**
	 * Implemented: Seek to the very beginning.
	 */
	@Override
	public boolean seekTo(float t) {
		if(t==0&&currentTrack!=null) {
			play(currentTrack);
			return true;
		}
		return false;
		
	}

	@Override
	public void previousAlbum() {
		DlnaHistory rover=history.peek();
		String currentAlbum=rover==null?null:rover.getAlbumId();
		rover=rover==null?null:history.pop();
		do {
			rover=rover==null?null:history.pop();
			if(rover!=null) {
				if(!rover.getAlbumId().equals(currentAlbum)) {
					try {
						Track t=resolveUri(rover.getUri());
						if(t!=null) {
							play(t);
						}
					} catch(Throwable t) {
						LOGGER.error(t.getMessage(),t);
					}
					break;
				}
			}
		} while(rover!=null);
	}

	@Override
	public int supports(String src) {
		if(src.startsWith("player:")&&name.equalsIgnoreCase(src.substring("player:".length()))) {
			return 0;
		} else if(src.startsWith("dlna://")) {
			return 0;
		} else {
			return Integer.MAX_VALUE;
		}
	}
	
	protected Track resolveUri(String src) throws BaseException{
		int p=src.lastIndexOf('/');
		String id=src.substring(p+1);
		String host=src.substring("dlna://".length(),p);
		DlnaBrowser browser=browsers.computeIfAbsent(host, (k)-> new DlnaBrowser(context, "http://"+host));
		Content<?> content=browser.resolve(id);
		if(content!=null) {
			DidlLite.Track t=context.cast(DidlLite.Track.class, content);
			if(t!=null) {
				return new Track(browser,t);
			}
			DidlLite.Container container=context.cast(DidlLite.Container.class,content);
			if(container!=null) {
				return new Album(browser,container).randomTrack();
			}
		}
		return null;
	}
	
	@Override
	public AudioInfo getCurrentInfo() {
		AudioInfo info=super.getCurrentInfo();
		return currentTrack==null?info:currentTrack.getInfo(info);
	}
	
	@Override
	public boolean play(String src) {
		if(src.startsWith("player:")&&name.equalsIgnoreCase(src.substring("player:".length()))) {
			if(!hasAudio()) {
				randomTrack();
			}
			controls.onAudioFormatChanged(getFormat());
			controls.onStateChanged();
			return true;
		} else if(src.startsWith("dlna://")) try {
			Track track=resolveUri(src);
			if(track!=null) {
				play(track);
				return true;
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
		return false;
	}
	
	protected Album getAlbum(DlnaBrowser browser,Content<?> child) {
		Container c=child.getParent().update();
		return new Album(browser,c);
	}
	
	public class Album {
		DlnaBrowser browser;
		Container container;
		
		public Album(DlnaBrowser browser,Container container) {
			this.browser=browser;
			this.container=container;
		}
		
		public boolean isEmpty() {
			return !container.hasTracks();
		}
		
		public Track shuffle(Track current) {
			DidlLite.Track t=container.shuffle(current.t);
			return t==null?current:new Track(browser,this,t);
		}
		
		public Track first(Track current) {
			DidlLite.Track f=container.first();
			return f==null?current:new Track(browser,this,f);
		}
				
		public Track randomTrack() {
			DidlLite.Track t=container.randomTrack();
			return t==null?null:new Track(browser,this,t);
		}
		
		public Track next(Track current) {
			DidlLite.Track n=current.t.next(repeatAlbum);
			if(n==null) {
				return current;
			} else {
				return new Track(browser,this,n);
			}
		}
		
		public Album next() {
			Container c=container.next();
			if(c==null) {
				return this;
			} else {
				return new Album(browser,c);
			}
		}
		
		public String toString() {
			return "Album["+container.getTitle()+"]";
		}
	}
	
	public class Track {
		Album album;
		DlnaBrowser browser;
		not.alexa.hermes.media.players.dlna.DidlLite.Track t;
		
		public Track(DlnaBrowser browser,not.alexa.hermes.media.players.dlna.DidlLite.Track t) {
			this(browser,null,t);
		}

		public Track(DlnaBrowser browser,Album album,not.alexa.hermes.media.players.dlna.DidlLite.Track t) {
			this.album=album;
			this.browser=browser;
			this.t=t;
		}
		
		public DlnaBrowser getBrowser() {
			return browser;
		}
		
		public String getAlbumId() {
			return t==null?null:t.getParentId();
		}
		
		public AudioStream open() {
			Track candidate=this;
			for(int i=0;i<10;i++) try {
				return new MP3AudioStream(candidate.t.open(),candidate.getInfo(), 1f, candidate.t.getSize());
			} catch(Throwable t) {
				LoggerFactory.getLogger(DlnaPlayer.class).error("Getting "+this.t.getId()+" failed.",t);
				candidate=new Album(browser,browser.getRootContainer()).randomTrack();
			}
			return new Silence();
		}
		
		public AudioInfo getInfo(AudioInfo template) {
			return t.getArtist()==null?template:template.forArtist(t.getArtist());
		}
		
		public String getInfo() {
			String name=t.getTitle();
			if(name.endsWith(".mp3")) {
				name=name.substring(0,name.length()-4);
			}
			if(name.lastIndexOf(" - ")>0) {
				name=name.substring(name.lastIndexOf(" - ")+3).trim();
			}
			return name.trim();
		}
		
		public Album getAlbum() {
			if(album==null) {
				album=DlnaPlayer.this.getAlbum(browser,t);
			}
			return album;
		}
		public Track firstTrack() {
			return getAlbum().first(this);
		}
		
		public Track next() {
			if(repeatTrack) {
				return this;
			} else if(shuffle) {
				return shuffle();
			} else {
				Track next=getAlbum().next(this);
				return next==null?this:next;
			}
		}
		
		public Track shuffle() {
			return getAlbum().shuffle(this);
		}
		
		public String getUri() {
			return browser.getControlUri().replace("http://","dlna://")+"/"+t.getId();
		}
		
		public String toString() {
			return "Audio["+t.getTitle()+"]";
		}
	}
	
	class DlnaHistory {
		private String uri;
		private String albumId;
		private DlnaHistory(String uri,String albumId) {
			this.uri=uri;
			this.albumId=albumId;
		}
		
		public String getUri() {
			return uri;
		}
		
		public String getAlbumId() {
			return albumId;
		}
	}
}
