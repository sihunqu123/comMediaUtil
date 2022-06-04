package util.media;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import util.commonUtil.ComCMDUtil;
import util.commonUtil.ComFileUtil;
import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;
import util.commonUtil.ConfigManager;
import util.commonUtil.model.FileInfo;
import util.helper.FileNameEscaper;

public class ComMediaUtil {

//	private static String FFMPEGPath = ConfigManager.getConfigManager(ComMediaUtil.class.getResource("ComMediaUtil.properties")).getString("FFMPEGPath");
	private static String FFMPEGPath;

	static String encode = "gbk";


	private static boolean isInJar() {
		String resource = ComMediaUtil.class.getResource("ComMediaUtil.class").toString();
		boolean isInJar = resource.startsWith("jar");
		ComLogUtil.info("resource:" + resource
				+ ", isInJar:" + isInJar
				);
		return isInJar;
	}


	private static void copyFfmpegOutFrmJar(URL inputFileURL, String outputFilePath) throws IOException {
		//gets program.exe from inside the JAR file as an input stream
		InputStream is;
		try {
			is = inputFileURL.openStream();
			//sets the output stream to a system folder
			OutputStream os = new FileOutputStream(outputFilePath);

			//2048 here is just my preference
			byte[] b = new byte[2048];
			int length;

			while ((length = is.read(b)) != -1) {
			    os.write(b, 0, length);
			}
			is.close();
			os.close();
		} catch (IOException e) {
			ComLogUtil.info("copy ffmpeg.exe to: " + FFMPEGPath + " failed");
			throw e;
		}
		ComLogUtil.info("copyed ffmpeg.exe to: " + FFMPEGPath);
	}

	static {
		if(isInJar()) {
			FFMPEGPath = new File("ffmpeg.exe").getAbsolutePath();
			if(new File(FFMPEGPath).exists()) {
				ComLogUtil.info("target ffmpeg.exe file : " + FFMPEGPath + " already exists");
			} else {
				ComLogUtil.info("target ffmpeg.exe file : " + FFMPEGPath + " not exists, will copy from jar file");
				try {
					copyFfmpegOutFrmJar(ComMediaUtil.class.getResource("/resource/ffmpeg.exe"), FFMPEGPath);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			// otherwise load from config
			FFMPEGPath = ConfigManager.getConfigManager(ComMediaUtil.class.getResource("ComMediaUtil.properties")).getString("FFMPEGPath");
		}
	}

	private static boolean isFFMPEGReady = new File(FFMPEGPath).exists();

	private static String DEFAULT_MEDIA_ENCODING = "mp4";

	/**
	 * concate multiple media file into one file.
	 * @param targetFile the expected file to concatenate to.
	 * @paramo overwriteExistFile whether overwrite exists targetFile
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

		if(ComStrUtil.isBlankOrNull(mediaEncoding)) mediaEncoding = DEFAULT_MEDIA_ENCODING;

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
	 * concat multiple media file into one file.
	 * @param targetFile the expected file to concat to.
	 * @param overwriteExistFile whether overwrite exists targetFile
	 * @param srcFiles the src file parts to concat from.
	 * @return true: success; false: failed.
	 * @throws Exception
	 */
	public static boolean concatMedia(File targetFile, boolean overwriteExistFile, File... srcFiles) throws Exception {
		return concatMedia(targetFile, overwriteExistFile
				//, ComFileUtil.getFileExtension(srcFiles[0], false)
				, DEFAULT_MEDIA_ENCODING
				, srcFiles);
	}

	public static String wrapWithQuote(String str) {
		return " \"" + str + "\"";
	}

	public static void mergeDir(String dir) {
		mergeDir(new File(dir));
	}

	public static void mergeDir(File dir) {
		File[] listFiles = dir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				if(pathname.isDirectory() || ComStrUtil.isBlankOrNull(ComRegexUtil.getMatchedString(pathname.getName(), "^\\d_")) || !".mkv".equals(ComFileUtil.getFileExtension(pathname, true))) {
					return true;
				}
				return false;
			}
		});

