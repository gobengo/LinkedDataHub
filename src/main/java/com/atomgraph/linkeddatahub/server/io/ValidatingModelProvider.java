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
package com.atomgraph.linkeddatahub.server.io;

import com.atomgraph.linkeddatahub.apps.model.EndUserApplication;
import com.atomgraph.linkeddatahub.model.Agent;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import org.apache.jena.rdfxml.xmloutput.impl.Basic;
import org.apache.jena.riot.Lang;
import com.atomgraph.processor.vocabulary.SIOC;
import com.atomgraph.server.exception.SPINConstraintViolationException;
import com.atomgraph.spinrdf.constraints.ConstraintViolation;
import com.atomgraph.spinrdf.constraints.ObjectPropertyPath;
import com.atomgraph.spinrdf.constraints.SimplePropertyPath;
import com.atomgraph.spinrdf.vocabulary.SP;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.binary.Hex;
import org.apache.jena.rdf.model.RDFWriterI;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS provider that skolemizes blank node resources in the input RDF model.
 * It also fixes values of various properties.
 * 
 * @see com.atomgraph.linkeddatahub.server.interceptor.RDFPostCleanupInterceptor#fixValues(List<String>, List<String>, String)
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ValidatingModelProvider extends com.atomgraph.server.io.ValidatingModelProvider
{
    private static final Logger log = LoggerFactory.getLogger(ValidatingModelProvider.class);
    
    @Context UriInfo uriInfo;
    @Context Providers providers;
    @Context SecurityContext securityContext;

    @Inject javax.inject.Provider<com.atomgraph.linkeddatahub.apps.model.Application> application;

    private final MessageDigest messageDigest;

    public ValidatingModelProvider(MessageDigest messageDigest)
    {
        this.messageDigest = messageDigest;
    }
    
//    @Override
//    public Model read(Model model, InputStream is, Lang lang, String baseURI)
//    {
//        if (lang == null) throw new IllegalArgumentException("Lang must be not null");
//
//        CollectingErrorHandler errorHandler = new CollectingErrorHandler(); // collect parse errors. do not throw exceptions
//        //ParserProfile parserProfile = RiotLib.profile(baseURI, true, true, errorHandler);
//        read(model, is, lang, baseURI, errorHandler);
//
//        if (!errorHandler.getViolations().isEmpty())
//        {
//            if (log.isDebugEnabled()) log.debug("RDF syntax errors detected while parsing model: {}", errorHandler.getViolations());
//            throw new RDFSyntaxException(errorHandler.getViolations(), model);
//        }
//
//        return model;
//    }
    
    @Override
    public Model write(Model model, OutputStream os, Lang lang, String baseURI)
    {
        if (lang == null) throw new IllegalArgumentException("Lang must be not null");
        
        if (lang.equals(Lang.RDFXML)) // round-tripping RDF/XML with user input may contain invalid URIs
        {
            //RDFWriter writer = model.getWriter(RDFLanguages.RDFXML.getName());
            RDFWriterI writer = new Basic(); // workaround for Jena 3.0.1 bug: https://issues.apache.org/jira/browse/JENA-1168
            writer.setProperty("allowBadURIs", true);
            writer.write(model, os, null);

            return model;
        }

        return super.write(model, os, lang, baseURI);
    }
    
    @Override
    public Model processRead(Model model)
    {
        ResIterator it = model.listSubjects();
        try
        {
            while (it.hasNext())
            {
                Resource resource = it.next();
                processRead(resource);
            }
        }
        finally
        {
            it.close();
        }

        return super.processRead(model); // apply processing from superclasses
    }
    
    public Resource processRead(Resource resource) // this logic really belongs in a ContainerRequestFilter but we don't want to buffer and re-serialize the Model
    {
        // TO-DO: convert to lambda functions
        if (resource.hasProperty(DCTerms.format) && resource.getProperty(DCTerms.format).getObject().isLiteral())
        {
            Resource format = resource.getProperty(DCTerms.format).
                changeObject(com.atomgraph.linkeddatahub.MediaType.toResource(MediaType.valueOf(resource.getProperty(DCTerms.format).getString()))).getResource();
            if (log.isDebugEnabled()) log.debug("Resource: {} Format: {}", resource, format);
        }

        if (resource.hasProperty(FOAF.mbox) && resource.getProperty(FOAF.mbox).getObject().isLiteral())
        {
            Resource email = resource.getProperty(FOAF.mbox).
                changeObject(resource.getModel().createResource("mailto:" + resource.getProperty(FOAF.mbox).getString())).getResource();
            if (log.isDebugEnabled()) log.debug("Resource: {} Email: {}", resource, email);
        }

        if (resource.hasProperty(FOAF.phone) && resource.getProperty(FOAF.phone).getObject().isLiteral())
        {
            Resource phone = resource.getProperty(FOAF.phone).
                changeObject(resource.getModel().createResource("tel:" + resource.getProperty(FOAF.phone).getString())).getResource();
            if (log.isDebugEnabled()) log.debug("Resource: {} Phone: {}", resource, phone);
        }

        if (resource.hasProperty(SIOC.EMAIL) && resource.getProperty(SIOC.EMAIL).getObject().isLiteral())
        {
            Resource email = resource.getProperty(SIOC.EMAIL).
                changeObject(resource.getModel().createResource("mailto:" + resource.getProperty(SIOC.EMAIL).getString())).getResource();
            if (log.isDebugEnabled()) log.debug("Resource: {} Email: {}", resource, email);
        }
        
        if (resource.hasProperty(SP.text) && resource.getProperty(SP.text).getObject().isLiteral())
        {
            try
            {
                String queryString = resource.getProperty(SP.text).getString();
                QueryFactory.create(queryString);
            }
            catch (QueryParseException ex)
            {
                if (log.isDebugEnabled()) log.debug("Bad request - SPARQL query is syntactically incorrect", ex);
                List<ConstraintViolation> cvs = new ArrayList<>();
                List<SimplePropertyPath> paths = new ArrayList<>();
                paths.add(new ObjectPropertyPath(resource, SP.text));
                cvs.add(new ConstraintViolation(resource, paths, null, ex.getMessage(), null));
                throw new SPINConstraintViolationException(cvs, resource.getModel());
            }
        }
        
        if (resource.hasProperty(RDF.type, SP.Update) &&
                resource.hasProperty(SP.text) &&
                resource.getProperty(SP.text).getObject().isLiteral())
        {
            String updateString = resource.getProperty(SP.text).getString();
            try
            {
                UpdateRequest update = UpdateFactory.create(updateString);
                Resource type = null;
                if (type != null)
                {
                    resource.addProperty(RDF.type, type);
                    if (log.isDebugEnabled()) log.debug("Resource: {} adding type: {}", resource, type);
                }
            }
            catch (QueryParseException ex)
            {
                if (log.isDebugEnabled()) log.debug("Bad request - SPARQL update is syntactically incorrect", ex);
                List<ConstraintViolation> cvs = new ArrayList<>();
                List<SimplePropertyPath> paths = new ArrayList<>();
                paths.add(new ObjectPropertyPath(resource, SP.text));
                cvs.add(new ConstraintViolation(resource, paths, null, ex.getMessage(), null));
                throw new SPINConstraintViolationException(cvs, resource.getModel());
            }
        }
        
        if (resource.hasProperty(RDF.type, OWL.Ontology))
        {
            // clear cached OntModel if ontology is updated. TO-DO: send event instead
            OntDocumentManager.getInstance().getFileManager().removeCacheModel(resource.getURI());
        }
        
        return resource;
    }
    
    @Override
    public Model processWrite(Model model)
    {
        // show foaf:mbox in end-user apps
        if (getApplication().get().canAs(EndUserApplication.class)) return model;
        // show foaf:mbox for authenticated agents
        if (getSecurityContext() != null && getSecurityContext().getUserPrincipal() instanceof Agent) return model;

        // show foaf:mbox_sha1sum for all other agents (in admin apps)
        return super.processWrite(hashMboxes(getMessageDigest()).apply(model)); // apply processing from superclasses
    }

    public static Function<Model, Model> hashMboxes(MessageDigest messageDigest)
    {
        return model ->
        {
            StmtIterator it = model.listStatements(null, FOAF.mbox, (Resource)null);
            List<Statement> mboxes = it.toList();
            it.close();

            List<Statement> mboxHashes = mboxes.stream().map(stmt -> mboxHashStmt(stmt, messageDigest)).
                filter(Objects::nonNull).
                collect(Collectors.toList());

            model.remove(mboxes);
            model.add(mboxHashes);
            
            return model;
        };
    }
    
    public static Statement mboxHashStmt(Statement stmt, MessageDigest messageDigest)
    {
        try (InputStream is = new ByteArrayInputStream(stmt.getResource().getURI().getBytes(StandardCharsets.UTF_8));
            DigestInputStream dis = new DigestInputStream(is, messageDigest))
        {
            String sha1Sum = Hex.encodeHexString(dis.getMessageDigest().digest());
            return stmt.getModel().createStatement(stmt.getSubject(), FOAF.mbox_sha1sum, sha1Sum);
        }
        catch (IOException ex)
        {
            // ignore mbox
            return null;
        }
    }
    
    @Override
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
    public javax.inject.Provider<com.atomgraph.linkeddatahub.apps.model.Application> getApplication()
    {
        return application;
    }
    
    public SecurityContext getSecurityContext()
    {
        return securityContext;
    }
    
    public MessageDigest getMessageDigest()
    {
        return messageDigest;
    }
    
}