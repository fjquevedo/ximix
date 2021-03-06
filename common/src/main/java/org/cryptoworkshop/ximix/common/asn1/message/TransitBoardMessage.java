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
package org.cryptoworkshop.ximix.common.asn1.message;

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Creation message for setting a transit board for carrying out an operation.
 */
public class TransitBoardMessage
    extends ASN1Object
{
    private final long operationNumber;
    private final String boardName;
    private final int stepNumber;

    /**
     * Base constructor.
     *
     * @param operationNumber the ID of the operation the transit board will be used for.
     * @param boardName the name of the board the transit board is associated with.
     * @param stepNumber the number of the step in the operation the transit board is for.
     */
    public TransitBoardMessage(long operationNumber, String boardName, int stepNumber)
    {
        this.operationNumber = operationNumber;
        this.boardName = boardName;
        this.stepNumber = stepNumber;
    }

    private TransitBoardMessage(ASN1Sequence seq)
    {
        this.operationNumber = ASN1Integer.getInstance(seq.getObjectAt(0)).getValue().longValue();
        this.boardName = DERUTF8String.getInstance(seq.getObjectAt(1)).getString();
        this.stepNumber = ASN1Integer.getInstance(seq.getObjectAt(2)).getValue().intValue();
    }

    public static final TransitBoardMessage getInstance(Object o)
    {
        if (o instanceof TransitBoardMessage)
        {
            return (TransitBoardMessage)o;
        }
        else if (o != null)
        {
            return new TransitBoardMessage(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new ASN1Integer(BigInteger.valueOf(operationNumber)));
        v.add(new DERUTF8String(boardName));
        v.add(new ASN1Integer(BigInteger.valueOf(stepNumber)));

        return new DERSequence(v);
    }

    public String getBoardName()
    {
        return boardName;
    }

    public long getOperationNumber()
    {
        return operationNumber;
    }

    public int getStepNumber()
    {
        return stepNumber;
    }
}
