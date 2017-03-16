package com.nercms.message.request;

import com.nercms.model.User;

/**
 *用户上线、下线后  向其余客户端 发送添增加 修改 请求
 * @author zsg
 *
 */
public class AlterUserResquest extends Request{
	public User user;
	public AlterUserResquest(){
	}
}
