package model;

import java.util.HashMap;
import java.util.Map;

import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;

//public class SubtitleStream extends MediaStream {
public class SubtitleStream extends MediaStream {

	boolean isClosedCaption = false;
	private String string;
	
	
	
	public boolean isClosedCaption() {
		return isClosedCaption;
	}

	public void setClosedCaption(boolean isClosedCaption) {
		this.isClosedCaption = isClosedCaption;
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
	
	public String getSuggestedFileNameSuffix() {
		String title = this.metadata.get("title");
		String sufix = "";
		if(ComStrUtil.hasContent(title)) {
			sufix = title;
		} else if(this.isClosedCaption()) {
			sufix = "closedCaption";
		}
		return "_" + name + "_" + sufix;
	}


	@Override
	public String toString() {
		return "SubtitleStream [index=" + index + ", name=" + name + ", metadata=" + metadata + ", isClosedCaption: " + isClosedCaption + "]";
	}
	
	public SubtitleStream(String cmdResult) {
		super(cmdResult);
		
//		String[] lines = cmdResult.trim().split("\n");
//		String name = "";
//		Map<String, String> metadata = new HashMap<String, String>();
//		Boolean isStreamFound = false;
//		Boolean isMetadataFound = false;
//		for(int i = 0; i < lines.length; i++) {
//			String line = lines[i];
//			if(isStreamFound) {
//				if(isMetadataFound) {
//					String[] keyMap = line.split(":");
//					if(keyMap.length > 1) {
//						metadata.put(keyMap[0].trim(), keyMap[1].trim());
//					}
//				} else {
//					String matchedString = ComRegexUtil.getMatchedString(line, "^\\s*Metadata:\\s*$");
//					if(ComStrUtil.isBlankOrNull(matchedString)) {
//						// do nothing if it's not reached the Stream part yet.
//					} else {
//						// if it's the beginning Metadata tag
//						isMetadataFound = true;
//					}
//				}
//			} else {
//				String matchedString = ComRegexUtil.getMatchedString(line, "^\\s*Stream #0:\\d+[^:]*:\\s+Subtitle");
//				
//				if(ComStrUtil.isBlankOrNull(matchedString)) {
//					// do nothing if it's not reached the Stream part yet.
//				} else {
//					// if it's the beginning Stream tag
//					isStreamFound = true;
//					name = line.split(":")[1];
//				}
//			}
//		}
//		
//		this.index = Integer.parseInt(ComRegexUtil.getMatchedString(name, "^\\d+"));
//		this.name = name;
//		this.metadata = metadata;
	}


	public SubtitleStream(String originalText, String name, int index, Map<String, String> metadata,
			boolean hasEmbededSubtitleStream, boolean isClosedCaption) {
		// TODO Auto-generated constructor stub
		super(originalText, name, index, metadata, MediaStream.mediaType_subtitle, hasEmbededSubtitleStream);
		this.isClosedCaption = isClosedCaption;
	}
	

}