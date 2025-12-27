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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import not.alexa.hermes.media.players.DlnaPlayer;
import not.alexa.hermes.media.players.dlna.DidlLite.Envelope;
import not.alexa.hermes.media.players.dlna.DidlLite.Container;
import not.alexa.hermes.media.players.dlna.DidlLite.Content;
import not.alexa.hermes.media.players.dlna.DidlLite.DidlResolver;
import not.alexa.hermes.media.players.dlna.DidlLite.Track;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;

/**
 * Browse an DLNA Media Server.
 * 
 * @author notalexa
 * @see DlnaPlayer
 */
public class DlnaBrowser {
	private static final Comparator<Track> DEFAULT_TRACK_COMPARATOR=new Comparator<Track>() {
		@Override
		public int compare(Track arg0, Track arg1) {
			int result=arg0.originalTrackNumber-arg1.originalTrackNumber;
			if(result==0&&arg0.title!=null) {
				result=arg0.title.compareTo(arg1.title);
			}
			return result;
		}
	};

	private static String REQUEST_TEMPLATE="<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
			+ "<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
			+ "   <s:Body>\n"
			+ "      <u:Browse xmlns:u=\"urn:schemas-upnp-org:service:ContentDirectory:1\">\n"
			+ "         <ObjectID>${id}</ObjectID>\n"
			+ "         <BrowseFlag>BrowseDirectChildren</BrowseFlag>\n"
			+ "         <Filter>*</Filter>\n"
			+ "         <StartingIndex>${offset}</StartingIndex>\n"
			+ "         <RequestedCount>100</RequestedCount>\n"
			+ "         <SortCriteria>+dc:title</SortCriteria>\n"
			+ "      </u:Browse>\n"
			+ "   </s:Body>\n"
			+ "</s:Envelope>";

	private Context context;
	private String controlUri;
	private final Container root=new ContainerImpl("0");
	
	public DlnaBrowser(Context context,String controlUri) {
		this.context=context.getTypeLoader().overlay(ContainerImpl.class,ItemImpl.class).createContext();
		this.context.putAdapter(this);
		this.controlUri=controlUri;
	}
	
	public Container getRootContainer() {
		return root;
	}
	
	public Content<?> resolve(String id) throws BaseException {
		DidlLite didl=resolve(null,id,true);
		Track t=didl.getFirstTrack();
		if(t!=null) {
			return t;
		} else {
			return didl.getFirstContainer();
		}
	}
	
	public String getControlUri() {
		return controlUri;
	}
	
	private DidlLite resolve(String clazz,String id,boolean self) throws BaseException {
		return resolve(clazz,id,self,0);
	}
	
	private DidlLite resolve(String clazz,String id,boolean self,int offset) throws BaseException {
		try {
			HttpURLConnection con=(HttpURLConnection)new URL(controlUri).openConnection();
			con.addRequestProperty("Content-Type","text/xml");
			con.addRequestProperty("SOAPACTION", "urn:schemas-upnp-org:service:ContentDirectory:1#Browse");
			con.addRequestProperty("Connection", "close");
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.connect();
			try(OutputStream out=con.getOutputStream()) {
				byte[] request=(self?REQUEST_TEMPLATE.replace("BrowseDirectChildren", "BrowseMetadata"):REQUEST_TEMPLATE).replace("${id}", id).replace("${offset}",Integer.toString(offset)).getBytes();
				out.write(request);
				out.flush();
			}
			if(con.getResponseCode()==200) try(InputStream in=con.getInputStream()) {
				ByteArrayOutputStream out=new ByteArrayOutputStream();
				byte[] buf=new byte[8192];
				int n;
				while((n=in.read(buf))>=0) {
					out.write(buf,0,n);
				}
				String s=out.toString();
				try(Decoder decoder=XMLCodingScheme.REST_SCHEME/*.newBuilder().setRootTag("Envelope").build()*/.createDecoder(context, s.getBytes(Charset.forName("UTF-8")))) {
					Envelope envelope=decoder.decode(Envelope.class);
					DidlLite didl= envelope.getResult(context,new Resolver(clazz,id,self), offset);
					return didl;
				}
			} else {
				throw new BaseException(BaseException.NOT_FOUND, "DLNA Content with id "+id);
			}
		} catch(IOException e) {
			return BaseException.throwException(e);
		}

	}
	
