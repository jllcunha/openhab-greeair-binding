/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.internal.discovery;

import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.openhab.binding.greeair.handler.GreeAirHandler;
import org.openhab.binding.greeair.internal.encryption.CryptoUtil;
import org.openhab.binding.greeair.internal.gson.GreeBindRequest4Gson;
import org.openhab.binding.greeair.internal.gson.GreeBindRequestPack4Gson;
import org.openhab.binding.greeair.internal.gson.GreeBindResponse4Gson;
import org.openhab.binding.greeair.internal.gson.GreeBindResponsePack4Gson;
import org.openhab.binding.greeair.internal.gson.GreeExecCommand4Gson;
import org.openhab.binding.greeair.internal.gson.GreeExecResponse4Gson;
import org.openhab.binding.greeair.internal.gson.GreeExecResponsePack4Gson;
import org.openhab.binding.greeair.internal.gson.GreeExecuteCommandPack4Gson;
import org.openhab.binding.greeair.internal.gson.GreeReqStatus4Gson;
import org.openhab.binding.greeair.internal.gson.GreeReqStatusPack4Gson;
import org.openhab.binding.greeair.internal.gson.GreeScanResponse4Gson;
import org.openhab.binding.greeair.internal.gson.GreeStatusResponse4Gson;
import org.openhab.binding.greeair.internal.gson.GreeStatusResponsePack4Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

/**
 * The GreeDevice object repesents a Gree Airconditioner and provides
 * device specific attributes as well a the functionality for the Air Conditioner
 *
 * @author John Cunha - Initial contribution
 */

public class GreeDevice {
    private final static Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private Boolean mIsBound = false;
    private InetAddress mAddress;
    private int mPort = 0;
    private String mKey;
    private GreeScanResponse4Gson mScanResponseGson = null;
    private GreeBindResponse4Gson bindResponseGson = null;
    private GreeStatusResponse4Gson statusResponseGson = null;
    private GreeStatusResponsePack4Gson prevStatusResponsePackGson = null;
    private final Logger logger = LoggerFactory.getLogger(GreeAirHandler.class);

    public Boolean getIsBound() {
        return mIsBound;
    }

    public void setIsBound(Boolean isBound) {
        this.mIsBound = isBound;
    }

    public InetAddress getAddress() {
        return mAddress;
    }

    public void setAddress(InetAddress address) {
        this.mAddress = address;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        this.mPort = port;
    }

    public String getKey() {
        return mKey;
    }

    public String getName() {
        return mScanResponseGson.packJson.name;
    }

    public String getId() {
        return mScanResponseGson.packJson.mac;
    }

    public GreeScanResponse4Gson getScanResponseGson() {
        return mScanResponseGson;
    }

    public void setScanResponseGson(GreeScanResponse4Gson gson) {
        mScanResponseGson = gson;
    }

    public GreeBindResponse4Gson getBindResponseGson() {
        return bindResponseGson;
    }

    public GreeStatusResponse4Gson getGreeStatusResponse4Gson() {
        return statusResponseGson;
    }

    public void BindWithDevice(DatagramSocket clientSocket) throws Exception {
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[347];
        Gson gson = new Gson();

        // Prep the Binding Request pack
        GreeBindRequestPack4Gson bindReqPackGson = new GreeBindRequestPack4Gson();
        bindReqPackGson.mac = getId();
        bindReqPackGson.t = "bind";
        bindReqPackGson.uid = 0;
        String bindReqPackStr = gson.toJson(bindReqPackGson);

        // Now Encrypt the Binding Request pack
        String encryptedBindReqPacket = CryptoUtil.encryptPack(CryptoUtil.GetAESGeneralKeyByteArray(), bindReqPackStr);

        // Prep the Binding Request
        GreeBindRequest4Gson bindReqGson = new GreeBindRequest4Gson();
        bindReqGson.cid = "app";
        bindReqGson.i = 1;
        bindReqGson.t = "pack";
        bindReqGson.uid = 0;
        bindReqGson.pack = new String(encryptedBindReqPacket.getBytes(), UTF8_CHARSET);
        String bindReqStr = gson.toJson(bindReqGson);
        sendData = bindReqStr.getBytes();

        // Now Send the request
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getAddress(), getPort());
        clientSocket.send(sendPacket);

        // Recieve a response
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String modifiedSentence = new String(receivePacket.getData());

