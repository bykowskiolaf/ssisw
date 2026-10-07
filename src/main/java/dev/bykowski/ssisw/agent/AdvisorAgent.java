package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.RecommendationService;
import dev.bykowski.ssisw.market.ScoredOffer;
import dev.bykowski.ssisw.simulation.SimulationObserver;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

public class AdvisorAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private final RecommendationService recommendationService = new RecommendationService();
    private SimulationObserver observer;

    @Override
    protected void setup() {
        observer = (SimulationObserver) getArguments()[0];
        registerAdvisorService();
        addBehaviour(new CyclicBehaviour() {
            private final MessageTemplate template = MessageTemplate.MatchOntology(
                    MarketplaceProtocol.RECOMMENDATION);

            @Override
            public void action() {
                ACLMessage request = receive(template);
                if (request == null) {
                    block();
                    return;
                }
                AdvisorAgent.this.handleRecommendation(request);
            }
        });
    }

    private void handleRecommendation(ACLMessage request) {
        ACLMessage response = request.createReply();
        try {
            MarketplaceProtocol.RecommendationRequest payload = codec.decode(
                    request.getContent(), MarketplaceProtocol.RecommendationRequest.class);
            List<ScoredOffer> recommendations = recommendationService.recommendTop3(
                    payload.offers(), payload.criteria());
            observer.recommendationGenerated(
                    request.getSender().getLocalName(), recommendations);
            response.setPerformative(ACLMessage.INFORM);
            response.setContent(codec.encode(
                    new MarketplaceProtocol.RecommendationResponse(recommendations)));
        } catch (RuntimeException exception) {
            response.setPerformative(ACLMessage.FAILURE);
            response.setContent(exception.getMessage());
        }
        send(response);
    }

    private void registerAdvisorService() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription service = new ServiceDescription();
        service.setType(MarketplaceProtocol.ADVISOR_SERVICE);
        service.setName(getLocalName());
        description.addServices(service);
        try {
            DFService.register(this, description);
        } catch (FIPAException exception) {
            throw new IllegalStateException("Cannot register advisor " + getLocalName(), exception);
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
