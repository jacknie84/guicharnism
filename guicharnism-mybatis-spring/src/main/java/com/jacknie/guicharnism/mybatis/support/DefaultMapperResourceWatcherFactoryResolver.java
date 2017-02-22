/**
 * Created by jacknie, 2017. 2. 22.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.jacknie.guicharnism.mybatis.support;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactory;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactoryResolver;

/**
 * @author jacknie
 *
 */
public class DefaultMapperResourceWatcherFactoryResolver implements MapperResourceWatcherFactoryResolver {
	
	private final Class<?>[] builtInWatcherFactories = { NioMapperResourceWatcherFactory.class };

	@Override
	public MapperResourceWatcherFactory resolveFactory(Map<String, Object> argumentMap) {
		
		Assert.notEmpty(argumentMap);
		
		for (Class<?> factoryClass : builtInWatcherFactories) {
			for (Entry<String, Object> entry : argumentMap.entrySet()) {
				PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(factoryClass, entry.getKey());
				if (pd != null) {
					Method setter = pd.getWriteMethod();
					if (setter != null) {
						try {
							Object target = factoryClass.newInstance();
							setter.invoke(target, entry.getValue());
							return (MapperResourceWatcherFactory) target;
						} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new IllegalStateException(e);
						}
					}
				}
			}
		}
		return null;
	}

}
