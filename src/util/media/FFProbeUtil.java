package util.media;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.MediaStream;
import model.SubtitleStream;
import model.VideoDuration;
import model.VideoResolution;
import util.commonUtil.ComCMDUtil;
import util.commonUtil.ComFileUtil;
import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;
import util.commonUtil.CommonUtil;
import util.commonUtil.ConfigManager;
import util.commonUtil.model.FileName;

public class FFProbeUtil {

	public FFProbeUtil() {
		// TODO Auto-generated constructor stub
	}
	
	private static String FFProbePath;
	
	private static boolean isFFprobeReady;
	
	static {
		if(CommonUtil.isInJar()) {
			FFProbePath = new File("ffprobe.exe").getAbsolutePath();
			if(new File(FFProbePath).exists()) {
				ComLogUtil.info("target ffprobe.exe file : " + FFProbePath + " already exists");
			} else {
				ComLogUtil.info("target ffprobe.exe file : " + FFProbePath + " not exists, will copy from jar file");
				try {
					CommonUtil.copyExeOutFrmJar(ComMediaUtil.class.getResource("/resource/ffprobe.exe"), FFProbePath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			// otherwise load from config
			FFProbePath = ConfigManager.getConfigManager(ComMediaUtil.class.getResource("ComMediaUtil.properties")).getString("FFProbePath");
		}
		isFFprobeReady = new File(FFProbePath).exists();
	}
	
	private static MediaStream[] extractStreams(String str) {
		// First,  extract all subtitle strings into groups.
		String[] lines = str.trim().split("\n");
		List<String> groups = new ArrayList<String>();
		String tmpGroup = "";
		Boolean isStreamFound = false;
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String matchedString = ComRegexUtil.getMatchedString(line, "^\\s*Stream #0:\\d+[^:]*: .*");
			if(ComStrUtil.isBlankOrNull(matchedString)) {
				if(isStreamFound) {
					if(ComStrUtil.isBlankOrNull(line.trim())) { // stop extracting when hit empty line
						break;
					}
					tmpGroup += line + '\n';
				} else {
					// do nothing if it's not reached the Stream part yet.
				}
			} else {
				// if it's the beginning Stream tag
				isStreamFound = true;
				if(!ComStrUtil.isBlankOrNull(tmpGroup)) {
					// save the last group
					groups.add(tmpGroup);
				}
				tmpGroup = line + '\n';
			}
		}
		
		// for the last group, we need to add it here manually.
		if(!ComStrUtil.isBlankOrNull(tmpGroup)) {
			groups.add(tmpGroup);
		}
		
		// finally, Converts those subtitle groups into SubtitleStreams
		int size = groups.size();
		
		MediaStream[] retVal = new MediaStream[size];
		for(int i = 0; i < size; i++) {
			String group = groups.get(i);
			retVal[i] = new MediaStream(group);
		}
		return retVal;
	}
	
	public static SubtitleStream[] extractSubtitles(String str) {
		// First,  extract all subtitle strings into groups.
		String[] lines = str.trim().split("\n");
		MediaStream[] extractStreams = extractStreams(str);
		List<SubtitleStream> arrayList = new ArrayList<SubtitleStream>();
		int length = extractStreams.length;
		for(int i = 0; i < length; i++) {
			MediaStream mediaStream = extractStreams[i];
			if(MediaStream.mediaType_subtitle.equalsIgnoreCase(mediaStream.getMediaType())) {
				// (String originalText, String name, int index, Map<String, String> metadata, String mediaType, boolean hasEmbededSubtitleStream) {
				SubtitleStream subtitleStream = new SubtitleStream(mediaStream.getOriginalText(), mediaStream.getName()
						, mediaStream.getIndex(), mediaStream.getMetadata(), mediaStream.isHasEmbededSubtitleStream(), false);
				arrayList.add(subtitleStream);
			} else if(MediaStream.mediaType_video.equalsIgnoreCase(mediaStream.getMediaType()) && mediaStream.isHasEmbededSubtitleStream()) {
				SubtitleStream subtitleStream = new SubtitleStream(mediaStream.getOriginalText(), mediaStream.getName()
						, mediaStream.getIndex(), mediaStream.getMetadata(), mediaStream.isHasEmbededSubtitleStream(), true);
				arrayList.add(subtitleStream);
			}
		}
		SubtitleStream[] retVal = new SubtitleStream[arrayList.size()];
		arrayList.toArray(retVal);
		return retVal;
	}
	
	public static SubtitleStream[] probeSubtitles(String srcFile) throws Exception {
		if(!isFFprobeReady) {
			throw new Exception("Error: FFProbe " + isFFprobeReady + " is not ready!");
		}

		SubtitleStream[] retVal = null; 
		
		// first bak those name.
		String srcFile_ = null;
		String srcFile_dir = null;
//		String srcFile_name = null;
		String srcFile_ext = null;
		try {
			srcFile_dir = ComFileUtil.getFileDiretory(srcFile);
//			srcFile_name = ComFileUtil.getFileName(srcFile, false);
			srcFile_ext = ComFileUtil.getFileExtension(srcFile, true);
			srcFile_ = srcFile_dir + ComStrUtil.getRandomString(32) + srcFile_ext;
		} catch (Exception e1) {
			e1.printStackTrace();
			throw e1;
		}

		if(new File(srcFile).renameTo(new File(srcFile_))) {
			String printedLog;
			try {
				printedLog = ComCMDUtil.runCMD(new StringBuilder(FFProbePath).append(" \"").append(srcFile_).append("\"")
						.toString());
				ComLogUtil.info("probe printedLog" + printedLog);
				
				SubtitleStream[] extractedSubtitles = FFProbeUtil.extractSubtitles(printedLog);
				retVal = extractedSubtitles;
				for(int i = 0; i < extractedSubtitles.length; i++){
					SubtitleStream subtitle = extractedSubtitles[i];
					ComLogUtil.info("subtitle: " + ComLogUtil.objToFormatString(subtitle));
				}
				
				// TODO: add logic to check if succeed.
				if(true) {
					ComLogUtil.info(srcFile + " probeSuccess.");
				} else {
//					ComLogUtil.info(srcFile + " probeFailed. needChk log:" + printedLogs[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				ComLogUtil.info(srcFile + " probeFailed. needChk");
				throw e;
			} finally {
				new File(srcFile_).renameTo(new File(srcFile));
			}
		} else {
			String msg = "rename failed srcFile:" + srcFile;
			ComLogUtil.error(msg);
			throw new Exception(msg);
		}

		return retVal;
	}
	
	/**
	 * refer: https://superuser.com/questions/841235/how-do-i-use-ffmpeg-to-get-the-video-resolution
	 * @param srcFile the source video file
	 * @return VideoResolution
	 * @throws Exception
	 */
	public static VideoResolution probeVideoResolution(String srcFile) throws Exception {
		if(!isFFprobeReady) {
			throw new Exception("Error: FFProbe " + isFFprobeReady + " is not ready!");
		}

		VideoResolution retVal = null; 
		
		// first bak those name.
		String srcFile_ = null;
		String srcFile_dir = null;
//		String srcFile_name = null;
		String srcFile_ext = null;
		try {
			srcFile_dir = ComFileUtil.getFileDiretory(srcFile);
//			srcFile_name = ComFileUtil.getFileName(srcFile, false);
			srcFile_ext = ComFileUtil.getFileExtension(srcFile, true);
			srcFile_ = srcFile_dir + ComStrUtil.getRandomString(32) + srcFile_ext;
		} catch (Exception e1) {
			e1.printStackTrace();
			throw e1;
		}

		if(new File(srcFile).renameTo(new File(srcFile_))) {
			String printedLog = "";
			try {
				printedLog = ComCMDUtil.runCMD(new StringBuilder(FFProbePath).append(" -v error -select_streams v:0 -show_entries stream=width,height -of csv=s=x:p=0 ")
						.append(" \"")
						.append(srcFile_).append("\"")
						.toString());
//				ComLogUtil.info("probe printedLog" + printedLog);
				
				String[] split = printedLog.split("\n");
				String probeRetVal = split[4];
//				ComLogUtil.debug("probe retVal: " + probeRetVal);
				String[] resultArr = probeRetVal.split("x");
				Integer width = Integer.parseInt(resultArr[0].trim(), 10);
				Integer height = Integer.parseInt(resultArr[1].trim(), 10);
				
				retVal = new VideoResolution(width, height);
				
				// TODO: add logic to check if succeed.
				if(true) {
//					ComLogUtil.debug(srcFile + " probeSuccess.");
				} else {
//					ComLogUtil.info(srcFile + " probeFailed. needChk log:" + printedLogs[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				ComLogUtil.info(srcFile + " probeFailed. needChk(maybe this video file is invalid). printedLog: " + printedLog);
				throw e;
			} finally {
				new File(srcFile_).renameTo(new File(srcFile));
			}
		} else {
			String msg = "rename failed srcFile:" + srcFile + " to file: " + srcFile_;
			ComLogUtil.error(msg);
			throw new Exception(msg);
		}

		return retVal;
	}
	
	/**
	 * refer: https://superuser.com/questions/841235/how-do-i-use-ffmpeg-to-get-the-video-resolution
	 * @param srcFile the source video file
	 * @return VideoResolution
	 * @throws Exception
	 */
	private static String probeVideoDurationSexagesimal(String srcFile) throws Exception {
		if(!isFFprobeReady) {
			throw new Exception("Error: FFProbe " + isFFprobeReady + " is not ready!");
		}
		

		String retVal = null; 
		
		// first bak those name.
		String srcFile_dir = null;
//				String srcFile_name = null;
		String srcFile_ext = null;
		FileName fileNameSrc = new FileName(srcFile);
		FileName fileNameTarget = new FileName(srcFile);
		try {
			
			srcFile_dir = fileNameSrc.getDir();
//					srcFile_name = ComFileUtil.getFileName(srcFile, false);
			srcFile_ext = fileNameSrc.getExt();
			
			// we should not use the keep folder of the target Disk, since it should be a virtual Folder
			// which cause the source file and the target file are NOT in the same physical disk, then the rename action would cost a long time.
//			fileNameTarget.setDir(fileNameSrc.getDisk() + "\\keep\\");
			fileNameTarget.setDir(srcFile_dir);
			fileNameTarget.setFileName(ComStrUtil.getRandomString(32));
		} catch (Exception e1) {
			e1.printStackTrace();
			throw e1;
		}

		
		if(fileNameSrc.toFile().renameTo(fileNameTarget.toFile())) {
			String printedLog = "";
			try {
				printedLog = ComCMDUtil.runCMD(
						new StringBuilder("")
						.append(FFProbePath).append(" -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 -sexagesimal ")
						.append(" \"")
						.append(fileNameTarget).append("\"")
						.toString());
//						ComLogUtil.info("probe printedLog" + printedLog);
				
				String[] split = printedLog.split("\n");
				String probeRetVal = split[4]; 
//				ComLogUtil.debug("probe retVal: " + probeRetVal);
				
				retVal = probeRetVal.trim();
				
				// TODO: add logic to check if succeed.
				if(true) {
//					ComLogUtil.debug(srcFile + " probeSuccess.");
				} else {
//							ComLogUtil.info(srcFile + " probeFailed. needChk log:" + printedLogs[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				ComLogUtil.info(srcFile + " probeFailed. needChk(maybe this video file is invalid). printedLog: " + printedLog);
				throw e;
			} finally {
				fileNameTarget.toFile().renameTo(fileNameSrc.toFile());
			}
		} else {
			String msg = "rename failed srcFile:" + srcFile;
			ComLogUtil.error(msg);
			throw new Exception(msg);
		}

		return retVal;
	}
	
	/**
	 * refer: https://superuser.com/questions/841235/how-do-i-use-ffmpeg-to-get-the-video-resolution
	 * @param srcFile the source video file
	 * @return VideoResolution
	 * @throws Exception
	 */
	private static Float probeVideoDurationSec(String srcFile) throws Exception {
		if(!isFFprobeReady) {
			throw new Exception("Error: FFProbe " + isFFprobeReady + " is not ready!");
		}

		Float retVal = null; 
		
		// first bak those name.
		String srcFile_dir = null;
//		String srcFile_name = null;
		String srcFile_ext = null;
		FileName fileNameSrc = new FileName(srcFile);
		FileName fileNameTarget = new FileName(srcFile);
		try {
			
			srcFile_dir = fileNameSrc.getDir();
//			srcFile_name = ComFileUtil.getFileName(srcFile, false);
			srcFile_ext = fileNameSrc.getExt();
			
			// we should not use the keep folder of the target Disk, since it should be a virtual Folder
			// which cause the source file and the target file are NOT in the same physical disk, then the rename action would cost a long time.
//			fileNameTarget.setDir(fileNameSrc.getDisk() + "\\keep\\");
			fileNameTarget.setDir(srcFile_dir);
			fileNameTarget.setFileName(ComStrUtil.getRandomString(32));
		} catch (Exception e1) {
			e1.printStackTrace();
			throw e1;
		}

		
		if(fileNameSrc.toFile().renameTo(fileNameTarget.toFile())) {
			String printedLog = "";
			try {
				
				printedLog = ComCMDUtil.runCMD(
						new StringBuilder("")
						.append(FFProbePath).append(" -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 ")
						.append(" \"")
						.append(fileNameTarget).append("\"")
						.toString());
//				ComLogUtil.info("probe printedLog" + printedLog);
				
				String[] split = printedLog.split("\n");
				String probeRetVal = split[4]; 
//				ComLogUtil.debug("probe retVal: " + probeRetVal);
				
				retVal = Float.parseFloat(probeRetVal);
				
				// TODO: add logic to check if succeed.
				if(true) {
//					ComLogUtil.debug(srcFile + " probeSuccess.");
				} else {
//					ComLogUtil.info(srcFile + " probeFailed. needChk log:" + printedLogs[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
				ComLogUtil.info(srcFile + " probeFailed. needChk(maybe this video file is invalid). printedLog: " + printedLog);
				throw e;
			} finally {
				fileNameTarget.toFile().renameTo(fileNameSrc.toFile());
			}
		} else {
			String msg = "rename failed srcFile:" + srcFile + " to " + fileNameTarget + ", maybe the parent folder of the targetFile does NOT exist???";
			ComLogUtil.error(msg);
			throw new Exception(msg);
		}

		return retVal;
	}
	
	/**
	 * refer: https://superuser.com/questions/841235/how-do-i-use-ffmpeg-to-get-the-video-resolution
	 * @param srcFile the source video file
	 * @return VideoResolution
	 * @throws Exception
	 */
	public static VideoDuration probeVideoDuration(String srcFile) throws Exception {

		Float probeVideoDurationSec = probeVideoDurationSec(srcFile);
		String probeVideoDurationSexagesimal = probeVideoDurationSexagesimal(srcFile);

		return new VideoDuration(probeVideoDurationSec, probeVideoDurationSexagesimal);
	}
	
	public static void main(String[] args) throws Exception {
		ComLogUtil.info(probeVideoResolution("E:\\Downloads\\part1\\MDVR\\MDVR-044-有村のぞみVR解禁！手招きすれば即尺即ハメしてくれる僕専属ご奉仕メイド_1.mp4"));
	}
	
}

