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

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import not.alexa.hermes.media.AudioControls;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.streams.Silence;

/**
 * A playback stream plays a sequence of tracks continously one after the other.
 * 
 * @author notalexa
 */
public class PlaybackStream implements Closeable, StreamEntry.Listener, AudioStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlaybackStream.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "playback");
		}
	});
    private final Listener listener;
    private volatile boolean closed = false;
    private AudioControls controls;
    
    private PlaybackPlayer player;
    private StreamEntry head;
    private StreamEntry currentTrack;
    private StreamEntry fadeoutTrack;
    private Runnable onPlaybackEnded;

    public PlaybackStream(PlaybackPlayer player, Listener listener) {
    	this.player=player;
        this.listener = listener;
    }
    
    @Override
    public void setAudioControls(AudioControls controls) {
    	this.controls=controls;
    }
    
    public void setOnPlaybackEnded(Runnable onPlaybackEnded) {
    	this.onPlaybackEnded=onPlaybackEnded;
    }

    private StreamEntry add(PlaybackTrack playable) {
    	StreamEntry newEntry=new StreamEntry(player,playable,this);
    	newEntry.next=head;
    	// This is the only strong reference if not playing.
    	// Setting head to null makes all (possibly initialized) entries unavailable
    	head=newEntry;
    	return newEntry;
    }

    /**
     * Adds the next track to the internal queue.
     */
    private void addNext() {
    	PlaybackTrack track=player.nextTrack(this);
        if (track != null) {
        	add(track).start();
        }
    }

    /**
     * Gets the next track in the player list 
     */
    private synchronized void advance() {
        if (closed) {
        	return;
        }
        PlaybackTrack next = player.nextTrack(this);
        if (next == null) {
        	Runnable r=onPlaybackEnded;
        	if(r!=null) {
        		onPlaybackEnded=null;
        		executorService.execute(r);
        	}
            return;
        }
        playInternal(next);
    }

    @Override
    public void playbackEnded(StreamEntry entry,int time) {
    	if(entry==currentTrack||currentTrack==null) {
    		LOGGER.info("{} ended.",entry.spec);
    		// Because of potentially undelivered chunks we do not set currentTrack to null at this point.
	        listener.playbackFinished(entry.playbackId, time);
	        if(!entry.disableNextTrack) {
	        	advance();
	        }
    	}
    }

    @Override
    public void loadingStarted(StreamEntry entry) {
        LOGGER.trace("{} started loading.", entry);
	    listener.loadingStarted(entry.playbackId);
    }

    @Override
    public void loadingFailed(StreamEntry entry, Throwable t) {
    	if(entry==currentTrack) {
	        listener.loadingFailed(entry.playbackId,t);
    	}
    }
    
	@Override
    public void loadingFinished(StreamEntry entry, AudioInfo metadata) {
    }


    @Override
    public void playbackSeeked(StreamEntry entry, int pos) {
    	if(entry==currentTrack) {
    		listener.playbackSeeked(entry.playbackId, pos);
    	}
    }

    @Override
    public void playbackError(StreamEntry entry, Throwable t) {
    	if(entry==currentTrack) {
    		listener.playbackFailed(entry.playbackId,t);
    	}
    }

    
    @Override
    public void ready(StreamEntry entry,int pos) {
    	synchronized (entry) {
    		if(entry==currentTrack) {
    			LOGGER.warn("Track {} requested input twice.",entry.spec);
    		} else {
		    	LOGGER.info("{} ready.", entry.spec);
		    	// Remove explicit references of preloaded tracks
		    	head=null;
//		    	head=entry;
		    	if(listener.ready(entry.playbackId, entry.spec,pos)) {
		    		if(currentTrack!=null&&currentTrack!=fadeoutTrack) {
		    			currentTrack.close();
		    		}
		    		synchronized (controls.getStreamLock()) {
				    	currentTrack=entry;
				    	controls.onAudioFormatChanged(entry.getFormat());
				    	controls.onStateChanged();
					}
		    	}
    		}
    	}
    }


	@Override
	public void trackChanged(StreamEntry entry) {
		listener.playbackStarted(entry.playbackId, entry.getCurrentInfo(), entry.initialSeektime);
	}

	@Override
	public void trackIsFinishing(StreamEntry track,int delivered,int expected) {
		LOGGER.debug("Track {} is finishing.",track);
        executorService.execute(this::addNext);
	}

    @Override
	public boolean seekTo(float time) {
		return seekCurrent((int)(time*1000));
	}

	@Override
	public AudioInfo getCurrentInfo() {
		return currentTrack==null?(fadeoutTrack==null?null:fadeoutTrack.getCurrentInfo()):currentTrack.getCurrentInfo();
	}

    private synchronized String playInternal(StreamEntry playable) {
        int pos=playable.spec.startPos();
        playable.startPlaying(pos);
        LOGGER.debug("{} has been added to the stream (pos: {}).", playable, pos);
        return playable.playbackId;
    }


    private synchronized String playInternal(PlaybackTrack playable) {
    	if(currentTrack!=null) {
    		currentTrack.disableNextTrack();
    	}
        fadeoutTrack=currentTrack;
        StreamEntry next=head;
		while(true) {
			if(next==null) {
		    	LOGGER.info("Start "+playable+" with queue.start");
		        next=add(playable);
		        break;
		    } else if(next.spec.getId().equals(playable.getId())) {
		    	break;
		    } else {
		    	next=next.next;
		    }
		}
        if(fadeoutTrack!=null) {
        	fadeoutTrack=null;
        }
        return playInternal(next);
    }

    /**
     * Start playing the given track.
     *
     * @param track the content to be played
     */
    public String play(PlaybackTrack track) {
        return playInternal(track);
    }

    /**
     * Seek to the specified position on the queue head.
     *
     * @param pos the time in milliseconds
     */
    public boolean seekCurrent(int pos) {
    	if(currentTrack==null) {
    		return false;
    	}
    	if(fadeoutTrack!=null) {
    		fadeoutTrack=null;
    	}
    	if(currentTrack.seek(pos)) {
    		controls.flush();
    		return true;
    	} else {
    		return false;
    	}
    }

    public String currentPlaybackId() {
    	StreamEntry entry=currentTrack;
    	return entry==null?null:entry.playbackId;
    }
    

	@Override
	public AudioFormat getFormat() {
		return currentTrack==null?new Silence().getFormat():currentTrack.getFormat();
	}

	@Override
	public int next() throws IOException {
		int n1=0;
		if(fadeoutTrack!=null) {
			n1=fadeoutTrack.next();
			if(n1>0x10000) {
				fadeoutTrack=null;
				n1=0;
			}
		}
		int n=currentTrack==null?0:currentTrack.next();
		if(n>0x10000) {
			currentTrack=null;
			return 0;
		}
		return n;
	}

	@Override
	public boolean isStream() {
		return false;
	}


    /**
     * Close the session by clearing the queue which will close all entries.
     */
    @Override
    public void close() {
        closed = true;
        if(fadeoutTrack!=null) {
        	fadeoutTrack.close();
        } 
        if(currentTrack!=null) {
        	currentTrack.close();
        }
        System.gc();
    }
    
    protected void finalize() {
    	close();
    }

    public interface Listener {
    	
        boolean ready(String playbackId, PlaybackTrack track,int pos);

        void loadingStarted(String playbackId);

        void loadingFailed(String playbackId,Throwable cause);
        
        void loadingFinished(String playbackId);

        void playbackFailed(String playbackId, Throwable cause);

        void playbackStarted(String playbackId, AudioInfo metadata, int pos);

        void playbackFinished(String playbackId,int endedAt);
        
        void playbackSeeked(String playbackId,int pos);
    }
}
