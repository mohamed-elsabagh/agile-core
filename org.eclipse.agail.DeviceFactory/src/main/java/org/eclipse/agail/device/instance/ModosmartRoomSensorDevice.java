/*******************************************************************************
 * Copyright (C) 2017 Create-Net / FBK.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Create-Net / FBK - initial API and implementation
 ******************************************************************************/
package org.eclipse.agail.device.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.freedesktop.dbus.DBusSigHandler;
import org.freedesktop.dbus.exceptions.DBusException;
import org.eclipse.agail.Device;
import org.eclipse.agail.Protocol;
import org.eclipse.agail.Protocol.NewRecordSignal;
import org.eclipse.agail.device.base.AgileBLEDevice;
import org.eclipse.agail.device.base.SensorUuid;
import org.eclipse.agail.object.DeviceDefinition;
import org.eclipse.agail.object.DeviceOverview;
import org.eclipse.agail.object.RecordObject;
import org.eclipse.agail.object.DeviceComponent;
import org.eclipse.agail.exception.AgileNoResultException;

public class ModosmartRoomSensorDevice extends AgileBLEDevice implements Device {
  protected Logger logger = LoggerFactory.getLogger(ModosmartRoomSensorDevice.class);
  protected static final Map<String, SensorUuid> sensors = new HashMap<String, SensorUuid>();
  private static final String Presence = "Presence";
  private static final String Temperature = "Temperature";
  private static final String Humidity = "Humidity";

  private static final String BatteryLevel = "Battery Level";

  private static final String FirmwareRevision = "Firmware Revision";


	{
		subscribedComponents.put(Presence, 0);
		subscribedComponents.put(Temperature, 0);
		subscribedComponents.put(Humidity, 0);

		subscribedComponents.put(BatteryLevel, 0);

		subscribedComponents.put(FirmwareRevision, 0);
	}

 	{
		profile.add(new DeviceComponent(Presence, ""));
		profile.add(new DeviceComponent(Temperature, ""));
		profile.add(new DeviceComponent(Humidity, ""));

		profile.add(new DeviceComponent(BatteryLevel, ""));

		profile.add(new DeviceComponent(FirmwareRevision, ""));
	}


 	static {
		sensors.put(Presence, new SensorUuid("0000a000-0000-1000-8000-00805f9b34fb", "0000a001-0000-1000-8000-00805f9b34fb", "", ""));
		sensors.put(Temperature, new SensorUuid("0000a000-0000-1000-8000-00805f9b34fb", "00002a6e-0000-1000-8000-00805f9b34fb", "", ""));
		sensors.put(Humidity, new SensorUuid("0000a000-0000-1000-8000-00805f9b34fb", "00002a6f-0000-1000-8000-00805f9b34fb", "", ""));

		sensors.put(BatteryLevel, new SensorUuid("0000180f-0000-1000-8000-00805f9b34fb", "00002a19-0000-1000-8000-00805f9b34fb", "", ""));

		sensors.put(FirmwareRevision, new SensorUuid("0000180a-0000-1000-8000-00805f9b34fb", "00002a26-0000-1000-8000-00805f9b34fb", "", ""));
	}

	public static boolean Matches(DeviceOverview d) {
		return d.name.contains("MODOSMART_ROOM_SENSOR");
	}

	public static String deviceTypeName = "MODOSMART_ROOM_SENSOR";

	public ModosmartRoomSensorDevice(DeviceOverview deviceOverview) throws DBusException {
		super(deviceOverview);
	}


	public ModosmartRoomSensorDevice(DeviceDefinition devicedefinition) throws DBusException {
		super(devicedefinition);
	}

	@Override
	public void Connect() throws DBusException {
		super.Connect();
		for (String componentName : subscribedComponents.keySet()) {
			logger.info("MODOSMART_ROOM_SENSOR Connect: " + componentName);
			//DeviceRead(componentName);
			if (subscribedComponents.get(componentName) > 0) {
				logger.info("Resubscribing to {}", componentName);
				deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
			}
		}
	}


