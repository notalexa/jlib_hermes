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
package not.alexa.hermes.media.players.dlna;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Field;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;

/**
 * Protocol specific class for DLNA.
 * 
 * @author notalexa
 */
public class DidlLite {
	private static final Random RANDOM=new Random();
	private static final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor();
	@JsonProperty List<Track> item;
	@JsonProperty List<Container> container;
	private DidlResolver resolver;
	private Future<DidlLite> futuredNext;
	private int offset;
	private int totals;
	private int returned;
	
	DidlLite() {
	}
	
	public DidlLite bind(DidlResolver resolver,int offset,int returned, int totals) {
		this.offset=offset;
		this.returned=returned;
		this.totals=totals;
		this.resolver=resolver;
		if(offset==0) {
			if(totals==returned) {
				if(item!=null) {
					resolver.sort(item);
				}
			} else {
				trigger();
			}
		}
		return this;
	}
	
	private DidlLite trigger() {
		if(futuredNext==null&&(offset+returned)<totals) {
			futuredNext=executor.submit(new Callable<DidlLite>() {
				@Override
				public DidlLite call() throws Exception {
					return resolver.resolve(offset+returned);
				}
			});
		}
		return this;		
	}
	
	private DidlLite next() {
		if(futuredNext!=null) try {
			DidlLite didl=futuredNext.get();
			return didl==null?null:didl.trigger();
		} catch(Throwable t) {
		}
		return null;
	}
	
	private Stream<Track> filteredTracks(Set<String> excluded) {
		DidlLite next=next();
		return Stream.concat(item==null?Stream.empty():item.stream().filter(track -> track.isAudio() && !excluded.contains(track.id)),next==null?Stream.empty():next.filteredTracks(excluded));
	}

	private Stream<Container> filteredContainer(Set<String> excluded) {
		DidlLite next=next();
		return Stream.concat(container==null?Stream.empty():container.stream().filter(track -> !excluded.contains(track.id)),next==null?Stream.empty():next.filteredContainer(excluded));
	}

	Content<?> random(Set<String> excluded) {
		int n=totals-excluded.size();
		if(n>0) {
			int j=0;
			if(n>1) {
				synchronized (RANDOM) {
					j=RANDOM.nextInt(n);
				}
			}
			List<Track> tracks=filteredTracks(excluded).collect(Collectors.toList());
			if(tracks.size()>0&&j<item.size()) {
				return tracks.get(j%tracks.size());
			}
			List<Container> container=filteredContainer(excluded).collect(Collectors.toList());
			if(container.size()>0) {
				return container.get(j%container.size());
			} else if(tracks.size()>0) {
				return tracks.get(j%tracks.size());
			}
		}
		return null;
	}
	
	public Track getFirstTrack() {
		return item!=null&&item.size()>0?item.get(0):null;
	}

	public Container getFirstContainer() {
		return container!=null&&container.size()>0?container.get(0):null;
	}

	public static class Content<T> {
		@Field(type="xml",name="@id")
		@JsonProperty String id;
		@Field(type="xml",name="@parentID")
		@JsonProperty String parentID;
		@Field(type="xml",name="@restricted")
		@JsonProperty int restricted;		
		
		protected Container createContainer(String id) {
			return new Container(id);
		}
		
		public String getId() {
			return id;
		}
		
		public String getParentId() {
			return parentID;
		}
		
		public final Container getParent() {
			return parentID==null?null:createContainer(parentID);
		}
		
		protected boolean update(Content<?> content) {
			if(content.id.equals(id)) {
				this.parentID=content.parentID;
				this.restricted=content.restricted;
				return true;
			} else {
				return false;
			}
		}
		
		@SuppressWarnings("unchecked")
		public T update() {
			return (T)this;
		}
	}
	
	public static class Container extends Content<Container> {
		@Field(type="xml",name="@searchable")
		@JsonProperty int searchable;
		@Field(type="xml",name="@childCount")
		@JsonProperty int childCount;
		@JsonProperty String title;
		@Field(type="xml",name="class")
		@JsonProperty String upnpClass;

		protected Container() {}

		protected Container(String id) {
			this.id=id;
			this.title=id;
			this.upnpClass="object.container";
		}
		
		public String getTitle() {
			return title;
		}
		
		public Iterable<Container> resolveContainer() {
			return Collections.emptyList();
		}
		
		protected List<Track> filter(List<Track> items) {
			return items.stream().filter(item -> item.isAudio()).collect(Collectors.toList());
		}

		public Iterable<Track> resolveTracks() {
			return Collections.emptyList();
		}
		
		public Container next() {
			Container parent=getParent();
			if(parent!=null) {
				for(Iterator<Container> containers=parent.resolveContainer().iterator();containers.hasNext();) {
					Container c=containers.next();
					if(c.getId().equals(getId())) {
						if(containers.hasNext()) {
							return containers.next();
						}
					}
				}
				return parent.next();
			} else {
				// Root container: Return first
				for(Container c:resolveContainer()) {
					return c;
				}
			}
			return null;
		}
		
		public Track nextItem(Container previous) {
			return null;
		}
		
