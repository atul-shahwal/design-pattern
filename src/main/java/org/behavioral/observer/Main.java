package org.behavioral.observer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    public static void main(String[] args) throws IOException {
        Subject channel = new YoutubeChannel();
        Observer aman = new Subscriber("Aman");
        Observer raman = new Subscriber("Raman");
        channel.subscribe(aman);
        channel.subscribe(raman);
        channel.notifyChanges("kong");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        while (true){
            System.out.println("Press 1 to upload vedio");
            System.out.println("Press 2 to create new Subscriber");
            System.out.println("Press 3 to exit");
            int c = Integer.parseInt(bufferedReader.readLine());
            if(c == 1){
                //new vedio upload code
                System.out.println("vedio upload name");
                String vedioTitle = bufferedReader.readLine().trim();
                channel.notifyChanges(vedioTitle);
            }
            else if(c == 2){
                //create new subscriber code
                System.out.println("Enter Name of Subscriber");
                String subsName = bufferedReader.readLine().trim();
                Subscriber subscriber = new Subscriber(subsName);
                channel.subscribe(subscriber);
            }
            else if(c == 3){
                //create exit code
                System.out.println("Thanks for using ");
                break;
            }
            else {
                //wrong input code
                System.out.println("wrong input");
            }
        }
    }
}
