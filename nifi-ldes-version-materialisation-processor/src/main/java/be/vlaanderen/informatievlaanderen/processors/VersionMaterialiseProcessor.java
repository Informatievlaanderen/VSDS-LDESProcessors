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
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static be.vlaanderen.informatievlaanderen.processors.VersionMaterialiser.reduceToLDESMemberOnlyModel;
import static be.vlaanderen.informatievlaanderen.processors.VersionMaterialiser.versionMaterialise;

@Tags({"ldes, vsds"})
@CapabilityDescription("Version materialisation of an LDES stream")
public class VersionMaterialiseProcessor extends AbstractProcessor {

    public static final ValueFactory vf = SimpleValueFactory.getInstance();
    public static final PropertyDescriptor IS_VERSION_OF = new PropertyDescriptor
            .Builder().name("isVersionOf")
            .displayName("Predicate used for isVersionOf")
            .required(true)
            .defaultValue("http://purl.org/dc/terms/isVersionOf")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .addValidator(StandardValidators.URI_VALIDATOR)
            .build();

    public static final PropertyDescriptor RESTRICT_OUTPUT_TO_MEMBER = new PropertyDescriptor
            .Builder().name("MembersOnly")
            .displayName("Restrict output to members")
            .description("When enabled, only the member and the blank nodes references are included.")
            .required(false)
            .defaultValue("false")
            .allowableValues("true", "false")
            .addValidator(StandardValidators.BOOLEAN_VALIDATOR)
            .build();


    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("Success")
            .description("Success relationship")
            .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("Failure")
            .description("Failure relationship")
            .build();

    private List<PropertyDescriptor> descriptors;

    private Set<Relationship> relationships;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        descriptors = new ArrayList<>();
        descriptors.add(IS_VERSION_OF);
        descriptors.add(RESTRICT_OUTPUT_TO_MEMBER);
        descriptors = Collections.unmodifiableList(descriptors);

        relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
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
        IRI isVersionOf = vf.createIRI(context.getProperty(IS_VERSION_OF).getValue());
        boolean RESTRICT_TO_MEMBERS = context.getProperty(RESTRICT_OUTPUT_TO_MEMBER).getValue().contentEquals("true");

        if ( flowFile == null ) {
            return;
        }
        AtomicBoolean is_successful = new AtomicBoolean(false);
        StringWriter outputStream = new StringWriter();
        session.read(flowFile, new InputStreamCallback() {
            @Override
            public void process(InputStream in) throws IOException {
                try {
                    String FragmentRDF = IOUtils.toString(in);
                    InputStream targetStream = IOUtils.toInputStream(FragmentRDF);
                    Model inputModel = Rio.parse(targetStream, "", RDFFormat.NQUADS);

                    Model versionMaterialisedModel = versionMaterialise(inputModel, isVersionOf);
                    if (RESTRICT_TO_MEMBERS)
                        versionMaterialisedModel = reduceToLDESMemberOnlyModel(versionMaterialisedModel);

                    Rio.write(versionMaterialisedModel, outputStream, RDFFormat.NQUADS);
                    is_successful.set(true);
                }
                catch (Exception e) {
                    getLogger().warn("Couldn't apply version materialisation on FlowFile.");
                    throw new RuntimeException(e);
                }

            }
        });
        if (is_successful.get()) {
            flowFile = session.write(flowFile, out -> out.write(outputStream.toString().getBytes()));
            session.transfer(flowFile, REL_SUCCESS);
        }
        else
            session.transfer(flowFile, REL_FAILURE);

    }

}
