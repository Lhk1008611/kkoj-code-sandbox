package com.lhk.kkojcodesandbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {
	/**
	 * 执行所花费的内存（kb）
	 */
	private long memory;

	/**
	 * 执行所花费的时间（ms）
	 */
	private long time;

	/**
	 * 题目执行信息
	 */
	private String message;
}