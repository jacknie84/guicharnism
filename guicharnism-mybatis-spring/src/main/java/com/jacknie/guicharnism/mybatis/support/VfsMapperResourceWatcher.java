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

import java.io.File;
import java.io.IOException;

import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.ResourceUtils;

import com.jacknie.guicharnism.mybatis.FileMapperResourceWatcher;
import com.jacknie.guicharnism.mybatis.MapperResourceWatchContext;

/**
 * @author jacknie
 *
 */
public class VfsMapperResourceWatcher extends FileMapperResourceWatcher {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Object mutex = new Object();
	private final FileListener fileListener = new MapperResourceFileListener();
	private final FileSystemManager vfsManager;
	
	private volatile File modifiedFile;
	
	public VfsMapperResourceWatcher(MapperResourceWatchContext watchContext, Resource watchTarget) {
		super(watchContext, watchTarget);
		try {
			this.vfsManager = VFS.getManager();
		} catch (FileSystemException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	protected File receiveModification(File watchTargetDirectory) throws IOException, InterruptedException {
		synchronized (mutex) {
			FileObject folder = vfsManager.resolveFile(watchTargetDirectory.toURI());
			DefaultFileMonitor fileMonitor = new DefaultFileMonitor(fileListener);
			fileMonitor.setRecursive(true);
			fileMonitor.addFile(folder);
			logger.debug("VFS file monitor start. [target: {}]", folder);
			fileMonitor.start();
			mutex.wait();
			fileMonitor.stop();
			logger.debug("VFS file monitor stop. [target: {}]", folder);
			
			try {
				return modifiedFile;
			} finally {
				modifiedFile = null;
			}
		}
	}
	
	private class MapperResourceFileListener implements FileListener {

		@Override
		public void fileCreated(FileChangeEvent event) throws Exception {
			//do nothing...
		}

		@Override
		public void fileDeleted(FileChangeEvent event) throws Exception {
			//do nothing...
		}

		@Override
		public void fileChanged(FileChangeEvent event) throws Exception {
			synchronized (mutex) {
				FileObject changedFile = event.getFile();
				File modifiedFile = ResourceUtils.getFile(changedFile.getURL());
				VfsMapperResourceWatcher.this.modifiedFile = modifiedFile;
				mutex.notifyAll();
			}
		}
		
	}

}
