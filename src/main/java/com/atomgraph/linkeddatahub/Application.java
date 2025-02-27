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
package com.atomgraph.linkeddatahub;

import com.atomgraph.linkeddatahub.server.mapper.ResourceExistsExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.HttpHostConnectExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.MessagingExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.auth.webid.WebIDLoadingExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.auth.webid.InvalidWebIDURIExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.auth.AuthorizationExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.auth.AuthenticationExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.auth.webid.WebIDCertificateExceptionMapper;
import com.atomgraph.client.MediaTypes;
import com.atomgraph.client.locator.PrefixMapper;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.LocationMapper;
import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriterRegistry;
import com.atomgraph.client.mapper.ClientErrorExceptionMapper;
import com.atomgraph.client.util.DataManager;
import com.atomgraph.client.util.DataManagerImpl;
import com.atomgraph.client.vocabulary.AC;
import com.atomgraph.client.writer.function.UUID;
import com.atomgraph.core.exception.ConfigurationException;
import com.atomgraph.core.io.DatasetProvider;
import com.atomgraph.core.io.ModelProvider;
import com.atomgraph.core.io.QueryProvider;
import com.atomgraph.core.io.ResultSetProvider;
import com.atomgraph.core.io.UpdateRequestProvider;
import com.atomgraph.core.mapper.BadGatewayExceptionMapper;
import com.atomgraph.core.provider.QueryParamProvider;
import com.atomgraph.linkeddatahub.writer.factory.DataManagerFactory;
import com.atomgraph.server.mapper.NotFoundExceptionMapper;
import com.atomgraph.core.riot.RDFLanguages;
import com.atomgraph.core.riot.lang.RDFPostReaderFactory;
import com.atomgraph.core.vocabulary.A;
import com.atomgraph.linkeddatahub.server.mapper.auth.webid.InvalidWebIDPublicKeyExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.ModelExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.OntClassNotFoundExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.jena.QueryExecExceptionMapper;
import com.atomgraph.linkeddatahub.server.mapper.jena.RiotParseExceptionMapper;
import com.atomgraph.linkeddatahub.apps.model.AdminApplication;
import com.atomgraph.linkeddatahub.model.Agent;
import com.atomgraph.linkeddatahub.model.CSVImport;
import com.atomgraph.linkeddatahub.apps.model.EndUserApplication;
import com.atomgraph.linkeddatahub.model.File;
import com.atomgraph.linkeddatahub.model.Service;
import com.atomgraph.linkeddatahub.writer.factory.xslt.XsltExecutableSupplier;
import com.atomgraph.linkeddatahub.writer.factory.XsltExecutableSupplierFactory;
import com.atomgraph.client.util.XsltResolver;
import com.atomgraph.client.writer.function.Construct;
import com.atomgraph.client.writer.function.ConstructForClass;
import com.atomgraph.linkeddatahub.client.filter.ClientUriRewriteFilter;
import com.atomgraph.linkeddatahub.io.HtmlJsonLDReaderFactory;
import com.atomgraph.linkeddatahub.io.JsonLDReader;
import com.atomgraph.linkeddatahub.writer.ModelXSLTWriter;
import com.atomgraph.linkeddatahub.listener.ImportListener;
import com.atomgraph.linkeddatahub.model.Import;
import com.atomgraph.linkeddatahub.model.RDFImport;
import com.atomgraph.linkeddatahub.model.UserAccount;
import com.atomgraph.linkeddatahub.server.mapper.auth.webid.WebIDDelegationExceptionMapper;
import com.atomgraph.linkeddatahub.model.impl.AgentImpl;
import com.atomgraph.linkeddatahub.model.impl.CSVImportImpl;
import com.atomgraph.linkeddatahub.model.impl.FileImpl;
import com.atomgraph.linkeddatahub.model.impl.ImportImpl;
import com.atomgraph.linkeddatahub.model.impl.RDFImportImpl;
import com.atomgraph.linkeddatahub.model.impl.UserAccountImpl;
import com.atomgraph.linkeddatahub.server.event.SignUp;
import com.atomgraph.linkeddatahub.server.factory.AgentContextFactory;
import com.atomgraph.linkeddatahub.server.factory.ApplicationFactory;
import com.atomgraph.linkeddatahub.server.filter.request.ApplicationFilter;
import com.atomgraph.linkeddatahub.server.filter.request.auth.WebIDFilter;
import com.atomgraph.linkeddatahub.server.io.ValidatingModelProvider;
import com.atomgraph.server.mapper.ConfigurationExceptionMapper;
import com.atomgraph.linkeddatahub.server.factory.OntologyFactory;
import com.atomgraph.linkeddatahub.server.factory.ServiceFactory;
import com.atomgraph.linkeddatahub.server.filter.request.OntologyFilter;
import com.atomgraph.linkeddatahub.server.filter.request.RDFPostCleanupFilter;
import com.atomgraph.linkeddatahub.server.filter.request.AuthorizationFilter;
import com.atomgraph.linkeddatahub.server.filter.request.auth.IDTokenFilter;
import com.atomgraph.linkeddatahub.server.filter.request.ContentLengthLimitFilter;
import com.atomgraph.linkeddatahub.server.filter.request.auth.ProxiedWebIDFilter;
import com.atomgraph.linkeddatahub.server.filter.response.ResponseHeaderFilter;
import com.atomgraph.linkeddatahub.server.filter.response.BackendInvalidationFilter;
import com.atomgraph.linkeddatahub.server.filter.response.XsltExecutableFilter;
import com.atomgraph.linkeddatahub.server.interceptor.RDFPostCleanupInterceptor;
import com.atomgraph.linkeddatahub.server.mapper.auth.oauth2.TokenExpiredExceptionMapper;
import com.atomgraph.linkeddatahub.server.model.impl.Dispatcher;
import com.atomgraph.linkeddatahub.server.security.AgentContext;
import com.atomgraph.linkeddatahub.server.util.MessageBuilder;
import com.atomgraph.linkeddatahub.vocabulary.LDH;
import com.atomgraph.linkeddatahub.vocabulary.LDHC;
import com.atomgraph.linkeddatahub.vocabulary.Google;
import com.atomgraph.linkeddatahub.vocabulary.LAPP;
import com.atomgraph.linkeddatahub.writer.Mode;
import com.atomgraph.linkeddatahub.writer.ModelXSLTWriterBase;
import com.atomgraph.linkeddatahub.writer.factory.ModeFactory;
import com.atomgraph.processor.vocabulary.AP;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.server.mapper.OntologyExceptionMapper;
import com.atomgraph.server.mapper.ParameterExceptionMapper;
import com.atomgraph.server.mapper.jena.DatatypeFormatExceptionMapper;
import com.atomgraph.server.mapper.jena.QueryParseExceptionMapper;
import com.atomgraph.server.mapper.jena.RiotExceptionMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.jena.enhanced.BuiltinPersonalities;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.riot.RDFParserRegistry;
import org.slf4j.Logger;
import java.net.URI;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletContext;
import javax.ws.rs.core.CacheControl;
import javax.xml.transform.Source;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.LoggerFactory;
import com.atomgraph.server.mapper.SHACLConstraintViolationExceptionMapper;
import com.atomgraph.server.mapper.SPINConstraintViolationExceptionMapper;
import com.atomgraph.spinrdf.vocabulary.SP;
import com.github.jsonldjava.core.DocumentLoader;
import com.github.jsonldjava.core.JsonLdOptions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import net.jodah.expiringmap.ExpiringMap;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import nu.xom.XPathException;
import org.apache.http.HttpResponse;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.graph.GraphReadOnly;
import org.apache.jena.vocabulary.LocationMappingVocab;
import org.apache.jena.vocabulary.RDF;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.HttpMethodOverrideFilter;

