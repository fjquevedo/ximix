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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTF8String;

/**
 * Request message to download up to a specified number of messages from a given board.
 */
public class BoardDownloadMessage
    extends ASN1Object
{
    private final String boardName;
    private final int maxNumberOfMessages;

    /**
     * Base constructor.
     *
     * @param boardName the name of the board to download from.
     * @param maxNumberOfMessages the maximum number of messages to be sent back in any response.
     */
    public BoardDownloadMessage(String boardName, int maxNumberOfMessages)
    {
        this.boardName = boardName;
        this.maxNumberOfMessages = maxNumberOfMessages;
    }

    private BoardDownloadMessage(ASN1Sequence seq)
    {
        this.boardName = DERUTF8String.getInstance(seq.getObjectAt(0)).getString();
        this.maxNumberOfMessages = ASN1Integer.getInstance(seq.getObjectAt(1)).getValue().intValue();
    }

    public static final BoardDownloadMessage getInstance(Object o)
    {
        if (o instanceof BoardDownloadMessage)
        {
            return (BoardDownloadMessage)o;
        }
        else if (o != null)
        {
            return new BoardDownloadMessage(ASN1Sequence.getInstance(o));
        }

        return null;
    }

    @Override
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new DERUTF8String(boardName));
        v.add(new ASN1Integer(maxNumberOfMessages));

        return new DERSequence(v);
    }

    public String getBoardName()
    {
        return boardName;
    }

    public int getMaxNumberOfMessages()
    {
        return maxNumberOfMessages;
    }
}
