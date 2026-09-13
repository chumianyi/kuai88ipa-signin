package com.kuai88ipa.signin

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 88ipa HTML页面解析器
 */
object HtmlParser {

    /**
     * 解析应用列表页（首页、分类页、搜索结果页）
     */
    fun parseAppList(html: String): List<AppInfo> {
        val doc: Document = Jsoup.parse(html)
        val apps = mutableListOf<AppInfo>()

        // 每个应用项在 .mdui-container-fluid 中包含 application/链接
        val items = doc.select("div.mdui-container-fluid")
        for (item in items) {
            val linkEl = item.selectFirst("a[href*=/application/]") ?: continue
            val href = linkEl.attr("href")
            val id = href.removePrefix("/application/").removeSuffix(".html")
            if (id.isEmpty() || !id.matches(Regex("\\d+"))) continue

            // 图标
            val imgEl = item.selectFirst("img")
            val iconUrl = imgEl?.attr("src") ?: ""

            // 名称+版本
            val titleEl = item.selectFirst(".is-app-category-title")
            var name = ""
            var version = ""
            if (titleEl != null) {
                val raw = titleEl.text().trim()
                // 格式: "应用名 版本号" — 版本号通常在末尾，以空格分隔的数字.数字
                val match = Regex("^(.*?)\\s+(\\d[\\d.]*)$").find(raw)
                if (match != null) {
                    name = match.groupValues[1].trim()
                    version = match.groupValues[2].trim()
                } else {
                    name = raw
                }
            }

            // 大小+iOS要求
            var size = ""
            var iosVer = ""
            val captionEl = item.selectFirst(".mdui-typo-caption")
            if (captionEl != null) {
                val raw = captionEl.text().trim()
                // 格式: "iOS 6.0 + ｜972.11 KB"
                val sizeMatch = Regex("([\\d.]+\\s*(?:KB|MB|GB))").find(raw)
                size = sizeMatch?.groupValues?.get(1) ?: ""
                val iosMatch = Regex("iOS\\s+([\\d.]+)").find(raw)
                iosVer = iosMatch?.groupValues?.get(1)?.let { "iOS $it+" } ?: ""
            }

            if (name.isNotEmpty()) {
                apps.add(AppInfo(id, name, version, size, iosVer, iconUrl))
            }
        }

        return apps.distinctBy { it.id }
    }

    /**
     * 解析应用详情页
     */
    fun parseAppDetail(html: String, appId: String): AppDetail? {
        val doc: Document = Jsoup.parse(html)

        // 图标
        val iconEl = doc.selectFirst("div.mdui-m-t-1 img, .mdui-card img")
        val iconUrl = iconEl?.attr("src") ?: ""

        // 名称（从title或页面标题）
        var name = ""
        val titleEl = doc.selectFirst("title")
        if (titleEl != null) {
            name = titleEl.text().substringBefore(" - ").trim()
        }

        // 版本
        var version = ""
        doc.select("div.mdui-float-left.mdui-typo-caption-opacity").forEach { el ->
            val next = el.nextElementSibling()
            if (el.text().contains("版本") && next != null) {
                version = next.text().trim()
            }
        }

        // 大小
        var size = ""
        doc.select("div.mdui-float-left.mdui-typo-caption-opacity").forEach { el ->
            val next = el.nextElementSibling()
            if (el.text().contains("大小") && next != null) {
                size = next.text().trim()
            }
        }

        // iOS版本要求
        var iosVer = ""
        val iosEl = doc.selectFirst("div.mdui-float-right.mdui-typo-caption")
        if (iosEl != null) {
            iosVer = iosEl.text().trim()
        }

        // 截图
        val screenshots = mutableListOf<String>()
        doc.select("img[src*=/data/preview/]").forEach { el ->
            val src = el.attr("src")
            if (src.isNotEmpty()) {
                val fullUrl = if (src.startsWith("http")) src else "https://www.88ipa.com$src"
                screenshots.add(fullUrl)
            }
        }

        // 描述
        var description = ""
        val descEl = doc.selectFirst("p[style*=white-space]")
        if (descEl != null) {
            description = descEl.text().trim()
        }

        if (name.isEmpty()) return null

        return AppDetail(
            id = appId,
            name = name,
            version = version,
            size = size,
            iosVersion = iosVer,
            iconUrl = if (iconUrl.startsWith("http")) iconUrl else "https://www.88ipa.com$iconUrl",
            screenshots = screenshots,
            description = description,
            category = ""
        )
    }
}
