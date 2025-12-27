# Media Player

This project implements audio players for various sources, a class to control a player over hermes and output control using pulse audio.

Base class for exposing audio functionality to hermes is ``not.alexa.hermes.media.HermesPlayer``. The class defines

* an intent handler for various intents
* a component handling play bytes

A base configuration (for tracks in the music folder and a tuner) is

```
class: not.alexa.hermes.media.HermesPlayer
publishState: true
initialVolume: 0.5
noInfo: I don't know
shortInfo: You are listening to {0}
longInfo: You are listening to {0} by {1}
player:
  class: not.alexa.hermes.media.players.AudioPlayers
  players:
  - class: not.alexa.hermes.media.players.JukeBox
    name: jukebox
    baseDir: ${user.home}/Music
  - class: not.alexa.hermes.media.players.Tuner
```

``publishState`` enables output of state info to the *topic* ``notalexa/mediaplayer/state`` (see ``HermesPlayer.StateMessage``), ``initialVolume`` is
the initial volume on startup, the info fields controls the reply of the *intent* ``mediaplayer/info`` (0 is the title, 1 is the artist) and player is the
underlying player.

The following *intents* are recognized:

* ``mediaplayer/play`` or ``mediaplayer/turnon``: Start playing. A source can be specified in the slot ``source``
* ``mediaplayer/pause`` or ``mediaplayer/turnoff``: Stop playing
* ``mediaplayer/volume``: Set the volume. Either slot ``value`` for an absolute value 0&leq;value&leq;1 or ``offset`` for a relative value -1&leq;offset&leq;1 must be set.
* ``mediaplayer/shuffle``: To enable shuffling
* ``mediaplayer/noshuffle``: To disable shuffling
* ``mediaplayer/forward``: Disable repeating tracks and albums
* ``mediaplayer/info``: Reply information about the current track
* ``mediaplayer/state``: Request the current state of the player. The state is written to the *topic* ``notalexa/mediaplayer/state`` independent of ``publishState``
* ``mediaplayer/track``: Slot ``cmd`` **must** contain one of the following commands:
<ul>
<li>``next``: Play the next track
<li>``prev``: Play the previous track
<li>``replay``: Replay the current track (start from the beginning)
<li>``first``: Play the first track of the album
<li>``random``: Play a random track in the album
<li>``repeat``: Repeat the current track
<li>``forward``: Stop repeating the current track
<li>``peek``: Peek the tracks in this album. The slot ``hint`` **must** be set and the slot ``duration`` with the number of seconds to peek **may** be set (defaulting
to 5 seconds). 
</ul>
* ``mediaplayer/album``: Slot ``cmd`` **must** contain one of the following commands:
<ul>
<li>``next``: Play the first track in the next album
<li>``prev``: Play the first track in the previous album
<li>``replay``: Replay the current album (start from the beginning)
<li>``random``: Play the first track of a random album
<li>``repeat``: Repeat the current album
<li>``forward``: Stop repeating the current album
<li>``peek``: Peek the albums in this player. The slot ``hint`` **must** be set and the slot ``duration`` with the number of seconds to peek **may** be set (defaulting
to 5 seconds). 
</ul>
* ``select``: Select the currently peeked track or album. The slot ``choose`` can be set to ``false`` to ignore the selection of the currently peeked track or album.
  
  
Output can be controlled using the intent handler ``not.alexa.hermes.media.pulseaudio.PulseAudioHandler``. The handler recognizes the intent

* ``mediaplayer/output``: The slot ``device`` contains the name of the output device (sink).
  
A possible configuration is

```
class: not.alexa.hermes.media.pulseaudio.PulseAudioHandler
sinks:
- key: alsa_output.platform-soc_sound.stereo-fallback
  value: speakers
```

In this configuration one fixed sink with name ``speakers`` is defined (the key is taken from a ``pactl list short sinks`` on a raspberry pi with a hifiberry dac).

