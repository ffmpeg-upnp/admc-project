/*
 * Copyright (C) 2010 Teleal GmbH, Switzerland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.teleal.cling.model.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.teleal.cling.model.Constants;

/**
 * Device type with a fixed <code>schemas-upnp-org</code> namespace.
 *
 * @author Christian Bauer
 */
public class UDADeviceType extends DeviceType {

    public static final String DEFAULT_NAMESPACE = "schemas-upnp-org";

    // This pattern also accepts decimal versions, not only integers (as would be required by UDA), but cuts off fractions
    public static final Pattern PATTERN =
            Pattern.compile("urn:" + DEFAULT_NAMESPACE + ":device:(" + Constants.REGEX_TYPE + "):([0-9]+).*");

    public UDADeviceType(String type) {
        super(DEFAULT_NAMESPACE, type, 1);
    }

    public UDADeviceType(String type, int version) {
        super(DEFAULT_NAMESPACE, type, version);
    }

    public static UDADeviceType valueOf(String s) throws InvalidValueException {
        Matcher matcher = PATTERN.matcher(s);
        if (matcher.matches()) {
            return new UDADeviceType(matcher.group(1), Integer.valueOf(matcher.group(2)));
        } else {
            throw new InvalidValueException("Can't parse UDA device type string (namespace/type/version): " + s);
        }
    }

}
