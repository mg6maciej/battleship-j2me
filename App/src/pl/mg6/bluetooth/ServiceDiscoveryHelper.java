/*
 * Copyright (C) 2013 Maciej Górski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.mg6.bluetooth;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.bluetooth.*;
import pl.mg6.Application;

/**
 *
 * @author maciej
 */
public final class ServiceDiscoveryHelper implements DiscoveryListener {

	private LocalDevice localDevice;
	private DiscoveryAgent discoveryAgent;
	private int discoverableMode;

	private static final int[] attrServiceName = new int[] { 0x100 };
	private UUID[] serviceUuid;
	private int transId;
	private final Vector devices = new Vector();
	private final Vector cached = new Vector();
	private final Vector services = new Vector();
	private boolean doInquiryLater;

	//#mdebug
	private static final String[] returnCodes = {
		"inquiry completed", "completed", "terminated", "error", "no records",
		"inquiry terminated", "device not reachable", "inquiry error"
	};
	
	private void debug(String str, Throwable ex) {
		Application.debug(str, ex);
	}
	//#enddebug

	public ServiceDiscoveryHelper() throws BluetoothStateException {
//		//#mdebug
//		String btApiVer = LocalDevice.getProperty("bluetooth.api.version");
//		if (true || btApiVer != null && btApiVer.indexOf("1.1") != -1) {
//			try {
//				Bluetooth11 bt11 = (Bluetooth11) Class.forName("pl.mg6.bluetooth.Bluetooth11Impl").newInstance();
//				boolean powerOn = bt11.isPowerOn();
//				//#debug
//				debug("SDH.bt11.isPowerOn: " + powerOn, null);
//			} catch (Throwable ex) {
//				debug("SDH.bt11", ex);
//			}
//		} else {
//			debug("SDH.btApiVer: " + btApiVer, null);
//		}
//		//#enddebug
		localDevice = LocalDevice.getLocalDevice();
		//#mdebug
		String name = localDevice.getFriendlyName();
		debug("SDH.getFriendlyName: " + name, null);
		String address = localDevice.getBluetoothAddress();
		debug("SDH.getBluetoothAddress: " + address, null);
		//#enddebug
		discoveryAgent = localDevice.getDiscoveryAgent();
		discoverableMode = localDevice.getDiscoverable();
		//#debug
		debug("localDevice.getDiscoverable: " + Integer.toHexString(discoverableMode), null);
		if (!setDiscoverable(true)) {
			throw new BluetoothStateException();
		}
		setDiscoverable(false);
	}

	public String getMyName() throws BluetoothStateException {
		String name = localDevice.getFriendlyName();
		StringBuffer sb = new StringBuffer(name);
		String map = "ĄAĆCĘEŁLŃNÓOŚSŹZŻZąaćcęełlńnóośsźzżz";
		for_each_character:
		for (int i = sb.length() - 1; i >= 0; i--) {
			char c = sb.charAt(i);
			if (!(('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')
					|| ('0' <= c && c <= '9') || c == ' ' || c == '-' || c == '_')) {
				for (int j = 0; j < map.length(); j += 2) {
					if (c == map.charAt(j)) {
						sb.setCharAt(i, map.charAt(j + 1));
						continue for_each_character;
					}
				}
				sb.deleteCharAt(i);
			}
		}
		//#debug
		debug("getMyName: " + name + "/" + sb.toString(), null);
		return sb.toString();
	}

	public boolean setDiscoverable(boolean discoverable) throws BluetoothStateException {
		int mode = discoverable ? DiscoveryAgent.GIAC : DiscoveryAgent.NOT_DISCOVERABLE;
		boolean ret = localDevice.setDiscoverable(mode);
		//#debug
		debug("localDevice.setDiscoverable " + mode + ": " + ret, null);
		return ret;
	}

