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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Collections;

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
	private WakeOnLan wol;

	public TVWakeup(String mac) {
		super("tv wakeup");
		setDaemon(true);
		if(mac!=null) {
			wol=new WakeOnLan(mac);
		}
	}
	
	public void shutdown() {
		shutdown=true;
	}
	
	public void run() {
		try(MulticastSocket socket=new MulticastSocket(5353)) {
			InetAddress addr=InetAddress.getByName("224.0.0.251");
			socket.joinGroup(addr);
			while(!shutdown) {
				if(wol!=null) {
					wol.send();
				}
				DatagramPacket data=new DatagramPacket(QUERY, QUERY.length,addr,5353);
				socket.send(data);
				Thread.sleep(1000);
			}
		} catch(Throwable t) {
			Handler.LOGGER.error("Sending wake up query failed.",t);
		}
	}
	
	/**
	 * Implements Wake on Lan (WOL) support. The magic packet is written on each interface with a broadcast address
	 * using the {@link #send()} method.
	 */
	public static class WakeOnLan {
		private byte[] message;
		
		/**
		 * Construct wake on lan support.
		 * 
		 * @param mac the mac address in format 11:22:33:44:55:66
		 */
		public WakeOnLan(String mac) {
			try {
				byte[] message=new byte[16*7];
				message[0]=message[1]=message[2]=message[3]=message[4]=message[5]=(byte)0xff;
				String[] s=mac.split(":");
				if(s.length==6) {
					for(int i=0;i<6;i++) {
						byte b=(byte)Integer.parseInt(s[i],16);
						for(int j=6;j<=16*6;j+=6) {
							message[j+i]=b;
						}
					}
				}
				this.message=message;
			} catch(Throwable t) {
				//Misconfigured. Ignore
			}
			
		}

		/**
		 * Send the magic packet over every network interface with a broadcast address.
		 */
		public void send() {
	        if(message!=null) for(int i=0;i<1;i++) try(DatagramSocket socket = new DatagramSocket()) {
				for(NetworkInterface nif:Collections.list(NetworkInterface.getNetworkInterfaces())) {
					for(InterfaceAddress ia:nif.getInterfaceAddresses()) {
						if(ia.getBroadcast()!=null) {
				            DatagramPacket packet = new DatagramPacket(message, message.length, ia.getBroadcast(), 9);
					        socket.send(packet);
						}
					}
				}
	        } catch(Throwable t) {
	        	// Ignore
	        }
		}
	}
}
