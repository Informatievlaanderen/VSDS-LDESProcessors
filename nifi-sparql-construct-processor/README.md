# Apache NiFi SPARQL construct processor

A processor that runs a SPARQL CONSTRUCT query on a FlowFile, and hands over the result of the query to downstream processors.

# Build the NAR file (Jar for NiFi)
```
mvn package
```
This will result in a NAR file being written to the 'nifi-extensions' folder. This folder is shared with the NiFi docker container.

