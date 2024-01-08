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
package not.alexa.hermes.upnp;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.HermesApi;
import not.alexa.hermes.intent.handling.IntentHandler;
import not.alexa.hermes.nlu.NLUIntent;
import not.alexa.netobjects.Context;
import not.alexa.upnp.LocationDescriptor;
import not.alexa.upnp.UPnP;
import not.alexa.upnp.UPnPMessage;
import not.alexa.upnp.location.ClassLoaderLocationDescriptor;
import not.alexa.upnp.location.PublishedLocationDescriptor;

/**
 * An uPnP extension for intent handling. This helps to publish your hermes environment
 * in your home network.
 * <br>A possible configuration may look like
 * <pre>
 * - class: not.alexa.hermes.upnp.UPnPHandler
 *   httpPort: 49999
 *   publish:
 *   - urn: urn:schemas-upnp-org:device:notalexa-butler:1
 *     uuid: c36d063d-3cb5-36c0-b6c1-5318b940db02
 *     descriptor:
 *       name: hermes.xml
 *       content: |
 *         <?xml version="1.0" encoding="UTF-8" standalone="no"?>
 *         <butler id="address">
 *           <name>My Home, my Castle</name>
 *           <site id="default" mqtt="tcp://${MQTT_HOST}:1883"/>
 *           <locations siteId="default">
 *             <name>Living Room</name>
 *           </locations>
 *           <locations siteId="default">
 *             <name>Kitchen</name>
 *           </locations>
 *         </butler>
 * </pre>
 * The {@code uuid} is optional (and the descriptor fictional). If not set, the {@code urn} will be taken as the {@code uuid} assuming that there is exactly one device
 * in the network.
 * The following properties are availabe:
 * <ul>
 * <li>{@code address}: The multicast address (defaults to {@code 239.255.255.250}).
 * <li>{@code port}: The multicast port to use (defaults to {@code 1900}).
 * <li>{@code httpPort}: The http port do deliver descriptors. This is necessary if constant descriptors (like in the example above) are delivered.
 * <li>{@code sayByeByteOnClose}: Emit the bye-bye message on close (defaults to {@code true}.
 * <li>{@code ttl}: The time to live for a published message (defaults to {@code 300}.
 * <li>{@code mx}: The mx value to use (defaults to {@code 5}.
 * <li>{@code publish}: A list of {@link Device} to publish.
 * </ul>
 * 
 * @author notalexa
 */
public class UPnPHandler implements IntentHandler {
    @JsonProperty(defaultValue="239.255.255.250") String address;
    @JsonProperty(defaultValue="1900") int port=1900;
    @JsonProperty(defaultValue="-1") private int httpPort=-1;
    @JsonProperty List<Device> publish=new ArrayList<>();
    @JsonProperty(defaultValue="true") boolean sayByeByeOnClose=true;
    @JsonProperty(defaultValue="300") int ttl=300;
    @JsonProperty(defaultValue="5") int mx=5;

    private UPnP upnp;
    
	public UPnPHandler() {
	}

	@Override
	public boolean onIntentReceived(HermesApi api, NLUIntent intent) {
		if("resetUPnP".equals(intent.getIntent())) {
			if(upnp!=null) {
				upnp.reset();
			}
			return true;
		}
		return false;
	}

	@Override
	public void startup(HermesApi api,Context context) {
		if(upnp==null) try {
			upnp=new UPnP(address, port).setMX(mx).setTTL(ttl).sayByeByeOnClose(sayByeByeOnClose).setHttpPort(httpPort);
			upnp.start();
			for(Device msg:publish) {
				upnp.publish(msg.asUPnPMessage());
			}
		} catch(Throwable t) {
			upnp=null;
			context.getLogger().error("Failed to startup UPnP",t);
		}
	}

	@Override
	public void shutdown(HermesApi api,Context context) {
		if(upnp!=null) {
			upnp.close();
		}
	}
	
	/**
	 * A class representing a device to publish. Possible properties are (see <a href="http://upnp.org/specs/arch/UPnPDA10_20000613.pdf">this document</a> for details):
	 * <ul>
	 * <li>{@code urn}: The urn of the device
	 * <li>{@code uuid}: The uuid of the device (if not set, the {@code urn} is used to generate a UUID).
	 * <li>{@code descriptor}: The descriptor of this device. The descriptor consists of
	 * <ul>
	 * <li>{@code name}: The name of the descriptor (that is the path under which it's published).
	 * <li>{@¢ode content) (optional): The content of the descriptor. If the content is {@code null}, the name is resolved using the class loader.
	 * Otherwise, if the content has multiple lines, the content is assumed to be constant. Otherwise, if the content an URL, the URL is taken as the descriptor URL (therefore, it should be http or https). If not,
	 * the content is again resolved using the class loader.
	 * </ul>
	 * </ul> 
	 * 
	 * @author notalexa
	 */
	public static class Device {
	    @JsonProperty(required = true) protected String urn;
	    @JsonProperty(required = true) protected String uuid;
	    @JsonProperty(required = true) protected Descriptor descriptor;
		@JsonCreator
		public Device(@JsonProperty("urn") String urn,@JsonProperty("uuid") String uuid,@JsonProperty("descriptor") Descriptor descriptor) {
			this.urn=urn;
			this.uuid=uuid;
			this.descriptor=descriptor;
		}
		
		public UPnPMessage asUPnPMessage() {
			return new UPnPMessage(uuid==null?UUID.nameUUIDFromBytes(urn.getBytes()).toString():uuid, urn, descriptor.asLocationDescriptor());
		}
	}
	
	/**
	 * A descriptor based on {@linkplain PublishedLocationDescriptor}.
	 * 
	 * @author notalexa
	 */
	public static class Descriptor {
		@JsonProperty(required = true) protected String name;
		@JsonProperty protected String content;
		
		@JsonCreator
		public Descriptor(@JsonProperty("name") String name,@JsonProperty("content") String content) {
			this.name=name;
			this.content=content;
		}
		
		public LocationDescriptor asLocationDescriptor() {
			return content==null?new ClassLoaderLocationDescriptor(name):new PublishedLocationDescriptor(name, content);
		}
	}

}