/**
 * JAX-RS 1.x application subclass.
 * Used to configure the JAX-RS web application in <code>web.xml</code>.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://jersey.github.io/documentation/1.19.1/jax-rs.html#d4e186">Deploying a RESTful Web Service</a>
 */
public class Application extends ResourceConfig
{
    
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static final String REQUEST_ACCESS_PATH = "request access";
    public static final String AUTHORIZATION_REQUEST_PATH = "acl/authorization-requests/";
    public static final String UPLOADS_PATH = "uploads";
    
    private final EventBus eventBus = new EventBus();
    private final DataManager dataManager;
    private final MediaTypes mediaTypes;
    private final Client client, importClient, noCertClient;
    private final Query authQuery, ownerAuthQuery, webIDQuery, agentQuery, userAccountQuery, ontologyQuery; // no relative URIs
    private final Integer maxGetRequestSize;
    private final boolean preemptiveAuth;
    private final Processor xsltProc = new Processor(false);
    private final XsltCompiler xsltComp;
    private final XsltExecutable xsltExec;
    private final OntModelSpec ontModelSpec;
    private final Source stylesheet;
    private final boolean cacheStylesheet;
    private final boolean resolvingUncached;
    private final URI baseURI, uploadRoot; // TO-DO: replace baseURI with ServletContext URI?
    private final boolean invalidateCache;
    private final Integer cookieMaxAge;
    private final CacheControl authCacheControl;
    private final Integer maxContentLength;
    private final Address notificationAddress;
    private final Authenticator authenticator;
    private final Properties emailProperties = new Properties();
    private final KeyStore keyStore, trustStore;
    private final URI secretaryWebIDURI;
    private final ExpiringMap<URI, Model> webIDmodelCache = ExpiringMap.builder().expiration(1, TimeUnit.DAYS).build(); // TO-DO: config for the expiration period?
    private final ExpiringMap<String, Model> oidcModelCache = ExpiringMap.builder().variableExpiration().build();
    private final Map<URI, XsltExecutable> xsltExecutableCache = new HashMap<>();
    private final MessageDigest messageDigest;
    
    private Dataset contextDataset;
    
