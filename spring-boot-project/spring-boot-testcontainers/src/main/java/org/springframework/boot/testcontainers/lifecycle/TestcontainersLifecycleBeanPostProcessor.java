/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.testcontainers.lifecycle;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link BeanPostProcessor} to manage the lifecycle of {@link Startable startable
 * containers}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @see TestcontainersLifecycleApplicationContextInitializer
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class TestcontainersLifecycleBeanPostProcessor implements DestructionAwareBeanPostProcessor {

	private final ConfigurableListableBeanFactory beanFactory;

	TestcontainersLifecycleBeanPostProcessor(ConfigurableListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Startable startable) {
			startable.start();
		}
		return bean;
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof Startable;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (bean instanceof Startable startable && !isDestroyedByFramework(beanName) && !isReusedContainer(bean)) {
			startable.close();
		}
	}

	private boolean isDestroyedByFramework(String beanName) {
		try {
			BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(beanName);
			String destroyMethodName = beanDefinition.getDestroyMethodName();
			return !"".equals(destroyMethodName);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	private boolean isReusedContainer(Object bean) {
		return (bean instanceof GenericContainer<?> container) && container.isShouldBeReused();
	}

}
