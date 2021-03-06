/* Copyright 2016 Etnetera a.s.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.etnetera.seb.listener.impl;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import cz.etnetera.seb.Seb;
import cz.etnetera.seb.event.SebEvent;
import cz.etnetera.seb.event.impl.AfterPageInitEvent;
import cz.etnetera.seb.event.impl.BeforeDriverQuitEvent;
import cz.etnetera.seb.event.impl.BeforePageInitEvent;
import cz.etnetera.seb.event.impl.OnReportEvent;
import cz.etnetera.seb.listener.SebListener;

public class ScreenshotListener extends SebListener {

	@SuppressWarnings("unchecked")
	@Override
	public void init(Seb seb) {
		super.init(seb);
		// enable this listener on report as default
		enable(OnReportEvent.class);
	}

	@Override
	public void beforePageInit(BeforePageInitEvent event) {
		takeScreenshot(event);
	}
	
	@Override
	public void afterPageInit(AfterPageInitEvent event) {
		takeScreenshot(event);
	}

	@Override
	public void beforeDriverQuit(BeforeDriverQuitEvent event) {
		takeScreenshot(event);
	}

	@Override
	public void onReport(OnReportEvent event) {
		takeScreenshot(event);
	}

	protected void takeScreenshot(SebEvent event) {
		if (event.getSeb().isReported() && isScreenshotDriver(event))
			saveFile(event, event.getDriver(TakesScreenshot.class).getScreenshotAs(OutputType.BYTES), null, "png");
	}

	protected boolean isScreenshotDriver(SebEvent event) {
		return event.getDriver() instanceof TakesScreenshot;
	}

}
