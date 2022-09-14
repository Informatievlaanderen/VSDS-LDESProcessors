package be.vlaanderen.informatievlaanderen.processors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class Tree {
    public static final String NAMESPACE = "https://w3id.org/tree#";
    /**
     * Recommended prefix for the TREE namespace: "tree"
     */
    public static final String PREFIX = "tree";

    /**
     * An immutable {@link Namespace} constant that represents the TREE namespace.
     */
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    public final static IRI MEMBER = SimpleValueFactory.getInstance().createIRI(NAMESPACE, "member");
}
