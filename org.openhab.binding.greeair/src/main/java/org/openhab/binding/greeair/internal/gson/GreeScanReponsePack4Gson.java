/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.internal.gson;

/**
 *
 * The GreeScanReponsePack4Gson class is used by Gson to hold values returned by
 * the Air Conditioner during Scan Requests to the Air Conditioner.
 *
 * @author John Cunha - Initial contribution
 */
public class GreeScanReponsePack4Gson {

    public String t = null;
    public String cid = null;
    public String bc = null;
    public String brand = null;
    public String catalog = null;
    public String mac = null;
    public String mid = null;
    public String model = null;
    public String name = null;
    public String series = null;
    public String vender = null;
    public String ver = null;
    public int lock = 0;
}
