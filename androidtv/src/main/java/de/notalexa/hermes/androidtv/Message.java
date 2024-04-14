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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.notalexa.hermes.androidtv.Handler.Connection;

/**
 * Represents a message which can be send to the TV.
 * 
 */
public interface Message {
	public boolean perform(Connection con) throws Throwable;
	
	/**
	 * Resolve a message (as a string) to a list of messages of type {@link Message}.
	 * The incoming message should have:
	 * <ul>
	 * <li>Basic messages separated by {@code ,}.
	 * <li>A keycode from the list in {@link KeyCode}. The name is case insensitive and the preceding {@code KEYCODE_} can be omitted.
	 * Therefor {@code turnon} or {@code turnoff} is perfectly valid.
	 * <li>A number which is transformed in a list of {@code KeyCode}'s for every digit.
	 * <li>A deep link starting with {@code http}. The link can be continued after a {@code ,} by quoting the {@code ,} with {@code \} (which must be quoted
	 * at the end of a command with {@code \\}):
	 * <ul>
	 * <li>{@code http://example.com/?list=a\,b\,c} results in {@code http://example.com/?list=a,b,c}.
	 * <li>{@code http://example.com/?list=a\\,b\\,c\\} results in {@code http://example.com/?list=a\} (and commands {@code b\} and {@code c\} which are ignored).
	 * <li>{@code http://example.com/?list=a\\\,b\\\,c\\} results in {@code http://example.com/?list=a\,b\,c\}.
	 * </ul>
	 * <li>The keyword {@code won} (wait for on) waiting until either the connection timed out (ignoring everything else after this) or the
	 * TV is on.
	 * <li>{@code w<time>} with {@code time} a number indicating to wait this number of milliseconds until processing the next message.
	 * </ul>
	 * 
	 *  
	 * @param code
	 * @return
	 */
	public static Message[] resolve(String msg) {
		String[] codes=msg.split(",");
		List<Message> result=new ArrayList<>();
		String backup=null;
		for(String c:codes) try {
			if(backup!=null) {
				c=backup+","+c;
				backup=null;
			}
			if(c.endsWith("\\")) {
				c=c.substring(0,c.length()-1);
				if(c.endsWith("\\\\")) {
					backup=c.substring(0,c.length()-1);
					continue;
				} else {
					if(!c.endsWith("\\")) {
						backup=c;
						continue;
					}
				}
			}
			if(c.startsWith("http")) {
				result.add(new DeepLink(c));
			} else if("won".equals(c)) {
				result.add(new Wait());
			} else if(c.startsWith("w")) {
				// Handle this as a "normal" keycode if parsing fails
				long time=Long.parseLong(c.substring(1));
				result.add(new Wait(time));
			} else {
				int i=Integer.parseInt(c);
				List<KeyCode> programCodes=new ArrayList<>();
				if(i>=0) do {
					switch(i%10) {
						case 0:programCodes.add(KeyCode.KEYCODE_0); break;
						case 1:programCodes.add(KeyCode.KEYCODE_1); break;
						case 2:programCodes.add(KeyCode.KEYCODE_2); break;
						case 3:programCodes.add(KeyCode.KEYCODE_3); break;
						case 4:programCodes.add(KeyCode.KEYCODE_4); break;
						case 5:programCodes.add(KeyCode.KEYCODE_5); break;
						case 6:programCodes.add(KeyCode.KEYCODE_6); break;
						case 7:programCodes.add(KeyCode.KEYCODE_7); break;
						case 8:programCodes.add(KeyCode.KEYCODE_8); break;
						case 9:programCodes.add(KeyCode.KEYCODE_9); break;
					}
					i/=10;
				} while(i>0);
				Collections.reverse(programCodes);
				result.addAll(programCodes);
			}
		} catch(Throwable t) {
			KeyCode keyCode=KeyCode.fromString(c);
			if(keyCode!=KeyCode.KEYCODE_UNKNOWN) {
				result.add(keyCode);
			}
		}
		return result.toArray(new Message[0]);
	}

}
