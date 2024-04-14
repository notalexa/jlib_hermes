# Android TV

This subproject provides a hermes handler controlling devices using the ATV remote control (v2) (used by Google within the Remote Service App since 2021 for example). Features are:

* Send keycodes to the tv.
* Send deep links to the tv.
* Wake up using multicast DNS (zeroconf).
* Turn on/off the tv.
* Provide waiting intervals.

Current limitations are:

* Remote service protocol version 2 (protobuf) only.
* The device (key) must already be paired. For a description see [here](androidtv/README.md#pairing)
* All other features of the Remote Service API are not supported.

To get started, configure an intent handler using

```[yaml]
- class: de.notalexa.hermes.androidtv.IntentHandler
  ip: 192.168.0.33
  keystore: <path to the keystore>
  passwd: <secret of the keystore (and key)>
```
`192.168.0.33` should be replaced by the ip address of your TV (which can be found using [this guide](https://displayradar.com/find-ip-address-on-tv).
The keystore (and it's password) must be configured **and paired to the tv** as described [below](#setup-security).  

The handler understands the intent `tv:executeCommand` with a slot `cmd`. The intent base ``tv`` can be changed using the configuration parameter
``intentBase``. The value of ``cmd`` should be a comma separated list of commands with the following content:


* A keycode from the list in {@link KeyCode}. The name is case insensitive and the preceding `KEYCODE_` can be omitted.
Therefor `turnon` or `turnoff` is perfectly valid.
* A number which is transformed in a list of {@code KeyCode}'s for every digit.
* A deep link starting with `http`. The link can be continued after a `,` by quoting the `,` with `\` (which must be quoted
at the end of a command with `\\`):
    * `http://example.com/?list=a\,b\,c` results in `http://example.com/?list=a,b,c`.
    * `http://example.com/?list=a\\,b\\,c\\` results in `http://example.com/?list=a\` (and commands `b\` and c\` which are ignored).
    * `http://example.com/?list=a\\\,b\\\,c\\` results in `http://example.com/?list=a\,b\,c\`.
* The keyword `won` (wait for on) waiting until either the connection timed out (ignoring everything else after this) or the
* TV is on.
* `w<time>` with `time` a number indicating to wait this number of milliseconds until processing the next message.
 
## Using deep links with a deep link lauchner
 
Deep links are the only possibility to launch applications on your TV. Unfortunately some popular apps do not provide an implementation of
deep links (like "Magenta TV" for example). In this case, you can use the [Deep Link Launcher](https://github.com/notalexa/proj_deeplinklauncher)
coming soon. With this launcher installed on your TV, it's possible to launch "Magenta TV" as `http://launcher.notalexa.de/launch?app=de.telekom.magentatv.tv`.

## Setup Security

To connect to the TV, you have to **pair a key** with the TV. An application can use this paired key to authenticate against the TV. Pairing the
key doesn't restrict to whatever application or host. You can copy the key (and certificate) to any host you want and can use it with any application
you want. This project expects a paired key inside a PKCS#12 keystore. Setting this up is described in the next sections.

This project doesn't provide pairing code. We describe pairing using [atvremote](https://github.com/drosoCode/atvremote) (which also provided together with <a href="https://github.com/Aymkdn/assistant-freebox-cloud/wiki/Google-TV-(aka-Android-TV-Remote-Control-(v2)">Aymkdn's wiki on the protocol v2</a>
the necessary insights into the protocol).

The steps are now:

<ol>
<li>
Create a private key ``app.key`` and self signed certificate ``app.crt`` using openssl

```
openssl req -x509 -newkey rsa:2048 -keyout app.key -out app.crt -sha256 -days 3650 -nodes -subj "/CN=Android TV Remote"
```

or on windows with this [description](https://learn.microsoft.com/en-us/entra/identity-platform/howto-create-self-signed-certificate).

<li> Pair the key using [atvremote](https://github.com/drosoCode/atvremote) with your TV:

1. Download the correct executable from the repository and put it into a bin directory.
1. Find out the IP address of your TV and turn the TV on.
1. Execute ``atvremote -ip="192.168.0.33" -version=2 -pair -cert=app.crt -key=app.key`` (on linux machines).
1. On the TV, a pairing code appears which must be entered.

<li>The last step is to create a PKCS#12 keystore as needed by the handler.

```
openssl pkcs12 -export -in app.crt -inkey app.key -name pairingkey -out pairingkey.p12
```

(the password is the password needed in your handler configuration)




</ol>






 
 
 
 