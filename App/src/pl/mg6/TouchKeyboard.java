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
//#if TouchScreen
public final class TouchKeyboard {

	private BaseCanvas parent;
	private TextField textField;

	private int fontId = -1;
	private int backgroundColor;
	private int borderColor;
	private int selectionColor;

	private int width;
	private int height;

	private int pressedX;
	private int pressedY;
	private int pressedTimeout;

	public static final int SIZE_PREFERRED = -1;
	public static final int SIZE_CURRENT = -2;

	private static final int ROW_COUNT = 3;
	private static final int COLUMN_COUNT = 11;

	private static final String[] QWERTY_LOWERCASE = {
		"qwertyuiop",
		"asdfghjkl",
		"zxcvbnm  "
	};

	private static final String[] QWERTY_UPPERCASE = {
		"QWERTYUIOP",
		"ASDFGHJKL",
		"ZXCVBNM  "
	};

	private static final String[] OTHER = {
		"123.,?!'()",
		"456-+*/\\\"",
		"7890_:;@#"
	};

	private static final String[] ADDON = {
		"\u007F", "\u2397\u2398", "\u2190\u2192"
	};

	private String[][] pages;
	private int currentPage;

	public TouchKeyboard(BaseCanvas parent, TextField textField) {
		this.parent = parent;
		this.textField = textField;
		createPages();
	}

	private void createPages() {
		int pageCount = 0;
		int[] pageIndexes = new int[3];
		if (isLegalCharIn(QWERTY_LOWERCASE)) {
			pageIndexes[pageCount] = 0;
			pageCount++;
		}
		if (isLegalCharIn(QWERTY_UPPERCASE)) {
			pageIndexes[pageCount] = 1;
			pageCount++;
		}
		if (isLegalCharIn(OTHER)) {
			pageIndexes[pageCount] = 2;
			pageCount++;
		}
		pages = new String[pageCount][ROW_COUNT];
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < pageCount; i++) {
			String[] page;
			int index = pageIndexes[i];
			if (index == 0) {
				page = QWERTY_LOWERCASE;
			} else if (index == 1) {
				page = QWERTY_UPPERCASE;
			} else {
				page = OTHER;
			}
			for (int j = 0; j < ROW_COUNT; j++) {
				sb.setLength(0);
				sb.append(page[j]);
				for (int k = 0; k < sb.length(); k++) {
					if (!textField.isLegalChar(sb.charAt(k))) {
						sb.setCharAt(k, '\u0000');
					}
				}
				if (pageCount == 1 && j == 1) {
					sb.append("\u0000\u0000");
				} else {
					sb.append(ADDON[j]);
				}
				pages[i][j] = sb.toString();
			}
		}
	}

	private boolean isLegalCharIn(String[] page) {
		for (int i = 0; i < page.length; i++) {
			for (int j = 0; j < page[i].length(); j++) {
				if (textField.isLegalChar(page[i].charAt(j))) {
					return true;
				}
			}
		}
		return false;
	}

	public void update(int dt) {
		pressedTimeout -= dt;
	}

	public void render(Graphics g, int x, int y) {
		parent.fillRect(g, borderColor, x, y, width, height);
		int cellW = (width - 1) / COLUMN_COUNT;
		int cellH = (height - 1) / ROW_COUNT;
		x += (width - cellW * COLUMN_COUNT) / 2;
		y += (height - cellH * ROW_COUNT) / 2;
		parent.fillRect(g, backgroundColor, x + 1, y + 1, COLUMN_COUNT * cellW - 1, ROW_COUNT * cellH - 1);
		String lastRow = pages[currentPage][ROW_COUNT - 1];
		for (int i = 1; i < COLUMN_COUNT; i++) {
			int len = ROW_COUNT;
			if (lastRow.charAt(i) != '\u0000' && lastRow.charAt(i) == lastRow.charAt(i - 1)) {
				len--;
			}
			parent.drawLine(g, borderColor, x + i * cellW, y, x + i * cellW, y + len * cellH);
		}
		for (int i = 1; i < ROW_COUNT; i++) {
			parent.drawLine(g, borderColor, x, y + i * cellH, x + COLUMN_COUNT * cellW, y + i * cellH);
		}
		x += 1;
		y += 1;
		int charY = (cellH - parent.getFontHeight(fontId)) / 2;
		for (int i = 0; i < ROW_COUNT; i++) {
			int xx = x;
			for (int j = 0; j < COLUMN_COUNT; j++) {
				char c = pages[currentPage][i].charAt(j);
				if (c == '\u0000') {
					parent.fillRect(g, borderColor, xx, y, cellW - 1, cellH - 1);
				} else {
					if (j == pressedX && i == pressedY && pressedTimeout > 0) {
						int fillX = xx;
						int fillW = cellW - 1;
						if (i == 2) {
							int k = j;
							while (k - 1 >= 0 && pages[currentPage][i].charAt(k - 1) == c) {
								k--;
							}
							fillX -= (j - k) * cellW;
							fillW += (j - k) * cellW;
							k = j;
							while (k + 1 < COLUMN_COUNT && pages[currentPage][i].charAt(k + 1) == c) {
								k++;
							}
							fillW += (k - j) * cellW;
						}
						parent.fillRect(g, selectionColor, fillX, y, fillW, cellH - 1);
					}
					int charX = (cellW - parent.getCharWidth(c, fontId)) / 2;
					parent.drawChar(g, c, fontId, xx + charX, y + charY, Graphics.TOP | Graphics.LEFT);
				}
				xx += cellW;
			}
			y += cellH;
		}
	}

	public void pointerPressed(int x, int y) {
		int cellW = (width - 1) / COLUMN_COUNT;
		int cellH = (height - 1) / ROW_COUNT;
		x -= (width - cellW * COLUMN_COUNT) / 2;
		y -= (height - cellH * ROW_COUNT) / 2;
		if (x < 0 || y < 0) {
			return;
		}
		x /= cellW;
		y /= cellH;
		if (x >= 0 && x < COLUMN_COUNT && y >= 0 && y < ROW_COUNT) {
			pressedX = x;
			pressedY = y;
			pressedTimeout = 300;
			int pressed = pages[currentPage][y].charAt(x);
			if (pressed == '\u2397') {
				prevPage();
			} else if (pressed == '\u2398') {
				nextPage();
			} else {
				switch (pressed) {
					case '\u007F': pressed = BaseCanvas.KEY_DELETE; break;
					case '\u2190': pressed = BaseCanvas.KEY_LEFT; break;
					case '\u2192': pressed = BaseCanvas.KEY_RIGHT; break;
				}
				textField.internalKeyPressed(pressed);
			}
		}
	}

	public void prevPage() {
		if (currentPage == 0) {
			currentPage = pages.length - 1;
		} else {
			currentPage--;
		}
	}

	public void nextPage() {
		if (currentPage == pages.length - 1) {
			currentPage = 0;
		} else {
			currentPage++;
		}
	}

	public void setFontsAndColors(int font, int background, int border, int selection) {
		fontId = font;
		backgroundColor = background;
		borderColor = border;
		selectionColor = selection;
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
	}

	public int getPreferredWidth() {
		return parent.getWidth();
	}

	public int getPreferredHeight() {
		return 1 + 2 + (2 * parent.getFontHeight(fontId) + 1) * ROW_COUNT;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
//#endif