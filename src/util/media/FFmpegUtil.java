package util.media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import model.SubtitleStream;
import util.commonUtil.ComCMDUtil;
import util.commonUtil.ComFileUtil;
import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;
import util.commonUtil.CommonUtil;
import util.commonUtil.ConfigManager;
import util.commonUtil.model.FileInfo;
import util.commonUtil.model.FileName;
import util.helper.FileNameEscaper;

public class FFmpegUtil {

	public FFmpegUtil() {
		// TODO Auto-generated constructor stub
	}

	private static String FFMPEGPath;
	
	static {
		if(CommonUtil.isInJar()) {
			FFMPEGPath = new File("ffmepg.exe").getAbsolutePath();
			if(new File(FFMPEGPath).exists()) {
				ComLogUtil.info("target ffmepg.exe file : " + FFMPEGPath + " already exists");
			} else {
				ComLogUtil.info("target ffmepg.exe file : " + FFMPEGPath + " not exists, will copy from jar file");
				try {
					CommonUtil.copyExeOutFrmJar(FFmpegUtil.class.getResource("/resource/ffmepg.exe"), FFMPEGPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			// otherwise load from config
			FFMPEGPath = ConfigManager.getConfigManager(ComMediaUtil.class.getResource("ComMediaUtil.properties")).getString("FFMPEGPath");
		}
		isFFMPEGReady = new File(FFMPEGPath).exists();
	}

	private static boolean isFFMPEGReady;


	public static String getFFMPEGPath() {
		return FFMPEGPath;
	}


	public static boolean isFFMPEGReady() {
		return isFFMPEGReady;
	}
	

	/**
	 * concate multiple media file into one file.
	 * @param targetFile the expected file to concatenate to.
	 * @param overwriteExistFile whether overwrite exists targetFile
	 * @param mediaEncoding the encoding of src media Files. default 'mp4'
	 * @param srcFiles the src file parts to concatenate from.
	 * @return true: success; false: failed.
	 * @throws Exception
	 */
	public static boolean concatMedia(File targetFile, boolean overwriteExistFile, String mediaEncoding, File... srcFiles) throws Exception {
		if(!isFFMPEGReady) {
			throw new Exception("Error: FFMPEG " + FFMPEGPath + " is not ready!");
		}

		if(targetFile.isDirectory()) throw new Exception("Error:targetFile must be a file, not directory!");
		if(!overwriteExistFile && targetFile.exists()) {
			System.out.println("targetFile:" + targetFile.getPath() + " already exists");
			return true;
		}

		if(ComStrUtil.isBlankOrNull(mediaEncoding)) mediaEncoding = ComMediaUtil.DEFAULT_MEDIA_ENCODING;

		FileNameEscaper escaper = new FileNameEscaper();
		File targetFileEscaped = escaper.escape(targetFile, false, mediaEncoding);
//		String dir = ComFileUtil.getFileDiretory(targetFileEscaped);


		String dir = ComFileUtil.getFileDiretory(targetFile);
		String tempFileName = "deleteMe_" + ComStrUtil.getRandomString(5) + ".txt";
		File tempFile = ComFileUtil.createFile(dir + tempFileName);
		int length = srcFiles.length;

		// to concatenate all media, ffmpeg need to create a temp file, which contains all media files needed to be concatenated.
		StringBuilder tempFileContent = new StringBuilder();
		// write all media files into the temp file
		for(int i = 0; i < length; i++) {
			if(i != 0) tempFileContent.append(ComFileUtil.EOL);
			tempFileContent.append("file ").append(ComRegexUtil.replaceAllLiterally(escaper.escape(srcFiles[i], true, mediaEncoding).getPath(), "\\", "\\\\"));
		}
		System.out.println("fileContent:" + tempFileContent);
		try {
			ComFileUtil.writeString2File(tempFileContent.toString(), tempFile, ComFileUtil.UTF8, false);
			String cmd = wrapWithQuote(FFMPEGPath) + " -f concat -safe 0 -i " + wrapWithQuote(tempFile.getPath()) + " -c copy " + wrapWithQuote(targetFileEscaped.getPath());
			System.out.println("cmd:" + cmd);
			String res = ComCMDUtil.runCMD(cmd);
			System.out.println("res:" + res);
			if(targetFileEscaped.exists()) {
				if(targetFile.exists()) targetFile.delete();
				return true;
			}
			return false;
		} finally {
			tempFile.delete();
			escaper.recoverAll();
		}
	}

	/**
	 * do a merge
	 * @param videoFile
	 * @param audioFile
	 * @param outputFile
	 * @return
	 * @throws Exception
	 */
	static boolean mergeAVAct(String videoFile, String audioFile, String outputFile) {
		if(!isFFMPEGReady) {
			ComLogUtil.error("Error: FFMPEG " + FFMPEGPath + " is not ready!");
			return false;
		}
		String printedLog;
		try {
			printedLog = ComCMDUtil.runCMD(new StringBuilder(FFMPEGPath).append(" -i \"").append(videoFile).append("\"")
					.append(" -i \"").append(audioFile).append("\"")
					.append(" -c:v copy -c:a copy").append("  \"").append(outputFile).append("\"").toString()).replaceAll("\n", " \\n ");
			if(isSuccess(printedLog)) {
				ComLogUtil.info(videoFile + " mergeSuccess.");
				return true;
			} else {
				ComLogUtil.info(videoFile + " mergeFailed. needChk log:" + printedLog);
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			ComLogUtil.info(videoFile + " mergeFailed. needChk");
			return false;
		}
	}
	
	private static Integer duration2Second(String duration) {
		String[] split = duration.split(":");
		return new Integer(Integer.parseInt(split[0]) * 60 * 60 + Integer.parseInt(split[1]) * 60 + Integer.parseInt(split[2]));
	}
	
	/**
	 * check if merges successfully by investigating log.
	 * @param log
	 * @return
	 */
	private static boolean isSuccess(String log) {
		//check muxing
		if(!log.contains("muxing overhead")) {
			return false;
		}
		//check duration
		String[] matchedStringArr = ComRegexUtil.getMatchedStringArr(log, "(?<=Duration:\\s{1,20})[^\\.]+");
		if(matchedStringArr == null || matchedStringArr.length != 2) {
			ComLogUtil.error("needChk no Duration found!");
			return false;
		}
		//check duraiton differ
		if(Math.abs(duration2Second(matchedStringArr[0]) - duration2Second(matchedStringArr[1])) > 5) {// if duration of audio/video differ > 5 seconds
			ComLogUtil.error("needChk duration too greater video:" + matchedStringArr[0] + ", audio:" + matchedStringArr[1]);
			// return false;
			// now, he latest ffmpeg will add padding to the shorter one(video or audio) if their length not match, and the mux process will still succeed.
			return true;
		}
		//check 2 audio video match
		if(log.contains("Invalid nal size") || log.contains("missing picture in access")) {
			ComLogUtil.info("needChk AV not match.  Invalid nal size | missing picture in access");
			return false;
		}

		return true;
	}

	public static String wrapWithQuote(String str) {
		return " \"" + str + "\"";
	}

	public static Boolean videoToGif(File video, File gif, String startFrom, String howLongWillLast, int frames, String quality) throws Exception {

		FileNameEscaper escaper = new FileNameEscaper();
		try {
			File videoEscaped = escaper.escape(video, true, ".mp4");
			File gifEscaped = escaper.escape(gif, false, ".gif");

			String cmd = new StringBuilder(wrapWithQuote(FFMPEGPath)).append(" -y -i ").append(wrapWithQuote(videoEscaped.getPath()))
					.append(" -ss ").append(startFrom)
					.append(" -t ").append(howLongWillLast)
					.append(" -pix_fmt rgb24")
					.append(" -s ").append(quality)
					.append(" -r ").append(frames)
					.append(" -ss ").append(startFrom)
					.append(wrapWithQuote(gifEscaped.getPath()))
					.toString();

			System.out.println("cmd:" + cmd);
			String res = ComCMDUtil.runCMD(cmd);
//			System.out.println("res:" + res);
			if(gifEscaped.exists()) {
				if(gifEscaped.length() > 0) {
					return true;
				}
				ComLogUtil.error("videoToGif failed: the target gif size is 0!" + gif.getAbsolutePath());
			}
		} finally {
			escaper.recoverAll();
		}
		return false;
	}
	

	/**
	 * Extract subtitle of given metaInfo(SubtitleStream) from srcFile
	 * @param srcFile the source File to extract subtitle from
	 * @param subtitle the metaInfo of the subtitle to be extracted.
	 * @return extracted srt file
	 */
	public static File extractSubtitle(String srcFile, SubtitleStream subtitle, boolean isClosedCaption) {
		String printedLog = "";
		FileName outputFile = new FileName(srcFile).append(subtitle.getSuggestedFileNameSuffix()).setExt(".srt");
//		FileName outputFile = null;
		try {
			if(isClosedCaption) {
//				printedLog = ComCMDUtil.runCMD(new StringBuilder(" cd /d d:\\tmp")
//						printedLog = ComCMDUtil.runCMD(new StringBuilder(" cd /d \"D:\\THREE_STOOGES_1940_1942_V3D2\" ")
				FileName srcFileName = new FileName(srcFile);
				
				printedLog = ComCMDUtil.runCMD(new StringBuilder(" cd /d \"").append(srcFileName.getDir()).append("\" \n ")
						.append(FFMPEGPath).append(" -f lavfi -i \"movie=").append(srcFileName.getFileNameAndExtension()).append("[out0+subcc]\"")
						.append(" -map s ")
						.append("  \"").append(outputFile).append("\"")
						.toString());
			} else {
				printedLog = ComCMDUtil.runCMD(new StringBuilder(FFMPEGPath).append(" -i \"").append(srcFile).append("\"")
						.append(" -map 0:").append(subtitle.getIndex())
						.append("  \"").append(outputFile).append("\"").toString());
			}
		} catch(Exception e) {
			ComLogUtil.error("extractSubtitle failed - srcFile: " + srcFile + ", subtitle: " + subtitle + ", isClosedCaption: " + isClosedCaption + ", cmdLog:" + printedLog);
			ComLogUtil.error(e);
		}
		ComLogUtil.info("extractSubtitle printedLog: " + printedLog);
		return outputFile.toFile();
	}
	
	public static void main(String[] args) {
		extractSubtitle("D:\\THREE_STOOGES_1940_1942_V3D2\\a.vob", null, true);
	}

}
