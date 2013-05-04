/**
 * Copyright 2012-2013 eBay Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.ebay.web.cors;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ebay.web.cors.handlers.CORSHandler;
import com.ebay.web.cors.handlers.DefaultInvalidCORSHandler;
import com.ebay.web.cors.handlers.DefaultNonCORSHandler;
import com.ebay.web.cors.handlers.DefaultPreflightCORSHandler;
import com.ebay.web.cors.handlers.DefaultSimpleCORSHandler;

/**
 * <p>
 * A servlet filter to support CORS (Cross-Origin Resource Sharing).
 * </p>
 * 
 * <p>
 * An incoming HTTP request is intercepted, and is inspected as per CORS spec,
 * and with help of {@link CORSConfiguration}. Appropriate, response headers (as
 * per spec) are added, if required.
 * </p>
 * 
 * @author <a href="mailto:mosoni@ebay.com">Mohit Soni</a>
 * @see <a href="http://www.w3.org/TR/cors/">CORS spec</a>
 * 
 */
public class CORSFilter implements Filter {
	/** Request handler for a simple CORS request. */
	private CORSHandler simpleRequestHandler;

	/** Request handler for a pre-flight CORS request. */
	private CORSHandler preFlightRequestHandler;

	/** Request handler for a normal request that's not a CORS request. */
	private CORSHandler nonCORSRequestHandler;

	/** Request handler for a CORS request, that's not valid. */
	private CORSHandler invalidCORSRequestHandler;

	/** Configuration object */
	private CORSConfiguration corsConfiguration;

	/**
	 * Creates a CORS filter, and loads configuration from classpath, by default
	 * from cors-configuration.properties.
	 * 
	 * @throws IOException
	 */
	public CORSFilter() {
		super();
	}

	public CORSFilter(CORSConfiguration corsConfiguration) {
		super();
		this.corsConfiguration = corsConfiguration;
		assignDefaultHandlers();
	}

	public void doFilter(final ServletRequest servletRequest,
			final ServletResponse servletResponse, final FilterChain filterChain)
			throws IOException, ServletException {
		if (!(servletRequest instanceof HttpServletRequest)
				|| !(servletResponse instanceof HttpServletResponse)) {
			String message = "CORS doesn't support non-HTTP request or response.";
			throw new ServletException(message);
		}

		// Safe to downcast at this point.
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		// Determines the CORS request type.
		CORSRequestType requestType = CORSRequestType.checkRequestType(request,
				corsConfiguration);

		// Adds CORS specific attributes to request.
		CORSRequestDecorator.decorateCORSProperties(request, requestType);

		switch (requestType) {
		case SIMPLE:
			this.simpleRequestHandler.handle(request, response, filterChain);
			break;
		case PRE_FLIGHT:
			this.preFlightRequestHandler.handle(request, response, filterChain);
			break;
		case NOT_CORS:
			this.nonCORSRequestHandler.handle(request, response, filterChain);
			break;
		default:
			this.invalidCORSRequestHandler.handle(request, response,
					filterChain);
			break;
		}
	}

	public void init(final FilterConfig filterConfig) throws ServletException {
		if (filterConfig != null) {
			try {
				this.corsConfiguration = CORSConfiguration
						.loadFromFilterConfig(filterConfig);
			} catch (Exception e) {
				throw new ServletException(
						"Error loading configuration using filter init", e);
			}
		} else {
			try {
				this.corsConfiguration = CORSConfiguration.loadDefault();
			} catch (IOException e) {
				throw new ServletException(
						"Error loading configuration from cors-configuration.properties file", e);
			}
		}
		assignDefaultHandlers();
	}

	public void destroy() {

	}

	/**
	 * Set a {@link CORSHandler} to handle Simple CORS request.
	 * 
	 * @param simpleRequestHandler
	 *            A handler implementing {@link CORSHandler}.
	 */
	public void setSimpleRequestHandler(final CORSHandler simpleRequestHandler) {
		this.simpleRequestHandler = simpleRequestHandler;
	}

	/**
	 * Set a {@link CORSHandler} to handle pre-flight CORS request.
	 * 
	 * @param preFlightRequestHandler
	 *            A handler implementing {@link CORSHandler}.
	 */
	public void setPreFlightRequestHandler(
			final CORSHandler preFlightRequestHandler) {
		this.preFlightRequestHandler = preFlightRequestHandler;
	}

	/**
	 * Set a {@link CORSHandler} to handle a normal request that's not a CORS
	 * request.
	 * 
	 * @param nonCORSRequestHandler
	 *            A handler implementing {@link CORSHandler}.
	 */
	public void setNonCORSRequestHandler(final CORSHandler nonCORSRequestHandler) {
		this.nonCORSRequestHandler = nonCORSRequestHandler;
	}

	/**
	 * Set a {@link CORSHandler} to handle an invalid CORS request.
	 * 
	 * @param invalidCORSRequestHandler
	 *            A handler implementing {@link CORSHandler}.
	 */
	public void setInvalidCORSRequestHandler(
			final CORSHandler invalidCORSRequestHandler) {
		this.invalidCORSRequestHandler = invalidCORSRequestHandler;
	}

	/**
	 * Set {@link CORSConfiguration} for the filter.
	 * 
	 * @param corsConfiguration
	 *            The configuration object.
	 */
	public void setCorsConfiguration(CORSConfiguration corsConfiguration) {
		this.corsConfiguration = corsConfiguration;
	}

	/**
	 * Assigns default handlers.
	 */
	private void assignDefaultHandlers() {
		this.simpleRequestHandler = new DefaultSimpleCORSHandler(
				corsConfiguration);
		this.preFlightRequestHandler = new DefaultPreflightCORSHandler(
				corsConfiguration);
		this.nonCORSRequestHandler = new DefaultNonCORSHandler();
		this.invalidCORSRequestHandler = new DefaultInvalidCORSHandler();
	}
	
	/**
	 * The Access-Control-Allow-Origin header indicates whether a resource can
	 * be shared based by returning the value of the Origin request header in
	 * the response.
	 */
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

	/**
	 * The Access-Control-Allow-Credentials header indicates whether the
	 * response to request can be exposed when the omit credentials flag is
	 * unset. When part of the response to a preflight request it indicates that
	 * the actual request can include user credentials.
	 */
	public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

	/**
	 * The Access-Control-Expose-Headers header indicates which headers are safe
	 * to expose to the API of a CORS API specification
	 */
	public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";

	/**
	 * The Access-Control-Max-Age header indicates how long the results of a
	 * preflight request can be cached in a preflight result cache.
	 */
	public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";

	/**
	 * The Access-Control-Allow-Methods header indicates, as part of the
	 * response to a preflight request, which methods can be used during the
	 * actual request.
	 */
	public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

	/**
	 * The Access-Control-Allow-Headers header indicates, as part of the
	 * response to a preflight request, which header field names can be used
	 * during the actual request.
	 */
	public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
}