    public Application(@Context ServletConfig servletConfig) throws URISyntaxException, MalformedURLException, IOException
    {
        this(servletConfig,
            new MediaTypes(),
            servletConfig.getServletContext().getInitParameter(A.maxGetRequestSize.getURI()) != null ? Integer.valueOf(servletConfig.getServletContext().getInitParameter(A.maxGetRequestSize.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(A.cacheModelLoads.getURI()) != null ? Boolean.parseBoolean(servletConfig.getServletContext().getInitParameter(A.cacheModelLoads.getURI())) : true,
            servletConfig.getServletContext().getInitParameter(A.preemptiveAuth.getURI()) != null ? Boolean.parseBoolean(servletConfig.getServletContext().getInitParameter(A.preemptiveAuth.getURI())) : false,
            servletConfig.getServletContext().getInitParameter(AP.cacheSitemap.getURI()) != null ? Boolean.valueOf(servletConfig.getServletContext().getInitParameter(AP.cacheSitemap.getURI())) : true,
            new PrefixMapper(servletConfig.getServletContext().getInitParameter(AC.prefixMapping.getURI()) != null ? servletConfig.getServletContext().getInitParameter(AC.prefixMapping.getURI()) : null),
            com.atomgraph.client.Application.getSource(servletConfig.getServletContext(), servletConfig.getServletContext().getInitParameter(AC.stylesheet.getURI()) != null ? servletConfig.getServletContext().getInitParameter(AC.stylesheet.getURI()) : null),
            servletConfig.getServletContext().getInitParameter(AC.cacheStylesheet.getURI()) != null ? Boolean.parseBoolean(servletConfig.getServletContext().getInitParameter(AC.cacheStylesheet.getURI())) : false,
            servletConfig.getServletContext().getInitParameter(AC.resolvingUncached.getURI()) != null ? Boolean.parseBoolean(servletConfig.getServletContext().getInitParameter(AC.resolvingUncached.getURI())) : true,
            servletConfig.getServletContext().getInitParameter(LDHC.clientKeyStore.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.clientKeyStore.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.clientKeyStorePassword.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.clientKeyStorePassword.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.secretaryCertAlias.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.secretaryCertAlias.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.clientTrustStore.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.clientTrustStore.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.clientTrustStorePassword.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.clientTrustStorePassword.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.authQuery.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.authQuery.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.ownerAuthQuery.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.ownerAuthQuery.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.webIDQuery.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.webIDQuery.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.agentQuery.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.agentQuery.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.userAccountQuery.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.userAccountQuery.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.ontologyQuery.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.ontologyQuery.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.baseUri.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.baseUri.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.proxyScheme.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.proxyScheme.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.proxyHost.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.proxyHost.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.proxyPort.getURI()) != null ? Integer.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.proxyPort.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.uploadRoot.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.uploadRoot.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.invalidateCache.getURI()) != null ? Boolean.parseBoolean(servletConfig.getServletContext().getInitParameter(LDHC.invalidateCache.getURI())) : false,
            servletConfig.getServletContext().getInitParameter(LDHC.cookieMaxAge.getURI()) != null ? Integer.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.cookieMaxAge.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.authCacheControl.getURI()) != null ? CacheControl.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.authCacheControl.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.maxContentLength.getURI()) != null ? Integer.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.maxContentLength.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.maxConnPerRoute.getURI()) != null ? Integer.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.maxConnPerRoute.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.maxTotalConn.getURI()) != null ? Integer.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.maxTotalConn.getURI())) : null,
            // TO-DO: respect "timeout" header param in the ConnectionKeepAliveStrategy?
            servletConfig.getServletContext().getInitParameter(LDHC.importKeepAlive.getURI()) != null ? (HttpResponse response, HttpContext context) -> Integer.valueOf(servletConfig.getServletContext().getInitParameter(LDHC.importKeepAlive.getURI())) : null,
            servletConfig.getServletContext().getInitParameter(LDHC.notificationAddress.getURI()) != null ? servletConfig.getServletContext().getInitParameter(LDHC.notificationAddress.getURI()) : null,
            servletConfig.getServletContext().getInitParameter("mail.user") != null ? servletConfig.getServletContext().getInitParameter("mail.user") : null,
            servletConfig.getServletContext().getInitParameter("mail.password") != null ? servletConfig.getServletContext().getInitParameter("mail.password") : null,
            servletConfig.getServletContext().getInitParameter("mail.smtp.host") != null ? servletConfig.getServletContext().getInitParameter("mail.smtp.host") : null,
            servletConfig.getServletContext().getInitParameter("mail.smtp.port") != null ? servletConfig.getServletContext().getInitParameter("mail.smtp.port") : null,
            servletConfig.getServletContext().getInitParameter(Google.clientID.getURI()) != null ? servletConfig.getServletContext().getInitParameter(Google.clientID.getURI()) : null,
            servletConfig.getServletContext().getInitParameter(Google.clientSecret.getURI()) != null ? servletConfig.getServletContext().getInitParameter(Google.clientSecret.getURI()) : null
        );

        URI contextDatasetURI = servletConfig.getServletContext().getInitParameter(LDHC.contextDataset.getURI()) != null ? new URI(servletConfig.getServletContext().getInitParameter(LDHC.contextDataset.getURI())) : null;
        if (contextDatasetURI == null)
        {
            if (log.isErrorEnabled()) log.error("Context dataset URI '{}' not configured", LDHC.contextDataset.getURI());
            throw new ConfigurationException(LDHC.contextDataset);
        }
        this.contextDataset = getDataset(servletConfig.getServletContext(), contextDatasetURI);
    }
    
    public Application(final ServletConfig servletConfig, final MediaTypes mediaTypes,
            final Integer maxGetRequestSize, final boolean cacheModelLoads, final boolean preemptiveAuth, final boolean cacheSitemap,
            final LocationMapper locationMapper, final Source stylesheet, final boolean cacheStylesheet, final boolean resolvingUncached,
            final String clientKeyStoreURIString, final String clientKeyStorePassword,
            final String secretaryCertAlias,
            final String clientTrustStoreURIString, final String clientTrustStorePassword,
            final String authQueryString, final String ownerAuthQueryString, final String webIDQueryString, final String agentQueryString, final String userAccountQueryString, final String ontologyQueryString,
            final String baseURIString, final String proxyScheme, final String proxyHostname, final Integer proxyPort,
            final String uploadRootString, final boolean invalidateCache,
            final Integer cookieMaxAge, final CacheControl authCacheControl, final Integer maxPostSize,
            final Integer maxConnPerRoute, final Integer maxTotalConn, final ConnectionKeepAliveStrategy importKeepAliveStrategy,
            final String notificationAddressString, final String mailUser, final String mailPassword, final String smtpHost, final String smtpPort,
            final String googleClientID, final String googleClientSecret)
    {
        if (clientKeyStoreURIString == null)
        {
            if (log.isErrorEnabled()) log.error("Client key store ({}) not configured", LDHC.clientKeyStore.getURI());
            throw new ConfigurationException(LDHC.clientKeyStore);
        }

        if (secretaryCertAlias == null)
        {
            if (log.isErrorEnabled()) log.error("Secretary client certificate alias ({}) not configured", LDHC.secretaryCertAlias.getURI());
            throw new ConfigurationException(LDHC.secretaryCertAlias);
        }
        
        if (clientTrustStoreURIString == null)
        {
            if (log.isErrorEnabled()) log.error("Client truststore store ({}) not configured", LDHC.clientTrustStore.getURI());
            throw new ConfigurationException(LDHC.clientTrustStore);
        }
        
        if (authQueryString == null)
        {
            if (log.isErrorEnabled()) log.error("Authentication SPARQL query is not configured properly");
            throw new ConfigurationException(LDHC.authQuery);
        }
        this.authQuery = QueryFactory.create(authQueryString);
        
        if (ownerAuthQueryString == null)
        {
            if (log.isErrorEnabled()) log.error("Owner authorization SPARQL query is not configured properly");
            throw new ConfigurationException(LDHC.ownerAuthQuery);
        }
        this.ownerAuthQuery = QueryFactory.create(ownerAuthQueryString);
        
        if (webIDQueryString == null)
        {
            if (log.isErrorEnabled()) log.error("WebID SPARQL query is not configured properly");
            throw new ConfigurationException(LDHC.webIDQuery);
        }
        this.webIDQuery = QueryFactory.create(webIDQueryString);
        
        if (userAccountQueryString == null)
        {
            if (log.isErrorEnabled()) log.error("UserAccount SPARQL query is not configured properly");
            throw new ConfigurationException(LDHC.userAccountQuery);
        }
        this.userAccountQuery = QueryFactory.create(userAccountQueryString);
        
        if (agentQueryString == null)
        {
            if (log.isErrorEnabled()) log.error("Agent SPARQL query is not configured properly");
            throw new ConfigurationException(LDHC.agentQuery);
        }
        this.agentQuery = QueryFactory.create(agentQueryString);
        if (ontologyQueryString == null)
        {
            if (log.isErrorEnabled()) log.error("Ontology SPARQL query is not configured properly");
            throw new ConfigurationException(LDHC.ontologyQuery);
        }
        this.ontologyQuery = QueryFactory.create(ontologyQueryString);
        
        if (baseURIString == null)
        {
            if (log.isErrorEnabled()) log.error("Base URI property '{}' not configured", LDHC.baseUri.getURI());
            throw new ConfigurationException(LDHC.baseUri);
        }
        baseURI = URI.create(baseURIString);
        
        if (uploadRootString == null)
        {
            if (log.isErrorEnabled()) log.error("Upload root ({}) not configured", LDHC.uploadRoot.getURI());
            throw new ConfigurationException(LDHC.uploadRoot);
        }
        
        if (cookieMaxAge == null)
        {
            if (log.isErrorEnabled()) log.error("JWT cookie max age property '{}' not configured", LDHC.cookieMaxAge.getURI());
            throw new ConfigurationException(LDHC.cookieMaxAge);
        }
        this.cookieMaxAge = cookieMaxAge;

        this.mediaTypes = mediaTypes;
        this.maxGetRequestSize = maxGetRequestSize;
        this.preemptiveAuth = preemptiveAuth;
        this.stylesheet = stylesheet;
        this.cacheStylesheet = cacheStylesheet;
        this.resolvingUncached = resolvingUncached;
        this.maxContentLength = maxPostSize;
        this.invalidateCache = invalidateCache;
        this.authCacheControl = authCacheControl;
        this.property(Google.clientID.getURI(), googleClientID);
        this.property(Google.clientSecret.getURI(), googleClientSecret);
        
        try
        {
            this.uploadRoot = new URI(uploadRootString);
        }
        catch (URISyntaxException ex)
        {
            if (log.isErrorEnabled()) log.error("Upload root URI syntax error: {}", ex);
            throw new IllegalStateException(ex);
        }

        if (notificationAddressString != null)
        {
            try
            {
                InternetAddress[] notificationAddresses = InternetAddress.parse(notificationAddressString);
                // if (notificationAddresses.size() == 0) throw Exception...
                notificationAddress = notificationAddresses[0];
            }
            catch (AddressException ex)
            {
                throw new IllegalStateException(ex);
            }
        }
        else notificationAddress = null;

        // add RDF/POST reader
        RDFLanguages.register(RDFLanguages.RDFPOST);
        RDFParserRegistry.registerLangTriples(RDFLanguages.RDFPOST, new RDFPostReaderFactory());

        // add HTML/JSON-LD reader
        DocumentLoader documentLoader = new DocumentLoader();
        JsonLdOptions jsonLdOptions = new JsonLdOptions();
        try (InputStream contextStream = servletConfig.getServletContext().getResourceAsStream("/WEB-INF/classes/com/atomgraph/linkeddatahub/schema.org.jsonldcontext.json"))
        {
            String jsonContext = new String(contextStream.readAllBytes(), StandardCharsets.UTF_8);
            documentLoader.addInjectedDoc("http://schema.org", jsonContext);
            documentLoader.addInjectedDoc("https://schema.org", jsonContext);
            jsonLdOptions.setDocumentLoader(documentLoader);

            ParserProfile profile = RiotLib.profile(HtmlJsonLDReaderFactory.HTML, null, ErrorHandlerFactory.getDefaultErrorHandler());
            RDFLanguages.register(HtmlJsonLDReaderFactory.HTML);
            RDFParserRegistry.registerLangTriples(HtmlJsonLDReaderFactory.HTML,
                new HtmlJsonLDReaderFactory(new JsonLDReader(Lang.JSONLD, profile, profile.getErrorHandler()), jsonLdOptions));
        }
        catch (IOException ex)
        {
            if (log.isErrorEnabled()) log.error("schema.org @context not found", ex);
        }

        // register plain RDF/XML writer as default
        RDFWriterRegistry.register(Lang.RDFXML, RDFFormat.RDFXML_PLAIN); 

        // initialize mapping for locally stored vocabularies
        LocationMapper.setGlobalLocationMapper(locationMapper);
        if (log.isTraceEnabled()) log.trace("LocationMapper.get(): {}", locationMapper);
        
        try
        {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(new java.io.File(new URI(clientKeyStoreURIString))), clientKeyStorePassword.toCharArray());

            trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream(new java.io.File(new URI(clientTrustStoreURIString))), clientTrustStorePassword.toCharArray());
            
            client = getClient(keyStore, clientKeyStorePassword, trustStore, maxConnPerRoute, maxTotalConn, null);
            importClient = getClient(keyStore, clientKeyStorePassword, trustStore, maxConnPerRoute, maxTotalConn, importKeepAliveStrategy);
            noCertClient = getNoCertClient(trustStore, maxConnPerRoute, maxTotalConn);
            
            if (proxyHostname != null)
            {
                ClientRequestFilter rewriteFilter = new ClientUriRewriteFilter(baseURI, proxyScheme, proxyHostname, proxyPort); // proxyPort can be null
                
                client.register(rewriteFilter);
                importClient.register(rewriteFilter);
                noCertClient.register(rewriteFilter);
            }
            
            Certificate secretaryCert = keyStore.getCertificate(secretaryCertAlias);
            if (secretaryCert == null)
            {
                if (log.isErrorEnabled()) log.error("Secretary certificate with alias {} does not exist in client keystore {}", secretaryCertAlias, clientKeyStoreURIString);
                throw new IllegalStateException(new CertificateException("Secretary certificate with alias '" + secretaryCertAlias + "' does not exist in client keystore '" + clientKeyStoreURIString + "'"));
            }
            if (!(secretaryCert instanceof X509Certificate))
            {
                if (log.isErrorEnabled()) log.error("Secretary certificate with alias {} is not a X509Certificate", secretaryCertAlias);
                throw new IllegalStateException(new CertificateException("Secretary certificate with alias " + secretaryCertAlias + " is not a X509Certificate"));
            }
            X509Certificate secretaryX509Cert = (X509Certificate)secretaryCert;
            secretaryX509Cert.checkValidity();// check if secretary WebID client certificate is valid
            secretaryWebIDURI = WebIDFilter.getWebIDURI(secretaryX509Cert);
            if (secretaryWebIDURI == null)
            {
                if (log.isErrorEnabled()) log.error("Secretary certificate with alias {} is not a valid WebID sertificate (SNA URI is missing)", secretaryCertAlias);
                throw new IllegalStateException(new CertificateException("Secretary certificate with alias " + secretaryCertAlias + " not a valid WebID sertificate (SNA URI is missing)"));
            }
            
            SP.init(BuiltinPersonalities.model);
            BuiltinPersonalities.model.add(Agent.class, AgentImpl.factory);
            BuiltinPersonalities.model.add(UserAccount.class, UserAccountImpl.factory);
            BuiltinPersonalities.model.add(AdminApplication.class, new com.atomgraph.linkeddatahub.apps.model.admin.impl.ApplicationImplementation());
            BuiltinPersonalities.model.add(EndUserApplication.class, new com.atomgraph.linkeddatahub.apps.model.end_user.impl.ApplicationImplementation());
            BuiltinPersonalities.model.add(com.atomgraph.linkeddatahub.apps.model.Application.class, new com.atomgraph.linkeddatahub.apps.model.impl.ApplicationImplementation());
            BuiltinPersonalities.model.add(com.atomgraph.linkeddatahub.apps.model.Dataset.class, new com.atomgraph.linkeddatahub.apps.model.impl.DatasetImplementation());
            BuiltinPersonalities.model.add(Service.class, new com.atomgraph.linkeddatahub.model.generic.ServiceImplementation(noCertClient, mediaTypes, maxGetRequestSize));
