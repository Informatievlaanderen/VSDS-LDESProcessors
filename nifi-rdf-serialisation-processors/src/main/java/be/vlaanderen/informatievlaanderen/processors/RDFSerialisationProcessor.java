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

import com.github.jsonldjava.core.DocumentLoader;
import com.github.jsonldjava.core.JsonLdOptions;
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
import org.eclipse.rdf4j.rio.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


@Tags({"ldes, vsds, SPARQL"})
@CapabilityDescription("Changes the serialisation of the RDF in a FlowFile")
public class RDFSerialisationProcessor extends AbstractProcessor {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();

    public static final PropertyDescriptor SOURCE_SERIALISATION = new PropertyDescriptor
            .Builder().name("SOURCE_SERIALISATION")
            .displayName("Serialisation of the incoming FlowFile.")
            // @todo Make optional, and determine by mimetype.
            .required(true)
            .allowableValues(RIO_SUPPORTED_FORMATS())
            .build();

    public static final PropertyDescriptor TARGET_SERIALISATION = new PropertyDescriptor
            .Builder().name("TARGET_SERIALISATION")
            .displayName("Serialisation of the outgoing FlowFile.")
            .required(true)
            .allowableValues(RIO_SUPPORTED_FORMATS())
            .build();

    public static final PropertyDescriptor CONTEXT_URI = new PropertyDescriptor
            .Builder().name("CONTEXT_URI")
            .displayName("The JSON-LD context to apply.")
            .required(true)
            .dependsOn(TARGET_SERIALISATION, "JSON-LD")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
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

    public static AllowableValue[] RIO_SUPPORTED_FORMATS() {
        List<AllowableValue> supportedFileFormats = new ArrayList<>();

        Collection<RDFParserFactory> RDFParserFactories = RDFParserRegistry.getInstance().getAll();
        RDFParserFactories.forEach(rdfParserFactory -> {
            RDFFormat format = rdfParserFactory.getRDFFormat();
            supportedFileFormats.add(new AllowableValue(format.getName()));
        });
        return supportedFileFormats.toArray(new AllowableValue[0]);
    }



    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(SOURCE_SERIALISATION);
        descriptors.add(TARGET_SERIALISATION);
        descriptors.add(CONTEXT_URI);
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
        //System.setProperty(DocumentLoader.DISALLOW_REMOTE_CONTEXT_LOADING, "true");
        FlowFile flowFile = session.get();
        RDFFormat SOURCE_SER = getRDFFormatByName(context.getProperty(SOURCE_SERIALISATION).getValue());
        RDFFormat TARGET_SER = getRDFFormatByName(context.getProperty(TARGET_SERIALISATION).getValue());
        String JsonLDContext = context.getProperty(CONTEXT_URI).getValue();

        if ( flowFile == null ) {
            return;
        }
        StringWriter outputStream = new StringWriter();
        AtomicBoolean executedSuccessfully = new AtomicBoolean(false);
        session.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(InputStream RDFStream) throws IOException {
                Model inputModel = Rio.parse(RDFStream, "", SOURCE_SER);
                List<Resource> members = new LinkedList<>();
                if (!JsonLDContext.isEmpty()) {
                    // LDES Member statements
                    inputModel.getStatements(null, Tree.MEMBER, null).forEach(memberStatement -> {
                        // @todo Include statement that the ldes:member belongs to the stream?
                        //outModel.add(memberStatement);
                        members.add((Resource) memberStatement.getObject());
                    });
                    Map<String, Object> frame = getOptions(JsonLDContext);
                    outputStream.write(RDF2JSONLD.modelToJSONLD(inputModel, frame, members.get(0)));
                }
                else {
                    Rio.write(inputModel, outputStream, TARGET_SER);
                }
                executedSuccessfully.set(true);
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

    private Map<String, Object> getOptions(String jsonContext) {
        Map<String, Object> context = new HashMap<>();
        context.put("@context", jsonContext);
        return context;
    }



    private static RDFFormat getRDFFormatByName(String serialisationName) {
        Collection<RDFParserFactory> RDFParserFactories = RDFParserRegistry.getInstance().getAll();
        // @todo Should not set this to NQUADS as fallback...
        AtomicReference<RDFFormat> selectedFormat = new AtomicReference<>(RDFFormat.NQUADS);
        RDFParserFactories.forEach(rdfParserFactory -> {
            RDFFormat format = rdfParserFactory.getRDFFormat();
            if (format.getName().contentEquals(serialisationName)) {
                selectedFormat.set(format);
            }
        });
        return selectedFormat.get();
    }
}

