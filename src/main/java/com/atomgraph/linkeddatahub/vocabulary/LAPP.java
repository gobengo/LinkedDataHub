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
public class LAPP
{

    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "https://w3id.org/atomgraph/linkeddatahub/apps#";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI()
    {
        return NS;
    }
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );

    // DOMAIN

    public static final OntClass Dataset = m_model.createClass( NS + "Dataset" );
    
    public static final OntClass Application = m_model.createClass( NS + "Application" );

    public static final OntClass AdminApplication = m_model.createClass( NS + "AdminApplication" );

    public static final OntClass EndUserApplication = m_model.createClass( NS + "EndUserApplication" );
    
    public static final ObjectProperty adminApplication = m_model.createObjectProperty( NS + "adminApplication" );

    public static final ObjectProperty endUserApplication = m_model.createObjectProperty( NS + "endUserApplication" );

    public static final ObjectProperty proxy = m_model.createObjectProperty( NS + "proxy" );

    public static final ObjectProperty prefix = m_model.createObjectProperty( NS + "prefix" );
    
    public static final DatatypeProperty readOnly = m_model.createDatatypeProperty( NS + "readOnly" );

}
