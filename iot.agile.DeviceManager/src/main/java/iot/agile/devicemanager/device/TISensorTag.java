package iot.agile.devicemanager.device;

import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;

import iot.agile.Device;
import iot.agile.object.DeviceDefinition;

public class TISensorTag extends DeviceImp implements Device {

	private static final String GATT_SERVICE = "GATT_SERVICE";

	private static final String GATT_CHARACTERSTICS = "GATT_CHARACTERSTICS";

	private static final String PAYLOAD = "PAYLOAD";

        private static final String TEMPERATURE = "Temperature";
        private static final String ACCELEROMETER = "Accelerometer";
        private static final String HUMIDITY = "Humidity";
        private static final String MAGNETOMETER = "Magnetometer";
        private static final String PRESSURE = "Pressure";
        private static final String GYROSCOPE = "Gyroscope";
        private static final String OPTICAL = "Optical";

	private static class SensorUuid {
		public String serviceUuid;
		public String charUuid;
		public String charConfigUuid;
		public String charFreqUuid;

		public SensorUuid(String service, String ch, String charConfig, String charFreq) {
			serviceUuid = service;
			charUuid = ch;
			charConfigUuid = charConfig;
			charFreqUuid = charFreq;
		}
	}

	private static final Map<String, String> componentUnits = new HashMap<String, String>();
	static {
		componentUnits.put(TEMPERATURE, "Degree celsius (°C)");
		componentUnits.put(ACCELEROMETER, "");
		componentUnits.put(HUMIDITY, "Relative humidity (%RH)");
		componentUnits.put(MAGNETOMETER, "");
		componentUnits.put(PRESSURE, "Hecto pascal (hPa)");
		componentUnits.put(GYROSCOPE, "");

		componentUnits.put(OPTICAL, "Light intensity (W/sr)");
	}

	private static final Map<String, SensorUuid> sensors = new HashMap<String, SensorUuid>();
	static {
		sensors.put(TEMPERATURE,   new SensorUuid("f000aa00-0451-4000-b000-000000000000", "f000aa01-0451-4000-b000-000000000000", "f000aa02-0451-4000-b000-000000000000", "f000aa03-0451-4000-b000-000000000000"));
		sensors.put(ACCELEROMETER, new SensorUuid("f000aa10-0451-4000-b000-000000000000", "f000aa11-0451-4000-b000-000000000000", "f000aa12-0451-4000-b000-000000000000", "f000aa13-0451-4000-b000-000000000000"));
		sensors.put(HUMIDITY,      new SensorUuid("f000aa20-0451-4000-b000-000000000000", "f000aa21-0451-4000-b000-000000000000", "f000aa22-0451-4000-b000-000000000000", "f000aa23-0451-4000-b000-000000000000"));
		sensors.put(MAGNETOMETER,  new SensorUuid("f000aa30-0451-4000-b000-000000000000", "f000aa31-0451-4000-b000-000000000000", "f000aa32-0451-4000-b000-000000000000", "f000aa33-0451-4000-b000-000000000000"));
		sensors.put(PRESSURE,      new SensorUuid("f000aa40-0451-4000-b000-000000000000", "f000aa41-0451-4000-b000-000000000000", "f000aa42-0451-4000-b000-000000000000", "f000aa43-0451-4000-b000-000000000000"));
		sensors.put(GYROSCOPE,     new SensorUuid("f000aa50-0451-4000-b000-000000000000", "f000aa51-0451-4000-b000-000000000000", "f000aa52-0451-4000-b000-000000000000", "f000aa53-0451-4000-b000-000000000000"));

		sensors.put(OPTICAL,       new SensorUuid("f000aa70-0451-4000-b000-000000000000", "f000aa71-0451-4000-b000-000000000000", "f000aa72-0451-4000-b000-000000000000", "f000aa73-0451-4000-b000-000000000000"));
	}

	// Write 0x0001 to enable notifications, 0x0000 to disable.
	// Write 0x01 to enable data collection, 0x00 to disable.
	private static final byte[] TURN_ON_SENSOR = { 0X01 };

