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

import java.util.Vector;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 *
 * @author maciej
 */
public abstract class Application extends MIDlet {

	public static Application instance;
	public static Display display;

	private BaseCanvas canvas;

	protected void startApp() throws MIDletStateChangeException {
		if (canvas == null) {

			if (instance != null) {
				throw new MIDletStateChangeException();
			}
			instance = this;
			display = Display.getDisplay(this);

			canvas = createCanvas();
			canvas.startMainThread();
			display.setCurrent(canvas);
		}
	}

	protected abstract BaseCanvas createCanvas();

	protected void pauseApp() {
	}

	protected void destroyApp(boolean unconditional) {
		destroy();
	}

	public void exit() {
		destroy();
		notifyDestroyed();
	}

	private void destroy() {
		canvas.stopMainThread();
		canvas = null;
	}

	//#mdebug
	public static Vector exceptions = new Vector();
	private static long currentTime, lastTime = -1;

	public static void debug(String str, Throwable ex) {
		if (ex != null) {
			ex.printStackTrace();
		} else {
			System.out.println(str);
		}
//		synchronized (exceptions) {
//			exceptions.addElement("");
//			if (ex != null) {
//				String message = ex.getMessage();
//				if (message != null) {
//					exceptions.addElement(message);
//				}
//				exceptions.addElement(ex.getClass().getName());
//			}
//			exceptions.addElement(str);
//			currentTime = System.currentTimeMillis();
//			exceptions.addElement(Utils.formatTime(currentTime) + (lastTime != -1 ? " (+" + Utils.formatTimeSpan(currentTime - lastTime) + ")" : ""));
//			lastTime = currentTime;
//		}
	}
	//#enddebug
}
