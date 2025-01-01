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

import javax.sound.sampled.SourceDataLine;

import not.alexa.hermes.media.AudioSink;
import not.alexa.hermes.media.MasterStream;
import not.alexa.hermes.media.players.JukeBox;
import not.alexa.hermes.media.players.Tuner;

/**
 * Interface to handle decoration of source data lines. The typical use case is to decorate a line based on a "profile" provided in the audio format.
 * For example, the decorator may provide a specific equalizer setup.
 * <br>All basic Players are able
 * to provide a static profile. Therefore a {@link JukeBox} for Pop may introduce another profile than a {@link JukeBox} for classic.
 * <br>In addition, the {@link Tuner} supports profiles by adding a query parameter {@code profile} in the provided URL.
 * 
 * @author notalexa
 */
public interface SourceDataLineDecorator {
	/**
	 * Init this decorator for a given sink and master stream
	 * @param sink the sink this decorator is attached to
	 * @param stream the current master stream
	 */
	public void init(AudioSink sink,MasterStream stream);
	/**
	 * Decorate the given line. 
	 * 
	 * @param line to decorate
	 * @return decorated line
	 */
	public SourceDataLine decorate(SourceDataLine line);
	/**
	 * Dispose the line. A line is kept "as long as possible" (that is until another line should start input). Therefore the line can create line events on
	 * additional resources to inform associated listeners (the player for example) that something happened. A use case is a line observing input from an additional
	 * audio source informing the system that audio input started (or stopped).
	 * 
	 * @param line the line to dispose
	 */
	public void dispose(SourceDataLine line);
	
	/**
	 * Close this decorator freeing all resources needed.
	 */
	public void close();
}
