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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.MP3Decoder;
import javazoom.jl.decoder.OutputBuffer;
import not.alexa.hermes.media.AudioStream;
import not.alexa.hermes.media.io.SeekableInputStream;

/**
 * MP3 audio stream based on an underlying input stream.
 * 
 * @author notalexa
 */
public class MP3AudioStream implements AudioStream {
	private final SeekableInputStream seek;
    private final MP3InputStream in;
    private AudioInfo info;
    private int offset;
    protected final long size;

    public MP3AudioStream(InputStream audioIn, String title,float normalizationFactor, long size) throws IOException {
    	this(audioIn,new AudioInfo(null,title,-1),normalizationFactor,size);
    }
    
    public MP3AudioStream(InputStream audioIn, AudioInfo info,float normalizationFactor, long size) throws IOException {
    	audioIn=new BufferedInputStream(audioIn,2048);
    	seek=audioIn instanceof SeekableInputStream?(SeekableInputStream)audioIn:null;
    	this.size=size;
    	this.info=info;
    	offset=new ID3Handler().parse(audioIn);
    	try {
	        this.in = new MP3InputStream(audioIn, normalizationFactor,size-offset);
	        if(in.duration>0) {
	        	info=info.forDuration(in.duration/1000f);
	        }
    	} catch(BitstreamException t) {
    		throw new IOException(t);
    	}
    }
        
    @Override
	public boolean seekTo(float time) {
    	if(in.duration>0) {
    		if(seekBytes(offset+(long)((size-offset)*time*1000/in.duration))) {
    			in.position=1000*time;
        		in.clear();
    			return true;
    		}
    	} else if(time==0f&&seekBytes(offset)) {
    		in.position=0;
    		in.clear();
    		return true;
    	}
    	return false;
	}
    
    @Override
	public int update(boolean primary,int totalVolume,int volume,byte[] buffer,int offset,int length) throws IOException {
		if(primary) {
			return in.read(buffer, offset, length);
		} else {
			return AudioStream.super.update(primary,totalVolume,volume,buffer,offset,length);
		}
	}

    protected boolean seekBytes(long bytes) {
    	return seek==null?false:seek.seekTo(bytes);
    }

	@Override
	public AudioInfo getCurrentInfo() {
		return info.forPosition(in.position/1000f);
	}

    public void close() {
    	try {
    		in.close();
    	} catch(Throwable t) {
    	}
    }
    
    protected boolean handleEOS(ByteBuffer buffer) {
    	return true;
    }
    
    public AudioFormat getFormat() {
    	return in.audioFormat;
    }
    
    private class ID3Handler extends ID3 {

		@Override
		protected void handleTag(String tag, byte[] data, int size) throws IOException {
	        if("TIT2".equals(tag)) {
	        	info=new AudioInfo(info.getArtist(),id3String(data,size),info.getDuration());
	        } else if("TPE1".equals(tag)) {
	        	info=new AudioInfo(id3String(data,size),info.getTitle(),info.getDuration());
	        }
		}
    }

    private class MP3InputStream extends InputStream {
        private final static int MAX_READ_SIZE = 96 * 1024;
        private final static int MP3_BUFFER_SIZE = 128 * 1024;
        private final InputStream in;
        private final Bitstream bitstream;
        private final ByteBuffer buffer;
        private final MP3Decoder decoder;
        private final AudioFormat audioFormat;
        private final OutputBuffer outputBuffer;
        private boolean eos;
        private boolean fullyRead;
        private float position;
        private float frameTime;
        private float duration;

        /**
         * Initializes the stream, reads the first header, retrieves important stream information and unreads the header
         *
         * @param in the MP3 stream
         * @param normalisationPregain the normalization pregain applied to the raw PCM
         * @param the size of the file or <code>-1</code> if not known
         */
        MP3InputStream(InputStream in, float normalisationPregain,long size) throws BitstreamException {
            this.in = in;
            eos = false;
            bitstream = new Bitstream(in);
            buffer = ByteBuffer.allocateDirect(MP3_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            buffer.limit(0);
            decoder = new MP3Decoder();
            Header header = bitstream.readFrame();
            frameTime=header.ms_per_frame();
            duration=size>0?header.total_ms((int)size):-1;
            bitstream.unreadFrame();
            audioFormat=new AudioFormat(header.getSampleRate(), 16, header.mode() == Header.SINGLE_CHANNEL ? 1 : 2, true, false);
            outputBuffer = new OutputBuffer(audioFormat.getChannels(), false);
            decoder.setOutputBuffer(outputBuffer);
            outputBuffer.setReplayGainScale(normalisationPregain);
        }
        
        private void clear() {
        	buffer.clear();
        	buffer.flip();
        }

        private boolean fillBuffer() throws IOException {
            buffer.clear();
            if(!fullyRead) try {
	            int total = 0;
	            while(total < MAX_READ_SIZE) {
	                Header header;
                    header = bitstream.readFrame();
	                if(header==null) {
	                    fullyRead=true;
                    	eos=total==0&&handleEOS(buffer);
	                    break;
	                }
                    decoder.decodeFrame(header, bitstream);
	                bitstream.closeFrame();
	                position+=frameTime;
	                int bytesRead = outputBuffer.reset();
	                buffer.put(outputBuffer.getBuffer(), 0, bytesRead);
	                total += bytesRead;
	            }
            } catch(BitstreamException|DecoderException e) {
            	throw new IOException(e);
            } else {
            	eos=handleEOS(buffer);
            }
            buffer.flip();
            return !eos;
        }

        @Override
        public void close() throws IOException {
        	eos=fullyRead=true;
            try {
                bitstream.close();
            } catch (BitstreamException ex) {
                throw new IOException(ex);
            }
            in.close();
        }

        @Override
        public int read() throws IOException {
        	if(eos) {
        		return -1;
        	}
            if(buffer.remaining()==0) {
                if(!fillBuffer()) {
                	return -1;
                }
            }
            return buffer.get()&0xff;
        }
        
        @Override
		public int read(byte[] b, int off, int len) throws IOException {
        	if(eos) {
        		return -1;
        	}
            if (buffer.remaining()==0) {
                if(!fillBuffer()) {
                	return -1;
                }
            }
            len=Math.min(len,buffer.remaining());
        	buffer.get(b, off, len);
        	return len;
		}

		int readShort() throws IOException {
        	if(eos) {
        		return Integer.MAX_VALUE;
        	}
            if (buffer.remaining()<=1) {
                if(!fillBuffer()) {
                	return Integer.MAX_VALUE;
                }
            }
        	return buffer.getShort();
        }
    }

	@Override
	public int next() throws IOException {
		return in.readShort();
	}

	@Override
	public boolean isStream() {
		return size<=0;
	}
}
