package dev.bykowski.ssisw.agent;

import dev.bykowski.ssisw.communication.MarketplaceProtocol;
import dev.bykowski.ssisw.communication.MessageCodec;
import dev.bykowski.ssisw.market.BuyerAccount;
import dev.bykowski.ssisw.market.CarOffer;
import dev.bykowski.ssisw.market.SearchCriteria;
import dev.bykowski.ssisw.market.ScoredOffer;
import dev.bykowski.ssisw.market.NegotiationResult;
import dev.bykowski.ssisw.market.NegotiationService;
import dev.bykowski.ssisw.market.SaleRecord;
import dev.bykowski.ssisw.market.SellerReputation;
import dev.bykowski.ssisw.market.ReturnResult;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class BuyerAgent extends Agent {

    private final MessageCodec codec = new MessageCodec();
    private BuyerAccount account;
    private SearchCriteria criteria;
    private SimulationObserver observer;
    private int expectedSellerCount;
    private int targetPurchases;
    private long discoveryDeadline;
    private boolean demonstrateReturn;

    @Override
    protected void setup() {
        Object[] arguments = getArguments();
        account = new BuyerAccount(getLocalName(), (long) arguments[0]);
        criteria = (SearchCriteria) arguments[1];
        observer = (SimulationObserver) arguments[2];
        expectedSellerCount = (int) arguments[3];
        targetPurchases = (int) arguments[4];
        demonstrateReturn = (boolean) arguments[5] && "Buyer01".equals(getLocalName());
        discoveryDeadline = System.currentTimeMillis() + 5_000;

        addBehaviour(new TickerBehaviour(this, 100) {
            @Override
            protected void onTick() {
                List<AID> sellers = findSellers();
                Optional<AID> advisor = findAdvisor();
                Optional<AID> negotiator = findService(MarketplaceProtocol.NEGOTIATOR_SERVICE);
                Optional<AID> reputation = findService(MarketplaceProtocol.REPUTATION_SERVICE);
                Optional<AID> trend = findService(MarketplaceProtocol.TREND_SERVICE);
                if (sellers.size() >= expectedSellerCount && advisor.isPresent() && negotiator.isPresent()
                        && reputation.isPresent() && trend.isPresent()) {
                    stop();
                    addBehaviour(new PurchaseFlowBehaviour(
                            sellers, advisor.orElseThrow(), negotiator.orElseThrow(),
                            reputation.orElseThrow(), trend.orElseThrow()));
                } else if (System.currentTimeMillis() >= discoveryDeadline) {
                    stop();
                    observer.simulationFailed(getLocalName(), "required marketplace agent discovery timed out");
                    doDelete();
                }
            }
        });
    }

    private Optional<AID> findAdvisor() {
        return findService(MarketplaceProtocol.ADVISOR_SERVICE);
    }

    private Optional<AID> findService(String serviceType) {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription service = new ServiceDescription();
        service.setType(serviceType);
        template.addServices(service);
        try {
            DFAgentDescription[] matches = DFService.search(this, template);
            return matches.length == 0 ? Optional.empty() : Optional.of(matches[0].getName());
        } catch (FIPAException exception) {
            return Optional.empty();
        }
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
        private static final int WAITING_FOR_REPUTATION = 1;
        private static final int WAITING_FOR_RECOMMENDATION = 2;
        private static final int WAITING_FOR_NEGOTIATION = 3;
        private static final int WAITING_FOR_RESERVATION = 4;
        private static final int WAITING_FOR_PURCHASE = 5;
        private static final int WAITING_FOR_RETURN = 6;
        private static final int WAITING_FOR_RATING = 7;
        private static final int WAITING_FOR_TREND = 8;
        private static final int WAITING_TO_RETRY = 9;
        private static final int FINISHED = 10;
        private static final int MAX_RESERVATION_ATTEMPTS = 50;

        private final List<AID> sellers;
        private final AID advisor;
        private final AID negotiator;
        private final AID reputationAgent;
        private final AID trendAgent;
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
        private SearchCriteria activeCriteria;
        private long agreedTotalPln;
        private Map<String, Double> sellerReputations = Map.of();
        private Transaction completedTransaction;
        private boolean returnAttempted;

        private PurchaseFlowBehaviour(
                List<AID> sellers,
                AID advisor,
                AID negotiator,
                AID reputationAgent,
                AID trendAgent) {
            super(BuyerAgent.this);
            this.sellers = sellers;
            this.advisor = advisor;
            this.negotiator = negotiator;
            this.reputationAgent = reputationAgent;
            this.trendAgent = trendAgent;
            sendSearch();
        }

        @Override
        public void action() {
            switch (state) {
                case WAITING_FOR_OFFERS -> collectOffers();
                case WAITING_FOR_REPUTATION -> receiveReputation();
                case WAITING_FOR_RECOMMENDATION -> receiveRecommendation();
                case WAITING_FOR_NEGOTIATION -> receiveNegotiation();
                case WAITING_FOR_RESERVATION -> receiveReservation();
                case WAITING_FOR_PURCHASE -> receivePurchase();
                case WAITING_FOR_RETURN -> receiveReturn();
                case WAITING_FOR_RATING -> receiveRating();
                case WAITING_FOR_TREND -> receiveTrend();
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
            activeCriteria = new SearchCriteria(
                    criteria.bodyType(), criteria.fuelType(), criteria.minimumYear(),
                    Math.min(criteria.maximumTotalCostPln(), account.budgetPln()));
            request.setContent(codec.encode(new MarketplaceProtocol.SearchRequest(activeCriteria)));
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
                if (offers.isEmpty()) {
                    fail("no affordable offers before reaching the purchase target");
                    return;
                }
                sendReputationQuery();
                return;
            }
            block(25);
        }

        private void sendReputationQuery() {
            ACLMessage request = new ACLMessage(ACLMessage.QUERY_REF);
            request.addReceiver(reputationAgent);
            request.setOntology(MarketplaceProtocol.REPUTATION_QUERY);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.ReputationQuery(
                    offers.stream().map(CarOffer::sellerName).distinct().toList())));
            send(request);
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_REPUTATION;
        }

        private void receiveReputation() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.REPUTATION_QUERY),
                    MessageTemplate.MatchSender(reputationAgent)));
            if (response == null) {
                waitOrFail("reputation query timed out");
                return;
            }
            if (response.getPerformative() != ACLMessage.INFORM) {
                fail("reputation query failed");
                return;
            }
            try {
                Map<String, SellerReputation> reputations = codec.decode(
                        response.getContent(), MarketplaceProtocol.ReputationResponse.class).reputations();
                sellerReputations = reputations.entrySet().stream().collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                                Map.Entry::getKey, entry -> entry.getValue().averageRating()));
                sendRecommendationRequest();
            } catch (RuntimeException exception) {
                fail("cannot process reputation response: " + exception.getMessage());
            }
        }

        private void sendRecommendationRequest() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(advisor);
            request.setOntology(MarketplaceProtocol.RECOMMENDATION);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(
                    new MarketplaceProtocol.RecommendationRequest(
                            List.copyOf(offers), activeCriteria, sellerReputations)));
            send(request);
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_RECOMMENDATION;
        }

        private void receiveRecommendation() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.RECOMMENDATION),
                    MessageTemplate.MatchSender(advisor)));
            if (response == null) {
                if (System.currentTimeMillis() >= deadline) {
                    retryOrFinish();
                } else {
                    block(25);
                }
                return;
            }
            if (response.getPerformative() != ACLMessage.INFORM) {
                fail("advisor rejected the recommendation request");
                return;
            }
            try {
                List<ScoredOffer> recommendations = codec.decode(
                        response.getContent(), MarketplaceProtocol.RecommendationResponse.class)
                        .recommendations();
                if (recommendations == null || recommendations.isEmpty() || recommendations.size() > 3
                        || recommendations.stream().anyMatch(this::isInvalidRecommendation)) {
                    fail("advisor returned an invalid recommendation");
                    return;
                }
                CarOffer recommended = recommendations.getFirst().offer();
                selectedOffer = offers.stream()
                        .filter(offer -> sameOffer(offer, recommended))
                        .findFirst()
                        .orElseThrow();
                selectedSeller = sellers.stream()
                        .filter(seller -> seller.getLocalName().equals(selectedOffer.sellerName()))
                        .findFirst()
                        .orElseThrow();
                sendNegotiationRequest();
            } catch (RuntimeException exception) {
                fail("cannot process advisor response: " + exception.getMessage());
            }
        }

        private void sendNegotiationRequest() {
            long initialOffer = selectedOffer.car().additionalCostPln()
                    + Math.round(selectedOffer.car().pricePln() * 0.80);
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(negotiator);
            request.setOntology(MarketplaceProtocol.NEGOTIATION);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.NegotiationRequest(
                    selectedOffer,
                    getLocalName(),
                    initialOffer,
                    Math.min(account.budgetPln(), selectedOffer.car().totalCostPln()))));
            send(request);
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_NEGOTIATION;
        }

        private void receiveNegotiation() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.NEGOTIATION),
                    MessageTemplate.MatchSender(negotiator)));
            if (response == null) {
                if (System.currentTimeMillis() >= deadline) {
                    retryOrFinish();
                } else {
                    block(25);
                }
                return;
            }
            try {
                MarketplaceProtocol.NegotiationResponse payload = codec.decode(
                        response.getContent(), MarketplaceProtocol.NegotiationResponse.class);
                NegotiationResult result = payload.result();
                if (response.getPerformative() != ACLMessage.INFORM || !isValidNegotiation(result)) {
                    retryOrFinish();
                    return;
                }
                agreedTotalPln = result.agreedTotalPln();
                sendReservation();
            } catch (RuntimeException exception) {
                fail("cannot process negotiation response: " + exception.getMessage());
            }
        }

        private boolean isValidNegotiation(NegotiationResult result) {
            return result != null
                    && result.accepted()
                    && Objects.equals(result.carId(), selectedOffer.car().id())
                    && result.rounds() >= 1
                    && result.rounds() <= NegotiationService.MAXIMUM_ROUNDS
                    && result.agreedTotalPln() >= NegotiationService.minimumTotal(selectedOffer.car())
                    && result.agreedTotalPln() <= selectedOffer.car().totalCostPln()
                    && account.canAfford(result.agreedTotalPln());
        }

        private boolean isInvalidRecommendation(ScoredOffer recommendation) {
            if (recommendation == null || recommendation.offer() == null
                    || recommendation.offer().car() == null) {
                return true;
            }
            CarOffer recommended = recommendation.offer();
            return !Double.isFinite(recommendation.score())
                    || recommendation.score() < 0.0
                    || recommendation.score() > 1.0
                    || !activeCriteria.matches(recommended.car())
                    || !account.canAfford(recommended.car().totalCostPln())
                    || offers.stream().noneMatch(offer -> sameOffer(offer, recommended));
        }

        private boolean sameOffer(CarOffer left, CarOffer right) {
            return Objects.equals(left.sellerName(), right.sellerName())
                    && Objects.equals(left.car().id(), right.car().id());
        }

        private void sendReservation() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(selectedSeller);
            request.setOntology(MarketplaceProtocol.RESERVATION);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.ReservationRequest(
                    selectedOffer.car().id(), getLocalName(), agreedTotalPln)));
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
                completedTransaction = transaction;
                if (demonstrateReturn && !returnAttempted) {
                    returnAttempted = true;
                    sendReturn();
                } else {
                    completeKeptPurchase();
                }
                return;
            } else if (completedPurchases < targetPurchases) {
                retryOrFinish();
                return;
            }
            finish();
        }

        private void sendReturn() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(selectedSeller);
            request.setOntology(MarketplaceProtocol.RETURN);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.ReturnRequest(
                    completedTransaction.id(), getLocalName())));
            send(request);
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_RETURN;
        }

        private void receiveReturn() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.RETURN),
                    MessageTemplate.MatchSender(selectedSeller)));
            if (response == null) {
                waitOrFail("return response timed out");
                return;
            }
            if (response.getPerformative() == ACLMessage.INFORM) {
                ReturnResult result = codec.decode(
                        response.getContent(), MarketplaceProtocol.ReturnResponse.class).result();
                if (result == null || !result.accepted()
                        || !completedTransaction.equals(result.transaction())) {
                    fail("seller returned an invalid return confirmation");
                    return;
                }
                account.refund(result.transaction().totalCostPln());
                observer.returnCompleted(result, account.budgetPln());
                attempts = 0;
                sendSearch();
                return;
            }
            completeKeptPurchase();
        }

        private void completeKeptPurchase() {
            completedPurchases++;
            sendRating();
        }

        private void sendRating() {
            int rating = 3 + Math.floorMod(completedTransaction.id().hashCode(), 3);
            ACLMessage request = new ACLMessage(ACLMessage.INFORM);
            request.addReceiver(reputationAgent);
            request.setOntology(MarketplaceProtocol.RATING);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.RatingRequest(
                    completedTransaction.id(), completedTransaction.sellerName(), getLocalName(), rating)));
            send(request);
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_RATING;
        }

        private void receiveRating() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.RATING),
                    MessageTemplate.MatchSender(reputationAgent)));
            if (response == null) {
                waitOrFail("rating acknowledgement timed out");
                return;
            }
            if (response.getPerformative() != ACLMessage.INFORM) {
                fail("rating was rejected");
                return;
            }
            sendSaleEvent();
        }

        private void sendSaleEvent() {
            ACLMessage request = new ACLMessage(ACLMessage.INFORM);
            request.addReceiver(trendAgent);
            request.setOntology(MarketplaceProtocol.SALE_EVENT);
            request.setConversationId(conversationId);
            expectedReplyToken = UUID.randomUUID().toString();
            request.setReplyWith(expectedReplyToken);
            request.setContent(codec.encode(new MarketplaceProtocol.SaleEventRequest(
                    getLocalName(), new SaleRecord(completedTransaction, selectedOffer.car()))));
            send(request);
            deadline = System.currentTimeMillis() + 1_500;
            state = WAITING_FOR_TREND;
        }

        private void receiveTrend() {
            ACLMessage response = receive(MessageTemplate.and(
                    phaseTemplate(MarketplaceProtocol.SALE_EVENT),
                    MessageTemplate.MatchSender(trendAgent)));
            if (response == null) {
                waitOrFail("trend acknowledgement timed out");
                return;
            }
            if (response.getPerformative() != ACLMessage.INFORM) {
                fail("sale event was rejected");
                return;
            }
            if (completedPurchases < targetPurchases) {
                attempts = 0;
                sendSearch();
            } else {
                finish();
            }
        }

        private void waitOrFail(String timeoutMessage) {
            if (System.currentTimeMillis() >= deadline) {
                fail(timeoutMessage);
            } else {
                block(25);
            }
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
                fail("reservation or advisor retry limit exceeded");
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

        private void fail(String message) {
            state = FINISHED;
            observer.simulationFailed(getLocalName(), message);
            doDelete();
        }

        @Override
        public boolean done() {
            return state == FINISHED;
        }
    }
}