//            BuiltinPersonalities.model.add(com.atomgraph.linkeddatahub.model.DydraService.class, new com.atomgraph.linkeddatahub.model.dydra.impl.ServiceImplementation(noCertClient, mediaTypes, maxGetRequestSize));
            BuiltinPersonalities.model.add(Import.class, ImportImpl.factory);
            BuiltinPersonalities.model.add(RDFImport.class, RDFImportImpl.factory);
            BuiltinPersonalities.model.add(CSVImport.class, CSVImportImpl.factory);
            BuiltinPersonalities.model.add(File.class, FileImpl.factory);
        
            // TO-DO: config property for cacheModelLoads
            dataManager = new DataManagerImpl(locationMapper, new HashMap<>(), client, mediaTypes, cacheModelLoads, preemptiveAuth, resolvingUncached);
            ontModelSpec = OntModelSpec.OWL_MEM_RDFS_INF;
            ontModelSpec.setImportModelGetter(dataManager);
            OntDocumentManager.getInstance().setFileManager((FileManager)dataManager);
            OntDocumentManager.getInstance().setCacheModels(cacheSitemap); // need to re-set after changing FileManager
            ontModelSpec.setDocumentManager(OntDocumentManager.getInstance());

            if (mailUser != null && mailPassword !=  null) // enable SMTP authentication
            {
                emailProperties.put("mail.smtp.auth", "true");
                emailProperties.put("mail.smtp.starttls.enable", "true"); // connect via TLS https://support.google.com/a/answer/2956491?hl=en
                authenticator = new Authenticator() 
                {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        return new PasswordAuthentication(mailUser, mailPassword);
                    }
                };
            }
            else authenticator = null;

            if (smtpHost == null) throw new IllegalStateException(new IllegalStateException("Cannot initialize email service: SMTP host not configured"));
            if (smtpPort == null) throw new IllegalStateException(new IllegalStateException("Cannot initialize email service: SMTP port not configured"));
            emailProperties.put("mail.smtp.host", smtpHost);
            emailProperties.put("mail.smtp.port", Integer.valueOf(smtpPort));
            
            try
            {
                this.messageDigest = MessageDigest.getInstance("SHA1");
            }
            catch (NoSuchAlgorithmException ex)
            {
                if (log.isErrorEnabled()) log.error("SHA1 algorithm not found", ex);
                throw new InternalServerErrorException(ex);
            }

            xsltProc.registerExtensionFunction(new UUID());
