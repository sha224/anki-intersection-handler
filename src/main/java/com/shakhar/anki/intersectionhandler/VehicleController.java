package com.shakhar.anki.intersectionhandler;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.LocalizationIntersectionUpdateMessage;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;

import java.io.IOException;
import java.util.List;

public class VehicleController {

    private String vehicleAddress;
    private int speed;
    private Vehicle vehicle;

    public VehicleController(String vehicleAddress, int speed) throws IOException {
        this.vehicleAddress = vehicleAddress;
        this.speed = speed;
        init();
    }

    public void init() throws IOException {
        AnkiConnector connector = new AnkiConnector("localhost", 5000);
        List<Vehicle> vehicles = connector.findVehicles();
        for (Vehicle iterVehicle : vehicles) {
            if (iterVehicle.getAddress().equals(vehicleAddress)) {
                vehicle = iterVehicle;
                break;
            }
        }
    }

    public void run() {
        if (vehicle == null)
            return;
        Vehicle myVehicle = vehicle;
        myVehicle.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> myVehicle.disconnect()));
        myVehicle.sendMessage(new SdkModeMessage());
        myVehicle.sendMessage(new SetSpeedMessage(speed, speed));
    }
}