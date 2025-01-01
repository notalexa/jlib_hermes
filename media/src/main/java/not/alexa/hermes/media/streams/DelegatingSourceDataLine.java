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

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Implementation of {@link SourceDataLine} delegating to a provided line.
 * Listener events are translated to represent this line as the source line.
 * Classes extending this class can fire events using the {@link #fireEvent(javax.sound.sampled.LineEvent.Type, long)} method.
 * 
 * @author notalexa
 */
public class DelegatingSourceDataLine implements SourceDataLine {
	private SourceDataLine delegate;	
	private List<LineListener> listeners=new ArrayList<>();

	public DelegatingSourceDataLine(SourceDataLine delegate) {
		this.delegate=delegate;
		delegate.addLineListener(new LineListener() {
			@Override
			public void update(LineEvent event) {
				fireEvent(event.getType(),event.getFramePosition());
			}
		});
	}
	
	protected void fireEvent(LineEvent.Type type,long pos) {
		if(listeners.size()>0) {
			LineEvent event=new LineEvent(this, type, pos);
			for(LineListener listener:listeners) {
				listener.update(event);
			}
		}
	}

	public void addLineListener(LineListener arg0) {
		if(!listeners.contains(arg0)) {
			listeners.add(arg0);
		}
	}

	public int available() {
		return delegate.available();
	}

	public void close() {
		delegate.close();
	}

	public void drain() {
		delegate.drain();
	}

	public void flush() {
		delegate.flush();
	}

	public int getBufferSize() {
		return delegate.getBufferSize();
	}

	public Control getControl(Type arg0) {
		return delegate.getControl(arg0);
	}

	public Control[] getControls() {
		return delegate.getControls();
	}

	public AudioFormat getFormat() {
		return delegate.getFormat();
	}

	public int getFramePosition() {
		return delegate.getFramePosition();
	}

	public float getLevel() {
		return delegate.getLevel();
	}

	public Line.Info getLineInfo() {
		return delegate.getLineInfo();
	}

	public long getLongFramePosition() {
		return delegate.getLongFramePosition();
	}

	public long getMicrosecondPosition() {
		return delegate.getMicrosecondPosition();
	}

	public boolean isActive() {
		return delegate.isActive();
	}

	public boolean isControlSupported(Type arg0) {
		return delegate.isControlSupported(arg0);
	}

	public boolean isOpen() {
		return delegate.isOpen();
	}

	public boolean isRunning() {
		return delegate.isRunning();
	}

	public void open() throws LineUnavailableException {
		delegate.open();
	}

	public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
		delegate.open(format, bufferSize);
	}

	public void open(AudioFormat format) throws LineUnavailableException {
		delegate.open(format);
	}

	public void removeLineListener(LineListener listener) {
		listeners.remove(listener);
	}

	public void start() {
		delegate.start();
	}

	public void stop() {
		delegate.stop();
	}

	public int write(byte[] b, int off, int len) {
		return delegate.write(b, off, len);
	}
}
