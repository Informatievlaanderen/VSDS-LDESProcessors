# Geospatial projection

## About
This NiFi processor changes all geo:wktLiterals within a Linked Data Event Stream from one projection to another.

The source coordinate reference system needs to be [present in the data (literal)](https://opengeospatial.github.io/ogc-geosparql/geosparql11/spec.html#_rdfs_datatype_geowktliteral).

## Build
This NiFi plugin can be build with Maven (needs maven installed):
```
mvn package
```
The NAR file is copied to the 'nifi-extentions' folder, in the root of the repository.

## Install
Either copy the NAR file to the NiFi extentions folder or bind-mount the 'nifi-extentions' folder in docker-compose.yml.

## Run
This processor depends on an external service to perform the conversion. Build the gdal image in the gdal-docker folder and run it.
```
docker build gdal-docker -t gdal-service
docker run -p 8090:8090 gdal-service
```

## Configuration
Add the processor to the NiFi workflow and configure it. The processor offers 2 parameters:

 - Target CRS: Set this to the URI of the target coordinate system, e.g. http://www.opengis.net/def/crs/EPSG/0/4326
 - Service endpoint URL: The location where the gdal service is running.

