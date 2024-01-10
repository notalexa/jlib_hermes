# Text to Speech

This subproject provides a hermes component which implements a text to speech feature based on a script (defaulting to ``tts.sh``).

Programmatically, the following instantiates an API using a setup suitable for ``nanotts``.

```
HermesApi api=new HermesMqtt(context,"default",
  new TTS("tts.sh",
    "de_DE",
    new HashSet&lt;String&gt;(Arrays.asList(
      "de-DE",
      "en-US",
      "en-GB",
      "es-ES",
      "fr-FR",
      "it-IT")))).subscribeTo(...);
```

where ``tts.sh`` may look like 

```
#!/bin/bash
echo "$2" | nanotts -l ~/lib/nanotts/pico/lang -w -o /tmp/$$.wav -v $1 && \
cat /tmp/$$.wav && rm /tmp/$$.wav
```

The above text to speech configuration is equivalent to the following declarative approach useful in a hermes server:

```
class: not.alexa.hermes.service.tts.TTS
script: tts.sh # optional
defaultLanguage: de-DE
languages:
- de-DE
- en-US
- en-GB
- es-ES
- fr-FR
- it-IT
```

