package com;

import fr.inria.lille.shexjava.GlobalFactory;
import fr.inria.lille.shexjava.schema.Label;
import fr.inria.lille.shexjava.schema.ShexSchema;
import fr.inria.lille.shexjava.schema.parsing.GenParser;
import fr.inria.lille.shexjava.validation.RefineValidation;
import fr.inria.lille.shexjava.validation.ValidationAlgorithm;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ApplicationSample {

    public static void main(String[] args) throws Exception {
        Path schemaFile = Paths.get("classes","shex","fhir.shex");
        Path inputFile = Paths.get("classes","input","patient-examples-cypress-template.ttl");
        FileInputStream inputStream = new FileInputStream(inputFile.toString());
        List<Path> importDirectories = Collections.emptyList();

        // Perform ShEx validation on RDF
        RDF4J factory = new RDF4J();
        GlobalFactory.RDFFactory = factory; //set the global factory used in shexjava

        // load and create the shex schema
        System.out.println("Reading schema     " + System.currentTimeMillis());
        ShexSchema schema = GenParser.parseSchema(schemaFile,importDirectories);
        System.out.println("End reading schema " + System.currentTimeMillis());

        // load the model
        System.out.println("Reading data");
        String baseIRI = "http://a.example.shex/";
        Model data = Rio.parse(inputStream, baseIRI, RDFFormat.TURTLE);

        String rootSubjectIri = null;
        // Find the IRI of the subject with a nodeRole=treeRoot
        for (Resource resourceStream : data.subjects()) {
            if (resourceStream instanceof SimpleIRI) {
                Model filteredModel = data.filter(resourceStream, factory.getValueFactory().createIRI("http://hl7.org/fhir/nodeRole"), factory.getValueFactory().createIRI("http://hl7.org/fhir/treeRoot"), (Resource)null);
                if (filteredModel != null && filteredModel.subjects().size() == 1) {
                    Optional<Resource> rootResource = filteredModel.subjects().stream().findFirst();
                    if (rootResource.isPresent()) {
                        rootSubjectIri = rootResource.get().stringValue();
                        break;
                    }

                }
            }
        }

        // create the graph
        Graph dataGraph = factory.asGraph(data);

        // choose focus node and shapelabel
        IRI focusNode = factory.createIRI(rootSubjectIri);
        Label shapeLabel = new Label(factory.createIRI("http://hl7.org/fhir/shape/All"));

        ValidationAlgorithm validation = new RefineValidation(schema, dataGraph);
        System.out.println("Validating instance data         " + System.currentTimeMillis());
        validation.validate(focusNode, shapeLabel);
        System.out.println("End Validating instance data     " + System.currentTimeMillis());

        boolean result = validation.getTyping().isConformant(focusNode, shapeLabel);
        System.out.println("Does "+focusNode+" has shape "+shapeLabel+"? "+result);

        System.out.println("-------------------------------");



    }

}
