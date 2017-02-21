package com.jacknie.guicharnism.mybatis;

import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class MapperResourceWatchContext {

	private final MultiValueMap<String, Resource> resourceMap = new LinkedMultiValueMap<String, Resource>();
	
	public MapperResourceWatcherFactory resolveFactory(Map<String, Object> parameterMap) {
		return null;
	}
	
	public MapperResourceWatcherFactory resolveFactory(String name, Object value) {
		String realoadTargetFilePattern = value.toString();
		NioMapperResourceWatcherFactory factory = new NioMapperResourceWatcherFactory();
		factory.setRealoadTargetFilePattern(realoadTargetFilePattern);
		return factory;
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