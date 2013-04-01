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
package pl.aplinako;

import pl.mg6.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.rms.RecordStoreException;
import pl.mg6.*;
import pl.mg6.bluetooth.ServiceDiscoveryHelper;

/**
 *
 * @author maciej
 */
public class StatkiCanvas extends BaseCanvas {

	private int state = STATE_LOADING;
	private volatile int stateSync = STATE_NONE;

	private static final int STATE_NONE = -1;
	private static final int STATE_LOADING = 1;
	private static final int STATE_CLOSING = 2;
	private static final int STATE_MAIN_MENU = 3;
	private static final int STATE_SCORES_MENU = 4;
	private static final int STATE_OPTIONS_MENU = 5;
	private static final int STATE_ABOUT_MENU = 6;
	private static final int STATE_WRONG_NAME = 7;
	private static final int STATE_SERVER_WAIT = 8;
	private static final int STATE_CLIENT_SEARCH = 9;
	private static final int STATE_CLIENT_WAIT = 10;
	private static final int STATE_INCOMPATIBLE_VERSIONS = 11;
	private static final int STATE_GAME_OPTIONS_MENU = 12;
	private static final int STATE_GAME_OPTIONS_SHIPS_MENU = 13;
	private static final int STATE_WAIT_FOR_ACCEPT = 14;
	private static final int STATE_GAME_SETUP = 15;
	private static final int STATE_GAME_PLAYING = 16;
	private static final int STATE_WIN = 17;
	private static final int STATE_LOSS = 18;

	private int loadProgress = 0;

	private int[] blackFonts;
	private int[] whiteFonts;

	private int fontIndex;

	private int blackFont = -1;
	private int whiteFont = -1;

	private int target1Image = -1;
	private int target2Image = -1;
	private int target3Image = -1;

	//#if TouchScreen
	private int arrowsImage = -1;
	private int arrowsImageWidth;
	private int arrowsImageHeight;

	private int restartImage = -1;
	private int restartImageWidth;
	private int restartImageHeight;

	private int rotateImage = -1;
	private int rotateImageWidth;
	private int rotateImageHeight;

	private int hintImage = -1;
	private int hintImageWidth;
	private int hintImageHeight;

	private int chatImage = -1;
	private int chatImageWidth;
	private int chatImageHeight;
	//#endif

//	private int messageSound = -1;
//	private int hitSound = -1;
//	private int missedSound = -1;

	private static final int VIBRATE_TIME_HIT = 500;
	private static final int VIBRATE_TIME_MESSAGE = 200;
	private static final int VIBRATE_TIME_STATE_CHANGE = 100;

	private ServiceDiscoveryHelper sdHelper;

	private static final int MENU_MARGIN = 5;
	private static final int MENU_ITEM_MARGIN = 3;
	private static final int MENU_ITEMS_SPACING = 1;
	private static final int MENU_VALUES_SPACING = 10;

	private static final String[] closingMenuItems = {
		"Bluetooth wyłączony", "Zamknij"
	};
	private static final String[] mainMenuItems = {
		"Nowa gra", "Szukaj", "Wyniki", "Opcje", "O programie", "Wyjście"
	};
	private int mainMenuSelectedIndex = 0;

	private static final int SCORES_PER_PAGE = 5;
	private int scoresPage;

	private String[] getScoresMenuItems() {
		int count = getScoresItemsCount();
		String[] scoresMenuItems;
		if (count > 0) {
			scoresMenuItems = new String[count + 1];
			for (int i = 0; i < count; i++) {
				int index = scoresPage * SCORES_PER_PAGE + i;
				scoresMenuItems[i] = (index + 1) + ". " + scoresOppNames[index];
			}
			scoresMenuItems[count] = "Wróć";
		} else {
			scoresMenuItems = new String[] {
				"Brak wyników", "Wróć"
			};
		}
		return scoresMenuItems;
	}

	private String[] getScoresMenuValues() {
		int count = getScoresItemsCount();
		String[] scoresMenuValues;
		if (count > 0) {
			scoresMenuValues = new String[count];
			for (int i = 0; i < count; i++) {
				int index = scoresPage * SCORES_PER_PAGE + i;
				scoresMenuValues[i] = scoresWinCount[index] + "-" + scoresLossCount[index];
			}
		} else {
			scoresMenuValues = null;
		}
		return scoresMenuValues;
	}

	private int getScoresMenuSelectedIndex() {
		int count = getScoresItemsCount();
		if (count > 0) {
			return count;
		} else {
			return 1;
		}
	}

	private int getScoresItemsCount() {
		if (scoresOppIds.length == 0) {
			return 0;
		}
		if (scoresPage == getScoresMaxPage()) {
			return ((scoresOppIds.length - 1) % SCORES_PER_PAGE) + 1;
		} else {
			return SCORES_PER_PAGE;
		}
	}

	private int getScoresMaxPage() {
		return (scoresOppIds.length - 1) / SCORES_PER_PAGE;
	}

	private static final String[] optionsMenuItems = {
		"Dźwięki", "Wibracja", "Czcionka", "Imię", "Reset", "Wróć"
	};
	private static final String[] optionsMenuFontValues = {
		"MAŁA", "ŚREDNIA", "DUŻA"
	};

	private String[] getOptionsMenuValues() {
		String[] optionsMenuValues = {
			sound ? "TAK" : "NIE",
			vibration ? "TAK" : "NIE",
			optionsMenuFontValues[fontIndex],
			myName,
		};
		return optionsMenuValues;
	}

	private int optionsMenuSelectedIndex = optionsMenuItems.length - 1;
	private static final int OPTIONS_MENU_NAME_INDEX = 3;

	private static final String[] info = {
		Application.instance.getAppProperty("MIDlet-Name"),
		Application.instance.getAppProperty("MIDlet-Version"),
	};

	private static final String[] aboutMenuItems = {
		" " + Utils.join(info, ' ') + " ", null, " Programowanie ", " Maciej Górski ", null, "", "Wróć"
	};

	private static final String SCORES_RS_NAME = "StatkiScores";
	private static final byte SCORES_VERSION = 1;

	private String[] scoresOppIds;
	private String[] scoresOppNames;
	private int[] scoresWinCount;
	private int[] scoresLossCount;

	private static final String DATA_RS_NAME = "StatkiData";
	private static final byte DATA_VERSION = 1;

	private boolean sound;
	private volatile boolean vibration;

	private static final String[] wrongNameMenuItems = {
		"Brak imienia", "Wróć"
	};

	private static final String[] serverWaitMenuItems = {
		"Oczekiwanie...", "Anuluj"
	};

	private String[] getClientSearchMenuItems() {
		boolean searching = sdHelper.isSearching();
		int serviceCount = sdHelper.getServiceCount();
		String[] menu = new String[serviceCount + 2];
		if (searching) {
			menu[0] = "Wyszukiwanie...";
		} else if (serviceCount > 0) {
			menu[0] = "Zakończono";
		} else {
			menu[0] = "Nie znaleziono gry...";
		}
		menu[1] = "Anuluj";
		for (int i = 0; i < serviceCount; i++) {
			menu[i + 2] = sdHelper.getName(i);
//			for (int j = 0; j < scoresOppIds.length; j++) {
//				if (scoresOppIds[j].equals(sdHelper.getBluetoothAddress(i))) {
//					menu[i + 2] += " (" + scoresWinCount[j] + "-" + scoresLossCount[j] + ")";
//					break;
//				}
//			}
		}
		return menu;
	}

	private String[] getClientSearchMenuValues() {
		int serviceCount = sdHelper.getServiceCount();
		String[] menu = new String[serviceCount + 2];
		for (int i = 0; i < serviceCount; i++) {
			for (int j = 0; j < scoresOppIds.length; j++) {
				if (scoresOppIds[j].equals(sdHelper.getBluetoothAddress(i))) {
					menu[i + 2] = scoresWinCount[j] + "-" + scoresLossCount[j];
					break;
				}
			}
		}
		return menu;
	}

	private int clientSearchSelectedIndex;

	private static final String[] incompatibleVersionsMenuItems = {
		"Niekompatybilne",
		"wersje Statków.",
		"Twoja wersja: " + info[1],
		null,
		null,
		"Wróć"
	};

	private static final String[] gameOptionsMenuItems = {
		"X", "Y", "Statki", "Start", "Rozłącz"
	};

	private String[] getGameOptionsMenuValues() {
		String[] gameOptionsMenuValues = {
			String.valueOf(sizeX),
			String.valueOf(sizeY),
			".."
		};
		if (isServer || hasFirstStarted) {
			gameOptionsMenuItems[3] = "Start";
		} else {
			gameOptionsMenuItems[3] = "";
		}
		return gameOptionsMenuValues;
	}

	private int gameOptionsMenuSelectedIndex = gameOptionsMenuItems.length - 1;
	private volatile int sizeX, sizeY;

	private static final String[] gameOptionsShipsMenuItems = {
		"Lotniskowce", "Pancerniki", "Krążowniki", "Niszczyciele", "Okręty podwodne", "Wróć"
	};

	private String[] getGameOptionsShipsMenuValues() {
		String[] gameOptionsShipsMenuValues = {
			String.valueOf(ship5),
			String.valueOf(ship4),
			String.valueOf(ship3),
			String.valueOf(ship2),
			String.valueOf(ship1)
		};
		return gameOptionsShipsMenuValues;
	}

	private int gameOptionsShipsMenuSelectedIndex = gameOptionsShipsMenuItems.length - 1;
	private volatile int ship5, ship4, ship3, ship2, ship1;

	private volatile boolean hasFirstStarted;

	private static final String[] rematchMenuItems = {
		null, "Ukryj menu", "Rewanż", "Rozłącz"
	};
	private int rematchMenuSelectedIndex = rematchMenuItems.length - 2;

	private TextField chatTextField;
	//#if TouchScreen
	private TouchKeyboard chatTouchKeyboard;
	//#endif
	private Vector messages;

	private TextField myNameTextField;
	//#if TouchScreen
	private TouchKeyboard myNameTouchKeyboard;
	//#endif

	private volatile int stateTime;
	private volatile boolean showRematchMenu;
	
	private volatile boolean meReady;
	private volatile boolean oppReady;

	private volatile boolean myTurnNextGame;
	private volatile boolean myTurn;
	private volatile boolean meWaitingAfterShot;
	private volatile int myTurnTimeout;

	private byte[][] myShips;
	private byte[][] oppShips;

	private static final byte SHIP_UNKNOWN = 0;
	private static final byte SHIP_NO = -128;

	private int myCursorX, myCursorY;
	private volatile int oppCursorX, oppCursorY;

	private static final int MY_CURSOR_COLOR_1 = Utils.COLOR_SKY_BLUE_1;
	private static final int MY_CURSOR_COLOR_2 = Utils.COLOR_WHITE;
	private static final int OPP_CURSOR_COLOR_1 = Utils.COLOR_SCARLET_RED_3;
	private static final int OPP_CURSOR_COLOR_2 = Utils.COLOR_BLACK;
	private static final int CURSOR_COLOR_TRANSITION_TIME = 500;

	private int nextShipDirection;
	private static final int SD_RIGHT = 0;
	private static final int SD_DOWN = 1;

	private int[] shipsToDrop;
	private int[] shipsLeft;

	private boolean showShipsLeftMenu;

	private static final String[] shipsLeftMenuItems = {
		"Lotniskowce", "Pancerniki", "Krążowniki", "Niszczyciele", "Okręty podwodne"
	};
	private String[] getShipsLeftMenuValues() {
		return new String[] {
			String.valueOf(shipsLeft[0]),
			String.valueOf(shipsLeft[1]),
			String.valueOf(shipsLeft[2]),
			String.valueOf(shipsLeft[3]),
			String.valueOf(shipsLeft[4]),
		};
	}

	private volatile long shotTime;
	private volatile int shotLag;
	private int lagFont = -1;

	private static final UUID SERVICE_UUID = new UUID("482e52b990774509accaa356d318d4fe", false);
	private static final String SERVER_CONNECTION_URL = "btspp://localhost:" + SERVICE_UUID + ";authenticate=true;authorize=true;name=";

	private static final int NAME_MAX_LENGTH = 16;
	private String myName;
	private volatile String oppName;
	private String oppId;
	private volatile byte oppProtocolVersion;
	private volatile String oppAppVersion;
	private volatile boolean oppWriting;
	private boolean writing;

	private String clientConnectionURL;

	private StreamConnectionNotifier notifier;
	private StreamConnection conn;

	private Thread readerThread;
	private volatile Thread writerThread;

	private volatile boolean sendClose;
	private boolean isServer;

	private final Vector writeBuffer = new Vector();

	private static final byte BTC_PROTOCOL_VERSION = -6;
	private static final byte BTC_CLOSE = -1;
	private static final byte BTC_MESSAGE = 0;
	private static final byte BTC_SET_OPTIONS = 1;
	private static final byte BTC_SET_OPTION = 2;
	private static final byte BTC_GAME_FIRST_START = 3;
	private static final byte BTC_GAME_SECOND_START = 4;
	private static final byte BTC_YOUR_TURN = 5;
	private static final byte BTC_READY = 6;
	private static final byte BTC_CURSOR_POSITION = 7;
	private static final byte BTC_SHOT = 8;
	private static final byte BTC_SHOT_REPLY = 9;
	private static final byte BTC_YOU_WIN = 10;
	private static final byte BTC_MY_SHIPS = 11;
	private static final byte BTC_MY_NAME = 12;
	private static final byte BTC_CURRENT_SCORES = 13;
	private static final byte BTC_STARTED_WRITING = 14;
	private static final byte BTC_STOPPED_WRITING = 15;

