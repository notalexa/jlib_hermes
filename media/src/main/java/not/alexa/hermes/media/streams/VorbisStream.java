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
package not.alexa.hermes.media.streams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;

import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.io.SeekableInputStream;

/**
 * Audio stream based on a vorbis encoding (packaged in an ogg container).
 * <br>
 * The stream handles a spotify extension including gain and peak from a non audio packet.
 * 
 * @author notalexa
 */
public class VorbisStream implements AudioStream {
	private static final Logger LOGGER=LoggerFactory.getLogger(VorbisStream.class);
	private OggReader reader;
	private DspState jorbisDspState = new DspState();
	private Block jorbisBlock = new Block(jorbisDspState);
	private Comment jorbisComment = new Comment();
	private Info jorbisInfo = new Info();
	private float[][][] pcmInfo;
	private int[] pcmIndex;
	
	private int[] decodingBuffer;
	private int size;
	private int offset;
	
	private int audioStream=-1;
	private AudioInfo info;
	private AudioFormat format;
	private float normalizationFactor;
	private long fileSize;
	private float gain=0;
	private float peak=1;
	private long frame;
	private SeekableInputStream input;
	
	public VorbisStream(InputStream stream,AudioInfo info,float normalizationFactor,long size) throws IOException {
		this(new OggReader(stream),info,normalizationFactor,size);
		if(stream instanceof SeekableInputStream) {
			input=(SeekableInputStream)stream;
		}
	}

	public VorbisStream(OggReader reader,AudioInfo info,float normalizationFactor,long size) throws IOException {
		this.reader=reader;
		this.info=info;
		this.normalizationFactor=32767*normalizationFactor;
		this.fileSize=size;
		if(fillBuffer(nextAudioPacket())) {
			this.info=info.forNormalizationData(gain, peak);
			format=decorate(new AudioFormat(jorbisInfo.rate,16, jorbisInfo.channels, true, false));
		} else {
			throw new IOException("Not a Vorbis stream");
		}
	}
	
	@Override
	public AudioInfo getCurrentInfo() {
		return info.forPosition(time());
	}
	
	/**
	 * Decorate the format (e.g. add a profile). This default to noop.
	 * @param format the format to decorate
	 * @return the decorated format
	 */
    protected AudioFormat decorate(AudioFormat format) {
    	return format;
    }

	protected float time() {
		return ((float)frame)/((float)jorbisInfo.rate);
	}

	@Override
	public synchronized boolean seekTo(float time) {
		long frame=(long)(time*jorbisInfo.rate);
		long refFrame=this.frame;
		if(frame<refFrame&&input!=null) {
			refFrame=this.frame=0;
			jorbisDspState = new DspState();
			jorbisBlock = new Block(jorbisDspState);
			jorbisComment = new Comment();
			jorbisInfo=new Info();
			audioStream=-1;
			input.seekTo(0);
			reader=new OggReader(input);
		}
		if(frame>refFrame) {
			LOGGER.debug("Skip forward from frame {} to {} (rate={}, time={})...",refFrame,frame,jorbisInfo.rate,time);
			long lastFrame=0;
			while(true) try {
				Packet packet=nextAudioPacket();
				if(packet==null) {
					LOGGER.debug("EOF reached (lastFrame={}).",lastFrame);
					return true;
				} else if(packet.granulepos>frame) {
					LOGGER.debug("Skip to packet with granulepos {}.",packet.granulepos);
					fillBuffer(packet);
					return true;
				} else {
					lastFrame=packet.granulepos;
				}
			} catch(Throwable t) {
				return true;
			}
		} else if(frame==this.frame) {
			return true;
		}
		return AudioStream.super.seekTo(time);
	}

	public boolean fillBuffer(Packet audioPacket) throws IOException {
		if(audioPacket==null) {
			return false;
		}
		if(jorbisBlock.synthesis(audioPacket) == 0) {
			jorbisDspState.synthesis_blockin(jorbisBlock);
		} else {
			throw new IOException();
		}
		
		int samples=jorbisDspState.synthesis_pcmout(pcmInfo, pcmIndex);
		size=samples*jorbisInfo.channels;
		if(decodingBuffer==null||decodingBuffer.length<size) {
			decodingBuffer=new int[size];
		}
		int delta=jorbisInfo.channels;
		for(int channel=0;channel<jorbisInfo.channels;channel++) {
			int sampleOffset=channel;
			for(int sample=0;sample<samples;sample++) {
				int value = (int) (pcmInfo[0][channel][pcmIndex[channel] + sample] * normalizationFactor);
				if(value>Short.MAX_VALUE) {
					value=Short.MAX_VALUE;
				} else if(value<Short.MIN_VALUE) {
					value=Short.MIN_VALUE;
				}
				decodingBuffer[sampleOffset]=value;
				sampleOffset+=delta;
			}
		}
		offset=0;
		jorbisDspState.synthesis_read(samples);
        long granulepos = audioPacket.granulepos;
        if (granulepos != -1 && audioPacket.e_o_s == 0) {
            frame = granulepos-samples;
        }
		return true;
	}

	@Override
	public int next() throws IOException {
		while(size<=offset) {
			synchronized(this) {
				if(!fillBuffer(nextAudioPacket())) {
					return Integer.MAX_VALUE;
				}
			}
		}
		int ret=decodingBuffer[offset++];
		if(ret>32767||ret<-32768) {
			System.out.println("IMPOSSIBLE!!!");
		}
		return ret;
	}
	
	protected void handleUnknownPacket(Packet packet) {
		if(packet.b_o_s!=0&&packet.e_o_s!=0&&packet.bytes==139) {
			ByteBuffer buffer=ByteBuffer.wrap(packet.packet_base,packet.packet+(144-28), 16);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			gain=buffer.getFloat();
			peak=buffer.getFloat();
		}
	}

	public synchronized Packet nextAudioPacket() throws IOException {
		while(true) {
			Packet next=reader.nextPacket();
			Page page=reader.currentPage();
			if(next==null) {
				return null;
			}
			if(audioStream<0) {
				if(jorbisInfo.synthesis_headerin(jorbisComment,next) >=0) {
					audioStream=page.serialno();
					for(int i=1;i<3;i++) {
						if(jorbisInfo.synthesis_headerin(jorbisComment, nextAudioPacket())<0) {
							throw new IOException("Unable to init");
						}
					}
					jorbisDspState.synthesis_init(jorbisInfo);
					jorbisBlock.init(jorbisDspState);
					pcmInfo = new float[1][][];
					pcmIndex = new int[jorbisInfo.channels];
					return nextAudioPacket();
				} else {
					handleUnknownPacket(next);
				}
			} else if(page.serialno()==audioStream) {
				return next;
			}
		}
	}

	@Override
	public void close() {
		try {
			reader.close();
		} catch(Throwable t) {
		}
	}
	
	@Override
	public boolean isStream() {
		return fileSize<0;
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}
}