	private static final byte[] TURN_OFF_SENSOR = { 0X00 };

	/**
	 * 
	 * @param devicedefinition
	 * @throws DBusException
	 */
	public TISensorTag(DeviceDefinition devicedefinition) throws DBusException {
		super(devicedefinition);
	}

	@Override
	public String DeviceRead(String sensorName) {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (deviceStatus.equals(CONNECTED)) {
				if (isSensorSupported(sensorName.trim())) {
					try {
						// turn on sensor
						deviceProtocol.Write(address, getEnableSensorProfile(sensorName));
						/**
						 * The default read data period (frequency) of most of
						 * sensor tag sensors is 1000ms therefore the first data
						 * will be available to read after 1000ms for these we
						 * call Read method after 1 second
						 */
						Thread.sleep(1010);
						// read value
						byte[] readValue = deviceProtocol.Read(address, getReadValueProfile(sensorName));
 						// TODO: Sending {0x00} raised error on dbus
						// deviceProtocol.Write(address,
						// getTurnOffSensorProfile(sensorName));
						return formatReading(sensorName, readValue);
					} catch (Exception e) {
						logger.debug("Error in reading value from Sensor {}", e);
						e.printStackTrace();
					}
				} else {
					logger.debug("Sensor not supported: {}", sensorName);
					return null;
				}
			} else {
				logger.debug("BLE Device not connected: {}", deviceName);
				return null;
			}
		} else {
			logger.debug("Protocol not supported:: {}", protocol);
			return null;
		}
		return null;
	}

	@Override
	public void Subscribe(String sensorName) {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (deviceStatus.equals(CONNECTED)) {
				if (isSensorSupported(sensorName.trim())) {
					try {
						logger.info("Enabling sensor for subscribtion");
						deviceProtocol.Write(address, getEnableSensorProfile(sensorName));
						byte[] period = { 100 };
						deviceProtocol.Write(address, getFrequencyProfile(sensorName, period));
						deviceProtocol.Subscribe(address, getReadValueProfile(sensorName));
						
						//send signal 
						signalNewSubscribeValue(sensorName);
						
					} catch (DBusException e) {
						e.printStackTrace();
					}
				} else {
					logger.info("Sensor not supported: {}", sensorName);
				}
			} else {
				logger.info("BLE Device not connected: {}", deviceName);
			}
		} else {
			logger.info("Protocol not supported:: {}", protocol);
		}
	}

	@Override
	public void Unsubscribe(String sensorName) throws DBusException {
		if ((protocol.equals(BLUETOOTH_LOW_ENERGY)) && (deviceProtocol != null)) {
			if (deviceStatus.equals(CONNECTED)) {
				if (isSensorSupported(sensorName.trim())) {
					try {
						// disable notification
						deviceProtocol.Unsubscribe(address, getReadValueProfile(sensorName));
						// TODO: Sending {0x00} on dbus has an exception
						// turn off sensor
						// deviceProtocol.Write(address,
						// getTurnOffSensorProfile(sensorName));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					logger.debug("Sensor not supported: {}", sensorName);
				}
			} else {
				logger.debug("BLE Device not connected: {}", deviceName);
			}
		} else {
			logger.debug("Protocol not supported:: {}", protocol);
		}
	}

	// =======================Utility methods===========================
	@Override
	protected boolean isSensorSupported(String sensorName) {
		return sensors.containsKey(sensorName);
	}

	private Map<String, String> getEnableSensorProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charConfigUuid);
		}
		profile.put(PAYLOAD, new String(TURN_ON_SENSOR));
		return profile;
	}

	private Map<String, String> getReadValueProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charUuid);
		}
		return profile;
	}

	private Map<String, String> getTurnOffSensorProfile(String sensorName) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charConfigUuid);
		}
		profile.put(PAYLOAD, new String(TURN_OFF_SENSOR));
		return profile;
	}

	private Map<String, String> getFrequencyProfile(String sensorName, byte[] frequency) {
		Map<String, String> profile = new HashMap<String, String>();
		SensorUuid s = sensors.get(sensorName);
		if (s != null) {
			profile.put(GATT_SERVICE, s.serviceUuid);
			profile.put(GATT_CHARACTERSTICS, s.charFreqUuid);
		}
		profile.put(PAYLOAD, new String(frequency));
		return profile;
	}

	/**
	 *
	 * The sensor service returns the data in an encoded format which can be
	 * found in the
	 * wiki(http://processors.wiki.ti.com/index.php/SensorTag_User_Guide#
	 * IR_Temperature_Sensor). Convert the raw sensor reading value format to
	 * human understandable value and print it.
	 * 
	 * @param sensorName
	 *            Name of the sensor to read value from
	 * @param readingValue
	 *            the raw value read from the sensor
	 * @return
	 */
	@Override
	protected String formatReading(String sensorName, byte[] readData) {
		float result;
		int rawData;
		if (sensorName.contains(TEMPERATURE)) {
			rawData = shortSignedAtOffset(readData, 2);
			result = convertCelsius(rawData);
		} else if (sensorName.contains(HUMIDITY)) {
			rawData = shortSignedAtOffset(readData, 2);
			result = convertHumidity(rawData);
		} else if (sensorName.contains(PRESSURE)) {
			int lowerByte = Byte.toUnsignedInt(readData[3]);
			int upperByte = Byte.toUnsignedInt(readData[4]);
			int upper = Byte.toUnsignedInt(readData[5]);
			int rawResult = (upperByte << 8) + (lowerByte & 0xff) + upper;
			float pressure = convertPressure(rawResult);

			int t_r; // Temperature raw value from sensor
			int p_r; // Pressure raw value from sensor
			Double t_a; // Temperature actual value in unit centi degrees
						// celsius
			Double S; // Interim value in calculation
			Double O; // Interim value in calculation
			Double p_a; // Pressure actual value in unit Pascal.

			t_r = shortSignedAtOffset(readData, 0);
			p_r = shortSignedAtOffset(readData, 3);

			t_a = (100 * (readData[0] * t_r / Math.pow(2, 8) + readData[1] * Math.pow(2, 6))) / Math.pow(2, 16);
			S = readData[3] + readData[4] * t_r / Math.pow(2, 17)
					+ ((readData[5] * t_r / Math.pow(2, 15)) * t_r) / Math.pow(2, 19);
			p_a = ((S * p_r) / Math.pow(2, 14));
			return Double.toString(p_a);
			// result = Float.toString(pressure);
		} else if (sensorName.equals(OPTICAL)) {
			rawData = shortSignedAtOffset(readData, 0);
			result = convertOpticalRead(rawData);
		} else {
			// TODO Other sensor values
			return readData.toString();
		}
		return Float.toString(result);
	}

	@Override
	protected String getMeasurementUnit(String sensorName) {
		String ret = componentUnits.get(sensorName);
		return ret;
	}

	/**
	 * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's
	 * complement values in the format LSB MSB.
	 *
	 * This function extracts these 16 bit two's complement values.
	 */
	private static Integer shortSignedAtOffset(byte[] value, int offset) {
		Integer lowerByte = Byte.toUnsignedInt(value[offset]);
		Integer upperByte = Byte.toUnsignedInt(value[offset + 1]); // Note:
		return (upperByte << 8) + lowerByte;
	}

	/**
	 * Converts temperature into degree Celsius
	 *
	 * @param raw
	 * @return
	 */
	private float convertCelsius(int raw) {
		return raw / 128f;
	}

	/**
	 * Formats humidity value according to SensorTag WIKI
	 * 
	 * @param raw
	 * @return
	 */
	private float convertHumidity(int raw) {
		return (((float) raw) / 65536) * 100;
	}

	private float convertPressure(int raw) {
		return raw / 100;
	}

	private float convertOpticalRead(int raw) {
		int e = (raw & 0x0F000) >> 12; // Interim value in calculation
		int m = raw & 0x0FFF; // Interim value in calculation

		return (float) (m * (0.01 * Math.pow(2.0, e)));
	}

}
