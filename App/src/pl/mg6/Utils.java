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

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/**
 *
 * @author maciej
 */
public final class Utils {

	private Utils() {
	}

	public static final int COLOR_BUTTER_1 = 0xfce94f;
	public static final int COLOR_BUTTER_2 = 0xedd400;
	public static final int COLOR_BUTTER_3 = 0xc4a000;
	public static final int COLOR_ORANGE_1 = 0xfcaf3e;
	public static final int COLOR_ORANGE_2 = 0xf57900;
	public static final int COLOR_ORANGE_3 = 0xce5c00;
	public static final int COLOR_CHOCOLADE_1 = 0xe9b96e;
	public static final int COLOR_CHOCOLADE_2 = 0xc17d11;
	public static final int COLOR_CHOCOLADE_3 = 0x8f5902;
	public static final int COLOR_CHAMELEON_1 = 0x8ae234;
	public static final int COLOR_CHAMELEON_2 = 0x73d216;
	public static final int COLOR_CHAMELEON_3 = 0x4e9a06;
	public static final int COLOR_SKY_BLUE_1 = 0x729fcf;
	public static final int COLOR_SKY_BLUE_2 = 0x3465a4;
	public static final int COLOR_SKY_BLUE_3 = 0x204a87;
	public static final int COLOR_PLUM_1 = 0xad7fa8;
	public static final int COLOR_PLUM_2 = 0x75507b;
	public static final int COLOR_PLUM_3 = 0x5c3566;
	public static final int COLOR_SCARLET_RED_1 = 0xef2929;
	public static final int COLOR_SCARLET_RED_2 = 0xcc0000;
	public static final int COLOR_SCARLET_RED_3 = 0xa40000;
	public static final int COLOR_ALUMINIUM_1 = 0xeeeeec;
	public static final int COLOR_ALUMINIUM_2 = 0xd3d7cf;
	public static final int COLOR_ALUMINIUM_3 = 0xbabdb6;
	public static final int COLOR_ALUMINIUM_4 = 0x888a85;
	public static final int COLOR_ALUMINIUM_5 = 0x555753;
	public static final int COLOR_ALUMINIUM_6 = 0x2e3436;

	public static final int COLOR_BLACK = 0x000000;
	public static final int COLOR_WHITE = 0xffffff;
	
	public static final int COLOR_RED = 0xff0000;
	public static final int COLOR_GREEN = 0x00ff00;
	public static final int COLOR_BLUE = 0x0000ff;
	
	public static final int COLOR_YELLOW = 0xffff00;
	public static final int COLOR_MAGENTA = 0xff00ff;
	public static final int COLOR_CYAN = 0x00ffff;

	public static int getColor(int color1, int color2, int percent, int max) {
		if (percent < 0 || percent > max) {
			throw new IllegalArgumentException();
		}
		int red = (color2 & COLOR_RED) - (color1 & COLOR_RED);
		int green = (color2 & COLOR_GREEN) - (color1 & COLOR_GREEN);
		int blue = (color2 & COLOR_BLUE) - (color1 & COLOR_BLUE);
		red >>= 15;
		green >>= 7;
		blue <<= 1;
		red |= 0x1;
		green |= 0x1;
		blue |= 0x1;
		red = red * percent / max;
		green = green * percent / max;
		blue = blue * percent / max;
		red <<= 15;
		green <<= 7;
		blue >>= 1;
		red = (red & COLOR_RED) + (color1 & COLOR_RED);
		green = (green & COLOR_GREEN) + (color1 & COLOR_GREEN);
		blue = (blue & COLOR_BLUE) + (color1 & COLOR_BLUE);
		int color = (int) (red | green | blue);
		return color;
	}

	public static void sleep(int dt) {
		try {
			if (dt > 0) {
				Thread.sleep(dt);
			}
		} catch (InterruptedException _) {
			//#debug
			Application.debug("sleep", _);
		}
	}

	private static Random random;

	public static int random() {
		if (random == null) {
			random = new Random();
		}
		return random.nextInt();
	}