	private static final byte PROTOCOL_VERSION = 1;

	private static final byte BTC_OPTION_X = 100;
	private static final byte BTC_OPTION_Y = 101;
	private static final byte BTC_OPTION_SHIP5 = 102;
	private static final byte BTC_OPTION_SHIP4 = 103;
	private static final byte BTC_OPTION_SHIP3 = 104;
	private static final byte BTC_OPTION_SHIP2 = 105;
	private static final byte BTC_OPTION_SHIP1 = 106;

	public void run() {
		Thread t = Thread.currentThread();
		if (t == readerThread) {
			runReader();
		} else if (t == writerThread) {
			runWriter();
		} else {
			super.run();
		}
	}

	private void runReader() {
		DataInputStream input = null;
		writeBuffer.removeAllElements();
		int stateAfterDisconnection = STATE_MAIN_MENU;
		try {
			if (isServer) {
				sdHelper.setDiscoverable(true);
				try {
					notifier = (StreamConnectionNotifier) Connector.open(SERVER_CONNECTION_URL + myName);
					conn = notifier.acceptAndOpen();
				} finally {
					sdHelper.setDiscoverable(false);
				}
			} else {
				conn = (StreamConnection) Connector.open(clientConnectionURL);
			}

			sendProtocolVersion();

			writerThread = new Thread(this);
			writerThread.setPriority(Thread.MAX_PRIORITY);
			writerThread.start();

			//input = conn.openDataInputStream();
			input = new DataInputStream(new BufferedInputStream(conn.openInputStream()));

			boolean sameProtocolVersions = false;

			oppProtocolVersion = 0;
			oppAppVersion = "1.0";

			byte data = input.readByte();
			if (data == BTC_PROTOCOL_VERSION) {
				oppId = RemoteDevice.getRemoteDevice(conn).getBluetoothAddress();
				//#debug
				debug("remote bt addr: " + oppId, null);
				input.readByte(); // ignore, BTC_CLOSE
				oppProtocolVersion = input.readByte();
				oppAppVersion = input.readUTF();
				//#debug
				debug("opp version: " + oppProtocolVersion + " " + oppAppVersion, null);
				if (oppProtocolVersion == PROTOCOL_VERSION) {
					sameProtocolVersions = true;
				}
			}
			if (sameProtocolVersions) {
				stateSync = STATE_GAME_OPTIONS_MENU;
				vibrate(VIBRATE_TIME_STATE_CHANGE);

				if (isServer) {
					sendOptions();
				} else {
					sendMyName();
				}
				int index = Utils.indexOf(scoresOppIds, oppId);
				if (index != -1) {
					sendCurrentScores(index);
				}
				data = input.readByte();
				while (data != BTC_CLOSE) {
					read(input, data);
					data = input.readByte();
				}
				if (sendClose) {
					send(BTC_CLOSE);
				}
			} else {
				incompatibleVersionsMenuItems[3] = "Przeciwnika: " + oppAppVersion;
				incompatibleVersionsMenuItems[4] = (PROTOCOL_VERSION < oppProtocolVersion
						? "Pobierz nową wersję" : "");
				stateAfterDisconnection = STATE_INCOMPATIBLE_VERSIONS;
			}
		} catch (SecurityException ex) {
			//#debug
			debug("SC.security", ex);
		} catch (IOException ex) {
			//#debug
			debug("SC.read_io", ex);
		} finally {
			Connection[] tmp = { notifier, conn };
			notifier = null;
			conn = null;

			Thread t = writerThread;
			if (t != null) {
				synchronized (writeBuffer) {
					writerThread = null;
					writeBuffer.notify();
				}
			}

			stateSync = stateAfterDisconnection;

			if (t != null) {
				try {
					t.join();
				} catch (InterruptedException ex) {
					//#debug
					debug("SC.join", ex);
				}
			}
			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
					//#debug
					debug("SC.input.close", ex);
				}
			}
			if (tmp[1] != null) {
				try {
					tmp[1].close();
				} catch (IOException ex) {
					//#debug
					debug("SC.conn.close", ex);
				}
			}
			if (tmp[0] != null) {
				try {
					tmp[0].close();
				} catch (IOException ex) {
					//#debug
					debug("SC.notifier.close", ex);
				}
			}
		}
	}

	private void read(DataInputStream input, byte data) throws IOException {
		switch (data) {
			case BTC_MESSAGE:
				String message = input.readUTF();
				messages.addElement(oppName + ": " + message);
				vibrate(VIBRATE_TIME_MESSAGE);
				break;
			case BTC_SET_OPTIONS:
				sizeX = input.readInt();
				sizeY = input.readInt();
				ship5 = input.readInt();
				ship4 = input.readInt();
				ship3 = input.readInt();
				ship2 = input.readInt();
				ship1 = input.readInt();
				break;
			case BTC_SET_OPTION:
				switch (input.readByte()) {
					case BTC_OPTION_X:
						sizeX = input.readInt();
						break;
					case BTC_OPTION_Y:
						sizeY = input.readInt();
						break;
					case BTC_OPTION_SHIP5:
						ship5 = input.readInt();
						break;
					case BTC_OPTION_SHIP4:
						ship4 = input.readInt();
						break;
					case BTC_OPTION_SHIP3:
						ship3 = input.readInt();
						break;
					case BTC_OPTION_SHIP2:
						ship2 = input.readInt();
						break;
					case BTC_OPTION_SHIP1:
						ship1 = input.readInt();
						break;
				}
				break;
			case BTC_GAME_FIRST_START:
				hasFirstStarted = true;
				vibrate(VIBRATE_TIME_STATE_CHANGE);
				break;
			case BTC_GAME_SECOND_START:
				stateSync = STATE_GAME_SETUP;
				vibrate(VIBRATE_TIME_STATE_CHANGE);
				break;
			case BTC_YOUR_TURN:
				myTurnNextGame = false;
				myTurn = true;
				break;
			case BTC_READY:
				oppReady = true;
				vibrate(VIBRATE_TIME_STATE_CHANGE);
				break;
			case BTC_CURSOR_POSITION:
				oppCursorX = input.readInt();
				oppCursorY = input.readInt();
				break;
			case BTC_SHOT:
				int shotX = input.readInt();
				int shotY = input.readInt();
				byte shotValue = myShips[shotY][shotX];
				if (shotValue == SHIP_UNKNOWN) {
					shotValue = SHIP_NO;
					myTurnTimeout = 1000;
				} else if (shotValue > SHIP_UNKNOWN) {
					shotValue = (byte) -shotValue;
					vibrate(VIBRATE_TIME_HIT);
				}
				myShips[shotY][shotX] = shotValue;
				sendShotReply(shotX, shotY, shotValue);
				if (shotValue != SHIP_NO) {
					checkDestroyedShip(myShips, shotX, shotY);
					checkLoss();
				}
				break;
			case BTC_SHOT_REPLY:
				shotLag = (int) (System.currentTimeMillis() - shotTime);
				int replyX = input.readInt();
				int replyY = input.readInt();
				byte replyValue = input.readByte();
				oppShips[replyY][replyX] = replyValue;
				if (replyValue == SHIP_NO) {
					myTurn = false;
				} else {
					checkDestroyedShip(oppShips, replyX, replyY);
					vibrate(VIBRATE_TIME_HIT);
				}
				meWaitingAfterShot = false;
				break;
			case BTC_YOU_WIN:
				try {
					updateScores(oppId, oppName, true);
				} catch (Exception ex) {
					//#debug
					debug("SC.updateScores, true", ex);
				}
				sendMyShips();
				stateSync = STATE_WIN;
				stateTime = 0;
				hasFirstStarted = false;
				showRematchMenu = true;
				break;
			case BTC_MY_SHIPS:
				int count = input.readInt();
				for (int i = 0; i < count; i++) {
					int x = input.readInt();
					int y = input.readInt();
					oppShips[y][x] = input.readByte();
				}
				break;
			case BTC_MY_NAME:
				oppName = input.readUTF();
				break;
			case BTC_CURRENT_SCORES:
				int loss = input.readInt();
				int win = input.readInt();
				try {
					updateScores(oppId, oppName, win, loss);
				} catch (Exception ex) {
					//#debug
					debug("SC.updateScores " + win + " " + loss, ex);
				}
				break;
			case BTC_STARTED_WRITING:
				oppWriting = true;
				break;
			case BTC_STOPPED_WRITING:
				oppWriting = false;
				break;
		}
	}

	private void checkLoss() {
		boolean loss = true;
		outer:
		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				if (myShips[i][j] > SHIP_UNKNOWN) {
					loss = false;
					break outer;
				}
			}
		}
		if (loss) {
			send(BTC_YOU_WIN);
			try {
				updateScores(oppId, oppName, false);
			} catch (Exception ex) {
				//#debug
				debug("SC.updateScores, false", ex);
			}
			stateSync = STATE_LOSS;
			stateTime = 0;
			hasFirstStarted = false;
			showRematchMenu = true;
		}
	}

	private void checkDestroyedShip(byte[][] ships, int x, int y) {
		int value = ships[y][x];
		int minX = x;
		int minY = y;
		int maxX = x;
		int maxY = y;
		do {
			minX--;
		} while (minX >= 0 && ships[y][minX] == value);
		do {
			minY--;
		} while (minY >= 0 && ships[minY][x] == value);
		do {
			maxX++;
		} while (maxX < sizeX && ships[y][maxX] == value);
		do {
			maxY++;
		} while (maxY < sizeY && ships[maxY][x] == value);
		if (maxX - minX + maxY - minY - 3 == -value) {
			for (int i = minY; i <= maxY; i++) {
				if (i < 0 || i >= sizeY) {
					continue;
				}
				for (int j = minX; j <= maxX; j++) {
					if (j < 0 || j >= sizeX) {
						continue;
					}
					if (ships[i][j] == SHIP_UNKNOWN) {
						ships[i][j] = SHIP_NO;
					}
				}
			}
			if (ships == oppShips) {
				shipsLeft[value + 5]--;
			}
		} else {
			for (int i = y - 1; i <= y + 1; i += 2) {
				if (i < 0 || i >= sizeY) {
					continue;
				}
				for (int j = x - 1; j <= x + 1; j += 2) {
					if (j < 0 || j >= sizeX) {
						continue;
					}
					if (ships[i][j] == SHIP_UNKNOWN) {
						ships[i][j] = SHIP_NO;
					}
				}
			}
		}
	}

	private void runWriter() {
		synchronized(writeBuffer) {
			DataOutputStream output = null;
			try {
				output = conn.openDataOutputStream();
				while (writerThread == Thread.currentThread()) {
					write(output);
					writeBuffer.wait();
				}
				write(output);
			} catch (InterruptedException ex) {
				//#debug
				debug("SC.write_interrupt", ex);
			} catch (IOException ex) {
				//#debug
				debug("SC.write_io", ex);
			} finally {
				if (output != null) {
					try {
						output.close();
					} catch (IOException ex) {
						//#debug
						debug("SC.output.close", ex);
					}
				}
			}
		}
	}

	private void sendObj(Object data) {
		synchronized (writeBuffer) {
			writeBuffer.addElement(data);
			writeBuffer.notify();
		}
	}

	private void send(String data) {
		sendObj(data);
	}

	private void send(byte data) {
		sendObj(new Byte(data));
	}

	private void send(int data) {
		sendObj(new Integer(data));
	}

	private void sendProtocolVersion() {
		synchronized (writeBuffer) {
			send(BTC_PROTOCOL_VERSION);
			send(BTC_CLOSE);
			send(PROTOCOL_VERSION);
			send(info[1]);
		}
	}

	private void sendMessage(String message) {
		synchronized (writeBuffer) {
			send(BTC_MESSAGE);
			send(message);
		}
		messages.addElement(myName + ": " + message);
	}

	private void sendOptions() {
		synchronized (writeBuffer) {
			send(BTC_SET_OPTIONS);
			send(sizeX);
			send(sizeY);
			send(ship5);
			send(ship4);
			send(ship3);
			send(ship2);
			send(ship1);
		}
	}

	private void sendOption(byte option, int value) {
		synchronized (writeBuffer) {
			send(BTC_SET_OPTION);
			send(option);
			send(value);
		}
	}

	private void sendCursorPosition() {
		synchronized (writeBuffer) {
			send(BTC_CURSOR_POSITION);
			send(myCursorX);
			send(myCursorY);
		}
	}

	private void sendShot() {
		shotTime = System.currentTimeMillis();
		synchronized (writeBuffer) {
			send(BTC_SHOT);
			send(myCursorX);
			send(myCursorY);
		}
	}

	private void sendShotReply(int x, int y, byte value) {
		synchronized (writeBuffer) {
			send(BTC_SHOT_REPLY);
			send(x);
			send(y);
			send(value);
		}
	}

	private void sendMyShips() {
		int count = 0;
		byte value;
		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				value = myShips[i][j];
				if (value > SHIP_UNKNOWN) {
					count++;
				}
			}
		}
		synchronized (writeBuffer) {
			send(BTC_MY_SHIPS);
			send(count);
			for (int i = 0; i < sizeY; i++) {
				for (int j = 0; j < sizeX; j++) {
					value = myShips[i][j];
					if (value > SHIP_UNKNOWN) {
						send(j);
						send(i);
						send(value);
					}
				}
			}
		}
	}

	private void sendMyName() {
		synchronized (writeBuffer) {
			send(BTC_MY_NAME);
			send(myName);
		}
	}

	private void sendCurrentScores(int index) {
		synchronized (writeBuffer) {
			send(BTC_CURRENT_SCORES);
			send(scoresWinCount[index]);
			send(scoresLossCount[index]);
		}
		//#debug
		debug("scores sent: " + scoresWinCount[index] + " " + scoresLossCount[index], null);
	}

	private void write(DataOutputStream output) throws IOException {
		while (!writeBuffer.isEmpty()) {
			Object data = writeBuffer.firstElement();
			writeBuffer.removeElementAt(0);
			if (data instanceof String) {
				output.writeUTF((String) data);
			} else if (data instanceof Byte) {
				output.writeByte(((Byte) data).byteValue());
			} else if (data instanceof Integer) {
				output.writeInt(((Integer) data).intValue());
			}
		}
		output.flush();
	}

	private void vibrate(int dt) {
		if (vibration) {
			Application.display.vibrate(dt);
		}
	}

	protected void update(int dt) {
		if (stateSync != STATE_NONE) {
			state = stateSync;
			stateSync = STATE_NONE;
		}
		switch (state) {
			case STATE_LOADING:
				load();
				break;
			case STATE_CLOSING:
				for (int i = 0; i < keysPressedCount; i++) {
					if (getGameAction(keysPressed[i]) == FIRE) {
						Application.instance.exit();
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(closingMenuItems, null, pointerStartX, pointerStartY);
					if (index == 1) {
						Application.instance.exit();
					}
				}
				//#endif
				break;
			case STATE_MAIN_MENU:
				chatTextField.setText("");
				chatTextField.setActive(false);
				oppWriting = false;
				writing = false;
				messages.removeAllElements();
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == UP) {
						if (mainMenuSelectedIndex > 0) {
							mainMenuSelectedIndex--;
						}
					} else if (gameAction == DOWN) {
						if (mainMenuSelectedIndex < mainMenuItems.length - 1) {
							mainMenuSelectedIndex++;
						}
					} else if (gameAction == FIRE) {
						if (mainMenuSelectedIndex == 0) {
							if (checkMyName()) {
								isServer = true;
								startConnection();
								state = STATE_SERVER_WAIT;
							}
						} else if (mainMenuSelectedIndex == 1) {
							if (checkMyName()) {
								sdHelper.start(SERVICE_UUID);
								clientSearchSelectedIndex = 0;
								state = STATE_CLIENT_SEARCH;
							}
						} else if (mainMenuSelectedIndex == 2) {
							state = STATE_SCORES_MENU;
						} else if (mainMenuSelectedIndex == 3) {
							state = STATE_OPTIONS_MENU;
						} else if (mainMenuSelectedIndex == 4) {
							state = STATE_ABOUT_MENU;
						} else if (mainMenuSelectedIndex == 5) {
							try {
								writeData();
							} catch (Exception ex) {
								//#debug
								debug("SC.rs_write", ex);
							}
							Application.instance.exit();
						}
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(mainMenuItems, null, pointerStartX, pointerStartY);
					if (index >= 0) {
						if (mainMenuSelectedIndex != index) {
							mainMenuSelectedIndex = index;
						} else if (mainMenuSelectedIndex == 0) {
							if (checkMyName()) {
								isServer = true;
								startConnection();
								state = STATE_SERVER_WAIT;
							}
						} else if (mainMenuSelectedIndex == 1) {
							if (checkMyName()) {
								sdHelper.start(SERVICE_UUID);
								clientSearchSelectedIndex = 0;
								state = STATE_CLIENT_SEARCH;
							}
						} else if (mainMenuSelectedIndex == 2) {
							state = STATE_SCORES_MENU;
						} else if (mainMenuSelectedIndex == 3) {
							state = STATE_OPTIONS_MENU;
						} else if (mainMenuSelectedIndex == 4) {
							state = STATE_ABOUT_MENU;
						} else if (mainMenuSelectedIndex == 5) {
							try {
								writeData();
							} catch (Exception ex) {
								//#debug
								debug("SC.rs_write", ex);
							}
							Application.instance.exit();
						}
					}
				}
				//#endif
				break;
			case STATE_SCORES_MENU:
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == UP || gameAction == LEFT) {
						previousScoresPage();
					} else if (gameAction == DOWN || gameAction == RIGHT) {
						nextScoresPage();
					} else if (gameAction == FIRE) {
						state = STATE_MAIN_MENU;
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(getScoresMenuItems(), getScoresMenuValues(), pointerStartX, pointerStartY);
					if (index >= 0) {
						if (index == getScoresMenuSelectedIndex()) {
							state = STATE_MAIN_MENU;
						} else {
							if (pointerStartX < getWidth() / 2) {
								previousScoresPage();
							} else {
								nextScoresPage();
							}
						}
					} else {
						if (pointerStartX < getWidth() / 2) {
							previousScoresPage();
						} else {
							nextScoresPage();
						}
					}
				}
				//#endif
				break;
			case STATE_OPTIONS_MENU:
				myNameTextField.update(dt);
				//#if TouchScreen
				if (hasPointerEvents()) {
					myNameTouchKeyboard.update(dt);
				}
				//#endif
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (myNameTextField.isActive()) {
						if (keysPressed[i] == KEY_SELECT || keysPressed[i] == KEY_ENTER) {
							myNameTextField.setActive(false);
							myName = myNameTextField.getText().trim();
							myNameTextField.setText(myName);
						} else {
							myNameTextField.keyPressed(keysPressed[i]);
						}
					} else if (gameAction == UP) {
						if (optionsMenuSelectedIndex > 0) {
							optionsMenuSelectedIndex--;
						}
					} else if (gameAction == DOWN) {
						if (optionsMenuSelectedIndex < optionsMenuItems.length - 1) {
							optionsMenuSelectedIndex++;
						}
					} else if (gameAction == FIRE) {
						if (optionsMenuSelectedIndex == 0) {
							//sound = !sound;
						} else if (optionsMenuSelectedIndex == 1) {
							vibration = !vibration;
							vibrate(VIBRATE_TIME_HIT);
						} else if (optionsMenuSelectedIndex == 2) {
							fontIndex++;
							fontIndex %= blackFonts.length;
							updateFontFromIndex();
						} else if (optionsMenuSelectedIndex == 3) {
							myNameTextField.setActive(true);
							myNameTextField.setDimensions(getItemsWidth(optionsMenuItems, getOptionsMenuValues()), TextField.SIZE_PREFERRED);
						} else if (optionsMenuSelectedIndex == 4) {
							try {
								resetData();
								updateFontFromIndex();
								myNameTextField.setText(myName);
							} catch (Exception ex) {
								//#debug
								debug("SC.rs_reset", ex);
							}
						} else if (optionsMenuSelectedIndex == 5) {
							state = STATE_MAIN_MENU;
							try {
								writeData();
							} catch (Exception ex) {
								//#debug
								debug("SC.options.rs_write", ex);
							}
						}
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					if (myNameTextField.isActive() && pointerStartY < myNameTouchKeyboard.getHeight()) {
						myNameTouchKeyboard.pointerPressed(pointerStartX, pointerStartY);
					} else {
						int index = getMenuIndex(optionsMenuItems, getOptionsMenuValues(), pointerStartX, pointerStartY);
						if (index >= 0) {
							if (optionsMenuSelectedIndex != index) {
								if (myNameTextField.isActive()) {
									if (index == OPTIONS_MENU_NAME_INDEX) {

									} else {
										myNameTextField.setActive(false);
										myName = myNameTextField.getText().trim();
										myNameTextField.setText(myName);
									}
								}
								optionsMenuSelectedIndex = index;
							} else if (optionsMenuSelectedIndex == 0) {
								//sound = !sound;
							} else if (optionsMenuSelectedIndex == 1) {
								vibration = !vibration;
								vibrate(VIBRATE_TIME_HIT);
							} else if (optionsMenuSelectedIndex == 2) {
								fontIndex++;
								fontIndex %= blackFonts.length;
								updateFontFromIndex();
							} else if (optionsMenuSelectedIndex == 3) {
								myNameTextField.setActive(true);
								myNameTextField.setDimensions(getItemsWidth(optionsMenuItems, getOptionsMenuValues()), TextField.SIZE_PREFERRED);
							} else if (optionsMenuSelectedIndex == 4) {
								try {
									resetData();
									updateFontFromIndex();
									myNameTextField.setText(myName);
								} catch (Exception ex) {
									//#debug
									debug("SC.rs_reset", ex);
								}
							} else if (optionsMenuSelectedIndex == 5) {
								state = STATE_MAIN_MENU;
								try {
									writeData();
								} catch (Exception ex) {
									//#debug
									debug("SC.options.rs_write", ex);
								}
							}
						} else if (myNameTextField.isActive()) {
							myNameTextField.setActive(false);
							myName = myNameTextField.getText().trim();
							myNameTextField.setText(myName);
						}
					}
				}
				//#endif
				break;
			case STATE_ABOUT_MENU:
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == FIRE) {
						state = STATE_MAIN_MENU;
						break;
					}
					//#mdebug
					else if (keysPressed[i] == KEY_STAR) {
						renderExceptions = !renderExceptions;
					} else if (renderExceptions) {
						if (gameAction == UP) {
							if (renderExceptionsY > 0) {
								renderExceptionsY -= getFontHeight(blackFont);
								if (renderExceptionsY < 0) {
									renderExceptionsY = 0;
								}
							}
						} else if (gameAction == DOWN) {
							renderExceptionsY += getFontHeight(blackFont);
						} else if (gameAction == LEFT) {
							if (renderExceptionsX > 0) {
								renderExceptionsX -= getFontHeight(blackFont);
								if (renderExceptionsX < 0) {
									renderExceptionsX = 0;
								}
							}
						} else if (gameAction == RIGHT) {
							renderExceptionsX += getFontHeight(blackFont);
						} else if (keysPressed[i] == KEY_POUND) {
							renderExceptionsX = 0;
							renderExceptionsY = 0;
						}
					}
					//#enddebug
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(aboutMenuItems, null, pointerStartX, pointerStartY);
					if (index == aboutMenuItems.length - 1) {
						state = STATE_MAIN_MENU;
					}
					//#mdebug
					else if (pointerStartX >= 7 * getWidth() / 8 && pointerStartY < getHeight() / 8) {
						renderExceptions = !renderExceptions;
					}
					//#enddebug
				}
				//#mdebug
				if (renderExceptions) {
					int w = getWidth();
					int h = getHeight();
					if (pointerState == POINTER_STATE_PRESSED) {
						if (pointerStartX < w / 4 && pointerStartY < h / 4) {
							renderExceptionsX = 0;
							renderExceptionsY = 0;
						}
						lastDraggedX = pointerEndX;
						lastDraggedY = pointerEndY;
					} else if (pointerState == POINTER_STATE_DRAGGED) {
						renderExceptionsX -= pointerEndX - lastDraggedX;
						renderExceptionsY -= pointerEndY - lastDraggedY;
						if (renderExceptionsX < 0) {
							renderExceptionsX = 0;
						}
						if (renderExceptionsY < 0) {
							renderExceptionsY = 0;
						}
						lastDraggedX = pointerEndX;
						lastDraggedY = pointerEndY;
					}
				}
				//#enddebug
				//#endif
				break;
			case STATE_WRONG_NAME:
				for (int i = 0; i < keysPressedCount; i++) {
					if (getGameAction(keysPressed[i]) == FIRE) {
						state = STATE_OPTIONS_MENU;
						optionsMenuSelectedIndex = OPTIONS_MENU_NAME_INDEX;
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(serverWaitMenuItems, null, pointerStartX, pointerStartY);
					if (index == serverWaitMenuItems.length - 1) {
						state = STATE_OPTIONS_MENU;
						optionsMenuSelectedIndex = OPTIONS_MENU_NAME_INDEX;
					}
				}
				//#endif
				break;
			case STATE_SERVER_WAIT:
				for (int i = 0; i < keysPressedCount; i++) {
					if (getGameAction(keysPressed[i]) == FIRE) {
						try {
							notifier.close();
						} catch (NullPointerException ex) {
							//#debug
							debug("SC.notifier_npe", ex);
						} catch (IOException ex) {
							//#debug
							debug("SC.notifier_io", ex);
						}
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(serverWaitMenuItems, null, pointerStartX, pointerStartY);
					if (index == serverWaitMenuItems.length - 1) {
						try {
							notifier.close();
						} catch (NullPointerException ex) {
							//#debug
							debug("SC.notifier_npe", ex);
						} catch (IOException ex) {
							//#debug
							debug("SC.notifier_io", ex);
						}
					}
				}
				//#endif
				break;
			case STATE_CLIENT_SEARCH:
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == UP) {
						if (clientSearchSelectedIndex > 0) {
							clientSearchSelectedIndex--;
						}
					} else if (gameAction == DOWN) {
						if (clientSearchSelectedIndex < sdHelper.getServiceCount()) {
							clientSearchSelectedIndex++;
						}
					} else if (gameAction == FIRE) {
						if (clientSearchSelectedIndex == 0) {
							state = STATE_MAIN_MENU;
							sdHelper.stop();
						} else {
							isServer = false;
							hasFirstStarted = false;
							oppName = sdHelper.getName(clientSearchSelectedIndex - 1);
							clientConnectionURL = sdHelper.getConnectionURL(clientSearchSelectedIndex - 1);
							sdHelper.stop();
							startConnection();
							state = STATE_CLIENT_WAIT;
						}
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(getClientSearchMenuItems(), getClientSearchMenuValues(), pointerStartX, pointerStartY);
					if (index > 0) {
						index--;
						if (clientSearchSelectedIndex != index) {
							clientSearchSelectedIndex = index;
						} else {
							if (clientSearchSelectedIndex == 0) {
								state = STATE_MAIN_MENU;
								sdHelper.stop();
							} else {
								isServer = false;
								hasFirstStarted = false;
								oppName = sdHelper.getName(clientSearchSelectedIndex - 1);
								clientConnectionURL = sdHelper.getConnectionURL(clientSearchSelectedIndex - 1);
								sdHelper.stop();
								startConnection();
								state = STATE_CLIENT_WAIT;
							}
						}
					}
				}
				//#endif
				break;
			case STATE_CLIENT_WAIT:
				break;
			case STATE_INCOMPATIBLE_VERSIONS:
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == FIRE) {
						state = STATE_MAIN_MENU;
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(incompatibleVersionsMenuItems, null, pointerStartX, pointerStartY);
					if (index == incompatibleVersionsMenuItems.length - 1) {
						state = STATE_MAIN_MENU;
					}
				}
				//#endif
				break;
			case STATE_GAME_OPTIONS_MENU:
				if (chatTextField.isActive()) {
					updateChat(dt);
					return;
				}
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == UP) {
						if (gameOptionsMenuSelectedIndex > (isServer ? 0 : gameOptionsMenuItems.length - 3)) {
							gameOptionsMenuSelectedIndex--;
						}
					} else if (gameAction == DOWN) {
						if (gameOptionsMenuSelectedIndex < gameOptionsMenuItems.length - 1) {
							gameOptionsMenuSelectedIndex++;
						}
					} else if (gameAction == LEFT) {
						if (gameOptionsMenuSelectedIndex == 0) {
							if (sizeX > 2) {
								sizeX--;
								if (checkGameOptions()) {
									sendOption(BTC_OPTION_X, sizeX);
								} else {
									sizeX++;
								}
							}
						} else if (gameOptionsMenuSelectedIndex == 1) {
							if (sizeY > 2) {
								sizeY--;
								if (checkGameOptions()) {
									sendOption(BTC_OPTION_Y, sizeY);
								} else {
									sizeY++;
								}
							}
						}
					} else if (gameAction == RIGHT) {
						if (gameOptionsMenuSelectedIndex == 0) {
							if (sizeX < 32) {
								sizeX++;
								sendOption(BTC_OPTION_X, sizeX);
							}
						} else if (gameOptionsMenuSelectedIndex == 1) {
							if (sizeY < 32) {
								sizeY++;
								sendOption(BTC_OPTION_Y, sizeY);
							}
						}
					} else if (gameAction == FIRE) {
						if (gameOptionsMenuSelectedIndex == 2) {
							state = STATE_GAME_OPTIONS_SHIPS_MENU;
						} else if (gameOptionsMenuSelectedIndex == 3) {
							if (isServer) {
								myTurnNextGame = true;
								gameSetup();
								send(BTC_GAME_FIRST_START);
								state = STATE_WAIT_FOR_ACCEPT;
							} else if (hasFirstStarted) {
								gameSetup();
								send(BTC_GAME_SECOND_START);
								if (Utils.random() % 2 == 0) {
									send(BTC_YOUR_TURN);
									myTurnNextGame = true;
								} else {
									myTurn = true;
									myTurnNextGame = false;
								}
								state = STATE_GAME_SETUP;
							}
						} else if (gameOptionsMenuSelectedIndex == 4) {
							send(BTC_CLOSE);
							sendClose = false;
							state = STATE_MAIN_MENU;
						}
						break;
					} else if (keysPressed[i] == KEY_LEFT_SOFT) {
						startWriting();
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(gameOptionsMenuItems, getGameOptionsMenuValues(), pointerStartX, pointerStartY);
					if (index >= (isServer ? 0 : gameOptionsMenuItems.length - 3)) {
						if (gameOptionsMenuSelectedIndex != index) {
							gameOptionsMenuSelectedIndex = index;
						} else if (gameOptionsMenuSelectedIndex == 0) {
							if (pointerStartX < getWidth() / 2) {
								if (sizeX > 2) {
									sizeX--;
									if (checkGameOptions()) {
										sendOption(BTC_OPTION_X, sizeX);
									} else {
										sizeX++;
									}
								}
							} else {
								if (sizeX < 32) {
									sizeX++;
									sendOption(BTC_OPTION_X, sizeX);
								}
							}
						} else if (gameOptionsMenuSelectedIndex == 1) {
							if (pointerStartX < getWidth() / 2) {
								if (sizeY > 2) {
									sizeY--;
									if (checkGameOptions()) {
										sendOption(BTC_OPTION_Y, sizeY);
									} else {
										sizeY++;
									}
								}
							} else {
								if (sizeY < 32) {
									sizeY++;
									sendOption(BTC_OPTION_Y, sizeY);
								}
							}
						} else if (gameOptionsMenuSelectedIndex == 2) {
							state = STATE_GAME_OPTIONS_SHIPS_MENU;
						} else if (gameOptionsMenuSelectedIndex == 3) {
							if (isServer) {
								myTurnNextGame = true;
								gameSetup();
								send(BTC_GAME_FIRST_START);
								state = STATE_WAIT_FOR_ACCEPT;
							} else if (hasFirstStarted) {
								gameSetup();
								send(BTC_GAME_SECOND_START);
								if (Utils.random() % 2 == 0) {
									send(BTC_YOUR_TURN);
									myTurnNextGame = true;
								} else {
									myTurn = true;
									myTurnNextGame = false;
								}
								state = STATE_GAME_SETUP;
							}
						} else if (gameOptionsMenuSelectedIndex == 4) {
							send(BTC_CLOSE);
							sendClose = false;
							state = STATE_MAIN_MENU;
						}
					}
					if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
						startWriting();
					}
				}
				//#endif
				break;
			case STATE_GAME_OPTIONS_SHIPS_MENU:
				if (chatTextField.isActive()) {
					updateChat(dt);
					return;
				}
				for (int i = 0; i < keysPressedCount; i++) {
					int gameAction = getGameAction(keysPressed[i]);
					if (gameAction == UP) {
						if (isServer && gameOptionsShipsMenuSelectedIndex > 0) {
							gameOptionsShipsMenuSelectedIndex--;
						}
					} else if (gameAction == DOWN) {
						if (isServer && gameOptionsShipsMenuSelectedIndex < gameOptionsShipsMenuItems.length - 1) {
							gameOptionsShipsMenuSelectedIndex++;
						}
					} else if (gameAction == LEFT) {
						if (gameOptionsShipsMenuSelectedIndex == 0) {
							decreaseShip5Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 1) {
							decreaseShip4Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 2) {
							decreaseShip3Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 3) {
							decreaseShip2Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 4) {
							decreaseShip1Count();
						}
					} else if (gameAction == RIGHT) {
						if (gameOptionsShipsMenuSelectedIndex == 0) {
							increaseShip5Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 1) {
							increaseShip4Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 2) {
							increaseShip3Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 3) {
							increaseShip2Count();
						} else if (gameOptionsShipsMenuSelectedIndex == 4) {
							increaseShip1Count();
						}
					} else if (gameAction == FIRE) {
						if (gameOptionsShipsMenuSelectedIndex == 5) {
							state = STATE_GAME_OPTIONS_MENU;
						}
						break;
					} else if (keysPressed[i] == KEY_LEFT_SOFT) {
						startWriting();
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					int index = getMenuIndex(gameOptionsShipsMenuItems, getGameOptionsShipsMenuValues(), pointerStartX, pointerStartY);
					if (index >= (isServer ? 0 : gameOptionsShipsMenuItems.length - 1)) {
						if (gameOptionsShipsMenuSelectedIndex != index) {
							gameOptionsShipsMenuSelectedIndex = index;
						} else if (gameOptionsShipsMenuSelectedIndex == 0) {
							if (pointerStartX < getWidth() / 2) {
								decreaseShip5Count();
							} else {
								increaseShip5Count();
							}
						} else if (gameOptionsShipsMenuSelectedIndex == 1) {
							if (pointerStartX < getWidth() / 2) {
								decreaseShip4Count();
							} else {
								increaseShip4Count();
							}
						} else if (gameOptionsShipsMenuSelectedIndex == 2) {
							if (pointerStartX < getWidth() / 2) {
								decreaseShip3Count();
							} else {
								increaseShip3Count();
							}
						} else if (gameOptionsShipsMenuSelectedIndex == 3) {
							if (pointerStartX < getWidth() / 2) {
								decreaseShip2Count();
							} else {
								increaseShip2Count();
							}
						} else if (gameOptionsShipsMenuSelectedIndex == 4) {
							if (pointerStartX < getWidth() / 2) {
								decreaseShip1Count();
							} else {
								increaseShip1Count();
							}
						} else if (gameOptionsShipsMenuSelectedIndex == 5) {
							state = STATE_GAME_OPTIONS_MENU;
						}
					}
					if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
						startWriting();
					}
				}
				//#endif
				break;
			case STATE_WAIT_FOR_ACCEPT:
				if (chatTextField.isActive()) {
					updateChat(dt);
					return;
				}
				for (int i = 0; i < keysPressedCount; i++) {
					if (keysPressed[i] == KEY_LEFT_SOFT) {
						startWriting();
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
						startWriting();
					}
				}
				//#endif
				break;
			case STATE_GAME_SETUP:
				if (meReady && oppReady) {
					state = STATE_GAME_PLAYING;
					stateTime = 0;
				}
				if (chatTextField.isActive()) {
					updateChat(dt);
					return;
				}
				if (!meReady) {
					for (int i = 0; i < keysPressedCount; i++) {
						int gameAction = getGameAction(keysPressed[i]);
						int key = mapKey(keysPressed[i]);
						moveCursor(gameAction, key, true);
						if (key == KEY_NUM0) {
							restartGameSetup();
						} else if (key == KEY_POUND) {
							rotateNextShip();
						} else if (gameAction == FIRE) {
							tryDropShip();
						}
					}
					//#if TouchScreen
					if (pointerState == POINTER_STATE_PRESSED) {
						int w = getWidth();
						int h = getHeight();
						if (pointerStartX >= w - arrowsImageWidth && pointerStartY < arrowsImageHeight) {
							handleArrowsPressed(true);
						} else if (w >= h && pointerStartX >= w - Math.max(restartImageWidth, rotateImageWidth)) {
							int yOffset = (h - arrowsImageHeight - restartImageHeight - rotateImageHeight - chatImageHeight) / 3;
							if (arrowsImageHeight + yOffset <= pointerStartY && pointerStartY < arrowsImageHeight + yOffset + restartImageHeight) {
								restartGameSetup();
							} else if (h - chatImageHeight - yOffset - rotateImageHeight <= pointerStartY && pointerStartY < h - chatImageHeight - yOffset) {
								rotateNextShip();
							}
						} else if (w < h && pointerStartY < Math.max(restartImageHeight, rotateImageHeight)) {
							int xOffset = (w - restartImageWidth - rotateImageWidth - arrowsImageWidth) / 3;
							if (xOffset <= pointerStartX && pointerStartX < xOffset + restartImageWidth) {
								restartGameSetup();
							} else if (w - arrowsImageWidth - xOffset - rotateImageWidth <= pointerStartX && pointerStartX < w - arrowsImageWidth - xOffset) {
								rotateNextShip();
							}
						} else {
							int chatHeight = oppWriting || !messages.isEmpty() ? (getFontHeight(blackFont) + 5) : 0;
							int widthForHUD = 0;
							int heightForHUD = 0;
							if (hasPointerEvents()) {
								if (w >= h) {
									widthForHUD = arrowsImageWidth;
								} else {
									heightForHUD = arrowsImageHeight;
								}
								if (chatHeight < chatImageHeight) {
									chatHeight = chatImageHeight;
								}
							}
							int size = Math.min((w - 1 - widthForHUD) / sizeX, (h - 1 - heightForHUD - chatHeight) / sizeY);
							if (size > Math.max(w, h) >> 3) {
								size = Math.max(w, h) >> 3;
							}
							int x = (w - 1 - widthForHUD - size * sizeX) >> 1;
							int y = heightForHUD + (h - 1 - chatHeight - size * sizeY) >> 1;
							if (pointerStartX >= x && pointerStartY >= y) {
								int newCursorX = (pointerStartX - x) / size;
								int newCursorY = (pointerStartY - y) / size;
								if (newCursorX < sizeX && newCursorY < sizeY) {
									int shipSize = getNextShipSize();
									if (nextShipDirection == SD_RIGHT) {
										if (newCursorX > sizeX - shipSize) {
											newCursorX = sizeX - shipSize;
										}
									} else if (nextShipDirection == SD_DOWN) {
										if (newCursorY > sizeY - shipSize) {
											newCursorY = sizeY - shipSize;
										}
									}
									if (myCursorX != newCursorX || myCursorY != newCursorY) {
										myCursorX = newCursorX;
										myCursorY = newCursorY;
									} else {
										tryDropShip();
									}
								}
							}
						}
					}
					//#endif
				}
				for (int i = 0; i < keysPressedCount; i++) {
					if (keysPressed[i] == KEY_LEFT_SOFT) {
						startWriting();
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
						startWriting();
					}
				}
				//#endif
				break;
			case STATE_GAME_PLAYING:
				stateTime += dt;
				if (myTurnTimeout > 0) {
					myTurnTimeout -= dt;
					if (myTurnTimeout <= 0) {
						myTurn = true;
					}
				}
				if (chatTextField.isActive()) {
					updateChat(dt);
					return;
				}
				if (showShipsLeftMenu) {
					for (int i = 0; i < keysPressedCount; i++) {
						if (getGameAction(keysPressed[i]) == FIRE || mapKey(keysPressed[i]) == KEY_POUND) {
							showShipsLeftMenu = false;
							break;
						}
					}
					//#if TouchScreen
					if (pointerState == POINTER_STATE_PRESSED) {
						showShipsLeftMenu = false;
					}
					//#endif
					return;
				}
				if (myTurn) {
					for (int i = 0; i < keysPressedCount; i++) {
						int gameAction = getGameAction(keysPressed[i]);
						int key = mapKey(keysPressed[i]);
						int oldCursorX = myCursorX;
						int oldCursorY = myCursorY;
						moveCursor(gameAction, key, false);
						if (oldCursorX != myCursorX || oldCursorY != myCursorY) {
							sendCursorPosition();
						}
						if (gameAction == FIRE) {
							tryShoot();
							break;
						} else if (key == KEY_POUND) {
							showShipsLeftMenu = true;
						}
					}
					//#if TouchScreen
					if (pointerState == POINTER_STATE_PRESSED) {
						int w = getWidth();
						int h = getHeight();
						if (pointerStartX >= w - arrowsImageWidth && pointerStartY < arrowsImageHeight) {
							handleArrowsPressed(false);
						} else if (w >= h && pointerStartX >= w - hintImageWidth) {
							int yOffset = (h - arrowsImageHeight - hintImageHeight - chatImageHeight) / 2;
							if (arrowsImageHeight + yOffset <= pointerStartY && pointerStartY < arrowsImageHeight + yOffset + hintImageHeight) {
								showShipsLeftMenu = true;
							}
						} else if (w < h && pointerStartY < hintImageHeight) {
							int xOffset = (w - hintImageWidth - arrowsImageWidth) / 2;
							if (xOffset <= pointerStartX && pointerStartX < xOffset + hintImageWidth) {
								showShipsLeftMenu = true;
							}
						} else {
							int chatHeight = oppWriting || !messages.isEmpty() ? (getFontHeight(blackFont) + 5) : 0;
							int widthForHUD = 0;
							int heightForHUD = 0;
							if (hasPointerEvents()) {
								if (w >= h) {
									widthForHUD = arrowsImageWidth;
								} else {
									heightForHUD = arrowsImageHeight;
								}
								if (chatHeight < chatImageHeight) {
									chatHeight = chatImageHeight;
								}
							}
							int size = Math.min((w - 1 - widthForHUD) / sizeX, (h - 1 - heightForHUD - chatHeight) / sizeY);
							if (size > Math.max(w, h) >> 3) {
								size = Math.max(w, h) >> 3;
							}
							int x = (w - 1 - widthForHUD - size * sizeX) >> 1;
							int y = heightForHUD + (h - 1 - chatHeight - size * sizeY) >> 1;
							if (pointerStartX >= x && pointerStartY >= y) {
								int newCursorX = (pointerStartX - x) / size;
								int newCursorY = (pointerStartY - y) / size;
								if (newCursorX < sizeX && newCursorY < sizeY) {
									if (myCursorX != newCursorX || myCursorY != newCursorY) {
										myCursorX = newCursorX;
										myCursorY = newCursorY;
										sendCursorPosition();
									} else {
										tryShoot();
									}
								}
							}
						}
					}
					//#endif
				}
				for (int i = 0; i < keysPressedCount; i++) {
					if (keysPressed[i] == KEY_LEFT_SOFT) {
						startWriting();
						break;
					}
				}
				//#if TouchScreen
				if (pointerState == POINTER_STATE_PRESSED) {
					if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
						startWriting();
					}
				}
				//#endif
				break;
			case STATE_WIN:
			case STATE_LOSS:
				stateTime += dt;
				if (chatTextField.isActive()) {
					updateChat(dt);
					return;
				}
				if (showRematchMenu) {
					for (int i = 0; i < keysPressedCount; i++) {
						int gameAction = getGameAction(keysPressed[i]);
						if (gameAction == UP) {
							if (rematchMenuSelectedIndex > 0) {
								rematchMenuSelectedIndex--;
							}
						} else if (gameAction == DOWN) {
							if (rematchMenuSelectedIndex < rematchMenuItems.length - 2) {
								rematchMenuSelectedIndex++;
							}
						} else if (gameAction == FIRE) {
							if (rematchMenuSelectedIndex == 0) {
								showRematchMenu = false;
							} else if (rematchMenuSelectedIndex == 1) {
								if (state == STATE_LOSS) {
									myTurnNextGame = true;
									gameSetup();
									send(BTC_GAME_FIRST_START);
									state = STATE_WAIT_FOR_ACCEPT;
								} else if (hasFirstStarted) {
									gameSetup();
									send(BTC_GAME_SECOND_START);
									if (!myTurnNextGame) {
										send(BTC_YOUR_TURN);
										myTurnNextGame = true;
									} else {
										myTurn = true;
										myTurnNextGame = false;
									}
									state = STATE_GAME_SETUP;
								}
							} else if (rematchMenuSelectedIndex == 2) {
								send(BTC_CLOSE);
								sendClose = false;
								state = STATE_MAIN_MENU;
							}
							break;
						} else if (keysPressed[i] == KEY_LEFT_SOFT) {
							startWriting();
							break;
						}
					}
					//#if TouchScreen
					if (pointerState == POINTER_STATE_PRESSED) {
						int index = getMenuIndex(rematchMenuItems, null, pointerStartX, pointerStartY);
						if (index >= 1) {
							index--;
							if (rematchMenuSelectedIndex != index) {
								rematchMenuSelectedIndex = index;
							} else if (rematchMenuSelectedIndex == 0) {
								showRematchMenu = false;
							} else if (rematchMenuSelectedIndex == 1) {
								if (state == STATE_LOSS) {
									myTurnNextGame = true;
									gameSetup();
									send(BTC_GAME_FIRST_START);
									state = STATE_WAIT_FOR_ACCEPT;
								} else if (hasFirstStarted) {
									gameSetup();
									send(BTC_GAME_SECOND_START);
									if (!myTurnNextGame) {
										send(BTC_YOUR_TURN);
										myTurnNextGame = true;
									} else {
										myTurn = true;
										myTurnNextGame = false;
									}
									state = STATE_GAME_SETUP;
								}
							} else if (rematchMenuSelectedIndex == 2) {
								send(BTC_CLOSE);
								sendClose = false;
								state = STATE_MAIN_MENU;
							}
						}
						if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
							startWriting();
						}
					}
					//#endif
				} else {
					for (int i = 0; i < keysPressedCount; i++) {
						int gameAction = getGameAction(keysPressed[i]);
						if (gameAction == FIRE) {
							showRematchMenu = true;
							break;
						} else if (keysPressed[i] == KEY_LEFT_SOFT) {
							startWriting();
							break;
						}
					}
					//#if TouchScreen
					if (pointerState == POINTER_STATE_PRESSED) {
						if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
							startWriting();
						} else {
							showRematchMenu = true;
						}
					}
					//#endif
				}
				break;
		}
	}

	private void startWriting() {
		chatTextField.setActive(true);
	}

	//#if TouchScreen
	private void handleArrowsPressed(boolean setup) {
		int oldCursorX = myCursorX;
		int oldCursorY = myCursorY;
		int xIndex = (pointerStartX - (getWidth() - arrowsImageWidth)) / (arrowsImageWidth / 3);
		int yIndex = (pointerStartY) / (arrowsImageHeight / 2);
		if (yIndex == 0) {
			if (xIndex == 0 || xIndex == 2) {
				if (setup) {
					tryDropShip();
				} else {
					tryShoot();
				}
			} else {
				moveCursorUp(setup);
			}
		} else {
			if (xIndex == 0) {
				moveCursorLeft(setup);
			} else if (xIndex == 1) {
				moveCursorDown(setup);
			} else {
				moveCursorRight(setup);
			}
		}
		if (!setup && (oldCursorX != myCursorX || oldCursorY != myCursorY)) {
			sendCursorPosition();
		}
	}
	//#endif

	private int mapKey(int key) {
		//#if NokiaE72
//# 		switch (key) {
//# 			case 'r': key = KEY_NUM1; break;
//# 			case 't': key = KEY_NUM2; break;
//# 			case 'y': key = KEY_NUM3; break;
//# 			case 'u': key = KEY_STAR; break;
//# 			case 'f': key = KEY_NUM4; break;
//# 			case 'g': key = KEY_NUM5; break;
//# 			case 'h': key = KEY_NUM6; break;
//# 			case 'j': key = KEY_POUND; break;
//# 			case 'v': key = KEY_NUM7; break;
//# 			case 'b': key = KEY_NUM8; break;
//# 			case 'n': key = KEY_NUM9; break;
//# 			case 'm': key = KEY_NUM0; break;
//# 		}
		//#endif
		return key;
	}

	private void moveCursorUp(boolean setup) {
		if (myCursorY > 0) {
			myCursorY--;
		} else {
			myCursorY = sizeY - (setup && nextShipDirection == SD_DOWN ? getNextShipSize() : 1);
		}
	}

	private void moveCursorDown(boolean setup) {
		if (myCursorY < sizeY - (setup && nextShipDirection == SD_DOWN ? getNextShipSize() : 1)) {
			myCursorY++;
		} else {
			myCursorY = 0;
		}
	}

	private void moveCursorLeft(boolean setup) {
		if (myCursorX > 0) {
			myCursorX--;
		} else {
			myCursorX = sizeX - (setup && nextShipDirection == SD_RIGHT ? getNextShipSize() : 1);
		}
	}

	private void moveCursorRight(boolean setup) {
		if (myCursorX < sizeX - (setup && nextShipDirection == SD_RIGHT ? getNextShipSize() : 1)) {
			myCursorX++;
		} else {
			myCursorX = 0;
		}
	}

	private void moveCursor(int gameAction, int key, boolean setup) {
		if (gameAction == UP || key == KEY_NUM1 || key == KEY_NUM2 || key == KEY_NUM3) {
			moveCursorUp(setup);
		}
		if (gameAction == DOWN || key == KEY_NUM7 || key == KEY_NUM8 || key == KEY_NUM9) {
			moveCursorDown(setup);
		}
		if (gameAction == LEFT || key == KEY_NUM1 || key == KEY_NUM4 || key == KEY_NUM7) {
			moveCursorLeft(setup);
		}
		if (gameAction == RIGHT || key == KEY_NUM3 || key == KEY_NUM6 || key == KEY_NUM9) {
			moveCursorRight(setup);
		}
	}

	private void previousScoresPage() {
		if (scoresPage > 0) {
			scoresPage--;
		} else {
			scoresPage = getScoresMaxPage();
		}
	}

	private void nextScoresPage() {
		if (scoresPage < getScoresMaxPage()) {
			scoresPage++;
		} else {
			scoresPage = 0;
		}
	}

	private void decreaseShip5Count() {
		if (ship5 > 0) {
			ship5--;
			if (simpleCheckGameOptions()) {
				sendOption(BTC_OPTION_SHIP5, ship5);
			} else {
				ship5++;
			}
		}
	}

	private void decreaseShip4Count() {
		if (ship4 > 0) {
			ship4--;
			if (simpleCheckGameOptions()) {
				sendOption(BTC_OPTION_SHIP4, ship4);
			} else {
				ship4++;
			}
		}
	}

	private void decreaseShip3Count() {
		if (ship3 > 0) {
			ship3--;
			if (simpleCheckGameOptions()) {
				sendOption(BTC_OPTION_SHIP3, ship3);
			} else {
				ship3++;
			}
		}
	}

	private void decreaseShip2Count() {
		if (ship2 > 0) {
			ship2--;
			if (simpleCheckGameOptions()) {
				sendOption(BTC_OPTION_SHIP2, ship2);
			} else {
				ship2++;
			}
		}
	}

	private void decreaseShip1Count() {
		if (ship1 > 0) {
			ship1--;
			if (simpleCheckGameOptions()) {
				sendOption(BTC_OPTION_SHIP1, ship1);
			} else {
				ship1++;
			}
		}
	}

	private void increaseShip5Count() {
		ship5++;
		if (checkGameOptions()) {
			sendOption(BTC_OPTION_SHIP5, ship5);
		} else {
			ship5--;
		}
	}

	private void increaseShip4Count() {
		ship4++;
		if (checkGameOptions()) {
			sendOption(BTC_OPTION_SHIP4, ship4);
		} else {
			ship4--;
		}
	}

	private void increaseShip3Count() {
		ship3++;
		if (checkGameOptions()) {
			sendOption(BTC_OPTION_SHIP3, ship3);
		} else {
			ship3--;
		}
	}

	private void increaseShip2Count() {
		ship2++;
		if (checkGameOptions()) {
			sendOption(BTC_OPTION_SHIP2, ship2);
		} else {
			ship2--;
		}
	}

	private void increaseShip1Count() {
		ship1++;
		if (checkGameOptions()) {
			sendOption(BTC_OPTION_SHIP1, ship1);
		} else {
			ship1--;
		}
	}

	//#if TouchScreen
	private int getMenuIndex(String[] items, String[] values, int pointerX, int pointerY) {
		int width = getItemsWidth(items, values);
		int fontHeight = getFontHeight(blackFont);
		int itemsCount = items.length;
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				itemsCount--;
			}
		}
		int height = (2 * MENU_ITEM_MARGIN + MENU_ITEMS_SPACING + fontHeight) * itemsCount - MENU_ITEMS_SPACING;
		int x = (getWidth() - width) >> 1;
		int y = (getHeight() - height) >> 1;
		int w = width;
		int h = fontHeight + 2 * MENU_ITEM_MARGIN;
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				if (Utils.isInRect(pointerX, pointerY, x, y, w, h)) {
					return i;
				}
				y += h + MENU_ITEMS_SPACING;
			}
		}
		return -1;
	}
	//#endif

	private void load() {
		switch (loadProgress) {
			case 0:
				blackFonts = new int[] {
					loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL, Utils.COLOR_BLACK),
					loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM, Utils.COLOR_BLACK),
					loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_LARGE, Utils.COLOR_BLACK),
				};
				whiteFonts = new int[] {
					loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL, Utils.COLOR_WHITE),
					loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM, Utils.COLOR_WHITE),
					loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_LARGE, Utils.COLOR_WHITE),
				};
				fontIndex = 1;

				blackFont = blackFonts[fontIndex];
				whiteFont = whiteFonts[fontIndex];

				lagFont = loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL, Utils.COLOR_PLUM_1);
				break;
			case 1:
				try {
					sdHelper = new ServiceDiscoveryHelper();
				} catch (Throwable ex) {
					//#debug
					debug("SC.initial", ex);
					state = STATE_CLOSING;
					return;
				}
				break;
			case 2:
				try {
					if (!readData()) {
						resetData();
					}
				} catch (Exception exRead) {
					//#debug
					debug("SC.load.rs_read", exRead);
					try {
						resetData();
					} catch (Exception exReset) {
						//#debug
						debug("SC.load.rs_reset", exReset);
					}
				}
				break;
			case 3:
				try {
					if (!readScores()) {
						initScores(0);
					}
				} catch (Exception ex) {
					initScores(0);
					//#debug
					debug("SC.read.score", ex);
				}
				break;
			case 4:
				target1Image = loadImage("/target1.png");
				break;
			case 5:
				target2Image = loadImage("/target2.png");
				break;
			case 6:
				target3Image = loadImage("/target3.png");
				break;
			case 7:
				//#if TouchScreen
				if (hasPointerEvents()) {
					arrowsImage = loadImage("/arrows.png");
					arrowsImageWidth = getImageWidth(arrowsImage);
					arrowsImageHeight = getImageHeight(arrowsImage);
				}
				//#endif
				break;
			case 8:
				//#if TouchScreen
				if (hasPointerEvents()) {
					restartImage = loadImage("/restart.png");
					restartImageWidth = getImageWidth(restartImage);
					restartImageHeight = getImageHeight(restartImage);
					rotateImage = loadImage("/rotate.png");
					rotateImageWidth = getImageWidth(rotateImage);
					rotateImageHeight = getImageHeight(rotateImage);
				}
				//#endif
				break;
			case 9:
				//#if TouchScreen
				if (hasPointerEvents()) {
					hintImage = loadImage("/hint.png");
					hintImageWidth = getImageWidth(hintImage);
					hintImageHeight = getImageHeight(hintImage);
					chatImage = loadImage("/chat.png");
					chatImageWidth = getImageWidth(chatImage);
					chatImageHeight = getImageHeight(chatImage);
				}
				//#endif
				break;
