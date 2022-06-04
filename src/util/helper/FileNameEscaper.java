package util.helper;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import util.commonUtil.ComFileUtil;
import util.commonUtil.ComStrUtil;
import util.commonUtil.model.FileInfo;

/**
 * escape file name to avoid encoding problem. And recover to original file name if needed.
 * @author Administrator
 *
 */
public class FileNameEscaper {
	/**
	 * a map to track the original file and escaped file.
	 */
	private Map<File, File> before_after_map = new HashMap<File, File>();

	/**
	 * how many random char needed to be created for escapedFile name.
	 */
	private final static int randomCharNumber = 9;

	/**
	 * escape file with originalFile's extension.
	 * @param originalFile
	 * @param needToRenameOriginalFile whether need to rename original File to escapedFile now. true: need to rename. false: won't rename.
	 * @return escapedFile who shared a same extension with originalFile.
	 * @throws Exception
	 */
	public File escape(File originalFile, boolean needToRenameOriginalFile) throws Exception {
		return escape(originalFile, needToRenameOriginalFile, ComFileUtil.getFileExtension(originalFile, true));
	}

	/**
	 * escape file with given extension.
	 * @param originalFile
	 * @param needToRenameOriginalFile whether need to rename original File to escapedFile now. true: need to rename. false: won't rename.
	 * @param extension the expected extension for escapedFile
	 * @return escapedFile with given extension.
	 * @throws Exception
	 */
	public File escape(File originalFile, boolean needToRenameOriginalFile, String extension) throws Exception {
		FileInfo fileInfo = ComFileUtil.getFileInfo(originalFile);
		File escapedFile = new File(fileInfo.getDir() + "escapedFile_" + ComStrUtil.getRandomNumString(randomCharNumber) + FileInfo.getFileExtensionWithDot(extension));
		if (needToRenameOriginalFile) {
			if (!originalFile.renameTo(escapedFile)) throw new Exception("escape file " + originalFile.getPath() + " to " + escapedFile.getPath()+ " failed");
		}
		before_after_map.put(originalFile, escapedFile);
		return escapedFile;
	}

	/**
	 * rename escapedFile back to originalFile.
	 */
	public void recoverAll() {
		Iterator<File> it = before_after_map.keySet().iterator();
		while(it.hasNext()) {
			File originalFile = it.next();
			File escapedFile = before_after_map.get(originalFile);
			if(escapedFile != null && escapedFile.exists()) escapedFile.renameTo(originalFile);
		}
	}

}
