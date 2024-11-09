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

import java.io.InputStream;

/**
 * Extension of {@code InputStream} providing better seekable features.
 * 
 * @author notalexa
 */
public abstract class SeekableInputStream extends InputStream {
	
	/**
	 * Seek to the absolute position {@code position}. If {@code position}
	 * is not a position in the file (e.g. negative or greater than the size of
	 * the stream), it's assumed to seek to the beginning (resp. end) of the file.
	 * 
	 * @param position the position to seek to
	 * @return if seeking is possible
	 */
	public boolean seekTo(long position) {
		return false;
	}

	/**
	 * Seek relative to the current position. If the target position 
	 * is not a position in the file (e.g. negative or greater than the size of
	 * the stream), it's assumed to seek to the beginning (resp. end) of the file.
	 * 
	 * @param bytes the number of bytes to seek
	 * @return if seeking is possible
	 */
	public final boolean seekBy(long bytes) {
		return seekTo(currentPosition()+bytes);
	}

	/**
	 * 
	 * @return the current position in the stream (or {@code -1} if the current position is unknown)
	 */
	public abstract long currentPosition(); 

	/**
	 * 
	 * @return the size of this stream. A value of {@code -1} indicates that the stream size is unknown (and seeking <b>must</b> fail).
	 */
	public abstract long size();
}
