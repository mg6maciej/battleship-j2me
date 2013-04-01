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

import javax.microedition.lcdui.Graphics;

/**
 *
 * @author maciej
 */
public final class TextField {

	private BaseCanvas parent;

	private StringBuffer buffer = new StringBuffer();
	private int caretPosition;
	private int maxLength;

	private int type;
	private String legalChars;
	
	private boolean password;

	private static final char PASSWORD_CHAR = '*';

	public static final int TYPE_DIGITS = 1 << 1;
	public static final int TYPE_LETTERS = 1 << 2;
	public static final int TYPE_ANY = 0;

	public static final int LENGTH_UNLIMITED = -1;
	
	private int primaryFontId = -1;
	private int secondaryFontId = -1;
	private int backgroundColor;
	private int borderColor;
	private int foregroundColor;
	
	private int width;
	private int height;

	public static final int SIZE_PREFERRED = -1;
	public static final int SIZE_CURRENT = -2;

	private int maxInputMethodWidth;

	private int viewPosition;

	private int state = STATE_INACTIVE;
	private int stateTime = 0;

	private static final int STATE_INACTIVE = 0;
	private static final int STATE_ACTIVE = 1;
	private static final int STATE_MULTITAP = 2;

	private static final int CARET_VISIBLE_TIME = 500;
	private static final int CARET_INVISIBLE_TIME = 200;
	private static final int MULTITAP_TIMEOUT = 1500;

	private int imIndex;

	private static final String[] INPUT_METHODS = { "abc", "ABC", "123" };
	private static final int IM_LOWERCASE = 0;
	private static final int IM_UPPERCASE = 1;
	private static final int IM_DIGITS = 2;

	private int mtKeyIndex;
	private int mtValueIndex;

	//#if QwertyKeyboard
	//#if NokiaE72
//# 	private static final int[] MULTITAP_KEYS = {
//# 		'r', 't', 'y', 'u', 'i', 'o',
//# 		'f', 'g', 'h', 'j', 'k',
//# 		'v', 'b', 'n', 'm', ',', '.',
//# 		'@', '(', ')', '&',
//# 	};
//# 	private static final String[] MULTITAP_VALUES = {
//# 		"R1", "T2", "Y3", "U*", "I+", "O=",
//# 		"F4", "G5", "H6", "J#", "K-",
//# 		"V7", "B8", "N9", "M0", ",;", ".:",
//# 		"@/", "?(", "!)", "'&"
//# 	};
//# 	private static final int[] MULTITAP_DIGITS_KEYS = {
//# 		'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
//# 		'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
//# 		'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.',
//# 		'@', '(', ')', '&',
//# 	};
//# 	private static final String[] MULTITAP_DIGITS_VALUES = {
//# 		"", "", "", "1", "2", "3", "*", "+", "=", "",
//# 		"", "", "", "4", "5", "6", "#", "-", "",
//# 		"", "", "", "7", "8", "9", "0", ",;", ".:",
//# 		"@/", "?(", "!)", "'&"
//# 	};
	//#else
//# 	private static final int[] MULTITAP_KEYS = { };
//# 	private static final String[] MULTITAP_VALUES = { };
//# 	private static final int[] MULTITAP_DIGITS_KEYS = { };
//# 	private static final String[] MULTITAP_DIGITS_VALUES = { };
	//#endif
	//#else
	private static final int[] MULTITAP_KEYS = {
		BaseCanvas.KEY_NUM0, BaseCanvas.KEY_NUM1,
		BaseCanvas.KEY_NUM2, BaseCanvas.KEY_NUM3,
		BaseCanvas.KEY_NUM4, BaseCanvas.KEY_NUM5,
		BaseCanvas.KEY_NUM6, BaseCanvas.KEY_NUM7,
		BaseCanvas.KEY_NUM8, BaseCanvas.KEY_NUM9,
	};
	private static final String[] MULTITAP_VALUES = {
		" 0", ".,?!'\"1-()@/:_", "ABC2", "DEF3", "GHI4",
		"JKL5", "MNO6", "PQRS7", "TUV8", "WXYZ9",
	};
	private static final int[] MULTITAP_DIGITS_KEYS = { };
	private static final String[] MULTITAP_DIGITS_VALUES = { };
	//#endif
	private String[] multitapValues;
	private String[] multitapDigitsValues;

