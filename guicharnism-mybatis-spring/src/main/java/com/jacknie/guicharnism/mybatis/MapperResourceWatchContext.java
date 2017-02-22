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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;

import com.jacknie.guicharnism.mybatis.support.NioMapperResourceWatcherFactory;

public class MapperResourceWatchContext {

	private final MultiValueMap<String, Resource> resourceMap = new LinkedMultiValueMap<String, Resource>();
	private final PathMatcher pathMatcher = new AntPathMatcher();
	
	private String realoadTargetFilePattern;
	
	public MapperResourceWatcherFactory resolveFactory() {
		//TODO: 1.6 환경 개발자 고려 VFS Watcher 개발 예정
		return new NioMapperResourceWatcherFactory();
	}
	
	public boolean isAlreadyWatched(Resource directory) throws IOException {
		String path = directory.getFile().getAbsolutePath();
		return resourceMap.containsKey(path);
	}

	public void setRealoadTargetFilePattern(String realoadTargetFilePattern) {
		if (pathMatcher.isPattern(realoadTargetFilePattern)) {
			this.realoadTargetFilePattern = realoadTargetFilePattern;
		}
		else {
			throw new IllegalArgumentException("\"" + realoadTargetFilePattern + "\" is not ant pattern.");
		}
	}
	
	public boolean isMatchedFileName(String fileName) {
		return pathMatcher.match(realoadTargetFilePattern, fileName);
	}
	
	public void addTargetDirectory(File watchTargetDirectory) throws FileNotFoundException {
		
		if (!watchTargetDirectory.exists()) {
			throw new FileNotFoundException(watchTargetDirectory.getAbsolutePath());
		}
		if (!watchTargetDirectory.isDirectory()) {
			throw new IllegalArgumentException("File object is not referenced directory.");
		}
		
		File[] children = watchTargetDirectory.listFiles(new FileFilter() {

			@Override
			public boolean accept(File file) {
				return file.isFile() && isMatchedFileName(file.getName());
			}
			
		});
		if (children != null) {
			for (File child : children) {
				Resource resource = new FileSystemResource(child);
				String directory = watchTargetDirectory.getAbsolutePath();
				resourceMap.add(directory, resource);
			}
		}
	}
	
	public List<Resource> getResources(String directory) {
		return resourceMap.get(directory);
	}
}