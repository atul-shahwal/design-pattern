package org.creational.prototype;

import java.util.ArrayList;
import java.util.List;

public class NetworkConnection implements Cloneable{

    private String ip;
    private String importantData;

    private List<String> domains = new ArrayList<>();

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getImportantData() {
        return importantData;
    }

    public void setImportantData(String importantData) {
        this.importantData = importantData;
    }

    public List<String> getDomains() {
        return domains;
    }

    public void setDomains(List<String> domains) {
        this.domains = domains;
    }

    public void loadImpoertantData() throws InterruptedException {
        domains.add("www.google.com");
        domains.add("www.yahoo.com");
        domains.add("www.facebook.com");
        this.importantData = "very very important data";
        //it will take five minutes
        Thread.sleep(6000);
    }

    @Override
    public String toString() {
        return "NetworkConnection{" +
                "ip='" + ip + '\'' +
                ", importantData='" + importantData + '\'' +
                ", domains=" + domains +
                '}';
    }

//    @Override
//    protected Object clone() throws CloneNotSupportedException {
//        return super.clone();
//    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        // logic for deep copying
        NetworkConnection networkConnection = new NetworkConnection();
        networkConnection.setIp(this.ip);
        networkConnection.setImportantData(this.importantData);
        for(String s: this.getDomains()){
           networkConnection.getDomains().add(s);
        }
        return networkConnection;
    }
}
