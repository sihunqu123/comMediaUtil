package model;

import java.util.ArrayList;
import java.util.List;

import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;

public class Subtitle {

	public int index;
	public String timeArrage;
	public String content;
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getTimeArrage() {
		return timeArrage;
	}
	public void setTimeArrage(String timeArrage) {
		this.timeArrage = timeArrage;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}

	public Subtitle(int index, String timeArrage, String content) {
		super();
		this.index = index;
		this.timeArrage = timeArrage;
		this.content = content;
	}
	
	public Subtitle(String str) {
		super();
		String[] lines = str.trim().split("\n");
		String content = "";
		Boolean isIndexFound = false;
		Boolean isTimeRangeFound = false;
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			
			if(isIndexFound) {
				
				if(isTimeRangeFound) {
					
					if(ComStrUtil.isBlankOrNull(line)) {
						// no need to add empty line.
					} else {
						if(ComStrUtil.isBlankOrNull(content)) {
							content += line;	
						} else {
							content += " . " + line; // use comma to separte each line
						}
						
					}
				} else { // then it's the timeRange
					isTimeRangeFound = true;
					this.timeArrage = line;
				}
				
			} else {
				String matchedString = ComRegexUtil.getMatchedString(line, "^\\d+\\s*$");
				if(ComStrUtil.isBlankOrNull(matchedString)) {
					// do nothing if index not found yet.
				} else {
					// if it's the index
					isIndexFound = true;
					this.index = Integer.parseInt(matchedString);
				}
			}
		}

		this.content = content;
	}
	@Override
	public String toString() {
		return new StringBuffer("" + this.index).append('\n')
				.append(this.timeArrage).append('\n')
				.append(this.content).append('\n')
				.append('\n')
				.toString();
	}

}

