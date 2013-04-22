import lejos.nxt.Motor;

public class Rotor {

	/**
	 * Moves an individual motor.
	 * 
	 * @param motorPort
	 *            the motor to be moved (A,B,C)
	 * @param speed
	 *            negative to turn backwards, positive to turn forwards
	 */
	public static void MoveMotor(String motorPort, int speed) {
		if (motorPort.equals("A")) {
			// Motor.A.get
			Motor.A.setSpeed(speed);
			Motor.A.forward();
		} else if (motorPort.equals("B")) {
			Motor.B.setSpeed(speed);
			Motor.B.forward();
		} else if (motorPort.equals("C")) {
			Motor.C.setSpeed(speed);
			Motor.C.forward();
		}

	}

	/**
	 * Moves the robot in a certain direction.
	 * 
	 * @param direction
	 *            the direction the robot should move in
	 * @param speed
	 *            negative to turn backwards, positive to turn forwards
	 */
	public static void Move(String direction, int speed) {
		Manager.pilot.setTravelSpeed(speed);
		System.out.println(speed);
		if (direction.equals("00")) {
			// Forward
			Manager.pilot.forward();
			System.out.println("forward");
		}
		if (direction.equals("01")) {
			// Backward
			Manager.pilot.backward();
			System.out.println("backward");
		}
		if (direction.equals("10")) {
			// Left
			Manager.pilot.rotate(-90);
			System.out.println("left");
		}
		if (direction.equals("11")) {
			// Right
			Manager.pilot.rotate(90);
			System.out.println("right");
		}
	}

	/**
	 * Stops the robot.
	 */
	public static void Stop() {
		// StopMovement
		System.out.println("Stopping");
		if (Manager.pilot.isMoving()) {
			Manager.pilot.stop();
		} else {
			Motor.A.stop();
			Motor.B.stop();
			Motor.C.stop();
		}
	}
}
