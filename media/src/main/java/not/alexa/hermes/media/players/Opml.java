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
package not.alexa.hermes.media.players;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.hermes.media.AudioStream.AudioInfo;

/**
 * Helper class for TuneIn.
 * 
 * @author notalexa
 */
class Opml {
	@JsonProperty protected Body body;

	public Opml() {
	}
	
	public static String getUrl(String id) {
		if(id.startsWith("tunein://")) {
			id=id.substring("tunein://".length());
		}
		return "http://opml.radiotime.com/Tune.ashx?id="+id.replace('?','&');
	}
	
	public static String getDescriptionUrl(String id) {
		return "http://opml.radiotime.com/describe.ashx?id="+id;
	}
	
	public String getTitle(String defaultTitle) {
		return body==null?defaultTitle:body.getTitle(defaultTitle);
	}
	
	public String getStation(String defaultStation) {
		return body==null?defaultStation:body.getStation(defaultStation);
	}

	public String getArtist(String defaultArtist) {
		return body==null?defaultArtist:body.getArtist(defaultArtist);
	}

	public static class Outline {
		@JsonProperty("@text") String text;
		@JsonProperty Station station;
		protected Outline() {
		}

		public String getTitle(String defaultTitle) {
			return station.getTitle(text==null?defaultTitle:text);
		}
		
		public String getArtist(String defaultArtist) {
			return station.getArtist(defaultArtist);
		}
		
		public String getStation(String defaultStation) {
			return text==null?defaultStation:text;
		}
	}
	
	public static class Station {
		@JsonProperty String current_song;
		@JsonProperty String current_artist;
		protected Station() {}

		public String getTitle(String defaultTitle) {
			return current_song==null?defaultTitle:current_song;
		}

		public String getArtist(String defaultArtist) {
			return current_artist==null?defaultArtist:current_artist;
		}
	}
	
	public static class Body {
		@JsonProperty protected List<Outline> outline;
		protected Body() {}

		public String getTitle(String defaultTitle) {
			return outline==null||outline.isEmpty()?defaultTitle:outline.get(0).getTitle(defaultTitle);
		}

		public String getStation(String defaultStation) {
			return outline==null||outline.isEmpty()?defaultStation:outline.get(0).getStation(defaultStation);
		}

		public String getArtist(String defaultArtist) {
			return outline==null||outline.isEmpty()?defaultArtist:outline.get(0).getArtist(defaultArtist);
		}
	}

	public not.alexa.hermes.media.AudioStream.AudioInfo getAudioInfo() {
		return new AudioInfo(getArtist(null),getTitle(null),-1);
	}

	public not.alexa.hermes.media.AudioStream.AudioInfo getOverview() {
		return new AudioInfo(null,getStation(null),-1);
	}
}