		public Track first() {
			Iterator<Track> items=resolveTracks().iterator();
			if(items.hasNext()) {
				return items.next();
			} else {
				for(Container container:resolveContainer()) {
					Track t=container.first();
					if(t!=null) {
						return t;
					}
				}
				return null;
			}
		}
		
		@Override
		public boolean update(Content<?> content) {
			if(content instanceof Container) {
				return update((Container)content);
			} else {
				return false;
			}
		}
		
		public boolean update(Container other) {
			if(super.update(other)) {
				searchable=other.searchable;
				childCount=other.childCount;
				title=other.title;
				upnpClass=other.upnpClass;
				return true;
			} else {
				return false;
			}
		}
		
		public Content<?> random(Set<String> excluded) {
			return null;
		}
		
		public Track randomTrack() {
			return null;
		}
				
		public String toString() {
			return "Container["+id+"]";
		}

		public boolean hasTracks() {
			return resolveTracks().iterator().hasNext();
		}

		public Track shuffle(Track t) {
			return null;
		}
	}
	
	public static class Track extends Content<Track> {
		@JsonProperty String title;
		@Field(type="xml",name="class")
		@JsonProperty String upnpClass;
		@JsonProperty(defaultValue="-1") int originalTrackNumber;
		@JsonProperty Res res;
		@JsonProperty String artist;
		@JsonProperty String creator;
		
		protected Track() {}
		
		public String getTitle() {
			return title;
		}
		
		public String getArtist() {
			return artist==null?creator:artist;
		}
		
		public int getSize() {
			return res==null?-1:res.size;
		}
		
		public InputStream open() throws IOException {
			if(res!=null&&res.uri!=null) try {
				return new URL(res.uri).openStream();
			} catch(MalformedURLException e) {
				throw new FileNotFoundException(res.uri);
			}
			throw new IOException("Not a track");
		}		
		
		public boolean isAudio() {
			return res!=null&&res.protocolInfo.indexOf("audio")>0;
		}
		public boolean isImage() {
			return res!=null&&res.protocolInfo.indexOf("image")>0;
		}
		
		public final Track next(boolean repeat) {
			Container parent=getParent().update();
			Track first=null;
			for(Iterator<Track> tracks=parent.resolveTracks().iterator();tracks.hasNext();) {
				Track track=tracks.next();
				if(first==null) {
					first=track;
				}
				if(track.id.equals(id)) {
					if(tracks.hasNext()) {
						return tracks.next();
					} else if(repeat) {
						return first;
					}
				}
			}
			Track item=parent.nextItem(parent);
			return item==null?this:item;
		}
		
		public final Track first() {
			return getParent().first();
		}
		
		public String toString() {
			return "Track["+id+":"+title+"]";
		}
		
		@Override
		public boolean update(Content<?> content) {
			if(content instanceof Track) {
				return update((Track)content);
			} else {
				return false;
			}
		}
		
		public boolean update(Track other) {
			if(super.update(other)) {
				title=other.title;
				upnpClass=other.upnpClass;
				originalTrackNumber=other.originalTrackNumber;
				res=other.res;
				return true;
			} else {
				return false;
			}
		}
	}
	
	public static class Res {
		@Field(type="xml",name="@protocolInfo")
		@JsonProperty String protocolInfo;
		@Field(type="xml",name="@size")
		@JsonProperty int size;
		@Field(type="xml",name="@duration")
		@JsonProperty String duration;
		@Field(type="xml",name="#text")
		@JsonProperty String uri;
		
	}
	
	public interface DidlResolver {
		public DidlLite resolve(int offset) throws BaseException;
		public void sort(List<Track> item);
	}

	/**
	 * 
	 * Soap Envelope for {@link DidlLite}.
	 * 
	 * @author notalexa
	 */
	static class Envelope {
		private static CodingScheme SCHEME=XMLCodingScheme.REST_SCHEME.newBuilder().setRootTag("DIDL-Lite").build();
		@JsonProperty(value="Body") public Body body;
		public Envelope() {
		}
		
		public int getMatches() {
			if(body!=null&&body.browseResponse!=null) {
				return body.browseResponse.totalMatches;
			} else {
				return -1;
			}
		}
		
		public DidlLite getResult(Context context,DidlResolver resolver,int offset) throws BaseException {
			if(body!=null&&body.browseResponse!=null&&body.browseResponse.result!=null) {
				String data=body.browseResponse.result;
				try(Decoder decoder2=SCHEME.createDecoder(context, data.getBytes(Charset.forName("UTF-8")))) {
					DidlLite didl=decoder2.decode(DidlLite.class);
					return didl==null?null:didl.bind(resolver,offset,body.browseResponse.numberReturned,body.browseResponse.totalMatches);
				}
			} else {
				return null;
			}
		}
		
		static class Body {
			@JsonProperty(value="BrowseResponse") BrowseResponse browseResponse;
		}
		
		static class BrowseResponse {
			@JsonProperty(value="Result") public String result;
			@JsonProperty(value="NumberReturned") public int numberReturned;
			@JsonProperty(value="TotalMatches") public int totalMatches;
		}
	}
}
