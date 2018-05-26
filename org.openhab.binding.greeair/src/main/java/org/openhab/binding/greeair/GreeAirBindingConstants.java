/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair;

//import org.eclipse.jdt.annotation.NonNullByDefault;
//import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link GreeAirBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author John Cunha - Initial contribution
 */
// @NonNullByDefault
public class GreeAirBindingConstants {

    private static final String BINDING_ID = "greeair";

    public static final ThingTypeUID THING_TYPE_GREEAIRCON = new ThingTypeUID(BINDING_ID, "greeair");

    /**
     * Contains the Port that is used to communicate using UDP with Gree Airconditioners. .
     */
    public static final int GREE_PORT = 7000;

    /**
     * Contains the character set to be used to communicate with Gree Airconditioners.
     */
    public static final String CHARSET = "UTF-8";

    /**
     * Contains the refresh rate to be used for retrieving data from the Gree Airconditioner.
     */
    public static final String REFRESH_TIME = "refresh";

    /*
     * The minimum refresh time in milliseconds. Any REFRESH command send to a Thing, before
     * this time has expired, will not trigger an attempt to request status data from the
     * Gree Airconditioner.
     **/
    public static final int MINIMUM_REFRESH_TIME = 1000;

    /*
     * The default refresh time in milliseconds to be used for Discovery when the user has no option to set the value.
     **/
    public static final int DEFAULT_REFRESH_TIME = 1000;

    /*
     * The timeout for the Datagram socket used to communicate with Gree Airconditioners.
     * This is particularly important when scanning for devices because this will effectively
     * be the amount of time spent scanning.
     **/
    public static final int DATAGRAM_SOCKET_TIMEOUT = 5000;

    /*
     * The IP Address used to used to send Scan Datagram to Gree Airconditioners.
     **/
    // public static final String DATAGRAM_BROADCAST_IP_ADDRESS = "192.168.1.255";

    // List of all Thing Type UIDs
    public static final ThingTypeUID GREE_THING_TYPE = new ThingTypeUID(BINDING_ID, "greeairthing");

    /**
     * Contains the IP network address of the Gree Airconditioner.
     */
    public static final String THING_PROPERTY_IP = "ipAddress";

    // List of all Channel ids
    public static final String POWER_CHANNEL = "powerchannel";
    public static final String MODE_CHANNEL = "modechannel";
    public static final String TURBO_CHANNEL = "turbochannel";
    public static final String LIGHT_CHANNEL = "lightchannel";
    public static final String TEMP_CHANNEL = "tempchannel";
    public static final String SWINGV_CHANNEL = "swingverticalchannel";
    public static final String WINDSPEED_CHANNEL = "windspeedchannel";
    public static final String AIR_CHANNEL = "airchannel";
    public static final String DRY_CHANNEL = "drychannel";
    public static final String HEALTH_CHANNEL = "healthchannel";
    public static final String PWRSAV_CHANNEL = "pwrsavchannel";

}
