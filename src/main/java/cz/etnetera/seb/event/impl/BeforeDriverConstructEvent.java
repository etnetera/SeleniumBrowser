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
package cz.etnetera.seb.event.impl;

import org.openqa.selenium.remote.DesiredCapabilities;

import cz.etnetera.seb.event.SebEvent;
import cz.etnetera.seb.listener.SebListener;

public class BeforeDriverConstructEvent extends SebEvent {
	
	protected DesiredCapabilities capabilities;
	
	public BeforeDriverConstructEvent with(DesiredCapabilities capapabilities) {
		this.capabilities = capapabilities;
		return this;
	}
	
	public DesiredCapabilities getCapabilities() {
		if (capabilities == null) {
			capabilities = new DesiredCapabilities();
		}
		return capabilities;
	}

	@Override
	protected void notifySpecific(SebListener listener) {
		listener.beforeDriverConstruct(this);
	}
	
}
