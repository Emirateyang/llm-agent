package com.llmagent.json;

import com.llmagent.util.JsonUtil;

public interface JsonCodecFactory {
    /**
     * Create a new {@link JsonUtil.JsonCodec}.
     * @return the new {@link JsonUtil.JsonCodec}.
     */
    JsonUtil.JsonCodec create();
}
