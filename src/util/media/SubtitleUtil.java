package util.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import model.Subtitle;
import model.SubtitleStream;
import util.commonUtil.ComCMDUtil;
import util.commonUtil.ComFileUtil;
import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;
import util.commonUtil.model.FileName;

public class SubtitleUtil {

	public SubtitleUtil() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * remove meaningless tags. e.g. <font face="Monospace">  </font> {\an7} \h\h\h\h\h\h\h\h\h\h\h\h\h\h\h\h\h\h\h\h
	 * @throws Exception 
	 */
	public static void removeCloseCaptionTag(File file) throws Exception {
		String str = ComFileUtil.readFile2String(file);
		String retVal = str.replaceAll("<font face=\"Monospace\">", "")
			.replaceAll("</font>", "")
			.replaceAll("\\{\\\\an7\\}", "")
			.replaceAll("\\\\h", "");
		ComFileUtil.writeString2File(retVal, file, "UTF-8");
		ComLogUtil.info("unnecessary tag has been removed for file: " + file);
	}

	public static File[] extractSubtitles(String srcFile) throws Exception {
		SubtitleStream[] subtitles = FFProbeUtil.probeSubtitles(srcFile);
		File[] extractedSubtitles = new File[subtitles.length];
		for(int i = 0; i < subtitles.length; i++){
			SubtitleStream subtitle = subtitles[i];
			ComLogUtil.info("saving subtitle: " + ComLogUtil.objToFormatString(subtitle));
			extractedSubtitles[i] = FFmpegUtil.extractSubtitle(srcFile, subtitle, subtitle.isClosedCaption());
			if(subtitle.isClosedCaption()) { // need to remove meaningless tags
				removeCloseCaptionTag(extractedSubtitles[i]);
			}
		}
		return extractedSubtitles;
	}
	
	public static boolean extractCondensedSubtitles(String srcFile) throws Exception {
		File[] extractSubtitles = extractSubtitles(srcFile);
		for(int i = 0; i < extractSubtitles.length; i++){
			File extractSubtitle = extractSubtitles[i];
			condenseSubtitle(extractSubtitle.getPath());
//			extractSubtitle.delete();
		}
		return true;
	}
	
	public static boolean extractCondensedSubtitles(File srcFile) throws Exception {
		return extractCondensedSubtitles(srcFile.getPath());
	}
	
	public static boolean extractCondensedSubtitlesInFolder(String srcFolder, int minSizeInMB) throws Exception {
		File[] files = new File(srcFolder).listFiles();
		for(int i = 0; i < files.length; i++){
			File file = files[i];
			
			if(file.isDirectory()) {
				extractCondensedSubtitlesInFolder(file.getPath(), minSizeInMB);
			}
			
			if(!ComMediaUtil.isVideo(file)) {
				ComLogUtil.info("skip non-video file: " + file.getPath());
				continue;
			}
			long fileSizeMB = ComFileUtil.getFileSizeMB(file);
			if(fileSizeMB <= minSizeInMB) {
				ComLogUtil.info("skip too small file: " + file.getPath() + ", fileSizeMB:" + fileSizeMB);
				continue;
			}
			
			ComLogUtil.info("extracting file: " + file.getPath());
			extractCondensedSubtitles(file);
		}
		return true;
	}
	
	public static boolean condenseSubtitle(String file) throws Exception {
		String str = ComFileUtil.readFile2String(file);
		
		String[] lines = str.trim().split("\n");
		List<String> groups = new ArrayList<String>();
		String preIndexStr = "";
		String tmpGroup = "";
		Boolean isIndexFound = false;
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String matchedString = ComRegexUtil.getMatchedString(line, "^\\d+\\s*$");
			if(ComStrUtil.isBlankOrNull(matchedString)) {
				if(isIndexFound) {
					tmpGroup += line + '\n';
				} else {
					preIndexStr += line + '\n';
				}
			} else {
				// if it's the index
				isIndexFound = true;
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
		
//		ComLogUtil.info("preIndexStr:" + preIndexStr);
//		ComLogUtil.info("content:" + ComLogUtil.objToFormatString(groups));
		
		int size = groups.size();
		StringBuffer sb = new StringBuffer("");
		for(int i = 0; i < size; i++) {
			String group = groups.get(i);
			Subtitle subtitle = new Subtitle(group);
			sb.append(subtitle);
		}
//		ComLogUtil.info("final srt: " + sb);
		String outputFile = new FileName(file).append("_condensed").toString();
		ComFileUtil.writeString2File(sb.toString(), outputFile, "UTF-8");
		ComLogUtil.info("srt: " + file + " condensed to " + outputFile);
		return true;
	}
	
	public static void main(String[] args) throws Exception {
//		condenseSubtitle("E:\\Downloads\\Wolf.Creek.S01.2160p.WEB.X265-DEFLATE\\Wolf.Creek.S01E02.INTERNAL.2160p.WEB.X265-DEFLATE_3(eng)_English (SDH).srt");
		// test probe
//		probeSubtitle("E:\\Downloads\\Wolf.Creek.S01.2160p.WEB.X265-DEFLATE\\Wolf.Creek.S01E01.INTERNAL.2160p.WEB.X265-DEFLATE.mkv");
		
//		extractSubtitles("E:\\Downloads\\Wolf.Creek.S01.2160p.WEB.X265-DEFLATE\\Wolf.Creek.S01E02.INTERNAL.2160p.WEB.X265-DEFLATE.mkv");
//		 extractCondensedSubtitles("D:\\THREE_STOOGES_1940_1942_V3D2\\a.vob");
//		removeCloseCaptionTag(new File("D:\\THREE_STOOGES_1940_1942_V3D2\\a_0[0x1e0]_closedCaption.srt"));
		
		
//		extractCondensedSubtitlesInFolder("E:\\Downloads\\Wolf.Creek.S02.1080p\\", 10);
		
		
//		extractCondensedSubtitlesInFolder("D:\\TheThreeStooges\\", 100);
		extractCondensedSubtitles("D:\\TheThreeStooges\\THREE_STOOGES_1946_1948 V5D1\\VTS_15_1.VOB");
//		extractCondensedSubtitles("D:\\TheThreeStooges\\todo\\THREE_STOOGES_ULTIMATE_COLL_D18_extracted\\Part_1.vob");
//		extractCondensedSubtitlesInFolder("D:\\TheThreeStooges\\todo\\THREE_STOOGES_1955_1959 V8D3_extracted\\", 100);
		
	}
	
	

}
