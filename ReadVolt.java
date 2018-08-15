
// javac -cp /usr/share/java/RXTXcomm.jar:json-20180130.jar:. ReadVolt.java
// java -cp /usr/share/java/RXTXcomm.jar:json-20180130.jar:. ReadVolt /dev/ttypUSB1

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// Logger
import java.util.logging.FileHandler;
import java.util.logging.Logger;

// JSON
// Downloadable jar: http://mvnrepository.com/artifact/org.json/json
import org.json.JSONObject;

// RxTx
// http://rxtx.qbang.org/wiki/index.php/Download
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

/**
 * This version of the TwoWaySerialComm example makes use of the
 * SerialPortEventListener to avoid polling.
 *
 */
public class ReadVolt {
	static Logger logger = Logger.getLogger("BatteryLog");
	FileHandler fh;

	public ReadVolt() {
		super();

		// init logger
		try {
			fh = new FileHandler("/var/log/battery.log", true); // true for append mode
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.addHandler(fh);
		VerySimpleFormatter formatter = new VerySimpleFormatter();
		fh.setFormatter(formatter);

	}

	/*
	 * public void shutDown() throws Exception { }
	 */

	void connect(String portName) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			System.out.println("Error: Port is currently in use");
		} else {
			CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

			if (commPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) commPort;
				serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				InputStream in = serialPort.getInputStream();
				OutputStream out = serialPort.getOutputStream();

				(new Thread(new SerialWriter(out))).start();

				serialPort.addEventListener(new SerialReader(in));
				serialPort.notifyOnDataAvailable(true);

			} else {
				System.out.println("Error: Only serial ports are handled by this example.");
			}
		}
	}

	// Rewrite and initialize the logger centrally

	/**
	 * Handles the input coming from the serial port. A new line character is
	 * treated as the end of a block in this example.
	 */
	public static class SerialReader implements SerialPortEventListener {
		private InputStream in;
		private byte[] buffer = new byte[1024];

		public SerialReader(InputStream in) {
			this.in = in;
		}

		public void serialEvent(SerialPortEvent arg0) {
			int data;

			// Demo JSON object
			// {"A0_mV":"12607","A1_mV":"0","A0_RAW":"500","A1_RAW":"0"}

			try {

				int len = 0;
				while ((data = in.read()) > -1) {
					if (data == '\n') {
						break;
					}
					buffer[len++] = (byte) data;
				}

				String json = new String(buffer, 0, len);

				// decode JSON
				JSONObject obj = new JSONObject(json);
				String rawBatteryVoltage = obj.getString("A0_mV");

				// convert from mV to V with 2 decimals
				double batteryVoltage = (double) (Integer.parseInt(rawBatteryVoltage) / 10) / 100.0;

				logger.info(batteryVoltage + " V");

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

	}

	/** */
	public static class SerialWriter implements Runnable {
		OutputStream out;

		public SerialWriter(OutputStream out) {
			this.out = out;
		}

		public void run() {
			try {
				int c = 0;
				while ((c = System.in.read()) > -1) {
					this.out.write(c);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	public static void main(String[] args) {

		try {
			System.out.println("Connecting to Arduino on " + args[0]);

			(new ReadVolt()).connect(args[0]); // "/dev/ttyUSB0"

			/*
			 * Runtime.getRuntime().addShutdownHook(new Thread() { public void run() {
			 * r.shutDown();
			 * 
			 * } });
			 */

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
