package not.alexa.hermes.media.playback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class Album {
    @JsonProperty private String id;
    @JsonProperty private String name;
    @JsonProperty private String[] trackIds;

    protected Album() {
    }

    public Album(String id,String name) {
        this.id=id;
        this.name=name;
    }

    public Album(String id,String name,String[] ids) {
        this(id,name);
        this.trackIds=ids;
    }

    public Album(String id,String name, List<? extends PlaybackTrack> tracks) {
        this(id,name);
        setTracks(tracks);
    }

    public List<? extends PlaybackTrack> setTracks(List<? extends PlaybackTrack> tracks) {
        this.trackIds=new String[tracks.size()];
        for(int i=0;i<tracks.size();i++) {
            trackIds[i]=tracks.get(i).getId();
        }
        return tracks;
    }

    public List<? extends PlaybackTrack> getAlbumTracks() {
        return Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public Object getName() {
        return name;
    }
}
