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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import javazoom.jl.decoder.BitstreamException;
import not.alexa.hermes.media.AudioPlayer;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.PlayerState;
import not.alexa.hermes.media.streams.MP3AudioStream;
import not.alexa.hermes.media.streams.Silence;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.DefaultTypeLoader;

/**
 * A tuner plays network streams and is optimized for streaming. To turn the tuner on use either {@code player:tuner} (starts the last played stream if any)
 * or choose an url. The following additional types are recognized:
 * <ul>
 * <li>{@code tuner://silence} is fix and plays silence
 * <li>{@code tuner://&lt;name&gt;} selects one of the configured urls (see below for configuration) or the error stream if the name is not configured.
 * <li>{@code tunein://&lt;name&gt;} selects the TuneIn channel with the given id. To find out the id of a stream, go to <a href="https://tunein.com">TuneIn</a>
 * and search for the stream. The id is the last part (beginning with {@code s}) of the url. For example, searching for "WDR 2" results in <a href="https://tunein.com/radio/WDR-2-Rheinland-1004-s213886/">https://tunein.com/radio/WDR-2-Rheinland-1004-s213886/</a>
 * and the id is {@code s213886}.
 * </ul>
 * Configuration: Two (optional) parameters can be set:
 * <ul>
 * <li>{@code errorURL} denotes the url used if an url cannot be resolved (for example if {@code tuner://unknown} is requested but {@code unknown} is not a
 * configured URL. This field defaults to {@code tuner://silence} but if somebody wants WDR 2 as it's favourite error stream it can be set to {@code tunein://s213886}.
 * <li>{@code urls} is a map between symbolic names and real urls (including {@code tuner://silence}). The stream can than selected using {@code tuner://&lt;symbolic name&gt;}. After
 * <pre>
 * urls:
 * - key: wdr2
 *   value: tunein://s213886
 * </pre>
 * it's possible to select "WDR 2" using tuner://wdr2.
 * </ul>
 * 
 * @author notalexa
 * 
 */
public class Tuner extends AbstractPlayer<Tuner.StreamEntry> implements AudioPlayer {
	private final Logger LOGGER=LoggerFactory.getLogger(Tuner.class);
	private final AudioInfo DEFAULT_INFO=new AudioInfo(null,"Tuner",-1);
	private static CodingScheme SCHEME=XMLCodingScheme.DEFAULT_SCHEME.newBuilder().setRootTag("opml").setRootType(Opml.class).build();
	
