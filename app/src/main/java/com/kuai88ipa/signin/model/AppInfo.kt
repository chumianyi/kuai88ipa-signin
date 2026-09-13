package com.kuai88ipa.signin

/**
 * 应用列表项数据模型
 */
data class AppInfo(
    val id: String,
    val name: String,
    val version: String,
    val size: String,
    val iosVersion: String,
    val iconUrl: String
)

/**
 * 应用详情数据模型
 */
data class AppDetail(
    val id: String,
    val name: String,
    val version: String,
    val size: String,
    val iosVersion: String,
    val iconUrl: String,
    val screenshots: List<String>,
    val description: String,
    val category: String
)
