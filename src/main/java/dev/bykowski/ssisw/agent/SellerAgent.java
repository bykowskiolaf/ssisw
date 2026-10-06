package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.CarOffer;
import dev.bykowski.ssisw.market.ReservationResult;
import dev.bykowski.ssisw.market.SellerInventory;
import dev.bykowski.ssisw.market.Transaction;
import dev.bykowski.ssisw.simulation.SimulationObserver;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Optional;

public class SellerAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private SellerInventory inventory;
    private SimulationObserver observer;
    private long reservationDurationMillis;

    @Override
    protected void setup() {
        Object[] arguments = getArguments();
        inventory = (SellerInventory) arguments[0];
        observer = (SimulationObserver) arguments[1];
        reservationDurationMillis = (long) arguments[2];
        registerSellerService();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage message = receive();
                if (message == null) {
                    block();
                    return;
                }
                SellerAgent.this.handle(message);
            }
        });
        addBehaviour(new TickerBehaviour(this, Math.min(1_000, reservationDurationMillis)) {
            @Override
            protected void onTick() {
                inventory.releaseExpiredReservations();
            }
        });
    }

    private void handle(ACLMessage message) {
        try {
            switch (message.getOntology()) {
                case MarketplaceProtocol.SEARCH -> handleSearch(message);
                case MarketplaceProtocol.RESERVATION -> handleReservation(message);
                case MarketplaceProtocol.PURCHASE -> handlePurchase(message);
                default -> reply(message, ACLMessage.NOT_UNDERSTOOD, "Unknown marketplace message");
            }
        } catch (RuntimeException exception) {
            reply(message, ACLMessage.NOT_UNDERSTOOD, exception.getMessage());
        }
    }

    private void handleSearch(ACLMessage message) {
        MarketplaceProtocol.SearchRequest request = codec.decode(
                message.getContent(), MarketplaceProtocol.SearchRequest.class);
        Optional<CarOffer> offer = inventory.findBestOffer(request.criteria());
        if (offer.isEmpty()) {
            reply(message, ACLMessage.REFUSE, "NO_OFFER");
            return;
        }
        CarOffer proposed = offer.orElseThrow();
        observer.offerProposed(getLocalName(), message.getSender().getLocalName(),
                proposed.car().id(), proposed.car().totalCostPln());
        reply(message, ACLMessage.PROPOSE,
                codec.encode(new MarketplaceProtocol.SearchResponse(proposed)));
    }

    private void handleReservation(ACLMessage message) {
        MarketplaceProtocol.ReservationRequest request = codec.decode(
                message.getContent(), MarketplaceProtocol.ReservationRequest.class);
        String senderName = message.getSender().getLocalName();
        if (!senderName.equals(request.buyerName())) {
            reply(message, ACLMessage.REFUSE, "BUYER_ID_MISMATCH");
            return;
        }
        ReservationResult result = inventory.reserve(
                request.carId(), senderName, reservationDurationMillis);
        reply(message, result.accepted() ? ACLMessage.AGREE : ACLMessage.REFUSE,
                codec.encode(new MarketplaceProtocol.ReservationResponse(result)));
    }

    private void handlePurchase(ACLMessage message) {
        MarketplaceProtocol.PurchaseRequest request = codec.decode(
                message.getContent(), MarketplaceProtocol.PurchaseRequest.class);
        String senderName = message.getSender().getLocalName();
        if (!senderName.equals(request.buyerName())) {
            reply(message, ACLMessage.FAILURE, "BUYER_ID_MISMATCH");
            return;
        }
        Optional<Transaction> transaction = inventory.purchase(request.carId(), senderName);
        if (transaction.isEmpty()) {
            reply(message, ACLMessage.FAILURE, "PURCHASE_REJECTED");
            return;
        }
        reply(message, ACLMessage.INFORM,
                codec.encode(new MarketplaceProtocol.PurchaseResponse(transaction.orElseThrow())));
    }

    private void reply(ACLMessage request, int performative, String content) {
        ACLMessage response = request.createReply();
        response.setPerformative(performative);
        response.setContent(content == null ? "" : content);
        send(response);
    }

    private void registerSellerService() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription service = new ServiceDescription();
        service.setType(MarketplaceProtocol.SELLER_SERVICE);
        service.setName(getLocalName());
        description.addServices(service);
        try {
            DFService.register(this, description);
        } catch (FIPAException exception) {
            throw new IllegalStateException("Cannot register seller " + getLocalName(), exception);
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