//				messageSound = loadSound("/t1mwoodblock.mid", "audio/midi");
//				hitSound = loadSound("/t1mwoodblock.mid", "audio/midi");
//				missedSound = loadSound("/t1mwoodblock.mid", "audio/midi");
			case 10:
				chatTextField = new TextField(this);
				messages = new Vector();
				myNameTextField = new TextField(this, myName, NAME_MAX_LENGTH, TextField.TYPE_LETTERS | TextField.TYPE_DIGITS, " -_", false);
				//#if TouchScreen
				if (hasPointerEvents()) {
					chatTouchKeyboard = new TouchKeyboard(this, chatTextField);
					myNameTouchKeyboard = new TouchKeyboard(this, myNameTextField);
				}
				//#endif
				updateFontFromIndex();

				state = STATE_MAIN_MENU;
				break;
		}
		loadProgress++;
	}

	private void updateFontFromIndex() {
		blackFont = blackFonts[fontIndex];
		whiteFont = whiteFonts[fontIndex];
		chatTextField.setFontsAndColors(blackFont, whiteFont, Utils.COLOR_WHITE, Utils.COLOR_BLACK, Utils.COLOR_BLACK);
		int w = getWidth();
		//#if TouchScreen
		if (hasPointerEvents()) {
			w -= chatImageWidth;
		}
		//#endif
		chatTextField.setDimensions(w, TextField.SIZE_PREFERRED);
		myNameTextField.setFontsAndColors(blackFont, whiteFont, Utils.COLOR_WHITE, Utils.COLOR_BLACK, Utils.COLOR_BLACK);

		//#if TouchScreen
		if (hasPointerEvents()) {
			chatTouchKeyboard.setFontsAndColors(blackFont, Utils.COLOR_WHITE, Utils.COLOR_BLACK, Utils.COLOR_ALUMINIUM_3);
			chatTouchKeyboard.setDimensions(TouchKeyboard.SIZE_PREFERRED, TouchKeyboard.SIZE_PREFERRED);
			myNameTouchKeyboard.setFontsAndColors(blackFont, Utils.COLOR_WHITE, Utils.COLOR_BLACK, Utils.COLOR_ALUMINIUM_3);
			myNameTouchKeyboard.setDimensions(TouchKeyboard.SIZE_PREFERRED, TouchKeyboard.SIZE_PREFERRED);
		}
		//#endif
	}

	private boolean readData() throws IOException, RecordStoreException {
		byte[] data = readRecordStore(DATA_RS_NAME);
		if (data != null) {
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			dis.readByte();
			sound = dis.readBoolean();
			vibration = dis.readBoolean();
			fontIndex = dis.readInt();
			sizeX = dis.readInt();
			sizeY = dis.readInt();
			ship5 = dis.readInt();
			ship4 = dis.readInt();
			ship3 = dis.readInt();
			ship2 = dis.readInt();
			ship1 = dis.readInt();
			myName = dis.readUTF();
			return true;
		}
		return false;
	}

	private void writeData() throws IOException, RecordStoreException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte(DATA_VERSION);
		dos.writeBoolean(sound);
		dos.writeBoolean(vibration);
		dos.writeInt(fontIndex);
		dos.writeInt(sizeX);
		dos.writeInt(sizeY);
		dos.writeInt(ship5);
		dos.writeInt(ship4);
		dos.writeInt(ship3);
		dos.writeInt(ship2);
		dos.writeInt(ship1);
		dos.writeUTF(myName);
		byte[] data = baos.toByteArray();
		writeRecordStore(DATA_RS_NAME, data);
	}

	private void resetData() throws BluetoothStateException, RecordStoreException {
		deleteRecordStore(DATA_RS_NAME);
		sound = false;
		vibration = true;
		fontIndex = 1;
		if (get7ItemsMenuHeight() > getHeight()) {
			fontIndex = 0;
		}
		sizeX = 10;
		sizeY = 10;
		ship5 = 0;
		ship4 = 1;
		ship3 = 2;
		ship2 = 3;
		ship1 = 4;
		myName = sdHelper.getMyName().trim();
		if (myName.length() > NAME_MAX_LENGTH) {
			myName = myName.substring(0, NAME_MAX_LENGTH);
		}
	}

	private boolean readScores() throws IOException, RecordStoreException {
		byte[] scores = readRecordStore(SCORES_RS_NAME);
		if (scores != null) {
			ByteArrayInputStream bais = new ByteArrayInputStream(scores);
			DataInputStream dis = new DataInputStream(bais);
			dis.readByte();
			int count = dis.readInt();
			initScores(count);
			for (int i = 0; i < count; i++) {
				scoresOppIds[i] = dis.readUTF();
				scoresOppNames[i] = dis.readUTF();
				scoresWinCount[i] = dis.readInt();
				scoresLossCount[i] = dis.readInt();
			}
			return true;
		}
		return false;
	}

	private void writeScores() throws IOException, RecordStoreException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte(SCORES_VERSION);
		int count = scoresOppIds.length;
		dos.writeInt(count);
		for (int i = 0; i < count; i++) {
			dos.writeUTF(scoresOppIds[i]);
			dos.writeUTF(scoresOppNames[i]);
			dos.writeInt(scoresWinCount[i]);
			dos.writeInt(scoresLossCount[i]);
		}
		byte[] scores = baos.toByteArray();
		writeRecordStore(SCORES_RS_NAME, scores);
	}

	private void initScores(int count) {
		scoresOppIds = new String[count];
		scoresOppNames = new String[count];
		scoresWinCount = new int[count];
		scoresLossCount = new int[count];
	}

	private void updateScores(String id, String name, boolean win) throws Exception {
		int count = scoresOppIds.length;
		int index = Utils.indexOf(scoresOppIds, id);
		if (index == -1) {
			index = count;
			increaseScoresCapacity();
		}
		scoresOppIds[index] = id;
		scoresOppNames[index] = name;
		if (win) {
			scoresWinCount[index]++;
		} else {
			scoresLossCount[index]++;
		}
		sortScores();
		writeScores();
	}

	private void updateScores(String id, String name, int win, int loss) throws Exception {
		int index = Utils.indexOf(scoresOppIds, id);
		//#debug
		debug("scores received: " + win + " " + loss, null);
		if (index == -1) {
			int count = scoresOppIds.length;
			increaseScoresCapacity();
			scoresOppIds[count] = id;
			scoresOppNames[count] = name;
			scoresWinCount[count] = win;
			scoresLossCount[count] = loss;
			sortScores();
			writeScores();
			//#debug
			debug("scores updated: " + id + " " + name, null);
		}
	}

	private void increaseScoresCapacity() {
		int count = scoresOppIds.length;
		String[] tmpOppIds = scoresOppIds;
		String[] tmpOppNames = scoresOppNames;
		int[] tmpWinCount = scoresWinCount;
		int[] tmpLossCount = scoresLossCount;
		initScores(count + 1);
		System.arraycopy(tmpOppIds, 0, scoresOppIds, 0, count);
		System.arraycopy(tmpOppNames, 0, scoresOppNames, 0, count);
		System.arraycopy(tmpWinCount, 0, scoresWinCount, 0, count);
		System.arraycopy(tmpLossCount, 0, scoresLossCount, 0, count);
	}

	private void sortScores() {
		int j;
		for (int i = 1; i < scoresOppIds.length; i++) {
			j = i;

			String id = scoresOppIds[j];
			String name = scoresOppNames[j];
			int win = scoresWinCount[j];
			int loss = scoresLossCount[j];

			int key = getScoresWinLossRatio(j);

			while (j > 0 && getScoresWinLossRatio(j - 1) < key) {
				scoresOppIds[j] = scoresOppIds[j - 1];
				scoresOppNames[j] = scoresOppNames[j - 1];
				scoresWinCount[j] = scoresWinCount[j - 1];
				scoresLossCount[j] = scoresLossCount[j - 1];
				j--;
			}
			scoresOppIds[j] = id;
			scoresOppNames[j] = name;
			scoresWinCount[j] = win;
			scoresLossCount[j] = loss;
		}
	}

	private int getScoresWinLossRatio(int index) {
		int win = scoresWinCount[index];
		int loss = scoresLossCount[index];
		return (win << 10) / (win + loss);
	}

	private int get7ItemsMenuHeight() {
		int fontHeight = getFontHeight(blackFonts[fontIndex]);
		int height = 2 * MENU_MARGIN + (2 * MENU_ITEM_MARGIN + MENU_ITEMS_SPACING + fontHeight) * 7 - MENU_ITEMS_SPACING;
		return height;
	}

	private boolean checkMyName() {
		if (myName.length() == 0) {
			state = STATE_WRONG_NAME;
			return false;
		}
		return true;
	}

	private void startConnection() {
		sendClose = true;
		readerThread = new Thread(this);
		readerThread.setPriority(Thread.MAX_PRIORITY);
		readerThread.start();
	}

	private boolean checkGameOptions() {
		return simpleCheckGameOptions() && (checkGameOptions(sizeX, sizeY) || checkGameOptions(sizeY, sizeX));
	}

	private boolean simpleCheckGameOptions() {
		return ship1 > 0 || ship2 > 0 || ship3 > 0 || ship4 > 0 || ship5 > 0;
	}

	private boolean checkGameOptions(int w, int h) {
		if (ship5 > 0 && w < 5 || ship4 > 0 && w < 4 || ship3 > 0 && w < 3) {
			return false;
		}
		int[] ships = { 0, ship1, ship2, ship3, ship4, ship5 };
		int len = w;
		boolean removed;
		while (h > 0 && (ships[1] > 0 || ships[2] > 0 || ships[3] > 0 || ships[4] > 0 || ships[5] > 0)) {
			removed = false;
			for (int i = len > 5 ? 5 : len; i > 0; i--) {
				if (ships[i] > 0) {
					ships[i]--;
					len -= i + 1;
					removed = true;
					break;
				}
			}
			if (len <= 0 || !removed) {
				h -= 3;
				len = w;
			}
		}
		return !(ships[1] > 0 || ships[2] > 0 || ships[3] > 0 || ships[4] > 0 || ships[5] > 0);
	}

	private void gameSetup() {
		myShips = new byte[sizeY][sizeX];
		oppShips = new byte[sizeY][sizeX];
		myCursorX = (sizeX - 1) / 2;
		myCursorY = (sizeY - 1) / 2;
		shipsToDrop = new int[] { ship5, ship4, ship3, ship2, ship1 };
		shipsLeft = new int[] { ship5, ship4, ship3, ship2, ship1 };
		int shipSize = getNextShipSize();
		if (sizeY > sizeX) {
			nextShipDirection = SD_DOWN;
			if (myCursorY > sizeY - shipSize) {
				myCursorY = sizeY - shipSize;
			}
		} else {
			nextShipDirection = SD_RIGHT;
			if (myCursorX > sizeX - shipSize) {
				myCursorX = sizeX - shipSize;
			}
		}

		myTurn = false;
		
		meReady = false;
		oppReady = false;

		showShipsLeftMenu = false;

		shotLag = -1;
	}

	private byte getNextShipSize() {
		byte nextShipSize = 5;
		for (int i = 0; i < shipsToDrop.length; i++) {
			if (shipsToDrop[i] > 0) {
				break;
			}
			nextShipSize--;
		}
		return nextShipSize;
	}

	private void restartGameSetup() {
		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				myShips[i][j] = SHIP_UNKNOWN;
			}
		}
		myCursorX = (sizeX - 1) / 2;
		myCursorY = (sizeY - 1) / 2;
		shipsToDrop = new int[] { ship5, ship4, ship3, ship2, ship1 };
		int shipSize = getNextShipSize();
		if (sizeY > sizeX) {
			nextShipDirection = SD_DOWN;
			if (myCursorY > sizeY - shipSize) {
				myCursorY = sizeY - shipSize;
			}
		} else {
			nextShipDirection = SD_RIGHT;
			if (myCursorX > sizeX - shipSize) {
				myCursorX = sizeX - shipSize;
			}
		}
	}

	private void rotateNextShip() {
		if (nextShipDirection == SD_RIGHT) {
			int shipSize = getNextShipSize();
			if (sizeY - shipSize >= 0) {
				nextShipDirection = SD_DOWN;
				if (myCursorY > sizeY - shipSize) {
					myCursorY = sizeY - shipSize;
				}
			}
		} else if (nextShipDirection == SD_DOWN) {
			int shipSize = getNextShipSize();
			if (sizeX - shipSize >= 0) {
				nextShipDirection = SD_RIGHT;
				if (myCursorX > sizeX - shipSize) {
					myCursorX = sizeX - shipSize;
				}
			}
		}
	}

	private void tryDropShip() {
		byte shipSize = getNextShipSize();
		int shipTopX = myCursorX;
		int shipTopY = myCursorY;
		int shipBottomX = myCursorX;
		int shipBottomY = myCursorY;
		if (nextShipDirection == SD_RIGHT) {
			shipBottomX += shipSize - 1;
		} else if (nextShipDirection == SD_DOWN) {
			shipBottomY += shipSize - 1;
		}
		for (int i = shipTopY - 1; i <= shipBottomY + 1; i++) {
			if (i < 0 || i >= sizeY) {
				continue;
			}
			for (int j = shipTopX - 1; j <= shipBottomX + 1; j++) {
				if (j < 0 || j >= sizeX) {
					continue;
				}
				if (myShips[i][j] != SHIP_UNKNOWN) {
					return;
				}
			}
		}
		for (int i = shipTopY; i <= shipBottomY; i++) {
			for (int j = shipTopX; j <= shipBottomX; j++) {
				myShips[i][j] = shipSize;
			}
		}
		shipsToDrop[5 - shipSize]--;
		boolean ready = true;
		for (int i = 0; i < shipsToDrop.length; i++) {
			if (shipsToDrop[i] > 0) {
				ready = false;
			}
		}
		if (ready) {
			synchronized (writeBuffer) {
				myCursorX = (sizeX - 1) / 2;
				myCursorY = (sizeY - 1) / 2;
				sendCursorPosition();
				send(BTC_READY);
			}
			meReady = true;
		}
	}

	private void tryShoot() {
		if (!meWaitingAfterShot && oppShips[myCursorY][myCursorX] == SHIP_UNKNOWN) {
			sendShot();
			meWaitingAfterShot = true;
		}
	}

	private void updateChat(int dt) {
		chatTextField.update(dt);
		//#if TouchScreen
		if (hasPointerEvents()) {
			chatTouchKeyboard.update(dt);
		}
		//#endif
		for (int i = 0; i < keysPressedCount; i++) {
			if (keysPressed[i] == KEY_SELECT || keysPressed[i] == KEY_ENTER) {
				maybeSendMessage();
				chatTextField.setActive(false);
			} else if (keysPressed[i] == KEY_LEFT_SOFT) {
				chatTextField.setActive(false);
			} else {
				chatTextField.keyPressed(keysPressed[i]);
			}
		}
		//#if TouchScreen
		if (pointerState == POINTER_STATE_PRESSED) {
			if (pointerStartY < chatTouchKeyboard.getHeight()) {
				chatTouchKeyboard.pointerPressed(pointerStartX, pointerStartY);
			} else if (pointerStartX >= getWidth() - chatImageWidth && pointerStartY >= getHeight() - chatImageHeight) {
				maybeSendMessage();
			} else {
				chatTextField.setActive(false);
			}
		}
		//#endif
		int len = chatTextField.length();
		if (writing && (len == 0 || !chatTextField.isActive())) {
			send(BTC_STOPPED_WRITING);
			writing = false;
			//#debug
			debug("stopped writing", null);
		} else if (!writing && len > 0) {
			send(BTC_STARTED_WRITING);
			writing = true;
			//#debug
			debug("started writing", null);
		}
	}

	private void maybeSendMessage() {
		String text = chatTextField.getText();
		if (text.length() > 0) {
			chatTextField.setText("");
			text = text.trim();
			if (text.length() > 0) {
				sendMessage(text);
			}
		}
	}

	protected void render(Graphics g) {
		int w = getWidth();
		int h = getHeight();
		fillRect(g, Utils.COLOR_PLUM_3, 0, 0, w, h);
		if (state == STATE_LOADING) {
			renderMenu(g, new String[] { "Ładowanie... " + (10 * loadProgress) + "%" }, -1);
			return;
		} else if (state == STATE_CLOSING) {
			renderMenu(g, closingMenuItems, closingMenuItems.length - 1);
			return;
		}
		int chatHeight = oppWriting || !messages.isEmpty() ? (getFontHeight(blackFont) + 5) : 0;
		int widthForHud = 0;
		int heightForHUD = 0;
		//#if TouchScreen
		if (hasPointerEvents() && (state == STATE_GAME_SETUP || state == STATE_GAME_PLAYING && myTurn)) {
			if (w >= h) {
				widthForHud = arrowsImageWidth;
			} else {
				heightForHUD = arrowsImageHeight;
			}
			if (chatHeight < chatImageHeight) {
				chatHeight = chatImageHeight;
			}
		}
		//#endif
		int size = Math.min((w - 1 - widthForHud) / sizeX, (h - 1 - heightForHUD - chatHeight) / sizeY);
		if (size > Math.max(w, h) >> 3) {
			size = Math.max(w, h) >> 3;
		}
		int x = (w - 1 - widthForHud - size * sizeX) >> 1;
		int y = heightForHUD + (h - 1 - chatHeight - size * sizeY) >> 1;
		renderField(g, x, y, size);
		x++; y++;

		switch (state) {
			case STATE_MAIN_MENU:
				renderMenu(g, mainMenuItems, mainMenuSelectedIndex);
				break;
			case STATE_SCORES_MENU:
				renderMenu(g, getScoresMenuItems(), getScoresMenuValues(), getScoresMenuSelectedIndex());
				break;
			case STATE_OPTIONS_MENU:
				renderMenu(g, optionsMenuItems, getOptionsMenuValues(), optionsMenuSelectedIndex);
				break;
			case STATE_ABOUT_MENU:
				//#mdebug
				if (renderExceptions) {
					fillRect(g, Utils.COLOR_WHITE, 0, 0, w, h);
					int fontHeight = getFontHeight(blackFont);
					int index = Application.exceptions.size() - 1;
					y = -renderExceptionsY;
					while (index >= 0 && y < h) {
						if (y + fontHeight > 0) {
							drawString(g, (String) Application.exceptions.elementAt(index), blackFont, -renderExceptionsX, y, Graphics.TOP | Graphics.LEFT);
						}
						index--;
						y += fontHeight;
					}
					return;
				} else
				//#enddebug
				{
					renderMenu(g, aboutMenuItems, aboutMenuItems.length - 1);
				}
				break;
			case STATE_WRONG_NAME:
				renderMenu(g, wrongNameMenuItems, wrongNameMenuItems.length - 1);
				break;
			case STATE_SERVER_WAIT:
				renderMenu(g, serverWaitMenuItems, serverWaitMenuItems.length - 1);
				break;
			case STATE_CLIENT_SEARCH:
				renderMenu(g, getClientSearchMenuItems(), getClientSearchMenuValues(), clientSearchSelectedIndex + 1);
				break;
			case STATE_CLIENT_WAIT:
				renderMenu(g, new String[] { "Łączenie..." }, -1);
				break;
			case STATE_INCOMPATIBLE_VERSIONS:
				renderMenu(g, incompatibleVersionsMenuItems, incompatibleVersionsMenuItems.length - 1);
				break;
			case STATE_GAME_OPTIONS_MENU:
				renderMenu(g, gameOptionsMenuItems, getGameOptionsMenuValues(), gameOptionsMenuSelectedIndex);
				break;
			case STATE_GAME_OPTIONS_SHIPS_MENU:
				renderMenu(g, gameOptionsShipsMenuItems, getGameOptionsShipsMenuValues(), gameOptionsShipsMenuSelectedIndex);
				break;
			case STATE_WAIT_FOR_ACCEPT:
				renderMenu(g, new String[] { "Oczekiwanie..." }, -1);
				break;
			case STATE_GAME_SETUP:
				renderShips(g, x, y, size, myShips);
				if (!meReady) {
					renderNextShip(g, x, y, size);
					//#if TouchScreen
					if (hasPointerEvents()) {
						drawImage(g, arrowsImage, w, 0, Graphics.TOP | Graphics.RIGHT);
						if (w >= h) {
							int yOffset = (h - arrowsImageHeight - restartImageHeight - rotateImageHeight - chatImageHeight) / 3;
							drawImage(g, restartImage, w, arrowsImageHeight + yOffset, Graphics.TOP | Graphics.RIGHT);
							drawImage(g, rotateImage, w, h - chatImageHeight - yOffset, Graphics.BOTTOM | Graphics.RIGHT);
						} else {
							int xOffset = (w - restartImageWidth - rotateImageWidth - arrowsImageWidth) / 3;
							drawImage(g, restartImage, xOffset, 0, Graphics.TOP | Graphics.LEFT);
							drawImage(g, rotateImage, w - arrowsImageWidth - xOffset, 0, Graphics.TOP | Graphics.RIGHT);
						}
					}
					//#endif
				} else if (!oppReady) {
					renderMenu(g, new String[] { "Oczekiwanie..." }, -1);
				}
				break;
			case STATE_GAME_PLAYING:
				if (myTurn) {
					renderShips(g, x, y, size, oppShips);
					renderCursor(g, x, y, size, myCursorX, myCursorY);//, MY_CURSOR_COLOR_1, MY_CURSOR_COLOR_2);
					//#if TouchScreen
					if (hasPointerEvents()) {
						drawImage(g, arrowsImage, w, 0, Graphics.TOP | Graphics.RIGHT);
						if (w >= h) {
							int yOffset = (h - arrowsImageHeight - hintImageHeight - chatImageHeight) / 2;
							drawImage(g, hintImage, w, arrowsImageHeight + yOffset, Graphics.TOP | Graphics.RIGHT);
						} else {
							int xOffset = (w - hintImageWidth - arrowsImageWidth) / 2;
							drawImage(g, hintImage, xOffset, 0, Graphics.TOP | Graphics.LEFT);
						}
					}
					//#endif
					if (showShipsLeftMenu) {
						renderMenu(g, shipsLeftMenuItems, getShipsLeftMenuValues(), -1);
					}
				} else {
					renderShips(g, x, y, size, myShips);
					renderCursor(g, x, y, size, oppCursorX, oppCursorY);//, OPP_CURSOR_COLOR_1, OPP_CURSOR_COLOR_2);
				}
				if (shotLag != -1) {
					drawString(g, "lag:" + shotLag, lagFont, w, 0, Graphics.TOP | Graphics.RIGHT);
				}
				break;
			case STATE_WIN:
			case STATE_LOSS:
				if (stateTime / 3000 % 2 == 0) {
					renderShips(g, x, y, size, oppShips);
				} else {
					renderShips(g, x, y, size, myShips);
				}
				if (showRematchMenu) {
					if (state == STATE_WIN) {
						rematchMenuItems[0] = "Wygrana";
						if (hasFirstStarted) {
							rematchMenuItems[2] = "Rewanż";
						} else {
							rematchMenuItems[2] = "";
						}
					} else {
						rematchMenuItems[0] = "Przegrana";
						rematchMenuItems[2] = "Rewanż";
					}
					renderMenu(g, rematchMenuItems, rematchMenuSelectedIndex + 1);
				}
				break;
		}
		if (chatTextField.isActive()) {
			int textFieldHeight = chatTextField.getPreferredHeight();
			if (!messages.isEmpty()) {
				int index = (messages.size() > 5 ? messages.size() - 5 : 0);
				chatHeight = getFontHeight(blackFont) * (messages.size() - index) + 4;
				int messagesY = h - chatHeight - textFieldHeight;
				fillRect(g, Utils.COLOR_SKY_BLUE_1, 0, messagesY, w, chatHeight);
				drawLine(g, Utils.COLOR_BLACK, 0, messagesY - 1, w, messagesY - 1);
				messagesY += 2;
				while (index < messages.size()) {
					String message = (String) messages.elementAt(index);
					drawString(g, message, blackFont, 5, messagesY, Graphics.TOP | Graphics.LEFT);
					messagesY += getFontHeight(blackFont);
					index++;
				}
			}
			chatTextField.render(g, 0, h - textFieldHeight);
			//#if TouchScreen
			if (hasPointerEvents()) {
				if (!messages.isEmpty()) {
					fillRect(g, Utils.COLOR_SKY_BLUE_1, w - chatImageWidth, h - textFieldHeight, chatImageWidth, textFieldHeight);
				}
				chatTouchKeyboard.render(g, 0, 0);
			}
			//#endif
		} else if (oppWriting || !messages.isEmpty()) {
			chatHeight = getFontHeight(blackFont) + 4;
			int messagesY = h - chatHeight;
			fillRect(g, Utils.COLOR_SKY_BLUE_1, 0, messagesY, w, chatHeight);
			drawLine(g, Utils.COLOR_BLACK, 0, messagesY - 1, w, messagesY - 1);
			messagesY += 2;
			String message;
			if (oppWriting) {
				message = oppName + " pisze...";
			} else {
				int index = messages.size() - 1;
				message = (String) messages.elementAt(index);
			}
			drawString(g, message, blackFont, 5, messagesY, Graphics.TOP | Graphics.LEFT);
		}
		//#if TouchScreen
		if (hasPointerEvents() && state >= STATE_GAME_OPTIONS_MENU) {
			drawImage(g, chatImage, w, h, Graphics.BOTTOM | Graphics.RIGHT);
		}
		//#endif
	}

	private void renderMenu(Graphics g, String[] items, int selectedIndex) {
		renderMenu(g, items, null, selectedIndex);
	}

	private void renderMenu(Graphics g, String[] items, String[] values, int selectedIndex) {
		int width = getItemsWidth(items, values);
		int itemWithoutMarginWidth = width - 2 * MENU_ITEM_MARGIN;
		int fontHeight = getFontHeight(blackFont);
		int itemsCount = items.length;
		for (int i = 0; i < items.length; i++) {
			if (items[i] == null) {
				itemsCount--;
			}
		}
		int height = 2 * MENU_MARGIN + (2 * MENU_ITEM_MARGIN + MENU_ITEMS_SPACING + fontHeight) * itemsCount - MENU_ITEMS_SPACING;
		int x = (getWidth() - (width + 2 * MENU_MARGIN)) >> 1;
		int y = (getHeight() - height) >> 1;
		fillRect(g, Utils.COLOR_BUTTER_1, x, y, width + 2 * MENU_MARGIN, height);
		drawRect(g, Utils.COLOR_BLACK, x, y, width + 2 * MENU_MARGIN, height);
		x += MENU_MARGIN + MENU_ITEM_MARGIN;
		y += MENU_MARGIN + MENU_ITEM_MARGIN;
		for (int i = 0; i < items.length; i++) {
			if (i == selectedIndex) {
				if (items == optionsMenuItems && myNameTextField.isActive()) {
					myNameTextField.render(g, x - MENU_ITEM_MARGIN, y - MENU_ITEM_MARGIN);
					//#if TouchScreen
					if (hasPointerEvents()) {
						myNameTouchKeyboard.render(g, 0, 0);
					}
					//#endif
					y += fontHeight + 2 * MENU_ITEM_MARGIN + MENU_ITEMS_SPACING;
					continue;
				}
				fillRect(g, Utils.COLOR_SKY_BLUE_1, x - MENU_ITEM_MARGIN, y - MENU_ITEM_MARGIN, width, fontHeight + 2 * MENU_ITEM_MARGIN);
				drawRect(g, Utils.COLOR_SKY_BLUE_3, x - MENU_ITEM_MARGIN, y - MENU_ITEM_MARGIN, width, fontHeight + 2 * MENU_ITEM_MARGIN);
			}
			if (values != null && i < values.length && values[i] != null) {
				drawString(g, items[i], blackFont, x, y, Graphics.TOP | Graphics.LEFT);
				drawString(g, values[i], blackFont, x + itemWithoutMarginWidth, y, Graphics.TOP | Graphics.RIGHT);
			} else if (items[i] != null) {
				drawString(g, items[i], blackFont, x + itemWithoutMarginWidth / 2, y, Graphics.TOP | Graphics.HCENTER);
			} else {
				int yy = y - 1 - MENU_ITEM_MARGIN - MENU_ITEMS_SPACING / 2;
				drawLine(g, Utils.COLOR_BLACK, x + itemWithoutMarginWidth / 5, yy, x + itemWithoutMarginWidth - itemWithoutMarginWidth / 5, yy);
				continue;
			}
			y += fontHeight + 2 * MENU_ITEM_MARGIN + MENU_ITEMS_SPACING;
		}
	}

	private int getItemsWidth(String[] items, String[] values) {
		int maxStrWidth = 0;
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				int strWidth = getStringWidth(items[i], blackFont);
				if (values != null && i < values.length && values[i] != null) {
					strWidth += MENU_VALUES_SPACING + getStringWidth(values[i], blackFont);
				}
				if (maxStrWidth < strWidth) {
					maxStrWidth = strWidth;
				}
			}
		}
		return maxStrWidth + 2 * MENU_ITEM_MARGIN;
	}

	private void renderField(Graphics g, int x, int y, int size) {
		fillRect(g, Utils.COLOR_PLUM_2, x, y, sizeX * size, sizeY * size);
		for (int i = 0; i <= sizeX; i++) {
			drawLine(g, Utils.COLOR_ALUMINIUM_6, x + i * size, y, x + i * size, y + sizeY * size);
		}
		for (int i = 0; i <= sizeY; i++) {
			drawLine(g, Utils.COLOR_ALUMINIUM_6, x, y + i * size, x + sizeX * size, y + i * size);
		}
	}

	private void renderShips(Graphics g, int x, int y, int size, byte[][] ships) {
		for (int i = 0; i < sizeY; i++) {
			for (int j = 0; j < sizeX; j++) {
				byte ship = ships[i][j];
				if (ship != SHIP_UNKNOWN) {
					int color = Utils.COLOR_CHAMELEON_3;
					if (ship == SHIP_NO) {
						color = Utils.COLOR_PLUM_1;
					} else if (ship < SHIP_UNKNOWN) {
						color = Utils.COLOR_SCARLET_RED_1;
					}
					fillRect(g, color, x + j * size, y + i * size, size - 1, size - 1);
				}
			}
		}
	}

	private void renderNextShip(Graphics g, int x, int y, int size) {
		int color = Utils.COLOR_CHAMELEON_2;
		int shipSize = getNextShipSize();
		int shipTopX = myCursorX;
		int shipTopY = myCursorY;
		int shipBottomX = myCursorX;
		int shipBottomY = myCursorY;
		if (nextShipDirection == SD_RIGHT) {
			shipBottomX += shipSize - 1;
		} else if (nextShipDirection == SD_DOWN) {
			shipBottomY += shipSize - 1;
		}
		outer:
		for (int i = shipTopY - 1; i <= shipBottomY + 1; i++) {
			if (i < 0 || i >= sizeY) {
				continue;
			}
			for (int j = shipTopX - 1; j <= shipBottomX + 1; j++) {
				if (j < 0 || j >= sizeX) {
					continue;
				}
				if (myShips[i][j] != SHIP_UNKNOWN) {
					color = Utils.COLOR_SCARLET_RED_1;
					break outer;
				}
			}
		}
		for (int i = shipTopY; i <= shipBottomY; i++) {
			for (int j = shipTopX; j <= shipBottomX; j++) {
				fillRect(g, color, x + j * size, y + i * size, size - 1, size - 1);
			}
		}
	}

	private void renderCursor(Graphics g, int x, int y, int size, int cursorX, int cursorY) {
		x += cursorX * size;
		y += cursorY * size;
		size--;
		x += size / 2;
		y += size / 2;
		int targetImage;
		if (size < 8) {
			targetImage = target1Image;
		} else if (size < 24) {
			targetImage = target2Image;
		} else {
			targetImage = target3Image;
		}
		drawImage(g, targetImage, x, y, Graphics.HCENTER | Graphics.VCENTER);
	}

