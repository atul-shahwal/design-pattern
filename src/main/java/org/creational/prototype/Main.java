package org.creational.prototype;

public class Main {
    public static void main(String[] args) throws InterruptedException{
        System.out.println("creating objects through prototype Design patten");
        NetworkConnection networkConnection = new NetworkConnection();
        networkConnection.setIp("192.168.4.4");
        networkConnection.loadImpoertantData();
        System.out.println(networkConnection);
        System.out.println(networkConnection.hashCode());
        NetworkConnection networkConnection1 = null;
        NetworkConnection networkConnection2 = null;
        //we want new object of network connection
        try {
            networkConnection1 = (NetworkConnection) networkConnection.clone();
            networkConnection2 = (NetworkConnection) networkConnection.clone();
        }catch (CloneNotSupportedException exception){
            System.out.println("error occured");
        }
//        networkConnection.getDomains().remove(0);
//        System.out.println("Clearly we can see shallow copy");
//        System.out.println(networkConnection);
//        System.out.println(networkConnection1);
//        System.out.println(networkConnection2);
//        System.out.println();
//        System.out.println();
//
//        System.out.println(networkConnection.getIp().hashCode());
//        System.out.println(networkConnection1.getIp().hashCode());
//        System.out.println(networkConnection2.getIp().hashCode());

        //deep copying
        networkConnection.getDomains().remove(0);
        System.out.println(networkConnection);
        System.out.println(networkConnection1);
        System.out.println(networkConnection2);

    }
}
