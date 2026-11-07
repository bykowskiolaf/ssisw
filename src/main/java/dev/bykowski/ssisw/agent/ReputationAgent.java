package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.ReputationService;
import dev.bykowski.ssisw.market.SellerReputation;
import dev.bykowski.ssisw.simulation.SimulationObserver;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Map;

public class ReputationAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private final ReputationService reputationService = new ReputationService();
    private SimulationObserver observer;

    @Override
    protected void setup() {
        observer = (SimulationObserver) getArguments()[0];
        registerService();
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage request = receive();
                if (request == null) {
                    block();
                    return;
                }
                ReputationAgent.this.handle(request);
            }
        });
    }

    private void handle(ACLMessage request) {
        ACLMessage response = request.createReply();
        try {
            if (MarketplaceProtocol.REPUTATION_QUERY.equals(request.getOntology())) {
                MarketplaceProtocol.ReputationQuery query = codec.decode(
                        request.getContent(), MarketplaceProtocol.ReputationQuery.class);
                Map<String, SellerReputation> reputations = reputationService.reputations(query.sellerNames());
                response.setPerformative(ACLMessage.INFORM);
                response.setContent(codec.encode(new MarketplaceProtocol.ReputationResponse(reputations)));
            } else if (MarketplaceProtocol.RATING.equals(request.getOntology())) {
                MarketplaceProtocol.RatingRequest rating = codec.decode(
                        request.getContent(), MarketplaceProtocol.RatingRequest.class);
                if (!request.getSender().getLocalName().equals(rating.buyerName())) {
                    throw new IllegalArgumentException("BUYER_ID_MISMATCH");
                }
                if (!observer.hasCompletedTransaction(
                        rating.transactionId(), rating.buyerName(), rating.sellerName())) {
                    throw new IllegalArgumentException("UNKNOWN_TRANSACTION");
                }
                SellerReputation reputation = reputationService.rate(
                        rating.transactionId(), rating.sellerName(), rating.rating());
                observer.ratingRecorded(rating.buyerName(), rating.sellerName(), rating.rating(), reputation);
                response.setPerformative(ACLMessage.INFORM);
                response.setContent(codec.encode(new MarketplaceProtocol.RatingResponse(reputation)));
            } else {
                response.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                response.setContent("Unknown reputation message");
            }
        } catch (RuntimeException exception) {
            response.setPerformative(ACLMessage.FAILURE);
            response.setContent(exception.getMessage());
        }
        send(response);
    }

    private void registerService() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription service = new ServiceDescription();
        service.setType(MarketplaceProtocol.REPUTATION_SERVICE);
        service.setName(getLocalName());
        description.addServices(service);
        try {
            DFService.register(this, description);
        } catch (FIPAException exception) {
            throw new IllegalStateException("Cannot register reputation agent", exception);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {
        }
    }
}
