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
package pl.mg6;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

/**
 *
 * @author maciej
 */
public abstract class BaseCanvas extends GameCanvas implements Runnable, PlayerListener {

	private Thread mainThread;
	private Graphics graphics;

	private boolean rotateDisplay = false;
	private Image bufferImage;
	private Graphics bufferGraphics;

	protected BaseCanvas() {
		super(false);
		setFullScreenMode(true);
	}

	public void run() {
		long lastTime;
		long currentTime = System.currentTimeMillis();
		int dt = 0;
		//#debug
		loadFpsFont();
		while (mainThread == Thread.currentThread()) {
			updateKeys(dt);
			//#if TouchScreen
			updatePointer(dt);
			//#endif
			checkSizeChanged();
			update(dt);
			draw();
			Utils.sleep(10);
			lastTime = currentTime;
			currentTime = System.currentTimeMillis();
			dt = (int) (currentTime - lastTime);
			flashBacklight(dt);
			//#debug
			updateFps(dt);
		}
	}

	void startMainThread() {
		mainThread = new Thread(this);
		mainThread.setPriority(Thread.MIN_PRIORITY);
		mainThread.start();
	}

	void stopMainThread() {
		mainThread = null;
	}

	private void draw() {
		if (graphics == null) {
			graphics = getGraphics();
		}
		if (rotateDisplay) {
			if (bufferImage == null
					|| bufferImage.getWidth() < getWidth()
					|| bufferImage.getHeight() < getHeight()) {
				int bufferWidth = getWidth();
				int bufferHeight = getHeight();
				if (bufferImage != null) {
					if (bufferWidth < bufferImage.getWidth()) {
						bufferWidth = bufferImage.getWidth();
					}
					if (bufferHeight < bufferImage.getHeight()) {
						bufferHeight = bufferImage.getHeight();
					}
				}
				bufferImage = null;
				bufferImage = Image.createImage(bufferWidth, bufferHeight);
				bufferGraphics = bufferImage.getGraphics();
			}
			render(bufferGraphics);
			//#debug
			renderFps(bufferGraphics);
			graphics.drawRegion(bufferImage, 0, 0, getWidth(), getHeight(), Sprite.TRANS_ROT90, 0, 0, Graphics.TOP | Graphics.LEFT);
		} else {
			render(graphics);
			//#debug
			renderFps(graphics);
		}
		flushGraphics();
	}

	protected abstract void update(int dt);

	protected abstract void render(Graphics g);

	protected abstract void sizeUpdate(int w, int h);

	// size changed

	private int width;
	private int height;

	private void checkSizeChanged() {
		int w = super.getWidth();
		int h = super.getHeight();
		if (width != w || height != h) {
			width = w;
			height = h;
			if (graphics != null) {
				graphics.setClip(0, 0, w, h);
			}
			sizeUpdate(w, h);
		}
	}

	public int getWidth() {
		return rotateDisplay ? height : width;
	}

	public int getHeight() {
		return rotateDisplay ? width : height;
	}

	// menu

	private String[] menuTitles;
	private String[][] menuItems;
	private int[][] menuNextIndexes;
	private boolean[][] menuIsVisibleItems;
	private boolean[][] menuIsSelectableItems;

	private int currentMenuIndex = -1;
	private int selectedItemIndex;

	public void setMenuCount(int count) {
		menuTitles = new String[count];
		menuItems = new String[count][];
		menuNextIndexes = new int[count][];
		menuIsVisibleItems = new boolean[count][];
		menuIsSelectableItems = new boolean[count][];
	}

	public void setMenu(int index, String title, String[] items, int[] nextIndexes) {
		boolean[] isVisible = new boolean[items.length];
		boolean[] isSelectable = new boolean[items.length];
		for (int i = 0; i < items.length; i++) {
			isVisible[i] = true;
			isSelectable[i] = true;
		}
		setMenu(index, title, items, nextIndexes, isVisible, isSelectable);
	}

