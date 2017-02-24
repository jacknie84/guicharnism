/**
 * Created by jacknie, 2017. 2. 24.
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

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import com.jacknie.guicharnism.mybatis.MapperResourceWatchContext;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactory;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactoryNotFoundException;
import com.jacknie.guicharnism.mybatis.MapperResourceWatcherFactoryResolver;

/**
 * @author jacknie
 *
 */
public class DefaultMapperResourceWatcherFactoryResolver implements MapperResourceWatcherFactoryResolver {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final String[] targetClasses = {
			"com.jacknie.guicharnism.mybatis.support.NioMapperResourceWatcherFactory", 
			"com.jacknie.guicharnism.mybatis.support.VfsMapperResourceWatcherFactory"
	};
	
	@Override
	public MapperResourceWatcherFactory resolveFactory(MapperResourceWatchContext watchContext) throws MapperResourceWatcherFactoryNotFoundException {
		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
		try {
			for (String className : targetClasses) {
				if (ClassUtils.isPresent(className, classLoader)) {
					logger.debug("[{}] factory instance loading...", className);
					Class<?> clazz = classLoader.loadClass(className);
					Constructor<?> constructor = clazz.getConstructor(watchContext.getClass());
					Object instance = constructor.newInstance(watchContext);
					return (MapperResourceWatcherFactory) instance;
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		throw new MapperResourceWatcherFactoryNotFoundException();
	}
}
