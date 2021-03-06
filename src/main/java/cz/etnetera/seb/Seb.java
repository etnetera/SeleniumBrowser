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
package cz.etnetera.seb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.openqa.selenium.support.pagefactory.AbstractAnnotations;
import org.openqa.selenium.support.pagefactory.DefaultElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocator;

import com.thoughtworks.selenium.webdriven.JavascriptLibrary;

import cz.etnetera.seb.configuration.BasicSebConfiguration;
import cz.etnetera.seb.configuration.SebConfiguration;
import cz.etnetera.seb.configuration.SebConfigurationConstructException;
import cz.etnetera.seb.element.SebElement;
import cz.etnetera.seb.element.SebElementConstructException;
import cz.etnetera.seb.element.SebElementInterceptor;
import cz.etnetera.seb.element.SebElementLoader;
import cz.etnetera.seb.element.SebFieldDecorator;
import cz.etnetera.seb.event.EventConstructException;
import cz.etnetera.seb.event.SebEvent;
import cz.etnetera.seb.event.impl.AfterDriverConstructEvent;
import cz.etnetera.seb.event.impl.AfterDriverQuitEvent;
import cz.etnetera.seb.event.impl.AfterSebQuitEvent;
import cz.etnetera.seb.event.impl.BeforeDriverConstructEvent;
import cz.etnetera.seb.event.impl.BeforeDriverQuitEvent;
import cz.etnetera.seb.event.impl.BeforeSebQuitEvent;
import cz.etnetera.seb.event.impl.LogEvent;
import cz.etnetera.seb.event.impl.OnFileSaveEvent;
import cz.etnetera.seb.event.impl.OnReportEvent;
import cz.etnetera.seb.event.impl.OnSebStartEvent;
import cz.etnetera.seb.listener.EventFiringSebBridgeListener;
import cz.etnetera.seb.listener.SebListener;
import cz.etnetera.seb.logic.Logic;
import cz.etnetera.seb.logic.LogicConstructException;
import cz.etnetera.seb.page.Page;
import cz.etnetera.seb.page.PageConstructException;
import cz.etnetera.seb.source.DataSource;
import cz.etnetera.seb.source.PropertySource;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * Wrapper class for {@link WebDriver}. It is configured using
 * {@link SebConfiguration}. Controls automatic reporting using listeners
 * provided by configuration.
 */
public class Seb implements SebContext {

	public static final String PROPERTIES_CONFIGURATION_KEY = "properties";

	public static final String DEFAULT_CONFIGURATION_KEY = "default";

	public static final String LABEL_DELIMITER = "-";

	public static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	protected SebConfiguration configuration;

	protected WebDriver driver;

	protected String baseUrl;

	protected String baseUrlRegex;

	protected boolean urlVerification;

	protected double waitTimeout;

	protected double waitRetryInterval;
	
	protected double waitBeforePageInitTimeout;

	protected Page page;

	protected String label;

	protected boolean reported;

	protected File reportDir;

	protected List<SebListener> listeners = new ArrayList<>();

	protected boolean alertSupported;

	protected boolean lazyDriver;

	protected Level logLevel;

	protected boolean started;

	protected Map<String, Object> dataHolder = new HashMap<String, Object>();

	protected SebUtils utils = new SebUtils();

	protected SebElementLoader elementLoader = new SebElementLoader();

	protected JavascriptLibrary javascriptLibrary = new JavascriptLibrary();

	/**
	 * Constructs a new instance with default configuration. It constructs
	 * {@link SebConfiguration} with system properties using
	 * {@link System#getProperties()} and properties from resource named
	 * seb.properties.
	 */
	public Seb() {
		this(false);
	}

	/**
	 * Set customStart to true to construct only and calling additional methods
	 * before manually calling {@link Seb#start()}.
	 * 
	 * Set customStart to <code>false</code> to construct a new instance with
	 * default configuration. It constructs {@link SebConfiguration} with system
	 * properties using {@link System#getProperties()} and properties from
	 * resource named seb.properties.
	 */
	public Seb(boolean customStart) {
		if (!customStart) {
			start();
		}
	}

