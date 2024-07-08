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
public class VideoResolution {

	private Integer width;
	private Integer height;
	
	public VideoResolution(Integer width, Integer height) {
		super();
		this.width = width;
		this.height = height;
	}
	
	public Integer getWidth() {
		return width;
	}
	public Integer getHeight() {
		return height;
	}
	
	@Override
	public String toString() {
		return width + "x" + height;
	}
	
}