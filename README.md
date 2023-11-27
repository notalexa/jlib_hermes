# Hermes API

Java library for the Hermes API (including Rhasspy extensions).

For a documentation of the different messages, see <a href="https://rhasspy.readthedocs.io/en/latest/reference/#mqtt-api">the Rhasspy reference</a>.
The implementation includes another extension: `FeaturesRequest` which can be used to obtain the currently available features in the
network (including the site ids). For a list of features see `Feature`.

`HermesApi` itself doesn't provide a mechanism of attaching to an MQTT instance. Instead, published messages are handled locally. To attach to a
MQTT instance, use `HermesMqtt`.
<p>
**Usage** follows the typical network object paradigm: Overlay the messages you want to implement and override the `HermesMessage#received(HermesApi)`
method. Define a type loader with the given overlays and create a context for this loader. In the context, register the resources, the overlays need.
Create an api instance with the context.

This API defines one overlay `FeaturesRequestHandler` of `FeaturesRequest` which can be used to implement the `Feature.Features`
feature. An example for this would be

```java
Context context=new DefaultTypeLoader().overlay(FeaturesRequestHandler.class).createContext();
HermesApi api=new HermesMqtt(context,"default");
```

(Note that implementing features **always requires a site id** while using a site doesn't.)
Another example can be found in the TTS implementation.


