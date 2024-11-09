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
package not.alexa.hermes.media.playback;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.AudioStream.AudioInfo;

/**
 *
 * One entry inside the playback stream
 *
 * @author notalexa
 */
class StreamEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamEntry.class);
    private static final int PRELOAD_INTERVAL=15;
    final String playbackId;
    Listener listener;
    private AudioStream audioStream;
    int initialSeektime = -1;
    private int state;
    private AudioChunk currentChunk;
    private LinkedBlockingQueue<AudioChunk> audioQueue=new LinkedBlockingQueue<>();
    private LinkedBlockingQueue<AudioChunk> decoderQueue=null;
    private Semaphore loadingSemaphore;
    private Worker worker;
    StreamEntry next;
    PlaybackPlayer player;
    PlaybackTrack spec;
    boolean disableNextTrack;
    
    StreamEntry(PlaybackPlayer player, PlaybackTrack spec, Listener listener) {
    	this.player=player;
        this.playbackId = UUID.randomUUID().toString();
        this.listener=listener;
        this.spec=spec;
        this.worker=new Worker(this);
    }

	public AudioFormat getFormat() {
		return audioStream.getFormat();
	}

    public void close() {
    	worker.close();
    }
    
    public void load() throws IOException {
        audioStream=player.load(spec,new Callback() {
        	
        	
			@Override
			public void downloadComplete(DownloadEvent event) {
				listener.loadingFinished(StreamEntry.this, getCurrentInfo());
			}

			@Override
			public void downloadFailed(Throwable cause) {
				LOGGER.error("Download of "+spec.getId()+" failed.",cause);
				listener.loadingFailed(StreamEntry.this, cause);
			}
        });
    }
    
    public AudioInfo getCurrentInfo() {
    	return audioStream.getCurrentInfo();
    }
    
    int next() {
    	while(currentChunk==null ) try {
    		currentChunk=audioQueue.poll(100,TimeUnit.MILLISECONDS);
    		if(currentChunk==null&&state<3) {
    			return 0;
    		}
    	} catch(Throwable t) {
    		listener.playbackEnded(this,0);
    		return 0;
    	}
    	if(currentChunk.endMarker) {
    		return Integer.MAX_VALUE;
    	} else {
    		int n=currentChunk.next();
    		if(n==Integer.MAX_VALUE) {
    			AudioChunk chunk=currentChunk;
    			currentChunk=null;
    			decoderQueue.offer(chunk);
    			return next();
    		}
			if(Math.abs(n)>32768) {
				System.out.println("Illegal");
			}

    		return n;
    	}
    }

    boolean seek(int pos) {
    	boolean ret=audioStream.seekTo(pos/1000f);
    	if(ret) {
    		worker.seekTo(pos);
    		listener.playbackSeeked(this, pos);
    	}
    	return ret;
    }
    
    void disableNextTrack() {
    	disableNextTrack=true;
    }
    
    public void start() {
    	if(state==0) {
    		state=1;
    		loadingSemaphore=new Semaphore(1);
    		worker.start();
    	}
    }
    
    public void startPlaying(int pos) {
    	start();
    	initialSeektime=pos;
    	if(loadingSemaphore.tryAcquire()) try {
				listener.loadingStarted(this);
		} finally {
			loadingSemaphore.release();
		}
    	worker.latch.countDown();
    }
    
    @Override
    public String toString() {
        return "PlayerQueueEntry{" + playbackId + "}";
    }

    /**
     * Interface for stream/entry communication
     * 
     * @author notalexa
     */
    interface Listener {
    	void ready(StreamEntry entry,int pos);

        void playbackError(StreamEntry entry, Throwable t);

        void playbackEnded(StreamEntry entry,int time);
        
        void trackIsFinishing(StreamEntry track,int currentSamples,int calculatedSamples);

        void loadingStarted(StreamEntry entry);

        void loadingFailed(StreamEntry entry, Throwable t);

        void loadingFinished(StreamEntry entry, AudioInfo metadata);

		void trackChanged(StreamEntry entry);
		
		void playbackSeeked(StreamEntry entry, int seekTime);
    }
	
    /**
     * One chunk in the audio stream
     * 
     * @author notalexa
     */
	private static class AudioChunk {
		private boolean endMarker;
		private int[] buffer;
		private int offset;
		private int len;
		
		private AudioChunk() {
			this(false);
		}
		private AudioChunk(boolean endMarker) {
			this.endMarker=endMarker;
			if(!endMarker) {
				buffer=new int[1024*8];
			}
		}
		private int next() {
			if(offset<len) {
				return buffer[offset++];
			} else {
				return Integer.MAX_VALUE;
			}
		}
	}
	
	/**
	 * Handler for delivering the playback audio stream until either
	 * <ul>
	 * <li>The stream finishes.
	 * <li>The entry is garbage collected (meaning not longer in use).
	 * </ul>
	 * 
	 * @author notalexa
	 */
	private static class Worker extends Thread {
		WeakReference<StreamEntry> ref;
		StreamEntry entry;
		CountDownLatch latch=new CountDownLatch(1);
		PlaybackTrack spec;
		boolean closed;
		private int samplesDelivered;
		AudioFormat format;
		
	    public Worker(StreamEntry playerQueueEntry) {
	    	super("playback-"+playerQueueEntry.playbackId);
	    	entry=playerQueueEntry;
	    	this.spec=entry.spec;
	    	ref=new WeakReference<StreamEntry>(entry);
		}

		private StreamEntry get() {
	    	return entry==null?ref.get():entry;
	    }
		
		private void seekTo(int pos) {
			if(format!=null) {
				samplesDelivered=(int)(pos*format.getSampleRate()/1000);
			}
		}
		
		public void close() {
			closed=true;
		}

	    private boolean ready() {
			StreamEntry entry=ref.get();
			if(entry!=null) {
				LOGGER.debug("Seek to {}ms.",entry.initialSeektime);
				if(entry.initialSeektime>=0) {
					if(!entry.audioStream.seekTo(entry.initialSeektime/1000f)) {
						entry.initialSeektime=0;
						System.out.println("Seeking failed.");
					} else {
						seekTo(entry.initialSeektime);
					}
				}
				entry.listener.ready(entry,Math.max(0, entry.initialSeektime));
				entry.listener.trackChanged(entry);
				entry.initialSeektime=-1;
				entry.state=3;
				return true;
			} else {
				return false;
			}
		}
	    
	    private boolean offer(AudioChunk chunk,boolean terminate) {
	    	StreamEntry track=ref.get();
	    	if(track!=null) {
				track.audioQueue.offer(chunk);
				if(terminate) {
					track.audioQueue.add(new AudioChunk(true));
				}
				return true;
	    	} else {
	    		return false;
	    	}
	    }
	    
	    private int runInternal() {
	   		LinkedBlockingQueue<AudioChunk> decoderQueue=entry.decoderQueue=new LinkedBlockingQueue<>();
	   		decoderQueue.add(new AudioChunk());
	   		decoderQueue.add(new AudioChunk());
	   		AudioStream audioStream=entry.audioStream;
	   		AudioInfo metadata=entry.audioStream.getCurrentInfo();
	   		float time=metadata==null||audioStream.isStream()?-1:metadata.getDuration();
	   		format=audioStream.getFormat();
	   		int totalSamples=(int)(time*format.getSampleRate());
	   		int threshold=totalSamples-(int)(PRELOAD_INTERVAL*format.getSampleRate());
	        entry=null;
	        AtomicInteger chunks=new AtomicInteger();
	        boolean closed=false;
	        try {
				while(!latch.await(500,TimeUnit.MILLISECONDS)) {
					if(ref.get()==null) {
						closed=true;
						break;
					}
				}
				if(!ready()) {
					closed=true;
				}
		        while(!closed) {
		        	if(Worker.this.closed||ref.get()==null) {
		        		closed=true;
		        		continue;
		        	} else try {
						AudioChunk chunk=decoderQueue.poll(100, TimeUnit.MILLISECONDS);
						if(chunk!=null) {
							chunks.incrementAndGet();
							chunk.len=chunk.offset=0;
						} else {
							// Blocked
							continue;
						}
						int n=chunk.buffer.length;
						for(int i=0;i<n;i++) {
							int a=audioStream.next();
							if(a==Integer.MAX_VALUE) {
								if(i>0) {
									offer(chunk,true);
									closed=true;
									continue;
								}
							} else {
								chunk.buffer[chunk.len++]=a;
								if(Math.abs(a)>32768) {
									System.out.println("Illegal");
								}
							}
						}
						samplesDelivered+=n/format.getChannels();
						if(samplesDelivered>threshold) {
							StreamEntry track=get();
							if(track!=null) {
								track.listener.trackIsFinishing(track,samplesDelivered,threshold);
							}
							threshold=Integer.MAX_VALUE;
						}
						offer(chunk,false);
						chunks.incrementAndGet();
		            } catch (IOException ex) {
		            	StreamEntry track=ref.get();
		            	if(track!=null) {
		            		track.listener.playbackError(track, ex);
		            	}
	                    return chunks.get();
		            }
		        }
	        } catch(InterruptedException e) {
	        }
	        LOGGER.debug("Samples delivered: {} of estimated {}.",samplesDelivered,totalSamples);
	        return chunks.get();
	    }
	    
	    @Override
	    public synchronized void run() {
	    	LOGGER.info("Load {} in {} with id {}",spec.getId(),entry,entry.playbackId);
	        for(int i=0;i<2;i++) try {
	            entry.load();
	            break;
	        } catch (IOException ex) {
	        	if(i==1) {
		            entry.listener.loadingFailed(entry, ex);
		            return;
	        	}
	        }
	        int chunks=0;
	   		AudioStream decoder=entry.audioStream;
            entry.listener.loadingFinished(entry, decoder.getCurrentInfo());
	   		String playbackId=entry.playbackId;	        
	   		try {
	        	if(!entry.loadingSemaphore.tryAcquire(1000L, TimeUnit.MILLISECONDS)) {
	        		LOGGER.warn("Unable to aquire loading semaphore!");
	        	} else {
		        	chunks=runInternal();
	        	}
	        } catch(InterruptedException e) {
        		LOGGER.warn("Playback interrupted");
	        }
	        LOGGER.info("{} terminated with {} chunks written.", playbackId,chunks);
	        StreamEntry entry=ref.get();
	        if(entry!=null) {
	        	entry.audioQueue.offer(new AudioChunk(true));
	        	entry.listener.playbackEnded(entry,0);
	        	entry.state=4;
	        	entry.loadingSemaphore.release();
	        }
	        try {
	        	decoder.close();
	        } catch(Throwable t) {
	        }
	    }
	}
}
