package com.shakhar.anki.intersectionhandler;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException {
        AnkiConnector connector = new AnkiConnector("localhost", 5000);
        List<Vehicle> vehicles = connector.findVehicles();
        if (vehicles.size() == 0) {
            System.out.println("No Vehicles found.");
            return;
        }
        System.out.println("Select Vehicle:");
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle vehicle = vehicles.get(i);
            System.out.printf("%d) %s - %s %n", i+1, vehicle.getAddress(), vehicle.getAdvertisement());
        }
        Scanner sc = new Scanner(System.in);
        int selection = sc.nextInt();
        Vehicle myVehicle = vehicles.get(selection - 1);
        myVehicle.connect();
        myVehicle.sendMessage(new SdkModeMessage());
        myVehicle.sendMessage(new SetSpeedMessage(300, 300));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> myVehicle.disconnect()));
    }
}
