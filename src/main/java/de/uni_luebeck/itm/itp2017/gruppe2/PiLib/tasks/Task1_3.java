package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.stream.Collectors;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import de.dennis_boldt.RXTX;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.util.Configuration;

public class Task1_3 implements Observer, ITask {

	private static final Logger LOG = LoggerFactory.getLogger(Task1_3.class);
	@Option(name = "--ports", usage = "Set USB ports")
	public String ports = null;

	@Option(name = "--rxtxlib", usage = "Set RXTX lib")
	public String rxtxlib = "/usr/lib/jni";

	@Option(name = "--baud", usage = "Set baud rate")
	public int baud = 115200;

	// Buffer to collect bytes bytes for a float
	private final List<Byte> buffer = new ArrayList<Byte>();

	private GpioPinDigitalOutput led;

	private final static double HIGH_BORDER = 70.0;

	private String currentValue = "00";
	
	public String getCurrentValue() {
		return currentValue.substring(0, currentValue.length()-1);
	}
	
	public void update(Observable o, Object arg) {
		// is received object a byte array?
		if (arg instanceof byte[]) {
			byte[] bytes = (byte[]) arg;
			for (byte b : bytes) {
				// is next byte the delimiter? => parse the buffered bytes to
				// float
				if ("0a".equals(String.format("%02x", b))) {
					byte[] bt = new byte[buffer.size()];
					for (int i = 0; i < bt.length; i++) {
						bt[i] = buffer.get(i);
					}

					// use try catch for robust input
					try {
						String string = new String(bt);
						//System.out.println(string);
						this.currentValue = string;

					} catch (Throwable t) {
						LOG.debug(t.getMessage(), t);
					}
					buffer.clear();
				} else {
					buffer.add(b);
				}
			}
		}
	}
	
	public void setAvgValue(float avg) {
		if (avg < HIGH_BORDER) {
			led.setState(PinState.HIGH);
		} else {
			led.setState(PinState.LOW);
		}
	}

	@Override
	public void run(Configuration config)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		// create gpio controller
		final GpioController gpio = GpioFactory.getInstance();

		// provision gpio pin #07 as an output pin and turn on
		led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, PinState.LOW);

		RXTX rxtx;
		try {
			rxtx = new RXTX(this.baud);
			rxtx.start(this.ports, this.rxtxlib, this);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}
