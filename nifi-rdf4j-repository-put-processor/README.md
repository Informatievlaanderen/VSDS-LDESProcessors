# Apache NiFi SPARQL put processor

A processor that materialises an LDES stream into a triplestore.

# Build the NAR file (Jar for NiFi)
```
mvn package
```
This will result in a NAR file being written to the 'release' folder. This folder is shared with the NiFi docker container.

# Download the NiFi client
Download the latest LDES NiFi Processor, and put in 'release' folder
e.g. https://github.com/Informatievlaanderen/VSDS-LDESClient-NifiProcessor/packages/1581623

# Run the stack
```
docker-compose up
```

