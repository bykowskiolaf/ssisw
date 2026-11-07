package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.MarketTrendSnapshot;
import dev.bykowski.ssisw.market.TrendService;
import dev.bykowski.ssisw.simulation.SimulationObserver;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.HashSet;
import java.util.Set;

public class TrendAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private final TrendService trendService = new TrendService();
    private final Set<String> recordedTransactions = new HashSet<>();
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
                TrendAgent.this.handle(request);
            }
        });
    }

    private void handle(ACLMessage request) {
        ACLMessage response = request.createReply();
        try {
            if (!MarketplaceProtocol.SALE_EVENT.equals(request.getOntology())) {
                throw new IllegalArgumentException("Unknown trend message");
            }
            MarketplaceProtocol.SaleEventRequest event = codec.decode(
                    request.getContent(), MarketplaceProtocol.SaleEventRequest.class);
            if (!request.getSender().getLocalName().equals(event.buyerName())) {
                throw new IllegalArgumentException("BUYER_ID_MISMATCH");
            }
            if (!observer.hasCompletedTransaction(event.sale().transaction())) {
                throw new IllegalArgumentException("UNKNOWN_TRANSACTION");
            }
            if (!recordedTransactions.add(event.sale().transaction().id())) {
                throw new IllegalStateException("Transaction already recorded");
            }
            MarketTrendSnapshot snapshot = trendService.record(event.sale());
            observer.trendUpdated(snapshot);
            response.setPerformative(ACLMessage.INFORM);
            response.setContent(codec.encode(new MarketplaceProtocol.SaleEventResponse(snapshot)));
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
        service.setType(MarketplaceProtocol.TREND_SERVICE);
        service.setName(getLocalName());
        description.addServices(service);
        try {
            DFService.register(this, description);
        } catch (FIPAException exception) {
            throw new IllegalStateException("Cannot register trend agent", exception);
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