//            xsltProc.registerExtensionFunction(new Construct(xsltProc));
//            xsltProc.registerExtensionFunction(new ConstructForClass(xsltProc));
            xsltProc.registerExtensionFunction(new com.atomgraph.linkeddatahub.writer.function.Construct(xsltProc));
            
            Model mappingModel = locationMapper.toModel();
            ResIterator prefixedMappings = mappingModel.listResourcesWithProperty(LocationMappingVocab.prefix);
            try
            {
                while (prefixedMappings.hasNext())
                {
                    Resource prefixMapping = prefixedMappings.next();
                    String prefix = prefixMapping.getRequiredProperty(LocationMappingVocab.prefix).getString();
                    // register mapped RDF documents in the XSLT processor so that document() returns them cached, throughout multiple transformations
                    TreeInfo doc = xsltProc.getUnderlyingConfiguration().buildDocumentTree(dataManager.resolve("", prefix));
                    xsltProc.getUnderlyingConfiguration().getGlobalDocumentPool().add(doc, prefix);
                }
                
                // register HTTPS URL of translations.rdf so it doesn't have to be requested repeatedly
                try (InputStream translations = servletConfig.getServletContext().getResourceAsStream(ModelXSLTWriterBase.TRANSLATIONS_PATH))
                {
                    TreeInfo doc = xsltProc.getUnderlyingConfiguration().buildDocumentTree(new StreamSource(translations));
                    xsltProc.getUnderlyingConfiguration().getGlobalDocumentPool().add(doc, baseURI.resolve(ModelXSLTWriterBase.TRANSLATIONS_PATH).toString());
                }
            }
            catch (XPathException | TransformerException ex)
            {
                if (log.isErrorEnabled()) log.error("Error reading mapped RDF document: {}", ex);
                throw new IllegalStateException(ex);
            }
            finally
            {
                prefixedMappings.close();
            }
            
            xsltComp = xsltProc.newXsltCompiler();
            xsltComp.setParameter(new QName("ldh", LDH.base.getNameSpace(), LDH.base.getLocalName()), new XdmAtomicValue(baseURI));
            xsltComp.setURIResolver(new XsltResolver(LocationMapper.get(), new HashMap<>(), client, mediaTypes, false, false, true)); // default Xerces parser does not support HTTPS
            xsltExec = xsltComp.compile(stylesheet);
        }
        catch (FileNotFoundException ex)
        {
            if (log.isErrorEnabled()) log.error("File not found", ex);
            throw new IllegalStateException(ex);
        }
        catch (IOException ex)
        {
            if (log.isErrorEnabled()) log.error("Could not load file", ex);
            throw new IllegalStateException(ex);
        }
        catch (KeyStoreException ex)
        {
            if (log.isErrorEnabled()) log.error("Key store error", ex);
            throw new IllegalStateException(ex);
        }
        catch (NoSuchAlgorithmException ex)
        {
            if (log.isErrorEnabled()) log.error("No such algorithm", ex);
            throw new IllegalStateException(ex);
        }
        catch (CertificateException ex)
        {
            if (log.isErrorEnabled()) log.error("Certificate error", ex);
            throw new IllegalStateException(ex);
        }
        catch (KeyManagementException | UnrecoverableKeyException ex)
        {
            if (log.isErrorEnabled()) log.error("Key management error", ex);
            throw new IllegalStateException(ex);
        }
        catch (URISyntaxException ex)
        {
            if (log.isErrorEnabled()) log.error("URI syntax error", ex);
            throw new IllegalStateException(ex);
        }
        catch (SaxonApiException ex)
        {
            if (log.isErrorEnabled()) log.error("System XSLT stylesheet error", ex);
            throw new IllegalStateException(ex);
        }
    }
    
    @PostConstruct
    public void init()
    {
        register(MultiPartFeature.class);

        registerResourceClasses();
        registerContainerRequestFilters();
        registerContainerResponseFilters();
        registerExceptionMappers();
        
        eventBus.register(this); // this system application will be receiving events about context changes
        
        register(new ValidatingModelProvider(getMessageDigest()));
        register(new ResultSetProvider());
        register(new QueryParamProvider());
        register(new UpdateRequestProvider());
        register(new ModelXSLTWriter(getXsltExecutable(), getOntModelSpec(), getDataManager(), getMessageDigest())); // writes (X)HTML responses

        final com.atomgraph.linkeddatahub.Application system = this;
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(system).to(com.atomgraph.linkeddatahub.Application.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bind(new com.atomgraph.client.MediaTypes()).to(com.atomgraph.client.MediaTypes.class).to(com.atomgraph.core.MediaTypes.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(AgentContextFactory.class).to(new TypeLiteral<Optional<AgentContext>>() {}).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(ServiceFactory.class).to(new TypeLiteral<Optional<Service>>() {}).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(ApplicationFactory.class).to(com.atomgraph.linkeddatahub.apps.model.Application.class).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(com.atomgraph.linkeddatahub.server.factory.DatasetFactory.class).to(new TypeLiteral<Optional<com.atomgraph.linkeddatahub.apps.model.Dataset>>() {}).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(OntologyFactory.class).to(new TypeLiteral<Optional<Ontology>>() {}).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(new com.atomgraph.core.factory.DataManagerFactory(getDataManager())).to(com.atomgraph.core.util.jena.DataManager.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(DataManagerFactory.class).to(com.atomgraph.client.util.DataManager.class).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(XsltExecutableSupplierFactory.class).to(XsltExecutableSupplier.class).
                in(RequestScoped.class);
            }
        });
        register(new AbstractBinder()
        {
            @Override
            protected void configure()
            {
                bindFactory(ModeFactory.class).to(new TypeLiteral<List<Mode>>() {}).
                in(RequestScoped.class);
            }
        });
        
//        if (log.isTraceEnabled()) log.trace("Application.init() with Classes: {} and Singletons: {}", getClasses(), getSingletons());
    }
    
    protected void registerResourceClasses()
    {
        register(Dispatcher.class);
    }
    
    protected void registerContainerRequestFilters()
    {
        register(new HttpMethodOverrideFilter());
        register(ApplicationFilter.class);
        register(OntologyFilter.class);
        register(ProxiedWebIDFilter.class);
        register(IDTokenFilter.class);
        register(AuthorizationFilter.class);
        register(ContentLengthLimitFilter.class);
        register(new RDFPostCleanupInterceptor()); // for application/x-www-form-urlencoded
        register(new RDFPostCleanupFilter()); // for multipart/form-data
    }

    protected void registerContainerResponseFilters()
    {
        register(new ResponseHeaderFilter());
        register(new XsltExecutableFilter());
        if (isInvalidateCache()) register(new BackendInvalidationFilter());
//        register(new ProvenanceFilter());
    }
    
    protected void registerExceptionMappers()
    {
        register(NotFoundExceptionMapper.class);
        register(ConfigurationExceptionMapper.class);
        register(OntologyExceptionMapper.class);
        register(ModelExceptionMapper.class);
        register(SPINConstraintViolationExceptionMapper.class);
        register(SHACLConstraintViolationExceptionMapper.class);
        register(DatatypeFormatExceptionMapper.class);
        register(ParameterExceptionMapper.class);
        register(QueryExecExceptionMapper.class);
        register(RiotExceptionMapper.class);
        register(RiotParseExceptionMapper.class); // move to Processor?
        register(ClientErrorExceptionMapper.class);
        register(HttpHostConnectExceptionMapper.class);
        register(BadGatewayExceptionMapper.class);
        register(OntClassNotFoundExceptionMapper.class);
        register(InvalidWebIDPublicKeyExceptionMapper.class);
        register(InvalidWebIDURIExceptionMapper.class);
        register(WebIDCertificateExceptionMapper.class);
        register(WebIDDelegationExceptionMapper.class);
        register(WebIDLoadingExceptionMapper.class);
        register(TokenExpiredExceptionMapper.class);
        register(ResourceExistsExceptionMapper.class);
        register(QueryParseExceptionMapper.class);
        register(AuthenticationExceptionMapper.class);
        register(AuthorizationExceptionMapper.class);
        register(MessagingExceptionMapper.class);
    }
    
    public static Dataset getDataset(final ServletContext servletContext, final URI uri) throws FileNotFoundException, MalformedURLException, IOException
    {
        String baseURI = servletContext.getResource("/").toString();

        try (InputStream datasetStream = (uri.isAbsolute() ? new FileInputStream(new java.io.File(uri)) : servletContext.getResourceAsStream(uri.toString())))
        {
            if (datasetStream == null) throw new IOException("Dataset not found at URI: " + uri.toString());
            Lang lang = RDFDataMgr.determineLang(uri.toString(), null, null);
            if (lang == null) throw new IOException("Could not determing RDF format from dataset URI: " + uri.toString());

            Dataset dataset = DatasetFactory.create();
            if (log.isDebugEnabled()) log.debug("Loading Model from dataset: {}", uri);
            RDFDataMgr.read(dataset, datasetStream, baseURI, lang);
            return dataset;
        }
    }

    public final Model getModel(Dataset dataset, Query query)
    {
        if (dataset == null) throw new IllegalArgumentException("Dataset cannot be null");
        if (query == null) throw new IllegalArgumentException("Query cannot be null");
        
        try (QueryExecution qex = QueryExecution.create(query, dataset))
        {
            if (query.isDescribeType()) return qex.execDescribe();
            if (query.isConstructType()) return qex.execConstruct();
            
            throw new IllegalStateException("Query is not DESCRIBE or CONSTRUCT");
        }
    }

    @Subscribe
    public void handleSignUp(SignUp event)
    {
        getWebIDModelCache().remove(event.getSecretaryWebID()); // clear secretary WebID from cache to get new acl:delegates statements after new signup
    }

    public Resource matchApp(Resource type, URI absolutePath)
    {
        return matchApp(getContextModel(), type, absolutePath); // make sure we return an immutable model
    }
    
    public Resource matchApp(Model appModel, Resource type, URI absolutePath)
    {
        return getLongestURIResource(getLengthMap(getRelativeBaseApps(appModel, type, absolutePath)));
    }
    
    public Resource getLongestURIResource(Map<Integer, Resource> lengthMap)
    {
        // select the app with the longest URI match, as the model contains a pair of EndUserApplication/AdminApplication
        TreeMap<Integer, Resource> apps = new TreeMap(lengthMap);
        if (!apps.isEmpty()) return apps.lastEntry().getValue();
        
        return null;
    }
    
    public Map<URI, Resource> getRelativeBaseApps(Model model, Resource type, URI absolutePath)
    {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        if (type == null) throw new IllegalArgumentException("Resource cannot be null");
        if (absolutePath == null) throw new IllegalArgumentException("URI cannot be null");

        Map<URI, Resource> apps = new HashMap<>();
        
        ResIterator it = model.listSubjectsWithProperty(RDF.type, type);
        try
        {
            while (it.hasNext())
            {
                Resource app = it.next();
                
                if (!app.hasProperty(LDT.base))
                    throw new InternalServerErrorException(new IllegalStateException("Application resource <" + app.getURI() + "> has no ldt:base value"));
                
                URI base = URI.create(app.getPropertyResourceValue(LDT.base).getURI());
                URI relative = base.relativize(absolutePath);
                if (!relative.isAbsolute()) apps.put(base, app);
            }
        }
        finally
        {
            it.close();
        }

        return apps;
    }
    
    public Resource matchDataset(Resource type, URI absolutePath)
    {
        return matchDataset(getContextModel(), type, absolutePath); // make sure we return an immutable model
    }
    
    public Resource matchDataset(Model appModel, Resource type, URI absolutePath)
    {
        return getLongestURIResource(getLengthMap(getRelativeDatasets(appModel, type, absolutePath)));
    }
    
    public Map<URI, Resource> getRelativeDatasets(Model model, Resource type, URI absolutePath)
    {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        if (type == null) throw new IllegalArgumentException("Resource cannot be null");
        if (absolutePath == null) throw new IllegalArgumentException("URI cannot be null");

        Map<URI, Resource> datasets = new HashMap<>();
        
        ResIterator it = model.listSubjectsWithProperty(RDF.type, type);
        try
        {
            while (it.hasNext())
            {
                Resource dataset = it.next();
                
                if (!dataset.hasProperty(LAPP.prefix))
                    throw new InternalServerErrorException(new IllegalStateException("Dataset resource <" + dataset.getURI() + "> has no lapp:prefix value"));
                
                URI prefix = URI.create(dataset.getPropertyResourceValue(LAPP.prefix).getURI());
                URI relative = prefix.relativize(absolutePath);
                if (!relative.isAbsolute() && !relative.toString().equals("")) datasets.put(prefix, dataset);
            }
        }
        finally
        {
            it.close();
        }

        return datasets;
    }
    
    public Map<Integer, Resource> getLengthMap(Map<URI, Resource> apps)
    {
        if (apps == null) throw new IllegalArgumentException("Map cannot be null");

        Map<Integer, Resource> lengthMap = new HashMap<>();
        
        Iterator<Map.Entry<URI, Resource>> it = apps.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<URI, Resource> entry = it.next();
            lengthMap.put(entry.getKey().toString().length(), entry.getValue());
        }
        
        return lengthMap;
    }

    public void submitImport(CSVImport csvImport, Service service, Service adminService, String baseURI, DataManager dataManager)
    {
        ImportListener.submit(csvImport, service, adminService, baseURI, dataManager);
    }
    
    public void submitImport(RDFImport rdfImport, Service service, Service adminService, String baseURI, DataManager dataManager)
    {
        ImportListener.submit(rdfImport, service, adminService, baseURI, dataManager);
    }
    
    public static Client getClient(KeyStore keyStore, String keyStorePassword, KeyStore trustStore, Integer maxConnPerRoute, Integer maxTotalConn, ConnectionKeepAliveStrategy keepAliveStrategy) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException
    {
        if (keyStore == null) throw new IllegalArgumentException("KeyStore cannot be null");
        if (keyStorePassword == null) throw new IllegalArgumentException("KeyStore password string cannot be null");
        if (trustStore == null) throw new IllegalArgumentException("KeyStore (truststore) cannot be null");

        // for client authentication
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword.toCharArray());

        // for trusting server certificate
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().
            register("https", new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE)).
            register("http", new PlainConnectionSocketFactory()).
            build();

        // https://github.com/eclipse-ee4j/jersey/issues/4449
        PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager(socketFactoryRegistry)
        {

            @Override
            public void close()
            {
                super.shutdown();
            }

            @Override
            public void shutdown()
            {
                // Disable shutdown of the pool. This will be done later, when this factory is closed
                // This is a workaround for finalize method on jerseys ClientRuntime which
                // closes the client and shuts down the connection pool when it is garbage collected
            };

        };
        if (maxConnPerRoute != null) conman.setDefaultMaxPerRoute(maxConnPerRoute);
        if (maxTotalConn != null) conman.setMaxTotal(maxTotalConn);
