package org.desingpatterns.question_generic.onlineauctionsystem.observer;

import org.desingpatterns.question_generic.onlineauctionsystem.entities.Auction;

public interface AuctionObserver {
    void onUpdate(Auction auction, String message);
}