//	private void renderCursor(Graphics g, int x, int y, int size, int cursorX, int cursorY, int color1, int color2) {
//		x += cursorX * size;
//		y += cursorY * size;
//		size--;
//		int percent = stateTime % (CURSOR_COLOR_TRANSITION_TIME << 1);
//		if (percent > CURSOR_COLOR_TRANSITION_TIME) {
//			percent = (CURSOR_COLOR_TRANSITION_TIME << 1) - percent;
//		}
//		int color = Utils.getColor(color1, color2, percent, CURSOR_COLOR_TRANSITION_TIME);
//		while (size > 0) {
//			drawRect(g, color, x, y, size, size);
//			x += 3;
//			y += 3;
//			size -= 6;
//		}
//	}

//	private void renderCursor(Graphics g, int x, int y, int size, int cursorX, int cursorY, int color) {
//		x += cursorX * size;
//		y += cursorY * size;
//		size--;
//		while (size > 0) {
//			drawRect(g, color, x, y, size, size);
//			x += 3;
//			y += 3;
//			size -= 6;
//		}
//	}

	//#mdebug
	private boolean renderExceptions;
	private int renderExceptionsX, renderExceptionsY;
	//#if TouchScreen
	private int lastDraggedX, lastDraggedY;
	//#endif

	private void debug(String str, Throwable ex) {
		Application.debug(str, ex);
	}
	//#enddebug

	protected void sizeUpdate(int w, int h) {
		//#if TouchScreen
		if (hasPointerEvents()) {
			if (chatTouchKeyboard != null) {
				chatTouchKeyboard.setDimensions(w, TouchKeyboard.SIZE_CURRENT);
			}
			if (myNameTouchKeyboard != null) {
				myNameTouchKeyboard.setDimensions(w, TouchKeyboard.SIZE_CURRENT);
			}
		}
		//#endif
		if (chatTextField != null) {
			//#if TouchScreen
			if (hasPointerEvents()) {
				w -= chatImageWidth;
			}
			//#endif
			chatTextField.setDimensions(w, TextField.SIZE_CURRENT);
		}
	}
}