The handler recognizes paired and trusted bluetooth devices which provide the a2dp profile. If not connected, the handler tries to connect to the device for one minute.
Typically, it is sufficient to turn the device on to initiate the connection (and the take over). The name of the device can be obtained using ``bluetoothctl devices``. The name (ignoring upper/lowercase) is the string after the mac address. For example, on the raspberry pi, the command produces

```
Device 98:8E:79:00:9C:0F Qudelix-5K
Device 7C:96:D2:E0:23:55 Teufel ROCKSTER Cross
```

with names ``qudelix-5k`` (a bluetooth headphone amplifier) and ``teufel rockster cross`` (bluetooth speakers) (both were paired and trusted in a previous step).

### Audio Players

The project provides three audio players:

##### AudioPlayers

``not.alexa.hermes.media.players.AudioPlayers`` is a container for players. Only configuration parameter is ``players``, which expects a list
of players (see above).

##### JukeBox

``not.alexa.hermes.media.players.JukeBox`` is a player for local tracks in the file system. 
A jukebox can be turned on using the url ``player:&lt;name&gt;`. In this case a random file is choosen if no file is currently set,
otherwise, the current track is resumed.
Additionally tracks can be selected using ``track:&lt;name&gt;`` (without file extension) and albums with ``album:&lt;name&gt;``. In both cases,
the name must match the file resp. directory name exactly.

It's possible to define different juke boxes (to distinguish between different genres for example). For example

```
players:
- class: not.alexa.hermes.media.players.JukeBox
  name: jukebox
  baseDir: ${user.home}/Music
- class: not.alexa.hermes.media.players.JukeBox
  name: pop
  baseDir: ${user.home}/Music/Pop
- class: not.alexa.hermes.media.players.JukeBox
  name: classical_music
  baseDir: ${user.home}/Music/Classical_Music
```

defines three boxes, one for all music, one for pop and one for classic.

##### Tuner

``not.alexa.hermes.media.players.Tuner`` is a player for network streams and optimized for streaming. 
To turn the tuner on use either ``player:tuner`` (starts the last played stream if any)
or choose an url. The following additional types are recognized:

 * ``tuner://silence`` is fix and plays silence
 * ``tuner://&lt;name&gt;`` selects one of the configured urls (see below for configuration) or the error stream if the name is not configured.
 * ``tunein://&lt;name&gt;`` selects the TuneIn channel with the given id. To find out the id of a stream, go to <a href="https://tunein.com">TuneIn</a>
and search for the stream. The id is the last part (beginning with ``s``) of the url. For example, searching for "WDR 2" results in <a href="https://tunein.com/radio/WDR-2-Rheinland-1004-s213886/">https://tunein.com/radio/WDR-2-Rheinland-1004-s213886/</a>
and the id is ``s213886``.

Configuration: Two (optional) parameters can be set:

<ul>
<li><code>errorURL</code> denotes the url used if an url cannot be resolved (for example if <code>tuner://unknown</code> is requested but <code>unknown</code> is not a
configured URL. This field defaults to <code>tuner://silence</code> but if somebody wants WDR 2 as it's favourite error stream it can be set to 
<code>tunein://s213886</code>.
<li> 
<code>urls</code> is a map between symbolic names and real urls (including <code>tuner://silence</code>). The stream can than selected using <code>tuner://&lt;symbolic name&gt;</code>. After

```
urls:
- key: wdr2
  value: tunein://s213886
```

it's possible to select "WDR 2" using ``tuner://wdr2``.
</ul>

##### DLNA player

``not.alexa.hermes.media.players.DlnaPlayer`` is a player accessing DLNA media content. Only (required) attribute is ``name`` representing
the name of the player. The player accepts URL's of the form ``dlna://<control-uri>/<content-id>`` which can be provided either hardcoded
or using a third party tool. Note, that

* the ``<control-uri>`` part can be deduced from the UPnP descriptor of the media server and
* the ``<content-id>`` is server specific, but ``0`` is always recognized as the root container.

Additional information can be found in [the documentation of the class](/media/src/main/java/not/alexa/hermes/media/players/DlnaPlayer.java)




  