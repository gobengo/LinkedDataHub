/**
 *  Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.linkeddatahub.io;

import static com.atomgraph.linkeddatahub.io.JsonLDReader.JSONLD_OPTIONS;
import com.github.jsonldjava.core.JsonLdOptions;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.Lang;
import static org.apache.jena.riot.Lang.JSONLD;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.ReaderRIOTBase;
import org.apache.jena.riot.RiotParseException;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.FileUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

/**
 * JSON-LD-in-HTML reader.
 * Can be used to read schema.org data.
 * 
 * @author {@literal Martynas Jusevičius <martynas@atomgraph.com>}
 */
public class HtmlJsonLDReader extends ReaderRIOTBase
{

    private final JsonLDReader jsonLDReader;
    private final JsonLdOptions options;
    
    public HtmlJsonLDReader(JsonLDReader jsonLDReader)
    {
        this(jsonLDReader, null);
    }
    
    public HtmlJsonLDReader(JsonLDReader jsonLDReader, JsonLdOptions options)
    {
        this.jsonLDReader = jsonLDReader;
        this.options = options;
    }

    @Override
    public void read(InputStream in, String baseURI, Lang lang, StreamRDF output, Context context)
    {
        read(FileUtils.asBufferedUTF8(in), baseURI, lang, output, context);
    }
    
    @Override
    public void read(Reader in, String baseURI, ContentType ct, StreamRDF output, Context context)
    {
        read(in, baseURI, RDFLanguages.contentTypeToLang(ct), output, context);
    }
    
    public void read(Reader in, String baseURI, Lang lang, StreamRDF output, Context context)
    {
        Document html = Parser.htmlParser().parseInput(in, baseURI);
        Elements jsonLdElements = html.selectXpath("/html//script[@type = 'application/ld+json']");

        if (jsonLdElements.isEmpty()) throw new RiotParseException("<script> element with type=\"application/ld+json\" not found",  -1,  -1);

        context.set(JSONLD_OPTIONS, getJsonLdOptions());
        
        // read from all <script type="application/ld+json"> elements
        for (Element element : jsonLdElements)
        {
            String jsonLd = element.data();
            getJsonLDReader().read(new StringReader(jsonLd), baseURI, JSONLD.getContentType(), output, context);
        }
    }

    public JsonLDReader getJsonLDReader()
    {
        return jsonLDReader;
    }
    
    public JsonLdOptions getJsonLdOptions()
    {
        return options;
    }
    
}