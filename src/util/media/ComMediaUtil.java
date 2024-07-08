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

import model.SubtitleStream;
import model.VideoDuration;
import model.VideoResolution;
import util.commonUtil.ComCMDUtil;
import util.commonUtil.ComFileUtil;
import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;
import util.commonUtil.ConfigManager;
import util.commonUtil.model.CheckResult;
import util.commonUtil.model.FileInfo;
import util.commonUtil.model.FileName;
import util.commonUtil.model.RegRule;
import util.helper.FileNameEscaper;

public class ComMediaUtil {

//	private static String FFMPEGPath = ConfigManager.getConfigManager(ComMediaUtil.class.getResource("ComMediaUtil.properties")).getString("FFMPEGPath");

	static String encode = "gbk";

	static String DEFAULT_MEDIA_ENCODING = "mp4";

	/**
	 * concat multiple media file into one file.
	 * @param targetFile the expected file to concat to.
	 * @param overwriteExistFile whether overwrite exists targetFile
	 * @param srcFiles the src file parts to concat from.
	 * @return true: success; false: failed.
	 * @throws Exception
	 */
	public static boolean concatMedia(File targetFile, boolean overwriteExistFile, File... srcFiles) throws Exception {
		return FFmpegUtil.concatMedia(targetFile, overwriteExistFile
				//, ComFileUtil.getFileExtension(srcFiles[0], false)
				, DEFAULT_MEDIA_ENCODING
				, srcFiles);
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
			if(FFmpegUtil.mergeAVAct(videoFile_, audioFile_, outputFile_)) {// changeNameBack.
				new File(videoFile_).renameTo(new File(videoFile));
				new File(audioFile_).renameTo(new File(audioFile));
				outputF.renameTo(new File(outputFile));
				ComLogUtil.info(outputFile + " mergeSuccess.");
				return true;
			} else {	// if merge failed, first rename AV file back to original name.
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

	
	

	public static final String QULITY_HIGH = "1280x960";
	public static final String QULITY_Media = "480x360";
	public static final String QULITY_LOW = "320x230";

	public static Boolean videoToGif(File video, File gif) throws Exception {
		return FFmpegUtil.videoToGif(video, gif, "00:00:00.000", "09:00:00.000", 5, QULITY_Media);
	}

	public static Boolean videoToGif(File video) throws Exception {
		FileInfo fileInfo = ComFileUtil.getFileInfo(video);
		fileInfo.setFileExt(".gif");
		return videoToGif(video, fileInfo.toFile());
	}


	private static List<String> vidoeExtensions = Arrays.asList(
//			"iso",
			"mkv",
			"avi",
			"flv",
			"webm",
			"rmvb",
			"ts",
			"vob",
			"mov",
			"m2ts",
			"wmv",
			"wmv",
//			"flac", // handle music file the same as video files temporally, need to remove this and create a musicExtensions later
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
			"log",
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
			"mp3",
			"m3u",
			"exe",
			"rtf",
			"ecp",
			"sfv",
			"td",
			"scr",
			"downloading",
			"qdl2",
			"md5",
			"nfo"
	);
	
	/**
	 * @return [ reg, isCaseSensitive ]
	 */
	private static RegRule[] junkFileNames = new RegRule [] {
		new RegRule("javplayer", false),
		new RegRule("モザイク破壊版", false), // this is NOT leak version, so it's low quality
		new RegRule("破坏版", false), // this is NOT leak version, so it's low quality
		new RegRule("4K_180x180", false), // 4k is too low for VR
		new RegRule("_1440p[_\\.]", false),
		new RegRule("_1600p[_\\.]", false),
		new RegRule("_1920p[_\\.]", false),
		new RegRule("^QR-1024\\.[^\\\\]+$", false),
		new RegRule("^宣传图\\.jpg$", false),
		new RegRule("^网址收藏~.jpg$", false),
		
		new RegRule("^歡迎加入.jpg$", false),
		new RegRule("^歡迎加入.jpg$", false),
		new RegRule("thumbs.jpg$", false),
		
		
		new RegRule("^撸一发吧 撸一发吧永久地址发布页.png$", false),
		
		
		new RegRule("^.*色中色论坛地址.*.jpg$", false),
		
		new RegRule("_TB.mp4$", false),	
		new RegRule("^sample.mp4$", false),	
		new RegRule("solji.kim.pdf", false),
		new RegRule("solji.kim", false)
		
	};
	
	/*
	 * @return [ junkFoldersReg, isCaseSensitive(1: true, 0: false) ]
	 */
	private static RegRule[] junkFoldersReg = new RegRule [] {
		new RegRule("U9A9磁力搜索", false),
		new RegRule("solji.kim", false),
		new RegRule("javplayer", false),
		new RegRule("モザイク破壊版", true)
	};
	
	private static List<String> subtitleExtensions = Arrays.asList(
			"sub",
			"idx",
			"ass",
			"srt"
	);
	
	private static List<String> reservedExtensions = Arrays.asList(
			"bup",
			"ifo",
			"clpi",
			"vob"
	);
	
	
	/**
	 * Keep those filename with `_KEEP.` string inside
	 * @param filenameOnly e.g. abc.mp4
	 * @return
	 */
	public static boolean isReservedFile(String filenameOnly) {
		String lowerCase = ("" + filenameOnly).toLowerCase();
		boolean shouldKeep = lowerCase.toUpperCase().indexOf("_KEEP.") > 0;
		shouldKeep = shouldKeep || lowerCase.startsWith("cover.") || ComFileUtil.getFileName(lowerCase, false).endsWith("cover");
		return shouldKeep || reservedExtensions.contains(ComFileUtil.getFileExtension(lowerCase, false));
	}
	
	/**
	 * 
	 * @param filenameOnly e.g. abc.mp4
	 * @return
	 */
	public static boolean isVideo(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return vidoeExtensions.contains(extension);
	}
	
	public static boolean isVideo(File filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return vidoeExtensions.contains(extension);
	}
	
	public static boolean isPicutre(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return picutreExtensions.contains(extension);
	}
	
	public static boolean isPicutre(File filenameOnly) {
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
	
	public static CheckResult checkJunkFile(String filenameOnly) {
		return ComRegexUtil.testRegs(filenameOnly, junkFileNames);
	}
	public static boolean isJunkFileType(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return junkFileExtensions.contains(extension);
	}
	
	
	/**
	 * 
	 * @param filenameOnly
	 * @return {
	 * 	result: 1 for isJunkFolder, otherwise is Not junkFolder,
	 * 	reason: the reg that matched,
	 * }
	 */
	public static CheckResult checkJunkFolder(String filenameOnly) {
		return ComRegexUtil.testRegs(filenameOnly, junkFoldersReg);
	}
	
	public static boolean isSubtitle(String filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return subtitleExtensions.contains(extension);
	}
	
	public static boolean isSubtitle(File filenameOnly) {
		String extension = ComFileUtil.getFileExtension(filenameOnly, false).toLowerCase();
		return subtitleExtensions.contains(extension);
	}
	
	public static VideoResolution getVideoResolution(File file) throws Exception {
		return FFProbeUtil.probeVideoResolution(file.getPath());
	}
	
	public static VideoDuration getVideoDuration(File file) throws Exception {
		return FFProbeUtil.probeVideoDuration(file.getPath());
	}
	
	public static VideoDuration getVideoDuration(String file) throws Exception {
		return FFProbeUtil.probeVideoDuration(file);
	}
	


	public static void main(String[] args) {

		try {

			
			// test merge
//			mergeAV("D:\\ori\\s_34008\\331808\\64\\video.m4s", "D:\\ori\\s_34008\\331808\\64\\audio.m4s", "D:\\ori\\s_34008\\331808\\64\\all.mkv");
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