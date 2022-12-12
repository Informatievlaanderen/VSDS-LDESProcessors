# Apache NiFi SPARQL construct processor

## About
A processor that runs a [SPARQL CONSTRUCT](https://www.w3.org/TR/rdf-sparql-query/) query on the contents of a FlowFile, and hands over the result of the query to downstream processors.
With this processor, new triples can be inferred, or a complete 'single message transform' can be applied, as to change the data to another datamodel.


## Build
This NiFi plugin can be build with Maven (needs maven installed):
```
mvn package
```
The NAR file is copied to the 'nifi-extentions' folder, in the root of the repository.

## Install
Either copy the NAR file to the NiFi extentions folder or bind-mount the 'nifi-extentions' folder in docker-compose.yml.

## Configuration
Add the processor to the NiFi workflow and configure it. The processor offers 2 parameters:

 - SPARQL Query: The SPARQL construct query that will be executed.
 - Inference mode: This property has two options:
   * Inference mode: The triples created by the construct query are added to the ones in the FlowFile. The result is the sum of the two.
   * Replace mode: The result of the processor (outgoing FlowFile) only contains the triples created by the query.