	/**
	 * Constructs a new instance with configuration constructed from given
	 * class. Configuration class needs to have constructor with no parameters.
	 * 
	 * @param configCls
	 *            The configuration class
	 * @deprecated As of Seb version 0.3.22, replaced by
	 *             <code>new Seb(true).withConfiguration(configCls).start()</code>
	 *             .
	 */
	@Deprecated
	public <T extends SebConfiguration> Seb(Class<T> configCls) {
		withConfiguration(configCls);
		start();
	}

	/**
	 * Constructs a new instance and configures it.
	 * 
	 * @param configuration
	 *            The configuration
	 * @deprecated As of Seb version 0.3.22, replaced by
	 *             <code>new Seb(true).withConfiguration(configuration).start()</code>
	 *             .
	 */
	@Deprecated
	public Seb(SebConfiguration configuration) {
		withConfiguration(configuration);
		start();
	}

	/**
	 * Set configuration to default one. It is {@link BasicSebConfiguration}.
	 * 
	 * @return Seb instance
	 */
	public Seb withDefaultConfiguration() {
		return withConfiguration(BasicSebConfiguration.class);
	}

	/**
	 * Set configuration to configuration constructed from given class.
	 * Configuration class needs to have constructor with no parameters.
	 * 
	 * @param configCls
	 *            The configuration class
	 * @return Seb instance
	 */
	public <T extends SebConfiguration> Seb withConfiguration(Class<T> configCls) {
		try {
			return withConfiguration(configCls.getConstructor().newInstance());
		} catch (Exception e) {
			throw new SebConfigurationConstructException("Unable to construct Seb configuration " + configCls.getName(),
					e);
		}
	}

	/**
	 * Set configuration to given configuration instance.
	 * 
	 * @param configuration
	 *            The configuration
	 * @return Seb instance
	 */
	public Seb withConfiguration(SebConfiguration configuration) {
		this.configuration = configuration;
		return this;
	}

	/**
	 * Set label
	 * 
	 * @param label
	 *            Seb label
	 * @return Seb instance
	 */
	public Seb withLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Set label joining given labels.
	 * 
	 * @param labels
	 *            Seb labels
	 * @return Seb instance
	 */
	public Seb withLabel(String... labels) {
		this.label = utils.join(LABEL_DELIMITER, (Object[]) labels);
		return this;
	}

	/**
	 * Adds listener without initiating it. It will be initiated on Seb start.
	 * 
	 * @param listener
	 *            The added listener
	 * @return Seb instance
	 */
	public Seb withListener(SebListener listener) {
		listeners.add(listener);
		return this;
	}

	public Seb start() {
		if (started)
			return this;
		if (label == null)
			useEnclosingMethodLabel();
		initConfiguration();
		initListeners();
		started = true;
		triggerEvent(constructEvent(OnSebStartEvent.class));
		if (!lazyDriver)
			initDriver();
		return this;
	}

	protected void initConfiguration() {
		if (configuration == null)
			withDefaultConfiguration();
		applyConfiguration();
	}

	protected void applyConfiguration() {
		configuration.init();
		baseUrl = configuration.getBaseUrl();
		baseUrlRegex = configuration.getBaseUrlRegex();
		urlVerification = configuration.isUrlVerification();
		waitTimeout = configuration.getWaitTimeout();
		waitRetryInterval = configuration.getWaitRetryInterval();
		waitBeforePageInitTimeout = configuration.getWaitBeforePageInitTimeout();
		reported = configuration.isReported();
		if (reported) {
			reportDir = configuration.getReportDir();
			if (!reportDir.exists()) {
				try {
					Files.createDirectories(reportDir.toPath());
				} catch (IOException e) {
					throw new SebException("Report directory does not exists and can not be created " + reportDir);
				}
			} else if (!reportDir.isDirectory()) {
				throw new SebException("Report directory is not directory " + reportDir);
			} else if (!reportDir.canWrite()) {
				throw new SebException("Report directory is not writable " + reportDir);
			}
		}
		if (configuration instanceof DataSource)
			dataHolder = ((DataSource) configuration).getDataHolder();

		List<SebListener> confListeners = configuration.getListeners();
		if (confListeners != null)
			listeners.addAll(confListeners);
		lazyDriver = configuration.isLazyDriver();
		logLevel = configuration.getLogLevel();
	}

