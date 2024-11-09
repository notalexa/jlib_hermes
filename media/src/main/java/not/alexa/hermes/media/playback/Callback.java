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
import java.io.OutputStream;

import not.alexa.hermes.media.AudioStream.AudioInfo;

/**
 * Callback for loading audio streams.
 * 
 * @author notalexa
 */
public interface Callback {
	
	/**
	 * Callback invoked after the download is complete.
	 * 
	 * @param event the download event
	 */
	public default void downloadComplete(DownloadEvent event) {
	}
	
	/**
	 * Called whenever the download failed.
	 * 
	 * @param cause the root cause of failure
	 */
	public default void downloadFailed(Throwable cause) {
	}
	
	/**
	 * Parameter to {@link Callback#downloadComplete(DownloadEvent). Allows to
	 * get the (complete) audio stream as a file.
	 * 
	 * @author notalexa
	 */
	public interface DownloadEvent {
		
		/**
		 * Save the download into the given output stream.
		 * 
		 * @param info the audio info of the stream
		 * @param out the output stream for saving the stream
		 * @return the updated audio info
		 * @throws IOException if an error occurs
		 */
		public AudioInfo downloaded(AudioInfo info,OutputStream out) throws IOException;
	}
}
