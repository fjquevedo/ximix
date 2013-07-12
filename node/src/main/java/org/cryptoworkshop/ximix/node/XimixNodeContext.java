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
package org.cryptoworkshop.ximix.node;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.util.io.Streams;
import org.cryptoworkshop.ximix.common.config.Config;
import org.cryptoworkshop.ximix.common.config.ConfigException;
import org.cryptoworkshop.ximix.common.config.ConfigObjectFactory;
import org.cryptoworkshop.ximix.common.message.CapabilityMessage;
import org.cryptoworkshop.ximix.common.service.KeyType;
import org.cryptoworkshop.ximix.common.service.NodeContext;
import org.cryptoworkshop.ximix.common.service.PrivateKeyOperator;
import org.cryptoworkshop.ximix.common.service.PublicKeyOperator;
import org.cryptoworkshop.ximix.common.service.Service;
import org.cryptoworkshop.ximix.common.service.ServicesConnection;
import org.cryptoworkshop.ximix.common.service.ThresholdKeyPairGenerator;
import org.cryptoworkshop.ximix.crypto.key.ECKeyManager;
import org.cryptoworkshop.ximix.crypto.key.ECNewDKGGenerator;
import org.cryptoworkshop.ximix.crypto.key.KeyManager;
import org.cryptoworkshop.ximix.crypto.key.KeyManagerListener;
import org.cryptoworkshop.ximix.crypto.operator.bc.BcECPrivateKeyOperator;
import org.cryptoworkshop.ximix.crypto.operator.bc.BcECPublicKeyOperator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XimixNodeContext
    implements NodeContext
{
    private Map<String, ServicesConnection> peerMap;
    private final ECKeyManager keyManager;

    private ExecutorService connectionExecutor = Executors.newCachedThreadPool();   // TODO configurable or linked to threshold
    private ScheduledExecutorService multiTaskExecutor = Executors.newScheduledThreadPool(5);   // TODO configurable or linked to threshold
    private ExecutorService decoupler = Executors.newSingleThreadExecutor();
    private CapabilitiesCache capabilitiesCache = null;


    private List<Service> services = new ArrayList<Service>();
    private final String name;

    public XimixNodeContext(Map<String, ServicesConnection> peerMap, final Config nodeConfig)
        throws ConfigException
    {
        this.peerMap = new HashMap<>(peerMap);

        this.name = nodeConfig.getStringProperty("name");  // TODO:

        this.peerMap.remove(this.name);

        List<ServiceConfig> configs = nodeConfig.getConfigObjects("services", new NodeConfigFactory());
        for (ServiceConfig config : configs)
        {
            if (config.getThrowable() != null)
            {
                config.getThrowable().printStackTrace();   // TODO: log!
            }
        }

        keyManager = new ECKeyManager(this);

        if (nodeConfig.getHomeDirectory() != null)
        {
            setupKeyManager(nodeConfig.getHomeDirectory(), keyManager);
        }


        capabilitiesCache = new CapabilitiesCache(this);

    }

    public String getName()
    {
        return name;
    }

    public CapabilityMessage[] getCapabilities()
    {
        List<CapabilityMessage> capabilityList = new ArrayList<CapabilityMessage>();

        for (Service service : services)
        {
            capabilityList.add(service.getCapability());
        }

        return capabilityList.toArray(new CapabilityMessage[capabilityList.size()]);
    }

    public void addConnection(XimixServices task)
    {
        connectionExecutor.execute(task);
    }

    public Map<String, ServicesConnection> getPeerMap()
    {
        return peerMap;
    }

    public void execute(Runnable task)
    {
        multiTaskExecutor.execute(task);
    }

    @Override
    public void schedule(Runnable task, long time, TimeUnit timeUnit)
    {
        multiTaskExecutor.schedule(task, time, timeUnit);
    }

    @Override
    public Executor getDecoupler()
    {
        return decoupler;
    }

    public SubjectPublicKeyInfo getPublicKey(String keyID)
    {
        try
        {
            return keyManager.fetchPublicKey(keyID);
        }
        catch (IOException e)
        {
            e.printStackTrace();  // TODO:
            return null;
        }
    }

    @Override
    public <T> T getDomainParameters(String keyID)
    {
        X9ECParameters params = SECNamedCurves.getByName("secp256r1");
        return (T)new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH(), params.getSeed());
    }

    @Override
    public boolean hasPrivateKey(String keyID)
    {
        return keyManager.hasPrivateKey(keyID);
    }

    @Override
    public PublicKeyOperator getPublicKeyOperator(String keyID)
    {
        try
        {
            SubjectPublicKeyInfo pubInfo = keyManager.fetchPublicKey(keyID);
            ECPublicKeyParameters keyParameters = (ECPublicKeyParameters)PublicKeyFactory.createKey(pubInfo);

            return new BcECPublicKeyOperator(keyParameters.getParameters());
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;
    }

    @Override
    public PrivateKeyOperator getPrivateKeyOperator(String keyID)
    {
        try
        {
            SubjectPublicKeyInfo pubInfo = keyManager.fetchPublicKey(keyID);
            ECPublicKeyParameters keyParameters = (ECPublicKeyParameters)PublicKeyFactory.createKey(pubInfo);

            return new BcECPrivateKeyOperator(keyParameters.getParameters(), keyManager.getPartialPrivateKey(keyID));
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;

    }

    public Service getService(Enum type)
    {
        for (Service service : services)
        {
            if (service.isAbleToHandle(type))
            {
                return service;
            }
        }

        return null;
    }

    @Override
    public boolean isStopCalled()
    {
        return multiTaskExecutor.isShutdown() || multiTaskExecutor.isTerminated();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor()
    {
        return multiTaskExecutor;
    }

    @Override
    public ThresholdKeyPairGenerator getKeyPairGenerator(KeyType keyType)
    {
        return new ECNewDKGGenerator(keyType, keyManager);
    }

    @Override
    public boolean shutdown(final int time, final TimeUnit timeUnit)
        throws InterruptedException
    {
        List<Runnable> tasks = connectionExecutor.shutdownNow();

        for (Runnable task : tasks)
        {
            XimixServices connection = (XimixServices)task;

            connection.stop();
        }

        multiTaskExecutor.shutdown();

        return multiTaskExecutor.awaitTermination(time, timeUnit);
    }

    /**
     * Reload our previous state and register listener's if required.
     *
     * @param homeDirectory root of the node's config
     * @param keyManager the key manager to be reloaded.
     */
    private void setupKeyManager(final File homeDirectory, KeyManager keyManager)
    {
        final File keyDir = new File(homeDirectory, "keys");
        final File   store = new File(keyDir, keyManager.getID() + ".p12");

        if (store.exists())
        {
            try
            {
                keyManager.load("Hello".toCharArray(), Streams.readAll(new FileInputStream(store)));
            }
            catch (IOException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            catch (GeneralSecurityException e)
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        keyManager.addListener(new KeyManagerListener()
        {
            @Override
            public void keyAdded(KeyManager keyManager, String keyID)
            {
                if (homeDirectory != null)
                {
                    try
                    {
                        byte[] enc = keyManager.getEncoded("Hello".toCharArray());

                        if (!keyDir.exists())
                        {
                            if (!keyDir.mkdir())
                            {
                                System.err.println("Eeeek!");  // TODO
                            }
                        }

                        if (store.exists())
                        {
                            if (!store.renameTo(new File(keyDir, keyManager.getID() + ".p12.bak")))
                            {
                                System.err.println("Eeeek!"); // TODO
                            }
                        }

                        FileOutputStream fOut = new FileOutputStream(store);

                        fOut.write(enc);

                        fOut.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    catch (GeneralSecurityException e)
                    {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            }
        });
    }

    private class ServiceConfig
    {
        private Exception throwable;

        ServiceConfig(Node configNode)
        {
            NodeList xmlNodes = configNode.getChildNodes();

            for (int i = 0; i != xmlNodes.getLength(); i++)
            {
                Node xmlNode = xmlNodes.item(i);

                if (xmlNode.getNodeName().equals("service"))
                {
                    NodeList attributes = xmlNode.getChildNodes();

                    for (int j = 0; j != xmlNodes.getLength(); j++)
                    {
                        Node attrNode = attributes.item(j);

                        if (attrNode == null)
                        {
                            continue;
                        }

                        if (attrNode.getNodeName().equals("implementation"))
                        {
                            try
                            {
                                Class clazz = Class.forName(attrNode.getTextContent());

                                Constructor constructor = clazz.getConstructor(NodeContext.class, Config.class);

                                Service impl = (Service)constructor.newInstance(XimixNodeContext.this, new Config(xmlNode));

                                services.add(impl);
                            }
                            catch (ClassNotFoundException e)
                            {
                                throwable = e;
                            }
                            catch (NoSuchMethodException e)
                            {
                                throwable = e;
                            }
                            catch (InvocationTargetException e)
                            {
                                throwable = e;
                            }
                            catch (InstantiationException e)
                            {
                                throwable = e;
                            }
                            catch (IllegalAccessException e)
                            {
                                throwable = e;
                            }
                            catch (ConfigException e)
                            {
                                throwable = e;
                            }
                        }
                    }
                }
            }
        }

        public Throwable getThrowable()
        {
            return throwable;
        }
    }

    private class NodeConfigFactory
        implements ConfigObjectFactory<ServiceConfig>
    {
        public ServiceConfig createObject(Node configNode)
        {
            return new ServiceConfig(configNode);
        }
    }
}
