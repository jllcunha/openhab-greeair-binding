/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.internal;

/**
 * The GreeAirconConfig is responsible for storing the thing configuration.
 *
 * @author John Cunha - Initial contribution
 */
public class GreeAirconConfig {
    private String ipAddress;
    private Integer refresh;

    public String getIpAddress() {
        return ipAddress;
    }

    public void setRefresh(Integer refresh) {
        this.refresh = refresh;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getRefresh() {
        return refresh;
    }

    public boolean isValid() {
        try {
            if (ipAddress.isEmpty()) {
                return false;
            }
            if (refresh.intValue() <= 0) {
                throw new IllegalArgumentException("Refresh time must be positive number!");
            }
            return true;
        } catch (Exception err) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "GreeAirconConfig{ipAddress=" + ipAddress + "}";
    }
}