	protected void initListeners() {
		listeners.forEach(l -> l.init(this));
	}

	protected void initDriver() {
		// collect capabilities
		DesiredCapabilities caps = configuration.getCapabilities();
		// notify listeners to allow its change
		BeforeDriverConstructEvent befDriverConstEvent = constructEvent(BeforeDriverConstructEvent.class).with(caps);
		triggerEvent(befDriverConstEvent);
		WebDriver drv = configuration.getDriver(befDriverConstEvent.getCapabilities());

		driver = new EventFiringWebDriver(drv).register(new EventFiringSebBridgeListener(this));
		triggerEvent(constructEvent(AfterDriverConstructEvent.class));

		// set driver specific configurations
		alertSupported = configuration.isAlertSupported(drv);
	}

	/**
	 * Is driver ready?
	 * 
	 * @return Driver status.
	 */
	public boolean hasDriver() {
		return driver != null;
	}

	/**
	 * Adds listener. It is automatically initiated.
	 * 
	 * @param listener
	 *            The added listener
	 */
	public void addListener(SebListener listener) {
		listener.init(this);
		listeners.add(listener);
	}

	/**
	 * Seb label which is mainly used for reporting.
	 * 
	 * @return Seb label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Modify Seb label
	 * 
	 * @param label
	 *            Seb label
	 * @deprecated As of Seb version 0.3.22, replaced by
	 *             <code>Seb.withLabel(String label)</code>.
	 */
	@Deprecated
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Modify Seb label joining given labels.
	 * 
	 * @param labels
	 *            Seb labels
	 * @deprecated As of Seb version 0.3.22, replaced by
	 *             <code>Seb.withLabel(String... labels)</code>.
	 */
	@Deprecated
	public void setLabel(String... labels) {
		this.label = utils.join(LABEL_DELIMITER, (Object[]) labels);
	}

	/**
	 * Base URL for pages.
	 * 
	 * @return The base URL
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * Updates base URL
	 * 
	 * @param baseUrl
	 *            New base URL
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Base URL regex for pages.
	 * 
	 * @return The base URL regex
	 */
	public String getBaseUrlRegex() {
		return baseUrlRegex;
	}

	/**
	 * Updates base URL regex
	 * 
	 * @param baseUrlRegex
	 *            New base URL regex
	 */
	public void setBaseUrlRegex(String baseUrlRegex) {
		this.baseUrlRegex = baseUrlRegex;
	}

	/**
	 * Is URL on pages verified?
	 * 
	 * @return Verification status
	 */
	public boolean isUrlVerification() {
		return urlVerification;
	}

	/**
	 * Toggles pages URL verification.
	 * 
	 * @param urlVerification
	 *            The new status
	 */
	public void setUrlVerification(boolean urlVerification) {
		this.urlVerification = urlVerification;
	}

	@Override
	public double getWaitTimeout() {
		return waitTimeout;
	}

	/**
	 * Sets default wait timeout.
	 * 
	 * @param waitTimeout
	 *            The default wait timeout.
	 */
	public void setWaitTimeout(double waitTimeout) {
		this.waitTimeout = waitTimeout;
	}

	@Override
	public double getWaitRetryInterval() {
		return waitRetryInterval;
	}

	/**
	 * Sets default wait retry interval.
	 * 
	 * @param waitRetryInterval
	 *            The default wait retry interval.
	 */
	public void setWaitRetryInterval(double waitRetryInterval) {
		this.waitRetryInterval = waitRetryInterval;
	}
	
	@Override
	public double getWaitBeforePageInitTimeout() {
		return waitBeforePageInitTimeout;
	}

	/**
	 * Sets default wait before page initialization timeout.
	 * 
	 * @param waitBeforePageInitTimeout
	 *            The default wait before page initialization timeout.
	 */
	public void setWaitBeforePageInitTimeout(double waitBeforePageInitTimeout) {
		this.waitBeforePageInitTimeout = waitBeforePageInitTimeout;
	}

	/**
	 * Is storing files using Seb enabled.
	 * 
	 * @return Reporting status.
	 */
	public boolean isReported() {
		return reported;
	}

