package com.jacknie.guicharnism.mybatis;

import java.io.File;
import java.io.IOException;

public interface MapperResourceWatcher {

	boolean isTargetFileName(String fileName);
	
	void watch(File watchTargetDirectory) throws IOException;
}
