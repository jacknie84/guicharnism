/**
 * Created by jacknie, 2017. 2. 21.
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
package com.jacknie.guicharnism.mybatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.jacknie.guicharnism.mybatis.support.DefaultMapperResourceWatcherFactoryResolver;

public class MapperResourceWatchContext {

	private final MultiValueMap<String, Resource> resourceMap = new LinkedMultiValueMap<String, Resource>();
	private final MapperResourceWatcherFactoryResolver factoryResolver;
	
	public MapperResourceWatchContext() {
		this(new DefaultMapperResourceWatcherFactoryResolver());
	}

	public MapperResourceWatchContext(MapperResourceWatcherFactoryResolver factoryResolver) {
		Assert.notNull(factoryResolver);
		this.factoryResolver = factoryResolver;
	}

	public MapperResourceWatcherFactory resolveFactory(Map<String, Object> argumentMap) {
		MapperResourceWatcherFactory factory = factoryResolver.resolveFactory(argumentMap);
		if (factory == null) {
			throw new IllegalStateException("not exists implemented factory.");
		}
		return factory;
	}
	
	public MapperResourceWatcherFactory resolveFactory(String name, Object value) {
		Map<String, Object> argumentMap = new HashMap<String, Object>();
		argumentMap.put(name, value);
		return resolveFactory(argumentMap);
	}
	
	public void addResource(String directory, Resource targetResource) {
		resourceMap.add(directory, targetResource);
	}
	
	public List<Resource> getResources(String directory) {
		return resourceMap.get(directory);
	}
	
	public boolean isAlreadyWatched(String directory) {
		return resourceMap.containsKey(directory);
	}
}