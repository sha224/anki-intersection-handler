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

    private final int ACCELERATION = 10000;

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
        vehicle.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> vehicle.disconnect()));
        vehicle.sendMessage(new SdkModeMessage());
        vehicle.sendMessage(new SetSpeedMessage(speed, ACCELERATION));
        LocalizationIntersectionUpdateHandler liuh = new LocalizationIntersectionUpdateHandler();
        vehicle.addMessageListener(LocalizationIntersectionUpdateMessage.class, liuh);
    }

    private class LocalizationIntersectionUpdateHandler implements MessageListener<LocalizationIntersectionUpdateMessage> {

        @Override
        public void messageReceived(LocalizationIntersectionUpdateMessage message) {
            if (message.getIntersectionCode() == 1) {
                vehicle.sendMessage(new SetSpeedMessage(0, ACCELERATION));
            }
        }
    }
}