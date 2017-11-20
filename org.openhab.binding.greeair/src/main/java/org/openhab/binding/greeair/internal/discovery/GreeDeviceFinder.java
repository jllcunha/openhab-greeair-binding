/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.internal.discovery;

import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openhab.binding.greeair.internal.encryption.CryptoUtil;
import org.openhab.binding.greeair.internal.gson.GreeScanReponsePack4Gson;
import org.openhab.binding.greeair.internal.gson.GreeScanRequest4Gson;
import org.openhab.binding.greeair.internal.gson.GreeScanResponse4Gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * The GreeDeviceFinder provides functionality for searching for
 * Gree Airconditioners on the network and keeping a list of
 * found devices.
 * 
 * @author John Cunha - Initial contribution
 */

public class GreeDeviceFinder {

    protected InetAddress mIPAddress = null;
    protected HashMap<String, GreeDevice> mDevicesHashMap = new HashMap<>();

    public GreeDeviceFinder(InetAddress broadcastAddress) throws UnknownHostException {
        mIPAddress = InetAddress.getByName("192.168.1.255");
    }

    public void Scan(DatagramSocket clientSocket) throws IOException, Exception {
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        // Send the Scan message
        // GreeProtocolUtils protocolUtils = new GreeProtocolUtils();
        // sendData = protocolUtils.CreateScanRequest();
        GreeScanRequest4Gson scanGson = new GreeScanRequest4Gson();
        scanGson.t = "scan";

        GsonBuilder gsonBuilder = new GsonBuilder();
        // gsonBuilder.setLenient();
        Gson gson = gsonBuilder.create();
        String scanReq = gson.toJson(scanGson);
        sendData = scanReq.getBytes();

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, mIPAddress, 7000);
        clientSocket.send(sendPacket);

        // Loop for respnses from devices until we get a timeout.
        boolean timeoutRecieved = false;
        while (!timeoutRecieved) {
            // Receive a response
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
                InetAddress remoteAddress = receivePacket.getAddress();
                int remotePort = receivePacket.getPort();

                // Read the response
                String modifiedSentence = new String(receivePacket.getData());
                StringReader stringReader = new StringReader(modifiedSentence);
                GreeScanResponse4Gson scanResponseGson = gson.fromJson(new JsonReader(stringReader),
                        GreeScanResponse4Gson.class);

                // If there was no pack, ignore the response
                if (scanResponseGson.pack == null) {
                    continue;
                }

                scanResponseGson.decryptedPack = CryptoUtil.decryptPack(CryptoUtil.GetAESGeneralKeyByteArray(),
                        scanResponseGson.pack);
                String decryptedMsg = CryptoUtil.decryptPack(CryptoUtil.GetAESGeneralKeyByteArray(),
                        scanResponseGson.pack);

                // If something was wrong with the decryption, ignore the response
                if (decryptedMsg == null) {
                    continue;
                }

                // Create the JSON to hold the response values
                stringReader = new StringReader(decryptedMsg);
                scanResponseGson.packJson = gson.fromJson(new JsonReader(stringReader), GreeScanReponsePack4Gson.class);

                // Now make sure the device is reported as a Gree device
                if (scanResponseGson.packJson.brand.equals("gree")) {
                    // Create a new GreeDevice
                    GreeDevice newDevice = new GreeDevice();
                    newDevice.setAddress(remoteAddress);
                    newDevice.setPort(remotePort);
                    newDevice.setScanResponseGson(scanResponseGson);

                    AddDevice(newDevice);
                }
            } catch (SocketTimeoutException e) {
                // We've received a timeout so lets quit searching for devices
                timeoutRecieved = true;
            }
        }
    }

    public void AddDevice(GreeDevice newDevice) {
        mDevicesHashMap.put(newDevice.getId(), newDevice);
    }

    public GreeDevice GetDevice(String id) {
        return mDevicesHashMap.get(id);
    }

    public HashMap<String, GreeDevice> GetDevices() {
        return mDevicesHashMap;
    }

    public GreeDevice GetDeviceByIPAddress(String ipAddress) {
        GreeDevice returnDevice = null;

        Set<String> keySet = mDevicesHashMap.keySet();
        Iterator<String> iter = keySet.iterator();
        while (returnDevice == null && iter.hasNext()) {
            Object thiskey = iter.next();
            GreeDevice currDevice = mDevicesHashMap.get(thiskey);
            if (currDevice != null && currDevice.getAddress().getHostAddress().equals(ipAddress)) {
                returnDevice = currDevice;
            }
        }

        return returnDevice;
    }

    public Integer GetScannedDeviceCount() {
        return new Integer(mDevicesHashMap.size());
    }
}
