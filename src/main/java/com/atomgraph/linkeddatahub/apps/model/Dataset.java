/**
 *  Copyright 2022 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.linkeddatahub.apps.model;

import com.atomgraph.linkeddatahub.model.Service;
import java.net.URI;
import org.apache.jena.rdf.model.Resource;

/**
 * A dataspace that returns Linked Data.
 * Can either have a base or a prefix URI. Does not have a service unlike applications.
 * Used for proxying third party Linked Data services.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public interface Dataset extends Resource
{
    
//    Resource getBase();
//    
//    URI getBaseURI();
    
    Resource getPrefix();
    
    Resource getProxy();
    
    URI getProxyURI();
    
    URI getProxied(URI uri);
    
    Service getService();

}
