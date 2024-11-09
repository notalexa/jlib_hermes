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

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;

/**
 * Reader for streams respecting the ogg container format.
 * 
 * @author notalexa
 */
class OggReader {
	private final int bufferSize=2048;
	private InputStream stream;
	private Packet joggPacket;
	private Page joggPage = new Page();
	private StreamState joggStreamState = new StreamState();
	private SyncState joggSyncState = new SyncState();
	
	public OggReader(InputStream stream) {
		this.stream=stream;
		joggSyncState.init();
		joggSyncState.buffer(bufferSize);
	}
	
	public Page currentPage() {
		return joggPage;
	}
	
	public void close() throws IOException {
		stream.close();
	}
	
	private boolean fill() throws IOException {
		int offset=joggSyncState.buffer(bufferSize);
		int count = stream.read(joggSyncState.data, offset, bufferSize);
		if(count>=0) {
			joggSyncState.wrote(count);
			return true;
		} else {
			return false;
		}
	}
	
	public int pageOffset() {
		return 0;
	}
	
	public Packet nextPacket() throws IOException {
		outerloop: while(true) {
			if(joggPacket!=null) {
				switch(joggStreamState.packetout(joggPacket)) {
				case -1: throw new IOException("There is a hole in the data");
				case 0: break;
				case 1: return joggPacket;
				}
			}
			while(true) {
				switch(joggSyncState.pageout(joggPage)) {
					case -1: throw new IOException("There is a hole in the data");
					case 0: if(!fill()) {
							return null;
						}
						continue;
					case 1: //System.out.println("Got page: "+joggPage.serialno()+", beginning="+joggPage.bos()+", ending="+joggPage.eos());
						if(joggPage.bos()!=0) {
							joggPacket=new Packet();
							joggStreamState=new StreamState();
							joggStreamState.init(joggPage.serialno());
						} else {
							if(joggPacket==null) {
								throw new IOException();
							}
						}
						joggStreamState.pagein(joggPage);
						continue outerloop;
				}
			}
		}
	}
}
