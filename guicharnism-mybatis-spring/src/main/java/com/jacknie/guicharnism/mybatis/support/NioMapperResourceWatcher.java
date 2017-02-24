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
package com.jacknie.guicharnism.mybatis.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;

import org.springframework.core.io.Resource;

import com.jacknie.guicharnism.mybatis.AbstractMapperResourceWatcher;
import com.jacknie.guicharnism.mybatis.MapperResourceWatchContext;

public class NioMapperResourceWatcher extends AbstractMapperResourceWatcher {

	/**
	 * @param watchContext
	 * @param configuration
	 */
	public NioMapperResourceWatcher(MapperResourceWatchContext watchContext, Resource watchTarget) {
		super(watchContext, watchTarget);
	}

	@Override
	protected File receiveModification(File watchTargetDirectory) throws IOException, InterruptedException {
		Path path = Paths.get(watchTargetDirectory.toURI());
		WatchService watchService = path.getFileSystem().newWatchService();
		path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
		WatchKey watchKey = watchService.take();
		List<WatchEvent<?>> events = watchKey.pollEvents();
		for (WatchEvent<?> event : events) {
			if (StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind())) {
				Path eventPath = (Path) event.context();
				return path.resolve(eventPath).toFile();
			}
		}
		return null;
	}
}
