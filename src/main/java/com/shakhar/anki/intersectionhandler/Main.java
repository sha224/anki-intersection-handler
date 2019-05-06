package com.shakhar.anki.intersectionhandler;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String configVehicleAddress = properties.getProperty("anki.vehicle.address");
        AnkiConnector connector = new AnkiConnector("localhost", 5000);
        List<Vehicle> vehicles = connector.findVehicles();
        Vehicle configVehicle = null;
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getAddress().equals(configVehicleAddress)) {
                configVehicle = vehicle;
                break;
            }
        }
        if (configVehicle == null) {
            System.out.println("Vehicle Not Found.");
            return;
        }
        Vehicle myVehicle = configVehicle;
        myVehicle.connect();
        myVehicle.sendMessage(new SdkModeMessage());
        myVehicle.sendMessage(new SetSpeedMessage(300, 300));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> myVehicle.disconnect()));
    }
}
