/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
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
    private final static HashMap<String, HashMap<String,Integer>> tempRanges = createTempRangeMap();
    private final static String [] tempScaleLUT = {"C","F"}; //used to convert TempUn integer to "C" to "F" string for hashmap
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

    private static HashMap<String, HashMap<String,Integer>> createTempRangeMap()
    {   //Create Hash Look Up for C and F Temperature Ranges for gree A/C units
        //f_range = {86,61}, c_range= {16,30}
        HashMap<String, HashMap<String,Integer>> tempRanges = new HashMap<>();
        HashMap<String, Integer> hmf = new HashMap<>();
        HashMap<String, Integer> hmc= new HashMap<>();

        hmf.put("min",new Integer(61)); // F
        hmf.put("max",new Integer(86));
        tempRanges.put("F",hmf);

        hmc.put("min",new Integer(16)); //C
        hmc.put("max",new Integer(30));
        tempRanges.put("C",hmc);

        return tempRanges;
    }

    private Integer[] ValidateTemperatureRangeForTempSet(Integer newVal,Integer CorF) {
        // Checks input ranges for validity and TempUn for validity
        // Uses newVal as priority and tries to validate and determine intent
        // For example if value is 75 and TempUn says Celsius, change TempUn to Fahrenheit
        //
        final String[] minMaxLUT = {"max","min"}; // looks up 0 = C = max, 1 = F = min
        HashMap<String, Integer> nullCorFLUT = new HashMap<>(); // simple look up table for logic
            nullCorFLUT.put("C",new Integer(0));
            nullCorFLUT.put("F",new Integer(1));
            nullCorFLUT.put("INVALID",new Integer(0));

        String validRangeCorF; // stores if the input range is a valid C or F temperature

        newVal = Math.max(newVal,Math.min(tempRanges.get("C").get("min"),tempRanges.get("F").get("min"))); // force to global min
        newVal = Math.min(newVal,Math.max(tempRanges.get("C").get("max"),tempRanges.get("F").get("max"))); // force to global max

        if ((newVal >= tempRanges.get("C").get("min") ) && ( newVal <= tempRanges.get("C").get("max")) ) {
            validRangeCorF = "C";
        } else if ((newVal >= tempRanges.get("F").get("min") ) && ( newVal <= tempRanges.get("F").get("max"))) {
            validRangeCorF = "F";
        }else{
            logger.warn("Input Temp request {} is invalid",newVal);
            validRangeCorF = "INVALID";
        }

        if (CorF == null){
            // if CorF wasnt initialized or is null set it from lookup
            CorF = nullCorFLUT.get(validRangeCorF);
        }

        if ((CorF == 1) && (validRangeCorF == "C")){ 
            CorF = 0; // input temp takes priority
        }
        else if ((CorF == 0) && (validRangeCorF == "F")){
            CorF = 1; // input temp takes priority
        }
        else if (validRangeCorF == "INVALID"){
            // force min or max temp based on CorF scale to be used
            newVal = tempRanges.get(tempScaleLUT[CorF]).get(minMaxLUT[CorF]);
        }
        
        return new Integer[]{newVal,CorF}; 
    }
    public void SetDeviceTempSet(DatagramSocket clientSocket, Integer value) throws Exception {
        // **value** :  set temperature in degrees celsius or Fahrenheit        
        // Only allow this to happen if this device has been bound
        if (getIsBound() != Boolean.TRUE) {
            return;
        }
        Integer [] retList;
        Integer newVal = new Integer(value);
        Integer outVal = new Integer(value);
        // Get Celsius or Fahrenheit from status message
        Integer CorF = GetIntStatusVal("TemUn");
        // TODO put a param in openhab to allow setting this from the config

        //If commanding Fahrenheit set halfStep to 1 or 0 to tell the A/C which F integer 
        //    temperature to use as celsius alone is ambigious
        Integer halfStep = new Integer(0); //default to C

        retList = ValidateTemperatureRangeForTempSet(newVal,CorF);
        newVal = retList[0];
        CorF = retList[1];

        if (CorF == 1){ //If Fahrenheit, 
            //value argument is degrees F, convert Fahrenheit to Celsius, 
            //SetTem input to A/C always in Celsius despite passing in 1 to TemUn
            outVal = new Integer((int) Math.round((newVal-32.)*5.0/9.0)); //Integer Truncated
            //******************TempRec TemSet Mapping for setting Fahrenheit****************************
            //Fahren = [68. , 69. , 70. , 71. , 72. , 73. , 74. , 75. , 76. , 77. , 78. , 79. , 80. , 81. , 82. , 83. , 84. , 85. , 86. ]
            //Celsiu = [20.0, 20.5, 21.1, 21.6, 22.2, 22.7, 23.3, 23.8, 24.4, 25.0, 25.5, 26.1, 26.6, 27.2, 27.7, 28.3, 28.8, 29.4, 30.0]
            //TemSet = [20,   21,   21,   22,   22,   23,   23,   24,   25,   25,   26,   26,   27,   27,   28,   28,   29,   29,  30, 30]
            //TemRec = [ 1,    0,    1,    0,    1,    0,    1,    0,   1,    0,    1,    0,    1,    0,    1,    0,    1,    0,   1,  0]
            //******************TempRec TemSet Mapping for setting Fahrenheit****************************
            // subtract the float verison - the int version to get the fractional difference
            // if the difference is positive set halfStep to 1, negative to 0
            halfStep = ((((newVal-32.)*5.0/9.0) - outVal) > 0) ? 1 : 0;
        }

        // Set the values in the HashMap
        HashMap<String, Integer> parameters = new HashMap<>();
        parameters.put("TemUn", CorF);
        parameters.put("SetTem",outVal);
        parameters.put("TemRec",halfStep);

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
         * "TemRec": (0 or 1), Send with SetTem, when TemUn==1, distinguishes between upper and lower integer Fahrenheit temp
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
        UpdateTempFtoC();
    }

    private void UpdateTempFtoC(){
        // Status message back from A/C always reports degrees C
        //    If using Fahrenheit, us SetTem, TemUn and TemRec to 
        //    reconstruct the Fahrenheit temperature
        // Get Celsius or Fahrenheit from status message
        Integer CorF = GetIntStatusVal("TemUn");
        Integer newVal = GetIntStatusVal("SetTem");
        Integer halfStep = GetIntStatusVal("TemRec");

        if (CorF == null  || newVal == null || halfStep == null){
            logger.warn("SetTem,TemUn or TemRec is invalid, not performing conversion");
        }
        else if (CorF == 1){    //convert SetTem to Fahrenheit
            // Find the valueName in the Returned Status object
            String columns[] = statusResponseGson.packJson.cols;
            Integer values[] = statusResponseGson.packJson.dat;
            List<String> colList = new ArrayList<>(Arrays.asList(columns));
            int valueArrayposition = colList.indexOf("SetTem");
            if (valueArrayposition != -1) {
                //convert Celsius to Fahrenheit,
                //SetTem status returns degrees C regardless of TempUn setting
                
                // Perform the float Celsius to Fahrenheit conversion
                //     add or subtract 0.5 based on the value of TemRec
                //     (0 = -0.5, 1 = +0.5)
                //     Pass into a rounding function, this yeild the correct Fahrenheit 
                //     Temperature to match A/C display
                newVal = new Integer((int) Math.round(((newVal*9.0/5.0)+32.0)+ halfStep-0.5));
                
                //Update the status array with F temp , 
                //    assume this is updating the array in situ
                values[valueArrayposition] = newVal;
            }
        }
    }
}
