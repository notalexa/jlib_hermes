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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Wakeup thread if the TV is not responding to the first connection attempt. The thread sends a multicast DNS query with
 * domains {@code _androidtvremote2._tcp.local} and {@code _googlecast._tcp.local}. Note that {@code _androidtvremote._tcp.local}
 * is ommitted since the version 1 protocol is not supported at the moment.
 * <p>Typically this wakes up the TV answering with it's domain and ip address which is ignored because the ip is statically configured.
 * 
 */
public class TVWakeup extends Thread {
	private static byte[] QUERY=new byte[] {
			0x00,0x00, // Id
			0x00,0x00, // Flags
			0x00,0x02, // Queries
			0x00,0x00, // Answers
			0x00,0x00, // Authority Answers
			0x00,0x00, // Additional Answers
			// Query _androidtvremote2._tcp.local
			0x11,0x5f,0x61,0x6e,0x64,0x72,0x6f,0x69,0x64,0x74,0x76,0x72,0x65,0x6d,0x6f,0x74,0x65,0x32,
			0x04,0x5f,0x74,0x63,0x70,
			0x05,0x6c,0x6f,0x63,0x61,0x6c,
			0x00,
			0x00,0x0c,
			0x00,0x01,
			// Query _googlecast._tcp.local
			0x0b,0x5f,0x67,0x6f,0x6f,0x67,0x6c,0x65,0x63,0x61,0x73,0x74,
			0x04,0x5f,0x74,0x63,0x70,
			0x05,0x6c,0x6f,0x63,0x61,0x6c,
			0x00,
			0x00,0x0c,
			0x00,0x01,
	};
	
	private boolean shutdown;
	

	public TVWakeup() {
		super("tv wakeup");
		setDaemon(true);
	}
	
	public void shutdown() {
		shutdown=true;
	}
	
	public void run() {
		try(MulticastSocket socket=new MulticastSocket(5353)) {
			InetAddress addr=InetAddress.getByName("224.0.0.251");
			socket.joinGroup(addr);
			while(!shutdown) {
				DatagramPacket data=new DatagramPacket(QUERY, QUERY.length,addr,5353);
				socket.send(data);
				Thread.sleep(1000);
			}
		} catch(Throwable t) {
			Handler.LOGGER.error("Sending wake up query failed.",t);
		}
	}
}
