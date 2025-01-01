# DSP Support

This subproject of the media player provides support for digital signal processors. Background and the only tested scenario is a Raspberry PI (5) with a Hifiberry AMP100 and the DSP add on. In this scenario, the DSP (an ADAU1451 from Analog Devices) is fully programmable using the Tool [SigmaStudio](https://www.analog.com/en/resources/evaluation-hardware-and-software/software/ss_sigst_02.html) provided by Analog Devices.

Hifiberry provides software for the DSP in [this repo](https://github.com/hifiberry/hifiberry-dsp) and some examples of DSP profiles [here](https://github.com/hifiberry/hifiberry-os/tree/master/buildroot/package/dspprofiles). Most of the low level code is a port of this software.

SigmaStudio runs on Windows and the DSP is not directly accessible from this computer. To test the profile in real time, a client/server approach is used. A SigmaStudio compatible server and how to install it cam be found in the repo mentioned above. The hifiberry toolkit and this implementation communicates with the DSP via this server and can be run anywhere in the network.

SigmaStudio stores profiles in a proprietary binary format in files with extension `dspproj`. The content can be exported after successful compilation. The exported XML file contains all data needed. The profile on the DSP can be manipulated directly by modifying values at specific addresses. For example, a mute element can be switched or the volume gain can be adjusted. These addresses may switch whenever the profile is changed and must be adjusted for different profiles. Hifiberry and this project introduces meta data assigning local names to these memory addresses resolved at runtime. Since these mappings are different for different profiles, mapping and currently loaded profile are linked using a checksum provided by the dsp.

## Enable DSP support in the player configuration

To enable dsp support, add the following configuration to the `HermesPlayer` configuration:

```
class: not.alexa.hermes.media.HermesPlayer
  profiler:
    class: not.alexa.hermes.media.streams.dsp.DSPProfileDecorator
    default: nodsp
  players: 
  - class: not.alexa.hermes.media.players.ExternalSourcePlayer
    name: tv
    profile: spdif
```

(and some additional profiles if wanted). This enables general profile support and an external player (playing silence on the audio line expecting some external input source) with name `tv` and the `spdif` profile.


## Creating a profile from the exported XML file

The profile stores the checksum of the underlying DSP program. At the time of writing, we were not able to determine how this checksum is really calculated. Therefore, a running DSP is needed for generating the profile. The program is transferred to the DSP, the checksum is read and the DSP is reset.

The profile can be created using

```
java -cp ... not.alexa.hermes.media.dsp.DSP [--host hostname] [--file] <XML export of SigmaStudio>
```

The profile is written to `stdout`. Logical names are generated from the names of the various controls. The following types are currently supported:

* `mute` are mute controls.
* `gain` are gain controls (such as volume).
* `register` are registers which can be read out.
* `data` can contain a certain value.

The following logical names are recognized in the [SourceDataLine](src/main/java/not/alexa/hermes/media/streams/dsp/DSPSourceDataLine.java) implementation of this project:

* `volume`of type `gain` controls the volume.
* `mute` of type `mute` controls if the line is muted.
* `lock` of type `register` denotes the "lock" register of input and controls, if the input channel is locked.
* `channel` of type `data` denotes the channel used in the profile.

In the AMP100 scenario, all controls are typically not needed, since volume and mute are controlled in the application (that is the source data line) and the channel is the application sound (serial input).
No lock is needed to determine if input is available. A special scenario is the SPDIF channel (the TV is attached to this channel in our scenario). In this case, the SPDIF lock register controls if input is available and the application has to open an audio line to enable output. The channel is the SPDF channel with no mixins and mute and volume are controlled inside the DSP. To support secondary line input, the SPDF line must be dimmed and the serial input must be mixed in. The provided profile uses a second channel for this purpose (since the secondary line is wrapped by some silence to separate the input, which means, that a logic inside the profile is not possible).

## Provided Profiles

We defined two profiles. The `dspproj` and XML exports are in folder [src/dist/dspprofiles](src/dist/dspprofiles), the profiles in [src/main/resources/profiles](src/main/resources/profiles):

* `nodsp` bypasses the DSP. Serial input is directly mapped to serial output. This profile can be used as the default profile
* `spdif` defines a profile suitable for SPDIF input.

 


