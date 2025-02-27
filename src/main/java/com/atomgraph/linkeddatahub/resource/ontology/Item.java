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
package com.atomgraph.linkeddatahub.resource.ontology;

import org.apache.jena.ontology.OntDocumentManager;
import java.net.URI;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Providers;
import com.atomgraph.core.MediaTypes;
import com.atomgraph.linkeddatahub.model.Service;
import com.atomgraph.client.util.DataManager;
import com.atomgraph.linkeddatahub.vocabulary.LSMT;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.UriInfo;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS resource that handles ontology item requests.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class Item extends com.atomgraph.linkeddatahub.resource.graph.Item
{

    private static final Logger log = LoggerFactory.getLogger(Item.class);

    private final Resource resource;
    
    @Inject
    public Item(@Context Request request, @Context UriInfo uriInfo, MediaTypes mediaTypes,
            com.atomgraph.linkeddatahub.apps.model.Application application, Optional<Ontology> ontology, Optional<Service> service,
            DataManager dataManager,
            @Context Providers providers, com.atomgraph.linkeddatahub.Application system)
    {
        super(request, uriInfo, mediaTypes, application, ontology, service, providers, system);
        this.resource = ModelFactory.createDefaultModel().createResource(uriInfo.getAbsolutePath().toString());
        if (log.isDebugEnabled()) log.debug("Constructing {}", getClass());
    }
    
    @PostConstruct
    public void init()
    {
        getResource().getModel().add(getDatasetAccessor().getModel(getURI().toString()));
    }
    
    @GET
    @Override
    public Response get(@QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        Resource topic = getResource().getPropertyResourceValue(FOAF.primaryTopic); // ontology is the topic
        
        if (getUriInfo().getQueryParameters().containsKey(LSMT.clear.getLocalName())) // this is just a flag, we don't need the argument value. TO-DO: change to post()!
        {
            if (topic == null)
            {
                if (log.isErrorEnabled()) log.error("Cannot clear ontology - no ontology is foaf:primaryTopic of this document: {}", getURI());
                throw new IllegalStateException("Cannot clear ontology - no ontology is foaf:primaryTopic of this document: " + getURI());
            }
            
            String ontologyURI = topic.getURI();
            
            if (OntDocumentManager.getInstance().getFileManager().hasCachedModel(ontologyURI))
                OntDocumentManager.getInstance().getFileManager().removeCacheModel(ontologyURI);
        }
        
        return super.get(defaultGraph, getURI());
    }
 
    @POST
    @Override
    public Response post(Model model, @QueryParam("default") @DefaultValue("false") Boolean defaultGraph, @QueryParam("graph") URI graphUri)
    {
        return super.post(model, false, getURI());
    }

    public Resource getResource()
    {
        return resource;
    }
    
}