	public static String formatDate(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(time));
		StringBuffer sb = new StringBuffer();
		formatDate(cal, sb);
		return sb.toString();
	}

	public static String formatDateAndTime(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(time));
		StringBuffer sb = new StringBuffer();
		formatDate(cal, sb);
		sb.append(' ');
		formatTime(cal, sb);
		return sb.toString();
	}

	public static String formatTime(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(time));
		StringBuffer sb = new StringBuffer();
		formatTime(cal, sb);
		return sb.toString();
	}

	private static void formatDate(Calendar cal, StringBuffer sb) {
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH) + 1;
		int d = cal.get(Calendar.DAY_OF_MONTH);
		sb.append(y);
		sb.append('-');
		if (m < 10) {
			sb.append('0');
		}
		sb.append(m);
		sb.append('-');
		if (d < 10) {
			sb.append('0');
		}
		sb.append(d);
	}

	private static void formatTime(Calendar cal, StringBuffer sb) {
		int h = cal.get(Calendar.HOUR_OF_DAY);
		int m = cal.get(Calendar.MINUTE);
		int s = cal.get(Calendar.SECOND);
		int ms = cal.get(Calendar.MILLISECOND);
		if (h < 10) {
			sb.append('0');
		}
		sb.append(h);
		sb.append(':');
		if (m < 10) {
			sb.append('0');
		}
		sb.append(m);
		sb.append(':');
		if (s < 10) {
			sb.append('0');
		}
		sb.append(s);
		sb.append('.');
		if (ms < 100) {
			sb.append('0');
			if (ms < 10) {
				sb.append('0');
			}
		}
		sb.append(ms);
	}

	public static final int TIME_MILLISECOND = 1;
	public static final int TIME_SECOND = 1000 * TIME_MILLISECOND;
	public static final int TIME_MINUTE = 60 * TIME_SECOND;
	public static final int TIME_HOUR = 60 * TIME_MINUTE;
	public static final int TIME_DAY = 24 * TIME_HOUR;

	public static String formatTimeSpan(long time) {
		int h = (int) (time / TIME_HOUR);
		int m = (int) (time / TIME_MINUTE % 60);
		int s = (int) (time / TIME_SECOND % 60);
		int ms = (int) (time % 1000);
		StringBuffer sb = new StringBuffer();
		if (h > 0 || m > 0) {
			if (h > 0) {
				sb.append(h);
				sb.append(':');
				if (m < 10) {
					sb.append('0');
				}
			}
			sb.append(m);
			sb.append(':');
			if (s < 10) {
				sb.append('0');
			}
		}
		sb.append(s);
		sb.append('.');
		if (ms < 100) {
			sb.append('0');
			if (ms < 10) {
				sb.append('0');
			}
		}
		sb.append(ms);
		return sb.toString();
	}

	public static String getString(char c, int repeated) {
		char[] array = new char[repeated];
		for (int i = 0; i < repeated; i++) {
			array[i] = c;
		}
		return new String(array);
	}

	public static String getString(String str, int repeated) {
		char[] array = new char[str.length() * repeated];
		for (int i = 0; i < repeated; i++) {
			str.getChars(0, str.length(), array, i * str.length());
		}
		return new String(array);
	}

	public static String join(String[] strs, char separator) {
		if (strs == null || strs.length == 0) {
			return null;
		}
		int length = strs.length - 1;
		for (int i = 0; i < strs.length; i++) {
			length += strs[i].length();
		}
		StringBuffer sb = new StringBuffer(length);
		sb.append(strs[0]);
		for (int i = 1; i < strs.length; i++) {
			sb.append(separator);
			sb.append(strs[i]);
		}
		return sb.toString();
	}

	public static String join(String[] strs, String separator) {
		if (strs == null || strs.length == 0) {
			return null;
		}
		int length = (strs.length - 1) * separator.length();
		for (int i = 0; i < strs.length; i++) {
			length += strs[i].length();
		}
		StringBuffer sb = new StringBuffer(length);
		sb.append(strs[0]);
		for (int i = 1; i < strs.length; i++) {
			sb.append(separator);
			sb.append(strs[i]);
		}
		return sb.toString();
	}

	public static String[] split(String str, char separator) {
		String[] ret = new String[count(str, separator) + 1];
		int index = 0;
		int start = 0;
		int end = str.indexOf(separator);
		while (end != -1) {
			ret[index] = str.substring(start, end);
			index++;
			start = end + 1;
			end = str.indexOf(separator, start);
		}
		ret[index] = str.substring(start);
		return ret;
	}

	public static String[] split(String str, String separator) {
		String[] ret = new String[count(str, separator) + 1];
		int index = 0;
		int start = 0;
		int end = str.indexOf(separator);
		while (end != -1) {
			ret[index] = str.substring(start, end);
			index++;
			start = end + 1;
			end = str.indexOf(separator, start);
		}
		ret[index] = str.substring(start);
		return ret;
	}

	public static int count(String str, char c) {
		int count = 0;
		int index = str.indexOf(c);
		while (index != -1) {
			count++;
			index = str.indexOf(c, index + 1);
		}
		return count;
	}

	public static int count(String str, String substr) {
		int count = 0;
		int index = str.indexOf(substr);
		while (index != -1) {
			count++;
			index = str.indexOf(substr, index + substr.length());
		}
		return count;
	}

	public static void trim(String[] strs) {
		if (strs != null) {
			for (int i = 0; i < strs.length; i++) {
				if (strs[i] != null) {
					strs[i] = strs[i].trim();
				}
			}
		}
	}

	public static boolean isNullOrEmpty(String str) {
		return str == null || str.length() == 0;
	}

	public static boolean isNullOrEmpty(int[] array) {
		return array == null || array.length == 0;
	}

	public static int indexOf(Object[] array, Object obj) {
		for (int i = 0; i < array.length; i++) {
			if (obj == array[i]) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(String[] array, String str) {
		for (int i = 0; i < array.length; i++) {
			if (str == null && array[i] == null || str != null && str.equals(array[i])) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(int[] array, int value) {
		for (int i = 0; i < array.length; i++) {
			if (value == array[i]) {
				return i;
			}
		}
		return -1;
	}

	public static int compareVersions(String v1, String v2) {
		String[] values1 = split(v1, '.');
		String[] values2 = split(v2, '.');
		int index = 0;
		while (true) {
			if (index == values1.length && index == values2.length) {
				return 0;
			}
			if (index == values1.length) {
				return -1;
			}
			if (index == values2.length) {
				return 1;
			}
			int i1 = Integer.parseInt(values1[index]);
			int i2 = Integer.parseInt(values2[index]);
			if (i1 < i2) {
				return -1;
			}
			if (i1 > i2) {
				return 1;
			}
			index++;
		}
	}

	public static boolean isInRect(int pointX, int pointY, int rectX, int rectY, int rectW, int rectH) {
		return !(pointX < rectX || pointX >= rectX + rectW || pointY < rectY || pointY >= rectY + rectH);
	}

	public static int bitCount(int x) {
		int count = 0;
		while (x != 0) {
			count++;
			x &= x - 1;
		}
		return count;
	}

//	public static final int REGION_TOP = 1001;
//	public static final int REGION_BOTTOM = 1002;
//	public static final int REGION_LEFT = 1003;
//	public static final int REGION_RIGHT = 1004;
//
//	// TTTTT LTTTTTTTTTR
//	// LTTTR LLTTTTTTTRR
//	// LLTRR LLLTTTTTRRR
//	// LLRRR LLLBBBBBRRR
//	// LLRRR LLBBBBBBBRR
//	// LLBRR LBBBBBBBBBR
//	// LBBBR
//	// BBBBB
//	public static int region4(int pointerX, int pointerY, int x, int y, int w, int h) {
//		pointerX -= x;
//		pointerY -= y;
//		w -= x;
//		h -= y;
//		if (w <= h) {
//			if (pointerX >= pointerY && w - 1 - pointerX >= pointerY) {
//				return REGION_TOP;
//			} else if (pointerX >= h - 1 - pointerY && w - 1 - pointerX >= h - 1 - pointerY) {
//				return REGION_BOTTOM;
//			} else if (pointerX < w / 2) {
//				return REGION_LEFT;
//			} else {
//				return REGION_RIGHT;
//			}
//		} else {
//			if (pointerY >= pointerX && h - 1 - pointerY >= pointerX) {
//				return REGION_LEFT;
//			} else if (pointerY >= w - 1 - pointerX && h - 1 - pointerY >= w - 1 - pointerX) {
//				return REGION_RIGHT;
//			} else if (pointerY < h / 2) {
//				return REGION_TOP;
//			} else {
//				return REGION_BOTTOM;
//			}
//		}
//	}
}
