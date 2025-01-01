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

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.streams.MP3AudioStream;
import not.alexa.hermes.media.streams.VorbisStream;

/**
 * One of the basic interfaces of this player representing an audio stream.
 * 
 * @author notalexa
 */
public interface AudioStream extends AutoCloseable, LineListener {
	
	/**
	 * Set the audio controls for this stream. Typically, a stream is interested in audio controls if it's a player but others may be interested too.
	 * 
	 * @param controls the controls for this audio stream
	 */
	public default void setAudioControls(AudioControls controls) {
	}
	
	/**
	 * Called, if some parameters (like volume) changed.
	 */
	public default void onAudioStateChanged() {
	}
	
	
	/**
	 * Do nothing
	 */
	@Override
	public default void update(LineEvent event) {
	}
	/**
	 * 
	 * @return the audio format of this stream. <i>This format may change over time!</i>. In this case, the stream <b>must</b> indicate a change using the
	 * {@link AudioControls#onAudioFormatChanged(AudioFormat)} method.
	 * 
	 * @see AudioControls#onAudioFormatChanged(AudioFormat)
	 */
	public AudioFormat getFormat();
	
	/**
	 * Seek to a specific (absolut) position in the audio stream
	 * @param time the time (in seconds) to seek to
	 * @return {@code true} if seeking was successful, {@code false} otherwise
	 */
	public default boolean seekTo(float time) {
		return false;
	}
	
	/**
	 * Resample this stream to a different audio format. This method is basically intended to sample up secondary streams to the main audio format.
	 * <br>The default implementation restricts itself to 16 bit signed pcm data (little endian) in mono or stereo format and uses linear interpolation.
	 *  
	 * @param format the new format to use
	 * @return an audio stream representing {@code this} stream in the given format or {@code null}, if conversion is not supported.
	 */
	public default AudioStream resample(AudioFormat format) {
		if(format.equals(getFormat())) {
			return this;
		} else {
			if(!format.getEncoding().equals(Encoding.PCM_SIGNED)
					||format.isBigEndian()
					||format.getChannels()>2
					||format.getSampleSizeInBits()!=16) {
				return null;
			}
			AudioStream baseStream=this;
			try {
				return new AudioStream() {
					float ratio=baseStream.getFormat().getSampleRate()/format.getSampleRate();
					int[][] accu=new int[2][format.getChannels()];
					float offset=1f;
					int channelOffset=0;
					boolean done;
					{
						fill();
					}
					
					@Override
					public void update(LineEvent event) {
						AudioStream.this.update(event);
					}

					private void fill() throws IOException {
						int c1=accu[0].length;
						int c2=baseStream.getFormat().getChannels();
						for(int i=0;i<c1;i++) {
							accu[0][i]=accu[1][i];
							if(c1==c2) {
								accu[1][i]=baseStream.next();
							} else if(c2==1) {
								// mono -> stereo
								accu[1][i]=i==0?baseStream.next():accu[1][0];
							} else {
								// stereo -> mono
								accu[1][i]=(baseStream.next()+baseStream.next())>>1;
							}
						}
						done=accu[1][accu[1].length-1]>Short.MAX_VALUE;
					}
					
					@Override
					public int next() throws IOException {
						if(done) {
							return Integer.MAX_VALUE;
						}
						if(channelOffset==accu[0].length-1) {
							while(offset>=1) {
								fill();
								if(done) {
									return Integer.MAX_VALUE;
								}
								offset-=1f;
							}
							offset+=ratio;
							channelOffset=0;
						} else {
							channelOffset++;
						}
						int o=(int)((1-offset)*accu[0][channelOffset]+offset*accu[1][channelOffset]);
						//System.out.println("O="+o+" (offset="+channelOffset+", x="+offset+")");
						return o;
					}
					
					@Override
					public boolean isStream() {
						return baseStream.isStream();
					}
					
					@Override
					public AudioFormat getFormat() {
						return format;
					}

					@Override
					public void close() {
						baseStream.close();
					}
				};
			} catch(Throwable t) {
				return null;
			}
		}
	}
	
	/**
	 * Get the next sample of this stream according to the current format. Special values indicate exceptional situations:
	 * <ul>
	 * <li>A value of {@code Integer.MAX_VALUE} indicates the <i>end of the stream</i>.
	 * <li>A value of {@code Integer.MAX_VALUE-1} indicates a <i>blocking condition</i>. Bulk operations <b>must</b> stop processing data.
	 * </ul>
	 * 
	 * @return the next sample of this stream or one of the exceptional values
	 * @throws IOException if an error occurs
	 */
	public int next() throws IOException;
	
