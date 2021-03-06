/**
 * Copyright 2013 Crypto Workshop Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cryptoworkshop.ximix.console.config;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 */
public class HTTPConfig
{
    private int port = 1887;
    private String host = "localhost";

    public HTTPConfig(Node n)
    {
        NodeList nl = n.getChildNodes();
        for (int t = 0; t < nl.getLength(); t++)
        {
            Node node = nl.item(t);
            if ("bind-port".equals(node.getNodeName()))
            {
                this.port = Integer.valueOf(node.getTextContent().trim());
            }

            if ("bind-host".equals(node.getNodeName()))
            {
                host = node.getTextContent();
            }
        }
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }
}