	public void setMenu(int index, String title, String[] items, int[] nextIndexes, boolean[] isVisible, boolean[] isSelectable) {
		if (items.length != nextIndexes.length
				|| items.length != isVisible.length
				|| items.length != isSelectable.length) {
			throw new IllegalArgumentException("set menu " + index);
		}
		menuTitles[index] = title;
		menuItems[index] = items;
		menuNextIndexes[index] = nextIndexes;
		menuIsVisibleItems[index] = isVisible;
		menuIsSelectableItems[index] = isSelectable;
	}

//	private void updateCurrentMenu() {
//		if (currentMenuIndex != -1) {
//			for (int i = 0; i < keysPressedCount; i++) {
//				int gameAction = getGameAction(keysPressed[i]);
//				if (gameAction == FIRE) {
//					if (selectedItemIndex != -1) {
//						int next = menuNextIndexes[currentMenuIndex][selectedItemIndex];
//						if (next != -1) {
//							setCurrentMenu(next);
//						}
//					}
//				} else if (gameAction == UP) {
//					selectPreviousMenuItem();
//				} else if (gameAction == DOWN) {
//					selectNextMenuItem();
//				}
//			}
//		}
//	}

	public int getCurrentMenu() {
		return currentMenuIndex;
	}

	public void setCurrentMenu(int index) {
		currentMenuIndex = index;
		selectedItemIndex = menuItems[currentMenuIndex].length - 1;
		while (selectedItemIndex >= 0 && !isVisibleAndSelectableMenuItem(selectedItemIndex)) {
			selectedItemIndex--;
		}
	}

	public int getSelectedItem() {
		return selectedItemIndex;
	}

	public String getSelectedItemStr() {
		return menuItems[currentMenuIndex][selectedItemIndex];
	}

	public void selectPreviousMenuItem() {
		if (selectedItemIndex != -1) {
			do {
				selectedItemIndex--;
				if (selectedItemIndex == -1) {
					selectedItemIndex = menuItems[currentMenuIndex].length - 1;
				}
			} while (!isVisibleAndSelectableMenuItem(selectedItemIndex));
		}
	}

	public void selectNextMenuItem() {
		if (selectedItemIndex != -1) {
			do {
				selectedItemIndex++;
				if (selectedItemIndex == menuItems[currentMenuIndex].length) {
					selectedItemIndex = 0;
				}
			} while (!isVisibleAndSelectableMenuItem(selectedItemIndex));
		}
	}

	private boolean isVisibleAndSelectableMenuItem(int itemIndex) {
		return menuIsVisibleItems[currentMenuIndex][itemIndex]
				&& menuIsSelectableItems[currentMenuIndex][itemIndex];
	}

	// backlight

	private static final boolean FLASH_BACKLIGHT = false;
	private static final int FLASH_TIME = 10000;

	private int lastBacklight = 0;

	private void flashBacklight(int dt) {
		if (FLASH_BACKLIGHT) {
			lastBacklight += dt;
			if (lastBacklight >= FLASH_TIME) {
				Application.display.flashBacklight(1);
				lastBacklight = 0;
			}
		}
	}

	//#mdebug
	// fps

	private static final boolean RENDER_FPS = true;
	private String fps = "fps:N/A";
	private int fpsFrames = 0;
	private int fpsTime = 0;
	private int fpsFont = -1;

	private void loadFpsFont() {
		if (RENDER_FPS) {
			fpsFont = loadSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL, Utils.COLOR_PLUM_1);
		}
	}

	private void updateFps(int dt) {
		if (RENDER_FPS) {
			fpsFrames++;
			fpsTime += dt;
			if (fpsTime >= Utils.TIME_SECOND) {
				fps = "fps:" + fpsFrames;
				fpsFrames = 0;
				fpsTime -= Utils.TIME_SECOND;
			}
		}
	}

	private void renderFps(Graphics g) {
		if (RENDER_FPS) {
			drawString(g, fps, fpsFont, 0, 0, Graphics.TOP | Graphics.LEFT);
		}
	}
	//#enddebug

	// keys

	public static final int KEY_UP = -1;
	public static final int KEY_DOWN = -2;
	public static final int KEY_LEFT = -3;
	public static final int KEY_RIGHT = -4;
	public static final int KEY_SELECT = -5;
	public static final int KEY_LEFT_SOFT = -6;
	public static final int KEY_RIGHT_SOFT = -7;
	public static final int KEY_DELETE = -8;
	
