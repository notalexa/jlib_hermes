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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

/**
 * A {@link SeekableInputStream} based on a file.
 * 
 * @author notalexa
 */
public class SeekableFileInputStream extends SeekableInputStream {
	private final File file;
	private final RandomAccessFile randomAccessFile;
	private final FileInputStream in;

	public SeekableFileInputStream(String file) throws IOException {
		this(new File(file));
	}

	public SeekableFileInputStream(File file) throws IOException {
		this.file=file;
		randomAccessFile=new RandomAccessFile(file, "r");
		in=new FileInputStream(randomAccessFile.getFD());
	}
	

	public int available() throws IOException {
		return in.available();
	}
	
	public long size() {
		return file.length();
	}

	public void close() throws IOException {
		in.close();
		randomAccessFile.close();
	}

	public FileChannel getChannel() {
		return in.getChannel();
	}

	public void mark(int readlimit) {
		in.mark(readlimit);
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	public int read() throws IOException {
		return in.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return in.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		return in.read(b);
	}

	public void reset() throws IOException {
		in.reset();
	}

	public long skip(long n) throws IOException {
		return in.skip(n);
	}

	@Override
	public boolean seekTo(long bytes) {
		try {
			randomAccessFile.seek(bytes);
			return true;
		} catch(Throwable t) {
			return false;
		}
	}

	@Override
	public long currentPosition() {
		try {
			return randomAccessFile.getFilePointer();
		} catch(Throwable t) {
			return -1;
		}
	}
}
