# Apache NiFi SPARQL select processor

## About
A processor that runs a [SPARQL SELECT](https://www.w3.org/TR/rdf-sparql-query/) query on the contents of a FlowFile, and hands over the result of the query to downstream processors.
This processor return a JSON array, with an object for each row returned by the query. The object contains key-value pairs.
This allows the rusulting output to be used by other NiFi processors that operate on JSON structures (e.g. ingest into a database).

Example:

A FLowFile containing the following data is passed to the processor.
```
<http://somewhere/MattJones/> <http://www.w3.org/2001/vcard-rdf/3.0#FN> "Matt Jones" .
<http://somewhere/MattJones/> <http://www.w3.org/2001/vcard-rdf/3.0#N> _:genid1 .
_:genid1 <http://www.w3.org/2001/vcard-rdf/3.0#Family> "Jones" .
_:genid1 <http://www.w3.org/2001/vcard-rdf/3.0#Given> "Matthew" .
            
<http://somewhere/RebeccaSmith/> <http://www.w3.org/2001/vcard-rdf/3.0#FN> "Becky Smith" .
<http://somewhere/RebeccaSmith/> <http://www.w3.org/2001/vcard-rdf/3.0#N> _:genid2 .
_:genid2 <http://www.w3.org/2001/vcard-rdf/3.0#Family> "Smith" .
_:genid2 <http://www.w3.org/2001/vcard-rdf/3.0#Given> "Rebecca" .
            
<http://somewhere/JohnSmith/> <http://www.w3.org/2001/vcard-rdf/3.0#FN> "John Smith" .
<http://somewhere/JohnSmith/> <http://www.w3.org/2001/vcard-rdf/3.0#N> _:genid3 .
_:genid3 <http://www.w3.org/2001/vcard-rdf/3.0#Family> "Smith" .
_:genid3 <http://www.w3.org/2001/vcard-rdf/3.0#Given> "John" .
            
<http://somewhere/SarahJones/> <http://www.w3.org/2001/vcard-rdf/3.0#FN> "Sarah Jones" .
<http://somewhere/SarahJones/> <http://www.w3.org/2001/vcard-rdf/3.0#N> _:genid4 .
_:genid4 <http://www.w3.org/2001/vcard-rdf/3.0#Family> "Jones" .
_:genid4 <http://www.w3.org/2001/vcard-rdf/3.0#Given> "Sarah" .
```
And the following query is set:
```
SELECT ?x ?fname ?gname
WHERE {
  ?x  <http://www.w3.org/2001/vcard-rdf/3.0#FN>  ?fname .
  ?x  <http://www.w3.org/2001/vcard-rdf/3.0#N>/<http://www.w3.org/2001/vcard-rdf/3.0#Given> ?gname .
}
```

The resulting output FlowFile will be:
```
[
   {
      "x":"http://somewhere/MattJones/",
      "fname":"Matt Jones",
      "gname":"Matthew"
   },
   {
      "x":"http://somewhere/RebeccaSmith/",
      "fname":"Becky Smith",
      "gname":"Rebecca"
   },
   {
      "x":"http://somewhere/JohnSmith/",
      "fname":"John Smith",
      "gname":"John"
   },
   {
      "x":"http://somewhere/SarahJones/",
      "fname":"Sarah Jones",
      "gname":"Sarah"
   }
]
```

## Build
This NiFi plugin can be build with Maven (needs maven installed):
```
mvn package
```
The NAR file is copied to the 'nifi-extentions' folder, in the root of the repository.

## Install
Either copy the NAR file to the NiFi extentions folder or bind-mount the 'nifi-extentions' folder in docker-compose.yml.

## Configuration
Add the processor to the NiFi workflow and configure it. The processor offers 1 parameter:

 - SPARQL Query: The SPARQL select query that will be executed.
