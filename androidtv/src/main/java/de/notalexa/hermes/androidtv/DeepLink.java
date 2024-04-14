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
 * Perform a deep link command on the TV.
 */
public class DeepLink implements Message {
	private static Protobuf proto=Protobuf.getInstance();
	private String link;
	
	/**
	 * 
	 * @param link the link to transmit to the TV
	 */
	public DeepLink(String link) {
		this.link=link;
	}

	@Override
	public boolean perform(Connection con) throws Throwable {
		Thread.sleep(500);
		Handler.LOGGER.info("Send link {} to {}",link,con.getIp());
		con.writeCommand(
				proto.message(
						proto.struct(90,
								proto.string(1,link))));
		return true;
	}
}
