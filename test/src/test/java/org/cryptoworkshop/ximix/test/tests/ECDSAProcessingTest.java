package org.cryptoworkshop.ximix.test.tests;

import java.io.IOException;
import java.math.BigInteger;
import java.net.SocketException;

import junit.framework.TestCase;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.cryptoworkshop.ximix.common.handlers.ThrowableHandler;
import org.cryptoworkshop.ximix.crypto.KeyGenerationOptions;
import org.cryptoworkshop.ximix.crypto.KeyType;
import org.cryptoworkshop.ximix.crypto.SignatureGenerationOptions;
import org.cryptoworkshop.ximix.crypto.client.KeyGenerationService;
import org.cryptoworkshop.ximix.crypto.client.SigningService;
import org.cryptoworkshop.ximix.mixnet.client.UploadService;
import org.cryptoworkshop.ximix.node.XimixNode;
import org.cryptoworkshop.ximix.registrar.XimixRegistrar;
import org.cryptoworkshop.ximix.registrar.XimixRegistrarFactory;
import org.cryptoworkshop.ximix.test.node.NodeTestUtil;
import org.cryptoworkshop.ximix.test.node.ResourceAnchor;
import org.cryptoworkshop.ximix.test.node.SquelchingThrowableHandler;
import org.junit.Assert;
import org.junit.Test;

import static org.cryptoworkshop.ximix.test.node.NodeTestUtil.getXimixNode;

/**
 *
 */
public class ECDSAProcessingTest extends TestCase
{



    @Override
    public void setUp()
        throws Exception
    {

    }

    @Override
    public void tearDown()
        throws Exception
    {
        NodeTestUtil.shutdownNodes();
    }

    @Test
    public void testECDSASigning()
        throws Exception
    {

        SquelchingThrowableHandler handler = new SquelchingThrowableHandler();

        //
        // Squelch out socket exceptions emitted by close of connections below.
        //
        handler.squelchType(SocketException.class);



        XimixNode nodeOne = getXimixNode("/conf/mixnet.xml", "/conf/node1.xml", handler);
        NodeTestUtil.launch(nodeOne, true);


        XimixNode nodeTwo = getXimixNode("/conf/mixnet.xml", "/conf/node2.xml", handler);
        NodeTestUtil.launch(nodeTwo, true);


        XimixNode nodeThree = getXimixNode("/conf/mixnet.xml", "/conf/node3.xml", handler);
        NodeTestUtil.launch(nodeThree, true);

        XimixNode nodeFour = getXimixNode("/conf/mixnet.xml", "/conf/node4.xml", handler);
        NodeTestUtil.launch(nodeFour, true);

        XimixNode nodeFive = getXimixNode("/conf/mixnet.xml", "/conf/node5.xml", handler);
        NodeTestUtil.launch(nodeFive, true);

        XimixRegistrar registrar = XimixRegistrarFactory.createAdminServiceRegistrar(ResourceAnchor.load("/conf/mixnet.xml"));

        KeyGenerationService keyGenerationService = registrar.connect(KeyGenerationService.class);

        KeyGenerationOptions keyGenOptions = new KeyGenerationOptions.Builder(KeyType.EC_ELGAMAL, "secp256r1")
            .withThreshold(2)
            .withNodes("A", "B", "C", "D")
            .build();

        byte[] encPubKey = keyGenerationService.generatePublicKey("ECKEY", keyGenOptions);

        UploadService client = registrar.connect(UploadService.class);

        ECPublicKeyParameters pubKey = (ECPublicKeyParameters)PublicKeyFactory.createKey(encPubKey);

        SigningService signingService = registrar.connect(SigningService.class);


       // client.uploadMessage("FRED", ballot.getEncoded());

        SHA256Digest sha256 = new SHA256Digest();

        byte[] message = "hello world!".getBytes();
        byte[] hash = new byte[sha256.getDigestSize()];

        sha256.update(message, 0, message.length);

        sha256.doFinal(hash, 0);

        SignatureGenerationOptions sigGenOptions = new SignatureGenerationOptions.Builder(KeyType.ECDSA)
            .withThreshold(2)
            .withNodes("A", "B", "C", "D")
            .build();

        byte[] dsaSig = signingService.generateSignature("ECKEY", sigGenOptions, hash);

        //
        // check the signature locally.
        //
        ECDSASigner signer = new ECDSASigner();

        ECPublicKeyParameters sigPubKey = (ECPublicKeyParameters)PublicKeyFactory.createKey(signingService.fetchPublicKey("ECKEY"));

        signer.init(false, sigPubKey);

        BigInteger[] rs = decodeSig(dsaSig);

        Assert.assertTrue(signer.verifySignature(hash, rs[0], rs[1]));


        //
        // Shutdown nodes and close services.
        //
        NodeTestUtil.shutdownNodes();
        keyGenerationService.close(null);
        signingService.close(null);
        client.close(null);

    }

    private static BigInteger[] decodeSig(
        byte[] encoding)
        throws IOException
    {
        ASN1Sequence s = ASN1Sequence.getInstance(encoding);
        BigInteger[] sig = new BigInteger[2];

        sig[0] = ((ASN1Integer)s.getObjectAt(0)).getValue();
        sig[1] = ((ASN1Integer)s.getObjectAt(1)).getValue();

        return sig;
    }

}