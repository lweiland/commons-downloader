package commonsdl;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.kohsuke.args4j.Option;

class Download {
	@Option(name = "--folder", required = false, usage = "Folder to Commons file lists", metaVar ="/path/to/folder")
	private String folder;
		
	@Option(name = "--file", required = false, usage = "Commons file list", metaVar = "/path/to/file")
	private String file;
	
	@Option(name = "--charset", required = false, usage = "Charset used in file list", metaVar = "CHARSET")
	private String charset;
	
	@Option(name = "--destination", required = false, usage = "Destination directory", metaVar = "/path/to/dir")
	private String destination;
	
	@Option(name = "--mode", required = false, usage = "Mode")
	
	private Mode mode;
	
	public Download() {
		this.charset = "UTF-8";
		this.destination = ".";
		this.mode = Mode.RESUME;
		
	}
	
	public Path getFolder() {
		return Paths.get(folder);
	}
	
	public boolean folderSet(){
		if (folder!=null)
			return true;
		else
			return false;
	}
	
	public Path getFile() {
		return Paths.get(file);
	}
	
	
	public Path getDestination() {
		return Paths.get(destination);
	}
	
	public Charset getCharset() {
		return Charset.forName(charset);
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public enum Mode {
		RESUME,
		RESTART
	}
	
	public enum Type{
		FILE,
		FOLDER
	}
}
