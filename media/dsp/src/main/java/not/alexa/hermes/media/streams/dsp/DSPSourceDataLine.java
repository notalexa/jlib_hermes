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
package not.alexa.hermes.media.streams.dsp;

import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import not.alexa.hermes.media.AudioSink;
import not.alexa.hermes.media.streams.DelegatingSourceDataLine;
import not.alexa.hermes.media.streams.dsp.DSP.Profile;

/**
 * Source data line wrapper managing a profile. This data line honors the {@link AudioSink#MIXINS} type to switch between channels if mixins are currently in the audio stream. 
 */
class DSPSourceDataLine extends DelegatingSourceDataLine {
	private static Logger LOGGER=LoggerFactory.getLogger(DSPSourceDataLine.class);
	private static byte[] CHANNEL_ZERO=new byte[] { 0,0,0,0};
	private static byte[] CHANNEL_ONE=new byte[] { 0,0,0,1};
	
	private DSPProfileDecorator profiler;
	private Profile profile;
	private int muteAddr=-1;
	private int channelAddr=-1;
	private int volumeAddr=-1;
	private int lockAddr=-1;
	private int currentChannel=0;
	private boolean muted;
	private boolean operational;
	private BooleanControl mixinsControl=new BooleanControl(AudioSink.MIXINS,false) {
		@Override
		public void setValue(boolean value) {
			if(value!=getValue()) {
				super.setValue(value);
				updateChannel(value);
			}
		}		
	};
	
	public DSPSourceDataLine(DSPProfileDecorator profiler,SourceDataLine delegate,Profile profile) {
		super(delegate);
		this.profiler=profiler;
		this.profile=profile;
	}
	
	public void dispose() {
		if(lockAddr>=0) {
			lockAddr=-1;
			close();
		}
	}
	
	@Override
	public void close() {
		super.close();
		if(lockAddr<0) try {
			operational=false;
			muteAddr=volumeAddr=channelAddr=lockAddr=-1;
			profiler.close(this);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	private void initProfile() {
		if(profile!=null) try {
			muteAddr=profile.getAddr("mute","mute");
			volumeAddr=profile.getAddr("gain","volume");
			lockAddr=profile.getAddr("register","lock");
			channelAddr=profile.getAddr("data","channel");
			System.out.println("Profile has: mute="+muteAddr+", volume="+volumeAddr+", lock="+lockAddr+", channel="+channelAddr);
			operational=profiler.apply(profile);
			if(lockAddr>=0) {
				new LockThread(this).start();
			}
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	

	public void updateChannel(boolean mixin) {
		if(channelAddr>=0&&operational) try {
			if(mixin) {
				if(currentChannel==0) {
					currentChannel=1;
					profiler.getDSP().poke(channelAddr, CHANNEL_ONE);
				}
			} else {
				if(currentChannel!=0) {
					currentChannel=0;
					profiler.getDSP().poke(channelAddr, CHANNEL_ZERO);
				}
			}
		} catch(Throwable t) {
		}
	}
	
	

	@Override
	public boolean isControlSupported(Type type) {
		return type==AudioSink.MIXINS||super.isControlSupported(type);
	}

	@Override
	public Control getControl(Type type) {
		if(type==AudioSink.MIXINS) {
			return mixinsControl;
		} else {
			Control control=super.isControlSupported(type)?super.getControl(type):null;
			if(type== BooleanControl.Type.MUTE&&muteAddr>=0) {
				BooleanControl booleanControl=(BooleanControl)control;
				return new BooleanControl(BooleanControl.Type.MUTE,muted) {
					@Override
					public void setValue(boolean value) {
						if(muted!=value) {
							muted=value;
							super.setValue(muted);
							if(booleanControl!=null) {
								booleanControl.setValue(muted);
							}
							if(operational) try {
								profiler.getDSP().poke(muteAddr, new byte[] { (byte)(muted?0:1),0,0,0 });
							} catch(Throwable t) {
								LOGGER.warn("Poke failed while (un)muting.",t);
							}
						}
					}
				};
			}
			if(control!=null&&control instanceof FloatControl) {
				FloatControl floatControl=(FloatControl)control;
				if(type==FloatControl.Type.MASTER_GAIN&&volumeAddr>=0) {
					return new FloatControl(FloatControl.Type.MASTER_GAIN, floatControl.getMinimum(), floatControl.getMaximum(), floatControl.getPrecision(), floatControl.getUpdatePeriod(), floatControl.getValue(), floatControl.getUnits(),floatControl.getMinLabel(),floatControl.getMidLabel(),floatControl.getMaxLabel()) {
						@Override
						public void setValue(float newValue) {
							int linearized=(int)(0x1000000*Math.pow(10,newValue/20f));
							if(operational) try {
								byte[] data=new byte[] { (byte)(0xff&(linearized>>24)) , (byte)(0xff&(linearized>>16)) , (byte)(0xff&(linearized>>8)) , (byte)(0xff&(linearized>>0)) };
								profiler.getDSP().poke(volumeAddr, data);
							} catch(Throwable t) {
								LOGGER.warn("Poke failed while setting volume.",t);
							}
							super.setValue(newValue);
						}
	
						@Override
						public void shift(float from, float to, int microseconds) {
							LOGGER.warn("Shift of volume control not implemented...");
							super.shift(from, to, microseconds);
						}
						
					};
				}
			}
			return control==null?super.getControl(type):control;
		}
	}

	@Override
	public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
		initProfile();
		super.open(format, bufferSize);
	}
	@Override
	public void open(AudioFormat format) throws LineUnavailableException {
		initProfile();
		super.open(format);
	}
	
	private static class LockThread extends Thread {
		DSP dsp;
		WeakReference<DSPSourceDataLine> line;
		private LockThread(DSPSourceDataLine line) {
			super("dsp-lock-thread");
			this.line=new WeakReference<DSPSourceDataLine>(line);
			this.dsp=line.profiler.getDSP();
			setDaemon(true);
		}
		
		public void run() {
			try {
				Thread.sleep(5000);
				boolean locked=true;
				long t=0;
				while(true) {
					DSPSourceDataLine l=line.get();
					if(l!=null&&dsp.isConnected()&&l.lockAddr>=0) try {
						int data=dsp.peekReg(l.lockAddr);
						if(data==0) {
							t=0;
							if(locked) {
								l.fireEvent(DSPProfileDecorator.UNLOCKED, -1);
								locked=false;
							}
						} else {
							if(!locked) {
								if(t>0) {
									if(System.currentTimeMillis()-t>15000) {
										l.fireEvent(DSPProfileDecorator.LOCKED, -1);
										locked=true;
										t=0;
									}
								} else {
									t=System.currentTimeMillis();
								}
							}
						}
						Thread.sleep(5000);
					} catch(InterruptedException|IOException e) {
						LOGGER.warn("Peek register failed.",t);
					} else {
						LOGGER.debug("Terminate lock thread: line is {}, connected={}",l,dsp.isConnected());
						break;
					}
				}
			} catch(Throwable t) {
			}
			DSPSourceDataLine l=line.get();
			if(l!=null) {
				l.lockAddr=-1;
			} else try {
				dsp.disconnect();
			} catch(Throwable t) {
			}
		}
	}	
}
