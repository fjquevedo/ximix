package org.cryptoworkshop.ximix.test.node;

import org.cryptoworkshop.ximix.common.conf.Config;
import org.cryptoworkshop.ximix.common.conf.ConfigException;
import org.cryptoworkshop.ximix.node.XimixNodeBuilder;

import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 */
public class TestXimixNodeFactory extends XimixNodeBuilder
{
    public TestXimixNodeFactory(Config peersConfig)
    {
        super(peersConfig);
    }

    public TestXimixNodeFactory(File file)
        throws ConfigException, FileNotFoundException
    {
        super(file);
    }


}