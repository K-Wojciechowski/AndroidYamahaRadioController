# Simple Android app for controlling Yamaha net radio

## Features

* Shows a list of internet radio stations on the preset/favourite list in the MusicCast app and allows selecting them to play
* List can be filtered by radio station language
* Basic device controls: volume up/down, power on/off

## Configuration

Create `app/radio.properties` based on the provided `radio.properties.example`. Fill in your radio's IP address (you'll need to configure a static IP, which you can do by accessing the audio system over HTTP, or by setting up static DHCP assignment in your home network). You can also configure filtering. The `radio.no_wifi_button.visible` allows hiding the Wi-Fi button, which can be useful if you're using automation (e.g. Samsung's Modes and Routines) to ensure Wi-Fi is enabled when the app is running.

## Acknowledgements

Based on API documentation sourced from the pyamaha project: <https://github.com/rsc-dev/pyamaha/tree/master/doc>

Not affiliated with Yamaha Corporation, trademarks used for informational purposes only.
