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

import org.apache.commons.io.IOUtils;
import org.apache.jena.assembler.Mode;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class VersionMaterialiseProcessorTest {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();

    private TestRunner testRunner;

    @BeforeEach
    public void init() {
        testRunner = TestRunners.newTestRunner(VersionMaterialiseProcessor.class);
    }

    /**
     * Assert that a test file can be version materialised successfully.
     *
     * @throws IOException
     */
    @Test
    public void testVersionMaterialiseMemberOnly() throws IOException {
        final TestRunner testRunner = TestRunners.newTestRunner(VersionMaterialiseProcessor.class);

        testRunner.setProperty(VersionMaterialiseProcessor.IS_VERSION_OF, "http://purl.org/dc/terms/isVersionOf");
        testRunner.setProperty(VersionMaterialiseProcessor.RESTRICT_OUTPUT_TO_MEMBER, "true");

        String versionedMember = turtleFileToQuadString("src/test/resources/ldes-member-versioned.ttl");
        testRunner.enqueue(versionedMember);

        testRunner.run();

        MockFlowFile FlowFileOut = testRunner.getFlowFilesForRelationship(VersionMaterialiseProcessor.REL_SUCCESS).get(0);
        Model FlowFileOutModel = Rio.parse(FlowFileOut.getContentStream(), "", RDFFormat.NQUADS);
        InputStream ComparisonFile = new FileInputStream("src/test/resources/ldes-member-unversioned.ttl");
        Model ComparisonModel = Rio.parse(ComparisonFile, "", RDFFormat.TURTLE);

        assert Models.isomorphic(FlowFileOutModel, ComparisonModel);
    }

    /**
     * Assert that a test file can be version materialised successfully.
     *
     * @throws IOException
     */
    @Test
    public void testVersionMaterialiseWithContext() throws IOException {
        final TestRunner testRunner = TestRunners.newTestRunner(VersionMaterialiseProcessor.class);

        testRunner.setProperty(VersionMaterialiseProcessor.IS_VERSION_OF, "http://purl.org/dc/terms/isVersionOf");
        testRunner.setProperty(VersionMaterialiseProcessor.RESTRICT_OUTPUT_TO_MEMBER, "false");

        String versionedMember = turtleFileToQuadString("src/test/resources/ldes-member-versioned.ttl");
        testRunner.enqueue(versionedMember);

        testRunner.run();

        MockFlowFile FlowFileOut = testRunner.getFlowFilesForRelationship(VersionMaterialiseProcessor.REL_SUCCESS).get(0);
        Model FlowFileOutModel = Rio.parse(FlowFileOut.getContentStream(), "", RDFFormat.NQUADS);
        InputStream ComparisonFile = new FileInputStream("src/test/resources/ldes-member-unversioned-context-included.ttl");
        Model ComparisonModel = Rio.parse(ComparisonFile, "", RDFFormat.TURTLE);

        assert Models.isomorphic(FlowFileOutModel, ComparisonModel);
    }

    private String turtleFileToQuadString(String filename) {
        StringWriter VersionedNQuads = new StringWriter();
        try {
            Path filePath = Path.of(filename);
            // Processor needs nquads for now.
            String versionedInput = Files.readString(filePath);
            InputStream versionedInputStream = IOUtils.toInputStream(versionedInput);
            Model versionedModel = Rio.parse(versionedInputStream, "", RDFFormat.TURTLE);

            Rio.write(versionedModel, VersionedNQuads, RDFFormat.NQUADS);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return VersionedNQuads.toString();
    }

}