	/**
	 * Toggles storing files using Seb.
	 * 
	 * @param reported
	 *            The reported status.
	 */
	public void setReported(boolean reported) {
		this.reported = reported;
	}

	/**
	 * Returns directory for storing report files using
	 * {@link SebContext#saveFile(File, String, String)} and similar methods.
	 * 
	 * @return The report directory.
	 */
	public File getReportDir() {
		return reportDir;
	}

	/**
	 * Is switching to alerts supported.
	 * 
	 * @return Support alert status
	 */
	public boolean isAlertSupported() {
		return alertSupported;
	}

	/**
	 * Basic log level.
	 * 
	 * @return The log level
	 */
	public Level getLogLevel() {
		return logLevel;
	}

	/**
	 * Returns utils instance.
	 * 
	 * @return The utils instance
	 */
	@Override
	public SebUtils getUtils() {
		return utils;
	}

	public SebElementLoader getElementLoader() {
		return elementLoader;
	}

	public JavascriptLibrary getJavascriptLibrary() {
		return javascriptLibrary;
	}

	public ElementLocator createElementLocator(SearchContext searchContext, Field field) {
		return new DefaultElementLocator(searchContext, field);
	}

	public ElementLocator createElementLocator(SearchContext searchContext, By by) {
		return createElementLocator(searchContext, by, false);
	}

	public ElementLocator createElementLocator(SearchContext searchContext, By by, boolean lookupCached) {
		return new DefaultElementLocator(searchContext, new AbstractAnnotations() {
			@Override
			public boolean isLookupCached() {
				return lookupCached;
			}

			@Override
			public By buildBy() {
				return by;
			}
		});
	}

	/**
	 * Quits Seb and wrapped {@link WebDriver}.
	 */
	public void quit() {
		if (!started)
			return;
		triggerEvent(constructEvent(BeforeSebQuitEvent.class));
		if (driver != null) {
			triggerEvent(constructEvent(BeforeDriverQuitEvent.class));
			driver.quit();
			triggerEvent(constructEvent(AfterDriverQuitEvent.class));
		}
		triggerEvent(constructEvent(AfterSebQuitEvent.class));
	}

	/**
	 * Sets label using enclosing method class name and method name.
	 */
	public Seb useEnclosingMethodLabel() {
		final StackTraceElement e = Thread.currentThread().getStackTrace()[2];
		final String s = e.getClassName();
		setLabel(s.substring(s.lastIndexOf('.') + 1, s.length()), e.getMethodName());
		return this;
	}

	/**
	 * Triggers {@link OnReportEvent} with given context and label.
	 * 
	 * @param context
	 *            The report context
	 * @param label
	 *            The report label
	 */
	public void report(SebContext context, String label) {
		triggerEvent(constructEvent(OnReportEvent.class, context).with(label));
	}

	/**
	 * Triggers {@link LogEvent} with given context, level and message.
	 * 
	 * @param context
	 *            The log context
	 * @param level
	 *            The log level
	 * @param message
	 *            The log message
	 */
	public void log(SebContext context, Level level, String message) {
		triggerEvent(constructEvent(LogEvent.class, context).with(level, message));
	}

	/**
	 * Triggers {@link LogEvent} with given context, level and throwable.
	 * 
	 * @param context
	 *            The log context
	 * @param level
	 *            The log level
	 * @param throwable
	 *            The log throwable
	 */
	public void log(SebContext context, Level level, Throwable throwable) {
		triggerEvent(constructEvent(LogEvent.class, context).with(level, throwable));
	}

	/**
	 * Triggers {@link LogEvent} with given context, level, message and
	 * throwable.
	 * 
	 * @param context
	 *            The log context
	 * @param level
	 *            The log level
	 * @param message
	 *            The log message
	 * @param throwable
	 *            The log throwable
	 */
	public void log(SebContext context, Level level, String message, Throwable throwable) {
		triggerEvent(constructEvent(LogEvent.class, context).with(level, message, throwable));
	}

