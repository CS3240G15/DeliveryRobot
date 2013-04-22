import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.UltrasonicSensor;
//import lejos.nxt.SensorPort;
//import lejos.nxt.SensorPortListener;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
//import lejos.nxt.comm.USB;
import lejos.robotics.navigation.DifferentialPilot;

public class Manager extends Object {

	@SuppressWarnings("unused")
	private static UltrasonicSensor sUltrasonic = new UltrasonicSensor(
			SensorPort.S2);
	@SuppressWarnings("unused")
	private static TouchSensor sTouch = new TouchSensor(SensorPort.S3);
	@SuppressWarnings("unused")
	private static LightSensor sLight = new LightSensor(SensorPort.S4);
	// measured in cm
	protected static DifferentialPilot pilot = new DifferentialPilot(5.3975f,
			17.4625f, Motor.A, Motor.C);
	private static int commandBufSize = 6;

	/**
	 * The managing program for the robot.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Waiting for Control...");

		// Establish the connection here
		NXTConnection connection = null;
		NXTConnection connection2 = null;
		// Open four data input and output streams for read and write
		// respectively for regular and error ops
		String input = "", output = "";
		final DataOutputStream oHandle;
		final DataInputStream iHandle;
		final DataOutputStream oHandleE;
		final DataInputStream iHandleE;

		connection = Bluetooth.waitForConnection();
		ArrayList<String> previousMsgs = new ArrayList<String>(commandBufSize);
		int seqNum = 1;

		// An additional check before opening streams
		if (connection == null) {
			System.out.println("Failed Connection to Control");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			System.exit(1);
		} else {
			System.out.println("Connected to Control");
		}

		oHandle = connection.openDataOutputStream();
		iHandle = connection.openDataInputStream();

		boolean canMove = true;
		do {
			try {
				byte[] buffer = new byte[256];
				int count = iHandle.read(buffer);
				if (count > 0) { // check if number of bytes read > 0
					input = (new String(buffer)).trim();
					// System.out.println("From PC: " + input);
					insert(previousMsgs, input);

					// message format [seqnum] [checksum] [opcode] [params]
					// check message integrity
					String seqnum = input.substring(0, 3);
					int checksum = Integer.parseInt(input.substring(4, 7));
					String opcode = input.substring(8, 12);

					System.out.println("SEQ: " + seqnum);
					System.out.println("CHK: " + checksum);
					System.out.println("OPCODE: " + opcode);
					System.out.println("PARAMS: " + input.substring(12));

					if (input.length() != checksum) {
						// Send back NACK
						output = seqnum + " 0";
					}
					if (opcode.equals("0011") && canMove) {
						// TakeReading
						System.out.println("Taking reading");
						output = seqnum + " 1 "
								+ Sensor.TakeReading(input.substring(13, 14));
					} else if (opcode.equals("0101")) {
						// EmergencyStop
						canMove = !canMove;
						System.out.print("canMove: " + canMove);
						output = seqnum + " 1";
					} else if (canMove) {
						System.out.println("About to perform action.");
						output = seqnum + " 1";
						performAction(input);
					}

					String str = output; // Where the ACK gets formatted
					oHandle.write(str.getBytes()); // ACK
					oHandle.flush(); // flush the output bytes
				}
				Thread.sleep(10);

			} catch (Exception e) {
				System.out.println("Error: " + e);
				previousMsgs.add(e.toString());
				break;
			}
		} while (!input.substring(8, 12).equals("0110")); // Abort

		System.out.println("Ending control session...");
		try {
			oHandle.close();
			iHandle.close();
			connection.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Connect to debugger
		if (connection != null)
			connection = null;
		Sound.playSample(new File("sos.wav"));
		System.out.println("Waiting for debugger...");
		connection2 = Bluetooth.waitForConnection();

		if (connection2 == null) {
			System.out.println("Failed");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			System.exit(1);
		} else {
			System.out.println("Connected to Debugger");
		}

		oHandleE = connection2.openDataOutputStream();
		iHandleE = connection2.openDataInputStream();

		if (!previousMsgs.isEmpty()) {
			for (String s : previousMsgs) {
				System.out.println("Previous messages:");
				int cSum = 2 + 1 + 1 + 3 + 6 + s.length();
				String out;
				if (cSum < 10) {
					out = "00" + seqNum + " 00" + cSum + " 1000 " + s;
				} else if (cSum >= 10 && cSum < 100) {
					out = "00" + seqNum + " 0" + cSum + " 1000 " + s;
				} else if (cSum >= 100 && cSum < 1000) {
					out = "00" + seqNum + " " + cSum + " 1000 " + s;
				} else {
					cSum = cSum - s.length() - 10;
					out = "00" + seqNum + " " + cSum + " 1000 " + "Too large.";
				}
				seqNum++;
				try {
					System.out.println(out);
					oHandleE.write(out.getBytes());
					oHandleE.flush(); // flush the output bytes
					// iHandleError for ACK?
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		// debugger
		do {
			try {
				byte[] buffer = new byte[256];
				int count = iHandleE.read(buffer);
				if (count > 0) { // check if number of bytes read > 0
					input = (new String(buffer)).trim();
					// System.out.println("From PC: " + input);
					insert(previousMsgs, input);

					// message format [seqnum] [checksum] [opcode] [params]
					// check message integrity
					String seqnum = input.substring(0, 3);
					int checksum = Integer.parseInt(input.substring(4, 7));
					String opcode = input.substring(8, 12);

					System.out.println("SEQ: " + seqnum);
					System.out.println("CHK: " + checksum);
					System.out.println("OPCODE: " + opcode);
					System.out.println("PARAMS: " + input.substring(12));

					if (input.length() != checksum) {
						// Send back NACK
						output = seqnum + " 0";
					}
					if (opcode.equals("0011") && canMove) {
						// TakeReading
						System.out.println("Taking reading");
						output = seqnum + " 1 "
								+ Sensor.TakeReading(input.substring(13, 14));
					} else if (opcode.equals("0101")) {
						// EmergencyStop
						canMove = !canMove;
						System.out.print("canMove: " + canMove);
						output = seqnum + " 1";
					} else if (canMove) {
						System.out.println("About to perform action.");
						output = seqnum + " 1";
						performAction(input);
					}

					String str = output; // Where the ACK gets formatted
					oHandleE.write(str.getBytes()); // ACK
					oHandleE.flush(); // flush the output bytes
				}
				Thread.sleep(10);

			} catch (Exception e) {
				System.out.println("Error: " + e);
				System.exit(1);
			}

		} while (!input.substring(8, 12).equals("0110"));

		System.out.println("Ending debug session...");
		try {
			oHandle.close();
			iHandle.close();
			connection2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Perform different actions based on the command
	 * 
	 * @param message
	 *            this is parsed to return a opcode to check against
	 */
	public static void performAction(String message) {
		System.out.println(message.substring(8, 12));
		if (message.substring(8, 12).equals("0000")) {
			// Move
			String direction = message.substring(13, 15);
			System.out.println("About to move");
			System.out.println(direction);
			int speed = Integer.parseInt(message.substring(16));
			System.out.println(speed);
			Rotor.Move(direction, speed);
		} else if (message.substring(8, 12).equals("0001")) {
			// MoveMotor
			String motorPort = message.substring(13, 14);
			System.out.println(motorPort);
			int speed = Integer.parseInt(message.substring(16));
			System.out.println("About to move");
			System.out.println(speed);
			Rotor.MoveMotor(motorPort, speed);
		} else if (message.substring(8, 12).equals("0010")) {
			// StopMovement
			Rotor.Stop();
		}
	}

	/**
	 * Wrapper for ArrayList to make it operate as a FIFO buffer with a specific
	 * size.
	 * 
	 * @param sA
	 *            list which contains commands
	 * @param s
	 *            the command to be added to the list
	 */
	private static void insert(ArrayList<String> sA, String s) {
		if (sA.size() + 1 == commandBufSize) {
			sA.remove(0);
			sA.add(s);
		} else if (sA.size() + 1 < commandBufSize) {
			sA.add(s);
		}
	}
}