	public void start(UUID uuid) {
		try {
			serviceUuid = new UUID[] { uuid };
			devices.removeAllElements();
			cached.removeAllElements();
			services.removeAllElements();
			//retrieveDevices(DiscoveryAgent.PREKNOWN);
			retrieveDevices(DiscoveryAgent.CACHED);
			doInquiryLater = !devices.isEmpty();
			if (doInquiryLater) {
				searchService();
			} else {
				transId = -1;
				boolean ret = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
				//#debug
				debug("discoveryAgent.startInquiry: " + ret, null);
			}
		} catch (BluetoothStateException ex) {
			//#debug
			debug("SDH.start", ex);
			serviceUuid = null;
		}
	}

	public void stop() {
		if (isSearching()) {
			if (transId == -1) {
				boolean ret = discoveryAgent.cancelInquiry(this);
				//#debug
				debug("discoveryAgent.cancelInquiry: " + ret, null);
			} else {
				boolean ret = discoveryAgent.cancelServiceSearch(transId);
				//#debug
				debug("discoveryAgent.cancelServiceSearch(" + transId + "): " + ret, null);
			}
			serviceUuid = null;
		}
		transId = -1;
		devices.removeAllElements();
		cached.removeAllElements();
		services.removeAllElements();
	}

	public boolean isSearching() {
		return serviceUuid != null;
	}

	public int getServiceCount() {
		return services.size();
	}

	public String getBluetoothAddress(int index) {
		ServiceRecord sr = (ServiceRecord) services.elementAt(index);
		return sr.getHostDevice().getBluetoothAddress();
	}

	public String getName(int index) {
		ServiceRecord sr = (ServiceRecord) services.elementAt(index);
		return (String) sr.getAttributeValue(attrServiceName[0]).getValue();
	}

	public String getConnectionURL(int index) {
		ServiceRecord sr = (ServiceRecord) services.elementAt(index);
		int security = ServiceRecord.AUTHENTICATE_NOENCRYPT;
		boolean master = false;
		String connectionURL = sr.getConnectionURL(security, master);
		if (connectionURL == null) {
			StringBuffer sb = new StringBuffer(69);
			sb.append("btspp://");
			sb.append(sr.getHostDevice().getBluetoothAddress());
			sb.append(':');
			sb.append(getConnectionChannelNumber(sr));
			sb.append(";authenticate=");
			sb.append(security != ServiceRecord.NOAUTHENTICATE_NOENCRYPT);
			sb.append(";encrypt=");
			sb.append(security == ServiceRecord.AUTHENTICATE_ENCRYPT);
			sb.append(";master=");
			sb.append(master);
			connectionURL = sb.toString();
		}
		return connectionURL;
	}

	private static long getConnectionChannelNumber(ServiceRecord sr) {
		long channelNumber;
		DataElement de = sr.getAttributeValue(0x0004);
		Enumeration e = (Enumeration) de.getValue();
		e.nextElement();
		de = (DataElement) e.nextElement();
		e = (Enumeration) de.getValue();
		e.nextElement();
		de = (DataElement) e.nextElement();
		int dt = de.getDataType();
		if (dt == DataElement.U_INT_8 || dt == DataElement.U_INT_16 || dt == DataElement.INT_16) {
			byte[] value = (byte[]) de.getValue();
			channelNumber = value[0];
		} else {
			channelNumber = de.getLong();
		}
		return channelNumber;
	}

	private void retrieveDevices(int option) {
		RemoteDevice[] rds = discoveryAgent.retrieveDevices(option);
		if (rds != null) {
			for (int i = 0; i < rds.length; i++) {
				//#debug
				debug((option == DiscoveryAgent.PREKNOWN ? "preknown " : "cached ") + rds[i].getBluetoothAddress(), null);
				put(rds[i], true);
			}
		}
	}

	private void put(RemoteDevice rd, boolean cache) {
		boolean found = false;
		for (int i = 0; i < devices.size(); i++) {
			if (rd.equals((RemoteDevice) devices.elementAt(i))) {
				found = true;
				break;
			}
		}
		if (!found) {
			for (int i = 0; i < cached.size(); i++) {
				if (rd.equals((RemoteDevice) cached.elementAt(i))) {
					found = true;
					break;
				}
			}
		}
		if (!found) {
			devices.addElement(rd);
			if (cache) {
				cached.addElement(rd);
			}
		}
	}