	/**
	 * Update a buffer with data from this audio stream. The buffer <b>is not empty if {@code primary} is {@code false}</b> but filled with the data of another stream (typically the 
	 * <i>primary</i> stream). The method should return the number of updated samples.
	 * <br>If {@code primary} flag is {@code true}, the stream <b>must</b> fill the buffer ignoring any data in the buffer.
	 * If {@code totalVolume==volume}, the stream <b>must</b> scale the audio stream but <b>must</b> ignore the data in the buffer (this stream overrides any stream inside the buffer). 
	 * 
	 * @param primary if {@code true}, fill the buffer, otherwise update the buffer. If set, no corrections are supposed to be done to the stream.
	 * @param totalVolume the total volume (a value 0&leq;v&leq;256)
	 * @param volume the volume of this stream (a value 0&leq;v&leq;256)
	 * @param buffer the buffer to fill
	 * @param offset the offset inside the buffer
	 * @param length the length (in bytes) inside the buffer to update
	 * @return the number of updated bytes
	 * @throws IOException if an error occurs
	 */
	public default int update(boolean primary,int totalVolume,int volume,byte[] buffer,int offset,int length) throws IOException {
		int savedOffset=offset;
		if(primary||totalVolume==volume) {
			while(length>0) {
				int n=next();
				if(n>0x10000) {
					return n==Integer.MAX_VALUE&&offset==savedOffset?-1:offset-savedOffset;
				}
				n=(n*totalVolume)>>8;
				buffer[offset]=(byte)n;
				buffer[offset+1]=(byte)(n>>8);
				offset+=2;
				length-=2;
			}
		} else if(length>0) {
			int secondaryGain=Math.min(Math.max(0,volume),totalVolume);
			int primaryGain=totalVolume-secondaryGain;
			while(length>0) {
				int n=next();
				if(n>0x10000) {
					return n==Integer.MAX_VALUE&&offset==savedOffset?-1:offset-savedOffset;
				}
				int o=(buffer[offset]&0xff)+(buffer[offset+1]<<8);
				o=(primaryGain*o+secondaryGain*n)>>8;
				buffer[offset]=(byte)o;
				buffer[offset+1]=(byte)(o>>8);
				offset+=2;
				length-=2;
			}
		}
		return offset-savedOffset;
	}

	/**
	 * Create a master stream for this audio stream making this audio stream the <b>primary</b> stream.
	 * 
	 * @param sink the sink to use for output
	 * @return the master stream for this audio stream
	 */
	public default AudioControls createControls(AudioSink sink) {
		return sink.attach(new MasterStream(this));
	}

	@Override
	public default void close() {
	}
	
	/**
	 * 
	 * @return {@code true} if this stream is a stream (that is has an infinite duration (at least approximately ;-)))
	 */
	public boolean isStream();
	
	/**
	 * 
	 * @return info about this stream. The method may return {@code null} if no info is available.
	 */
	public default AudioInfo getCurrentInfo() {
		return null;
	}
	
	/**
	 * Data container for information about a stream.
	 * 
	 * @author notalexa
	 */
	public class AudioInfo implements Cloneable {
		@JsonProperty private String artist;
		@JsonProperty private String title;
		@JsonProperty private float position;
		@JsonProperty private float duration;
		@JsonProperty(defaultValue = "0") float gain;
		@JsonProperty(defaultValue = "1") float peak;

		protected AudioInfo() {
		}

		public AudioInfo(String artist,String title,float duration) {
			this(artist,title,duration,0,1);
		}

		public AudioInfo(String artist,String title,float duration,float gain,float peak) {
			this.artist=artist;
			this.title=title;
			this.duration=duration;
			this.gain=gain;
			this.peak=peak;
		}
		
		/**
		 * 
		 * @param position the current position (in seconds) inside the stream
		 * @return infos with the position field updated
		 */
		public AudioInfo forPosition(float position) {
			try {
				AudioInfo info=(AudioInfo)super.clone();
				info.position=position;
				return info;
			} catch(Throwable t) {
				return this;
			}
		}

		public AudioInfo forDuration(float duration) {
			try {
				AudioInfo info=(AudioInfo)super.clone();
				info.duration=duration;
				return info;
			} catch(Throwable t) {
				return this;
			}
		}

		public AudioInfo forNormalizationData(float gain,float peak) {
			try {
				AudioInfo info=(AudioInfo)super.clone();
				info.gain=gain;
				info.peak=peak;
				return info;
			} catch(Throwable t) {
				return this;
			}
		}

		/**
		 * 
		 * @return the artist
		 */
		public String getArtist() {
			return artist;
		}
		
		/**
		 * 
		 * @return the title
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * 
		 * @return the duration of this stream. A negative value indicates that the stream is most likely a stream and the end
		 * is not known.
		 */
		public float getDuration() {
			return duration;
		}
		
		public float getGain() {
			return gain;
		}
		
		public float getPeak() {
			return peak;
		}
		
		/**
		 * 
		 * @return the current position in the stream (seconds since starting)
		 */
		public float getPosition() {
			return position;
		}
		
		public String toString() {
			return title+"[artist="+artist+", duration="+((int)duration)+", current="+((int)position)+"]";
		}
		
	}

	/**
	 * Simple interface for data interested in changes of an {@link AudioStream}
	 * 
	 */
	public interface AudioStreamListener {
		
		/**
		 * Called whenever the audio format changes
		 * @param format the new audio format
		 */
		public default void onAudioFormatChanged(AudioFormat format) {
		}
		
		/**
		 * Called whenever the <i>state</i> of this stream changes.
		 */
		public default void onStateChanged() {
		}
	}
	
	public enum MediaType {
		Wav,MP3,Vorbis,AAC,PCM,Unknown;
		
		public AudioStream create(InputStream in,long size,AudioInfo info,float normalizationFactor) throws IOException {
			switch(this) {
				case MP3: return new MP3AudioStream(in, info, normalizationFactor, size);
				case Vorbis: return new VorbisStream(in, info,normalizationFactor,size);
				default:
					throw new IOException("Unsupported Media Type "+this);
			}
		}
	}
}