//	public static final int KEY_CALL = -10;
//	public static final int KEY_HANGUP = -11;
//	public static final int KEY_MENU = -12;

	public static final int KEY_BACKSPACE = '\b';
	public static final int KEY_ENTER = '\n';

	private static final int INITIAL_KEYS_CACHE_SIZE = 2;
	private int[] internalKeysPressed = new int[INITIAL_KEYS_CACHE_SIZE];
	private int[] internalKeysRepeated = new int[INITIAL_KEYS_CACHE_SIZE];
	private int[] internalKeysPressedTime = new int[INITIAL_KEYS_CACHE_SIZE];
	private int internalKeysPressedCount = 0;
	private int[] internalKeysReleased = new int[INITIAL_KEYS_CACHE_SIZE];
	private int internalKeysReleasedCount = 0;
	private static final int KEY_REPEATED_TIME = 1000;
//	private static final int KEY_REPEATED_TIME_INITIAL = 500;
//	private static final int KEY_REPEATED_TIME_FEW = 300;
//	private static final int KEY_REPEATED_TIME_MANY = 100;
	private final Object keysMonitor = new Object();

	protected int[] keysPressed = new int[INITIAL_KEYS_CACHE_SIZE];
	protected int keysPressedCount;

	protected void keyPressed(int keyCode) {
		synchronized (keysMonitor) {
			//#debug
			debug("keyPressed " + keyCode, null);
			if (internalKeysPressed.length == internalKeysPressedCount) {
				int[] tmp = new int[internalKeysPressedCount + 1];
				System.arraycopy(internalKeysPressed, 0, tmp, 0, internalKeysPressedCount);
				internalKeysPressed = tmp;
				tmp = new int[internalKeysPressedCount + 1];
				System.arraycopy(internalKeysRepeated, 0, tmp, 0, internalKeysPressedCount);
				internalKeysRepeated = tmp;
				tmp = new int[internalKeysPressedCount + 1];
				System.arraycopy(internalKeysPressedTime, 0, tmp, 0, internalKeysPressedCount);
				internalKeysPressedTime = tmp;
			}
			internalKeysPressed[internalKeysPressedCount] = keyCode;
			internalKeysRepeated[internalKeysPressedCount] = 0;
			internalKeysPressedTime[internalKeysPressedCount] = 0;
			internalKeysPressedCount++;
		}
	}

	protected void keyReleased(int keyCode) {
		synchronized (keysMonitor) {
			if (internalKeysReleased.length == internalKeysReleasedCount) {
				int[] tmp = new int[internalKeysReleasedCount + 1];
				System.arraycopy(internalKeysReleased, 0, tmp, 0, internalKeysReleasedCount);
				internalKeysReleased = tmp;
			}
			internalKeysReleased[internalKeysReleasedCount] = keyCode;
			internalKeysReleasedCount++;
		}
	}

	protected void hideNotify() {
		synchronized (keysMonitor) {
			internalKeysPressedCount = 0;
			internalKeysReleasedCount = 0;
		}
	}

	private void updateKeys(int dt) {
		synchronized (keysMonitor) {
			keysPressedCount = 0;
			for (int i = 0; i < internalKeysPressedCount; i++) {
				internalKeysPressedTime[i] += dt;
				if (internalKeysRepeated[i] == 0
						|| internalKeysPressedTime[i] >= KEY_REPEATED_TIME / internalKeysRepeated[i]) {
//						|| internalKeysRepeated[i] == 1 && internalKeysPressedTime[i] >= KEY_REPEATED_TIME_INITIAL
//						|| internalKeysRepeated[i] >= 2 && internalKeysRepeated[i] <= 4 && internalKeysPressedTime[i] >= KEY_REPEATED_TIME_FEW
//						|| internalKeysRepeated[i] >= 5 && internalKeysPressedTime[i] >= KEY_REPEATED_TIME_MANY) {
				//if (internalKeysPressedTime[i] >= KEY_REPEATED_TIME) {
					keysPressedCount++;
				}
			}
			if (keysPressed.length < keysPressedCount) {
				keysPressed = new int[keysPressedCount];
			}
			keysPressedCount = 0;
			boolean pressed = false;
			for (int i = 0; i < internalKeysPressedCount; i++) {
				if (internalKeysRepeated[i] == 0) {
					pressed = true;
				} else if (internalKeysPressedTime[i] >= KEY_REPEATED_TIME / internalKeysRepeated[i]) {
					pressed = true;
					internalKeysPressedTime[i] -= KEY_REPEATED_TIME / internalKeysRepeated[i];
				}
//				} else if (internalKeysRepeated[i] >= 1 && internalKeysPressedTime[i] >= KEY_REPEATED_TIME_INITIAL) {
//					pressed = true;
//					internalKeysPressedTime[i] -= KEY_REPEATED_TIME_INITIAL;
//				} else if (internalKeysRepeated[i] >= 2 && internalKeysPressedTime[i] >= KEY_REPEATED_TIME_FEW) {
//					pressed = true;
//					internalKeysPressedTime[i] -= KEY_REPEATED_TIME_FEW;
//				} else if (internalKeysRepeated[i] >= 5 && internalKeysPressedTime[i] >= KEY_REPEATED_TIME_MANY) {
//					pressed = true;
//					internalKeysPressedTime[i] -= KEY_REPEATED_TIME_MANY;
//				}
				if (pressed) {
					internalKeysRepeated[i]++;
					keysPressed[keysPressedCount] = internalKeysPressed[i];
					keysPressedCount++;
					pressed = false;
				}
			}
			if (internalKeysReleasedCount > 0) {
				for (int i = internalKeysPressedCount - 1; i >= 0; i--) {
					for (int j = internalKeysReleasedCount - 1; j >= 0; j--) {
						if (internalKeysPressed[i] == internalKeysReleased[j]) {
							for (int k = i; k < internalKeysPressedCount - 1; k++) {
								internalKeysPressed[k] = internalKeysPressed[k + 1];
								internalKeysPressedTime[k] = internalKeysPressedTime[k + 1];
							}
							internalKeysPressedCount--;
							for (int k = j; k < internalKeysReleasedCount - 1; k++) {
								internalKeysReleased[k] = internalKeysReleased[k + 1];
							}
							internalKeysReleasedCount--;
							break;
						}
					}
				}
				internalKeysReleasedCount = 0;
			}
		}
	}

	//#if TouchScreen
	// pointer

	private int internalPointerStartX, internalPointerStartY;
	private int internalPointerEndX, internalPointerEndY;
	private int internalPointerState = POINTER_STATE_NONE;
	private int internalPointerNextState;
