package be.vlaanderen.informatievlaanderen.processors;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class VersionMaterialiser {
    public static final ValueFactory vf = SimpleValueFactory.getInstance();
    public static Model versionMaterialise(Model inputModel, IRI versionPredicate) {
        Model versionMaterialisedModel = new TreeModel();

        Map<Resource, IRI> versionIDEntityIDMap = getVersionIDEntityIDMap(inputModel, versionPredicate);

        inputModel.forEach(statement -> {
            Resource subject = statement.getSubject();
            IRI predicate = statement.getPredicate();
            Value object = statement.getObject();

            // Statement needs 'de-versioning', replacing the subject.
            if (versionIDEntityIDMap.containsKey(statement.getSubject()))
                subject = versionIDEntityIDMap.get(statement.getSubject());

            // Object references a versioned entity, replace it with the 'de-versioned' identifier.
            if (statement.getObject().isResource() && versionIDEntityIDMap.containsKey((Resource) statement.getObject()))
                object = versionIDEntityIDMap.get((Resource) statement.getObject());

            // Don't add isVersionOf statements.
            if (statement.getPredicate().equals(versionPredicate))
                return;

        /*
        @todo According to https://github.com/TREEcg/version-materialize-rdf.js "created" statements should be moved
        to a version object, referenced by hasVersion. Since I can't come up with a reason why this would be useful,
        or preferred over having the created statement in the entity itself, I'm not looking into this for now...
        */
            versionMaterialisedModel.add(vf.createStatement(subject, predicate, object));
        });
        return versionMaterialisedModel;
    }
    private static Map<Resource, IRI> getVersionIDEntityIDMap(Model model, IRI isVersionOfPredicate) {
        Map<Resource, IRI> map = new HashMap<>();
        model.getStatements(null, isVersionOfPredicate, null).forEach(memberStatement -> {
            if (!memberStatement.getObject().isResource()) {
                throw new RuntimeException(String.format(
                        "Statement <subject: %s predicate: %s> should have object identifier as object.",
                        memberStatement.getSubject().toString(),
                        memberStatement.getPredicate().toString()
                ));
            }
            map.put(memberStatement.getSubject(), (IRI) memberStatement.getObject());
        });
        return map;
    }

    /**
     * Builds a model limited to statements about the ldes:member, including potential nested blank nodes.
     * Excludes statements about referenced entities, provided as context.
     *
     * @param inputModel The model to reduce.
     * @return The reduced model.
     */
    public static Model reduceToLDESMemberOnlyModel(Model inputModel) {
        Stack<Resource> subjectsOfIncludedStatements = new Stack<>();
        Model LDESMemberModel = new TreeModel();

        // LDES Member statements
        inputModel.getStatements(null, Tree.MEMBER, null).forEach(memberStatement -> {
            // @todo Include statement that the ldes:member belongs to the stream?
            //outModel.add(memberStatement);
            subjectsOfIncludedStatements.push((Resource) memberStatement.getObject());
        });

        /*
         * LDES members can contain blank node references. All statements of those blank nodes
         * need to be included in the output as well. Blank nodes can be nested,
         * hence the need to keep track of them with a stack.
         */
        while (!subjectsOfIncludedStatements.isEmpty()) {
            Resource subject = subjectsOfIncludedStatements.pop();
            inputModel.getStatements(subject, null, null).forEach((Statement includedStatement) -> {
                LDESMemberModel.add(includedStatement);
                Value object = includedStatement.getObject();
                if (object.isBNode()) {
                    subjectsOfIncludedStatements.push((Resource) object);
                }
            });
        }
        return LDESMemberModel;
    }
}
