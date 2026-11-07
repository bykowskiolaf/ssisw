package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.NegotiationResult;
import dev.bykowski.ssisw.market.NegotiationService;
import dev.bykowski.ssisw.simulation.SimulationObserver;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class NegotiatorAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private final NegotiationService negotiationService = new NegotiationService();
    private SimulationObserver observer;

    @Override
    protected void setup() {
        observer = (SimulationObserver) getArguments()[0];
        registerNegotiatorService();
        addBehaviour(new CyclicBehaviour() {
            private final MessageTemplate template = MessageTemplate.MatchOntology(
                    MarketplaceProtocol.NEGOTIATION);

            @Override
            public void action() {
                ACLMessage request = receive(template);
                if (request == null) {
                    block();
                    return;
                }
                NegotiatorAgent.this.handleNegotiation(request);
            }
        });
    }

    private void handleNegotiation(ACLMessage request) {
        ACLMessage response = request.createReply();
        try {
            MarketplaceProtocol.NegotiationRequest payload = codec.decode(
                    request.getContent(), MarketplaceProtocol.NegotiationRequest.class);
            if (!request.getSender().getLocalName().equals(payload.buyerName())) {
                response.setPerformative(ACLMessage.REFUSE);
                response.setContent("BUYER_ID_MISMATCH");
            } else {
                NegotiationResult result = negotiationService.negotiate(
                        payload.offer(), payload.initialOfferTotalPln(), payload.buyerMaximumTotalPln());
                observer.negotiationCompleted(payload.buyerName(), result);
                response.setPerformative(result.accepted() ? ACLMessage.INFORM : ACLMessage.REFUSE);
                response.setContent(codec.encode(new MarketplaceProtocol.NegotiationResponse(result)));
            }
        } catch (RuntimeException exception) {
            response.setPerformative(ACLMessage.FAILURE);
            response.setContent(exception.getMessage());
        }
        send(response);
    }

    private void registerNegotiatorService() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription service = new ServiceDescription();
        service.setType(MarketplaceProtocol.NEGOTIATOR_SERVICE);
        service.setName(getLocalName());
        description.addServices(service);
        try {
            DFService.register(this, description);
        } catch (FIPAException exception) {
            throw new IllegalStateException("Cannot register negotiator " + getLocalName(), exception);
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {
            // The DF may already be unavailable during platform shutdown.
        }
    }
}
