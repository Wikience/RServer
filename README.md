Sample WRRS Java Server
-----------------------

See [WRRS JavaScript Client](https://github.com/Wikience/WRRS-JS) for details.

**Note:** this WRRS Java server is intended only for demonstration purposes and not a production ready software. 

Running this Sample WRRS Java Server
------------------------------------

* Create ```RServer-1-DEMO-all.jar``` by running maven ```package``` task.
* Create folder ```netcdf```
* Place folders ```conf```, ```xml``` and ```netcdf``` in the working directory of ```RServer-1-DEMO-all.jar```
* Download some sample NetCDF files ftp://climate.wikience.org/netcdf_sample/ into ```netcdf``` folder.
* ```java -jar RServer-1-DEMO-all.jar``` 

Author
------
[Antonio Rodriges](http://www.wikience.org/rodriges/) (developer & maintainer).

**License:** [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)