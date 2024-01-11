# Hermes API

Java library for the Hermes API (including Rhasspy extensions).

For a documentation of the different messages, see <a href="https://rhasspy.readthedocs.io/en/latest/reference/#mqtt-api">the Rhasspy reference</a>.
The implementation includes another extension: `FeaturesRequest` which can be used to obtain the currently available features in the
network (including the site ids). For a list of features see `Feature`.

`HermesApi` itself doesn't provide a mechanism of attaching to an MQTT instance. Instead, published messages are handled locally. To attach to a
MQTT instance, use `HermesMqtt` or ``HermesServer``.
<p>Clients typically provides messages but don't act on them. In this case it's sufficient to instantiate an API instance and publish to a MQTT client:

```java
HermesApi api=new HermesMqtt(context)
  .subscribeTo(...);
```

Exceptions of this rule where components needs to be registered are:

* Act on response messages like ``SayFinished``.
* Act on reply messages generated as an answer to the handling of an ``NLUIntent``.

If components needs to be provided either because we want to define a server or in situations mentioned above, the API 
**follows the typical network object paradigm**: Overlay the messages you want to implement and override the `HermesMessage.received(HermesApi)`
method to handle the request. Define a ``HermesComponent`` which configures the API with the following values:

* add new messages to ``extensions`` (see below)
* add resources to ``resources``. Resource instances are needed if overlays are inner classes of the resource class.
* add overlays to ``overlays``. This includes possible overlays of ``extensions``.

Add the components to the API constructor. An example of a server just implementing features is

```java
HermesApi api=new HermesMqtt(context,"default",Feature.getFeatureComponent())
   .subscribeTo(...);
```

(Note that implementing features **always requires a site id** while using a site doesn't.) 
Since servers typically run as java processes, the class ``HermesServer`` helps to simply instantiate such a server: Provide a yaml file ``server.yaml``
configuring the server (for examples see the documentation of the class)
and call

```
java not.alexa.hermes.mqtt.HermesServer server.yaml
```

Beside the core implementation, the project provides additional implementations of various components:

## Text to speech

A text to speech component based on a script can be found in the [TTS subproject](tts/README.md).


## Intent handling

The package provides a basic component for intent handling. The ``Stack`` keeps a list of handlers implementing the ``IntentHandler`` interface. If active,
the implementation calls all handlers until the handler accepts the intent. To configure the stack logging all incoming intents and reply
to time and date requests, the following configuration can be used:

```
class: not.alexa.hermes.intent.handling.Stack
handlers:
- class: not.alexa.hermes.intent.handling.LogHandler
- class: not.alexa.hermes.intent.handling.DateTimeHandler
  timeFormat: it is now {0}{1,choice,0# o'clock|0<:{1}}
  dateFormat: today we have the {1}th {0}th
```

The following handlers are provided:

##### Log intent

``not.alexa.hermes.intent.handling.LogHandler`` has no parameters and logs the intent in a JSON format. Use this as the last handler if unknown intents should be
logged. Use this as the first handler, if all intents should be logged.


##### Date/Time intent

``not.alexa.hermes.intent.handling.DateTimeHandler`` is a configurable handler which provides date and time information and demonstrates the reply feature.
The class handles a time intent (configurable via ``timeIntent``and defaulting to ``currentTime``) and a date intent (configurable via ``dateIntent``and defaulting to ``currentDate``) and replies with the time formatted with ``timeFormat`` or the date formatted with ``dateFormat``.

##### UPnP and Http

It's common to publish home functionality using UPnP or providing resources using a HTTP server which are provided in the [UPnP subproject](upnp/README.md)

##### Media (Audio) Server

One of the major motivations for this implementation was to provide an audio server in the [media subproject](media/README.md). The server has the following features:

* Can be controlled using the hermes API.
* Is open for extensions using the ``AudioPlayer`` API.
* Implements handling of audio output from the hermes API by mixing it into the main audio stream (combine this with the text to speech component).
* Provides playing audio from files.
* Provides playing audio from (http) urls.
* Provides playing audio from TuneIn.
* Provides information about the current track (which can be transformed to speech and mixed into the stream).
* Provides a state channel for information about the current playing state useful for integration into home automation systems.

Currently not supported but planned are

* Support for other audio formats then MP3
* DLNA Media renderer support
