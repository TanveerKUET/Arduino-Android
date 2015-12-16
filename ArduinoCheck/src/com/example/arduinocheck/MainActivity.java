package com.example.arduinocheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.ArrayList;

import android.R.bool;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	PowerManager.WakeLock wL;   // wake-lock
	private static final String TAG = "bluetooth2";
	
	

	Button sendData;  //While pressing this button data from edittext willbe sent from Android to Arduino
	EditText getText;  //This edit text takes user input
	TextView txtArduino; //This show the temperature received from arduino 24
	Handler h;        //when a data is received from bluetooth then it gets a message from ConnectedThread 

	final int RECIEVE_MESSAGE = 1; // Status for Handler
	private BluetoothAdapter btAdapter = null;    //get the access of the bluetooth of the android phone 
	private BluetoothSocket btSocket = null;      
	private StringBuilder sb = new StringBuilder();   //stores the data sent by arduino

	private ConnectedThread mConnectedThread;       //All times run and check is there any data has come or not

	// SPP UUID service
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB"); // bluetooth
																	// connection
																	// er unique
																	// id

	// MAC-address of Bluetooth module (you must edit this line)
	private static String address = "98:D3:31:20:04:41";
	//private static String address = "98:D3:31:50:11:0D";
	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// Defination wake-lock
		PowerManager pM = (PowerManager) getSystemService(Context.POWER_SERVICE);
		WakeLock wL = pM.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"whatever");

		super.onCreate(savedInstanceState);
		wL.acquire();   //start wake-lock

		setContentView(R.layout.activity_main);

		sendData = (Button) findViewById(R.id.sendbutton1);
		txtArduino = (TextView) findViewById(R.id.txtArduino); 
		
		getText = (EditText)findViewById(R.id.editText1);
		
		h = new Handler() {
			public void handleMessage(android.os.Message msg) {
				switch (msg.what) {
				case RECIEVE_MESSAGE: // if receive massage
					Log.w("RECIEVE_MESSAGE",""+RECIEVE_MESSAGE);
					
					byte[] readBuf = (byte[]) msg.obj;
					String strIncom = new String(readBuf, 0, msg.arg1); // create
																		// string
																		// from
																		// bytes
																		// array
					Log.w("MESSAGE",""+strIncom);
					txtArduino.setText("" + strIncom + ""); // update TextView

					sb.append(strIncom); // append string
					int endOfLineIndex = sb.indexOf("\r\n"); // determine the
																// end-of-line
					if (endOfLineIndex > 0) { // if end-of-line,
						String sbprint = sb.substring(0, endOfLineIndex); // extract
																			// string
						
						Log.w("sbprintMESSAGE",""+sbprint);
						
						//sb.delete(0, sb.length()); // and clear
						//txtArduino.setText("temperature: " + sbprint + " degree fahrenheit "); // update TextView
																				
						sb.delete(0, sb.length()); // and clear
						// newly edited

					break;
				}
			};
			}
			};
		
		btAdapter = BluetoothAdapter.getDefaultAdapter(); // get Bluetooth
															// adapter
		checkBTState();
		
		sendData.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				
				String data = getText.getText().toString();
				mConnectedThread.write(data);
			}
		});


		}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device)
			throws IOException {
		if (Build.VERSION.SDK_INT >= 10) {
			try {
				final Method m = device.getClass().getMethod(
						"createInsecureRfcommSocketToServiceRecord",
						new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {
				Log.e(TAG, "Could not create Insecure RFComm Connection", e);
			}
		}
		return device.createRfcommSocketToServiceRecord(MY_UUID);
	}

	@Override
	public void onResume() {
		super.onResume();

		Log.e(TAG, "...onResume - try connect...");

		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		// Two things are needed to make a connection:
		// A MAC address, which we got above.
		// A Service ID or UUID. In this case we are using the
		// UUID for SPP.

		try {
			btSocket = createBluetoothSocket(device);
		} catch (IOException e) {
			errorExit("Fatal Error", "In onResume() and socket create failed: "
					+ e.getMessage() + ".");
		}

		/*
		 * try { btSocket = device.createRfcommSocketToServiceRecord(MY_UUID); }
		 * catch (IOException e) { errorExit("Fatal Error",
		 * "In onResume() and socket create failed: " + e.getMessage() + "."); }
		 */

		// Discovery is resource intensive. Make sure it isn't going on
		// when you attempt to connect and pass your message.
		btAdapter.cancelDiscovery();

		// Establish the connection. This will block until it connects.
		Log.e(TAG, "...Connecting...");
		try {
			btSocket.connect();
			Log.e(TAG, "....Connection ok...");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				errorExit("Fatal Error",
						"In onResume() and unable to close socket during connection failure"
								+ e2.getMessage() + ".");
			}
		}

		// Create a data stream so we can talk to server.
		Log.e(TAG, "...Create Socket...");

		mConnectedThread = new ConnectedThread(btSocket);
		mConnectedThread.start();
	}

	@Override
	public void onPause() {
		super.onPause();

		Log.d(TAG, "...In onPause()...");

		try {
			btSocket.close();
		} catch (IOException e2) {
			errorExit("Fatal Error", "In onPause() and failed to close socket."
					+ e2.getMessage() + ".");
		}
	}

	private void checkBTState() {
		// Check for Bluetooth support and then check to make sure it is turned
		// on
		// Emulator doesn't support Bluetooth and will return null
		if (btAdapter == null) {
			errorExit("Fatal Error", "Bluetooth not support");
		} else {
			if (btAdapter.isEnabled()) {
				Log.d(TAG, "...Bluetooth ON...");
			} else {
				// Prompt user to turn on Bluetooth
				Intent enableBtIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 1);
			}
		}
	}

	private void errorExit(String title, String message) {
		Toast.makeText(getBaseContext(), title + " - " + message,
				Toast.LENGTH_LONG).show();
		finish();
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024]; // buffer store for the stream
			int bytes; // bytes returned from read()

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.read(buffer); // Get number of bytes and
														// message in "buffer"
					Log.w("BUFFER",""+bytes+""+buffer);
					h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer)    
							.sendToTarget(); // Send to message queue Handler
				} catch (IOException e) {
					Log.e("EXCEPTION","No data...");
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(String message) {
			Log.d(TAG, "...Data to send: " + message + "...");
			byte[] msgBuffer = message.getBytes();
			try {
				mmOutStream.write(msgBuffer);
			} catch (IOException e) {
				Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
			}
		}
	}
}
