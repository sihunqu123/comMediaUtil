package model;

import java.util.HashMap;
import java.util.Map;

import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;

public class MediaStream {

	String originalText;
	String name;
	int index;
	Map<String, String> metadata;
	String mediaType;
	boolean hasEmbededSubtitleStream = false;
	
	public static final String mediaType_video = "Video";
	public static final String mediaType_audio = "Audio";
	public static final String mediaType_subtitle = "Subtitle";
	
	public boolean isHasEmbededSubtitleStream() {
		return hasEmbededSubtitleStream;
	}
	public void setHasEmbededSubtitleStream(boolean hasEmbededSubtitleStream) {
		this.hasEmbededSubtitleStream = hasEmbededSubtitleStream;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	public Map<String, String> getMetadata() {
		return metadata;
	}
	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}
	
	public String getOriginalText() {
		return originalText;
	}
	public void setOriginalText(String originalText) {
		this.originalText = originalText;
	}
	public String getMediaType() {
		return mediaType;
	}
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}

	public MediaStream(String originalText, String name, int index, Map<String, String> metadata, String mediaType,
			boolean hasEmbededSubtitleStream) {
		super();
		this.originalText = originalText;
		this.name = name;
		this.index = index;
		this.metadata = metadata;
		this.mediaType = mediaType;
		this.hasEmbededSubtitleStream = hasEmbededSubtitleStream;
	}
	public MediaStream(String originalText) {
		super();
		this.originalText = originalText;
		initCommonData();
	}

	@Override
	public String toString() {
		return "MediaStream [name=" + name + ", index=" + index + ", metadata=" + metadata + "]";
	}
	
	private void initCommonData() {
		String[] lines = this.originalText.trim().split("\n");
		String name = "";
		Map<String, String> metadata = new HashMap<String, String>();
		Boolean isStreamFound = false;
		Boolean isMetadataFound = false;
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if(isStreamFound) {
				if(isMetadataFound) {
					String[] keyMap = line.split(":");
					if(keyMap.length > 1) {
						metadata.put(keyMap[0].trim(), keyMap[1].trim());
					}
				} else {
					String matchedString = ComRegexUtil.getMatchedString(line, "^\\s*Metadata:\\s*$");
					if(ComStrUtil.isBlankOrNull(matchedString)) {
						// do nothing if it's not reached the Stream part yet.
//						matchedString = ComRegexUtil.getMatchedString(line, "^\\s*Side data:\\s*$");
					} else {
						// if it's the beginning Metadata tag
						isMetadataFound = true;
					}
				}
			} else {
				String matchedString = ComRegexUtil.getMatchedString(line, "^\\s*Stream #0:\\d+[^:]*: .*");
				if(ComStrUtil.isBlankOrNull(matchedString)) {
					// do nothing if it's not reached the Stream part yet.
				} else {
					// if it's the beginning Stream tag
					isStreamFound = true;
					String[] infos = line.split(":");
					name = infos[1].trim();
					this.mediaType = infos[2].trim();
				}
			}
		}
		
		if(mediaType_video.equalsIgnoreCase(this.mediaType)) { // closedCaption may embedded into video stream
			String isVideoStream = ComRegexUtil.getMatchedString(lines[0], "^\\s*Stream #0:\\d+[^:]*: " + mediaType_video);
			String sidedataString = ComRegexUtil.getMatchedString(lines[1], "^\\s*Side data:.*");
			String cpbString = ComRegexUtil.getMatchedString(lines[2], "^\\s*cpb:.*");
			if(
					ComStrUtil.hasContent(isVideoStream)
					&& ComStrUtil.hasContent(sidedataString)
					&& ComStrUtil.hasContent(cpbString)
					) {
				this.hasEmbededSubtitleStream = true;
			}
		}

		this.index = Integer.parseInt(ComRegexUtil.getMatchedString(name, "^\\d+"));
		this.name = name;
		this.metadata = metadata;
	}
	 
	
}