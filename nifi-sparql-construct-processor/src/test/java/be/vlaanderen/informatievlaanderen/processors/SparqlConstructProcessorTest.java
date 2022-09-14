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

public class SparqlConstructProcessorTest {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(SparqlConstructProcessor.class);
    }

    public final String ConstructQuery = """
        CONSTRUCT {
          <http://inferred-triple/> <http://test/> "Inferred data"
        }
        WHERE { ?s ?p ?o }
        """;

    public final String InvalidQuery = """
            SELECT ?s ?p ?o
            { ?s ?p ?o }
            """;

    public final String FlowFileContents = """
        <http://data-in-flowfile/> <http://test/> "Source data!" .
        """;

    /**
     * Assert that a SPARQL Construct processor can manipulate a FlowFile.
     */
    @Test
    public void testSparqlConstructInferenceMode() {
        final TestRunner testRunner = TestRunners.newTestRunner(new SparqlConstructProcessor());
        testRunner.setProperty(SparqlConstructProcessor.INFERENCE_MODE, "infer");
        testRunner.setProperty(SparqlConstructProcessor.SPARQL_QUERY, ConstructQuery);

        testRunner.enqueue(FlowFileContents);

        testRunner.run();

        MockFlowFile f = testRunner.getFlowFilesForRelationship(SparqlConstructProcessor.SUCCESS).get(0);
        assert f.getContent().contains("Source data!");
        assert f.getContent().contains("Inferred data");

    }

    /**
     * Assert that a SPARQL Construct processor can manipulate a FlowFile.
     */
    @Test
    public void testSparqlConstructReplaceMode() {
        final TestRunner testRunner = TestRunners.newTestRunner(new SparqlConstructProcessor());
        testRunner.setProperty(SparqlConstructProcessor.INFERENCE_MODE, "replace");
        testRunner.setProperty(SparqlConstructProcessor.SPARQL_QUERY, ConstructQuery);

        testRunner.enqueue(FlowFileContents);

        testRunner.run();

        MockFlowFile f = testRunner.getFlowFilesForRelationship(SparqlConstructProcessor.SUCCESS).get(0);
        // In replace mode, source data shouldn't be present.
        assert !f.getContent().contains("Source data!");
        assert f.getContent().contains("Inferred data");
    }

    /**
     * Assert that an invalid SPARQL query result in failure routing.
     */
    @Test
    public void testSparqlConstructInvalidQuery() {
        final TestRunner testRunner = TestRunners.newTestRunner(new SparqlConstructProcessor());
        testRunner.setProperty(SparqlConstructProcessor.INFERENCE_MODE, "replace");
        testRunner.setProperty(SparqlConstructProcessor.SPARQL_QUERY, InvalidQuery);

        testRunner.enqueue(FlowFileContents);

        testRunner.run();

        testRunner.assertAllFlowFilesTransferred(SparqlConstructProcessor.FAILURE);

    }

}
