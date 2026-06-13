package com.example.dianpinghelper.extractor

import com.example.dianpinghelper.model.ShopInfo

/**
 * 店铺信息提取引擎
 *
 * 使用正则规则从文本中提取店铺名称和地址。
 * 支持抖音、小红书、B站及通用文字格式。
 */
object ShopInfoExtractor {

    // ── 正则规则 ──────────────────────────────

    /** 「店名：xxx」/「店铺：xxx」 */
    private val SHOP_NAME_PREFIX = Regex(
        "(?:店铺?[名称叫]?|门店?[名称叫]?|探店)\\s*[：:]\\s*(.{2,40})"
    )

    /** 「📍店铺名」或「📍 店铺名」 */
    private val LOCATION_PIN = Regex("[📍📌]\\s*(.{2,30})")

    /** 抖音常见：「店铺名(地址)」 */
    private val DOUYIN_SHOP = Regex(
        "(?:📍|🏪)?\\s*([\\u4e00-\\u9fff\\w]{2,20})\\s*[（(]\\s*([^）)]{5,40})\\s*[）)]"
    )

    /** 小红书常见：「店名：xxx 📍地址」 */
    private val XIAOHONGSHU = Regex(
        "(?:店名|店铺|探店)[：:]\\s*([\\u4e00-\\u9fff\\w]{2,30})[\\s\\n]*[📍📌]\\s*([\\u4e00-\\u9fff\\w/\\\\,，。\\s]{5,50})"
    )

    /** 「地址：xxx」 */
    private val ADDRESS_PREFIX = Regex(
        "(?:地址|位置|地点|坐标|位于)\\s*[：:]\\s*(.{5,60})"
    )

    /** 地址片段：省市开头 */
    private val ADDRESS_PATTERN = Regex(
        "([省市自治区].{2,30}(?:路|街|巷|号|大厦|广场|中心|商场|城|苑|园|区|栋|楼|层))"
    )

    /** 抖音/B站/小红书 URL 检测 */
    private val PLATFORM_URLS = listOf(
        "douyin.com" to "抖音",
        "iesdouyin.com" to "抖音",
        "xiaohongshu.com" to "小红书",
        "xhslink.com" to "小红书",
        "bilibili.com" to "B站",
        "b23.tv" to "B站",
    )

    // ── 公开接口 ──────────────────────────────

    /**
     * 从文本中提取店铺信息
     */
    fun extract(text: String): ShopInfo? {
        if (text.isBlank()) return null

        val (name, address) = extractByRules(text)

        if (name.isNullOrBlank()) return null

        val platform = detectPlatform(text)

        return ShopInfo(
            name = name.trim(),
            address = address?.trim() ?: "",
            city = guessCity(text, address),
            sourceText = text,
        )
    }

    /**
     * 检测文本来自哪个平台
     */
    fun detectPlatform(text: String): String? {
        for ((domain, platform) in PLATFORM_URLS) {
            if (text.contains(domain, ignoreCase = true)) return platform
        }
        return null
    }

    // ── 规则提取 ──────────────────────────────

    private fun extractByRules(text: String): Pair<String?, String?> {
        var name: String? = null
        var address: String? = null

        // 1. 小红书格式
        val xhsMatch = XIAOHONGSHU.find(text)
        if (xhsMatch != null) {
            return xhsMatch.groupValues[1] to xhsMatch.groupValues[2]
        }

        // 2. 抖音格式
        val dyMatch = DOUYIN_SHOP.find(text)
        if (dyMatch != null) {
            return dyMatch.groupValues[1] to dyMatch.groupValues[2]
        }

        // 3. 「店名：xxx」
        val nameMatch = SHOP_NAME_PREFIX.find(text)
        if (nameMatch != null) {
            name = nameMatch.groupValues[1].trim()
                .replace(Regex("[，,。\\s]+$"), "")
        }

        // 4. 📍 标记
        if (name == null) {
            val pinMatch = LOCATION_PIN.find(text)
            if (pinMatch != null) {
                val candidate = pinMatch.groupValues[1].trim()
                if (candidate.length <= 20) {
                    name = candidate
                }
            }
        }

        // 5. 【】括号
        if (name == null) {
            val bracketMatch = Regex("[【\\[](.*?)[】\\]]").find(text)
            if (bracketMatch != null) {
                val n = bracketMatch.groupValues[1].trim()
                val skipKeywords = listOf("推荐", "打卡", "分享", "收藏", "攻略")
                if (n.length >= 2 && skipKeywords.none { n.contains(it) }) {
                    name = n
                }
            }
        }

        // 地址提取
        val addrMatch = ADDRESS_PREFIX.find(text)
        if (addrMatch != null) {
            address = addrMatch.groupValues[1].trim()
        }
        if (address == null) {
            val addr2Match = ADDRESS_PATTERN.find(text)
            if (addr2Match != null) {
                address = addr2Match.groupValues[1].trim()
            }
        }

        return name to address
    }

    /**
     * 从文本中猜测城市
     */
    private fun guessCity(text: String, address: String?): String {
        val cities = listOf(
            "北京", "上海", "广州", "深圳", "成都", "杭州", "武汉", "西安",
            "南京", "重庆", "长沙", "苏州", "天津", "郑州", "东莞", "青岛",
            "沈阳", "宁波", "昆明", "大连", "厦门", "合肥", "佛山", "福州",
            "哈尔滨", "济南", "温州", "南宁", "长春", "泉州", "石家庄",
        )

        // 优先从地址中找
        if (address != null) {
            for (city in cities) {
                if (address.contains(city)) return city
            }
        }

        // 再从完整文本中找
        for (city in cities) {
            if (text.contains(city)) return city
        }

        return ""
    }
}
