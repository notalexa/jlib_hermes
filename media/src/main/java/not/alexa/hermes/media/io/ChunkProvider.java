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

import java.util.function.BiConsumer;

import not.alexa.hermes.media.playback.Callback;

/**
 * Interface defining a provider of chunks. The chunks (in the correct order) are the content
 * of a playback file.
 * 
 * @author notalexa
 */
public interface ChunkProvider extends Callback {
	/**
	 * The default chunk size
	 */
	public static final int CHUNK_SIZE=0x20000;
	
	/**
	 * 
	 * @return the chunk size of this provider
	 */
	public int chunkSize();
	
	/**
	 * 
	 * @return the size of the underlying input stream
	 */
	public long size();
	
	/**
	 * Request a chunk from the input.
	 * 
	 * @param chunk the index of the requested chunk
	 * @param callback the callback to use after the chunk is retrieved
	 * @return the chunk itself if available (or loaded synchronously) or {@code null} otherwise
	 */
	public byte[] requestChunk(int chunk,BiConsumer<Integer,byte[]> callback);
}