	private void searchService() {
		if (!devices.isEmpty()) {
			RemoteDevice rd = (RemoteDevice) devices.firstElement();
			devices.removeElementAt(0);
			try {
				transId = discoveryAgent.searchServices(attrServiceName, serviceUuid, rd, this);
				//#debug
				debug("discoveryAgent.searchServices: " + transId, null);
			} catch (NullPointerException ex) {
				//#debug
				debug("SDH.searchService (on Sun emulators)", ex);
				searchService();
			} catch (BluetoothStateException ex) {
				//#debug
				debug("SDH.searchService.searchServices", ex);
				serviceUuid = null;
			}
		} else if (doInquiryLater) {
			try {
				transId = -1;
				boolean ret = discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
				//#debug
				debug("discoveryAgent.startInquiry: " + ret, null);
				doInquiryLater = false;
			} catch (BluetoothStateException ex) {
				//#debug
				debug("SDH.searchService.startInquiry", ex);
				serviceUuid = null;
			}
		} else {
			serviceUuid = null;
		}
	}

	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		//#mdebug
		String name = null;
		try {
			name = btDevice.getFriendlyName(false);
		} catch (IOException ex) {
		}
		debug("dd: " + btDevice.getBluetoothAddress()
				+ " " + Integer.toHexString(cod.getMajorDeviceClass())
				+ " " + Integer.toHexString(cod.getMinorDeviceClass())
				+ " " + Integer.toHexString(cod.getServiceClasses())
				+ (name != null ? " (" + name + ")" : ""), null);
		//#enddebug
		if (cod.getMajorDeviceClass() == 0x200) {
			put(btDevice, false);
		}
	}

	public void inquiryCompleted(int discType) {
		//#debug
		debug("ic: " + discType + " (" + returnCodes[discType] + ")", null);
		if (serviceUuid != null && discType != INQUIRY_ERROR && discType != INQUIRY_TERMINATED) {
			searchService();
		} else {
			serviceUuid = null;
		}
	}

	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		//#debug
		debug("sd: " + transID + (servRecord != null ? " count: " + servRecord.length : ""), null);
		services.addElement(servRecord[0]);
		//#mdebug
		int[] attrIds = servRecord[0].getAttributeIDs();
		for (int j = 0; j < attrIds.length; j++) {
			DataElement de = servRecord[0].getAttributeValue(attrIds[j]);
			System.out.print(attrIds[j]);
			printDataElement(de, "");
		}
		//#enddebug
	}

	//#mdebug
	private void printDataElement(DataElement de, String tab) {
		int dt = de.getDataType();
		System.out.print(tab + "(" + dt + ") --> ");
		if (dt == DataElement.BOOL) {
			System.out.println(de.getBoolean());
		} else if (dt == DataElement.U_INT_1
				|| dt == DataElement.U_INT_2 || dt == DataElement.U_INT_4
				|| dt == DataElement.INT_1 || dt == DataElement.INT_2
				|| dt == DataElement.INT_4 || dt == DataElement.INT_8) {
			System.out.println(de.getLong());
		} else {
			Object value = de.getValue();
			System.out.println(value);
			if (value instanceof Enumeration) {
				Enumeration e = (Enumeration) value;
				while (e.hasMoreElements()) {
					de = (DataElement) e.nextElement();
					printDataElement(de, tab + "  ");
				}
			}
		}
	}
	//#enddebug

	public void serviceSearchCompleted(int transID, int respCode) {
		//#debug
		debug("ssc: " + transID + " " + respCode + " (" + returnCodes[respCode] + ")", null);
		if (serviceUuid != null && respCode != SERVICE_SEARCH_ERROR && respCode != SERVICE_SEARCH_TERMINATED) {
			searchService();
		} else {
			serviceUuid = null;
		}
	}
}
