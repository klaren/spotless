/*
 * Copyright 2016-2021 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.markdown;

import static com.diffplug.spotless.markdown.LibMarkdownPreconditions.requireKeysAndValuesNonNull;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.ThrowingEx.Supplier;

/** A step for <a href="https://github.com/diffplug/freshmark">FreshMark</a>. */
public class FreshMarkStep {
	// prevent direct instantiation
	private FreshMarkStep() {}

	private static final String DEFAULT_VERSION = "1.3.1";
	private static final String NAME = "freshmark";
	private static final String MAVEN_COORDINATE = "com.diffplug.freshmark:freshmark:";
	private static final String FORMATTER_CLASS = "com.diffplug.freshmark.FreshMark";
	private static final String FORMATTER_METHOD = "compile";

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(Supplier<Map<String, ?>> properties, Provisioner provisioner) {
		return create(defaultVersion(), properties, provisioner);
	}

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(String version, Supplier<Map<String, ?>> properties, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(properties, "properties");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(JarState.from(MAVEN_COORDINATE + version, provisioner), properties.get()),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the formatter. */
		final JarState jarState;
		final NavigableMap<String, ?> properties;

		State(JarState jarState, Map<String, ?> properties) {
			this.jarState = jarState;
			// because equality is computed based on serialization, it's important to order the properties
			// before writing them
			this.properties = new TreeMap<>(properties);
			requireKeysAndValuesNonNull(this.properties);
		}

		FormatterFunc createFormat() throws Exception {
			Logger logger = Logger.getLogger(FreshMarkStep.class.getName());
			Consumer<String> loggingStream = logger::warning;

			ClassLoader classLoader = jarState.getClassLoader();

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor(Map.class, Consumer.class).newInstance(properties, loggingStream);
			Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
			return input -> (String) method.invoke(formatter, input);
		}
	}
}
