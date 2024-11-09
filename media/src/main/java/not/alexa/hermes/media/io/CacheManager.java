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
package not.alexa.hermes.media.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.AudioStream.AudioInfo;
import not.alexa.hermes.media.AudioStream.MediaType;
import not.alexa.hermes.media.playback.Callback;
import not.alexa.hermes.media.playback.PlaybackTrack;
import not.alexa.hermes.media.playback.PlaybackTrackFacade;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.json.JsonCodingScheme;

/**
 * {@link PlaybackTrackFacade} implementing a simple cache. Configurable is the cache size limit and
 * the location (directory) of the cache.
 */
public class CacheManager implements PlaybackTrackFacade {
	private static final Logger LOGGER=LoggerFactory.getLogger(CacheManager.class);
	private Context context;
	private File cacheDir;
	private long limit;
	private CacheMap metadataMap=new CacheMap();
	private boolean unusable;
	
	/**
	 * Creates a cache manager at the given location with a limit of 20Mb.
	 * 
	 * @param context the context to use for caching (must be able to resolve {@link CacheEntry})
	 * @param cacheDir the caching location to use
	 */
	public CacheManager(Context context,String cacheDir) {
		this(context,cacheDir,20*1024*1024);
	}

	/**
	 * Creates a cache manager at the given location with a choosable limit.
	 * 
	 * @param context the context to use for caching (must be able to resolve {@link CacheEntry})
	 * @param cacheDir the caching location to use
	 * @param limit the limit to use
	 */
	public CacheManager(Context context,String cacheDir,long limit) {
		this.cacheDir=new File(cacheDir);
		this.limit=limit;
		this.context=context;
		if(!this.cacheDir.exists()) {
			if(!this.cacheDir.mkdirs()) {
				unusable=true;
			}
		}
		unusable|=!(this.cacheDir.isDirectory()&&this.cacheDir.canRead()&&this.cacheDir.canWrite());
		if(!unusable) {
			File[] metaData=this.cacheDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".meta");
				}
			});
			unusable|=metaData!=null;
			if(!unusable) {
				Arrays.sort(metaData,new Comparator<File>() {
					@Override
					public int compare(File o1, File o2) {
						if(o1.lastModified()<o2.lastModified()) {
							return -1;
						} else if(o1.lastModified()>o2.lastModified()) {
							return 1;
						}
						return o1.getName().compareTo(o2.getName());
					}
				});
				for(File f:metaData) {
					metadataMap.put(new Metadata(f));
				}
			} else {
				LOGGER.warn("Caching is disabled (location={}).",this.cacheDir.getAbsolutePath());
			}
		}
	}
	
	@Override
	public PlaybackTrack decorate(PlaybackTrack info) {
		return info==null||unusable?info:new PlaybackTrack() {
			
			@Override
			public int startPos() {
				return info.startPos();
			}
			
			@Override
			public AudioStream load(Callback listener) throws IOException {
				return CacheManager.this.load(info,listener);
			}
			
			@Override
			public MediaType getMediaType() {
				return info.getMediaType();
			}
			
			@Override
			public String getId() {
				return info.getId();
			}
			
			@SuppressWarnings("unchecked")
			@Override
			public <T extends PlaybackTrack> T as(Class<T> clazz) {
				if(clazz.isInstance(info)) {
					return (T)info;
				} else {
					return PlaybackTrack.super.as(clazz);
				}
			}
			
			public String toString() {
				return info.toString();
			}
		};
	}

	private AudioStream load(PlaybackTrack spec, Callback haltListener) throws IOException {
		String id=spec.getId()+".meta";
		Metadata cached=metadataMap.get(id);
		while(true) {
			if(cached==null) {
				DownloadHandler downloadHandler=new DownloadHandler(haltListener);
				AudioStream stream=spec.load(downloadHandler);
				downloadHandler.metadata=new Metadata(new File(cacheDir,id),new CacheEntry(spec.getMediaType(),stream.getCurrentInfo()));
				return stream;
			}
			LOGGER.info("Track {} found in cache.",spec.getId());
			cached.file.setLastModified(System.currentTimeMillis());
			metadataMap.put(cached);
			AudioStream stream=cached.create();
			if(stream!=null) {
				return stream;
			} else {
				System.out.println("Invalid cache entry");
				cached=null;
			}
		}
	}
	
	private class Metadata {
		Metadata next;
		Metadata prev;
		File file;
		CacheEntry entry;
		
		Metadata(File file) {
			this.file=file;
		}

		Metadata(File file,CacheEntry entry) {
			this.file=file;
			this.entry=entry;
		}
		
		protected void prepare() throws IOException, BaseException {
			try(OutputStream out=new FileOutputStream(file)) {
				out.write(JsonCodingScheme.REST_SCHEME.createEncoder(context).encode(entry).asBytes());
			}
		}
		
		protected AudioStream create() throws IOException {
			File audioFile=getAudioFile();
			CacheEntry entry=getWrapper();
			return entry==null?null:entry.create(new SeekableFileInputStream(audioFile),(int)audioFile.length());
		}

		CacheEntry getWrapper() {
			if(entry==null) {
				synchronized (this) {
					if(entry==null) try(InputStream in=new FileInputStream(file)) {
						entry=JsonCodingScheme.REST_SCHEME.createDecoder(context, in).decode(CacheEntry.class);
					} catch(IOException|BaseException e) {
						e.printStackTrace();
					}
				}
			}
			return entry;
		}
		
		public File getAudioFile() {
			String name=file.getName().substring(0,file.getName().lastIndexOf('.'))+".audio";
			return new File(cacheDir,name);
		}

		public void invalidate() {
			file.delete();
			getAudioFile().delete();
		}		
	}
	
	private class CacheMap extends HashMap<String,Metadata> {
		private static final long serialVersionUID = 1L;
		Metadata first;
		Metadata last;
		long totals;
		
		public void put(Metadata metadata) {
			Metadata old=put(metadata.file.getName(),metadata);
			if(old==null) {
				totals+=metadata.getAudioFile().length();
				while(totals>limit&&last!=null) {
					totals-=last.getAudioFile().length();
					last.invalidate();
					remove(last.file.getName());
					if(last.prev!=null) {
						last.prev.next=null;
					} else {
						first=null;
					}
					last=last.prev;					
				}
				// Check for space
			} else {
				if(old.next!=null) {
					old.next.prev=old.prev;
				} else {
					last=old.prev;
				}
				if(old.prev!=null) {
					old.prev.next=old.next;
				} else {
					first=old.next;
				}
			}
			if(first!=null) {
				first.prev=metadata;
			}
			metadata.next=first;
			first=metadata;
			if(last==null) {
				last=first;
			}
		}
	}
	
	public class DownloadHandler implements Callback {
		Callback delegate;
		Metadata metadata;
		private DownloadHandler(Callback delegate) {
			this.delegate=delegate;
		}
		

		@Override
		public void downloadComplete(DownloadEvent download) {
			if(delegate!=null) {
				delegate.downloadComplete(download);
			}
			if(metadata!=null) {
				try(OutputStream out=new FileOutputStream(metadata.getAudioFile())) {
					metadata.entry.info=download.downloaded(metadata.entry.info,out);
					metadata.prepare();
					metadataMap.put(metadata);
				} catch(IOException|BaseException e) {
					e.printStackTrace();
					metadata.invalidate();
				}

			}
		}

		@Override
		public void downloadFailed(Throwable cause) {
			delegate.downloadFailed(cause);
		}
	}
	
	static class CacheEntry {
		@JsonProperty AudioInfo info;
		@JsonProperty MediaType type;
		CacheEntry() {}
		public CacheEntry(MediaType type,AudioInfo info) {
			this.type=type;
			this.info=info;
		}
		
		public void setNormalizationData(float gain,float peak) {
		}
		
		public float getNormalizationFactor() {
			return 1;
		}
		
		public AudioStream create(InputStream in,int size) throws IOException {
			return type.create(in, size, info, getNormalizationFactor());
		}
	}
}