	static {
		sortMultitapMap(MULTITAP_KEYS, MULTITAP_VALUES);
		sortMultitapMap(MULTITAP_DIGITS_KEYS, MULTITAP_DIGITS_VALUES);
	}

	private static void sortMultitapMap(int[] keys, String[] values) {
		int j;
		for (int i = 1; i < keys.length; i++) {
			j = i;

			int key = keys[j];
			String value = values[j];

			while (j > 0 && keys[j - 1] > key) {
				keys[j] = keys[j - 1];
				values[j] = values[j - 1];
				j--;
			}
			keys[j] = key;
			values[j] = value;
		}
	}

	private static int findMultitapKeyIndex(int[] keys, int key) {
		int min = 0;
		int max = keys.length;
		int avg;
		while (min < max) {
			avg = (min + max) >> 1;
			if (key == keys[avg]) {
				return avg;
			} else if (key > keys[avg]) {
				min = avg + 1;
			} else {
				max = avg;
			}
		}
		return -1;
	}

	public TextField(BaseCanvas parent) {
		this(parent, "", LENGTH_UNLIMITED, TYPE_ANY, null, false);
	}

	public TextField(BaseCanvas parent, String text, int maxLength, int type, String legalChars, boolean password) {
		this.parent = parent;
		setTypeAndLegalChars(type, legalChars);
		this.maxLength = maxLength;
		setText(text);
		this.imIndex = (type == TYPE_DIGITS ? IM_DIGITS : IM_LOWERCASE);
		this.password = password;
	}

	public void setFontsAndColors(int primaryFont, int secondaryFont, int background, int border, int foreground) {
		primaryFontId = primaryFont;
		secondaryFontId = secondaryFont;
		backgroundColor = background;
		borderColor = border;
		foregroundColor = foreground;

		maxInputMethodWidth = 0;
		if (type == TYPE_ANY || (type & TYPE_LETTERS) != 0) {
			maxInputMethodWidth = parent.getStringWidth(INPUT_METHODS[IM_LOWERCASE], secondaryFontId);
			int imWidth = parent.getStringWidth(INPUT_METHODS[IM_UPPERCASE], secondaryFontId);
			if (maxInputMethodWidth < imWidth) {
				maxInputMethodWidth = imWidth;
			}
		}
		if (type == TYPE_ANY || (type & TYPE_DIGITS) != 0) {
			int imWidth = parent.getStringWidth(INPUT_METHODS[IM_DIGITS], secondaryFontId);
			if (maxInputMethodWidth < imWidth) {
				maxInputMethodWidth = imWidth;
			}
		}
		maxInputMethodWidth += 4;
	}

	public void setDimensions(int w, int h) {
		if (w == SIZE_PREFERRED) {
			width = getPreferredWidth();
		} else if (w != SIZE_CURRENT) {
			width = w;
		}
		if (h == SIZE_PREFERRED) {
			height = getPreferredHeight();
		} else if (h != SIZE_CURRENT) {
			height = h;
		}
		updateViewPosition();
	}

	private void setTypeAndLegalChars(int type, String legalChars) {
		this.type = type;
		this.legalChars = legalChars;
		if (type != TYPE_ANY) {
			multitapValues = new String[MULTITAP_VALUES.length];
			multitapDigitsValues = new String[MULTITAP_DIGITS_VALUES.length];
			copyLegalChars(MULTITAP_VALUES, multitapValues);
			copyLegalChars(MULTITAP_DIGITS_VALUES, multitapDigitsValues);
		} else {
			multitapValues = MULTITAP_VALUES;
			multitapDigitsValues = MULTITAP_DIGITS_VALUES;
		}
	}

	boolean isLegalChar(char c) {
		return type == TYPE_ANY || (legalChars != null && legalChars.indexOf(c) != -1)
				|| ((type & TYPE_LETTERS) != 0 && (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')))
				|| ((type & TYPE_DIGITS) != 0 && ('0' <= c && c <= '9'));
	}

	private void copyLegalChars(String[] from, String[] to) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < from.length; i++) {
			sb.setLength(0);
			sb.append(from[i]);
			for (int j = sb.length() - 1; j >= 0; j--) {
				char c = sb.charAt(j);
				if (!isLegalChar(c)) {
					sb.deleteCharAt(j);
				}
			}
			to[i] = sb.toString();
		}
	}

