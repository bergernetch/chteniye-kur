// javac -cp /usr/share/java/RXTXcomm.jar:json-20180130.jar:. ReadVolt.java
// java -cp /usr/share/java/RXTXcomm.jar:json-20180130.jar:. ReadVolt /dev/ttypUSB1

// Serial
import gnu.io.*;

// JSON
import org.json.*;
// Downloadable jar: http://mvnrepository.com/artifact/org.json/json

// Logger
import java.util.logging.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This version of the TwoWaySerialComm example makes use of the
 * SerialPortEventListener to avoid polling.
 *
 */
public class ReadVolt
{
    public ReadVolt() {
        super();
    }

/*	public void shutDown() throws Exception {
}*/

    void connect ( String portName ) throws Exception {
        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if ( portIdentifier.isCurrentlyOwned() ) {
            System.out.println("Error: Port is currently in use");
        } else {
            CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);

            if ( commPort instanceof SerialPort ) {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);

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
     * Handles the input coming from the serial port. A new line character
     * is treated as the end of a block in this example.
     */
    public static class SerialReader implements SerialPortEventListener{
        private InputStream in;
        private byte[] buffer = new byte[1024];

	Logger logger = Logger.getLogger("BatteryLog");
	FileHandler fh;

        public SerialReader ( InputStream in ) {
            this.in = in;
        }

        public void serialEvent(SerialPortEvent arg0){
            int data;

// {"A0_mV":"12607","A1_mV":"0","A0_RAW":"500","A1_RAW":"0"}

        try {
		// true for append mode
		fh = new FileHandler("/var/log/battery.log", true);
                logger.addHandler(fh);
                VerySimpleFormatter formatter = new VerySimpleFormatter();
                fh.setFormatter(formatter);

                int len = 0;
                while ( ( data = in.read()) > -1 ) {
                    if ( data == '\n' ) {
                        break;
                    }
                    buffer[len++] = (byte) data;
                }

		String json = new String(buffer,0,len);
//                System.out.println("RAW: " + json);

		// decode JSON
		JSONObject obj = new JSONObject(json);
		String rawBatteryVoltage = obj.getString("A0_mV");
//		System.out.println("Battery: " + rawBatteryVoltage);

		double batteryVoltage = (double) (Integer.parseInt(rawBatteryVoltage)/10)/100.0;
//		System.out.println("Volts: " + (double) batteryVoltage / 1000);

//		logger.info(rawBatteryVoltage + " mV");

		logger.info(batteryVoltage + " V");

            } catch ( Exception e ) {
                e.printStackTrace();
                System.exit(-1);
            }
	}

    }

    /** */
    public static class SerialWriter implements Runnable {
        OutputStream out;

        public SerialWriter ( OutputStream out ) {
            this.out = out;
        }

        public void run () {
            try {
                int c = 0;
                while ( ( c = System.in.read()) > -1 ) {
                    this.out.write(c);
                }
            }
            catch ( IOException e ) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    public static void main ( String[] args ) {

        try {
		System.out.println("Connecting to Arduino on " + args[0]);

            (new ReadVolt()).connect(args[0]); // "/dev/ttyUSB0"
/*        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                   r.shutDown();

            }
         });
*/

        }
        catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
