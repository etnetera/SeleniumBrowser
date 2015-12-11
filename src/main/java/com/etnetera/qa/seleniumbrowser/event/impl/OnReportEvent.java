package com.etnetera.qa.seleniumbrowser.event.impl;

import com.etnetera.qa.seleniumbrowser.browser.Browser;
import com.etnetera.qa.seleniumbrowser.browser.BrowserUtils;
import com.etnetera.qa.seleniumbrowser.event.BrowserEvent;
import com.etnetera.qa.seleniumbrowser.listener.BrowserListener;

public class OnReportEvent extends BrowserEvent {

	protected String label;

	public OnReportEvent with(String label) {
		this.label = label;
		return this;
	}
	
	@Override
	public void notify(BrowserListener listener) {
		listener.onReport(this);
	}
	
	public String getLabel() {
		return label;
	}

	@Override
	protected String generateLabel() {
		return BrowserUtils.join(Browser.LABEL_DELIMITER, super.generateLabel(), label);
	}
	
}