//	private boolean internalPointerDragging;
//	private int internalPointerDraggingTime;

	protected int pointerStartX, pointerStartY;
	protected int pointerEndX, pointerEndY;
	protected int pointerState;
//	protected boolean pointerDragging;
//	protected int pointerDraggingTime;

	public static final int POINTER_STATE_NONE = -1;
	public static final int POINTER_STATE_PRESSED = 1;
	public static final int POINTER_STATE_DRAGGED = 2;
	public static final int POINTER_STATE_RELEASED = 3;

	private final Object pointerMonitor = new Object();

	protected void pointerPressed(int x, int y) {
		synchronized (pointerMonitor) {
			internalPointerStartX = x;
			internalPointerStartY = y;
			internalPointerEndX = x;
			internalPointerEndY = y;
			internalPointerState = POINTER_STATE_PRESSED;
			internalPointerNextState = POINTER_STATE_DRAGGED;
//			internalPointerDragging = false;
//			internalPointerDraggingTime = 0;
		}
	}

	protected void pointerDragged(int x, int y) {
		synchronized (pointerMonitor) {
			internalPointerEndX = x;
			internalPointerEndY = y;
		}
	}

	protected void pointerReleased(int x, int y) {
		synchronized (pointerMonitor) {
			internalPointerEndX = x;
			internalPointerEndY = y;
			if (internalPointerState == POINTER_STATE_PRESSED) {
				internalPointerNextState = POINTER_STATE_RELEASED;
			} else {
				internalPointerState = POINTER_STATE_RELEASED;
			}
		}
	}

	private void updatePointer(int dt) {
		synchronized (pointerMonitor) {
			pointerState = internalPointerState;
			if (internalPointerState != POINTER_STATE_NONE) {
				pointerStartX = internalPointerStartX;
				pointerStartY = internalPointerStartY;
				pointerEndX = internalPointerEndX;
				pointerEndY = internalPointerEndY;
				if (internalPointerState == POINTER_STATE_PRESSED) {
					internalPointerState = internalPointerNextState;
				} else if (internalPointerState == POINTER_STATE_RELEASED) {
					internalPointerState = POINTER_STATE_NONE;
				}
//				internalPointerDraggingTime += dt;
//				if (!internalPointerDragging
//						&& (internalPointerDraggingTime > 500
//						|| Math.abs(internalPointerEndX - internalPointerStartX) > 5
//						|| Math.abs(internalPointerEndY - internalPointerStartY) > 5)) {
//					internalPointerDragging = true;
//				}
//				pointerDragging = internalPointerDragging;
//				pointerDraggingTime = internalPointerDraggingTime;
			}
		}
	}
	//#endif

	// simple

	public void drawLine(Graphics g, int color, int x1, int y1, int x2, int y2) {
		g.setColor(color);
		g.drawLine(x1, y1, x2, y2);
	}

	public void drawRect(Graphics g, int color, int x, int y, int width, int height) {
		g.setColor(color);
		g.drawRect(x, y, width - 1, height - 1);
	}

	public void fillRect(Graphics g, int color, int x, int y, int width, int height) {
		g.setColor(color);
		g.fillRect(x, y, width, height);
	}

	public void fillRegion(Graphics g, int color, int[] points) {
		g.setColor(color);
		int h, i, j, k;
		int[] hits = new int[20];
		int hitCount;
		int minY = Integer.MAX_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (i = 1; i < points.length; i += 2) {
			if (minY > points[i]) {
				minY = points[i];
			}
			if (maxY < points[i]) {
				maxY = points[i];
			}
		}
		for (i = minY; i <= maxY; i++) {
			hitCount = 0;
			k = points.length - 1;
			for (j = 1; j < points.length; j += 2) {
				if ((points[j] < i && i <= points[k] || points[k] < i && i <= points[j]) && points[j] != points[k]) {
					if (hitCount == hits.length) {
						int[] tmp = new int[hitCount + 20];
						System.arraycopy(hits, 0, tmp, 0, hitCount);
						hits = tmp;
					}
					hits[hitCount] = points[j - 1] + ((((i - points[j]) * (points[k - 1] - points[j - 1]) << 1) / (points[k] - points[j]) + 1) >> 1);
					hitCount++;
				}
				k = j;
			}
			for (j = 1; j < hitCount; j++) {
				k = j;
				h = hits[k];
				while (k > 0 && hits[k - 1] > h) {
					hits[k] = hits[k - 1];
					k--;
				}
				hits[k] = h;
			}
			for (j = 0; j < hitCount; j += 2) {
				g.drawLine(hits[j], i, hits[j + 1], i);
			}
		}
	}

	// font

	private static final int FONT_CACHE_SIZE = 10;
	private static final short FONT_TYPE_ASCII_MONOSPACE = 0;
	private static final short FONT_TYPE_UNICODE_MONOSPACE = 1;
	private Font[] systemFonts = new Font[FONT_CACHE_SIZE];
	private int[] systemFontColors = new int[FONT_CACHE_SIZE];
	private short[][] bitmapFonts = new short[FONT_CACHE_SIZE][];
	private int[] bitmapFontImageIds = new int[FONT_CACHE_SIZE];
	private String[] fontNames = new String[FONT_CACHE_SIZE];
	private String[] fontImageNames = new String[FONT_CACHE_SIZE];

	public int loadSystemFont(int face, int style, int size, int color) {
		for (int i = 0; i < FONT_CACHE_SIZE; i++) {
			if (systemFonts[i] != null
					&& systemFonts[i].getFace() == face
					&& systemFonts[i].getStyle() == style
					&& systemFonts[i].getSize() == size
					&& systemFontColors[i] == color) {
				return i;
			}
		}
		for (int i = 0; i < FONT_CACHE_SIZE; i++) {
			if (systemFonts[i] == null && bitmapFonts[i] == null) {
				systemFonts[i] = Font.getFont(face, style, size);
				systemFontColors[i] = color;
				return i;
			}
		}
		return -1;
	}

	public int loadBitmapFont(String fontName, String fontImageName) {
		for (int i = 0; i < FONT_CACHE_SIZE; i++) {
			if (bitmapFonts[i] != null && fontNames[i] != null && fontNames[i].equals(fontName) && fontImageNames[i].equals(fontImageName)) {
				return i;
			}
		}
		for (int i = 0; i < FONT_CACHE_SIZE; i++) {
			if (systemFonts[i] == null && bitmapFonts[i] == null) {
				for (int j = 0; j < FONT_CACHE_SIZE; j++) {
					if (bitmapFonts[j] != null && fontNames[j] != null && fontNames[j].equals(fontName)) {
						bitmapFonts[i] = bitmapFonts[j];
					}
				}
				try {
					if (bitmapFonts[i] == null) {
						byte[] array = loadResource(fontName);
						byte type = array[0];
						switch (type) {
							case FONT_TYPE_ASCII_MONOSPACE:
								bitmapFonts[i] = new short[array.length];
								for (int j = 0; j < bitmapFonts[i].length; j++) {
									bitmapFonts[i][j] = (short) (array[j] & 0xff);
								}
								break;
							case FONT_TYPE_UNICODE_MONOSPACE:
								bitmapFonts[i] = new short[(array.length >> 1) + 1];
								bitmapFonts[i][0] = (short) (array[0] & 0xff);
								bitmapFonts[i][1] = (short) (array[1] & 0xff);
								for (int j = 2; j < bitmapFonts[i].length; j++) {
									bitmapFonts[i][j] = (short) (((array[2 * j - 2] & 0xff) << 8) | (array[2 * j - 1] & 0xff));
								}
						}
					}
					bitmapFontImageIds[i] = loadImage(fontImageName);
					fontNames[i] = fontName;
					fontImageNames[i] = fontImageName;
					return i;
				} catch (IOException ex) {
					//#debug
					debug("BC.loadBitmapFont: " + fontName + " " + fontImageName, ex);
				}
			}
		}
		return -1;
	}

	public void drawString(Graphics g, String str, int fontId, int x, int y, int anchor) {
		if (systemFonts[fontId] != null) {
			g.setFont(systemFonts[fontId]);
			g.setColor(systemFontColors[fontId]);
			g.drawString(str, x, y, anchor);
		} else {
			if ((anchor & Graphics.RIGHT) != 0) {
				x -= getStringWidth(str, fontId);
				anchor &= ~Graphics.RIGHT;
				anchor |= Graphics.LEFT;
			} else if ((anchor & Graphics.HCENTER) != 0) {
				x -= getStringWidth(str, fontId) >> 1;
				anchor &= ~Graphics.HCENTER;
				anchor |= Graphics.LEFT;
			}
			int type = bitmapFonts[fontId][0];
			switch (type) {
				case FONT_TYPE_ASCII_MONOSPACE:
				case FONT_TYPE_UNICODE_MONOSPACE:
					for (int i = 0; i < str.length(); i++) {
						char c = str.charAt(i);
						for (int j = 2; j < bitmapFonts[fontId].length; j++) {
							if (bitmapFonts[fontId][j] == c) {
								drawImageRegion(g, bitmapFontImageIds[fontId], (j - 2) * bitmapFonts[fontId][1], 0, bitmapFonts[fontId][1], getFontHeight(fontId), x, y, anchor);
								break;
							}
						}
						x += bitmapFonts[fontId][1];
					}
					break;
			}
		}
	}

	public void drawChar(Graphics g, char c, int fontId, int x, int y, int anchor) {
		if (systemFonts[fontId] != null) {
			g.setFont(systemFonts[fontId]);
			g.setColor(systemFontColors[fontId]);
			g.drawChar(c, x, y, anchor);
		} else {
			int type = bitmapFonts[fontId][0];
			switch (type) {
				case FONT_TYPE_ASCII_MONOSPACE:
				case FONT_TYPE_UNICODE_MONOSPACE:
					for (int i = 2; i < bitmapFonts[fontId].length; i++) {
						if (bitmapFonts[fontId][i] == c) {
							drawImageRegion(g, bitmapFontImageIds[fontId], (i - 2) * bitmapFonts[fontId][1], 0, bitmapFonts[fontId][1], getFontHeight(fontId), x, y, anchor);
							break;
						}
					}
					break;
			}
		}
	}

	public int getStringWidth(String str, int fontId) {
		if (systemFonts[fontId] != null) {
			return systemFonts[fontId].stringWidth(str);
		} else {
			int type = bitmapFonts[fontId][0];
			switch (type) {
				case FONT_TYPE_ASCII_MONOSPACE:
				case FONT_TYPE_UNICODE_MONOSPACE:
					return str.length() * bitmapFonts[fontId][1];
			}
			return 0;
		}
	}

	public int getCharWidth(char c, int fontId) {
		if (systemFonts[fontId] != null) {
			return systemFonts[fontId].charWidth(c);
		} else {
			int type = bitmapFonts[fontId][0];
			switch (type) {
				case FONT_TYPE_ASCII_MONOSPACE:
				case FONT_TYPE_UNICODE_MONOSPACE:
					return bitmapFonts[fontId][1];
			}
			return 0;
		}
	}

	public int getFontHeight(int fontId) {
		if (systemFonts[fontId] != null) {
			return systemFonts[fontId].getHeight();
		} else {
			return getImageHeight(bitmapFontImageIds[fontId]);
		}
	}

	// image

	private static final int IMAGE_CACHE_SIZE = 20;
	private Image[] images = new Image[IMAGE_CACHE_SIZE];
	private String[] imageNames = new String[IMAGE_CACHE_SIZE];

	public int loadImage(String name) {
		for (int i = 0; i < IMAGE_CACHE_SIZE; i++) {
			if (images[i] != null && imageNames[i] != null && imageNames[i].equals(name)) {
				return i;
			}
		}
		try {
			for (int i = 0; i < IMAGE_CACHE_SIZE; i++) {
				if (images[i] == null) {
					images[i] = Image.createImage(name);
					imageNames[i] = name;
					return i;
				}
			}
		} catch (IOException ex) {
			//#debug
			debug("loadImage " + name, ex);
		}
		return -1;
	}

	public void drawImage(Graphics g, int imageId, int x, int y, int anchor) {
		g.drawImage(images[imageId], x, y, anchor);
	}

	public void drawImageRegion(Graphics g, int imageId, int imageX, int imageY, int width, int height, int x, int y, int anchor) {
		g.drawRegion(images[imageId], imageX, imageY, width, height, Sprite.TRANS_NONE, x, y, anchor);
	}

	public void drawImageTile(Graphics g, int imageId, int width, int height, int borderWidth, int borderHeight, int x, int y) {
		int imageWidth = getImageWidth(imageId);
		int imageHeight = getImageHeight(imageId);
		//#mdebug
		if (borderWidth < 0 || borderHeight < 0 || imageWidth <= 2 * borderWidth || imageHeight <= 2 * borderHeight) {
			throw new IllegalArgumentException();
		}
		//#enddebug
		int clipX = g.getClipX();
		int clipY = g.getClipY();
		int clipWidth = g.getClipWidth();
		int clipHeight = g.getClipHeight();
		if (borderWidth > 0 && borderHeight > 0) {
			drawImageRegion(g, imageId, 0, 0, borderWidth, borderHeight, x, y, Graphics.TOP | Graphics.LEFT);
			drawImageRegion(g, imageId, imageWidth - borderWidth, 0, borderWidth, borderHeight, x + width, y, Graphics.TOP | Graphics.RIGHT);
			drawImageRegion(g, imageId, 0, imageHeight - borderHeight, borderWidth, borderHeight, x, y + width, Graphics.BOTTOM | Graphics.LEFT);
			drawImageRegion(g, imageId, imageWidth - borderWidth, imageHeight - borderHeight, borderWidth, borderHeight, x + width, y + width, Graphics.BOTTOM | Graphics.RIGHT);
		}
		if (borderHeight > 0) {
			g.clipRect(x + borderWidth, y, width - 2 * borderWidth, height);
			for (int xOffset = borderWidth; xOffset < width - 2 * borderWidth; xOffset += imageWidth - 2 * borderWidth) {
				drawImageRegion(g, imageId, borderWidth, 0, imageWidth - 2 * borderWidth, borderHeight, x + xOffset, y, Graphics.TOP | Graphics.LEFT);
				drawImageRegion(g, imageId, borderWidth, imageHeight - borderHeight, imageWidth - 2 * borderWidth, borderHeight, x + xOffset, y + height, Graphics.BOTTOM | Graphics.LEFT);
			}
			g.setClip(clipX, clipY, clipWidth, clipHeight);
		}
		if (borderWidth > 0) {
			g.clipRect(x, y + borderHeight, width, height - 2 * borderHeight);
			for (int yOffset = borderHeight; yOffset < height - 2 * borderHeight; yOffset += imageHeight - 2 * borderHeight) {
				drawImageRegion(g, imageId, 0, borderHeight, borderWidth, imageHeight - 2 * borderHeight, x, y + yOffset, Graphics.TOP | Graphics.LEFT);
				drawImageRegion(g, imageId, imageWidth - borderWidth, borderHeight, borderWidth, imageHeight - 2 * borderHeight, x + width, y + yOffset, Graphics.TOP | Graphics.RIGHT);
			}
		}
		g.clipRect(x + borderWidth, y + borderHeight, width - 2 * borderWidth, height - 2 * borderHeight);
		for (int yOffset = borderHeight; yOffset < height - 2 * borderHeight; yOffset += imageHeight - 2 * borderHeight) {
			for (int xOffset = borderWidth; xOffset < width - 2 * borderWidth; xOffset += imageWidth - 2 * borderWidth) {
				drawImageRegion(g, imageId, borderWidth, borderHeight, imageWidth - 2 * borderWidth, imageHeight - 2 * borderHeight, x + xOffset, y + yOffset, Graphics.TOP | Graphics.LEFT);
			}
		}
		g.setClip(clipX, clipY, clipWidth, clipHeight);
	}

	public int getImageWidth(int imageId) {
		return images[imageId].getWidth();
	}

	public int getImageHeight(int imageId) {
		return images[imageId].getHeight();
	}

	// recordstore

	public byte[] readRecordStore(String name) throws RecordStoreException {
		if (!existsRecordStore(name)) {
			return null;
		}
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(name, false);
			byte[] data = rs.getRecord(1);
			return data;
		} finally {
			if (rs != null) {
				rs.closeRecordStore();
			}
		}
	}

	public void writeRecordStore(String name, byte[] data) throws RecordStoreException {
		deleteRecordStore(name);
		RecordStore rs = null;
		try {
			rs = RecordStore.openRecordStore(name, true);
			rs.addRecord(data, 0, data.length);
		} finally {
			if (rs != null) {
				rs.closeRecordStore();
			}
		}
	}

	public void deleteRecordStore(String name) throws RecordStoreException {
		if (existsRecordStore(name)) {
			RecordStore.deleteRecordStore(name);
		}
	}

	private boolean existsRecordStore(String name) {
		String[] names = RecordStore.listRecordStores();
		if (names == null) {
			return false;
		}
		for (int i = 0; i < names.length; i++) {
			if (name.equals(names[i])) {
				return true;
			}
		}
		return false;
	}

	// sound

	private static final int SOUND_CACHE_SIZE = 10;
	private Player[] players = new Player[SOUND_CACHE_SIZE];
	private String[] soundNames = new String[SOUND_CACHE_SIZE];

	public int loadSound(String name, String type) {
		//#debug
		debug("BC.loadSound " + name + " " + type, null);
		for (int i = 0; i < SOUND_CACHE_SIZE; i++) {
			if (players[i] != null && soundNames[i] != null && soundNames[i].equals(name)) {
				return i;
			}
		}
		try {
			for (int i = 0; i < SOUND_CACHE_SIZE; i++) {
				if (players[i] == null) {
					InputStream is = getClass().getResourceAsStream(name);
					Player p = Manager.createPlayer(is, type);
					p.addPlayerListener(this);
					p.prefetch();
					players[i] = p;
					soundNames[i] = name;
					return i;
				}
			}
		} catch (Exception ex) {
			//#debug
			debug("BC.loadSound " + name + " " + type, ex);
		}
		return -1;
	}

	public void playSound(int soundId) {
		playSound(soundId, 1);
	}

	public void playSound(int soundId, int loopCount) {
		try {
			Player p = players[soundId];
			if (p.getState() == Player.STARTED) {
				//#debug
				debug("BC.playSound.stop", null);
				p.stop();
			}
			//#debug
			debug("BC.playSound.start", null);
			p.setMediaTime(0);
			p.setLoopCount(loopCount);
			p.start();
		} catch (Exception ex) {
			//#debug
			debug("BC.playSound " + soundId, ex);
		}
	}

	//#mdebug
	private static final String[] PLAYER_STATE_TEXTS = {
		"CLOSED", "UNREALIZED", "REALIZED", "PREFETCHED", "STARTED",
	};

	private static String getStateText(Player player) {
		return PLAYER_STATE_TEXTS[player.getState() / 100];
	}
	//#enddebug

	public void playerUpdate(Player player, String event, Object eventData) {
		//#debug
		debug("BC.playerUpdate " + event + " " + eventData + " " + player.getState() + " (" + getStateText(player) + ")", null);
	}

	// resource

	public byte[] loadResource(String name) throws IOException {
		InputStream is = getClass().getResourceAsStream(name);
		if (is == null) {
			//#debug
			debug("BC.loadResource " + name + ": null", null);
			return null;
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int b = is.read();
		while (b != -1) {
			baos.write(b);
			b = is.read();
		}
		is.close();
		return baos.toByteArray();
	}

	//#mdebug
	private static void debug(String str, Throwable ex) {
		Application.debug(str, ex);
	}
	//#enddebug
}
