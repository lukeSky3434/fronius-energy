# fronius-energy

Access PV live logs using the Fronius Solar API V1.

Java programm which reads the current power production of the fronius symo inverter. It also reads the daily energy productions.


Docker image is available here: [Image](https://hub.docker.com/repository/docker/pendl2/fronius-reader)

You can run the docker image with `docker run pendl2/fronius-reader`

Docker Environment Variable | Description 
------------ | -------------
FRONIUS.HOST | The host where, the Fronius API is running 

Example:
setting the host 0.0.0.0 via environment: `sudo docker run -e FRONIUS.HOST=0.0.0.0 pendl2/fronius-reader`

Open Points:

* Integrate the Shelly 3em energy meter
* Integrate Shelly Plug S components
