# UPnP and HTTP (server) support

Both UPnP and HTTP(server) support are realized as ``IntentHandler`` implementations.

## UPnP

Base class of the UPnP service is ``not.alexa.hermes.upnp.UPnPHandler``. For more insight into UPnP [see this tutorial](http://upnp.org/resources/documents/UPnP_UDA_tutorial_July2014.pdf). Configuration of this component has to parts:

* first part are overall configurations. In most cases, the default values for ``address``, ``port``, ``sayByeByeOnClose``, ``ttl`` and ``mx`` cam be kept. The configuration of ``httpPort`` is in most cases necessary. If positiv, a very simple http server is started which delivers the descriptor files. This server is necessary if the descriptor is either a constant or resolved using the class loader (see below).
* second part is a list of devices to publish. Each device consists of an
  <ul><li>uri (the device type) which is required.
  <li>a uuid identifying the device. This uuid must uniquely identify the device and is optional in this implementation. If not set, the implementation assumes exactly one device of the given type and uses a uuid generated out of the uri
  <li>a descriptor which describes the device</ul>
  
A valid configuration is for example

```
class: not.alexa.hermes.upnp.UPnPHandler
httpPort: 49999
publish:
- urn: urn:schemas-upnp-org:device:notalexa-butler:1
  uuid: c36d063d-3cb5-36c0-b6c1-5318b940db02
  descriptor:
  name: butler.xml
  content: |
    <?xml version="1.0" encoding="UTF-8" standalone="no"?>
    <butler id="address">
      <name>My Home, my Castle</name>
      <site id="default" mqtt="tcp://${MQTT_HOST}:1883"/>
      <locations siteId="default">
        <name>Living Room</name>
      </locations>
      <locations siteId="default">
        <name>Kitchen</name>
      </locations>
    </butler>
```

with a constant (fictional) descriptor.

The handler recognizes one intent: ``resetUPnP`` will reset all cached descriptors.
  
### UPnP device descriptor

The device descriptor is an XML file delivered via HTTP (the location is referenced in the UPnP message itself). In this implementation, the descriptor consists of
an (optional) name and (optional) content (but at least one must be set). If the name is <b>set</b>, the descriptor is delivered using this name and the mini HTTP server
and therefore, the HTTP port <b>must</b> set. If the name is <b>not set</b>, the content <b>must be set and must be a valid url pointing to the descriptor</b>.

For content, the following setups are possible:

* The content is set and starts either with &lt?xml or has more than one line: It is considered as a <b>constant value</b> and the name <b>must be set</b>.
* The content is null or doesn't describe an url: The name (in case of a null content) or the content is considered as a path and resolved using the class loader. The name <b>must be set</b>.
* The content is an url: The name is optional and the url should point to the descriptor.

 
## HTTP

Base class of the Http service is ``not.alexa.hermes.http.HttpHandler``. This handler doesn't recognize any intents. Configuration of this handler has two parts:

* First part are overall settings: ``port`` is required and denotes the port on which the server should listen.
* Second part is a list of handlers serving content for different urls.

A valid configuration is

```
class: not.alexa.hermes.http.HttpHandler
port: 8080
handlers:
- class: not.alexa.hermes.http.DirectoryHandler
  path: /
  dir: ${user.dir}/www
```

which delivers files from the base directory ``${user.dir}/www``.

### Directory handler

The directory handler serves files from a dedicated directory and has two configuration parameters:

* ``dir`` denotes the base directory
* ``path`` denotes the path which should match to use this handler

For example if directory is configured as in the above example, the content of ``http://localhost:8080/docs/hermes.html`` should be located at ``${user.dir}/www/docs/hermes.html``. This same file is resolved if path is set to ``/docs/`` (but the url ``http://localhost:8080/src/hermes.md`` will <b>not</b> be resolved to ``${user.dir}/www/src/hermes.md`` since the path doesn't match).
