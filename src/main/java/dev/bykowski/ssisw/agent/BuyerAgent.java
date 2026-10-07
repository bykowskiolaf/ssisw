package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.BuyerAccount;
import dev.bykowski.ssisw.market.BuyerDecisionService;
import dev.bykowski.ssisw.market.CarOffer;
import dev.bykowski.ssisw.market.SearchCriteria;
import dev.bykowski.ssisw.market.Transaction;
import dev.bykowski.ssisw.simulation.SimulationObserver;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BuyerAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private final BuyerDecisionService decisionService = new BuyerDecisionService();
    private BuyerAccount account;
    private SearchCriteria criteria;
    private SimulationObserver observer;
    private int expectedSellerCount;
    private int targetPurchases;

    @Override
    protected void setup() {
        Object[] arguments = getArguments();
        account = new BuyerAccount(getLocalName(), (long) arguments[0]);
        criteria = (SearchCriteria) arguments[1];
        observer = (SimulationObserver) arguments[2];
        expectedSellerCount = (int) arguments[3];
        targetPurchases = (int) arguments[4];

        addBehaviour(new TickerBehaviour(this, 100) {
            @Override
            protected void onTick() {
                List<AID> sellers = findSellers();
                if (sellers.size() >= expectedSellerCount) {
                    stop();
                    addBehaviour(new PurchaseFlowBehaviour(sellers));
                }
            }
        });
    }

    private List<AID> findSellers() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription service = new ServiceDescription();
        service.setType(MarketplaceProtocol.SELLER_SERVICE);
        template.addServices(service);
        try {
            DFAgentDescription[] matches = DFService.search(this, template);
            List<AID> sellers = new ArrayList<>(matches.length);
            for (DFAgentDescription match : matches) {
                sellers.add(match.getName());
            }
            return sellers;
        } catch (FIPAException exception) {
            return List.of();
        }
    }

    private final class PurchaseFlowBehaviour extends Behaviour {

        private static final int WAITING_FOR_OFFERS = 0;
        private static final int WAITING_FOR_RESERVATION = 1;
        private static final int WAITING_FOR_PURCHASE = 2;
        private static final int WAITING_TO_RETRY = 3;
        private static final int FINISHED = 4;
        private static final int MAX_RESERVATION_ATTEMPTS = 50;

        private final List<AID> sellers;
        private final List<CarOffer> offers = new ArrayList<>();
        private final Set<String> respondingSellers = new HashSet<>();
        private String conversationId;
        private String expectedReplyToken;
        private int state;
        private int replies;
        private int attempts;
        private int completedPurchases;
        private long deadline;
        private long retryAt;
        private CarOffer selectedOffer;
        private AID selectedSeller;

        private PurchaseFlowBehaviour(List<AID> sellers) {
            super(BuyerAgent.this);
            this.sellers = sellers;
            sendSearch();
        }

        @Override
        public void action() {
            switch (state) {
                case WAITING_FOR_OFFERS -> collectOffers();
                case WAITING_FOR_RESERVATION -> receiveReservation();
                case WAITING_FOR_PURCHASE -> receivePurchase();
                case WAITING_TO_RETRY -> retrySearchWhenReady();
                default -> block();
            }
        }

        private void sendSearch() {
            attempts++;
            offers.clear();
            respondingSellers.clear();
            replies = 0;
            conversationId = "purchase-" + UUID.randomUUID();
            ACLMessage request = new ACLMessage(ACLMessage.CFP);
            sellers.forEach(request::addReceiver);
            request.setOntology(MarketplaceProtocol.SEARCH);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            SearchCriteria affordableCriteria = new SearchCriteria(
                    criteria.bodyType(), criteria.fuelType(), criteria.minimumYear(),
                    Math.min(criteria.maximumTotalCostPln(), account.budgetPln()));
            request.setContent(codec.encode(new MarketplaceProtocol.SearchRequest(affordableCriteria)));
            send(request);
            observer.searchStarted(getLocalName(), sellers.size());
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_OFFERS;
        }

        private void collectOffers() {
            ACLMessage response = receive(phaseTemplate(MarketplaceProtocol.SEARCH));
            if (response != null && respondingSellers.add(response.getSender().getName())) {
                replies++;
                if (response.getPerformative() == ACLMessage.PROPOSE) {
                    MarketplaceProtocol.SearchResponse payload = codec.decode(
                            response.getContent(), MarketplaceProtocol.SearchResponse.class);
                    offers.add(payload.offer());
                }
            }
            if (replies >= sellers.size() || System.currentTimeMillis() >= deadline) {
                Optional<CarOffer> best = decisionService.selectBestAffordable(offers, account);
                if (best.isEmpty()) {
                    finish();
                    return;
                }
                selectedOffer = best.orElseThrow();
                selectedSeller = sellers.stream()
                        .filter(seller -> seller.getLocalName().equals(selectedOffer.sellerName()))
                        .findFirst()
                        .orElseThrow();
                sendReservation();
                return;
            }
            block(25);
        }

        private void sendReservation() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(selectedSeller);
            request.setOntology(MarketplaceProtocol.RESERVATION);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.ReservationRequest(
                    selectedOffer.car().id(), getLocalName())));
            send(request);
            state = WAITING_FOR_RESERVATION;
        }

        private void receiveReservation() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.RESERVATION),
                    MessageTemplate.MatchSender(selectedSeller)));
            if (response == null) {
                block();
                return;
            }
            if (response.getPerformative() != ACLMessage.AGREE) {
                observer.reservationRejected(getLocalName(), selectedOffer.car().id());
                retryOrFinish();
                return;
            }
            sendPurchase();
        }

        private void sendPurchase() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(selectedSeller);
            request.setOntology(MarketplaceProtocol.PURCHASE);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.PurchaseRequest(
                    selectedOffer.car().id(), getLocalName())));
            send(request);
            state = WAITING_FOR_PURCHASE;
        }

        private void receivePurchase() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.PURCHASE),
                    MessageTemplate.MatchSender(selectedSeller)));
            if (response == null) {
                block();
                return;
            }
            if (response.getPerformative() == ACLMessage.INFORM) {
                Transaction transaction = codec.decode(
                        response.getContent(), MarketplaceProtocol.PurchaseResponse.class).transaction();
                account.pay(transaction.totalCostPln());
                observer.purchaseCompleted(transaction, account.budgetPln());
                completedPurchases++;
                if (completedPurchases < targetPurchases) {
                    attempts = 0;
                    sendSearch();
                    return;
                }
            } else if (completedPurchases < targetPurchases) {
                retryOrFinish();
                return;
            }
            finish();
        }

        private MessageTemplate phaseTemplate(String ontology) {
            return MessageTemplate.and(
                    MessageTemplate.and(
                            MessageTemplate.MatchConversationId(conversationId),
                            MessageTemplate.MatchOntology(ontology)),
                    MessageTemplate.MatchInReplyTo(expectedReplyToken));
        }

        private void retryOrFinish() {
            if (attempts < MAX_RESERVATION_ATTEMPTS) {
                retryAt = System.currentTimeMillis()
                        + 50
                        + Math.floorMod(getLocalName().hashCode() + attempts * 31, 100);
                state = WAITING_TO_RETRY;
            } else {
                finish();
            }
        }

        private void retrySearchWhenReady() {
            long waitMillis = retryAt - System.currentTimeMillis();
            if (waitMillis > 0) {
                block(waitMillis);
                return;
            }
            sendSearch();
        }

        private void finish() {
            state = FINISHED;
            observer.buyerCompleted(getLocalName(), account.budgetPln());
            doDelete();
        }

        @Override
        public boolean done() {
            return state == FINISHED;
        }
    }
}