  @Override
  protected String DeviceRead(String componentName) {
	logger.info("MODOSMART_ROOM_SENSOR DeviceRead: "+ componentName);
    if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            byte[] result = deviceProtocol.Read(address, getReadValueProfile(componentName));
            return formatReading(componentName, result);
          } catch (DBusException e) {
            e.printStackTrace();
          }
        } else {
          throw new AgileNoResultException("Sensor not supported:" + componentName);
        }
      } else {
        throw new AgileNoResultException("BLE Device not connected: " + deviceName);
      }
    } else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
    }
    throw new AgileNoResultException("Unable to read "+componentName);
  }


 @Override
  public synchronized void Subscribe(String componentName) {
    if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            if (!hasOtherActiveSubscription(componentName)) {
              deviceProtocol.Subscribe(address, getReadValueProfile(componentName));
              addNewRecordSignalHandler();
            }
	    logger.info("MODOSMART_ROOM_SENSOR Subscribe");
            subscribedComponents.put(componentName, subscribedComponents.get(componentName) + 1);
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          throw new AgileNoResultException("Sensor not supported:" + componentName);
        }
      } else {
        throw new AgileNoResultException("BLE Device not connected: " + deviceName);
      }
    } else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
    }
  }

 @Override
  public synchronized void Unsubscribe(String componentName) throws DBusException {
    if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
      if (isConnected()) {
        if (isSensorSupported(componentName.trim())) {
          try {
            subscribedComponents.put(componentName, subscribedComponents.get(componentName) - 1);
            if (!hasOtherActiveSubscription(componentName)) {
              deviceProtocol.Unsubscribe(address, getReadValueProfile(componentName));
              removeNewRecordSignalHandler();
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        } else {
          throw new AgileNoResultException("Sensor not supported:" + componentName);
        }
      } else {
        throw new AgileNoResultException("BLE Device not connected: " + deviceName);
      }
    } else {
      throw new AgileNoResultException("Protocol not supported: " + protocol);
    }
  }

@Override
  public void Write(String componentName, String payload) {
            logger.debug("Device. Write not implemented");
	}

@Override
  public void Execute(String command) {
            logger.debug("Device. Execute not implemented");
	}

  @Override
  public List<String> Commands(){
            logger.debug("Device. Commands not implemented");
            return null;
      }

	// =======================Utility methods===========================

	private Map<String, String> getReadValueProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charValueUuid);
			logger.info("MODOSMART_ROOM_SENSOR Gatt Service: "+s.serviceUuid);
			logger.info("MODOSMART_ROOM_SENSOR Gatt Characteristic: "+s.charValueUuid);
		}
		return profile;
	}

	@Override
	protected boolean isSensorSupported(String sensorName) {
		return sensors.containsKey(sensorName);
	}



	/**
	 * Checks if there is another active subscription on the given component of
	 * the device
	 *
	 * @param componentName
	 * @return
	 */
	@Override
	protected boolean hasOtherActiveSubscription(String componentName) {
		for (String component : subscribedComponents.keySet()) {
			if (subscribedComponents.get(component) > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected String formatReading(String componentName, byte[] readData) {
    short int_result;
    double result;
    String value;
		switch (componentName) {
         case Presence:
            int_result = (short)(((readData[1] & 0xFF) << 8) | (readData[0] & 0xFF));
            value = Integer.toString(int_result);
            return value;
   		   case Temperature:
            int_result = (short)(((readData[1] & 0xFF) << 8) | (readData[0] & 0xFF));
            result = int_result * 0.01;
            value = String.valueOf(result);
            return value;
		     case Humidity:
            int_result = (short)(((readData[1] & 0xFF) << 8) | (readData[0] & 0xFF));
            result = int_result * 0.01;
            value = String.valueOf(result);
            return value;
		     case BatteryLevel:
            int_result = (short)((readData[0] & 0xFF));
            value = Integer.toString(int_result);
            return value;
		     case FirmwareRevision:
            value = new String(readData);
            return value;
		}
		return "0";
	}

	/**

	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void addNewRecordSignalHandler() {
		logger.info("MODOSMART_ROOM_SENSOR addNewRecordSignalHandler");
		try {
			if (newRecordSigHanlder == null && connection != null) {
 				newRecordSigHanlder = new DBusSigHandler<Protocol.NewRecordSignal>() {
					@Override
					public void handle(NewRecordSignal sig) {
						if (sig.address.equals(address)) {
 							for(String componentName : getComponentNames(sig.profile)){
 								String readVal = formatReading(componentName, sig.record);
 								if (Float.parseFloat(readVal) != 0.0) {
 									RecordObject recObj = new RecordObject(deviceID, componentName,
 											formatReading(componentName, sig.record), getMeasurementUnit(componentName), "",
 											System.currentTimeMillis());
 									data = recObj;
 									logger.info("Device notification component {} value {}", componentName, recObj.value);
 									lastReadStore.put(componentName, recObj);
 									try {
 										Device.NewSubscribeValueSignal newRecordSignal = new Device.NewSubscribeValueSignal(
 												AGILE_NEW_RECORD_SUBSCRIBE_SIGNAL_PATH, recObj);
 										connection.sendSignal(newRecordSignal);
 									} catch (DBusException e) {
 										e.printStackTrace();
 									}
 								}
 							}
						}
					}
				};
				connection.addSigHandler(Protocol.NewRecordSignal.class, newRecordSigHanlder);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Given the profile of the component returns the list of component names
	 *
	 * @param uuid
	 * @return
	 */
 	protected List<String> getComponentNames(Map<String, String> profile) {
		List<String> ret = new ArrayList<String>();
		String serviceUUID = profile.get(GATT_SERVICE);
		String charValueUuid = profile.get(GATT_CHARACTERSTICS);
		for (Entry<String, SensorUuid> su : sensors.entrySet()) {
			if (su.getValue().serviceUuid.equals(serviceUUID) && su.getValue().charValueUuid.equals(charValueUuid)) {
				ret.add(su.getKey());
			}
		}
		return ret;
	}

 	@Override
	protected String getMeasurementUnit(String sensor) {
 		return "";
 	}

  /**
   * Given the profile of the component returns the name of the sensor
   *
   * @param uuid
   * @return
   */
  @Override
  protected String getComponentName(Map<String, String> profile) {
    String serviceUUID = profile.get(GATT_SERVICE);
    String charValueUuid = profile.get(GATT_CHARACTERSTICS);
    for (Entry<String, SensorUuid> su : sensors.entrySet()) {
      if (su.getValue().serviceUuid.equals(serviceUUID) && su.getValue().charValueUuid.equals(charValueUuid)) {
        return su.getKey();
      }
    }
    return null;
  }
}
