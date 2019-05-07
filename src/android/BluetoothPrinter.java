/**
 * @Author: srehanuddin (main), Added package.json from @atmosuwiryo , Image Printing Function from CesarBalzer
 * @Date:   2018-06-08
 */

package com.ru.cordova.printer.bluetooth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Xml.Encoding;
import android.util.Base64;
// Charset localized
import java.nio.charset.StandardCharsets;
// Handle stacktrace back to app
import java.io.StringWriter;
import java.io.PrintWriter;
// Image printing method by CesarBalzer
import java.util.ArrayList;
import java.util.List;


public class BluetoothPrinter extends CordovaPlugin {
	private static final String LOG_TAG = "BluetoothPrinter";
	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;

	Bitmap bitmap;

	public BluetoothPrinter() {}

	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
	}

	public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		if (action.equals("list")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					listBT(callbackContext);
				}
			});
			return true;
		} else if (action.equals("connect")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run()  {
					try {
						String name = args.getString(0);
						if (findBT(callbackContext, name)) {
							try {
								connectBT(callbackContext);
							} catch (IOException e) {
								sendStackTrace(callbackContext, e);
							}
						} else {
							callbackContext.error("Bluetooth Device Not Found: " + name);
						}
					} catch (Exception e) {
						sendStackTrace(callbackContext, e);
					}
				}
			});
			return true;
		} else if (action.equals("disconnect")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					try {
						disconnectBT(callbackContext);
					} catch (IOException e) {
						sendStackTrace(callbackContext, e);
					}
				}
			});
			return true;
		} else if (action.equals("print") || action.equals("printImage")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					try {
						String msg = args.getString(0);
						printImage(callbackContext, msg);
					} catch (IOException e) {
						sendStackTrace(callbackContext, e);
					} catch (Exception e) {
						sendStackTrace(callbackContext, e);
					}
				}
			});
			return true;
		} else if (action.equals("printText")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					try {
						String msg = args.getString(0);
						printText(callbackContext, msg);
					} catch (IOException e) {
						sendStackTrace(callbackContext, e);
					} catch (Exception e) {
						sendStackTrace(callbackContext, e);
					}
				}
			});
			return true;
		} else if (action.equals("printQRCode")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					try {
						String msg = args.getString(0);
						printQRCode(callbackContext, msg);
					} catch (IOException e) {
						sendStackTrace(callbackContext, e);
					} catch (Exception e) {
						sendStackTrace(callbackContext, e);
					}
				}
			});
			return true;
		} else if (action.equals("printPOSCommand")) {
			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					try {
						String msg = args.getString(0);
						printPOSCommand(callbackContext, msg);
					} catch (IOException e) {
						sendStackTrace(callbackContext, e);
					} catch (Exception e) {
						sendStackTrace(callbackContext, e);
					}
				}
			});
			return true;
		}
		return false;
	}
	boolean sendStackTrace(CallbackContext callbackContext, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString(); // stack trace as a string
		System.out.println(sStackTrace);
		Log.e(LOG_TAG, sStackTrace);
		callbackContext.error(sStackTrace);
		return true;
	}

	//This will return the array list of paired bluetooth printers
	void listBT(CallbackContext callbackContext) {
		BluetoothAdapter mBluetoothAdapter = null;
		String errMsg = null;
		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				errMsg = "No bluetooth adapter available";
				Log.e(LOG_TAG, errMsg);
				callbackContext.error(errMsg);
				return;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
			}
			Set < BluetoothDevice > pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				JSONArray json = new JSONArray();
				for (BluetoothDevice device: pairedDevices) {
					/*
					Hashtable map = new Hashtable();
					map.put("type", device.getType());
					map.put("address", device.getAddress());
					map.put("name", device.getName());
					JSONObject jObj = new JSONObject(map);
					*/
					json.put(device.getName());
				}
				callbackContext.success(json);
			} else {
				callbackContext.error("No Bluetooth Device Found");
			}
			//Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
	}

	// This will find a bluetooth printer device
	boolean findBT(CallbackContext callbackContext, String name) {
		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				Log.e(LOG_TAG, "No bluetooth adapter available");
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
			}
			Set < BluetoothDevice > pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				for (BluetoothDevice device: pairedDevices) {
					if (device.getName().equalsIgnoreCase(name)) {
						mmDevice = device;
						return true;
					}
				}
			}
			Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}

	// Tries to open a connection to the bluetooth printer device
	boolean connectBT(CallbackContext callbackContext) throws IOException {
		try {
			// Standard SerialPortService ID
			UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
			beginListenForData();
			//Log.d(LOG_TAG, "Bluetooth Opened: " + mmDevice.getName());
			callbackContext.success("Bluetooth Opened: " + mmDevice.getName());
			return true;
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}

	// After opening a connection to bluetooth printer device,
	// we have to listen and check if a data were sent to be printed.
	void beginListenForData() {
		try {
			final Handler handler = new Handler();
			// This is the ASCII code for a newline character
			final byte delimiter = 10;
			stopWorker = false;
			readBufferPosition = 0;
			readBuffer = new byte[1024];
			workerThread = new Thread(new Runnable() {
				public void run() {
					while (!Thread.currentThread().isInterrupted() && !stopWorker) {
						try {
							int bytesAvailable = mmInputStream.available();
							if (bytesAvailable > 0) {
								byte[] packetBytes = new byte[bytesAvailable];
								mmInputStream.read(packetBytes);
								for (int i = 0; i < bytesAvailable; i++) {
									byte b = packetBytes[i];
									if (b == delimiter) {
										byte[] encodedBytes = new byte[readBufferPosition];
										System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
										/*
										final String data = new String(encodedBytes, "US-ASCII");
										readBufferPosition = 0;
										handler.post(new Runnable() {
											public void run() {
												myLabel.setText(data);
											}
										});
                                        */
									} else {
										readBuffer[readBufferPosition++] = b;
									}
								}
							}
						} catch (IOException ex) {
							stopWorker = true;
						}
					}
				}
			});
			workerThread.start();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//This will send data to bluetooth printer
	boolean printText(CallbackContext callbackContext, String msg) throws IOException {
		try {
			mmOutputStream.write(msg.getBytes(StandardCharsets.ISO_8859_1));
			//mmOutputStream.write(msg.getBytes("Cp858"));
			// tell the user data were sent
			Log.d(LOG_TAG, "printText: " +msg);
			callbackContext.success("Data Sent");
			return true;
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}


	boolean printPOSCommand(CallbackContext callbackContext, String posCommand) throws IOException {
		try {
			//mmOutputStream.write(("Inam").getBytes());
			//mmOutputStream.write((((char)0x0A) + "10 Rehan").getBytes());
			Log.d(LOG_TAG, "printPOSCommand: " + posCommand);
			final byte[] buffer = hexStringToBytes(posCommand);
			mmOutputStream.write(buffer);
			//mmOutputStream.write(0x0A);

			// tell the user data were sent
			callbackContext.success("Data Sent");
			return true;
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}

	//This will send data to bluetooth printer
	boolean printImage(CallbackContext callbackContext, String msg) throws IOException {
		try {

			//final String encodedString =" data:image/png;base64,-------------------------";
			final String encodedString = msg;
			final String pureBase64Encoded = encodedString.substring(encodedString.indexOf(",") + 1);

			final byte[] decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT);

			Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

			bitmap = decodedBitmap;

			int mWidth = bitmap.getWidth();
			int mHeight = bitmap.getHeight();
			int width = 250;
			int height = 250;
			if (mWidth > mHeight) {
				height = (int) (((float)mHeight/mWidth) * width);
			}else if (mWidth < mHeight) {
				width = (int) (((float)mWidth/mHeight) * height);
			}
			bitmap = resizeImage(bitmap, width, height);


			byte[] buffer = decodeBitmap(bitmap);
			// Some Printers like BIXOLON needs that every output written be a whole line to be printed.
			/*byte[] newLines = "\n".getBytes();
			byte[] newLines = hexStringToBytes("1B 64 02");
			byte[] c = new byte[buffer.length + newLines.length];
			System.arraycopy(buffer, 0, c, 0, buffer.length);
			System.arraycopy(newLines, 0, c, buffer.length, newLines.length);
			Log.d(LOG_TAG, "Image bitmap :"+bitmap);
			Log.d(LOG_TAG, "Image bytes:"+buffer);
			Log.d(LOG_TAG, "Image length:"+c.length);
			mmOutputStream.write(c);*/

			//byte[] buffer = new byte[50000];
			// Code provided by BIXOLON to ensure max printer buffer
			int nPos = 0, nWrite = 0;
			while(true)
			{
				nWrite = ((buffer.length - nPos) > 500) ? 500 : (buffer.length - nPos);

				mmOutputStream.write(buffer, nPos, nWrite);
				mmOutputStream.flush();

				nPos += nWrite;
				if(nPos >= buffer.length)
				{
					break;
				}
				else
				{
					Thread.sleep(50);
				}
			}
			// mmOutputStream.write(buffer);
			// tell the user data were sent
			//Log.d(LOG_TAG, "Data Sent");
			callbackContext.success("Data Sent");
			return true;


		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}

	// NOTE-PV: Not tested (From Josue Alexander Ibarra https://stackoverflow.com/a/29221432/9267268)
	// NOTE-PV: Other option: https://github.com/srehanuddin/Cordova-Plugin-Bluetooth-Printer/issues/24
	boolean printQRCode(CallbackContext callbackContext, String qrdata) throws IOException {
		try {
			int store_len = qrdata.length() + 3;
			byte store_pL = (byte)(store_len % 256);
			byte store_pH = (byte)(store_len / 256);
			// QR Code: Select the model
			//              Hex     1D      28      6B      04      00      31      41      n1(x32)     n2(x00) - size of model
			// set n1 [49 x31, model 1] [50 x32, model 2] [51 x33, micro qr code]
			// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=140
			byte[] modelQR = {
				(byte) 0x1d,
				(byte) 0x28,
				(byte) 0x6b,
				(byte) 0x04,
				(byte) 0x00,
				(byte) 0x31,
				(byte) 0x41,
				(byte) 0x32, // size of model
				(byte) 0x00
			};

			// QR Code: Set the size of module
			// Hex      1D      28      6B      03      00      31      43      n
			// n depends on the printer
			// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=141
			byte[] sizeQR = {
				(byte) 0x1d,
				(byte) 0x28,
				(byte) 0x6b,
				(byte) 0x03,
				(byte) 0x00,
				(byte) 0x31,
				(byte) 0x43,
				(byte) 0x03 // n
			};

			//          Hex     1D      28      6B      03      00      31      45      n
			// Set n for error correction [48 x30 -> 7%] [49 x31-> 15%] [50 x32 -> 25%] [51 x33 -> 30%]
			// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=142
			byte[] errorQR = {
				(byte) 0x1d,
				(byte) 0x28,
				(byte) 0x6b,
				(byte) 0x03,
				(byte) 0x00,
				(byte) 0x31,
				(byte) 0x45,
				(byte) 0x31 // n
			};

			// QR Code: Store the data in the symbol storage area
			// Hex      1D      28      6B      pL      pH      31      50      30      d1...dk
			// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=143
			//                        1D          28          6B         pL          pH  cn(49->x31) fn(80->x50) m(48->x30) d1…dk
			byte[] storeQR = {
				(byte) 0x1d,
				(byte) 0x28,
				(byte) 0x6b,
				store_pL,
				store_pH,
				(byte) 0x31,
				(byte) 0x50,
				(byte) 0x30
			};

			// QR Code: Print the symbol data in the symbol storage area
			// Hex      1D      28      6B      03      00      31      51      m
			// https://reference.epson-biz.com/modules/ref_escpos/index.php?content_id=144
			byte[] printQR = {
				(byte) 0x1d,
				(byte) 0x28,
				(byte) 0x6b,
				(byte) 0x03,
				(byte) 0x00,
				(byte) 0x31,
				(byte) 0x51,
				(byte) 0x30
			};

			// write() simply appends the data to the buffer
			mmOutputStream.write(modelQR);

			mmOutputStream.write(sizeQR);
			mmOutputStream.write(errorQR);
			mmOutputStream.write(storeQR);
			mmOutputStream.write(qrdata.getBytes());
			mmOutputStream.write(printQR);
			// tell the user data were sent
			//Log.d(LOG_TAG, "Data Sent");
			callbackContext.success("Data Sent");
			return true;
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}

	// disconnect bluetooth printer.
	boolean disconnectBT(CallbackContext callbackContext) throws IOException {
		try {
			stopWorker = true;
			mmOutputStream.close();
			mmInputStream.close();
			mmSocket.close();
			callbackContext.success("Bluetooth Disconnect");
			return true;
		} catch (Exception e) {
			sendStackTrace(callbackContext, e);
		}
		return false;
	}


	public byte[] getText(String textStr) {
		// TODO Auto-generated method stubbyte[] send;
		byte[] send = null;
		try {
			send = textStr.getBytes("GBK");
		} catch (UnsupportedEncodingException e) {
			send = textStr.getBytes();
		}
		return send;
	}

	public static byte[] hexStringToBytes(String hexString) {
		hexString = hexString.toUpperCase();
		String[] hexStrings = hexString.split(" ");
		byte[] bytes = new byte[hexStrings.length];
		for (int i = 0; i < hexStrings.length; i++) {
			char[] hexChars = hexStrings[i].toCharArray();
			bytes[i] = (byte)(charToByte(hexChars[0]) << 4 | charToByte(hexChars[1]));
		}
		return bytes;
	}

	//New implementation for printing Images by CesarBalzer
	public static byte[] newHexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte)(charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	private static byte charToByte(char c) {
		return (byte)
		"0123456789ABCDEF".indexOf(c);
	}

	//New implementation
	private static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
		Bitmap BitmapOrg = bitmap;
		int width = BitmapOrg.getWidth();
		int height = BitmapOrg.getHeight();

		if (width > w) {
			float scaleWidth = ((float) w) / width;
			float scaleHeight = ((float) h) / height + 24;
			Matrix matrix = new Matrix();
			matrix.postScale(scaleWidth, scaleWidth);
			Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
				height, matrix, true);
			return resizedBitmap;
		} else {
			Bitmap resizedBitmap = Bitmap.createBitmap(w, height + 24, Config.RGB_565);
			Canvas canvas = new Canvas(resizedBitmap);
			Paint paint = new Paint();
			canvas.drawColor(Color.WHITE);
			canvas.drawBitmap(bitmap, (w - width) / 2, 0, paint);
			return resizedBitmap;
		}
	}

	private static String hexStr = "0123456789ABCDEF";

	private static String[] binaryArray = {
		"0000",
		"0001",
		"0010",
		"0011",
		"0100",
		"0101",
		"0110",
		"0111",
		"1000",
		"1001",
		"1010",
		"1011",
		"1100",
		"1101",
		"1110",
		"1111"
	};

	public static byte[] decodeBitmap(Bitmap bmp) {
		int bmpWidth = bmp.getWidth();
		int bmpHeight = bmp.getHeight();
		List < String > list = new ArrayList < String > (); //binaryString list
		StringBuffer sb;
		int bitLen = bmpWidth / 8;
		int zeroCount = bmpWidth % 8;
		String zeroStr = "";
		if (zeroCount > 0) {
			bitLen = bmpWidth / 8 + 1;
			for (int i = 0; i < (8 - zeroCount); i++) {
				zeroStr = zeroStr + "0";
			}
		}

		for (int i = 0; i < bmpHeight; i++) {
			sb = new StringBuffer();
			for (int j = 0; j < bmpWidth; j++) {
				int color = bmp.getPixel(j, i);

				int r = (color >> 16) & 0xff;
				int g = (color >> 8) & 0xff;
				int b = color & 0xff;
				// if color close to white，bit='0', else bit='1'
				if (r > 160 && g > 160 && b > 160) {
					sb.append("0");
				} else {
					sb.append("1");
				}
			}
			if (zeroCount > 0) {
				sb.append(zeroStr);
			}
			list.add(sb.toString());
		}

		List < String > bmpHexList = binaryListToHexStringList(list);
		String commandHexString = "1D763000";
		String widthHexString = Integer.toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8 : (bmpWidth / 8 + 1));
		if (widthHexString.length() > 2) {
			Log.d(LOG_TAG, "DECODEBITMAP ERROR : width is too large");
			return null;
		} else if (widthHexString.length() == 1) {
			widthHexString = "0" + widthHexString;
		}
		widthHexString = widthHexString + "00";

		String heightHexString = Integer.toHexString(bmpHeight);
		if (heightHexString.length() > 2) {
			Log.d(LOG_TAG, "DECODEBITMAP ERROR : height is too large");
			return null;
		} else if (heightHexString.length() == 1) {
			heightHexString = "0" + heightHexString;
		}
		heightHexString = heightHexString + "00";

		List < String > commandList = new ArrayList < String > ();
		commandList.add(commandHexString + widthHexString + heightHexString);
		commandList.addAll(bmpHexList);

		Log.d(LOG_TAG, "DECODEBITMAP HEX: \n\n");
		for (String hexStr: commandList) {
			Log.d(LOG_TAG, hexStr);
		}
		return hexList2Byte(commandList);
	}

	public static List < String > binaryListToHexStringList(List < String > list) {
		List < String > hexList = new ArrayList < String > ();
		for (String binaryStr: list) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < binaryStr.length(); i += 8) {
				String str = binaryStr.substring(i, i + 8);

				String hexString = myBinaryStrToHexString(str);
				sb.append(hexString);
			}
			hexList.add(sb.toString());
		}
		return hexList;

	}

	public static String myBinaryStrToHexString(String binaryStr) {
		String hex = "";
		String f4 = binaryStr.substring(0, 4);
		String b4 = binaryStr.substring(4, 8);
		for (int i = 0; i < binaryArray.length; i++) {
			if (f4.equals(binaryArray[i])) {
				hex += hexStr.substring(i, i + 1);
			}
		}
		for (int i = 0; i < binaryArray.length; i++) {
			if (b4.equals(binaryArray[i])) {
				hex += hexStr.substring(i, i + 1);
			}
		}

		return hex;
	}

	public static byte[] hexList2Byte(List < String > list) {
		List < byte[] > commandList = new ArrayList < byte[] > ();

		for (String hexStr: list) {
			commandList.add(newHexStringToBytes(hexStr));
		}
		byte[] bytes = sysCopy(commandList);
		return bytes;
	}

	public static byte[] sysCopy(List < byte[] > srcArrays) {
		int len = 0;
		for (byte[] srcArray: srcArrays) {
			len += srcArray.length;
		}
		byte[] destArray = new byte[len];
		int destLen = 0;
		for (byte[] srcArray: srcArrays) {
			System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
			destLen += srcArray.length;
		}
		return destArray;
	}

}