	@JsonProperty(defaultValue = "tuner://silence") protected String errorURL;
	@JsonProperty protected Map<String,String> urls;
	StreamEntry currentEntry;
	Context context=new DefaultTypeLoader(getClass().getClassLoader()).createContext();
	LinkedList<StreamEntry> forwardList=new LinkedList<>();
	private ScheduledExecutorService executorService=Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r,"tuner-scheduler");
		}
	});

	public Tuner() {
		errorURL="tuner://silence";
	}
	
	public Tuner add(String key,String url) {
		urls.put(key, url);
		return this;
	}
	
	protected void play(StreamEntry entry) {
		play(false,entry);
	}
	
	protected void play(boolean force,StreamEntry entry) {
		if(entry!=currentEntry) {
			currentEntry=entry;
			history.add(entry);
			play(entry.open());
			LOGGER.info("Play {}",entry.url);
		} else if(force) {
			play(entry.open());
		}
	}

	@Override
	public int supports(String uri) {
		if("player:tuner".equals(uri)||uri.indexOf("://")>0) {
			return 0;
		} else {
			return urls==null?Integer.MAX_VALUE:urls.containsKey(uri)?0:Integer.MAX_VALUE;
		}
	}
	
	@Override
	public PlayerState getState() {
		state.capabilities&=~(PlayerState.NEXT_TRACK|PlayerState.PREVIOUS_TRACK);
		if(forwardList.size()>0) {
			state.capabilities|=PlayerState.NEXT_TRACK;
		}
		if(history.hasHistory()) {
			state.capabilities|=PlayerState.PREVIOUS_TRACK;
		}
		return super.getState();
	}
	
	protected StreamEntry resolveUrl(String uri) {
		return resolveUrl(uri,true);
	}
	protected StreamEntry resolveUrl(String uri,boolean resolveTunerUrls) {
		if("player:tuner".equals(uri)) {
			return currentEntry==null?null:new StreamEntry(currentEntry.url);
		}
		if(uri.startsWith("tunein://")) {
			String id=uri.substring("tunein://".length());
			return new TuneInEntry(id);
		} else if(resolveTunerUrls&&uri.startsWith("tuner://")) {
			return resolveUrl(urls==null?errorURL:urls.getOrDefault(uri.substring("tuner://".length()),errorURL),false);
		} else if(uri.indexOf("://")>0) {
			return new StreamEntry(uri);
		} else {
			return new StreamEntry(errorURL);
		}
	}

	@Override
	public boolean play(String uri) {
		StreamEntry entry=resolveUrl(uri);
		if(entry!=null) {
			if(currentEntry==null||!currentEntry.url.equals(entry.url)) {
				forwardList.clear();
			}
			play(entry);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void nextTrack() {
		if(forwardList.size()>0) {
			StreamEntry entry=forwardList.removeFirst();
			play(entry);
		}
	}
	
	@Override
	public void previousTrack() {
		StreamEntry entry=history.peek();
		if(entry!=null) {
			if(forwardList.size()>10) {
				forwardList.removeLast();
			}
			forwardList.set(0,currentEntry);
			play(entry);
		}
	}
	
	protected AudioInfo getStateInfo() {
		return currentEntry==null?null:currentEntry.resolveOverview();
	}

	public class StreamEntry implements Runnable {
		protected String url;
		protected int delay=125;
		
		protected StreamEntry(String url) {
			this.url=url;
		}
		
		public void reopen(Throwable cause) {
			if(cause!=null) {
				LOGGER.warn("Request to reopen "+url+".",cause);
			}
			executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
			delay=Math.min(delay*2, 60000);
		}
		
		public void run() {
			if(currentEntry==this) {
				LOGGER.info("Reopen url {}",url);
				play(true,this);
			} else {
				LOGGER.info("Not active anymore (url {}).",url);
			}
		}

		protected MP3AudioStream resolveStream(String url) throws IOException, BitstreamException {
			URLConnection con=new URL(url).openConnection();
			con.setReadTimeout(3000);
			if("audio/x-mpegurl".equals(con.getContentType())) try(BufferedReader reader=new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String line;
				while((line=reader.readLine())!=null) try {
					return resolveStream(line.trim());
				} catch(Throwable t) {
				}
				throw new FileNotFoundException(url);
			} else if("audio/x-scpls".equals(con.getContentType())) try(BufferedReader reader=new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String line;
				while((line=reader.readLine())!=null) try {
					line=line.trim();
					if(line.startsWith("File")) {
						return resolveStream(line.substring(line.indexOf('=')+1).trim());
					}
				} catch(Throwable t) {
				}
				throw new FileNotFoundException(url);
			}
			return new MP3AudioStream(new FilterInputStream(con.getInputStream()) {
				boolean closed;
					@Override
				public void close() throws IOException {
					if(!closed) try {
						closed=true;
						super.close();
					} catch(IOException e) {
					}
				}
				
				@Override
				public int read() throws IOException {
					if(closed) {
						return -1;
					} else try {
						return super.read();
					} catch(IOException e) {
						LOGGER.warn("Read from {} failed: {}.",url,e.getMessage());
						close();
						reopen(e);
						return -1;
					}
				}
	
				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					if(closed) {
						return -1;
					} else try {
						return super.read(b, off, len);
					} catch(IOException e) {
						LOGGER.warn("Read from {} failed: {}.",url,e.getMessage());
						close();
						reopen(e);
						return -1;
					}
				}
			},getClass().getSimpleName(),1f,con.getContentLengthLong()) {

				@Override
				protected boolean handleEOS(ByteBuffer buffer) {
					if(size>0) {
						return super.handleEOS(buffer);
					} else {
						// We fill up with silence (typically, a reopen will replace this stream later)
						buffer.clear();
						buffer.put(new byte[buffer.capacity()]);
						// Not end of stream.
						return false;
					}
				}
				
				@Override
				public void close() {
					super.close();
				}
				
			};
		}

		public AudioStream open() {
			AudioStream in;
			try {
				in="tuner://silence".equals(url)?new Silence():resolveStream(url);
				delay=125;
			} catch(Throwable t) {
				reopen(t);
				in=getCurrent();
			}
			return in;
		}

		public String getURL() {
			return url;
		}
		
		public AudioInfo resolveAudioInfo() {
			return DEFAULT_INFO;
		}
		public AudioInfo resolveOverview() {
			return resolveAudioInfo();
		}
	}
	
	public class TuneInEntry extends StreamEntry {
		String id;
		AudioInfo info;
		AudioInfo overview;
		long infoTime;
		protected TuneInEntry(String id) {
			super(Opml.getUrl(id));
			this.id=id;
		}
		
		@Override
		public AudioInfo resolveAudioInfo() {
			if(info==null||System.currentTimeMillis()-15000>infoTime) {
				info=overview=DEFAULT_INFO;
				infoTime=System.currentTimeMillis();
				try(InputStream stream=new URL(Opml.getDescriptionUrl(id)).openStream();
						Decoder decoder=SCHEME.createDecoder(context, stream)) {
					Opml opml=decoder.decode(Opml.class);
					info=opml==null?DEFAULT_INFO:opml.getAudioInfo();
					overview=opml==null?DEFAULT_INFO:opml.getOverview();
				} catch(Throwable t) {
					context.getLogger().error("Failed to get description of "+id+".",t);
				}
			}
			return info;
		}
		
		@Override 
		public AudioInfo resolveOverview() {
			if(overview==null) {
				resolveAudioInfo();
			}
			return overview;
		}
	}

	@Override
	public AudioInfo getCurrentInfo() {
		return currentEntry==null?super.getCurrentInfo():currentEntry.resolveAudioInfo();
	}
}
