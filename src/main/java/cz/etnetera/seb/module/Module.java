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
package cz.etnetera.seb.module;


import cz.etnetera.seb.VerificationException;
import cz.etnetera.seb.element.SebElement;
import cz.etnetera.seb.event.impl.AfterModuleInitEvent;
import cz.etnetera.seb.event.impl.BeforeModuleInitEvent;
import cz.etnetera.seb.page.Page;

/**
 * Basic module which supports elements and modules auto loading
 * similar as {@link Page}. It extends {@link SebElement}.
 */
public class Module extends SebElement {
	
	@Override
	protected void initPresent() {
		try {
			triggerEvent(constructEvent(BeforeModuleInitEvent.class).with(this));
			beforeInit();
			beforeInitElements();
			initElements();
			afterInitElements();
			beforeSetup();
			setup();
			afterSetup();
			beforeVerify();
			verify();
			afterVerify();
			afterInit();
		} catch (Exception e) {
			throw e;
		}
		triggerEvent(constructEvent(AfterModuleInitEvent.class).with(this));
	}
	
	protected void verify() throws VerificationException {
		try {
			verifyThis();
		} catch (Exception e) {
			throw new VerificationException("Module is wrong " + this.getClass().getName(), e);
		}
	}
	
	/**
	 * Override this method to initialize
	 * custom elements or do some other things
	 * before verification.
	 */
	protected void setup() {
		// initialize custom elements etc.
	}

	/**
	 * Override this method to perform custom check
	 * after all fields are initiated and setup is done.
	 */
	protected void verifyThis() {
		// check if we are in right module
	}

	protected void beforeInit() {
		// do whatever you want
	}
	
	protected void beforeInitElements() {
		// do whatever you want
	}
	
	protected void afterInitElements() {
		// do whatever you want
	}
	
	protected void beforeSetup() {
		// do whatever you want
	}
	
	protected void afterSetup() {
		// do whatever you want
	}
	
	protected void beforeVerify() {
		// do whatever you want
	}
	
	protected void afterVerify() {
		// do whatever you want
	}
	
	protected void afterInit() {
		// do whatever you want
	}
	
}
