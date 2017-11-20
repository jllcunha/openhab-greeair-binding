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
 * The GreeExecResponsePack4Gson class is used by Gson to hold values returned from
 * the Air Conditioner during requests for Execution of Commands to the
 * Air Conditioner.
 *
 * @author John Cunha - Initial contribution
 */
public class GreeExecResponsePack4Gson {
    public String t = null;
    public String mac = null;
    public int r = 0;
    public String[] opt = null;
    public Integer[] p = null;
    public Integer[] val = null;
}
