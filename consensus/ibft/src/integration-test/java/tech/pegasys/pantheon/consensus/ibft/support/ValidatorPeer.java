/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.consensus.ibft.support;

import tech.pegasys.pantheon.consensus.ibft.ConsensusRoundIdentifier;
import tech.pegasys.pantheon.consensus.ibft.ibftevent.IbftEvents;
import tech.pegasys.pantheon.consensus.ibft.ibftevent.IbftReceivedMessageEvent;
import tech.pegasys.pantheon.consensus.ibft.ibftmessage.CommitMessage;
import tech.pegasys.pantheon.consensus.ibft.ibftmessage.NewRoundMessage;
import tech.pegasys.pantheon.consensus.ibft.ibftmessage.PrepareMessage;
import tech.pegasys.pantheon.consensus.ibft.ibftmessage.ProposalMessage;
import tech.pegasys.pantheon.consensus.ibft.ibftmessage.RoundChangeMessage;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.CommitPayload;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.MessageFactory;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.NewRoundPayload;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.PreparePayload;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.PreparedCertificate;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.ProposalPayload;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.RoundChangeCertificate;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.RoundChangePayload;
import tech.pegasys.pantheon.consensus.ibft.ibftmessagedata.SignedData;
import tech.pegasys.pantheon.consensus.ibft.statemachine.IbftController;
import tech.pegasys.pantheon.crypto.SECP256K1;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.crypto.SECP256K1.Signature;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.p2p.api.MessageData;
import tech.pegasys.pantheon.ethereum.p2p.wire.DefaultMessage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

// Each "inject" function returns the SignedPayload representation of the transmitted message.
public class ValidatorPeer {

  private final Address nodeAddress;
  private final KeyPair nodeKeys;
  private final MessageFactory messageFactory;
  private List<MessageData> receivedMessages = Lists.newArrayList();

  private final IbftController localNodeController;

  public ValidatorPeer(
      final NodeParams nodeParams,
      final MessageFactory messageFactory,
      final IbftController localNodeController) {
    this.nodeKeys = nodeParams.getNodeKeyPair();
    this.nodeAddress = nodeParams.getAddress();
    this.messageFactory = messageFactory;
    this.localNodeController = localNodeController;
  }

  public SignedData<ProposalPayload> injectProposal(
      final ConsensusRoundIdentifier rId, final Block block) {
    final SignedData<ProposalPayload> payload =
        messageFactory.createSignedProposalPayload(rId, block);
    injectMessage(ProposalMessage.create(payload));
    return payload;
  }

  public SignedData<PreparePayload> injectPrepare(
      final ConsensusRoundIdentifier rId, final Hash digest) {
    final SignedData<PreparePayload> payload =
        messageFactory.createSignedPreparePayload(rId, digest);
    injectMessage(PrepareMessage.create(payload));
    return payload;
  }

  public SignedData<CommitPayload> injectCommit(
      final ConsensusRoundIdentifier rId, final Hash digest) {
    final Signature commitSeal = SECP256K1.sign(digest, nodeKeys);
    final SignedData<CommitPayload> payload =
        messageFactory.createSignedCommitPayload(rId, digest, commitSeal);
    injectMessage(CommitMessage.create(payload));
    return payload;
  }

  public SignedData<NewRoundPayload> injectNewRound(
      final ConsensusRoundIdentifier rId,
      final RoundChangeCertificate roundChangeCertificate,
      final SignedData<ProposalPayload> proposalPayload) {

    final SignedData<NewRoundPayload> payload =
        messageFactory.createSignedNewRoundPayload(rId, roundChangeCertificate, proposalPayload);
    injectMessage(NewRoundMessage.create(payload));
    return payload;
  }

  public SignedData<RoundChangePayload> injectRoundChange(
      final ConsensusRoundIdentifier rId, final Optional<PreparedCertificate> preparedCertificate) {
    final SignedData<RoundChangePayload> payload =
        messageFactory.createSignedRoundChangePayload(rId, preparedCertificate);
    injectMessage(RoundChangeMessage.create(payload));
    return payload;
  }

  public void handleReceivedMessage(final MessageData message) {
    receivedMessages.add(message);
  }

  public List<MessageData> getReceivedMessages() {
    return Collections.unmodifiableList(receivedMessages);
  }

  public void clearReceivedMessages() {
    receivedMessages.clear();
  }

  public void injectMessage(final MessageData msgData) {
    final DefaultMessage message = new DefaultMessage(null, msgData);
    localNodeController.handleMessageEvent(
        (IbftReceivedMessageEvent) IbftEvents.fromMessage(message));
  }

  public MessageFactory getMessageFactory() {
    return messageFactory;
  }
}
