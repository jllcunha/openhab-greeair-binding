/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.handler;

import static org.openhab.binding.greeair.GreeAirBindingConstants.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.greeair.internal.GreeAirconConfig;
import org.openhab.binding.greeair.internal.discovery.GreeDevice;
import org.openhab.binding.greeair.internal.discovery.GreeDeviceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link GreeAirHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author John Cunha - Initial contribution
 */
// @NonNullByDefault
public class GreeAirHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(GreeAirHandler.class);
    private GreeDeviceFinder deviceFinder = null;
    private GreeDevice thisDevice = null;
    private GreeAirconConfig config;
    private Integer refreshTime;
    private ScheduledFuture<?> refreshTask;
    private boolean firstUpdatefinished = false;
    private long lastRefreshTime = 0;
    private String ipAddress = null;
    private String broadcastAddress = null;
    DatagramSocket clientSocket = null;

    @SuppressWarnings(value = { "null" })

    public GreeAirHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            // The thing is updated by the scheduled automatic refresh so do nothing here.
        } else if (channelUID.getId().equals(POWER_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDevicePower(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDevicePower(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(MODE_CHANNEL)) {
            try {
                int val = ((DecimalType) command).intValue();
                thisDevice.SetDeviceMode(clientSocket, val);
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(TURBO_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDeviceTurbo(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDeviceTurbo(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(LIGHT_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDeviceLight(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDeviceLight(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(TEMP_CHANNEL)) {
            try {
                int val = ((DecimalType) command).intValue();
                thisDevice.SetDeviceTempSet(clientSocket, val);
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(SWINGV_CHANNEL)) {
            try {
                int val = ((DecimalType) command).intValue();
                thisDevice.SetDeviceSwingVertical(clientSocket, val);
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(WINDSPEED_CHANNEL)) {
            try {
                int val = ((DecimalType) command).intValue();
                thisDevice.SetDeviceWindspeed(clientSocket, val);
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(AIR_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDeviceAir(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDeviceAir(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(DRY_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDeviceDry(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDeviceDry(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(HEALTH_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDeviceHealth(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDeviceHealth(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        } else if (channelUID.getId().equals(PWRSAV_CHANNEL)) {
            try {
                switch (command.toString()) {
                    case "ON":
                        thisDevice.SetDevicePwrSaving(clientSocket, 1);
                        break;
                    case "OFF":
                        thisDevice.SetDevicePwrSaving(clientSocket, 0);
                        break;
                }
            } catch (Exception e) {
                logger.debug("Greeair failed to update channel {} due to {} ", channelUID.getId(), e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("GreeAirconHandler for {} is initializing", thing.getUID());

        config = getConfig().as(GreeAirconConfig.class);
        logger.debug("GreeAirconHandler config for {} is {}", thing.getUID(), config);

        if (!config.isValid()) {
            logger.debug("GreeAirconHandler config of {} is invalid. Check configuration", thing.getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Invalid GreeAircon config. Check configuration.");
            return;
        }
        ipAddress = config.getIpAddress();
        refreshTime = config.getRefresh();
        broadcastAddress = config.getBroadcastAddress();

        // Now Scan For Airconditioners
        try {
            // First calculate the Broadcast address based on the available interfaces
            InetAddress broadcastIp = InetAddress.getByName(broadcastAddress);

            // Create a new Datagram socket with a specified timeout
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(DATAGRAM_SOCKET_TIMEOUT);

            // Firstly, lets find all Gree Airconditioners on the network
            deviceFinder = new GreeDeviceFinder(broadcastIp);
            deviceFinder.Scan(clientSocket);
            logger.debug("GreeAircon found {} Gree Devices during scanning", deviceFinder.GetScannedDeviceCount());

            // Now check that this one is amongst the air conditioners that responded.
            thisDevice = deviceFinder.GetDeviceByIPAddress(ipAddress);
            if (thisDevice != null) {
                // Ok, our device responded
                // Now let's Bind with it
                thisDevice.BindWithDevice(clientSocket);
                if (thisDevice.getIsBound()) {
                    logger.info("Gree AirConditioner Device {} from was Succesfully bound", thing.getUID());
                    updateStatus(ThingStatus.ONLINE);

                    // Start the automatic refresh cycles
                    startAutomaticRefresh();
                    return;
                }
            }
        } catch (UnknownHostException e) {
            logger.debug("Greeair failed to scan for airconditioners due to {} ", e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            logger.debug("Greeair failed to scan for airconditioners due to {} ", e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.debug("Greeair failed to scan for airconditioners due to {} ", e.getMessage());
            e.printStackTrace();
        }
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void dispose() {
        logger.debug("GreeAircon for {} is disposing", thing.getUID());
        clientSocket.close();
        if (refreshTask != null) {
            refreshTask.cancel(true);
        }
        lastRefreshTime = 0;
    }

    private boolean isMinimumRefreshTimeExceeded() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastRefresh = currentTime - lastRefreshTime;
        if (timeSinceLastRefresh < MINIMUM_REFRESH_TIME) {
            return false;
        }
        lastRefreshTime = currentTime;
        return true;
    }

    private void startAutomaticRefresh() {

        Runnable refresher = new Runnable() {
            @Override
            public void run() {

                try {
                    logger.debug("Greeair executing automatic update of values");
                    // safeguard for multiple REFRESH commands
                    if (isMinimumRefreshTimeExceeded()) {
                        logger.debug("Fetching status values from device.");
                        // Get the current status from the Airconditioner
                        thisDevice.getDeviceStatus(clientSocket);
                    } else {
                        logger.debug(
                                "Skipped fetching status values from device because minimum refresh time not reached");
                    }

                    // Update All Channels
                    List<Channel> channels = getThing().getChannels();
                    for (Channel channel : channels) {
                        publishChannelIfLinked(channel.getUID());
                    }

                } catch (Exception e) {
                    logger.debug("Greeair failed during automatic update of airconditioner values due to {} ",
                            e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        refreshTask = scheduler.scheduleWithFixedDelay(refresher, 0, refreshTime.intValue(), TimeUnit.SECONDS);
        logger.debug("Start Greeair automatic refresh with {} second intervals", refreshTime.intValue());
    }

    private void publishChannelIfLinked(ChannelUID channelUID) {
        String channelID = channelUID.getId();
        boolean statusChanged = false;
        if (channelID != null && isLinked(channelID)) {
            State state = null;
            Integer stateValue = null;
            switch (channelID) {
                case POWER_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Pow")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Pow");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
                case MODE_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Mod")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Mod");
                        state = new DecimalType(stateValue);
                    }
                    break;
                case TURBO_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Tur")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Tur");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
                case LIGHT_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Lig")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Lig");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
                case TEMP_CHANNEL:
                    if (thisDevice.HasStatusValChanged("SetTem")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("SetTem");
                        state = new DecimalType(stateValue);
                    }
                    break;
                case SWINGV_CHANNEL:
                    if (thisDevice.HasStatusValChanged("SwUpDn")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("SwUpDn");
                        state = new DecimalType(stateValue);
                    }
                    break;
                case WINDSPEED_CHANNEL:
                    if (thisDevice.HasStatusValChanged("WdSpd")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("WdSpd");
                        state = new DecimalType(stateValue);
                    }
                    break;
                case AIR_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Air")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Air");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
                case DRY_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Blo")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Blo");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
                case HEALTH_CHANNEL:
                    if (thisDevice.HasStatusValChanged("Health")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("Health");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
                case PWRSAV_CHANNEL:
                    if (thisDevice.HasStatusValChanged("SvSt")) {
                        statusChanged = true;
                        stateValue = thisDevice.GetIntStatusVal("SvSt");
                        if (stateValue.intValue() != 1) {
                            state = new StringType("OFF");
                        } else {
                            state = new StringType("ON");
                        }
                    }
                    break;
            }
            if (state != null && statusChanged == true) {
                updateState(channelID, state);
            }
        }
    }
}
