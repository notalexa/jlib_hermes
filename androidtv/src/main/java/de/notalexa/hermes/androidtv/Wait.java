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
package de.notalexa.hermes.androidtv;

import de.notalexa.hermes.androidtv.Handler.Connection;

/**
 * Wait either for time or until the TV is on.
 */
public class Wait implements Message {
	private boolean forRunning;
	private long time;
	
	/**
	 * Construct a wait until the TV is on (or the connection timed out).
	 */
	public Wait() {
		this(true,0);
	}
	
	/**
	 * Wait until the given time elapsed.
	 * 
	 * @param time the time to wait.
	 */
	public Wait(long time) {
		this(false,time);
	}
	
	/**
	 * Most generic constructor waiting for the given time after the TV is running controlled via {@link #forRunning}
	 * 
	 * @param forRunning if {@code true} wait until the TV is running
	 * @param time wait the given time
	 */
	public Wait(boolean forRunning,long time) {
		this.forRunning=forRunning;
		this.time=time;
	}

	@Override
	public boolean perform(Connection con) throws Throwable {
		if(forRunning) {
			if(!con.waitForRunningTV()) {
				return false;
			}
		}
		Thread.sleep(time);
		return true;
	}
}
