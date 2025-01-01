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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import not.alexa.hermes.media.streams.SourceDataLineDecorator;

/**
 * Audio sink consuming data from a {@link MasterStream}.
 */
public class AudioSink implements Runnable, AutoCloseable {
	public static final BooleanControl.Type MIXINS=new BooleanControl.Type("MIXINS") {};
	
	private static final Logger LOGGER=LoggerFactory.getLogger(AudioSink.class);
	private static final int RELEASE_DELAY=5;
	private static final int BUFFER_SIZE=0x800;	
    protected final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
    		new ThreadFactory() {
				
				@Override
				public Thread newThread(Runnable r) {
					Thread t=new Thread(r,"audio-sink-scheduler");
					t.setDaemon(true);
					return t;
				}
			}
    		);
    private final Object pauseLock = new Object();
    private int pauseCount=0;
    private MasterStream stream;
    private SourceDataLine output;
    private Thread thread;
    private final Listener listener;
    private volatile boolean closed = true;
    protected volatile boolean paused = true;
    private float volume;
    private boolean mixerLogged;
    private SourceDataLineDecorator lineDecorator;

    /**
     * Creates a new sink with the given listener and sets the initial volume.
     */
    public AudioSink(float initialVolume,SourceDataLineDecorator lineDecorator,Listener listener) {
        this.listener = listener==null?new Listener() {
			@Override
			public void sinkError(Throwable t) {
				AudioSink.this.sinkError(t);
			}
        }:listener;
        this.lineDecorator=lineDecorator;
        setVolume(initialVolume);
    }
    
	AudioControls attach(MasterStream stream) {
		if(this.stream!=null) {
			this.stream.detach();
		}
		if(lineDecorator!=null) {
			lineDecorator.init(this,stream);
		}
		this.stream=stream.attach(this);
		return this.stream;
	}


    /**
     * Start playing
     */
    public void resume() {
    	if(closed||thread==null||!thread.isAlive()) {
    		closed=false;
    		(thread = new Thread(this, "player-audio-sink")).start();
    	}
        paused = false;
        BooleanControl muteControl=output==null||!output.isControlSupported(BooleanControl.Type.MUTE)?null:(BooleanControl)output.getControl(BooleanControl.Type.MUTE);
        if(muteControl!=null) {
        	muteControl.setValue(false);
        }
        pauseCount++;
        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }
    }
    
    /**
     * Flush output
     */
    public void flush() {
    	if(output!=null) {
    		output.flush();
    	}
    }

    /**
     * Pause playing and release the playing thread after some time
     */
    public void pause() {
        paused = true;
        BooleanControl muteControl=output==null||!output.isControlSupported(BooleanControl.Type.MUTE)?null:(BooleanControl)output.getControl(BooleanControl.Type.MUTE);
        if(muteControl!=null) {
        	muteControl.setValue(true);
        }
        int c=++pauseCount;
        scheduler.schedule(() -> {
        	if(paused&&c==pauseCount) {
        		close();
        	}
        }, RELEASE_DELAY, TimeUnit.SECONDS);
    }

    /**
     * Sets the volume
     *
     * @param volume The volume between 0&leq;volume&leq;1
     */
    public boolean setVolume(float volume) {
    	volume=Math.min(1f,Math.max(0f, volume));
    	this.volume=volume;
        if(output!=null) {
            FloatControl ctrl=null;
            try {
            	ctrl=(FloatControl)output.getControl(FloatControl.Type.MASTER_GAIN);
                if (ctrl!=null) {
                    ctrl.setValue((float)(Math.log10(volume) * 20f));
                    return true;
                }
            } catch(Throwable t) {
            }
            if(ctrl==null) try {
            	ctrl=(FloatControl)output.getControl(FloatControl.Type.VOLUME);
            	if(ctrl!=null) {
            		ctrl.setValue(ctrl.getMaximum()*volume);
            		return true;
            	}
            } catch(Throwable t) {
            }
            return false;
        } else {
            return true;
        }
    }
    
	public float getVolume() {
		return volume;
	}


    @Override
    public void close() {
        closed = true;
        thread.interrupt();
    }

    @Override
    public void run() {
    	try {
    		closed=false;
	        byte[] buffer = new byte[BUFFER_SIZE * 2];
	        long time=0;
	        int bytes=0;
	        CheckPoint checkPoint=new CheckPoint();
	        AudioFormat  currentFormat=null;
	        BooleanControl mixinsControl=null;
	        while (!closed) {
	            if (paused) {
	            	if(output!=null) {
		            	output.flush();
		            	output.drain();
	            	}
	                synchronized (pauseLock) {
	                    try {
	                        pauseLock.wait();
	                    } catch (InterruptedException ex) {
	                        break;
	                    }
	                }
	            } else {
	                try {
	                    if(currentFormat==null) {
	                    	currentFormat=stream.getFormat();
	                        if(start(stream.getFormat())) {
	                        	if(output.isControlSupported(MIXINS)) {
	                        		mixinsControl=(BooleanControl)output.getControl(MIXINS);
	                        	}
		                        time=System.currentTimeMillis();
		                        checkPoint.update(currentFormat);
	                        } else if(output==null) {
	                        	pause();
	                        	continue;
	                        }
	                    }
	                    if(mixinsControl!=null) {
	                    	mixinsControl.setValue(stream.hasSecondaries());
	                    }
	                    int count = stream.read(currentFormat,buffer,0,buffer.length);
	                    if(count>=0) {
	                    	output.write(buffer, 0, count);
	                    	bytes+=count;
	                    	if(!checkPoint.update(count)) {
	                    		LOGGER.error("Terminate current audiosink due to check point failure");
	                    		return;
	                    	}
	                    } else {
                    		LOGGER.info("Terminate current play with count {}",count);
	                    	output.drain();
	                    	if(count==-2) {
	                    		currentFormat=null;
	                    	}  else {
		                    	LOGGER.info("Played {}ms ({} bytes).",(System.currentTimeMillis()-time),bytes);
		                    	pause();
	                    	}
	                    }
	                } catch (Throwable t) {
	                    if (closed) {
	                    	break;
	                    }
	                    pause();
	                    listener.sinkError(t);
	                }
	            }
	        }
            output.drain();
            output.close();
        } catch (Throwable t) {
	        LOGGER.error("Audio Sink stopped abnormally.",t);
        } finally {
        	closed=true;
        }
    }
    
    protected void sinkError(Throwable t) {
    }

    public interface Listener {
        void sinkError(Throwable t);
    }
    
    private void closeLine(SourceDataLine line) {
		line.stop();
		line.flush();
		line.close();
		if(lineDecorator!=null) {
			lineDecorator.dispose(line);
		}
    }
    
    protected boolean start(AudioFormat format) {
       try {
            if(acquireLine(format)) {
            	output.start();
            }
            return true;
        } catch (LineUnavailableException e) {
        	return false;
        }
    }
    
    private boolean acquireLine(AudioFormat format) throws LineUnavailableException {
        if (output == null || !output.getFormat().matches(format)||!output.isOpen()) {
        	if(output!=null) try {
        		closeLine(output);
        	} finally {
        		output=null;
        	}
        	DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, AudioSystem.NOT_SPECIFIED);
    		if(!mixerLogged) for(Info o:AudioSystem.getMixerInfo()) {
        		mixerLogged=true;
    			LOGGER.info("Mixer: {} from {} (name={}, version={})",o.getDescription(),o.getVendor(),o.getName(),o.getVersion());
    		}
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info)) {
                	SourceDataLine out=(SourceDataLine)mixer.getLine(info);
                	LOGGER.info("Use mixer {} for format {}.",mixer.getMixerInfo(),format);
                	if(lineDecorator!=null) {
                		out=lineDecorator.decorate(out);
                	}
                	output=out;
                	SourceDataLine lineRef=out;
                	output.addLineListener(new LineListener() {
						@Override
						public void update(LineEvent event) {
							if(output==lineRef) {
								stream.update(event);
							}
						}
					});
                	output.open(format);
                	setVolume(volume);
                	break;
                }
            }
            return output!=null;
        }
        return false;
    }

    /**
     * Simple checkpoint to avoid huge amounts of downloaded data if the sink is broken. The class checks if the
     * number of bytes written in 2 min is bounded using the audio format.
     */
    private class CheckPoint {
    	private int bytes=0;
    	private int bytesPer2Min=Integer.MAX_VALUE;
    	private long timestamp=System.currentTimeMillis();
    	
    	public void update(AudioFormat format) {
    		bytes=0;
    		bytesPer2Min=120*(int)(format.getSampleSizeInBits()*format.getSampleRate()*format.getChannels()/8);
    		timestamp=System.currentTimeMillis();
    	}
    	
    	public boolean update(int bytecount) {
    		bytes+=bytecount;
    		if(bytes>bytesPer2Min) {
    			long time=System.currentTimeMillis();
    			if((time-timestamp)<500) {
    				return false;
    			}
    			bytes=0;
    			timestamp=time;
    		}
    		return true;
    	}
    }
}