        // Read the response
        StringReader stringReader = new StringReader(modifiedSentence);
        bindResponseGson = gson.fromJson(new JsonReader(stringReader), GreeBindResponse4Gson.class);
        bindResponseGson.decryptedPack = CryptoUtil.decryptPack(CryptoUtil.GetAESGeneralKeyByteArray(),
                bindResponseGson.pack);

        // Create the JSON to hold the response values
        stringReader = new StringReader(bindResponseGson.decryptedPack);
        bindResponseGson.packJson = gson.fromJson(new JsonReader(stringReader), GreeBindResponsePack4Gson.class);

        // Now set the key and flag to indicate the bind was succesful
        mKey = bindResponseGson.packJson.key;
        setIsBound(Boolean.TRUE);
    }

    public void SetDevicePower(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound and values are valid
        if ((!Objects.equals(getIsBound(), Boolean.TRUE)) || (value.intValue() < 0 || value.intValue() > 1)) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Pow", value);
        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDevicePower() {
        return GetIntStatusVal("Pow");
    }

    public void SetDeviceMode(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound and values are valid
        if ((!Objects.equals(getIsBound(), Boolean.TRUE)) || (value.intValue() < 0 || value.intValue() > 4)) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Mod", value);
        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceMode() {
        return GetIntStatusVal("Mod");
    }

    public void SetDeviceSwingVertical(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound and values are valid
        // Only values 0,1,2,3,4,5,6,10,11 allowed
        if ((!Objects.equals(getIsBound(), Boolean.TRUE)) || (value.intValue() < 0 || value.intValue() > 11)
                || (value.intValue() > 6 && value.intValue() < 10)) {
            return;
        }
        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("SwUpDn", value);
        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceSwingVertical() {
        return GetIntStatusVal("SwUpDn");
    }

    public void SetDeviceWindspeed(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound and values are valid
        /*
         * Possible values are :
         * 0 : Auto
         * 1 : Low
         * 2 : Medium Low
         * 3 : Medium
         * 4 : Medium High
         * 5 : High
         */
        if ((!Objects.equals(getIsBound(), Boolean.TRUE)) || (value.intValue() < 0 || value.intValue() > 5)) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("WdSpd", value);
        parameters.put("Quiet", 0);
        parameters.put("Tur", 0);
        parameters.put("NoiseSet", 0);
        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceWindspeed() {
        return GetIntStatusVal("WdSpd");
    }

    public void SetDeviceTurbo(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound and values are valid
        if ((!Objects.equals(getIsBound(), Boolean.TRUE)) || (value.intValue() < 0 || value.intValue() > 1)) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Tur", value);
        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceTurbo() {
        return GetIntStatusVal("Tur");
    }

    public void SetDeviceLight(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound and values are valid
        if ((!Objects.equals(getIsBound(), Boolean.TRUE)) || (value.intValue() < 0 || value.intValue() > 1)) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Lig", value);
        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceLight() {
        return GetIntStatusVal("Lig");
    }

    public void SetDeviceTempSet(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound
        if (getIsBound() != Boolean.TRUE) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("TemUn", new Integer(0));
        parameters.put("SetTem", value);

        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceTempSet() {
        return GetIntStatusVal("SetTem");
    }

    public void SetDeviceAir(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound
        if (getIsBound() != Boolean.TRUE) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Air", value);

        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceAir() {
        return GetIntStatusVal("Air");
    }

    public void SetDeviceDry(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound
        if (getIsBound() != Boolean.TRUE) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Blo", value);

        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceDry() {
        return GetIntStatusVal("Blo");
    }

    public void SetDeviceHealth(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound
        if (getIsBound() != Boolean.TRUE) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("Health", value);

        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDeviceHealth() {
        return GetIntStatusVal("Health");
    }

    public void SetDevicePwrSaving(DatagramSocket clientSocket, Integer value) throws Exception {
        // Only allow this to happen if this device has been bound
        if (getIsBound() != Boolean.TRUE) {
            return;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("SvSt", value);
        parameters.put("WdSpd", new Integer(0));
        parameters.put("Quiet", new Integer(0));
        parameters.put("Tur", new Integer(0));
        parameters.put("SwhSlp", new Integer(0));
        parameters.put("SlpMod", new Integer(0));

        ExecuteCommand(clientSocket, parameters);
    }

    public Integer GetDevicePwrSaving() {
        return GetIntStatusVal("SvSt");
    }

    public Integer GetIntStatusVal(String valueName) {
        /*
         * Note : Values can be:
         * "Pow": Power (0 or 1)
         * "Mod": Mode: Auto: 0, Cool: 1, Dry: 2, Fan: 3, Heat: 4
         * "SetTem": Requested Temperature
         * "WdSpd": Fan Speed : Low:1, Medium Low:2, Medium :3, Medium High :4, High :5
         * "Air": Air Mode Enabled
         * "Blo": Dry
         * "Health": Health
         * "SwhSlp": Sleep
         * "SlpMod": ???
         * "Lig": Light On
         * "SwingLfRig": Swing Left Right
         * "SwUpDn": Swing Up Down: // Ceiling:0, Upwards : 10, Downwards : 11, Full range : 1
         * "Quiet": Quiet mode
         * "Tur": Turbo
         * "StHt": 0,
         * "TemUn": Temperature unit, 0 for Celsius, 1 for Fahrenheit
         * "HeatCoolType"
         * "TemRec":
         * "SvSt": Power Saving
         */
        // Find the valueName in the Returned Status object
        String columns[] = statusResponseGson.packJson.cols;
        Integer values[] = statusResponseGson.packJson.dat;
        List<String> colList = new ArrayList<>(Arrays.asList(columns));
        List<Integer> valList = new ArrayList<>(Arrays.asList(values));
        int valueArrayposition = colList.indexOf(valueName);
        if (valueArrayposition == -1) {
            return null;
        }

        // Now get the Corresponding value
        Integer value = valList.get(valueArrayposition);
        return value;
    }

    public Boolean HasStatusValChanged(String valueName) {
        if (prevStatusResponsePackGson == null) {
            return Boolean.TRUE;
        }
        // Find the valueName in the Current Status object
        String currcolumns[] = statusResponseGson.packJson.cols;
        Integer currvalues[] = statusResponseGson.packJson.dat;
        List<String> currcolList = new ArrayList<>(Arrays.asList(currcolumns));
        List<Integer> currvalList = new ArrayList<>(Arrays.asList(currvalues));
        int currvalueArrayposition = currcolList.indexOf(valueName);
        if (currvalueArrayposition == -1) {
            return null;
        }
        // Now get the Corresponding value
        Integer currvalue = currvalList.get(currvalueArrayposition);

        // Find the valueName in the Previous Status object
        String prevcolumns[] = prevStatusResponsePackGson.cols;
        Integer prevvalues[] = prevStatusResponsePackGson.dat;
        List<String> prevcolList = new ArrayList<>(Arrays.asList(prevcolumns));
        List<Integer> prevvalList = new ArrayList<>(Arrays.asList(prevvalues));
        int prevvalueArrayposition = prevcolList.indexOf(valueName);
        if (prevvalueArrayposition == -1) {
            return null;
        }
        // Now get the Corresponding value
        Integer prevvalue = prevvalList.get(prevvalueArrayposition);

        // Finally Compare the values
        return new Boolean(currvalue.intValue() != prevvalue.intValue());
    }

    protected void ExecuteCommand(DatagramSocket clientSocket, HashMap<String, Integer> parameters) throws Exception {
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        Gson gson = new Gson();

        // Convert the parameter map values to arrays
        String[] keyArray = parameters.keySet().toArray(new String[0]);
        Integer[] valueArray = parameters.values().toArray(new Integer[0]);

        // Prep the Command Request pack
        GreeExecuteCommandPack4Gson execCmdPackGson = new GreeExecuteCommandPack4Gson();
        execCmdPackGson.opt = keyArray;
        execCmdPackGson.p = valueArray;
        execCmdPackGson.t = "cmd";
        String execCmdPackStr = gson.toJson(execCmdPackGson);

        // Now Encrypt the Binding Request pack
        String encryptedCommandReqPacket = CryptoUtil.encryptPack(getKey().getBytes(), execCmdPackStr);
        // String unencryptedCommandReqPacket = CryptoUtil.decryptPack(device.getKey().getBytes(),
        // encryptedCommandReqPacket);

        // Prep the Command Request
        GreeExecCommand4Gson execCmdGson = new GreeExecCommand4Gson();
        execCmdGson.cid = "app";
        execCmdGson.i = 0;
        execCmdGson.t = "pack";
        execCmdGson.uid = 0;
        execCmdGson.pack = new String(encryptedCommandReqPacket.getBytes(), UTF8_CHARSET);
        String execCmdStr = gson.toJson(execCmdGson);
        sendData = execCmdStr.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getAddress(), getPort());
        clientSocket.send(sendPacket);

        // Recieve a response
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        String modifiedSentence = new String(receivePacket.getData());
        // System.out.println("FROM SERVER:" + modifiedSentence);
        // byte[] modifiedSentenceArray = receivePacket.getData();

        // Read the response
        StringReader stringReader = new StringReader(modifiedSentence);
        GreeExecResponse4Gson execResponseGson = gson.fromJson(new JsonReader(stringReader),
                GreeExecResponse4Gson.class);
        execResponseGson.decryptedPack = CryptoUtil.decryptPack(this.getKey().getBytes(), execResponseGson.pack);

        // Create the JSON to hold the response values
        stringReader = new StringReader(execResponseGson.decryptedPack);
        execResponseGson.packJson = gson.fromJson(new JsonReader(stringReader), GreeExecResponsePack4Gson.class);

    }

    public void getDeviceStatus(DatagramSocket clientSocket) throws Exception {
        Gson gson = new Gson();
        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];

        // Set the values in the HashMap
        ArrayList<String> columns = new ArrayList<>();
        columns.add("Pow");
        columns.add("Mod");
        columns.add("SetTem");
        columns.add("WdSpd");
        columns.add("Air");
        columns.add("Blo");
        columns.add("Health");
        columns.add("SwhSlp");
        columns.add("Lig");
        columns.add("SwingLfRig");
        columns.add("SwUpDn");
        columns.add("Quiet");
        columns.add("Tur");
        columns.add("StHt");
        columns.add("TemUn");
        columns.add("HeatCoolType");
        columns.add("TemRec");
        columns.add("SvSt");
        columns.add("NoiseSet");

        // Convert the parameter map values to arrays
        String[] colArray = columns.toArray(new String[0]);

        // Prep the Command Request pack
        GreeReqStatusPack4Gson reqStatusPackGson = new GreeReqStatusPack4Gson();
        reqStatusPackGson.t = "status";
        reqStatusPackGson.cols = colArray;
        reqStatusPackGson.mac = getId();
        String reqStatusPackStr = gson.toJson(reqStatusPackGson);

        // Now Encrypt the Binding Request pack
        String encryptedStatusReqPacket = CryptoUtil.encryptPack(getKey().getBytes(), reqStatusPackStr);

        // Prep the Status Request
        GreeReqStatus4Gson reqStatusGson = new GreeReqStatus4Gson();
        reqStatusGson.cid = "app";
        reqStatusGson.i = 0;
        reqStatusGson.t = "pack";
        reqStatusGson.uid = 0;
        reqStatusGson.pack = new String(encryptedStatusReqPacket.getBytes(), UTF8_CHARSET);
        String execCmdStr = gson.toJson(reqStatusGson);
        sendData = execCmdStr.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, getAddress(), getPort());
        clientSocket.send(sendPacket);

        logger.trace("Sending Status request packet to device");

        // Recieve a response
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        clientSocket.receive(receivePacket);
        logger.trace("Status request packet received from device");
        String modifiedSentence = new String(receivePacket.getData());

        // Keep a copy of the old response to be used to check if values have changed
        // If first time running, there will not be a previous GreeStatusResponsePack4Gson
        if (statusResponseGson != null && statusResponseGson.packJson != null) {
            prevStatusResponsePackGson = new GreeStatusResponsePack4Gson(statusResponseGson.packJson);
        }

        // Read the response
        StringReader stringReader = new StringReader(modifiedSentence);
        statusResponseGson = gson.fromJson(new JsonReader(stringReader), GreeStatusResponse4Gson.class);
        statusResponseGson.decryptedPack = CryptoUtil.decryptPack(this.getKey().getBytes(), statusResponseGson.pack);

        logger.trace("Response from device: {}", statusResponseGson.decryptedPack);

        // Create the JSON to hold the response values
        stringReader = new StringReader(statusResponseGson.decryptedPack);

        statusResponseGson.packJson = gson.fromJson(new JsonReader(stringReader), GreeStatusResponsePack4Gson.class);
    }

}
