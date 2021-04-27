# fronius-energy

Access PV live logs using the Fronius Solar API V1.

Java programm which reads the current power production of the fronius symo inverter. It also reads and logs the daily energy productions.

If the production is over the POWER.THRESHOLD value, it switches on the component Shelly PLUG-S - if it goes down under the threshold, it switches the component off.

![Fonius Inverter](symo.jpg)

![Shelly Plug-S](shelly-plug-s.jpg)

Docker image is available here: [Image](https://hub.docker.com/repository/docker/pendl2/fronius-reader)

You can run the docker image with `docker run pendl2/fronius-reader`

Docker Environment Variable | Default | Description 
------------ | ------------- | ------------- 
FRONIUS.HOST | fronius | The host where, the Fronius API is running 
SHELLY.PLUGS | shelly-plug-s-1 | The Shelly PlugS host address 
POWER.THRESHOLD | 3500 | Long value in Watt. If threshold is reached, it activates the Shelly Plug-S component
LOG.LEVEL | INFO | Log Level, can be switched to debug, for detailed information

Example:
setting the host 0.0.0.0 via environment: `sudo docker run -e FRONIUS.HOST=0.0.0.0 pendl2/fronius-reader`

Open Points:

* Integrate the Shelly 3em energy meter
