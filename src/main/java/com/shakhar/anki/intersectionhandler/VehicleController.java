package com.shakhar.anki.intersectionhandler;

import de.adesso.anki.AnkiConnector;
import de.adesso.anki.MessageListener;
import de.adesso.anki.Vehicle;
import de.adesso.anki.messages.LocalizationPositionUpdateMessage;
import de.adesso.anki.messages.SdkModeMessage;
import de.adesso.anki.messages.SetSpeedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VehicleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VehicleController.class);

    private static final int ACCELERATION = 10000;
    private static final int INTERSECTION_PIECE_ID = 10;
    private static final int STOP_DURATION = 3000;

    private static final String MULTICAST_ADDRESS = "225.225.225.225";
    private static final int MULTICAST_PORT = 1234;

    private static final byte ENTRY_BYTE = 1;
    private static final byte EXIT_BYTE = 2;

    private String ankiServerAddress;
    private int ankiServerPort;
    private String vehicleAddress;
    private int speed;
    private Vehicle vehicle;

    private InetAddress groupAddress;
    private MulticastSocket multicastSocket;
    private LinkedList<InetAddress> list;

    private Lock lock;
    private Condition firstInList;

    public VehicleController(String ankiServerAddress, int ankiServerPort, String vehicleAddress, int speed) throws IOException {
        this.ankiServerAddress = ankiServerAddress;
        this.ankiServerPort = ankiServerPort;
        this.vehicleAddress = vehicleAddress;
        this.speed = speed;
        groupAddress = InetAddress.getByName(MULTICAST_ADDRESS);
        multicastSocket = new MulticastSocket(MULTICAST_PORT);
        multicastSocket.joinGroup(groupAddress);
        list = new LinkedList<>();
        lock = new ReentrantLock();
        firstInList = lock.newCondition();
        initAnki();
    }

    public void initAnki() throws IOException {
        AnkiConnector connector = new AnkiConnector(ankiServerAddress, ankiServerPort);
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
        new Thread(() -> {
            while (true)
                multicastReceive();
        }).start();
    }

    private void go() {
        vehicle.sendMessage(new SetSpeedMessage(speed, ACCELERATION));
    }

    private void stop() {
        vehicle.sendMessage(new SetSpeedMessage(0, ACCELERATION));
    }

    private void onEnteringIntersection() {
        LOGGER.info("Entering Intersection");
        stop();
        long arrivalTime = System.currentTimeMillis();
        broadcastEntry();
        lock.lock();
        try {
            while (!(list.isEmpty() || isSelfAddress(list.get(0))))
                firstInList.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        long duration = System.currentTimeMillis() - arrivalTime;
        if (duration >= STOP_DURATION)
            go();
        else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    go();
                }
            }, duration);
        }
    }

    private void onExitingIntersection() {
        LOGGER.info("Exiting Intersection");
        broadcastExit();
    }

    private void broadcastEntry() {
        LOGGER.info("Broadcasting Entry");
        multicastSend(ENTRY_BYTE);
    }

    private void broadcastExit() {
        LOGGER.info("Broadcasting Exit");
        multicastSend(EXIT_BYTE);
    }

    private void multicastSend(byte b) {
        byte[] data = new byte[]{b};
        DatagramPacket packet = new DatagramPacket(data, data.length, groupAddress, MULTICAST_PORT);
        try {
            multicastSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void multicastReceive() {
        byte[] buffer = new byte[1];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            multicastSocket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        switch (buffer[0]) {
            case ENTRY_BYTE:
                onEntryReceive(packet.getAddress());
                break;
            case EXIT_BYTE:
                onExitReceive(packet.getAddress());
        }
    }

    private void onEntryReceive(InetAddress sender) {
        LOGGER.info("Received Entry Signal from {}", sender);
        lock.lock();
        try {
            list.add(sender);
        } finally {
            lock.unlock();
        }
    }

    private void onExitReceive(InetAddress sender) {
        LOGGER.info("Received Exit Signal from {}", sender);
        lock.lock();
        try {
            list.remove(sender);
            firstInList.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private static boolean isSelfAddress(InetAddress address) {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while(interfaces.hasMoreElements()) {
            Enumeration<InetAddress> addresses = interfaces.nextElement().getInetAddresses();
            while(addresses.hasMoreElements())
                if(addresses.nextElement().equals(address))
                    return true;
        }
        return false;
    }

    private class LocalizationPositionUpdateHandler implements MessageListener<LocalizationPositionUpdateMessage> {

        int lastRoadPiece;
        @Override
        public void messageReceived(LocalizationPositionUpdateMessage message) {
            if (message.getRoadPieceId() == INTERSECTION_PIECE_ID && lastRoadPiece != INTERSECTION_PIECE_ID) {
                onEnteringIntersection();
            } else if (message.getRoadPieceId() != INTERSECTION_PIECE_ID && lastRoadPiece == INTERSECTION_PIECE_ID) {
                onExitingIntersection();
            }
            lastRoadPiece = message.getRoadPieceId();
        }
    }
}