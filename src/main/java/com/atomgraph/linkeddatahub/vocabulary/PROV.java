/**
 *  Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.atomgraph.linkeddatahub.vocabulary;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class PROV {
    
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://www.w3.org/ns/prov#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI()
    {
        return NS;
    }
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
        
    public static final OntClass Entity = m_model.createClass( NS + "Entity" );

    public static final OntClass Activity = m_model.createClass( NS + "Activity" );

    public static final OntClass Agent = m_model.createClass( NS + "Agent" );

    public static final ObjectProperty wasAttributedTo = m_model.createObjectProperty( NS + "wasAttributedTo" );

    public static final ObjectProperty wasDerivedFrom = m_model.createObjectProperty( NS + "wasDerivedFrom" );

    public static final ObjectProperty wasGeneratedBy = m_model.createObjectProperty( NS + "wasGeneratedBy" );

    public static final ObjectProperty wasStartedBy = m_model.createObjectProperty( NS + "wasStartedBy" );

    public static final DatatypeProperty startedAtTime = m_model.createDatatypeProperty( NS + "startedAtTime" );

    public static final DatatypeProperty endedAtTime = m_model.createDatatypeProperty( NS + "endedAtTime" );
    
    public static final DatatypeProperty generatedAtTime = m_model.createDatatypeProperty( NS + "generatedAtTime" );

}