	@Overlay
	public class ContainerImpl extends Container {
		DidlLite didl;
		long lastUpdate;

		ContainerImpl() {
			super();
			lastUpdate=System.currentTimeMillis();
		}

		private ContainerImpl(String id) {
			super(id);
		}

		@Override
		public Container createContainer(String id) {
			return new ContainerImpl(id);
		}
		
		@Override
		public Content<?> random(Set<String> excluded) {
			if(didl==null) try {
				didl=resolve(upnpClass,id,false);
			} catch(BaseException e) {
				e.printStackTrace();
				return null;
			}
			return didl.random(excluded);
		}
		
		@Override
		public Track randomTrack() {
			return DlnaBrowser.this.random(this);
		}

		@Override
		public List<Container> resolveContainer() {
			if(didl==null) try {
				didl=resolve(upnpClass,id,false);
			} catch(BaseException e) {
				e.printStackTrace();
				return Collections.emptyList();
			}
			return didl==null||didl.container==null?Collections.emptyList():didl.container;
		}
		
		public Container update() {
			if(!id.equals("0")) {
				if(lastUpdate<System.currentTimeMillis()-30000) try {
					lastUpdate=System.currentTimeMillis();
					didl=null;
					update(resolve(upnpClass,id,true).getFirstContainer());
				} catch(Throwable t) {
					t.printStackTrace();
				}
			}
			return super.update();
		}

		@Override
		public Iterable<Track> resolveTracks() {
			if(didl==null) try {
				didl=resolve(upnpClass,id,false);
			} catch(BaseException e) {
				e.printStackTrace();
				return Collections.emptyList();
			}
			return didl==null||didl.item==null?Collections.emptyList():filter(didl.item);
		}

		@Override
		public Track nextItem(Container previous) {
			if(previous.id.equals(id)) {
				Container parent=getParent();
				if(parent==null) {
					if(id.equals("0")) {
						return first();
					}
					return null;
				} else {
					return parent.update().nextItem(this);
				}
			} else {
				Container anchor=null;
				for(Container c:resolveContainer()) {
					if(c.id.equals(previous.id)) {
						anchor=previous;
					} else if(anchor!=null) {
						Track track=c.first();
						if(track!=null) {
							return track;
						}
					}
				}
				return nextItem(this);
			}
		}
	}
	
	@Overlay
	public class ItemImpl extends Track {
		long lastUpdate;
		protected ItemImpl() {
			super();
			lastUpdate=System.currentTimeMillis();
		}

		@Override
		public Container createContainer(String id) {
			return new ContainerImpl(id);
		}

		@Override
		public Track update() {
			if(lastUpdate<System.currentTimeMillis()-30000) try {
				lastUpdate=System.currentTimeMillis();
				update(resolve(upnpClass,id,true).getFirstTrack());
			} catch(Throwable t) {
				t.printStackTrace();
			}
			return super.update();
		}
	}
		
	private Track random(Container container) {
		Set<String> excluded=new HashSet<String>();
		while(true) {
			Content<?> content=container.random(excluded);
				Track track=context.cast(Track.class, content);
				if(track!=null) {
					return track;
				} else {
					Container c=context.cast(Container.class,content);
					if(c!=null) {
						track=random(c);
						if(track!=null) {
							return track;
						} else {
							excluded.add(c.id);
						}
					} else {
						return null;
					}
				}
		}
		
	}
	
	public Track random() {
		return random(getRootContainer());
	}
	
	public class Resolver implements DidlResolver {
		String clazz;
		String id;
		boolean self;
		private Resolver(String clazz,String id,boolean self) {
			this.clazz=clazz;
			this.id=id;
			this.self=self;
		}
		
		public DidlLite resolve(int offset) throws BaseException {
			return DlnaBrowser.this.resolve(clazz,id, self,offset);
		}

		@Override
		public void sort(List<Track> item) {
			if(!self&&"object.container.storageFolder".equals(clazz)) {
				Collections.sort(item,DEFAULT_TRACK_COMPARATOR);
			}
		}
	}	
}
