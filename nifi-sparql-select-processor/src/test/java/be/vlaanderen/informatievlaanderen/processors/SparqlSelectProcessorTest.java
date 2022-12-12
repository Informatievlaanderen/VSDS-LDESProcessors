/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.vlaanderen.informatievlaanderen.processors;

import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SparqlSelectProcessorTest {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(SparqlSelectProcessor.class);
    }

    public final String SelectQuery = """
            SELECT ?x ?fname ?gname
            WHERE {
                ?x  <http://www.w3.org/2001/vcard-rdf/3.0#FN>  ?fname .
                ?x  <http://www.w3.org/2001/vcard-rdf/3.0#N>/<http://www.w3.org/2001/vcard-rdf/3.0#Given> ?gname .
            }
            """;

    public final String FlowFileContents = """
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
            """;

    /**
     * Assert that a SPARQL Construct processor can manipulate a FlowFile.
     */
    @Test
    public void testSparqlConstructInferenceMode() {
        final TestRunner testRunner = TestRunners.newTestRunner(new SparqlSelectProcessor());

        testRunner.setProperty(SparqlSelectProcessor.SPARQL_QUERY, SelectQuery);

        testRunner.enqueue(FlowFileContents);

        testRunner.run();

        MockFlowFile f = testRunner.getFlowFilesForRelationship(SparqlSelectProcessor.SUCCESS).get(0);
        System.out.print(f.getContent());
        assert f.getContent().equals("[{\"x\":\"http://somewhere/MattJones/\",\"fname\":\"Matt Jones\",\"gname\":\"Matthew\"},{\"x\":\"http://somewhere/RebeccaSmith/\",\"fname\":\"Becky Smith\",\"gname\":\"Rebecca\"},{\"x\":\"http://somewhere/JohnSmith/\",\"fname\":\"John Smith\",\"gname\":\"John\"},{\"x\":\"http://somewhere/SarahJones/\",\"fname\":\"Sarah Jones\",\"gname\":\"Sarah\"}]");

    }

}