	/**
	 * Constructs a new instance of {@link SebEvent} subclass with given context
	 * and local time.
	 * 
	 * @param eventCls
	 *            The event class to construct
	 * @param context
	 *            The context to use
	 * @return The event instance
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T extends SebEvent> T constructEvent(Class<T> eventCls, SebContext context) {
		try {
			return (T) eventCls.getConstructor().newInstance().with(context, LocalDateTime.now());
		} catch (Exception e) {
			throw new EventConstructException("Unable to construct event " + eventCls.getName(), e);
		}
	}

	@Override
	public SebContext getContext() {
		return this;
	}

	@Override
	public Seb getSeb() {
		return this;
	}

	@Override
	public SebConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getConfiguration(Class<T> configuration) {
		return (T) this.configuration;
	}

	@Override
	public WebDriver getDriver() {
		if (lazyDriver && driver == null) {
			initDriver();
		}
		return driver;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getDriver(Class<T> driver) {
		return (T) getDriver();
	}

	@Override
	public Page getPage() {
		return page;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getPage(Class<T> page) {
		return (T) this.page;
	}

	@Override
	public void setPage(Page page) {
		this.page = page;
	}

	@Override
	public boolean isAt(Class<?> page) {
		return this.page != null && page.isAssignableFrom(this.page.getClass());
	}

	@Override
	public void assertAt(Class<?> page) {
		if (!isAt(page))
			throw new AssertionError("Page " + page + " is not assignable to actual page "
					+ (this.page == null ? null : this.page.getClass()));
	}

	@Override
	public <T extends Page> T goToSafely(Class<T> page) {
		try {
			return goTo(page);
		} catch (WebDriverException e) {
			log(Level.INFO, "Unable to SAFELY go to page " + page, e);
			return null;
		}
	}

	@Override
	public <T extends Page> T goToSafely(T page) {
		try {
			return goTo(page);
		} catch (WebDriverException e) {
			log(Level.INFO, "Unable to SAFELY go to page " + page, e);
			return null;
		}
	}

	@Override
	public <T extends Page> T initPageSafely(Class<T> page) {
		try {
			return initPage(page);
		} catch (WebDriverException e) {
			log(Level.INFO, "Unable to SAFELY init page " + page, e);
			return null;
		}
	}

	@Override
	public <T extends Page> T initPageSafely(T page) {
		try {
			return initPage(page);
		} catch (WebDriverException e) {
			log(Level.INFO, "Unable to SAFELY init page " + page, e);
			return null;
		}
	}

	@Override
	public Page initOnePageSafely(Object... pages) {
		try {
			return initOnePage(pages);
		} catch (WebDriverException e) {
			log(Level.INFO, "Unable to SAFELY init any of given pages " + String.join(", ",
					Arrays.asList(pages).stream().map(p -> p.toString()).collect(Collectors.toList())), e);
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Page> T goTo(Class<T> page) {
		return (T) constructPage(page).goTo();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Page> T goTo(T page) {
		return (T) page.goTo();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Page> T initPage(Class<T> page) {
		return (T) constructPage(page).init();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Page> T initPage(T page) {
		return (T) page.init();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Page initOnePage(Object... pages) {
		Page verifiedPage = null;
		for (Object page : pages) {
			if (page instanceof Page) {
				verifiedPage = initPageSafely((Page) page);
			} else {
				verifiedPage = initPageSafely((Class<? extends Page>) page);
			}
			if (verifiedPage != null)
				return verifiedPage;
		}
		throw new VerificationException("Unable to init any of given pages "
				+ String.join(", ", Arrays.asList(pages).stream().map(p -> p.toString()).collect(Collectors.toList())));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Page> T constructPage(Class<T> page) {
		try {
			Constructor<T> ctor = page.getConstructor();
			return (T) ctor.newInstance().with(this);
		} catch (Exception e) {
			throw new PageConstructException("Unable to construct page " + page.getName(), e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends SebElement> T initSebElement(T element) {
		return (T) element.init();
	}

	public <T extends SebElement> T initSebElement(Class<T> element, SebContext context, WebElement webElement,
			boolean optional) {
		T sebElement = (T) constructSebElement(element, context, webElement, optional);
		if (!optional)
			sebElement.init();
		return sebElement;
	}

	@SuppressWarnings("unchecked")
	public <T extends SebElement> T constructSebElement(Class<T> element, SebContext context, WebElement webElement,
			boolean optional) {
		try {
			if (optional) {
				element = (Class<T>) new ByteBuddy().subclass(element).method(ElementMatchers.isPublic())
						.intercept(MethodDelegation.to(SebElementInterceptor.class).andThen(SuperMethodCall.INSTANCE))
						.make().load(context.getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
			}
			Constructor<T> ctor = element.getConstructor();
			return (T) ctor.newInstance().with(context, webElement, optional);
		} catch (Exception e) {
			throw new SebElementConstructException("Unable to construct module " + element.getName(), e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Logic> T initLogic(T logic) {
		return (T) logic.init();
	}

	@SuppressWarnings("unchecked")
	public <T extends Logic> T initLogic(Class<T> logic, SebContext context) {
		return (T) constructLogic(logic, context).init();
	}

	@SuppressWarnings("unchecked")
	public <T extends Logic> T constructLogic(Class<T> logic, SebContext context) {
		try {
			Constructor<T> ctor = logic.getConstructor();
			return (T) ctor.newInstance().with(context);
		} catch (Exception e) {
			throw new LogicConstructException("Unable to construct logic " + logic.getName(), e);
		}
	}

	public void initElements(SebContext context) {
		PageFactory.initElements(new SebFieldDecorator(context), context);
	}

	public SebAlert getAlert(SebContext context) {
		return new SebAlert(context);
	}
	
	@Override
	public void refreshPage() {
		driver.navigate().refresh();
	}

	@Override
	public void checkIfPresent(WebElement element) throws NoSuchElementException {
		if (element == null)
			throw new NoSuchElementException("Element is null");
		element.isDisplayed();
	}

	@Override
	public boolean isPresent(WebElement element) {
		try {
			checkIfPresent(element);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	@Override
	public boolean isNotPresent(WebElement element) {
		return !isPresent(element);
	}

	@Override
	public List<WebElement> findElements(By by) {
		return getDriver().findElements(by);
	}

	@Override
	public WebElement findElement(By by) {
		return getDriver().findElement(by);
	}

	@Override
	public <T extends SebElement> List<T> find(SebContext context, By by, Class<T> elementCls) {
		return elementLoader.find(context, by, elementCls);
	}

	@Override
	public <T extends SebElement> T findOne(SebContext context, By by, Class<T> elementCls, boolean optional) {
		return elementLoader.findOne(context, by, elementCls, optional);
	}

	@Override
	public void goToUrl(String url) {
		getDriver().get(url);
	}

	@Override
	public void triggerEvent(SebEvent event) {
		event.init();
		listeners.forEach(l -> event.notifyEnabled(l));
	}

	@Override
	public Map<String, Object> getDataHolder() {
		return dataHolder;
	}

	@Override
	public String getProperty(String key) {
		return configuration instanceof PropertySource ? ((PropertySource) configuration).getProperty(key) : null;
	}

	@Override
	public Path saveFile(String content, String name, String extension) {
		return saveFile(content.getBytes(), name, extension);
	}

	@Override
	public Path saveFile(byte[] bytes, String name, String extension) {
		if (!reported)
			return null;
		try {
			Path uniquePath = getUniqueFilePath(name, extension);
			Files.createDirectories(uniquePath.getParent());
			Path path = Files.write(uniquePath, bytes);
			triggerEvent(constructEvent(OnFileSaveEvent.class, this).with(path.toFile()));
			return path;
		} catch (IOException e) {
			throw new SebException("Unable to save file " + name, e);
		}
	}

	@Override
	public Path saveFile(File file, String name, String extension) {
		if (!reported)
			return null;
		try {
			Path uniquePath = getUniqueFilePath(name, extension);
			Files.createDirectories(uniquePath.getParent());
			Path path = Files.copy(file.toPath(), uniquePath);
			triggerEvent(constructEvent(OnFileSaveEvent.class, this).with(path.toFile()));
			return path;
		} catch (IOException e) {
			throw new SebException("Unable to save file " + name, e);
		}
	}

	protected Path getFilePath(String name, String extension) {
		return utils.getFilePath(reportDir, name, extension);
	}

	protected Path getUniqueFilePath(String name, String extension) {
		return utils.getUniqueFilePath(reportDir, name, extension);
	}

}
