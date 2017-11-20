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
 * The GreeReqStatusPack4Gson class is used by Gson to hold values to be send to
 * the Air Conditioner during requests for Status Updates to the
 * Air Conditioner.
 *
 * @author John Cunha - Initial contribution
 */
public class GreeReqStatusPack4Gson {

    public String t = null;
    public String[] cols = null;
    public String mac = null;

}
