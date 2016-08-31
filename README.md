Sample WRRS Java Server
-----------------------

See [WRRS JavaScript Client](https://github.com/Wikience/WRRS-JS) for details.

**Note:** this WRRS Java server is intended only for demonstration purposes and not a production ready software. 

Running this Sample WRRS Java Server
------------------------------------

* Create ```RServer-1-DEMO-all.jar``` by running maven ```package``` task.
* Create folder ```netcdf```
* Place folders ```conf```, ```xml``` and ```netcdf``` in the working directory of ```RServer-1-DEMO-all.jar```
* ```java -jar RServer-1-DEMO-all.jar```

 Folder ```netcdf``` contains a sample NetCDF file that could be used for WRRS-JS testing and running this server. 
 
 If you would like more NetCDF files, download some sample NetCDF files from ftp://climate.wikience.org/netcdf_sample/ into ```netcdf``` folder.


Author
------
[Antonio Rodriges](http://www.wikience.org/rodriges/) (developer & maintainer).

**License:** [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)