	private void commit() {
		setState(STATE_ACTIVE);
		char c = getCurrentMultitapChar();
		insertChar(c);
	}

	private char getCurrentMultitapChar() {
		String[] values;
		if (imIndex != IM_DIGITS) {
			values = multitapValues;
		} else {
			values = multitapDigitsValues;
		}
		char c = values[mtKeyIndex].charAt(mtValueIndex);
		if (imIndex == IM_LOWERCASE) {
			c = Character.toLowerCase(c);
		}
		return c;
	}

	private void insertChar(char c) {
		buffer.insert(caretPosition, c);
		caretPosition++;
		updateViewPosition();
	}

	private void updateViewPosition() {
		if (primaryFontId == -1) {
			return;
		}
		int textWidth;
		int caretViewPosition = -1;
		if (state == STATE_MULTITAP) {
			textWidth = parent.getStringWidth(getTextBeforeCaret(), primaryFontId);
			char c = getCurrentMultitapChar();
			textWidth += parent.getCharWidth(c, secondaryFontId);
			caretViewPosition = textWidth;
			textWidth += parent.getStringWidth(getTextAfterCaret(), primaryFontId);
		} else {
			textWidth = parent.getStringWidth(buffer.toString(), primaryFontId);
		}
		int maxTextWidth = width - maxInputMethodWidth - 4;
		if (textWidth <= maxTextWidth) {
			viewPosition = 0;
		} else if (viewPosition + maxTextWidth > textWidth) {
			viewPosition = textWidth - maxTextWidth;
		} else {
			if (caretViewPosition == -1) {
				caretViewPosition = parent.getStringWidth(getTextBeforeCaret(), primaryFontId);
			}
			if (viewPosition > caretViewPosition) {
				viewPosition = caretViewPosition - maxTextWidth / 3;
				if (viewPosition < 0) {
					viewPosition = 0;
				}
			} else if (viewPosition + maxTextWidth < caretViewPosition) {
				viewPosition = caretViewPosition - 2 * maxTextWidth / 3;
				if (viewPosition + maxTextWidth > textWidth) {
					viewPosition = textWidth - maxTextWidth;
				}
			}
		}
	}

	private String getTextPassword() {
		if (password) {
			return Utils.getString(PASSWORD_CHAR, buffer.length());
		} else {
			return buffer.toString();
		}
	}

	public String getText() {
		if (state == STATE_MULTITAP) {
			commit();
		}
		return buffer.toString();
	}

	public void setText(String text) {
		if (state == STATE_MULTITAP) {
			state = STATE_ACTIVE;
		}
		buffer.setLength(0);
		if (!Utils.isNullOrEmpty(text)) {
			buffer.append(text);
			for (int i = buffer.length() - 1; i >= 0; i--) {
				if (!isLegalChar(buffer.charAt(i))) {
					buffer.deleteCharAt(i);
				}
			}
		}
		caretPosition = buffer.length();
		updateViewPosition();
		setMaxLength(maxLength);
	}

	public int length() {
		return buffer.length() + (state == STATE_MULTITAP ? 1 : 0);
	}

	public void setMaxLength(int len) {
		maxLength = len;
		if (len != LENGTH_UNLIMITED && buffer.length() > len) {
			buffer.setLength(len);
			if (caretPosition > len) {
				caretPosition = len;
				updateViewPosition();
			}
		}
	}

	private String getTextBeforeCaret() {
		if (password) {
			return Utils.getString(PASSWORD_CHAR, caretPosition);
		}
		return buffer.toString().substring(0, caretPosition);
	}

	private String getTextAfterCaret() {
		if (password) {
			return Utils.getString(PASSWORD_CHAR, buffer.length() - caretPosition);
		}
		return buffer.toString().substring(caretPosition);
	}

