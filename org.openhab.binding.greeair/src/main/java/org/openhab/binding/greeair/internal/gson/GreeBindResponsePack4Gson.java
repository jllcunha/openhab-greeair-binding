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
 * The GreeBindResponsePack4Gson class is used by Gson to hold values returned from
 * the Air Conditioner during Binding
 *
 * @author John Cunha - Initial contribution
 */
public class GreeBindResponsePack4Gson {
    public String t = null;
    public String mac = null;
    public String key = null;
    public int r = 0;
}