//        if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));

        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.register(MultiPartFeature.class);
        config.register(new ModelProvider());
        config.register(new DatasetProvider());
        config.register(new ResultSetProvider());
        config.register(new QueryProvider());
        config.register(new UpdateRequestProvider());
        config.property(ClientProperties.FOLLOW_REDIRECTS, true);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, conman);
        if (keepAliveStrategy != null) config.property(ApacheClientProperties.KEEPALIVE_STRATEGY, keepAliveStrategy);

        return ClientBuilder.newBuilder().
            withConfig(config).
            sslContext(ctx).
            hostnameVerifier(NoopHostnameVerifier.INSTANCE). // has no effect due to the custom SSLContext
            build();
    }
    
    public static Client getNoCertClient(KeyStore trustStore, Integer maxConnPerRoute, Integer maxTotalConn)
    {
        try
        {
            // for trusting server certificate
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, tmf.getTrustManagers(), null);

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().
                register("https", new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE)).
                register("http", new PlainConnectionSocketFactory()).
                build();
        
            // https://github.com/eclipse-ee4j/jersey/issues/4449
            PoolingHttpClientConnectionManager conman = new PoolingHttpClientConnectionManager(socketFactoryRegistry)
            {

                @Override
                public void close()
                {
                    super.shutdown();
                }

                @Override
                public void shutdown()
                {
                    // Disable shutdown of the pool. This will be done later, when this factory is closed
                    // This is a workaround for finalize method on jerseys ClientRuntime which
                    // closes the client and shuts down the connection pool when it is garbage collected
                };
                
            };
            if (maxConnPerRoute != null) conman.setDefaultMaxPerRoute(maxConnPerRoute);
            if (maxTotalConn != null) conman.setMaxTotal(maxTotalConn);

            ClientConfig config = new ClientConfig();
            config.connectorProvider(new ApacheConnectorProvider());
            config.register(MultiPartFeature.class);
            config.register(new ModelProvider());
            config.register(new DatasetProvider());
            config.register(new ResultSetProvider());
            config.register(new QueryProvider());
            config.register(new UpdateRequestProvider()); // TO-DO: UpdateRequestProvider
            config.property(ClientProperties.FOLLOW_REDIRECTS, true);
            config.property(ApacheClientProperties.CONNECTION_MANAGER, conman);

            return ClientBuilder.newBuilder().
                withConfig(config).
                sslContext(ctx).
                hostnameVerifier(NoopHostnameVerifier.INSTANCE). // has no effect due to the custom SSLContext
                build();
        }
        catch (NoSuchAlgorithmException ex)
        {
            if ( log.isErrorEnabled()) log.error("No such algorithm: {}", ex);
            throw new IllegalStateException(ex);
        }
        catch (KeyStoreException ex)
        {
            if ( log.isErrorEnabled()) log.error("Key store error: {}", ex);
            throw new IllegalStateException(ex);
        }
        catch (KeyManagementException ex)
        {
            if ( log.isErrorEnabled()) log.error("Key management error: {}", ex);
            throw new IllegalStateException(ex);
        }
        
        //if (log.isDebugEnabled()) client.addFilter(new LoggingFilter(System.out));
    }
    
    public EventBus getEventBus()
    {
        return eventBus;
    }
    
    public DataManager getDataManager()
    {
        return dataManager;
    }
    
    public MediaTypes getMediaTypes()
    {
        return mediaTypes;
    }
    
    public Client getClient()
    {
        return client;
    }
    
    public URI getBaseURI()
    {
        return baseURI;
    }
    
    public URI getSecretaryWebIDURI()
    {
        return secretaryWebIDURI;
    }
    
    public Query getAuthQuery()
    {
        return authQuery;
    }
    
    public Query getOwnerAuthQuery()
    {
        return ownerAuthQuery;
    }
    
    public Query getWebIDQuery()
    {
        return webIDQuery;
    }
    
    public Query getAgentQuery()
    {
        return agentQuery;
    }
    
    public Query getUserAccountQuery()
    {
        return userAccountQuery;
    }
    
    public Query getOntologyQuery()
    {
        return ontologyQuery;
    }
    
    public Integer getMaxGetRequestSize()
    {
        return maxGetRequestSize;
    }
    
    public boolean isPreemptiveAuth()
    {
        return preemptiveAuth;
    }
    
    public OntModelSpec getOntModelSpec()
    {
        return ontModelSpec;
    }
    
    public XsltCompiler getXsltCompiler()
    {
        return xsltComp;
    }
    
    public XsltExecutable getXsltExecutable()
    {
        return xsltExec;
    }
    
    public Source getStylesheet()
    {
        return stylesheet;
    }

    public boolean isCacheStylesheet()
    {
        return cacheStylesheet;
    }

    public boolean isResolvingUncached()
    {
        return resolvingUncached;
    }
    
    public URI getUploadRoot()
    {
        return uploadRoot;
    }
    
    protected Dataset getContextDataset()
    {
        return contextDataset;
    }

    public Model getContextModel()
    {
        return ModelFactory.createModelForGraph(new GraphReadOnly(getContextDataset().getDefaultModel().getGraph()));
    }

    public boolean isInvalidateCache()
    {
        return invalidateCache;
    }

    public Integer getCookieMaxAge()
    {
        return cookieMaxAge;
    }

    public Integer getMaxContentLength()
    {
        return maxContentLength;
    }
    
    public CacheControl getAuthCacheControl()
    {
        return authCacheControl;
    }
    
    public KeyStore getKeyStore()
    {
        return keyStore;
    }
    
    public KeyStore getTrustStore()
    {
        return trustStore;
    }

    public Client getImportClient()
    {
        return importClient;
    }

    public Client getNoCertClient()
    {
        return noCertClient;
    }
    
    public Address getNotificationAddress()
    {
        return notificationAddress;
    }
    
    public final MessageBuilder getMessageBuilder()
    {
        if (authenticator != null) return MessageBuilder.fromPropertiesAndAuth(emailProperties, authenticator);
        else return MessageBuilder.fromProperties(emailProperties);
    }
    
    public ExpiringMap<URI, Model> getWebIDModelCache()
    {
        return webIDmodelCache;
    }
    
    public ExpiringMap<String, Model> getOIDCModelCache()
    {
        return oidcModelCache;
    }
    
    public Map<URI, XsltExecutable> getXsltExecutableCache()
    {
        return xsltExecutableCache;
    }
    
    public MessageDigest getMessageDigest()
    {
        return messageDigest;
    }
    
}