	public void render(Graphics g, int x, int y) {
		int w = width;
		int h = height;
		parent.drawRect(g, borderColor, x, y, w, h);
		x += 1;
		y += 1;
		w -= 2;
		h -= 2;
		parent.fillRect(g, backgroundColor, x, y, w, h);
		y += 1;
		h -= 2;
		int primaryTextY = y + (h - parent.getFontHeight(primaryFontId)) / 2;
		int secondaryTextY = y + (h - parent.getFontHeight(secondaryFontId)) / 2;
		if (state != STATE_INACTIVE) {
			String currentInputMethod = INPUT_METHODS[imIndex];
			int currentInputMethodWidth = parent.getStringWidth(currentInputMethod, secondaryFontId) + 4;
			parent.fillRect(g, foregroundColor, x + w - maxInputMethodWidth + 1, y, maxInputMethodWidth - 2, h);
			parent.drawString(g, currentInputMethod, secondaryFontId, x + w - (maxInputMethodWidth + currentInputMethodWidth) / 2 + 2, secondaryTextY, Graphics.TOP | Graphics.LEFT);
			w -= maxInputMethodWidth;
		}
		int clipX = g.getClipX();
		int clipY = g.getClipY();
		int clipWidth = g.getClipWidth();
		int clipHeight = g.getClipHeight();
		g.clipRect(x, y, w, h);
		x += 1;
		x -= viewPosition;
		if (state == STATE_MULTITAP) {
			String textBeforeCaret = getTextBeforeCaret();
			parent.drawString(g, textBeforeCaret, primaryFontId, x, primaryTextY, Graphics.TOP | Graphics.LEFT);
			int caretVisualPosition = parent.getStringWidth(textBeforeCaret, primaryFontId);
			x += caretVisualPosition;
			char c = getCurrentMultitapChar();
			int charWidth = parent.getCharWidth(c, secondaryFontId);
			parent.fillRect(g, foregroundColor, x, y, charWidth, h);
			parent.drawChar(g, c, secondaryFontId, x, secondaryTextY, Graphics.TOP | Graphics.LEFT);
			x += charWidth;
			String textAfterCaret = getTextAfterCaret();
			parent.drawString(g, textAfterCaret, primaryFontId, x, primaryTextY, Graphics.TOP | Graphics.LEFT);
		} else {
			String text = getTextPassword();
			parent.drawString(g, text, primaryFontId, x, primaryTextY, Graphics.TOP | Graphics.LEFT);
			if (state == STATE_ACTIVE && stateTime % (CARET_VISIBLE_TIME + CARET_INVISIBLE_TIME) < CARET_VISIBLE_TIME) {
				String textBeforeCaret = text.substring(0, caretPosition);
				x += parent.getStringWidth(textBeforeCaret, primaryFontId);
				parent.fillRect(g, foregroundColor, x, y, 1, h);
			}
		}
		g.setClip(clipX, clipY, clipWidth, clipHeight);
	}

	public void update(int dt) {
		stateTime += dt;
		if (state == STATE_MULTITAP && stateTime >= MULTITAP_TIMEOUT) {
			commit();
		}
	}

	public boolean isActive() {
		return state != STATE_INACTIVE;
	}

	public void setActive(boolean active) {
		if (active && state == STATE_INACTIVE) {
			setState(STATE_ACTIVE);
		}
		if (!active && state == STATE_ACTIVE) {
			setState(STATE_INACTIVE);
		}
		if (!active && state == STATE_MULTITAP) {
			commit();
			setState(STATE_INACTIVE);
		}
	}

	private void changeInputMethod() {
		if (state == STATE_MULTITAP) {
			commit();
		}
		if (type == TYPE_ANY || type == (TYPE_LETTERS | TYPE_DIGITS)) {
			imIndex++;
			imIndex %= INPUT_METHODS.length;
		} else if (type == TYPE_LETTERS) {
			imIndex++;
			imIndex %= INPUT_METHODS.length - 1;
		}
	}