		for(int i = 0; i < listFiles.length; i++) {
			if(listFiles[i].exists()) {
				if(listFiles[i].isDirectory()) {
					mergeDir(listFiles[i]);
				} else {
					String fileName = ComFileUtil.getFileDiretory(listFiles[i]) + ComFileUtil.getFileName(listFiles[i], false);
					File videoFile = new File(fileName + ".mp4");
					File audioFile = new File(fileName + ".webm");
					if(videoFile.exists() && audioFile.exists() && mergeAV(videoFile, audioFile, fileName + ".mkv")) {
						videoFile.delete();
						audioFile.delete();
					}
				}
			}
		}
	}


	public static boolean mergeAV(File videoFile, File audioFile, String outputFile) {
		return mergeAV(videoFile.getPath(), audioFile.getPath(), outputFile);
	}

	public static boolean mergeAV(File videoFile, File audioFile, File outputFile) {
		return mergeAV(videoFile.getPath(), audioFile.getPath(), outputFile.getPath());
	}

	public static boolean mergeAV(String videoFile, String audioFile, String outputFile) {
		if(videoFile.equals(audioFile)) {
			ComLogUtil.error("AV files are the same. - " + videoFile);
			return false;
		}
		//first bak those name.
		String videoFile_ = null;
		String videoFile_dir = null;
//		String videoFile_name = null;
		String videoFile_ext = null;
		String audioFile_ = null;
		String audioFile_dir = null;
//		String audioFile_name = null;
		String audioFile_ext = null;
		String outputFile_ = null;
		String outputFile_dir = null;
//		String outputFile_name = null;
		String outputFile_ext = null;
		try {
			videoFile_dir = ComFileUtil.getFileDiretory(videoFile);
//			videoFile_name = ComFileUtil.getFileName(videoFile, false);
			videoFile_ext = ComFileUtil.getFileExtension(videoFile, true);
			videoFile_ = videoFile_dir + ComStrUtil.getRandomString(32) + videoFile_ext;
			audioFile_dir = ComFileUtil.getFileDiretory(audioFile);
//			audioFile_name = ComFileUtil.getFileName(audioFile, false);
			audioFile_ext = ComFileUtil.getFileExtension(audioFile, true);
			audioFile_ = audioFile_dir + ComStrUtil.getRandomString(32) + audioFile_ext;
			outputFile_dir = ComFileUtil.getFileDiretory(outputFile);
//			outputFile_name = ComFileUtil.getFileName(outputFile, false);
			outputFile_ext = ComFileUtil.getFileExtension(outputFile, true);
			outputFile_ = outputFile_dir + ComStrUtil.getRandomString(32) + outputFile_ext;
		} catch (Exception e1) {
			e1.printStackTrace();
			return false;
		}

		File outputF = new File(outputFile_);
		if(new File(videoFile).renameTo(new File(videoFile_)) && new File(audioFile).renameTo(new File(audioFile_))) {
			if(mergeAVAct(videoFile_, audioFile_, outputFile_)) {//changeNameBack.
				new File(videoFile_).renameTo(new File(videoFile));
				new File(audioFile_).renameTo(new File(audioFile));
				outputF.renameTo(new File(outputFile));
				ComLogUtil.info(outputFile + " mergeSuccess.");
				return true;
			} else {	//if merge failed, first rename AV file back to original name.
				ComLogUtil.info(outputFile + " needChk mergeFailed.");
				new File(videoFile_).renameTo(new File(videoFile));
				new File(audioFile_).renameTo(new File(audioFile));
				if(outputF.exists()) {//if output file generted, delete it.
					outputF.delete();
				}
				return false;
			}
		} else {
			ComLogUtil.error("rename failed videoFile:" + videoFile);
			return false;
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
	private static boolean mergeAVAct(String videoFile, String audioFile, String outputFile) {
		if(!isFFMPEGReady) {
			ComLogUtil.error("Error: FFMPEG " + FFMPEGPath + " is not ready!");
			return false;
		}
		String printedLog;
		try {
			printedLog = runCMD(new StringBuilder(FFMPEGPath).append(" -i \"").append(videoFile).append("\"")
					.append(" -i \"").append(audioFile).append("\"")
					.append(" -c:v copy -c:a copy").append("  \"").append(outputFile).append("\"").toString());
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

	private static Integer duration2Second(String duration) {
		String[] split = duration.split(":");
		return new Integer(Integer.parseInt(split[0]) * 60 * 60 + Integer.parseInt(split[1]) * 60 + Integer.parseInt(split[2]));
	}

	private static String runCMD(String cmd) throws Exception {
		String[] command = { "cmd", };
		Process p = Runtime.getRuntime().exec(command);
		StringBuffer sb = new StringBuffer();
		new Thread(new SyncPipe(p.getErrorStream(), sb)).start();
		new Thread(new SyncPipe(p.getInputStream(), sb)).start();
//		ComLogUtil.info("cmd:" + cmd);
		cmd = cmd + " \n";
//		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), "GBK"));
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(p.getOutputStream(), encode));
//		bw.write("chcp\n");
		bw.write(cmd);
//		bw.write("echo \"ã€�ï½¾ï½¸ï½¼ï½°ï½ºï¾�ï¾„ç·�é›†ç·¨ã€‘å¿—æ�‘ã�‘ã‚“ã�®ãƒ�ã‚«æ®¿æ§˜ æœ€ã‚‚å†�ç”Ÿã�•ã‚Œã�¦ã�„ã‚‹ã‚»ã‚¯ã‚·ãƒ¼ãƒ�ã‚¿å‹•ç”»\"\n");
		bw.flush();
		bw.close();
//		PrintWriter stdin = new PrintWriter(p.getOutputStream());

//		stdin.println();
		//stdin.println(cmd);
		// write any other commands you want here
//		stdin.close();
		int returnCode = p.waitFor();
		ComLogUtil.info("sb:" + sb);
		System.out.println("Return code = " + returnCode);
		return sb.toString();
	}



	public static final String QULITY_HIGH = "1280x960";
	public static final String QULITY_Media = "480x360";
	public static final String QULITY_LOW = "320x230";

	public static Boolean videoToGif(File video, File gif) throws Exception {
		return videoToGif(video, gif, "00:00:00.000", "09:00:00.000", 5, QULITY_Media);
	}

	public static Boolean videoToGif(File video) throws Exception {
		FileInfo fileInfo = ComFileUtil.getFileInfo(video);
		fileInfo.setFileExt(".gif");
		return videoToGif(video, fileInfo.toFile());
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
	
	private static List<String> vidoeExtensions = Arrays.asList(
			"mkv",
			"avi",
			"flv",
			"webm",
			"ts",
			"mp4"
	);
	
	private static List<String> picutreExtensions = Arrays.asList(
			"jpg",
			"jpeg",
			"png",
			"ico",
			"pdf",
			"bmp",
			"icon"
	);
	
	private static List<String> txtExtensions = Arrays.asList(
			"txt",
			"text",
			"md"
	);
	
	private static List<String> richTextExtensions = Arrays.asList(
			"html",
			"htm",
			"mhtml",
			"xml",
			"chm",
			"apk",
			"doc",
			"mht"
	);
	
	private static List<String> urlExtensions = Arrays.asList(
			"url",
			"URL"
	);
	
	private static List<String> compressedFileExtensions = Arrays.asList(
			"rar",
			"gzip",
			"tar",
			"zip"
	);
	
	private static List<String> junkFileExtensions = Arrays.asList(
			"html",
			"db",
			"ini",
			"nfo"
	);
	
	/**
	 * 
	 * @param filenameOnly e.g. abc.mp4
	 * @return
	 */
	public static boolean isVideo(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return vidoeExtensions.contains(extension);
	}
	
	public static boolean isPicutre(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return picutreExtensions.contains(extension);
	}
	
	public static boolean isTxt(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return txtExtensions.contains(extension);
	}
	
	public static boolean isRichText(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return richTextExtensions.contains(extension);
	}
	
	public static boolean isURL(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return urlExtensions.contains(extension);
	}
	
	public static boolean isTorrent(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return "torrent".equalsIgnoreCase(extension);
	}
	
	public static boolean isCompressedFile(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return compressedFileExtensions.contains(extension);
	}
	
	public static boolean isJunkFileType(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return junkFileExtensions.contains(extension);
	}
	


	public static void main(String[] args) {

		try {
			mergeAV("D:\\ori\\s_34008\\331808\\64\\video.m4s", "D:\\ori\\s_34008\\331808\\64\\audio.m4s", "D:\\ori\\s_34008\\331808\\64\\all.mkv");
			// test FFMPEGPath
//			System.out.println(FFMPEGPath);
//			String aa = "aa1";
//			System.out.println((aa = "aa2") + "bb");

			// test videoToGif
//			ComLogUtil.info("videoToGif: " + videoToGif(new File("E:\\toDownload\\avid", "aaa.mp4")));
			// ComLogUtil.info("videoToGif: " + videoToGif(new File("E:\\toDownload\\avid", "SW_407_è€�å©†å¸®æˆ‘ç�Œé†‰ä»–çš„é—ºèœœä»¬ï¼Œæˆ‘å°±å�¯ä»¥ä¸ºæ‰€æ¬²ä¸ºäº†.mp4")));


			// test with en only char
//			System.out.println("concatRes:" + concatMedia(
//					new File("E:\\360Downloads\\temp\\aa.flv")
//					, true
//					, new File("E:\\360Downloads\\temp\\aa-1.flv")
//					, new File("E:\\360Downloads\\temp\\aa-2.flv")
//					, new File("E:\\360Downloads\\temp\\aa-3.flv")
//
//					));

			// test with en and zh chars
//			System.out.println("concatRes:" + concatMedia(
//					new File("E:\\360Downloads\\temp\\çœŸä¸‰å›½æ— å�Œ7 - å�•å¸ƒä¼ ã€�é•¿å®‰æ”¿å�˜IFã€‘è²‚è�‰ï¼ˆåˆƒå¼©è½°ç‚¸æœºï¼‰_P1_20581334.flv.blv"),
//					true
//					, new File("E:\\360Downloads\\temp\\çœŸä¸‰å›½æ— å�Œ7 - å�•å¸ƒä¼ ã€�é•¿å®‰æ”¿å�˜IFã€‘è²‚è�‰ï¼ˆåˆƒå¼©è½°ç‚¸æœºï¼‰_P1_20581334-1.flv.blv")
//					, new File("E:\\360Downloads\\temp\\çœŸä¸‰å›½æ— å�Œ7 - å�•å¸ƒä¼ ã€�é•¿å®‰æ”¿å�˜IFã€‘è²‚è�‰ï¼ˆåˆƒå¼©è½°ç‚¸æœºï¼‰_P1_20581334-2.flv.blv")
//					, new File("E:\\360Downloads\\temp\\çœŸä¸‰å›½æ— å�Œ7 - å�•å¸ƒä¼ ã€�é•¿å®‰æ”¿å�˜IFã€‘è²‚è�‰ï¼ˆåˆƒå¼©è½°ç‚¸æœºï¼‰_P1_20581334-3.flv.blv")
//			));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// test mergeAV
//		String title = "F:\\download\\youtube4\\20151128 2015é§�äºŒå‹•æ¼«ç¥­ FFK8_PL5N5Hv3klahHharj5pvgIF2mZkuaxe-Ga\\2015é§�äºŒå‹•æ¼«ç¥­ FFK8 Heroes of Cosplay 3(4K 2160p)[ç„¡é™�HD]";
//		String videoFile = title + ".mp4";
//		String audioFile = title + ".webm";
//		String outputFile = title + ".mkv";
//		ComLogUtil.info(mergeAV(videoFile, audioFile, outputFile));
	}
}



class SyncPipe implements Runnable {

	public SyncPipe(InputStream istrm, StringBuffer sb) {
		istrm_ = istrm;
		this.sb = sb;
//		ostrm_ = ostrm;
	}

	@Override
	public void run() {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(istrm_, ComMediaUtil.encode));
//			br = new BufferedReader(new InputStreamReader(istrm_, "GBK"));
			for(String readStr; (readStr = br.readLine()) != null;) {
//				resStr.append(readStr);
				sb.append(readStr);
//				ComLogUtil.info("cmdLog:" + readStr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(br != null) {
				try {br.close();} catch (IOException e) {e.printStackTrace();}
				br = null;
			}
		}
		/*
		try {
			final byte[] buffer = new byte[1024];
			for (int length = 0; (length = istrm_.read(buffer)) != -1;) {
				ostrm_.write(buffer, 0, length);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	private StringBuffer sb;

//	private final OutputStream ostrm_;
	private final InputStream istrm_;
}