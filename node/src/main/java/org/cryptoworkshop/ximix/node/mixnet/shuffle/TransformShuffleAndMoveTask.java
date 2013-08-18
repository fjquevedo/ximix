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
package org.cryptoworkshop.ximix.node.mixnet.shuffle;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.Commitment;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.commitments.HashCommitter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.cryptoworkshop.ximix.common.asn1.message.BoardUploadBlockMessage;
import org.cryptoworkshop.ximix.common.asn1.message.CommandMessage;
import org.cryptoworkshop.ximix.common.asn1.message.MessageCommitment;
import org.cryptoworkshop.ximix.common.asn1.message.MessageReply;
import org.cryptoworkshop.ximix.common.asn1.message.MessageWitnessBlock;
import org.cryptoworkshop.ximix.common.asn1.message.PermuteAndMoveMessage;
import org.cryptoworkshop.ximix.common.asn1.message.PostedMessage;
import org.cryptoworkshop.ximix.common.asn1.message.PostedMessageBlock;
import org.cryptoworkshop.ximix.common.asn1.message.TransitBoardMessage;
import org.cryptoworkshop.ximix.common.service.ServiceConnectionException;
import org.cryptoworkshop.ximix.common.service.ServicesConnection;
import org.cryptoworkshop.ximix.node.mixnet.board.BulletinBoard;
import org.cryptoworkshop.ximix.node.mixnet.board.BulletinBoardRegistry;
import org.cryptoworkshop.ximix.node.mixnet.transform.Transform;
import org.cryptoworkshop.ximix.node.service.NodeContext;

public class TransformShuffleAndMoveTask
    implements Runnable
{
    private final NodeContext nodeContext;
    private final PermuteAndMoveMessage message;
    private final BulletinBoardRegistry boardRegistry;
    private final CommandMessage.Type type;
    private final ServicesConnection peerConnection;

    public TransformShuffleAndMoveTask(NodeContext nodeContext, BulletinBoardRegistry boardRegistry, ServicesConnection peerConnection, CommandMessage.Type type, PermuteAndMoveMessage message)
    {
        this.nodeContext = nodeContext;
        this.boardRegistry = boardRegistry;
        this.peerConnection = peerConnection;
        this.type = type;
        this.message = message;
    }

    public void run()
    {
        BulletinBoard board = boardRegistry.getTransitBoard(message.getOperationNumber(), message.getBoardName(), message.getStepNumber());
        Transform transform = boardRegistry.getTransform(message.getTransformName());
        IndexCommitter committer = new IndexCommitter(new SHA256Digest(), new SecureRandom());

        try
        {
            PostedMessageBlock.Builder messageBlockBuilder = new PostedMessageBlock.Builder(20);    // TODO: make configurable
            MessageWitnessBlock.Builder messageWitnessBlockBuilder = new MessageWitnessBlock.Builder(messageBlockBuilder.capacity());

            IndexNumberGenerator indexGen = new IndexNumberGenerator(board.size(), new SecureRandom());  // TODO: specify random

            int nextStepNumber = message.getStepNumber() + 1;

            if (message.getKeyID() != null)
            {
                transform.init(PublicKeyFactory.createKey(nodeContext.getPublicKey(message.getKeyID())));

                for (PostedMessage postedMessage : board)
                {
                    byte[] transformed = transform.transform(postedMessage.getMessage());
                    int newIndex = indexGen.nextIndex();
                    Commitment commitment = committer.commit(newIndex);

                    messageBlockBuilder.add(newIndex, transformed, commitment.getCommitment());
                    messageWitnessBlockBuilder.add(postedMessage.getIndex(), new MessageCommitment(commitment));

                    if (messageBlockBuilder.isFull())
                    {
                        processMessageBlock(messageBlockBuilder, nextStepNumber);
                        processWitnessBlock(board, messageWitnessBlockBuilder);
                    }
                }
            }
            else
            {
                for (PostedMessage postedMessage : board)
                {
                    int newIndex = indexGen.nextIndex();
                    Commitment commitment = committer.commit(newIndex);

                    messageBlockBuilder.add(newIndex, postedMessage.getMessage(), commitment.getCommitment());
                    messageWitnessBlockBuilder.add(postedMessage.getIndex(), new MessageCommitment(commitment));

                    if (messageBlockBuilder.isFull())
                    {
                        processMessageBlock(messageBlockBuilder, nextStepNumber);
                        processWitnessBlock(board, messageWitnessBlockBuilder);
                    }
                }
            }

            if (!messageBlockBuilder.isEmpty())
            {
                processMessageBlock(messageBlockBuilder, nextStepNumber);
                processWitnessBlock(board, messageWitnessBlockBuilder);
            }

            MessageReply reply = peerConnection.sendMessage(CommandMessage.Type.TRANSFER_TO_BOARD_ENDED, new TransitBoardMessage(message.getOperationNumber(), board.getName(), nextStepNumber));

            if (reply.getType() != MessageReply.Type.OKAY)
            {
                throw new ServiceConnectionException("message failed");
            }

            board.clear();
        }
        catch (ServiceConnectionException e)
        {
            e.printStackTrace();
            // TODO: log?
        }
        catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void processMessageBlock(PostedMessageBlock.Builder messageBlockBuilder, int nextStepNumber)
        throws ServiceConnectionException
    {
        MessageReply reply = peerConnection.sendMessage(type, new BoardUploadBlockMessage(message.getOperationNumber(), message.getBoardName(), nextStepNumber, messageBlockBuilder.build()));

        messageBlockBuilder.clear();

        if (reply.getType() != MessageReply.Type.OKAY)
        {
            throw new ServiceConnectionException("message failed");
        }
    }

    private void processWitnessBlock(BulletinBoard board, MessageWitnessBlock.Builder messageWitnessBlockBuilder)
        throws ServiceConnectionException
    {
        board.postWitnessBlock(messageWitnessBlockBuilder.build());

        messageWitnessBlockBuilder.clear();
    }

    private class IndexCommitter
        extends HashCommitter
    {
        public IndexCommitter(ExtendedDigest digest, SecureRandom random)
        {
            super(digest, random);
        }

        public Commitment commit(int index)
        {
            return super.commit(BigInteger.valueOf(index).toByteArray());
        }
    }
}