	public void keyPressed(int key) {
		//#debug
		debug("TF.keyPressed " + key, null);
		if (imIndex != IM_DIGITS) {
			//#debug
			debug("imIndex !!!!! IM_DIGITS ", null);
			if (keyPressedMultitap(key, MULTITAP_KEYS, multitapValues)) {
				//#debug
				debug("TF.keyPressedMultitap", null);
				return;
			}
		} else {
			//#debug
			debug("imIndex = IM_DIGITS ", null);
			if (keyPressedMultitap(key, MULTITAP_DIGITS_KEYS, multitapDigitsValues)) {
				//#debug
				debug("TF.keyPressedMultitap", null);
				return;
			}
		}
		switch (key) {
			case BaseCanvas.KEY_LEFT:
				moveCaretLeft();
				break;
			case BaseCanvas.KEY_RIGHT:
				moveCaretRight();
				break;
			case BaseCanvas.KEY_DELETE:
			case BaseCanvas.KEY_BACKSPACE:
			case BaseCanvas.KEY_RIGHT_SOFT:
				deleteCharAtCaretPosition();
				break;
			case BaseCanvas.KEY_POUND:
			case -50:
				changeInputMethod();
				break;
			default:
				insertIfAscii(key, true);
		}
	}

	private boolean keyPressedMultitap(int key, int[] keys, String[] values) {
		int newKeyIndex = findMultitapKeyIndex(keys, key);
		if (newKeyIndex != -1) {
			if (state == STATE_MULTITAP) {
				if (mtKeyIndex == newKeyIndex) {
					mtValueIndex++;
					mtValueIndex %= values[mtKeyIndex].length();
					stateTime = 0;
					updateViewPosition();
					return true;
				}
				commit();
			}
			if (maxLength == LENGTH_UNLIMITED || buffer.length() < maxLength) {
				mtKeyIndex = newKeyIndex;
				mtValueIndex = 0;
				int len = values[mtKeyIndex].length();
				//#debug
				debug("values: " + values[mtKeyIndex], null);
				if (len == 1) {
					commit();
				} else if (len > 1) {
					setState(STATE_MULTITAP);
					//#debug
					debug("setState(STATE_MULTITAP)", null);
					updateViewPosition();
					//#debug
					debug("state: " + state, null);
				}
			}
			return true;
		}
		return false;
	}

	void internalKeyPressed(int key) {
		switch (key) {
			case BaseCanvas.KEY_LEFT:
				moveCaretLeft();
				break;
			case BaseCanvas.KEY_RIGHT:
				moveCaretRight();
				break;
			case BaseCanvas.KEY_DELETE:
				deleteCharAtCaretPosition();
				break;
			default:
				insertIfAscii(key, false);
		}
	}

	private void moveCaretLeft() {
		if (state == STATE_MULTITAP) {
			commit();
		} else if (caretPosition > 0) {
			caretPosition--;
			updateViewPosition();
		}
	}

	private void moveCaretRight() {
		if (state == STATE_MULTITAP) {
			commit();
		} else if (caretPosition < buffer.length()) {
			caretPosition++;
			updateViewPosition();
		}
	}

	private void deleteCharAtCaretPosition() {
		if (state == STATE_MULTITAP) {
			setState(STATE_ACTIVE);
		} else if (caretPosition > 0) {
			caretPosition--;
			buffer.deleteCharAt(caretPosition);
		}
		updateViewPosition();
	}

	private void insertIfAscii(int key, boolean applyInputMethod) {
		if (key >= ' ') {
			if (state == STATE_MULTITAP) {
				commit();
			}
			if (maxLength == LENGTH_UNLIMITED || buffer.length() < maxLength) {
				char c = (char) key;
				if (applyInputMethod) {
					if (imIndex == IM_LOWERCASE) {
						c = Character.toLowerCase(c);
					} else if (imIndex == IM_UPPERCASE) {
						c = Character.toUpperCase(c);
					}
				}
				if (isLegalChar(c)) {
					insertChar(c);
				}
			}
		}
	}

	private void setState(int newState) {
		state = newState;
		stateTime = 0;
	}

	public int getPreferredWidth() {
		return parent.getStringWidth(getText(), primaryFontId) + maxInputMethodWidth + 6;
	}

	public int getPreferredHeight() {
		return Math.max(parent.getFontHeight(primaryFontId), parent.getFontHeight(secondaryFontId)) + 6;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	//#mdebug
	private static void debug(String str, Throwable ex) {
		Application.debug(str, ex);
	}
	//#enddebug
}
