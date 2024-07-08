package model;

import java.util.HashMap;
import java.util.Map;

import util.commonUtil.ComLogUtil;
import util.commonUtil.ComRegexUtil;
import util.commonUtil.ComStrUtil;

/**
 * @author sihun
 *
 */
public class VideoDuration {

	private Float sec;
	private String sexagesimal;
	
	public VideoDuration(Float sec, String sexagesimal) {
		super();
		this.sec = sec;
		this.sexagesimal = sexagesimal;
	}

	public Float getSec() {
		return sec;
	}

	public String getSexagesimal() {
		return sexagesimal;
	}

	@Override
	public String toString() {
		return sexagesimal;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
//		return this.sec == ((VideoDuration)obj).sec;
//		return Math.abs(this.sec - ((VideoDuration)obj).sec) < 2;
		return true;
	}
	
	
}