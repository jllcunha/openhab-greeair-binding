/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.greeair.internal.gson;

/**
 *
 * The GreeStatusResponsePack4Gson class is used by Gson to hold values returned from
 * the Air Conditioner during requests for Status Updates to the
 * Air Conditioner.
 *
 * @author John Cunha - Initial contribution
 */
public class GreeStatusResponsePack4Gson {

    public GreeStatusResponsePack4Gson(GreeStatusResponsePack4Gson other) {
        cols = new String[other.cols.length];
        dat = new Integer[other.dat.length];
        System.arraycopy(other.cols, 0, cols, 0, other.cols.length);
        System.arraycopy(other.dat, 0, dat, 0, other.dat.length);
    }

    public String t = null;
    public String mac = null;
    public int r = 0;
    public String[] cols = null;
    public Integer[] dat = null;
}
