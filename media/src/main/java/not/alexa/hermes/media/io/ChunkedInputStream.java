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

import java.io.IOException;
import java.io.OutputStream;

import not.alexa.hermes.media.AudioStream.AudioInfo;
import not.alexa.hermes.media.playback.Callback;

/**
 * A seekable input stream based on a {@link ChunkProvider}.
 * 
 * @author notalexa
 * @see ChunkProvider
 */
public class ChunkedInputStream extends SeekableInputStream {
	private final long size;
	private long pos;
	private int chunkSize;
	private int chunk;
	private int offset;
	private byte[][] chunks;
	private int chunksLoaded;
	private Object waitLock=new Object();
	private int timeout;
	private ChunkProvider chunkProvider;
	private IOException loaderException;
	private boolean exceptionThrown;
	
	/**
	 * 
	 * @param timeout the timeout for loading a chunk
	 * @param chunkProvider the chunk provider responsible for loading chunks
	 */
	public ChunkedInputStream(int timeout,ChunkProvider chunkProvider) {
		this(timeout,chunkProvider,new byte[1+(int)((chunkProvider.size()/chunkProvider.chunkSize()))][]);
	}
	
	private ChunkedInputStream(int timeout,ChunkProvider chunkProvider,byte[][] chunks) {
		this.chunks=chunks;
		this.timeout=timeout;
		this.chunkProvider=chunkProvider;
		this.chunkSize=chunkProvider.chunkSize();
		this.size=chunkProvider.size();
		preload(0);
	}
	
	@Override
	public int read() throws IOException {
		if(pos==size) {
			if(loaderException!=null) {
				throw loaderException;
			}
			return -1;
		}
		if(offset==chunkSize) {
			this.chunk++;
			this.offset=0;
		}
		try {
			byte[] chunk=requestChunk(this.chunk,true);
			pos++;
			return chunk[offset++]&0xff;
		} catch(IOException e) {
			if(!exceptionThrown) {
				exceptionThrown=true;
				chunkProvider.downloadFailed(e);
			}
			throw e;
		}
	}

	@Override
	public int read(byte[] buffer, int index, int length) throws IOException {
		if(pos==size) {
			if(loaderException!=null) {
				throw loaderException;
			}
			return -1;
		}
		if(offset==chunkSize) {
			this.chunk++;
			preload(this.chunk);
			this.offset=0;
		}
		try {
			byte[] chunk=requestChunk(this.chunk,true);
			if(buffer==null||chunk==null) {
				throw new IOException("Internal chunk error"+buffer+", "+chunk);
			}
			int n=(int)Math.min(length,Math.min(chunkSize-offset,size-pos));
			System.arraycopy(chunk,offset, buffer,index, n);
			pos+=n;
			offset+=n;
			return n;
		} catch(IOException e) {
			if(!exceptionThrown) {
				exceptionThrown=true;
				chunkProvider.downloadFailed(e);
			}
			throw e;
		}
	}

	@Override
	public boolean seekTo(long pos) {
		try {
			int chunk=(int)(pos/chunkSize);
			requestChunk(chunk,false);
			this.chunk=chunk;
			offset=(int)(pos%chunkSize);
			this.pos=(int)pos;
			return true;
		} catch(Throwable t) {
			return false;			
		}
	}
	
	@Override
	public long size() {
		return size;
	}
	
	@Override
	public long currentPosition() {
		return pos;
	}

	@Override
	public void close() {
	}
	
	public void writeChunk(int c,byte[] buffer) {
		if(buffer.length>chunkSize||(buffer.length<chunkSize&&c!=chunks.length-1)) {
			loaderException=new IOException("Illegal chunk size");
			pos=size;
		} else if(chunks[c]==null){
			chunks[c]=buffer;
			chunksLoaded++;
			synchronized (waitLock) {
				waitLock.notifyAll();
			}
			if(chunksLoaded==chunks.length) {
				chunkProvider.downloadComplete(new Callback.DownloadEvent() {
					@Override
					public AudioInfo downloaded(AudioInfo info,OutputStream out) throws IOException {
						for(int i=0;i<chunks.length-1;i++) {
							out.write(chunks[i],0,chunkSize);
						}
						out.write(chunks[chunks.length-1],0,(int)(size%chunkSize));
						return info;
					}
				});
			}
		}
	}
	
	protected void preload(int chunk) {
		int to=Math.min(chunks.length,chunk+(chunk<5?5:chunks.length));
		try {
			for(int i=chunk+1;i<to;i++) {
				if(chunks[i]==null) {
					requestChunk(i, false);
				}
			}
		} catch(Throwable t) {
		}
	}
	
	protected byte[] requestChunk(int chunk,boolean wait) throws IOException {
		byte[] buffer=chunks[chunk];
		if(buffer==null) {
			synchronized(this) {
				if(buffer==null) {
					buffer=chunkProvider.requestChunk(chunk,this::writeChunk);
				}
			}
			if(wait) {
				long waitTime=0;
				while(buffer==null&&loaderException==null) try {
					synchronized (waitLock) {
						waitLock.wait(100);
						buffer=chunks[chunk];
						waitTime+=100;
						if(waitTime>timeout) {
							throw new IOException("Timeout while waiting for chunk #"+chunk);
						}
					}
				} catch(InterruptedException e) {
					throw new IOException(e);
				}
			}
			if(loaderException!=null) {
				throw loaderException;
			}
		}
		return buffer;
	}
}
