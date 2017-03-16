package com.nercms.message.request;

import com.nercms.filter.MagicFilterType;
import com.nercms.message.MessageTag;

/**
 * Created by zsg on 2016/11/9.
 */
public class ChangeFilterRequest extends Request {
    public MagicFilterType type;

    public ChangeFilterRequest() {
        this.tag = MessageTag.CHANGE_FILTER;
    }
}
