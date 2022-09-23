package be.vlaanderen.informatievlaanderen.processors;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
        import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
        import org.eclipse.rdf4j.rio.RDFFormat;
        import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
        import org.eclipse.rdf4j.rio.Rio;

import com.github.jsonldjava.core.JsonLdOptions;
        import com.github.jsonldjava.core.JsonLdProcessor;
        import com.github.jsonldjava.utils.JsonUtils;
        import com.google.common.base.Charsets;


public class RDF2JSONLD {
    public static String modelToJSONLD(Model model, Map<String, Object> frame, Resource member ) {
        String RDF4JJSONLD = modelToRDF4JJSONLD(model);
        try {
            Map<String, Object> framedJson = getFramedJson(createJsonObject(RDF4JJSONLD), frame);
            return filterMember(framedJson, member);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String filterMember(Map<String, Object> framedJson, Resource member) throws IOException {
        List<Map<String,Object>>  graph = (List<Map<String, Object>>) framedJson.get("@graph");
        AtomicReference<String> JSON = new AtomicReference<>(JsonUtils.toPrettyString(graph));
                graph.forEach(object -> {
            String ID = (String) object.get("id");
            if (ID.contentEquals(member.toString())) {

                try {
                    JSON.set(JsonUtils.toPrettyString(object));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return JSON.get();
    }

    private static Map<String, Object> getFramedJson(Object json, Map<String, Object> frame) {
        try {
            return JsonLdProcessor.frame(json, frame, new JsonLdOptions());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static Object createJsonObject(String ld) {
        try (InputStream inputStream =
                     new ByteArrayInputStream(ld.getBytes(Charsets.UTF_8))) {
            return JsonUtils.fromInputStream(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String modelToRDF4JJSONLD(Model model) {
        StringWriter out = new StringWriter();
        RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, out);
        try {
            writer.startRDF();
            for (Statement st : model) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        } catch (RDFHandlerException e) {
            throw new RuntimeException(e);
        }
        return out.getBuffer().toString();
    }
}
