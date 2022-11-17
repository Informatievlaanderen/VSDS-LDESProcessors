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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class RDFSerialisationProcessorTest {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(RDFSerialisationProcessor.class);
    }

    public final String FlowFileContents = """
        <http://data-in-flowfile/> <http://test/> "Source data!" .
        """;

    /**
     * Assert that a SPARQL Construct processor can manipulate a FlowFile.
     */
    @Test
    public void TestRDFSerialisation() throws IOException {
        testRunner.setProperty(RDFSerialisationProcessor.SOURCE_SERIALISATION, "Turtle");
        testRunner.setProperty(RDFSerialisationProcessor.TARGET_SERIALISATION, "JSON-LD");

        Path contextPath = Path.of("src/test/resources/gipod.jsonld");
        String context = Files.readString(contextPath);

        // @todo Replace this with inline file. How?
        System.out.print(context);
        testRunner.setProperty(RDFSerialisationProcessor.CONTEXT_URI, "https://essentialcomplexity.eu/gipod.jsonld");

        Path filePath = Path.of("src/test/resources/ldes-member.nt");
        String flowInput = Files.readString(filePath);
        testRunner.enqueue(flowInput);

        testRunner.run();

        MockFlowFile f = testRunner.getFlowFilesForRelationship(RDFSerialisationProcessor.SUCCESS).get(0);
        System.out.print(f.getContent());
    }

}
