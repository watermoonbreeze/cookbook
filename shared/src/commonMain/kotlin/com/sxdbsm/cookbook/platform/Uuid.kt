package com.sxdbsm.cookbook.platform

/**
 * @File : Uuid
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 跨平台随机 UUID 生成
 * <p>
 * 匿名统计标识用：生成一个不含任何设备硬标识、绝不可复原到个人的随机 UUID。
 * `expect fun` 跨平台声明，各平台目录提供 `actual`（Android=java.util.UUID）。
 * <p>
 * [AI生成] 阶段3 匿名统计：匿名标识生成（首启一次、存偏好、卸载重装即换新）。
 **/
expect fun randomUuid(): String
