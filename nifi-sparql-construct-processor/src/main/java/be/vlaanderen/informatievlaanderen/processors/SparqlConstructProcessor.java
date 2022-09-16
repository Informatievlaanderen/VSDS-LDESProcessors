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

import org.apache.nifi.components.AllowableValue;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.io.InputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Tags({"ldes, vsds, SPARQL"})
@CapabilityDescription("SPARQL construct manipulation of an RDF flowfile.")
public class SparqlConstructProcessor extends AbstractProcessor {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final AllowableValue INFER = new AllowableValue(
            "infer",
            "Inference mode",
            "In infer mode, the result of the query will be added to the contents of the flowfile"
    );
    public static final AllowableValue REPLACE_ALL = new AllowableValue(
            "replace",
            "Replace mode",
            "In replace mode, the result of the CONSTRUCT query will replace the contents of the flowfile."
    );
    public static final PropertyDescriptor SPARQL_QUERY = new PropertyDescriptor
            .Builder().name("SPARQL_QUERY")
            .displayName("SPARQL Query")
            .required(true)
            .defaultValue("CONSTRUCT {?s ?p ?o} WHERE {?s ?p ?o}")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor INFERENCE_MODE = new PropertyDescriptor
            .Builder().name("INFERENCE_MODE")
            .displayName("Inference mode")
            .required(true)
            .allowableValues(INFER, REPLACE_ALL)
            .defaultValue(REPLACE_ALL.getValue())
            .build();

    public static final Relationship SUCCESS = new Relationship.Builder()
            .name("SUCCESS")
            .description("Success relationship")
            .build();

    public static final Relationship FAILURE = new Relationship.Builder()
            .name("FAILURE")
            .description("Failure relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(SPARQL_QUERY);
        descriptors.add(INFERENCE_MODE);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(SUCCESS);
        relationships.add(FAILURE);
        relationships = Collections.unmodifiableSet(relationships);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) {
        FlowFile flowFile = session.get();
        String QUERY = context.getProperty(SPARQL_QUERY).getValue();

        if ( flowFile == null ) {
            return;
        }
        StringWriter outputStream = new StringWriter();
        AtomicBoolean executedSuccessfully = new AtomicBoolean(false);
        session.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(InputStream RDFStream) throws IOException {
                Repository db = new SailRepository(new MemoryStore());
                try (RepositoryConnection connection = db.getConnection()) {
                    Model inputModel = Rio.parse(RDFStream, "", RDFFormat.NQUADS);

                    // In infer mode, include the current contents of the flowfile.
                    if (context.getProperty(INFERENCE_MODE).getValue().equals(INFER.getValue())) {
                        Rio.write(inputModel, outputStream,RDFFormat.NQUADS);
                    }

                    connection.add(inputModel);
                    GraphQuery query = connection.prepareGraphQuery(QUERY);

                    RDFHandler quadsWriter = Rio.createWriter(RDFFormat.NQUADS, outputStream);
                    query.evaluate(quadsWriter);
                    executedSuccessfully.set(true);
                }
                catch (Exception e) {
                    getLogger().error("Error executing SPARQL CONSTRUCT query.");
                }
            }
        });
        if (executedSuccessfully.get()) {
            flowFile = session.write(flowFile, out -> out.write(outputStream.toString().getBytes()));
            session.transfer(flowFile, SUCCESS);
        }
        else {
            session.transfer(flowFile, FAILURE);
        }
    }
}
