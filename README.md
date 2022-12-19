# VSDS NiFi processors

One of the cornerstones on which the 'Smart Data Space Flanders' (~Vlaamse Smart Data Space, VSDS) is built, is the[Linked Data Event Stream (LDES)](https://w3id.org/ldes/specification) specification.
LDES allows data consumers to subscribe to a stream of data, and stay in sync with changes in near real-time.


This repository contains a collection of Apache Nifi processors that help working with LDES streams.
These processors are considered experimental. Once they reach sufficient maturity, they will be moved to the [LDES workbench](https://github.com/Informatievlaanderen/VSDS-LDESWorkbench-NiFi).

## Available processors

* [Geospatial projection](nifi-geospatial-projection-processor)
  Transforms geometries in a stream from one coordinate reference system to another.

* [Version materialisation](nifi-ldes-version-materialisation-processor)
  Turns [version objects](https://w3id.org/ldes/specification#version-materializations) into regular objects.

* [Put RDF4J repository](nifi-rdf4j-repository-put-processor)
  Materialises a stream into a triplestore, making use of the RDF4J repository api.

* [Sparql construct](nifi-sparql-construct-processor)
  Transforms data in stream by applying a SPARQL construct query on the data.
