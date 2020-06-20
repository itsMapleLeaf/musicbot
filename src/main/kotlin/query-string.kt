fun parseQueryString(queryString: String) =
    queryString.replace(Regex("^\\?"), "")
        .split("&")
        .map { keyValueString ->
            val keyValueList = keyValueString.split("=")
            val key = keyValueList.first()
            val value = keyValueList.getOrNull(1)
            key to value
        }
        .toMap()
