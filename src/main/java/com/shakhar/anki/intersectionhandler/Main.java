package com.shakhar.anki.intersectionhandler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
        String vehicleAddress = properties.getProperty("anki.vehicle.address");
        int speed = Integer.parseInt(properties.getProperty("anki.vehicle.speed", "300"));

        VehicleController vehicleController = new VehicleController(vehicleAddress, speed);
        vehicleController.run();
    }

}
