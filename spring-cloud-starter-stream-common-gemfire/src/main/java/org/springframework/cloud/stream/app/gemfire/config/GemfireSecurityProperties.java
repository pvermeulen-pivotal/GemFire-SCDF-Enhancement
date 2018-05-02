/*
 * Copyright 2017 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.springframework.cloud.stream.app.gemfire.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.UUID;

import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.distributed.DistributedMember;
import com.gemstone.gemfire.security.AuthInitialize;
import com.gemstone.gemfire.security.AuthenticationFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("gemfire.security")
public class GemfireSecurityProperties {
	private static final String CLASSPATH = "classpath:";
	private static Logger logger = LoggerFactory.getLogger(GemfireSecurityProperties.class);

	/**
	 * The cache username.
	 */
	private String username;

	/**
	 * The cache password.
	 */
	private String password;
	
	private String gemfireProperties;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Properties getGemfireProperties() {
		return buildGemfireProperties();
	}
	
	private Properties buildGemfireProperties() {
		Properties properties = new Properties();		
		if (StringUtils.hasText(this.gemfireProperties)) {
			logger.info("Parse GemFire Properties: " + this.gemfireProperties);
			String[] secProps = this.gemfireProperties.split(",");
			if (secProps != null && secProps.length > 0) {
				for (String secProp : secProps) {
					String[] props = secProp.split("=");
					if (props != null && props.length == 2) {
						if (props[1].toLowerCase().startsWith(CLASSPATH)) {
							InputStream is = GemfireSecurityProperties.class.getClassLoader()
									.getResourceAsStream(props[1].substring(CLASSPATH.length()));
							File f = new File("file" + UUID.randomUUID().toString().replaceAll("-", ""));
							OutputStream os;
							try {
								os = new FileOutputStream(f);
								org.apache.commons.io.IOUtils.copy(is, os);
								os.close();
								logger.info("Adding GemFire Property: " + props[0] + "=" + f.getAbsolutePath());
								properties.put(props[0], f.getAbsolutePath());
							} catch (Exception e) {
								logger.error("Exception processing GemFire property " + props[0] + "=" + props[1]);
							}
						} else {
							logger.info("Adding GemFire Property: " + props[0] + "=" + props[1]);
							properties.put(props[0], props[1]);
						}
					}
				}
			}
		}
		return properties;
		
	}

	public void setGemfireProperties(String gemfireProperties) {
		this.gemfireProperties = gemfireProperties;
	}

	@SuppressWarnings("unused")
	public static class UserAuthInitialize implements AuthInitialize {

		private static final String USERNAME = "security-username";
		private static final String PASSWORD = "security-password";

		private LogWriter securitylog;
		private LogWriter systemlog;

		public static AuthInitialize create() {
			return new UserAuthInitialize();
		}

		@Override
		public void init(LogWriter systemLogger, LogWriter securityLogger) throws AuthenticationFailedException {
			this.systemlog = systemLogger;
			this.securitylog = securityLogger;
		}

		@Override
		public Properties getCredentials(Properties props, DistributedMember server, boolean isPeer) throws AuthenticationFailedException {

			String username = props.getProperty(USERNAME);
			if (username == null) {
				throw new AuthenticationFailedException("UserAuthInitialize: username not set.");
			}

			String password = props.getProperty(PASSWORD);
			if (password == null) {
				throw new AuthenticationFailedException("UserAuthInitialize: password not set.");
			}

			Properties properties = new Properties();
			properties.setProperty(USERNAME, username);
			properties.setProperty(PASSWORD, password);
			return properties;
		}

		@Override
		public void close() {
		}
	}
}
