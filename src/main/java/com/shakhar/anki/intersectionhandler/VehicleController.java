package com.shakhar.anki.intersectionhandler;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.LocalizationPositionUpdateMessage;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VehicleController {

    private final int ACCELERATION = 10000;
    private final int INTERSECTION_PIECE_ID = 10;

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
        go();
        LocalizationPositionUpdateHandler liuh = new LocalizationPositionUpdateHandler();
        vehicle.addMessageListener(LocalizationPositionUpdateMessage.class, liuh);
    }

    private void go() {
        vehicle.sendMessage(new SetSpeedMessage(speed, ACCELERATION));
    }

    private void stop() {
        vehicle.sendMessage(new SetSpeedMessage(0, ACCELERATION));
    }

    private class LocalizationPositionUpdateHandler implements MessageListener<LocalizationPositionUpdateMessage> {

        int lastRoadPiece;
        @Override
        public void messageReceived(LocalizationPositionUpdateMessage message) {
            if (message.getRoadPieceId() == INTERSECTION_PIECE_ID && lastRoadPiece != INTERSECTION_PIECE_ID) {
                stop();
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        go();
                    }
                }, 3000);
            }
            lastRoadPiece = message.getRoadPieceId();
        }
    }
}