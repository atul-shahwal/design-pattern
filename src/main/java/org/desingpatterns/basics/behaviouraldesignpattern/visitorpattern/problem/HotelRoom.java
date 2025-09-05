package org.desingpatterns.basics.behaviouraldesignpattern.visitorpattern.problem;

public class HotelRoom {

    public int getRoomPrice(){
        //logic to calculate room-price
        return 0;
    }

    public void initiateRoomMaintenance(){
        //logic to start room maintenance
    }

    public void reserveRoom(){
        //logic to reserve the room
    }

    // many more operation can come over time.
    // in future if we modify this class this will be violation of open/close principle.
}
